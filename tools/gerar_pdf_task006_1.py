"""Gera o relatório da colisão seletiva usando apenas a biblioteca padrão."""

from pathlib import Path
from gerar_pdf_task002 import gerar_pdf


if __name__ == "__main__":
    docs = Path(__file__).resolve().parents[1] / "docs"
    gerar_pdf(docs / "TASK-006-1.md", docs / "Spirit_Gate_TASK-006-1.pdf", "TASK-006.1")
