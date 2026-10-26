from __future__ import annotations
import argparse
import json
from pathlib import Path
from b05_acceptance.core import assess


def main():
    ap = argparse.ArgumentParser(description='B05-M2P8/M2P9, exige DOCX oficiais e evidências do ONLYOFFICE real')
    ap.add_argument('--manifest', type=Path, required=True)
    ap.add_argument('--out', type=Path, required=True)
    args = ap.parse_args()
    out = args.out.resolve()
    manifest_dir = args.manifest.resolve().parent
    if out.exists() or not out.parent.is_dir() or not out.is_relative_to(manifest_dir):
        ap.error('Saída deve ser um novo arquivo dentro da campanha, sem sobrescrever')
    result = assess(args.manifest)
    out.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print('Decisão:', result['recommendation'])
    print('Casos:', len(result['cases']), '| Riscos:', len(result['risk_flags']), '| Pendências:', len(result['pending']))
    print('Gates: bloqueados; relatório consultivo.')

if __name__ == '__main__':
    main()
