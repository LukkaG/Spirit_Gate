"""Relatório completo e PDF pesquisável do Corujão VII, sem dependências novas."""
from pathlib import Path
import hashlib
import json
import re
import textwrap
import zlib
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
EVIDENCE = ROOT / ".task-checkpoints/TASK-212/corujao-vii"


def report():
    preservation = json.loads((EVIDENCE / "preservation-final.json").read_text(encoding="utf-8"))
    integration = json.loads((ROOT / "dist/SpiritGate-CorujaoVII-0.212.0-private/integracao.json").read_text(encoding="utf-8"))
    added = json.loads((EVIDENCE / "new-source-files.json").read_text(encoding="utf-8"))
    text = """# Spirit Gate — Corujão VII
## Relatório completo de implementação e validação local

Data de fechamento: 31/10/2026 (America/Sao_Paulo).
Escopo: TASK-201 a TASK-212. Projeto-K, Java 17, Swing/Java2D, TCP autoritativo.
Branch: beco. HEAD: 6d72c6595dee6dbe4dea832fe6047ee81f51d4a3.

## 1. Resultado da entrega

As doze tarefas do plano têm implementação integrada nos fontes locais e
validação técnica registrada. Foram gerados dois pacotes privados novos,
Anfitrião e Convidado, versão 0.212.0-private. A distribuição oficial 0.201.0
permaneceu intacta. Não houve commit, push, release ou deploy.

Criação e catálogo de sprites já implementados nas TASKs 201/202 foram
preservados. A autorização posterior do usuário ampliou a execução para todas
as tarefas e exigiu este PDF. As restrições históricas de iniciar apenas uma
tarefa foram superadas; as regras de integridade e autoridade foram mantidas.

A conclusão é técnica e local. Revisão visual humana e homologação entre dois
computadores/Radmin físico permanecem pendentes. Não se declara arte completa,
conta real, persistência online ou teste físico que não foi executado.

## 2. Funcionalidades disponíveis

201 — criação e seleção: nome validado, raça explícita, prévia, acessórios
compatíveis, identidade de sessão independente do nome e reconexão explícita.
Fluxo de janelas conserva o servidor do anfitrião enquanto a partida está viva.

202 — catálogo por raça: Human e Reaper com Idle/Walk em quatro direções,
cadência, escala e pivôs. Aetheri/Spirit usam Human provisório identificado.
Reaper ausente bloqueia entrada e usa fallback geométrico no mundo.

203 — combate visual: início, golpe, impacto, hit, recuperação e derrota
derivados do servidor; efeitos geométricos não causam dano. Idle/Walk continuam
no atlas. Eventos/snapshots repetidos não reaplicam efeitos de gameplay.

204 — HUD: barras reais de HP/energia, dano flutuante, alvo confirmado,
recuperação e estado de derrota; atualização Swing na EDT sem carregar imagens.

205 — IA: cada inimigo seleciona seu alvo vivo da região por distância/ID,
respeita obstáculos, alcance, perseguição, retorno, morte e respawn do motor.

206 — loot: recompensa individual por contribuição para jogadores vivos da
região. Tabela instalada, +3 XP e +5 moedas por mob; ledger por spawn/geração,
política de recompensas desabilitadas e coleta de pendências preservados.

207 — inventário: janela I, itens/quantidades privados, equipar/desequipar,
consumir e recolher pendentes. O servidor valida sequência, posse, slot,
categoria, nível, cooldown e transições. O cliente não altera itens localmente.

208 — progressão: XP, nível, próximo limiar, ataque e defesa publicados pelo
servidor; feedback de ganho e subida de nível. Sem comando de XP arbitrário.

209 — habilidades: hotbar selecionável, ataque racial, poção, Shunpo ofensivo
e defensivo com energia/cooldown reais. Domínio respeita raça, nível 15 e escolha
única; Shunpo exige nível 20. Descrições de técnicas não são novas habilidades.

210 — missão: conversar com a Anciã, aceitar missão individual, contribuir na
morte de slime, acompanhar objetivo, voltar ao NPC e receber uma poção uma vez.

211 — mundo: bosque/clareira leste, lobo adicional, vila/mercado/Nilo, árvores,
caminhos e segunda passagem recíproca. Duas regiões 960x640, documentos World
Studio, arte existente, spawns/portais e navegação verificados.

212 — integração: regressões, prova gráfica com duas janelas, integridade,
dois pacotes privados e smoke com seus runtimes/JARs, documentação e este PDF.

## 3. Arquitetura e compatibilidade

O servidor continua responsável por movimento, colisão, combate, IA, HP,
energia, morte, loot, inventário, XP, domínio, missão e transições. A interface
somente apresenta snapshots e envia intenções limitadas.

V1–V5 conservam seus frames. V6 é uma capacidade opt-in: opcode 17 transporta
hash da revisão, estado de combate e ficha privada; opcode 18 transporta
sequência, operação e chave. Sem caminhos, quantidade, atributos ou ID alheio
enviados pelo cliente. Limite de frame permanece 4096 bytes. Coleções, strings,
enums e slots são limitados. Comandos carregam hash da revisão atual.

LocalClient.character mantém V5. O fluxo Radmin atual usa LocalClient.gameplay.
Servidor antigo recusa com orientação de atualização; não há downgrade oculto.
Reconexão cria nova sessão efêmera. A ficha não é formato de save.

LauncherSession.start conserva o baseline histórico. startExpanded é usado no
novo fluxo e só cria o mundo novo se não existir revisão publicada. Publicações
existentes não são sobrescritas. A regra histórica impede clientes sem suporte
de conteúdo de entrar numa revisão nova que não conseguem sincronizar.

O código reutiliza InventoryService, LootService, RpgProgression,
DomainProgression, QuestService, AuthoredQuestSession, EnemyService, LiveRevision
e World Studio. Não foram criados novos bancos, autenticação ou contas reais.

## 4. Validação final comprovada

Java completo: 252 fontes e 221 arquivos de testes compilados; 689 executados,
668 aprovados em headless, 21 abortados por exigir GUI, zero falhas/skips.
Complemento Swing: 29 execuções aprovadas, cobrindo os 21 abortos e repetindo
oito casos. Não somar repetições como novos testes únicos.

Python: 97 testes aprovados, processo final com código 0.
Atualizador PowerShell: 39 verificações aprovadas.
Transporte GitHub PowerShell: 21 verificações aprovadas, HTTPS simulado.
Nenhuma publicação ou download de produção foi necessário.

Provas TCP reais locais: criação/reconexão; raças Human/Reaper; dano/HP; loot;
inventário privado; XP único; Shunpo e domínio; missão inteira; portais; revisão,
rollback, admissão histórica e segurança. Mocks complementam, não substituem,
essas provas. O teste adicional V6 cobre replay de equipamento/poção e transição.

Prova gráfica: duas janelas reais com conteúdo/raças, hotbar, diário e barras;
abertura do inventário. Capturas foram inspecionadas. Os mapas foram renderizados
com contornos de colisão e passaram no validador nativo, sem diagnósticos,
com zero amostras isoladas na análise de navegação.

Pacotes: manifests conferidos por tamanho e SHA-256, 391 arquivos por papel.
Smoke em três processos usando runtime/bin/java.exe e o JAR dos pacotes:
servidor, cliente Reaper e cliente Human; todos saíram 0. Clientes recebem
duas aparências e ficha V6. Convidado impedido de hospedar. Não foi executado
separadamente o EXE nativo nem um teste físico em duas máquinas.

## 5. Falhas encontradas e correções

O teste 203 encontrou hit residual após respawn; o estado foi limpo.
Compilações intermediárias dos testes 208/209/210 encontraram sobrecarga com
null e variáveis capturadas não finais; corrigidas antes das provas finais.

A primeira suite completa encontrou 11 falhas após trocar o baseline da
fábrica histórica. O coordenador classificava corretamente a expansão como
revisão nova e recusava peers antigos. Separar start/startExpanded preservou
o contrato histórico. Admissão, publicação, rollback e segurança foram repetidos
com sucesso; a suite inteira final passou. Nenhum teste foi desativado.

O primeiro comando Python via shell registrou “97 OK”, mas o PowerShell tratou
stderr de um teste negativo como falha de shell. Captura subprocess unificada
confirmou processo 0. O smoke inicial usou nome de JAR não normalizado no probe;
ajustado para app/spirit-gate-rpg.jar, o smoke final aprovou os dois pacotes.

O primeiro diagnóstico da skill de mapas apontou stdout cp1252; reexecutado
com PYTHONUTF8=1. Permanecem avisos de múltiplos Python e ferramentas opcionais
de mídia ausentes (ffmpeg/resvg/APIs/Grok). Não foram necessárias para reutilizar
arte instalada e os resultados originais foram preservados no checkpoint.

## 6. Preservação e rastreabilidade
"""
    text += f"\nBaseline: {preservation['baselineFiles']} arquivos; {len(preservation['changed'])} fontes existentes alteradas; nenhum ausente.\n"
    text += f"{preservation['protectedFiles']} arquivos de assets/saves/distribuição protegidos sem alteração.\n"
    text += f"{len(added)} arquivos novos em server/src: oito fontes e onze testes.\n"
    text += """
Fontes permaneceram estáveis durante os testes finais. A árvore Git já tinha
mudanças e exclusões anteriores: não houve reset, clean ou restauração dessas
exclusões. Três capturas históricas regeneradas pela suite foram movidas para
evidências, mantendo a limpeza anterior. Assets originais não foram regravados.

Checkpoint inicial: .task-checkpoints/CORUJAO-VII-REMAINING/.
Checkpoints por etapa: TASK-203 a TASK-212/corujao-vii/.
Complementos: TASK-212-GUI e TASK-212-INTEGRITY.
Patch incremental e lista de novos arquivos no checkpoint TASK-212.

Fontes existentes modificadas nesta autorização:
"""
    text += "\n".join("- " + n.replace("\\", "/") for n in preservation["changed"]) + "\n\nNovos arquivos de fonte/teste:\n"
    text += "\n".join("- " + n.replace("\\", "/") for n in added)
    text += """

Ferramentas adicionadas: corujao7_verify.py, CorujaoSevenPackageProbe.java,
smoke_corujao7_package.py e gerar_relatorio_corujao7.py. Documentação: três
arquivos por tarefa, plano, progresso, pendências, continuidade e consolidado.

## 7. Pacotes e como executar

Diretório: dist/SpiritGate-CorujaoVII-0.212.0-private/.
Extraia o ZIP do papel em uma pasta nova. Abra SpiritGate-Launcher.exe na raiz
do pacote. Anfitrião hospeda; convidado usa o endereço Radmin do anfitrião.
Feed remoto desativado: os pacotes não anunciam atualização oficial publicada.
Mantenha o oficial anterior em sua pasta. Não copie saves para versions.

Controles: WASD mover; SPACE atacar; I inventário; 1 usar ação selecionada;
SHIFT Shunpo ofensivo; Q defensivo; F conversar; E aceitar missão; C entregar;
P portal próximo; R retornar após morte; F2 diagnóstico. Use as opções de zoom.
O nível inicial normal é 1; os testes de habilidade usaram nível 20 no fixture
do servidor, sem oferecer comando de XP ao jogador.

Para executar os fontes: rodar-prototipo.cmd e opções Radmin do Portal.
Jogos offline e Coop conservam seus caminhos anteriores.

Hashes da entrega:
"""
    text += "\nManifest comum: " + integration["manifestSha256"] + "\n"
    for package in integration["packages"]:
        text += "\n" + package["role"] + ": " + Path(package["archive"]).name + "\nSHA-256: " + package["sha256"] + "\n"
    text += """

## 8. Gates e limites finais

Gate A: criação, raça e renderização verificados localmente.
Gate B: combate, HUD e IA integrados à autoridade do servidor.
Gate C: loot, inventário, progressão e habilidades com proteção contra replay.
Gate D: uma missão instalada e exploração/portais das duas regiões verificados.
Gate E: regressões, pacotes privados, evidências, documentação e PDF entregues.

Os gates acima representam validação técnica local. Não substituem aceite
artístico, legibilidade em todos os monitores ou homologação física Radmin.

Limites: Aetheri/Spirit sem atlas próprio; Reaper sem acessórios novos;
Run/Attack/Hit/Death sem quadros próprios; técnicas de domínio ainda descritivas;
uma missão instalada; duas regiões; IA usa colisão/movimento existentes, sem A*;
inventário de 20 slots e pendências finitas do motor; sem comércio online novo.
No teto técnico de pendências, novas recompensas podem ser recusadas pelo
LootService; itens anteriores são preservados. Sem economia global persistente.

Personagem, inventário, XP e missão pertencem à sessão experimental. Desconexão
e reconexão não salvam esse progresso como conta online. Não houve migração
de saves pessoais, contas públicas, TLS ou banco externo.

Próxima ação do responsável: revisar as capturas/experiência e organizar a
prova em dois computadores. Bugs vistos nessa homologação devem gerar evidência
nova, sem reclassificar os testes locais como teste físico.

## 9. Índice das evidências principais

TASK-212/corujao-vii/java-full-final.log — suite Java inteira.
TASK-212-GUI/corujao-vii/verification-gui.json — nove classes Swing.
TASK-212/corujao-vii/python-tests-clean.log — 97 testes Python.
TASK-212/corujao-vii/updater/results.json — 39 verificações.
TASK-212/corujao-vii/github-transport/results.json — 21 verificações.
TASK-212/corujao-vii/package-build/build-evidence.json — build e origem.
TASK-212/corujao-vii/package-smoke-final/result.json — processos empacotados.
TASK-212/corujao-vii/preservation-final.json — integridade e fontes estáveis.
TASK-212/corujao-vii/corujao7-source.patch — mudança contra o início desta rodada.
TASK-211/corujao-vii/*.sgmap e *-navigation.png — documentos e colisões.
TASK-212/corujao-vii/ui/*.png — duas janelas e inventário.

## 10. Apêndices por tarefa

Os documentos das TASKs 201/202 preservam suas datas, decisões e números
históricos. As frases “próxima tarefa não iniciada” nesses registros descrevem
aquele encerramento, não o estado final deste PDF. O resultado consolidado
atual é o das seções anteriores. Não somar rodadas repetidas como testes únicos.
"""
    for task in range(201, 213):
        for suffix in ("", "-TESTES"):
            path = DOCS / f"TASK-{task}{suffix}.md"
            text += "\n\n# Apêndice — " + path.name + "\n\n" + path.read_text(encoding="utf-8")
    return text


def pdf_text(value):
    value = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", value).replace("`", "").replace("**", "")
    for old, new in {"→": "->", "–": "-", "—": "-", "≥": ">=", "≤": "<=", "•": "-", "×": "x", "−": "-"}.items():
        value = value.replace(old, new)
    return value.encode("cp1252", errors="replace").replace(b"\\", b"\\\\").replace(b"(", b"\\(").replace(b")", b"\\)")


def write_pdf(text, destination, captures=None, cycle="VII", date="31/10/2026"):
    objects = [b"", b"", b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>",
               b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"]
    pages = []
    commands = []
    y = 780

    def page():
        nonlocal commands, y
        if not commands:
            return
        number = len(pages) + 1
        commands.append(b"BT /F1 8 Tf 0.35 0.4 0.45 rg 42 28 Td (Spirit Gate | Corujao " + pdf_text(cycle) + b" | " + pdf_text(date) + b" | " + str(number).encode() + b") Tj ET")
        stream = b"\n".join(commands)
        stream_id = len(objects) + 1
        objects.append(b"<< /Length " + str(len(stream)).encode() + b" >>\nstream\n" + stream + b"\nendstream")
        page_id = len(objects) + 1
        objects.append(f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents {stream_id} 0 R >>".encode())
        pages.append(page_id)
        commands, y = [], 780

    for line in text.splitlines():
        heading = line.startswith("#")
        size = 15 if line.startswith("# ") else 12 if heading else 9.5
        font = "F2" if heading else "F1"
        content = line.lstrip("# ") if heading else line
        if heading and y < 130:
            page()
        if heading:
            y -= 10
        for wrapped in textwrap.wrap(content, width=64 if heading else 99, break_long_words=True, break_on_hyphens=False) or [""]:
            if y < 55:
                page()
            color = "0.06 0.25 0.34" if heading else "0.08 0.11 0.15"
            commands.append(f"BT /{font} {size} Tf {color} rg 42 {y:.1f} Td (".encode() + pdf_text(wrapped) + b") Tj ET")
            y -= 19 if heading else 12.5
    page()
    if captures is None:
        captures = [(EVIDENCE / "ui/client-0.png", "Cliente Reaper: janela real conectada ao servidor TCP local"),
                (EVIDENCE / "ui/client-1.png", "Cliente Human: mesmo mundo e ficha privada V6"),
                (EVIDENCE / "ui/inventory.png", "Inventario de sessao: quantidades e operacoes autoritativas"),
                (ROOT / ".task-checkpoints/TASK-211/corujao-vii/forest-navigation.png", "Bosque: arte existente e contornos de colisao em vermelho"),
                (ROOT / ".task-checkpoints/TASK-211/corujao-vii/village-navigation.png", "Vila/mercado: portais, edificacao e arvore com colisao explicita")]
    for source, caption in captures:
        with Image.open(source) as raw:
            image = raw.convert("RGB")
            data = zlib.compress(image.tobytes())
            w, h = image.size
        image_id = len(objects) + 1
        objects.append(f"<< /Type /XObject /Subtype /Image /Width {w} /Height {h} /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length {len(data)} >>\nstream\n".encode() + data + b"\nendstream")
        dw = 510
        dh = dw * h / w
        stream = b"BT /F2 12 Tf 42 780 Td (" + pdf_text(caption) + b") Tj ET\n"
        stream += f"q {dw} 0 0 {dh:.2f} 42 {745-dh:.2f} cm /Capture Do Q\n".encode()
        stream += b"BT /F1 9 Tf 42 50 Td (Captura automatizada inspecionada; nao representa aceite artistico humano.) Tj ET"
        stream_id = len(objects) + 1
        objects.append(f"<< /Length {len(stream)} >>\nstream\n".encode() + stream + b"\nendstream")
        page_id = len(objects) + 1
        objects.append(f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> /XObject << /Capture {image_id} 0 R >> >> /Contents {stream_id} 0 R >>".encode())
        pages.append(page_id)
    objects[0] = b"<< /Type /Catalog /Pages 2 0 R >>"
    objects[1] = f"<< /Type /Pages /Count {len(pages)} /Kids [{' '.join(str(p)+' 0 R' for p in pages)}] >>".encode()
    data = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
    offsets = [0]
    for i, obj in enumerate(objects, 1):
        offsets.append(len(data))
        data += f"{i} 0 obj\n".encode() + obj + b"\nendobj\n"
    xref = len(data)
    data += f"xref\n0 {len(offsets)}\n0000000000 65535 f \n".encode()
    for offset in offsets[1:]:
        data += f"{offset:010d} 00000 n \n".encode()
    data += f"trailer\n<< /Size {len(offsets)} /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n".encode()
    destination.write_bytes(data)
    # Confere offsets e árvore de páginas sem depender de um visualizador externo.
    assert data[xref:xref+4] == b"xref"
    for i, offset in enumerate(offsets[1:], 1):
        assert data[offset:].startswith(f"{i} 0 obj".encode())
    return {"pages": len(pages), "images": len(captures), "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()}


if __name__ == "__main__":
    content = report()
    (DOCS / "CORUJAO-VII-RELATORIO-FINAL.md").write_text(content, encoding="utf-8")
    result = write_pdf(content, DOCS / "Spirit_Gate_Corujao_VII_Relatorio_Completo.pdf")
    (EVIDENCE / "pdf-verification.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(json.dumps(result, indent=2))
