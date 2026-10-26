"""Build both updater bootstraps from one validated host ZIP; never publishes."""
import argparse
import hashlib
import json
from pathlib import Path
from preparar_atualizacao import prepare


def integrate(package, destination, version, feed_url=""):
    destination.mkdir(parents=True, exist_ok=False)
    results = []
    manifests = []
    for role, label in (("anfitriao", "Anfitriao"), ("convidado", "Convidado")):
        folder = destination / label
        archive = prepare(package, folder, version, feed_url, role)
        manifests.append((folder / "canal/latest.json").read_bytes())
        results.append(dict(role=role, archive=str(archive.resolve()),
                            bootstrap=str((folder / "SpiritGate-Atualizavel").resolve()),
                            sha256=hashlib.sha256(archive.read_bytes()).hexdigest()))
    if manifests[0] != manifests[1]:
        raise RuntimeError("Host and guest must share a single game manifest")
    metadata = dict(version=version, sourcePackage=str(package.resolve()),
                    sourceSha256=hashlib.sha256(package.read_bytes()).hexdigest(),
                    commonChannel=str((destination / "Anfitriao/canal").resolve()),
                    manifestSha256=hashlib.sha256(manifests[0]).hexdigest(),
                    remoteUpdatesConfigured=bool(feed_url), published=False, packages=results)
    (destination / "integracao.json").write_text(json.dumps(metadata, indent=2), encoding="utf-8")
    return metadata


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("package", type=Path)
    parser.add_argument("destination", type=Path)
    parser.add_argument("--version", required=True)
    parser.add_argument("--feed-url", default="")
    args = parser.parse_args()
    print(json.dumps(integrate(args.package, args.destination, args.version, args.feed_url), indent=2))
