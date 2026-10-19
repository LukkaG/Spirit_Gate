"""Exporta as decisões e os resultados das pendências técnicas, sem dependências."""

from pathlib import Path
from gerar_pdf_task002 import gerar_pdf


if __name__ == "__main__":
    docs = Path(__file__).resolve().parents[1] / "docs"
    gerar_pdf(docs / "PENDENCIAS-IMPLEMENTADAS.md",
              docs / "Spirit_Gate_Pendencias_Tecnicas.pdf", "Pendências técnicas")
