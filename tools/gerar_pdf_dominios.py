"""Gera o PDF das decisões de raças e domínios a partir do Markdown correspondente."""
from pathlib import Path
import re
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.pagesizes import A4
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak,
)

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'docs/SISTEMA-DE-RACAS-E-DOMINIOS.md'
OUTPUT = ROOT / 'docs/Spirit_Gate_Racas_e_Dominios.pdf'
INK = colors.HexColor('#18283B')
ACCENT = colors.HexColor('#167A87')


def generate():
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle(name='BodySG', fontName='Helvetica', fontSize=9.4,
                              leading=13.4, textColor=INK, spaceAfter=7))
    styles.add(ParagraphStyle(name='TitleSG', parent=styles['BodySG'],
                              fontName='Helvetica-Bold', fontSize=31, leading=36,
                              spaceAfter=13))
    styles.add(ParagraphStyle(name='HeadingSG', parent=styles['BodySG'],
                              fontName='Helvetica-Bold', fontSize=18, leading=23,
                              textColor=ACCENT, spaceAfter=13))
    styles.add(ParagraphStyle(name='SubheadingSG', parent=styles['BodySG'],
                              fontName='Helvetica-Bold', fontSize=11, leading=15,
                              spaceBefore=7, spaceAfter=6, keepWithNext=True))
    styles.add(ParagraphStyle(name='CellSG', parent=styles['BodySG'],
                              fontSize=8.6, leading=11.6, spaceAfter=0))
    styles.add(ParagraphStyle(name='HeaderCellSG', parent=styles['CellSG'],
                              fontName='Helvetica-Bold', textColor=colors.white))
    story = []
    lines = SOURCE.read_text(encoding='utf-8').splitlines()
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        i += 1
        if not line:
            continue
        if line == '---':
            story.append(PageBreak())
        elif line.startswith('|'):
            rows = [line]
            while i < len(lines) and lines[i].strip().startswith('|'):
                rows.append(lines[i].strip())
                i += 1
            cells = [[c.strip() for c in row.strip('|').split('|')] for row in rows]
            cells = [row for row in cells if not all(re.fullmatch(r':?-+:?', cell) for cell in row)]
            data = [[Paragraph(escape(cell), styles['HeaderCellSG' if n == 0 else 'CellSG'])
                     for cell in row] for n, row in enumerate(cells)]
            widths = [110, 389] if len(cells[0]) == 2 else [68, 210, 221]
            table = Table(data, colWidths=widths, repeatRows=1, hAlign='LEFT')
            table.setStyle(TableStyle([
                ('BACKGROUND', (0, 0), (-1, 0), INK),
                ('ROWBACKGROUNDS', (0, 1), (-1, -1),
                 [colors.HexColor('#EDF4F5'), colors.HexColor('#F7F9FB')]),
                ('VALIGN', (0, 0), (-1, -1), 'TOP'),
                ('LEFTPADDING', (0, 0), (-1, -1), 8),
                ('RIGHTPADDING', (0, 0), (-1, -1), 8),
                ('TOPPADDING', (0, 0), (-1, -1), 7),
                ('BOTTOMPADDING', (0, 0), (-1, -1), 7),
            ]))
            story.extend([table, Spacer(1, 9)])
        elif line.startswith('# '):
            story.append(Paragraph(escape(line[2:]), styles['TitleSG']))
        elif line.startswith('## '):
            story.append(Paragraph(escape(line[3:]), styles['HeadingSG']))
        elif line.startswith('### '):
            story.append(Paragraph(escape(line[4:]), styles['SubheadingSG']))
        else:
            story.append(Paragraph(escape(line), styles['BodySG']))

    def decorate(canvas, doc):
        canvas.saveState()
        canvas.setStrokeColor(ACCENT)
        canvas.setLineWidth(2)
        canvas.line(48, 806, 547, 806)
        canvas.setFont('Helvetica', 8)
        canvas.setFillColor(INK)
        canvas.drawString(48, 819, 'SPIRIT GATE  /  DOCUMENTO DE DESIGN')
        canvas.drawString(48, 27, 'Raças, estilos e domínios • v1.1 • 21/10/2026')
        canvas.drawRightString(547, 27, str(doc.page))
        canvas.restoreState()

    doc = SimpleDocTemplate(str(OUTPUT), pagesize=A4, rightMargin=48,
                            leftMargin=48, topMargin=52, bottomMargin=48,
                            title='Spirit Gate — Raças, estilos de combate e domínios',
                            author='Spirit Gate', subject='Decisões de design — versão 1.1')
    doc.build(story, onFirstPage=decorate, onLaterPages=decorate)
    print(f'PDF gerado: {OUTPUT} ({OUTPUT.stat().st_size} bytes)')


if __name__ == '__main__':
    generate()
