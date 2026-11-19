"""Adaptador preparado de C02; somente injeção futura, sem runtime/SQL ou CLI."""
import time
from transport import (require,stop_client_with_permit,RelayCompletion,complete_c02)


def prepared_c02(suite,io,authorize):
    """Ordena os 15 passos; dependências SQL só virão de pacote R3 autorizado.

    io cria relay/worker, confere o dono SO do socket aceito, persiste UNCERTAIN,
    espera os pipes/handle e devolve OwnedProcess exatos. Este módulo não fornece
    essas dependências e não pode abrir banco por conta própria.
    """
    require(authorize() is True,'R3_AUTHORIZATION_REQUIRED')
    suite.w.close()
    relay=io.create_identified_relay()
    suite.w=io.create_relay_worker(relay)
    original=io.owned_worker(suite.w)
    intent=suite.intent('C02')
    before,prepared=suite.prepared(intent)
    pause=io.pause_event(relay)
    dispatch=suite.w.dispatch_commit()
    io.persist_uncertain(intent,dispatch)
    deadline=time.monotonic()+5
    while True:
        observed=suite.lookup(intent,suite.o)
        if observed['status']=='CONFIRMED': break
        require(time.monotonic()<deadline,'C02_OBSERVER_TIMEOUT')
    observer=io.observer_event(intent,observed)
    postcommit=suite.snapshot()
    stats=io.held_stats_event(relay)
    binding=io.verify_os_socket_owner(suite.w,relay)
    context=dict(scenario='C02',operation_id=intent['op'],phase='COMMIT_DISPATCHED_CLIENT_UNCERTAIN',caller_status='ECONOMIC_UNCERTAIN',
        connection_id=relay.connection_id,peer=binding['peer'],binding=binding,
        pause=pause,dispatch=dispatch,observer=observer,stats=stats)
    stop_client_with_permit(original,context,lambda permit:io.arm_private_channel(relay,permit))
    terminal,permit=io.wait_transport_and_pipes(relay)
    completion=RelayCompletion(io.owned_relay(relay),relay.capture,permit).close(terminal)
    suite.wait_gone(suite.w.pid)
    suite.replace('C02-new-worker')
    loaded=suite.load(intent)
    lookup=suite.lookup(loaded)
    require(lookup['status']=='CONFIRMED','C02_HISTORICAL_RECEIPT_NOT_CONFIRMED')
    retry=suite.apply(suite.load(intent))
    after=suite.snapshot()
    result=complete_c02(completion=completion,old_worker=original,new_worker=io.owned_worker(suite.w),
        intent=intent,reloaded_intent=loaded,backend_ended=True,
        observer_receipt=observed,lookup_receipt=lookup,retry_receipt=retry,
        baseline_after_commit=postcommit,after_retry=after)
    require(postcommit[0]['image']['head']['er']==before[0]['image']['head']['er']+1,'C02_REVISION_INCREMENT_MISMATCH')
    suite.remember(intent)
    io.persist_complete_result(result,intent,before,postcommit,after,prepared,completion)
    return result


if __name__=='__main__':
    raise SystemExit('P1_PREPARATION_ONLY: runtime SQL e execução R3 exigem aceite próprio')
