#!/usr/bin/env python3
"""CLI entry point (stdlib for DOCX; PyMuPDF for PDF)."""
import argparse
import sys
from pathlib import Path

from b05_m2p3.runner import analyze_campaign, write_reports


def main(argv=None):
    parser = argparse.ArgumentParser(description="B05-M2P3: analisar quatro operações independentes, sem promover documento")
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--out-dir", type=Path, required=True)
    parser.add_argument("--dpi", type=int, default=144)
    args = parser.parse_args(argv)
    try:
        if args.out_dir.resolve() == args.manifest.resolve().parent:
            raise ValueError("Saída deve ficar em diretório novo, distinto do manifesto")
        report = analyze_campaign(args.manifest, dpi=args.dpi)
        outputs = write_reports(report, args.out_dir)
    except (ValueError, OSError, RuntimeError, KeyError, TypeError) as exc:
        print(f"B05-M2P3: análise interrompida: {exc}", file=sys.stderr)
        return 2
    print("B05-M2P3: HOLD_QUARANTINE; sem promoção/merge/deploy")
    for path in outputs:
        print(path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
