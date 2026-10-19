"""Gera o relatório PDF da TASK-002 usando apenas a biblioteca padrão do Python."""

from pathlib import Path
import textwrap


def gerar_pdf(origem: Path, destino: Path, tarefa="TASK-002"):
    """Converte o relatório textual em páginas A4 com fonte PDF padrão."""
    linhas = []
    for linha in origem.read_text(encoding="utf-8").splitlines():
        linhas.extend(textwrap.wrap(linha, width=88) or [""])
    paginas = [linhas[i:i + 49] for i in range(0, len(linhas), 49)]
    objetos = [b"", b"", b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"]
    filhos = []
    for numero, pagina in enumerate(paginas, 1):
        pagina_id = len(objetos) + 1
        fluxo_id = pagina_id + 1
        filhos.append(f"{pagina_id} 0 R")
        objetos.append((f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                        f"/Resources << /Font << /F1 3 0 R >> >> /Contents {fluxo_id} 0 R >>").encode())
        comandos = [b"BT /F1 10 Tf 14 TL 42 795 Td"]
        for linha in pagina + ["", f"Spirit Gate - {tarefa} - página {numero}/{len(paginas)}"]:
            texto = linha.encode("cp1252", errors="strict").replace(b"\\", b"\\\\").replace(b"(", b"\\(").replace(b")", b"\\)")
            comandos.append(b"(" + texto + b") Tj T*")
        comandos.append(b"ET")
        fluxo = b"\n".join(comandos)
        objetos.append(f"<< /Length {len(fluxo)} >>\nstream\n".encode() + fluxo + b"\nendstream")
    objetos[0] = b"<< /Type /Catalog /Pages 2 0 R >>"
    objetos[1] = f"<< /Type /Pages /Kids [{' '.join(filhos)}] /Count {len(paginas)} >>".encode()
    resultado = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
    offsets = [0]
    for indice, objeto in enumerate(objetos, 1):
        offsets.append(len(resultado))
        resultado.extend(f"{indice} 0 obj\n".encode() + objeto + b"\nendobj\n")
    xref = len(resultado)
    resultado.extend(f"xref\n0 {len(offsets)}\n0000000000 65535 f \n".encode())
    for offset in offsets[1:]:
        resultado.extend(f"{offset:010d} 00000 n \n".encode())
    resultado.extend((f"trailer\n<< /Size {len(offsets)} /Root 1 0 R >>\n"
                      f"startxref\n{xref}\n%%EOF\n").encode())
    destino.write_bytes(resultado)
    print(f"PDF gerado: {destino.name} ({len(paginas)} páginas)")


if __name__ == "__main__":
    docs = Path(__file__).resolve().parents[1] / "docs"
    gerar_pdf(docs / "TASK-002.md", docs / "Spirit_Gate_TASK-002.pdf")
