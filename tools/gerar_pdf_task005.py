"""Exporta a implementação dos efeitos temporários sem dependências externas."""

from pathlib import Path
from gerar_pdf_task002 import gerar_pdf


if __name__ == "__main__":
    docs = Path(__file__).resolve().parents[1] / "docs"
    gerar_pdf(docs / "TASK-005.md", docs / "Spirit_Gate_TASK-005.pdf", "TASK-005")
