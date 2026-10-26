"""Build an isolated local image from pinned vendor source and the reviewed native change."""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess


def build(repo: Path, destination: Path, image: str, no_cache: bool = False):
    here=Path(__file__).resolve().parent
    lock=json.loads((here/'source.lock.json').read_text(encoding='utf-8'))
    repo=repo.resolve(strict=True);destination=destination.resolve()
    if '.private' not in repo.parts or '.private' not in destination.parts:
        raise ValueError('Private source and build directories required')
    if not re.fullmatch(r'nite/onlyoffice-savedate:[a-z0-9._-]+',image):
        raise ValueError('Use a dedicated nite/onlyoffice-savedate local image tag')
    if subprocess.check_output(['git','rev-parse','HEAD'],cwd=repo,text=True).strip()!=lock['revision']:
        raise ValueError('Unsupported source revision')
    destination.mkdir(parents=True,exist_ok=False)
    archive=destination/'original-source.tar'
    with archive.open('xb') as stream:
        subprocess.run(['git','archive','--format=tar','--prefix=sdkjs/',lock['revision']],cwd=repo,stdout=stream,check=True)
    if hashlib.sha256(archive.read_bytes()).hexdigest()!=lock['source_archive_sha256']:
        raise ValueError('Source archive integrity mismatch')
    for file in ['prepare_source.py','source.lock.json','Dockerfile.native']:
        shutil.copyfile(here/file,destination/file)
    (destination/'native').mkdir();shutil.copyfile(here/'native/savedate.js',destination/'native/savedate.js')
    args=['docker','build','--provenance=false','--build-arg','BASE_IMAGE='+lock['base_image'],
          '--build-arg','SOURCE_DATE_EPOCH='+str(lock['source_date_epoch']),'-f',str(destination/'Dockerfile.native'),'-t',image]
    if no_cache:args.append('--no-cache')
    args.append(str(destination))
    with (destination/'build-private.log').open('wb') as stream:
        subprocess.run(args,stdout=stream,stderr=subprocess.STDOUT,check=True)
    observed=json.loads(subprocess.check_output(['docker','image','inspect',image],text=True))[0]
    result={'image':image,'image_id':observed['Id'],'upstream_revision':lock['revision'],
            'base_image':lock['base_image'],'source_archive_verified':True,'source_date_epoch':lock['source_date_epoch']}
    (destination/'build-result.json').write_text(json.dumps(result,indent=2)+'\n')
    return result


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--sdk-source',required=True,type=Path);parser.add_argument('--out',required=True,type=Path)
    parser.add_argument('--image',required=True);parser.add_argument('--no-cache',action='store_true')
    args=parser.parse_args();print(json.dumps(build(args.sdk_source,args.out,args.image,args.no_cache),indent=2))
