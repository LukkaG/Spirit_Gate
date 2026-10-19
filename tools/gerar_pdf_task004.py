"""Exporta a documentação da Criostase sem dependências externas."""

from pathlib import Path
from gerar_pdf_task002 import gerar_pdf


if __name__ == "__main__":
    docs = Path(__file__).resolve().parents[1] / "docs"
    gerar_pdf(docs / "TASK-004.md", docs / "Spirit_Gate_TASK-004.pdf", "TASK-004")
