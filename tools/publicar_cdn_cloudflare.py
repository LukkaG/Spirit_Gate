"""Envia somente o plano auditado ao R2, preservando a ordem de ativacao do canal."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import urllib.request

from preparar_cdn import base_url, execute_plan, safe_name, MUTABLE_CACHE
from preparar_ativacao_cdn import validate_activation_zip

ROOT = Path(__file__).resolve().parents[1]
WRANGLER = ROOT / ".task-checkpoints/cloudflare-tools/node_modules/wrangler/bin/wrangler.js"


def cloudflare_environment():
    env = os.environ.copy()
    env["XDG_CONFIG_HOME"] = str(ROOT / ".task-checkpoints/cloudflare-auth")
    env["WRANGLER_LOG_PATH"] = str(ROOT / ".task-checkpoints/cloudflare-tools/logs")
    env["WRANGLER_SEND_METRICS"] = "false"
    return env


class WranglerUploader:
    def __init__(self, bucket, evidence, resume=False):
        if not re.fullmatch(r"[a-z0-9][a-z0-9-]{1,61}[a-z0-9]", bucket):
            raise ValueError("Nome de bucket invalido")
        if not WRANGLER.is_file() or not shutil.which("node"):
            raise RuntimeError("Instale o Wrangler local antes de publicar")
        self.bucket, self.evidence = bucket, evidence
        self.progress = evidence / "upload-progress.json"
        self.done = {}
        if resume and self.progress.exists():
            previous = json.loads(self.progress.read_text(encoding="utf-8"))
            if previous["bucket"] != bucket:
                raise ValueError("Progresso pertence a outro bucket")
            self.done = previous["objects"]
        self.count = 0

    def __call__(self, path, metadata):
        key = safe_name(metadata["key"])
        data = path.read_bytes()
        if len(data) != metadata["bytes"] or hashlib.sha256(data).hexdigest() != metadata["sha256"]:
            raise ValueError("Arquivo mudou depois da auditoria")
        # Repetir os ponteiros mutaveis e seguro; apenas o payload imutavel pode
        # aproveitar um envio confirmado deste mesmo plano/bucket.
        if metadata["immutable"] and self.done.get(key) == metadata["sha256"]:
            return
        command = [shutil.which("node"), str(WRANGLER), "r2", "object", "put", self.bucket + "/" + key,
                   "--file", str(path), "--remote", "--cache-control", metadata["cacheControl"],
                   "--content-type", metadata["contentType"]]
        result = subprocess.run(command, cwd=ROOT / "deploy/cdn-cloudflare", env=cloudflare_environment(),
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                text=True, encoding="utf-8", errors="replace", timeout=180)
        with (self.evidence / "upload.log").open("a", encoding="utf-8") as log:
            log.write(key + "\n" + result.stdout + "\n")
        if result.returncode:
            raise RuntimeError("Falha no envio de " + key + "; consulte upload.log")
        self.done[key] = metadata["sha256"]
        temporary = self.progress.with_suffix(".tmp")
        temporary.write_text(json.dumps({"bucket": self.bucket, "objects": self.done}, indent=2), encoding="utf-8")
        temporary.replace(self.progress)
        self.count += 1
        if self.count % 10 == 0 or not metadata["immutable"]:
            print(f"Enviados {self.count} objetos; fase {metadata['phase']}: {key}", flush=True)


def activation_metadata(plan, archive):
    audit = validate_activation_zip(archive, plan["feedUrl"])
    checksum = (audit["sha256"] + "\n").encode("ascii")
    return [dict(key="SpiritGate-Ativar-CDN.zip", bytes=audit["bytes"], sha256=audit["sha256"],
                 immutable=False, cacheControl=MUTABLE_CACHE, contentType="application/zip", phase=4),
            dict(key="SpiritGate-Ativar-CDN.zip.sha256", bytes=len(checksum), sha256=hashlib.sha256(checksum).hexdigest(),
                 immutable=False, cacheControl=MUTABLE_CACHE, contentType="text/plain", phase=4)]


def prepare_activation_uploads(destination, archive):
    archive = Path(archive)
    plan = json.loads((destination / "deployment-plan.json").read_text(encoding="utf-8"))
    metadata = activation_metadata(plan, archive)
    data = archive.read_bytes()
    if len(data) != metadata[0]["bytes"] or hashlib.sha256(data).hexdigest() != metadata[0]["sha256"]:
        raise ValueError("Ativador mudou depois da auditoria")
    folder = destination / "activation-upload"
    folder.mkdir(exist_ok=True)
    (folder / metadata[0]["key"]).write_bytes(data)
    (folder / metadata[1]["key"]).write_text(metadata[0]["sha256"] + "\n", encoding="ascii", newline="\n")
    return [(folder / item["key"], item) for item in metadata]


def verify_remote(destination, activation_zip=None):
    plan = json.loads((destination / "deployment-plan.json").read_text(encoding="utf-8"))
    base_url(plan["baseUrl"])
    checks = []
    items = [next(item for item in plan["uploads"] if item["key"] == key)
             for key in ("latest.json", "SpiritGate-Anfitriao-Windows64.zip", "SpiritGate-Convidado-Windows64.zip")]
    if activation_zip is not None:
        items.extend(activation_metadata(plan, activation_zip))
    for item in items:
        key = item["key"]
        request = urllib.request.Request(plan["baseUrl"] + "/" + key, headers={"Cache-Control": "no-cache"})
        with urllib.request.urlopen(request, timeout=90) as response:
            if response.url != request.full_url or "no-store" not in response.headers.get("Cache-Control", ""):
                raise ValueError("Endpoint ou politica de cache divergente")
            content = response.read(item["bytes"] + 1)
        if len(content) != item["bytes"] or hashlib.sha256(content).hexdigest() != item["sha256"]:
            raise ValueError("Conteudo remoto divergente: " + key)
        checks.append(key)
    result = {"verified": checks, "baseUrl": plan["baseUrl"], "version": plan["version"]}
    (destination / "online-verification.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
    return result


def publish(destination, uploader, activation_zip=None):
    # O kit e validado antes de qualquer callback capaz de escrever no R2.
    activation = prepare_activation_uploads(destination, activation_zip) if activation_zip is not None else []
    result = execute_plan(destination, uploader)
    receipt = {"uploaded": True, "onlineVerified": False, "version": result["version"], "baseUrl": result["baseUrl"]}
    if activation:
        receipt["activationUploaded"] = False
    (destination / "publication.json").write_text(json.dumps(receipt, indent=2), encoding="utf-8")
    for path, metadata in activation:
        uploader(path, metadata)
    if activation:
        receipt["activationUploaded"] = True
        (destination / "publication.json").write_text(json.dumps(receipt, indent=2), encoding="utf-8")
    verification = verify_remote(destination, activation_zip)
    receipt.update(published=True, onlineVerified=True, verification=verification)
    (destination / "publication.json").write_text(json.dumps(receipt, indent=2), encoding="utf-8")
    return verification


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("prepared", type=Path)
    parser.add_argument("--bucket", default="spiritgate-updates")
    parser.add_argument("--publish", action="store_true", help="Efetua as escritas no R2")
    parser.add_argument("--resume", action="store_true")
    parser.add_argument("--verify-only", action="store_true")
    parser.add_argument("--activation-zip", type=Path, help="Kit de ativacao auditado, vinculado ao mesmo canal")
    args = parser.parse_args()
    folder = args.prepared.resolve()
    activation = args.activation_zip.resolve() if args.activation_zip else None
    if args.verify_only:
        print(json.dumps(verify_remote(folder, activation), indent=2))
    elif args.publish:
        print(json.dumps(publish(folder, WranglerUploader(args.bucket, folder, args.resume), activation), indent=2))
    else:
        if activation is not None:
            content = json.loads((folder / "deployment-plan.json").read_text(encoding="utf-8"))
            activation_metadata(content, activation)
        plan = execute_plan(folder, lambda path, metadata: None)
        print(json.dumps({"validated": True, "writesExecuted": False, "version": plan["version"]}, indent=2))
