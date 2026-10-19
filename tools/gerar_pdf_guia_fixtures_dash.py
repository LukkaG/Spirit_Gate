"""Gera o guia de fixtures em PDF, preservando os exemplos Java sem dependências."""

from pathlib import Path
import re
import textwrap


PAGE_WIDTH = 595
PAGE_HEIGHT = 842
MARGIN = 42
TOP = 795
BOTTOM = 58
CODE_COLUMNS = 100


def _pdf_text(text):
    """Escapa texto português para as fontes padrão com codificação WinAnsi."""
    replacements = {"→": "->", "←": "<-", "≤": "<=", "≥": ">="}
    for original, replacement in replacements.items():
        text = text.replace(original, replacement)
    return (text.encode("cp1252", errors="strict").replace(b"\\", b"\\\\")
            .replace(b"(", b"\\(").replace(b")", b"\\)"))


def _lines(markdown):
    """Interpreta títulos e cercas Markdown sem remover a indentação do código."""
    code = False
    result = []
    for number, raw in enumerate(markdown.splitlines(), 1):
        if raw.startswith("```"):
            code = not code
            result.append(("gap", ""))
            continue
        if code:
            line = raw.expandtabs(4)
            if len(line) > CODE_COLUMNS:
                raise ValueError(
                    f"Linha {number}: código com {len(line)} colunas; "
                    f"divida explicitamente em linhas de até {CODE_COLUMNS} colunas."
                )
            result.append(("code", line))
            continue
        if not raw.strip():
            result.append(("gap", ""))
            continue
        heading = re.match(r"^(#{1,3})\s+(.+)$", raw)
        kind = "h" + str(len(heading.group(1))) if heading else "text"
        text = heading.group(2) if heading else raw
        text = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", text)
        text = text.replace("**", "").replace("`", "")
        width = 62 if kind == "h1" else 76 if kind == "h2" else 90
        indent = "  " if text.startswith(("- ", "* ")) else ""
        for line in textwrap.wrap(text, width=width, subsequent_indent=indent):
            result.append((kind, line))
    if code:
        raise ValueError("A fonte Markdown terminou com uma cerca de código aberta.")
    return result


def gerar_pdf(origem, destino):
    """Converte o guia em páginas A4 com títulos, código monoespaçado e paginação."""
    source = Path(origem)
    target = Path(destino)
    styles = {
        "h1": ("F2", 17, 24),
        "h2": ("F2", 13, 20),
        "h3": ("F2", 11, 17),
        "text": ("F1", 10, 14),
        "code": ("F3", 8.5, 11),
        "gap": ("F1", 10, 7),
    }
    pages = [[]]
    y = TOP
    for kind, line in _lines(source.read_text(encoding="utf-8")):
        font, size, height = styles[kind]
        needed = height + (35 if kind.startswith("h") else 0)
        if y - needed < BOTTOM:
            pages.append([])
            y = TOP
        if kind == "gap" and not pages[-1]:
            continue
        pages[-1].append((kind, line, font, size, height, y))
        y -= height

    objects = [
        b"<< /Type /Catalog /Pages 2 0 R >>",
        b"",
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>",
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>",
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>",
    ]
    children = []
    for number, page in enumerate(pages, 1):
        page_id = len(objects) + 1
        stream_id = page_id + 1
        children.append(f"{page_id} 0 R")
        objects.append(
            (f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 {PAGE_WIDTH} {PAGE_HEIGHT}] "
             f"/Resources << /Font << /F1 3 0 R /F2 4 0 R /F3 5 0 R >> >> "
             f"/Contents {stream_id} 0 R >>").encode("ascii")
        )
        commands = []
        for kind, line, font, size, height, baseline in page:
            if kind == "gap":
                continue
            if kind == "code":
                commands.append(
                    f"q 0.96 g {MARGIN - 3} {baseline - 3} "
                    f"{PAGE_WIDTH - 2 * MARGIN + 6} {height} re f Q".encode("ascii")
                )
            color = "0.10 0.20 0.32 rg" if kind.startswith("h") else "0 0 0 rg"
            commands.append(
                f"{color} BT /{font} {size} Tf 1 0 0 1 {MARGIN} {baseline} Tm (".encode("ascii")
                + _pdf_text(line) + b") Tj ET"
            )
        commands.append(f"0.75 G {MARGIN} 44 m {PAGE_WIDTH - MARGIN} 44 l S".encode("ascii"))
        footer = f"Spirit Gate | Guia de fixtures do dash | {number}/{len(pages)}"
        commands.append(
            f"0.35 g BT /F1 8 Tf 1 0 0 1 {MARGIN} 29 Tm (".encode("ascii")
            + _pdf_text(footer) + b") Tj ET"
        )
        stream = b"\n".join(commands)
        objects.append(f"<< /Length {len(stream)} >>\nstream\n".encode("ascii")
                       + stream + b"\nendstream")
    objects[1] = (f"<< /Type /Pages /Kids [{' '.join(children)}] "
                  f"/Count {len(pages)} >>").encode("ascii")
    output = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
    offsets = [0]
    for number, obj in enumerate(objects, 1):
        offsets.append(len(output))
        output.extend(f"{number} 0 obj\n".encode("ascii") + obj + b"\nendobj\n")
    xref = len(output)
    output.extend(f"xref\n0 {len(offsets)}\n0000000000 65535 f \n".encode("ascii"))
    for offset in offsets[1:]:
        output.extend(f"{offset:010d} 00000 n \n".encode("ascii"))
    output.extend(
        (f"trailer\n<< /Size {len(offsets)} /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n")
        .encode("ascii")
    )
    target.write_bytes(output)
    print(f"PDF gerado: {target.name} ({len(pages)} páginas)")


if __name__ == "__main__":
    docs = Path(__file__).resolve().parents[1] / "docs"
    gerar_pdf(docs / "GUIA-FIXTURES-DASH.md", docs / "Spirit_Gate_Guia_Fixtures_Dash.pdf")
