#!/usr/bin/env python3
import argparse,hashlib,json,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--name',required=True);p.add_argument('--code',required=True,type=int);p.add_argument('--output',type=Path,default=Path('dist'));a=p.parse_args()
out=a.output;out.mkdir(parents=True,exist_ok=True)
source=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
modules=subprocess.check_output(['git','submodule','status','--recursive'],text=True)
for flavor in ['beta','stable']:
 package='io.github.uroge.smarttube.'+flavor
 meta={'packageName':package,'versionCode':a.code,'sha256':{}}
 for abi in ['armeabi-v7a','arm64-v8a','x86']:
  matches=list(Path(f'smarttubetv/build/outputs/apk/st{flavor}/release').glob(f'*_{abi}.apk'))
  if len(matches)!=1:raise SystemExit(f'Expected exactly one {flavor}/{abi} APK')
  inspected=subprocess.run(['python3',str(Path(__file__).with_name('inspect-apk.py')),'--signed','--package',package,'--version-code',str(a.code),'--version-name',a.name,'--abi',abi,str(matches[0])],text=True,capture_output=True,check=True)
  json.loads(inspected.stdout.splitlines()[-1])
  name=f'smarttube_{flavor}_{abi}.apk';shutil.copy2(matches[0],out/name)
  meta['sha256'][abi]=hashlib.file_digest((out/name).open('rb'),'sha256').hexdigest()
  key='downloadUrlList' if abi=='armeabi-v7a' else 'downloadUrlList_'+abi
  meta[key]=[f'https://github.com/uroge/SmartTube/releases/download/v{a.name}/{name}']
 (out/f'smarttube_{flavor}2.json').write_text(json.dumps({'package':meta,a.name:{'versionCode':a.code,'changelog':['Security-maintained uroge build']}},indent=2))
(out/'provenance.json').write_text(json.dumps({'commit':source,'submodules':modules,'versionName':a.name,'versionCode':a.code},indent=2))
(out/'SHA256SUMS').write_text(''.join(f'{hashlib.file_digest(f.open("rb"),"sha256").hexdigest()}  {f.name}\n' for f in sorted(out.glob('*.apk'))))
