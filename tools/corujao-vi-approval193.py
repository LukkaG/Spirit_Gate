"""Registra a decisão explícita sem reescrever o checkpoint da proposta."""
from pathlib import Path
import runpy, json, datetime
root = Path.cwd()
session = root / '.task-checkpoints/corujao-vi-session-20261025T221534Z-be629798'
h = runpy.run_path(str(session / 'session.py'))
adr = root / 'docs/ADR-193-PERSISTENCIA-SINTETICA.md'
h['begin']('193-approval', adr)
out = h['checkpoint']('193-approval')
decision = '''SPIRIT GATE — APROVAÇÃO FORMAL DO ADR-193
Decisão do responsável: ADR-193 APROVADO.
Autorizo a implementação sequencial das TASKs 194, 195 e 196, exclusivamente conforme docs/ADR-193-PERSISTENCIA-SINTETICA.md.
Laboratório sintético local e isolado; arquivos novos online-synthetic-*; Java 17, SyntheticOnlineRepository, codecs existentes e RpgWorld. UUID de backups, CAS/locks, retry de snapshots integralmente idênticos, handles/grants efêmeros e restart sintético autorizados.
Proibidos launcher/gameplay real, contas/autenticação pública, saves pessoais, bancos/dependências externas, protocolo/schema incompatível, Docker/Testcontainers, commit/push/release/publicação e tasks fora do escopo previamente autorizado.
Exigidos testes positivos/negativos, evidências reais, hashes, diff, relatórios, isolamento e regressões. Falhas de integridade/perda de dados/confirmação falsa/isolamento interrompem dependentes. Aprovação de implementação e teste, sem homologação antecipada.
Origem: resposta explícita do usuário à pergunta de aprovação do ADR-193 nesta sessão.
'''
(out / 'decision.txt').write_text(decision, encoding='utf-8')
h['write'](out / 'approval.json', dict(approved=True, tasks=[194,195,196], proposed_adr_sha256=h['sha'](adr), origin='explicit_user_reply', utc=datetime.datetime.now(datetime.timezone.utc).isoformat()))
text=adr.read_text(encoding='utf-8').replace('**PROPOSTA — AGUARDANDO APROVAÇÃO DO USUÁRIO**','**APROVADO PELO RESPONSÁVEL**')
text+='\n\n## Decisão formal registrada\n\nAprovação explícita recebida em resposta à pergunta sobre este ADR. Autoriza implementar e testar 194–196 nos limites acima; não homologa resultados antecipadamente. Decisão e hash da proposta preservados em `'+out.relative_to(root).as_posix()+'`.\n'
adr.write_text(text,encoding='utf-8')
report=root/'docs/TASK-193-RELATORIO-FINAL.md'
text=report.read_text(encoding='utf-8')
text=text.replace('**PARCIAL — decisão de ADR pendente**','**VALIDADA — ADR APROVADO; implementação subsequente sujeita a testes**')
text+='\n\nAtualização formal: responsável aprovou expressamente o ADR-193 e a implementação sintética das TASKs 194–196. O gate de decisão da TASK-193 está atendido. A caracterização e as lacunas históricas acima permanecem como evidência anterior à correção. Registro: `'+out.relative_to(root).as_posix()+'`.\n'
report.write_text(text,encoding='utf-8')
h['integrity'](out,'after')
h['write'](out/'after.json',dict(status='ADR APROVADO / TASK-193 VALIDADA',sources_unchanged=h['sources']()==h['read'](out/'sources-before.json'),adr_sha256=h['sha'](adr),report_sha256=h['sha'](report)))
with (session/'progress.jsonl').open('a',encoding='utf-8') as stream: stream.write(json.dumps(dict(task='193',status='VALIDADA - ADR APROVADO',evidence=str(out)))+'\n')
progress=root/'docs/CORUJAO-VI-PROGRESSO.md'
text=progress.read_text(encoding='utf-8').replace('PARCIAL; auditoria e ADR preparados','VALIDADA; ADR aprovado expressamente').replace('[decisão pendente]','[ADR aprovado]')
text+='\n\nDecisão posterior: ADR-193 aprovado formalmente pelo responsável; TASK-193 validada. Implementação 194–196 autorizada exclusivamente em laboratório sintético local, sujeita às provas técnicas e aos demais gates.\n'
progress.write_text(text,encoding='utf-8')
print(out)
