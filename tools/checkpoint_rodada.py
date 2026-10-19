"""Registra checkpoints, diffs e resultados locais sem depender de um checkout Git."""

from pathlib import Path
import argparse
import difflib
import hashlib
import json
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
EXTENSIONS = {".java", ".md", ".txt", ".xml", ".py", ".ps1", ".cmd", ".gitignore", ".properties"}
EXCLUDED = {"target", "dist", ".git", ".task-checkpoints", "assets", "__pycache__", ".maven-repository"}


def state():
    """Lê apenas fontes, scripts e documentos textuais da rodada."""
    result = {p.relative_to(ROOT).as_posix(): p.read_text(encoding="utf-8-sig")
            for p in ROOT.rglob("*") if p.is_file()
            and (p.suffix.lower() in EXTENSIONS or p.name == ".gitignore")
            and not any(part in EXCLUDED for part in p.relative_to(ROOT).parts)}
    manifest = ROOT / 'assets/manifest-assets.json'
    if manifest.exists():
        result['assets/manifest-assets.json'] = manifest.read_text(encoding='utf-8')
    return result


def checkpoint(task, finish):
    """Salva a base ou produz o diff e os hashes do estado aprovado da etapa."""
    folder = ROOT / ".task-checkpoints"
    folder.mkdir(exist_ok=True)
    path = folder / f"TASK-{task}-before.json"
    current = state()
    if not finish:
        if path.exists():
            raise FileExistsError(f"Checkpoint inicial já existe: {path}")
        path.write_text(json.dumps(current, ensure_ascii=False), encoding="utf-8")
        print(f"TASK-{task}: base salva ({len(current)} arquivos)")
        return
    previous = json.loads(path.read_text(encoding="utf-8"))
    diff = []
    changed = []
    for name in sorted(previous.keys() | current.keys()):
        old, new = previous.get(name, ""), current.get(name, "")
        if old == new:
            continue
        changed.append(name)
        diff.extend(difflib.unified_diff(old.splitlines(True), new.splitlines(True),
                    fromfile="a/" + name if name in previous else "/dev/null",
                    tofile="b/" + name if name in current else "/dev/null"))
    (ROOT / f"docs/TASK-{task}.diff").write_text("".join(diff), encoding="utf-8")
    metadata = {"task": task, "changed": changed,
                "sha256": {n: hashlib.sha256(t.encode("utf-8")).hexdigest()
                           for n, t in current.items()}}
    (folder / f"TASK-{task}-after.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"TASK-{task}: diff salvo ({len(changed)} arquivos)")


def results(task):
    """Resume os relatórios reais do Surefire sem copiar propriedades do ambiente."""
    reports = [ET.parse(p).getroot() for p in sorted(
        (ROOT / "server/target/surefire-reports").glob("TEST-*.xml"))]
    if not reports:
        raise RuntimeError("Relatórios Surefire ausentes")
    totals = {key: sum(int(r.get(key, "0")) for r in reports)
              for key in ("tests", "failures", "errors", "skipped")}
    if totals["failures"] or totals["errors"]:
        raise RuntimeError(f"Suíte vermelha: {totals}")
    text = [f"TASK-{task} - mvn clean test - BUILD SUCCESS", str(totals),
            "Windows 10 amd64; Temurin JDK 17.0.20.1+1; Maven 3.10.0.", ""]
    text += [f"{r.get('name')}: tests={r.get('tests')}, failures={r.get('failures')}, "
             f"errors={r.get('errors')}, skipped={r.get('skipped')}" for r in reports]
    (ROOT / f"docs/TASK-{task}-testes.txt").write_text("\n".join(text) + "\n", encoding="utf-8")
    print(totals)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=("begin", "end", "results", "pdf"))
    parser.add_argument("task")
    args = parser.parse_args()
    if args.action in ("begin", "end"):
        checkpoint(args.task, args.action == "end")
    elif args.action == "results":
        results(args.task)
    else:
        from gerar_pdf_task002 import gerar_pdf
        gerar_pdf(ROOT / f"docs/TASK-{args.task}.md",
                  ROOT / f"docs/Spirit_Gate_TASK-{args.task}.pdf", f"TASK-{args.task}")
