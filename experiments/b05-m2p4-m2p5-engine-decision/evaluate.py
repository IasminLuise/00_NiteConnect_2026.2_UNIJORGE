#!/usr/bin/env python3
"""Offline analysis of acquired B05-M2P4/M2P5 evidence."""
from __future__ import annotations

import argparse
from pathlib import Path

from b05_decision.analysis import analyze, write


def main() -> int:
    parser = argparse.ArgumentParser(description="B05-M2P4/M2P5: diagnóstico e decisão consultiva")
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--out-dir", type=Path, required=True)
    args = parser.parse_args()
    result = analyze(args.manifest)
    locations = write(result, args.out_dir)
    print("HOLD_QUARANTINE / canPromote=false / officialApproved=false")
    print("M2P5:", result["m2p5"]["recommendation"])
    print("\n".join(str(path) for path in locations))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
