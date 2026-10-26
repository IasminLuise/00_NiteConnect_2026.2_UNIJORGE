"""Offline B05-M2P6/M2P7 evaluator. No changes to production gates."""
from __future__ import annotations

import argparse
from pathlib import Path

from b05_qualification.decision import evaluate, write


def main() -> None:
    parser = argparse.ArgumentParser(description="Two investigations, one qualification")
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--out-dir", type=Path, required=True)
    args = parser.parse_args()
    if args.out_dir.resolve().exists():
        parser.error("Diretório de saída existe, não será sobrescrito")
    report = evaluate(args.manifest)
    outputs = write(report, args.out_dir)
    for output in outputs:
        print(output)
    print("M2P7:", report["m2p7"]["recommendation"])
    print("B05: HOLD_QUARANTINE; nenhuma promoção autorizada")


if __name__ == "__main__":
    main()
