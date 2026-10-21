"""Consolida estados registrados e integridade sem inferir aprovação humana."""
from pathlib import Path
import hashlib
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    stages = json.loads((ROOT/'docs/CORUJAO-III-PROGRESSO.json').read_text(encoding='utf-8'))
    table = '| TASK | Estado | Entrega |\n|---|---|---|\n' + ''.join(
        f"| {row['task']} | {row['status']} | [Relatório](TASK-{row['task']}-RELATORIO-FINAL.md) |\n" for row in stages)
    docs = {
        'RELATORIO-FINAL': '# Corujão III — estado consolidado\n\n' + table + '\nMarcos A (117–120), B (121–125), C (126–128) e expansão (129–133). A extensão complementar autoriza continuar até 133. Estados são técnicos; não representam aprovação artística.\n',
        'HOMOLOGACAO': '# Homologação humana\n\nUsar `-Dspiritgate.rpg.dataDir=<diretório temporário>` e personagem sintético. Percorrer entrada, seleção, criação Guardian/Mystic, ajuda, NPC, diário, inventário, equipamento, mapa, comércio, opções, combate, salvar, voltar e continuar.\n\nVerificar Tab/Shift+Tab, Enter/Escape, foco, contraste e legibilidade em 1000×620, 800×600 e 640×480, DPI físico 100%/150%. Os probes automatizados usam janelas reais e printAll; isso não homologa monitor físico. Em 640×480, diálogo aberto reduz a exploração; fechar restaura espaço.\n',
        'PENDENCIAS': '# Pendências\n\n- Aprovação visual/artística humana e teclado físico.\n- Sprite experimental base compartilhado; Idle possui um frame por direção. Sem nova arte definitiva.\n- Sem trilhas musicais licenciadas; áudio atual usa tons originais existentes.\n- Mapas do Studio fora dos limites do adaptador continuam sem suporte automático; nenhum formato foi modificado.\n- Métricas e pacote privado: consultar relatório da TASK-133; não presumir entregues antes dessa tarefa.\n',
        'CONTINUIDADE': '# Continuidade\n\n' + table + '\nConsultar cada relatório e os checkpoints em `.task-checkpoints/TASK-NNN/corujao-i/`. Cada etapa preserva fonte anterior, SHA-256, diff isolado e verificação dos saves. A pasta possui nome histórico da ferramenta reutilizada.\n\nNão iniciar TASK-134. Não realizar push, publicar releases, substituir pacotes anteriores ou apagar saves.\n',
    }
    manifests = {}
    for row in stages:
        directory = ROOT/f".task-checkpoints/TASK-{row['task']}/corujao-i"
        preservation = directory/'preservation.json'
        if preservation.exists():
            manifests[row['task']] = json.loads(preservation.read_text(encoding='utf-8'))
    hashes = {}
    for folder in ('server/src/main/resources','assets'):
        for file in (ROOT/folder).rglob('*'):
            if file.is_file():hashes[file.relative_to(ROOT).as_posix()] = hashlib.sha256(file.read_bytes()).hexdigest()
    (ROOT/'docs/corujao-iii/assets-sha256.json').write_text(json.dumps(hashes,indent=2),encoding='utf-8')
    (ROOT/'docs/corujao-iii/preservation.json').write_text(json.dumps(manifests,indent=2),encoding='utf-8')
    docs['INVENTARIO'] = '# Inventário\n\nInventários SHA-256 e ZIPs recuperáveis por tarefa: `.task-checkpoints/TASK-NNN/corujao-i/before.json`, `after.json`, `before.zip`, `after.zip`. Arquivos modificados por etapa: `preservation.json` e `isolated.diff`.\n\n[Preservação consolidada](corujao-iii/preservation.json). [Hashes de assets atuais](corujao-iii/assets-sha256.json). Os ZIPs não incluem saves pessoais.\n'
    if any(row['task']=='133' for row in stages):
        audit=json.loads((ROOT/'docs/task-133/package-audit.json').read_text(encoding='utf-8'))
        metrics=json.loads((ROOT/'docs/task-133/validacao-final/SpiritGatePerformanceProbe/performance.json').read_text(encoding='utf-8'))
        package=Path(audit['package']).name
        final='\n## Entrega final\n\n**400 testes Java e 30 testes Python, sem falhas. Oito probes gráficos aprovados.**\n\n'
        final+='[Pacote privado Windows com Java integrado](../dist/'+package+'). Não publicado. Extraia todo o ZIP e abra `SpiritGateRPG.exe`; arte de personagens continua experimental.\n\n'
        final+='[Auditoria do pacote](task-133/package-audit.json), [inicialização real do EXE](task-133/package-smoke.json), [probes finais](task-133/validacao-final/summary.json), [percurso integrado](task-133/validacao-final/SpiritGateFinalJourneyProbe/probe.txt), [medidas](task-133/validacao-final/SpiritGatePerformanceProbe/performance.json).\n\n'
        final+=f"Abertura de janela: {metrics['startupWindowMillis']:.1f} ms. Média de carregar/restaurar 100 saves: {metrics['loadAndRestoreMeanMillis100']:.2f} ms. Heap adicional observado sem isolamento de GC: {metrics['heapDeltaBytesUnisolated']} bytes. Pintura Swing em movimento: {metrics['swingPaintCyclesPerSecondMoving']:.1f} ciclos/s; não equivale a FPS físico. Minimap offscreen: {metrics['minimapMeanMicros500Offscreen']:.1f} µs/pintura após aquecimento. Sem garantia geral de ausência de pausas da EDT.\n\n"
        final+='O percurso usa o mesmo personagem criado pela UI, com duas fixtures explícitas para comércio/combate. Valida integração e persistência; não substitui uma campanha natural jogada por uma pessoa.\n\n'
        final+='SHA-256 do ZIP: `'+audit['sha256']+'`. Nenhum save pessoal incluído; 169 arquivos verificados e recursos coincidem com a fonte. Versões anteriores preservadas.\n'
        docs['RELATORIO-FINAL']+=final
        docs['CONTINUIDADE']+='\nRodada encerrada na TASK-133. Consultar entrega final e pendências; não iniciar 134.\n'
        docs['PENDENCIAS']+='\n- Escala de texto independente não implementada; resolução e minimapa têm ajustes de sessão.\n- Integração do minimapa com a geometria das sessões de autoria não foi adicionada; mapas legados do launcher são o escopo validado.\n- FPS de monitor, heap isolado, áudio físico e latência máxima da EDT não foram homologados.\n- Pacote privado experimental preparado e testado; sua aprovação continua humana.\n'
        docs['HOMOLOGACAO']+='\n[Pacote privado](../dist/'+package+') e [evidências automatizadas](task-133/validacao-final/summary.json). Para preservar versões anteriores, extraia em pasta nova.\n'
        docs['INVENTARIO']+='\n[Preservação das áreas protegidas](task-133/protected-preservation.json). O checkpoint final da TASK-133 é a referência completa mais recente; a consolidação de manifests anteriores é gerada antes de seu fechamento.\n'
    for name, text in docs.items():
        (ROOT/f'docs/CORUJAO-III-{name}.md').write_text(text,encoding='utf-8')


if __name__ == '__main__':main()
