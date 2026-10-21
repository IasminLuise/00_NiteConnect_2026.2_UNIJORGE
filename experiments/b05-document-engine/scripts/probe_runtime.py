"""Local synthetic gateway/failure probes. Stops/restarts only this lab's CODE service."""
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError
import hashlib
import json
import re
import subprocess
from lab import COMPOSE, ROOT, compose_env
from wait_ready import wait_ready

HOST = 'http://localhost:18085'

def get(path):
    with urlopen(HOST + path, timeout=10) as response:
        return response.read()

def probe():
    document = next(d for d in json.loads(get('/lab/documents')) if d['name'] == 'F01.docx')
    current = json.loads(get('/lab/documents/' + document['id'] + '/current'))['version']
    path = f"/lab/documents/{document['id']}/versions/{current}"
    original = get(path)
    report = {'source_version': current, 'source_sha256': hashlib.sha256(original).hexdigest()}
    boundary='synthetic-b05-probe'
    body=('--'+boundary+'\r\nContent-Disposition: form-data; name="data"; filename="fixture.docx"\r\n\r\n').encode()+original+('\r\n--'+boundary+'--\r\n').encode()
    try:
        with urlopen(Request('http://localhost:19980/cool/convert-to/pdf',data=body,headers={'Content-Type':'multipart/form-data; boundary='+boundary}),timeout=10) as response:
            report['browser_conversion_status']=response.status
    except HTTPError as error:
        report['browser_conversion_status']=error.code
    assert report['browser_conversion_status']==403
    try:
        subprocess.run(COMPOSE+['stop','collabora'],check=True,capture_output=True,env=compose_env(),timeout=30)
        try:
            with urlopen(Request(HOST+path+'/pdf',data=b'',headers={'Origin':HOST}),timeout=55) as response:
                report['unavailable_conversion_status']=response.status
        except HTTPError as error:
            report['unavailable_conversion_status']=error.code
        assert report['unavailable_conversion_status']==503
        report['source_preserved']=hashlib.sha256(get(path)).hexdigest()==report['source_sha256']
        assert report['source_preserved']
    finally:
        subprocess.run(COMPOSE+['start','collabora'],check=True,capture_output=True,env=compose_env(),timeout=30)
    wait_ready()
    logs=subprocess.run(COMPOSE+['logs','--no-color'],capture_output=True,text=True,env=compose_env(),check=True).stdout
    report['raw_capability_patterns_in_logs']=len(re.findall(r'access_token(?:=|%3[Dd])[A-Za-z0-9_-]{43}',logs))
    (ROOT/'results'/'generated'/'runtime-probes.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,indent=2))
    assert report['raw_capability_patterns_in_logs']==0

if __name__=='__main__':
    probe()
