"""Revisão estática independente: lê arquivos; não importa nem executa o harness.

Executar a partir de qualquer diretório. A única escrita é resultado.json nesta
pasta nova. Não consulta processos, sockets, SQL ou credenciais.
"""
import hashlib
import json
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
RUN = ROOT / 'temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70'
EV = RUN / 'evidence'
OUT = Path(__file__).with_name('resultado.json')


def load(path):
    return json.loads(path.read_text(encoding='utf-8'))


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def canonical(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(',', ':')).encode()).hexdigest()


def main():
    checks = []

    def check(name, condition):
        checks.append({'verificacao': name, 'passou': bool(condition)})

    # Inventário independente anterior a qualquer gravação desta auditoria.
    inventory = {p.relative_to(ROOT).as_posix(): sha(p) for p in RUN.rglob('*') if p.is_file()}
    maps = {
        'selo_final_176': load(EV / 'artifact-sha256.json'),
        'preservacao_4147': load(EV / 'preservation-after.json'),
        'preparacao_43': load(RUN / 'preparation/artifact-sha256.json'),
    }
    differences = {}
    for label, mapping in maps.items():
        differences[label] = [p for p, h in mapping.items() if not (ROOT / p).is_file() or sha(ROOT / p) != h]
        check(label, not differences[label])
    check('contagens_dos_manifestos', [len(maps[k]) for k in maps] == [176, 4147, 43])
    check('baseline_antes_igual_depois', load(RUN / 'preparation/preservation-before.json') == maps['preservacao_4147'])

    events = [json.loads(line) for line in (EV / 'events.jsonl').read_text(encoding='utf8').splitlines()]
    transcript = [json.loads(line) for line in (EV / 'sql-transcript.jsonl').read_text(encoding='utf8').splitlines()]
    check('306_eventos_sequenciais', [e['sequence'] for e in events] == list(range(1, 307)))
    check('450_registros_SQL', len(transcript) == 450)
    check('SQLSTATE_somente_sucesso_e_negativas_previstas', {t['sqlstate'] for t in transcript} == {'00000', 'P0003', '42501'})
    snaps = [e for e in events if e['kind'] == 'SNAPSHOT_VALIDATED']
    check('58_snapshots_distintos', len(snaps) == len({e['file'] for e in snaps}) == 58)
    snapshot_errors = []
    for event in snaps:
        data = load(EV / event['file'])
        valid = canonical(data) == event['sha256'] and len(data) == 4
        for row in data:
            image = row['image']; head = image['head']
            rs = row['receipts']; ops = row['operations']; gs = row['grants']
            ids = [r['op'] for r in rs]
            valid &= len(ids) == len(set(ids)) == head['er'] == image['receipts'] == image['operations'] == image['grants']
            valid &= set(ids) == {o['op'] for o in ops} == {g['op'] for g in gs}
            valid &= len(gs) == len(ids) == len(ops)
            versions = {v['sr']: v for v in row['versions']}
            valid &= versions[head['sr']]['payload'] == image['state']
            valid &= versions[head['sr']]['er'] == head['er']
            for receipt in rs:
                historical = receipt['result']
                if historical is None:
                    archive = EV / receipt['archive']
                    valid &= archive.resolve().parent == EV.resolve() and sha(archive) == receipt['digest']
                    historical = load(archive)
                valid &= historical['op'] == receipt['op']
                valid &= historical['state'] == versions[receipt['sr']]['payload']
                valid &= receipt['er'] == versions[receipt['sr']]['er']
                operation = next(o for o in ops if o['op'] == receipt['op'])
                grant = next(g for g in gs if g['op'] == receipt['op'])
                valid &= operation['intent']['grant'] == grant['grant_id']
                valid &= operation['intent']['origin'] == grant['origin']
        if not valid:
            snapshot_errors.append(event['file'])
    check('imagens_historico_bijecao_revisoes', not snapshot_errors)

    results = load(EV / 'results.json')
    cases = {f'C{i:02}': load(EV / f'C{i:02}.json') for i in range(1, 10)}
    check('nove_PASSOU_com_evidencias_iguais', all(cases[r['case']] == r and r['status'] == 'PASSOU' for r in results['results']) and len(results['results']) == 9)
    c = {k: v['evidence'] for k, v in cases.items()}
    check('C01_C03_rollback_C04_C08_imagem_preservada', all(c[k]['before'] == c[k]['after'] for k in ('C01', 'C03', 'C04', 'C08')))
    check('C01_C03_consulta_negativa', all(c[k]['reconciliation']['status'] == 'NOT_FOUND' for k in ('C01', 'C03')))
    check('C02_reconciliacao_completa_separada_do_transporte', c['C02']['postcommit'] == c['C02']['after'] and c['C02']['result']['economic_test'] == 'SCENARIO_PASSED' and c['C02']['completion']['economic_test'] == 'NOT_EVALUATED' and c['C02']['completion']['sql_result'] == 'ECONOMIC_UNCERTAIN' and c['C02']['result']['operation_id'] == c['C02']['permit']['operation_id'] == 'C02-2')
    terminal = [e['metadata'] for e in events if e['kind'] == 'RELAY_WIRE_EVENT' and e['metadata'].get('event') == 'EXPECTED_CLIENT_DISCONNECT']
    check('dois_resets_esperados_com_contabilidade', len(terminal) == 2 and all(t['errno'] == 10054 and t['termination_requested'] and t['held_bytes'] == 18 and t['received_server_bytes'] == t['forwarded_server_bytes'] + t['held_bytes'] for t in terminal))
    check('permit_C02_hash', terminal[0]['permit_sha256'] == canonical(c['C02']['permit']))
    check('C05_incerto_antes_lookup_confirmado', c['C05']['caller_status_before_lookup'] == 'ECONOMIC_UNCERTAIN' and c['C05']['reconciliation']['status'] == 'CONFIRMED')
    check('C06_somente_B_pendente', c['C06']['identified_pending'] == ['C06-B-7'] and c['C06']['final'][2] == c['C06']['partial'][2])
    check('C07_prefixo_lacuna', c['C07']['partial'][0]['details']['watermarks'][0]['prefix'] == 0 and c['C07']['final'][0]['details']['watermarks'][0]['prefix'] == 2 and len(c['C07']['pending_after_restart']) == 1)
    check('C08_historico_indisponivel_incerto', c['C08']['unavailable'] == {'status': 'UNCERTAIN', 'reason': 'ARCHIVE_UNAVAILABLE'} and c['C08']['retry']['status'] == 'ARCHIVED')
    trials = c['C09']['trials']
    check('C09_tres_fronteiras', [t['last_boundary'] for t in trials] == ['PREPARED', 'COMMIT_ACK', 'COMMIT_DISPATCHED'] and trials[0]['classification'] == 'NOT_CONFIRMED' and trials[1]['classification'] == 'CONFIRMED' and trials[2]['caller_status'] == 'ECONOMIC_UNCERTAIN' and not trials[2]['ack_consumed_by_caller'] and trials[2]['reconciliation']['status'] == 'CONFIRMED')
    stops = [e for e in events if e['kind'] == 'DB_STOP_DISPATCHED']
    check('dois_immediate_tres_fast', [e['mode'] for e in stops] == ['immediate', 'immediate', 'fast', 'fast', 'fast'])
    log = (EV / 'postgres.log').read_text(encoding='utf8')
    recoveries = {}
    for key in ('C03', 'C04', 'C06-clean', 'C07-clean'):
        r = load(EV / f'recovery-{key}.json')
        recoveries[key] = r['kind']
        # JSON preserva CRLF misturado com LF; read_text normaliza o arquivo.
        # Normalização só em memória, sem alterar nenhum byte histórico.
        excerpt = r['log'].replace('\r\n', '\n')
        check('recovery_' + key, excerpt in log and r['kind'] == ('CRASH_RECOVERY' if key in ('C03', 'C04') else 'CLEAN_RESTART') and Path(r['identity']['data']).resolve() == (RUN / 'data').resolve() and r['identity']['port'] == '55435')
    check('WAL_diagnosticos_preservados_sem_PANIC_FATAL', 'unexpected pageaddr 0/5D6000' in log and 'invalid record length at 0/E15BA0: expected at least 24, got 0' in log and 'PANIC:' not in log and 'FATAL:' not in log)
    profiles = list(EV.glob('profile-*.json'))
    check('cinco_perfis_duraveis_logged', len(profiles) == 5 and all(all(load(p)['settings'][k]['setting'] == 'on' for k in ('fsync', 'synchronous_commit', 'full_page_writes')) and all(r['persistence'] == 'p' for r in load(p)['relations']) for p in profiles))
    final = load(EV / 'final-integrity.json'); control = load(EV / 'controls.json'); shutdown = load(EV / 'shutdown.json')
    check('integridade_global_11', all(final[k] == 11 for k in ('receipts', 'operations', 'grants', 'economic_revision')) and all(final[k] == 0 for k in ('orphan_operations', 'orphan_grants', 'orphan_heads', 'pending_transactions')))
    check('autoridade_CAS_privilegios_D33_nao_testado', control['stale_authority_sqlstate'] == 'P0003' and control['checkpoint']['status'] == 'CONFLICT' and control['direct_read_denied'] == control['cross_owner_denied'] == '42501' and not any(control['flags'].values()) and control['D33'] == 'NOT_TESTED')
    captures = [e for e in events if e['kind'] == 'RELAY_STDERR']
    check('30_capturas_completas', len(captures) == 30 and all(e['capture_complete'] and not e['truncated'] and not e['read_error'] for e in captures))
    check('descarte_historico_e_ausencia_atual_dos_diretorios', shutdown['stopped'] and shutdown['all_registered_processes_absent'] and shutdown['discarded'] == ['data', 'secrets'] and not shutdown['listeners'] and not shutdown['cleanup_errors'] and all(not (RUN / x).exists() for x in ('data', 'secrets')))
    check('quotas_historicas', results['elapsed_s'] <= 1200 and results['workers_created'] <= 32 and results['relay_instances'] == 2 and results['crashes'] == 2 and results['restarts'] == 4 and results['unique_intentions'] <= 64 and max(results['attempts'].values()) <= 3 and shutdown['peak_bytes'] <= 256 * 1024**2 and all(r['duration_s'] <= 120 for r in results['results']))
    check('arvore_historica_intacta_durante_revisao', inventory == {p.relative_to(ROOT).as_posix(): sha(p) for p in RUN.rglob('*') if p.is_file()})
    report = dict(data_utc=datetime.now(timezone.utc).isoformat(), metodo='LEITURA_E_CALCULO_ESTATICO_SEM_HARNESS', verificacoes=checks, total=len(checks), falhas=sum(not c['passou'] for c in checks), manifestos={k: len(v) for k, v in maps.items()}, divergencias_hash=differences, snapshot_errors=snapshot_errors, eventos_por_tipo=dict(Counter(e['kind'] for e in events)), recoveries=recoveries, inventario_historico=inventory, limites='Sem observação ao vivo; sem teste novo de PostgreSQL, D33, backup, restore ou failover.')
    OUT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    print(json.dumps({k: report[k] for k in ('total', 'falhas', 'manifestos')}, ensure_ascii=False))
    for item in checks:
        if not item['passou']:
            print('DIVERGENCIA:', item['verificacao'])
    raise SystemExit(bool(report['falhas']))


if __name__ == '__main__':
    main()
