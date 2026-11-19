"""Testes novos puros; mocks nunca são evidência econômica/operacional."""
import copy
import hashlib
import io
import json
import queue
import threading
import unittest
from unittest.mock import Mock
from unittest.mock import patch
import c02_integration
from legacy import lifecycle as historical_lifecycle
from lifecycle import OwnedProcess,StderrCapture
from relay import pump
from transport import (TransportContext,validate_permit,digest,RelayCompletion,
                       complete_c02,stop_client_with_permit)

IDENTITY=dict(ProcessId=42,ParentProcessId=7,ExecutablePath='python.exe',CommandLine='python client fixture',Created='2026-11-15T07:00:00Z')


def permit():
    """Metadados sintéticos coerentes com os eventos R2, sem payload."""
    return dict(client_identity=IDENTITY,controller_pid=7,handle_pid=42,
        scenario='C02',operation_id='same-op',phase='COMMIT_DISPATCHED_CLIENT_UNCERTAIN',caller_status='ECONOMIC_UNCERTAIN',
        connection_id='fixture-1',peer=['127.0.0.1',50001],binding=dict(owner_pid=42,peer=['127.0.0.1',50001]),
        observation=dict(sequence=5,state='RUNNING_MATCH',expected=IDENTITY,observed=IDENTITY,poll_before=None,poll_after=None,error=None),
        pause=dict(sequence=1,kind='RELAY_PAUSE',metadata=dict(event='PAUSE',paused=True,forwarded_server_bytes=0,received_server_bytes=0,held_bytes=0)),
        dispatch=dict(sequence=2,kind='COMMIT_DISPATCHED',pid=42),
        observer=dict(sequence=3,kind='OBSERVER_CONFIRMED_CALLER_UNCERTAIN',op='same-op'),
        stats=dict(sequence=4,kind='RELAY_STATS',metadata=dict(event='STATS',paused=True,forwarded_server_bytes=0,received_server_bytes=18,held_bytes=18)),
        request=dict(sequence=6,kind='PROCESS_TERMINATE',expected=IDENTITY))


def owned(code=0,identity=IDENTITY):
    """Handle simulado: terminate nunca vira operação real."""
    proc=Mock(pid=identity['ProcessId']); proc.poll.return_value=code
    value=OwnedProcess(proc,identity,Mock(return_value=identity),Mock(),controller_pid=7)
    return value


def capture(data=b'',limit=65536):
    """Drena BytesIO pelo mesmo código da thread real."""
    result=StderrCapture(io.BytesIO(data),Mock(),limit)
    result.drain()
    return result


def relay_owned(code=0):
    """Relay distinto do cliente também nos mocks."""
    return owned(code,dict(IDENTITY,ProcessId=99,CommandLine='python relay fixture'))


def terminal(p=None):
    """Evento esperado não contém resultado SQL nem aprovação econômica."""
    return dict(event='EXPECTED_CLIENT_DISCONNECT',transport_result='TRANSPORT_EXPECTED_DISCONNECT',
        termination_requested=True,permit_sha256=digest(p or permit()),source='client',operation='recv_client',
        error='ConnectionResetError',errno=10054,paused=True,forwarded_server_bytes=0,received_server_bytes=18,held_bytes=18)


class TransportTests(unittest.TestCase):
    """Socket, fase, vínculo, ACK e retenção são pré-condições independentes."""
    def exercise(self,p=None,error=None,eof=False):
        client,server=Mock(),Mock()
        server.recv.return_value=b'x'*18
        if eof: client.recv.return_value=b''
        else: client.recv.side_effect=error or ConnectionResetError(10054,'fixture')
        commands=queue.Queue(); commands.put('pause')
        events=[]; calls=0
        def ready(*args):
            nonlocal calls
            calls+=1
            if calls==1:
                if p is not None: commands.put(dict(command='expect_disconnect',permit=p))
                return [server],[],[]
            return [client],[],[]
        context=TransportContext(['127.0.0.1',50001],'fixture-1')
        result=pump(client,server,commands,lambda event,**fields:events.append(dict(event=event,**fields)),context,ready)
        return result,events,client,server

    def test_01_expected_10054_requires_full_permit(self):
        result,events,client,_=self.exercise(permit())
        self.assertEqual(result,'EXPECTED_CLIENT_DISCONNECT')
        self.assertEqual(events[-1]['held_bytes'],18)
        self.assertEqual(events[-1]['transport_result'],'TRANSPORT_EXPECTED_DISCONNECT')
        client.sendall.assert_not_called()

    def test_02_unexpected_10054_without_request(self):
        with self.assertRaises(ConnectionResetError): self.exercise()

    def test_03_reset_server_recv_never_expected(self):
        c,s=Mock(),Mock(); s.recv.side_effect=ConnectionResetError(10054,'fixture'); events=[]
        with self.assertRaises(ConnectionResetError): pump(c,s,queue.Queue(),lambda e,**k:events.append(k),TransportContext(['127.0.0.1',50001],'fixture-1'),lambda *a:([s],[],[]))
        self.assertEqual(events[-1]['source'],'server')
        self.assertEqual(events[-1]['operation'],'recv_server')

    def test_04_unknown_client_endpoint(self):
        p=permit(); p['peer'][1]=50002
        with self.assertRaisesRegex(RuntimeError,'CONNECTION_UNKNOWN'): self.exercise(p)

    def test_05_wrong_identity_blocks(self):
        p=permit(); p['observation']=dict(p['observation'],observed=dict(IDENTITY,Created='other'))
        with self.assertRaisesRegex(RuntimeError,'IDENTITY_NOT_CONFIRMED'): self.exercise(p)

    def test_06_eof_with_held_response(self):
        self.assertEqual(self.exercise(permit(),eof=True)[1][-1]['error'],'EOF')

    def test_07_reset_before_barrier(self):
        p=permit(); p['pause']['sequence']=7
        with self.assertRaisesRegex(RuntimeError,'BARRIER_ORDER'): self.exercise(p)

    def test_08_termination_for_another_client(self):
        p=permit(); p['request']['expected']=dict(IDENTITY,ProcessId=99)
        with self.assertRaisesRegex(RuntimeError,'NOT_REQUESTED'): self.exercise(p)

    def test_09_incompatible_phase(self):
        p=permit(); p['phase']='BEFORE_COMMIT'
        with self.assertRaisesRegex(RuntimeError,'PHASE'): self.exercise(p)

    def test_10_forwarded_response_is_failure(self):
        p=permit(); p['pause']['metadata']['forwarded_server_bytes']=1
        p['pause']['metadata']['received_server_bytes']=1
        with self.assertRaisesRegex(RuntimeError,'NOT_RETAINED'): self.exercise(p)

    def test_11_unknown_socket_owner(self):
        p=permit(); p['binding']['owner_pid']=99
        with self.assertRaisesRegex(RuntimeError,'SOCKET_NOT_OWNED'): self.exercise(p)

    def test_12_missing_observer_in_c02(self):
        p=permit(); p['observer']['kind']='NOT_CONFIRMED'
        with self.assertRaisesRegex(RuntimeError,'OBSERVER'): self.exercise(p)

    def test_13_counter_corruption(self):
        p=permit(); p['stats']['metadata']['received_server_bytes']=19
        with self.assertRaisesRegex(RuntimeError,'COUNTER'): self.exercise(p)

    def test_14_connection_abort_still_fails(self):
        with self.assertRaises(ConnectionAbortedError): self.exercise(permit(),ConnectionAbortedError(10053,'fixture'))

    def test_15_timeout_still_fails(self):
        with self.assertRaises(TimeoutError): self.exercise(permit(),TimeoutError('fixture'))

    def test_16_send_server_error_not_mislabelled_client(self):
        c,s=Mock(),Mock(); c.recv.return_value=b'opaque'; s.sendall.side_effect=ConnectionResetError(10054,'fixture'); events=[]
        with self.assertRaises(ConnectionResetError): pump(c,s,queue.Queue(),lambda e,**k:events.append(k),TransportContext(['127.0.0.1',50001],'fixture-1'),lambda *a:([c],[],[]))
        self.assertEqual((events[-1]['source'],events[-1]['operation']),('server','send_server'))

    def test_17_send_client_error_is_not_recv_disconnect(self):
        c,s=Mock(),Mock(); s.recv.return_value=b'opaque'; c.sendall.side_effect=ConnectionResetError(10054,'fixture'); events=[]
        with self.assertRaises(ConnectionResetError): pump(c,s,queue.Queue(),lambda e,**k:events.append(k),TransportContext(['127.0.0.1',50001],'fixture-1'),lambda *a:([s],[],[]))
        self.assertEqual((events[-1]['source'],events[-1]['operation']),('client','send_client'))

    def test_18_unexpected_eof_fails(self):
        with self.assertRaisesRegex(RuntimeError,'UNEXPECTED_EOF'): self.exercise(eof=True)

    def test_19_arm_is_one_use_and_frozen(self):
        p=permit(); ctx=TransportContext(p['peer'],p['connection_id']); ctx.arm(p,True,0,18,18)
        p['stats']['metadata']['held_bytes']=100
        self.assertEqual(ctx.permit['stats']['metadata']['held_bytes'],18)
        with self.assertRaisesRegex(RuntimeError,'ALREADY_ARMED'): ctx.arm(permit(),True,0,18,18)
        with self.assertRaisesRegex(RuntimeError,'FORWARDED'): ctx.expected('client','recv_client',None,True,1,19,18)

    def test_20_guard_emits_request_before_arm_and_terminate(self):
        o=owned(None); order=[]; seq=4
        def emit(kind,**data):
            nonlocal seq
            seq+=1; order.append(kind)
            return dict(sequence=seq,kind=kind,**data)
        o.emit=emit
        o.proc.poll.side_effect=[None,None,None,1]
        o.proc.wait.return_value=1
        o.proc.terminate.side_effect=lambda:order.append('HANDLE_TERMINATE')
        def ack(p): order.append('ARM_ACK'); return dict(event='EXPECT_DISCONNECT',permit_sha256=digest(p))
        result=stop_client_with_permit(o,permit(),ack)
        self.assertTrue(result['terminated_by_controller'])
        self.assertLess(order.index('PROCESS_TERMINATE'),order.index('ARM_ACK'))
        self.assertLess(order.index('ARM_ACK'),order.index('HANDLE_TERMINATE'))

    def test_21_bad_ack_prevents_terminate(self):
        o=owned(None); seq=4
        def emit(kind,**data):
            nonlocal seq
            seq+=1; return dict(sequence=seq,kind=kind,**data)
        o.emit=emit
        with self.assertRaisesRegex(RuntimeError,'NOT_ACKNOWLEDGED'): stop_client_with_permit(o,permit(),lambda p:{})
        o.proc.terminate.assert_not_called()

    def test_22_mismatch_prevents_arm(self):
        o=owned(None); o.inspect.return_value=dict(IDENTITY,Created='other')
        o.emit=lambda kind,**data:dict(sequence=7,kind=kind,**data)
        arm=Mock()
        with self.assertRaisesRegex(RuntimeError,'IDENTITY_MISMATCH'): stop_client_with_permit(o,permit(),arm)
        arm.assert_not_called(); o.proc.terminate.assert_not_called()


class CaptureTests(unittest.TestCase):
    """Bytes mistos, limite e erro de thread não interrompem silenciosamente."""
    def test_23_valid_utf8_count_hash(self):
        data=b'{"event":"RELAY_ERROR","error":"ValueError","errno":2}\n'
        value=capture(data).validate()
        self.assertEqual(value['bytes_read'],len(data)); self.assertEqual(value['sha256'],hashlib.sha256(data).hexdigest())
        self.assertEqual(value['decode_replacements'],0); self.assertIn('ValueError',value['stderr'])

    def test_24_invalid_utf8_survives(self):
        value=capture(b'erro \xe7\xff\n').validate()
        self.assertEqual(value['invalid_utf8_bytes'],2); self.assertTrue(value['text_lossy'])
        self.assertIsNone(value['read_error']); self.assertFalse(value['truncated'])

    def test_25_mixed_accents_no_codepage_assumption(self):
        data='ação válida '.encode()+b'\xe7\xe3o\n'
        value=capture(data).validate()
        self.assertEqual(value['invalid_utf8_bytes'],2)
        self.assertNotIn('cp1252',json.dumps(value))

    def test_26_limit_keeps_draining_and_hashes_all(self):
        data=b'x'*100000
        cap=capture(data,64); value=cap.snapshot()
        self.assertEqual(value['retained_bytes'],64); self.assertEqual(value['bytes_read'],len(data))
        self.assertEqual(value['sha256'],hashlib.sha256(data).hexdigest()); self.assertTrue(value['truncated'])
        self.assertFalse(value['capture_complete'])
        with self.assertRaisesRegex(RuntimeError,'INCOMPLETE'): cap.validate()

    def test_27_read_failure_in_thread_propagates(self):
        stream=Mock(); stream.read.side_effect=[b'partial',OSError('password=do-not-log')]
        cap=StderrCapture(stream,Mock()); thread=threading.Thread(target=cap.drain); thread.start(); thread.join(2)
        value=cap.snapshot(); self.assertFalse(thread.is_alive()); self.assertEqual(value['read_error'],'OSError')
        self.assertFalse(value['capture_complete']); self.assertFalse(value['truncated'])
        self.assertNotIn('do-not-log',json.dumps(value))
        with self.assertRaisesRegex(RuntimeError,'INCOMPLETE'): cap.validate()

    def test_28_sensitive_values_and_sql_never_persist(self):
        data=b'password=abc token=xyz\npostgresql://user:secret@localhost/db\nSELECT payload FROM private;\n{"event":"RELAY_ERROR","error":"ValueError","password":"hidden","sql":"secret SQL"}\n'
        value=capture(data).validate()
        for sensitive in ('abc','xyz','secret','hidden','payload','postgresql://','SELECT'):
            self.assertNotIn(sensitive,value['stderr'])
        self.assertFalse(value['raw_bytes_persisted'])

    def test_29_split_utf8_sequence(self):
        stream=Mock(); stream.read.side_effect=[b'\xc3',b'\xa7\n',b'']
        cap=StderrCapture(stream,Mock()); cap.drain()
        self.assertEqual(cap.validate()['invalid_utf8_bytes'],0)

    def test_30_literal_replacement_not_counted_as_invalid(self):
        self.assertEqual(capture('\ufffd'.encode()).validate()['decode_replacements'],0)

    def test_31_trailing_invalid_sequence_is_recorded(self):
        self.assertEqual(capture(b'\xe2\x82').validate()['invalid_utf8_bytes'],2)

    def test_32_without_eof_capture_is_incomplete(self):
        cap=StderrCapture(io.BytesIO(b''),Mock())
        with self.assertRaisesRegex(RuntimeError,'INCOMPLETE'): cap.validate()

    def test_47_sanitized_expansion_is_bounded_and_explicit(self):
        cap=capture(b'a\n'*3000)
        value=cap.snapshot()
        self.assertTrue(value['text_truncated']); self.assertFalse(value['capture_complete'])
        self.assertLessEqual(len(value['stderr'].encode()),65536)
        with self.assertRaisesRegex(RuntimeError,'INCOMPLETE'): cap.validate()


class CompletionTests(unittest.TestCase):
    """Resultados independentes: transporte/processo não aprovam transação/teste."""
    def test_33_zero_spontaneous_exit_is_not_success(self):
        o=relay_owned(); done=RelayCompletion(o,capture(),permit())
        with self.assertRaisesRegex(RuntimeError,'UNEXPECTED_EXIT'): done.close({'event':'EOF'})
        o.proc.terminate.assert_not_called()

    def test_34_nonzero_exit_is_failure(self):
        o=relay_owned(1)
        with self.assertRaisesRegex(RuntimeError,'NONZERO'): RelayCompletion(o,capture(),permit()).close(terminal())
        o.proc.terminate.assert_not_called()

    def test_35_close_twice_no_extra_action(self):
        o=relay_owned(); done=RelayCompletion(o,capture(),permit())
        first=done.close(terminal()); self.assertIs(done.close(terminal()),first)
        o.inspect.assert_not_called(); o.proc.terminate.assert_not_called()
        self.assertEqual(first['sql_result'],'ECONOMIC_UNCERTAIN'); self.assertEqual(first['economic_test'],'NOT_EVALUATED')

    def test_36_capture_error_invalidates_completion(self):
        cap=StderrCapture(io.BytesIO(),Mock())
        with self.assertRaisesRegex(RuntimeError,'CAPTURE_INCOMPLETE'): RelayCompletion(relay_owned(),cap,permit()).close(terminal())

    def test_37_no_new_worker_cannot_pass_c02(self):
        args=self.completion_args(); args['new_worker']=None
        with self.assertRaisesRegex(RuntimeError,'NEW_WORKER_REQUIRED'): complete_c02(**args)

    def completion_args(self):
        """Todos os dados abaixo são mocks, nunca prova SQL."""
        receipt=dict(status='CONFIRMED',receipt=dict(op='same-op'))
        return dict(completion=RelayCompletion(relay_owned(),capture(),permit()).close(terminal()),old_worker=owned(),
            new_worker=owned(None,dict(IDENTITY,ProcessId=43,Created='later')),intent={'op':'same-op'},reloaded_intent={'op':'same-op'},backend_ended=True,
            observer_receipt=receipt,lookup_receipt=receipt,retry_receipt=receipt,baseline_after_commit=[{'sr':1,'er':1,'coins':102}],after_retry=[{'sr':1,'er':1,'coins':102}])

    def test_38_changed_snapshot_is_rejected(self):
        args=self.completion_args(); args['after_retry']=[{'sr':2,'er':2,'coins':104}]
        with self.assertRaisesRegex(RuntimeError,'RETRY_CHANGED'): complete_c02(**args)

    def test_39_complete_mock_proof_only(self):
        self.assertEqual(complete_c02(**self.completion_args())['economic_test'],'SCENARIO_PASSED')

    def test_40_wrong_operation_id_is_rejected(self):
        args=self.completion_args(); args['intent']={'op':'other'}; args['reloaded_intent']={'op':'other'}
        with self.assertRaisesRegex(RuntimeError,'BOUND_TO_C02'): complete_c02(**args)

    def test_41_terminal_permit_mismatch_rejected(self):
        value=terminal(); value['permit_sha256']='wrong'
        with self.assertRaisesRegex(RuntimeError,'PROOF_MISSING'): RelayCompletion(relay_owned(),capture(),permit()).close(value)

    def test_42_reused_worker_is_rejected(self):
        args=self.completion_args(); args['new_worker']=args['old_worker']
        with self.assertRaisesRegex(RuntimeError,'NEW_WORKER_REQUIRED'): complete_c02(**args)

    def test_43_process_guards_are_original_class(self):
        self.assertIs(OwnedProcess,historical_lifecycle.OwnedProcess)

    def test_48_fixture_transport_cannot_approve_c02(self):
        args=self.completion_args(); args['completion']['scenario']='FIXTURE_EXPECTED'
        with self.assertRaisesRegex(RuntimeError,'BOUND_TO_C02'): complete_c02(**args)


class IntegrationTests(unittest.TestCase):
    """Adaptador C02 preparado sem runtime SQL; mocks verificam ordem e recusas."""
    def test_44_no_r3_authorization_no_dependencies_called(self):
        suite,io_layer=Mock(),Mock()
        with self.assertRaisesRegex(RuntimeError,'R3_AUTHORIZATION_REQUIRED'):
            c02_integration.prepared_c02(suite,io_layer,lambda:False)
        self.assertEqual(suite.mock_calls,[]); self.assertEqual(io_layer.mock_calls,[])

    def adapter(self):
        """Instancia dependências inertes; não representa resultado PostgreSQL."""
        s,io_layer=Mock(),Mock(); p=permit()
        old=owned(); new=owned(None,dict(IDENTITY,ProcessId=43,Created='later'))
        writer,new_writer=Mock(pid=100),Mock(pid=101)
        s.w=Mock(); s.o=Mock()
        relay=Mock(connection_id='fixture-1',capture=capture())
        io_layer.create_identified_relay.return_value=relay
        io_layer.create_relay_worker.return_value=writer
        io_layer.owned_worker.side_effect=lambda w:new if w is new_writer else old
        io_layer.owned_relay.return_value=relay_owned()
        s.intent.return_value={'op':'same-op'}
        s.load.return_value={'op':'same-op'}
        before=[{'image':{'head':{'er':0}}}]; after=[{'image':{'head':{'er':1}}}]
        s.prepared.return_value=(before,{'kind':'PREPARED_WITHOUT_COMMIT'})
        s.snapshot.return_value=after
        writer.dispatch_commit.return_value=p['dispatch']
        io_layer.pause_event.return_value=p['pause']; io_layer.observer_event.return_value=p['observer']
        io_layer.held_stats_event.return_value=p['stats']; io_layer.verify_os_socket_owner.return_value=p['binding']
        io_layer.wait_transport_and_pipes.return_value=(terminal(),p)
        receipt=dict(status='CONFIRMED',receipt=dict(op='same-op'))
        s.lookup.return_value=receipt; s.apply.return_value=receipt
        s.replace.side_effect=lambda label:setattr(s,'w',new_writer)
        return s,io_layer

    def test_45_adapter_retains_uncertain_until_full_reconciliation(self):
        s,io_layer=self.adapter()
        with patch.object(c02_integration,'stop_client_with_permit'):
            result=c02_integration.prepared_c02(s,io_layer,lambda:True)
        self.assertEqual(result['economic_test'],'SCENARIO_PASSED')
        methods=[call[0] for call in io_layer.mock_calls]
        self.assertLess(methods.index('persist_uncertain'),methods.index('observer_event'))
        self.assertLess(methods.index('wait_transport_and_pipes'),methods.index('persist_complete_result'))
        s.replace.assert_called_once_with('C02-new-worker'); s.apply.assert_called_once_with({'op':'same-op'})

    def test_46_adapter_partial_reconciliation_never_persists_success(self):
        s,io_layer=self.adapter()
        s.apply.return_value={'status':'CONFIRMED','receipt':{'op':'other'}}
        with patch.object(c02_integration,'stop_client_with_permit'):
            with self.assertRaisesRegex(RuntimeError,'RECEIPT_DIVERGED'):
                c02_integration.prepared_c02(s,io_layer,lambda:True)
        io_layer.persist_uncertain.assert_called_once()
        io_layer.persist_complete_result.assert_not_called()


if __name__=='__main__': unittest.main(verbosity=2)
