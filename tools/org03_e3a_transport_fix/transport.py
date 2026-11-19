"""Contrato observável do encerramento; não conhece SQL nem aprova economia."""
import copy
import hashlib
import json
import re
from lifecycle import OwnedProcess


def digest(value):
    """Hash canônico de metadados ou intenções; não registra seu conteúdo TCP."""
    return hashlib.sha256(json.dumps(value,sort_keys=True,separators=(',',':')).encode()).hexdigest()


def require(condition,reason):
    """Guardas permanecem ativas mesmo sob python -O."""
    if not condition: raise RuntimeError(reason)


def validate_permit(permit,peer,connection_id):
    """Confere vínculo, observação RUNNING_MATCH e ordem das barreiras/pedido."""
    p=copy.deepcopy(permit)
    require(isinstance(p.get('operation_id'),str) and re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_-]{0,127}',p['operation_id']) is not None,'OPERATION_ID_REQUIRED')
    expected=p['client_identity']; observed=p['observation']
    require(all(expected.get(k) for k in OwnedProcess.fields),'CLIENT_IDENTITY_INCOMPLETE')
    require(observed['state']=='RUNNING_MATCH' and observed['expected']==expected and observed['observed']==expected,'CLIENT_IDENTITY_NOT_CONFIRMED')
    require(observed['poll_before'] is None and observed['poll_after'] is None and not observed['error'],'CLIENT_HANDLE_NOT_RUNNING')
    require(p['controller_pid']==expected['ParentProcessId'] and p['handle_pid']==expected['ProcessId'],'CLIENT_HANDLE_UNLINKED')
    require(p['scenario'] in ('C02','C05','FIXTURE_EXPECTED'),'SCENARIO_NOT_AUTHORIZED')
    require(p['phase']=='COMMIT_DISPATCHED_CLIENT_UNCERTAIN' and p['caller_status']=='ECONOMIC_UNCERTAIN','DISCONNECT_PHASE_MISMATCH')
    require(p['connection_id']==connection_id and p['peer']==list(peer),'CLIENT_CONNECTION_UNKNOWN')
    require(p['binding']['owner_pid']==expected['ProcessId'] and p['binding']['peer']==list(peer),'CLIENT_SOCKET_NOT_OWNED')
    pause,dispatch,stats,request=(p[k] for k in ('pause','dispatch','stats','request'))
    require(pause['sequence']<dispatch['sequence']<stats['sequence']<request['sequence'],'BARRIER_ORDER_MISMATCH')
    require(stats['sequence']<observed['sequence']<request['sequence'],'IDENTITY_OBSERVATION_OUT_OF_PHASE')
    require(pause['kind']=='RELAY_PAUSE' and dispatch['kind']=='COMMIT_DISPATCHED' and stats['kind']=='RELAY_STATS','BARRIER_KIND_MISMATCH')
    require(dispatch['pid']==expected['ProcessId'] and request['kind']=='PROCESS_TERMINATE' and request['expected']==expected,'TERMINATION_NOT_REQUESTED_FOR_CLIENT')
    a,b=pause['metadata'],stats['metadata']
    require(all(type(value[k]) is int and value[k]>=0 for value in (a,b) for k in ('forwarded_server_bytes','received_server_bytes','held_bytes')),'COUNTER_TYPE_MISMATCH')
    require(a['event']=='PAUSE' and b['event']=='STATS' and a['paused'] and b['paused'],'PAUSE_NOT_PROVEN')
    require(a['held_bytes']==0 and a['received_server_bytes']==a['forwarded_server_bytes'],'PAUSE_COUNTER_DIVERGENCE')
    require(a['forwarded_server_bytes']==b['forwarded_server_bytes'] and b['held_bytes']>0,'RESPONSE_NOT_RETAINED')
    require(b['received_server_bytes']==b['forwarded_server_bytes']+b['held_bytes'],'COUNTER_DIVERGENCE')
    if p['scenario']=='C02':
        observer=p['observer']
        require(dispatch['sequence']<observer['sequence']<stats['sequence'] and observer['kind']=='OBSERVER_CONFIRMED_CALLER_UNCERTAIN' and observer.get('op')==p['operation_id'],'OBSERVER_CONFIRMATION_MISSING')
    return p


class TransportContext:
    """Uma conexão aceita; permit privado não pode trocar o socket beneficiário."""
    def __init__(self,peer,connection_id):
        self.peer=list(peer); self.connection_id=connection_id
        self.permit=None

    def arm(self,permit,paused,sent,received,held):
        """ACK ocorre no próprio pump antes do terminate do handle do cliente."""
        require(self.permit is None,'DISCONNECT_ALREADY_ARMED')
        value=validate_permit(permit,self.peer,self.connection_id)
        self._counters(value,paused,sent,received,held)
        self.permit=value
        return digest(value)

    def _counters(self,p,paused,sent,received,held):
        """Retenção pode crescer; encaminhamento nunca avança após PAUSE."""
        require(paused and sent==p['pause']['metadata']['forwarded_server_bytes'],'FORWARDED_BYTES_CHANGED')
        require(received==sent+held and held>=p['stats']['metadata']['held_bytes']>0,'HELD_BYTES_DIVERGED')

    def expected(self,source,operation,error,paused,sent,received,held):
        """Número do erro sozinho é insuficiente, inclusive para EOF."""
        if self.permit is None or source!='client' or operation!='recv_client': return False
        if error is not None and not (isinstance(error,ConnectionResetError) and getattr(error,'errno',None) in (10054,104)): return False
        self._counters(self.permit,paused,sent,received,held)
        return True


def stop_client_with_permit(owned,context,send_and_ack):
    """Intercepta somente o evento pré-terminate da guarda intacta R1."""
    original=owned.emit
    observation=None
    requested=False
    def emit(kind,**data):
        nonlocal observation,requested
        event=original(kind,**data)
        require(isinstance(event,dict) and 'sequence' in event,'CONTROLLER_EVENT_NOT_RECORDED')
        if kind=='PROCESS_OBSERVATION': observation=copy.deepcopy(event)
        if kind=='PROCESS_TERMINATE':
            require(not requested and observation is not None,'TERMINATION_OBSERVATION_MISSING')
            permit=copy.deepcopy(context)
            permit.update(client_identity=owned.expected,controller_pid=owned.controller_pid,handle_pid=owned.proc.pid,
                          observation=observation,request=event)
            validate_permit(permit,permit['peer'],permit['connection_id'])
            # Falha no ACK levanta antes de proc.terminate; não há retry/fallback.
            ack=send_and_ack(permit)
            require(ack.get('event')=='EXPECT_DISCONNECT' and ack.get('permit_sha256')==digest(permit),'DISCONNECT_ARM_NOT_ACKNOWLEDGED')
            requested=True
        return event
    owned.emit=emit
    try:
        result=owned.stop(timeout=5)
        require(requested and result['state']=='EXITED_CONFIRMED','EXPECTED_CLIENT_TERMINATION_NOT_COMPLETED')
        return result
    finally: owned.emit=original


class RelayCompletion:
    """Fim do transporte/processo não converte UNCERTAIN em cenário aprovado."""
    def __init__(self,owned,capture,permit):
        self.owned,self.capture=owned,capture
        self.permit=validate_permit(permit,permit['peer'],permit['connection_id'])
        require(owned.expected['ProcessId']!=self.permit['client_identity']['ProcessId'],'RELAY_CLIENT_HANDLE_COLLISION')
        self.result=None

    def close(self,terminal):
        """Só finaliza após handle encerrado; chamadas repetidas são inertes."""
        if self.result is not None: return self.result
        require(not self.owned.blocked,'RELAY_IDENTITY_BLOCKED')
        process=self.owned.observe()
        require(process['state']=='EXITED_CONFIRMED','RELAY_EXIT_NOT_CONFIRMED')
        require(process['exit_code']==0,'RELAY_NONZERO_EXIT')
        require(terminal and terminal['event']=='EXPECTED_CLIENT_DISCONNECT' and terminal['transport_result']=='TRANSPORT_EXPECTED_DISCONNECT','RELAY_UNEXPECTED_EXIT')
        require(terminal.get('permit_sha256')==digest(self.permit) and terminal.get('termination_requested') is True,'TRANSPORT_PROOF_MISSING')
        require(terminal.get('source')=='client' and terminal.get('operation')=='recv_client','TRANSPORT_SOCKET_MISMATCH')
        require(terminal.get('error')=='EOF' or (terminal.get('error')=='ConnectionResetError' and terminal.get('errno') in (10054,104)),'TRANSPORT_ERROR_MISMATCH')
        context=TransportContext(self.permit['peer'],self.permit['connection_id'])
        context._counters(self.permit,terminal['paused'],terminal['forwarded_server_bytes'],terminal['received_server_bytes'],terminal['held_bytes'])
        stderr=self.capture.validate()
        self.result=dict(transport_result='TRANSPORT_EXPECTED_DISCONNECT',process_result='RELAY_EXITED_CONFIRMED',
                         scenario=self.permit['scenario'],operation_id=self.permit['operation_id'],client_identity_sha256=digest(self.permit['client_identity']),
                         process=process,stderr=stderr,sql_result='ECONOMIC_UNCERTAIN',economic_test='NOT_EVALUATED')
        return self.result


def complete_c02(*,completion,old_worker,new_worker,intent,reloaded_intent,backend_ended,
                 observer_receipt,lookup_receipt,retry_receipt,baseline_after_commit,after_retry):
    """Gate preparado: exige consulente novo, recibo histórico e imagem integral."""
    require(completion.get('transport_result')=='TRANSPORT_EXPECTED_DISCONNECT' and completion.get('process_result')=='RELAY_EXITED_CONFIRMED','TRANSPORT_NOT_COMPLETE')
    require(completion.get('scenario')=='C02' and completion.get('operation_id')==intent.get('op') and completion.get('client_identity_sha256')==digest(old_worker.expected),'TRANSPORT_NOT_BOUND_TO_C02_CLIENT_AND_OPERATION')
    require(new_worker is not None and new_worker.proc is not old_worker.proc,'NEW_WORKER_REQUIRED')
    require(not old_worker.blocked and not new_worker.blocked,'WORKER_IDENTITY_BLOCKED')
    require(old_worker.observe()['state']=='EXITED_CONFIRMED' and new_worker.observe()['state']=='RUNNING_MATCH','WORKER_IDENTITY_NOT_CONFIRMED')
    require(old_worker.expected!=new_worker.expected and backend_ended is True,'OLD_BACKEND_OR_WORKER_REUSED')
    require(intent==reloaded_intent and intent.get('op'),'INTENTION_CHANGED')
    require(observer_receipt==lookup_receipt==retry_receipt and lookup_receipt.get('status')=='CONFIRMED','HISTORICAL_RECEIPT_DIVERGED')
    require(lookup_receipt['receipt']['op']==intent['op'],'OPERATION_ID_CHANGED')
    require(bool(baseline_after_commit) and baseline_after_commit==after_retry,'RETRY_CHANGED_REVISIONS_OR_RESOURCES')
    return dict(economic_test='SCENARIO_PASSED',sql_result='CONFIRMED',operation_id=intent['op'],intent_sha256=digest(intent))
