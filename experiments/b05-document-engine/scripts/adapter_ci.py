"""Synthetic adapter-to-Java-gate smoke. No editor, Word or official documents."""
import os
import subprocess
import tempfile
from pathlib import Path

from adapter_cases import savedate_source
from docx_adapter import normalize_copy, digest, verify_idempotence
from fidelity import ROOT


def main():
    source = savedate_source()
    adapted, record = normalize_copy(source, {'original_sha256': digest(source),
        'template_version': 'source-free-ci-v1', 'fields': [
            {'part': 'word/header1.xml', 'instruction': 'SAVEDATE \\* MERGEFORMAT'}]})
    if not verify_idempotence(adapted, record):
        raise AssertionError('Idempotence failed')
    target = ROOT / 'wopi-host/target'
    classpath = os.pathsep.join(str(target / name) for name in ('test-classes', 'classes'))
    (ROOT / '.runtime').mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='adapter-ci-', dir=ROOT / '.runtime') as name:
        directory = Path(name)
        fixture = directory / 'adapted.docx'
        fixture.write_bytes(adapted)
        subprocess.run(['java', '-cp', classpath, 'br.edu.unijorge.experiments.AdapterGateSmoke',
                        str(fixture), str(directory / 'data')], check=True)


if __name__ == '__main__':
    main()
