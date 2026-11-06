#!/usr/bin/env python3
"""Resolve Maven coordinates from Gradle's production tree and query OSV. Fail closed."""
import argparse,json,re,urllib.request
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('tree',type=Path);a=p.parse_args()
packages=set()
for line in a.tree.read_text().splitlines():
 m=re.search(r'([A-Za-z0-9_.-]+):([A-Za-z0-9_.-]+):([^\s]+)(?: -> ([^\s]+))?',line)
 if not m:continue
 group,name,version,selected=m.groups()
 if selected:
  if ':' in selected:
   group,name,version=selected.split(':',2)
  else:version=selected
 if not re.fullmatch(r'[0-9][A-Za-z0-9_.+-]*',version):continue
 packages.add((group+':'+name,version))
if not packages:raise SystemExit('No production dependencies found')
queries=[{'package':{'name':name,'ecosystem':'Maven'},'version':version} for name,version in sorted(packages)]
request=urllib.request.Request('https://api.osv.dev/v1/querybatch',data=json.dumps({'queries':queries}).encode(),headers={'Content-Type':'application/json'})
results=json.load(urllib.request.urlopen(request,timeout=90))['results']
report=[]
for query,result in zip(queries,results):
 for vulnerability in result.get('vulns',[]):report.append({'package':query['package']['name'],'version':query['version'],'id':vulnerability['id']})
Path('dependency-audit.json').write_text(json.dumps({'dependencies':queries,'findings':report},indent=2))
print(f'{len(queries)} production dependencies, {len(report)} advisory matches; dependency-audit.json')
# A reviewed exception must identify a specific dependency/version/advisory and explain reachability.
exceptions_path=Path(__file__).with_name('dependency-exceptions.json')
exceptions=json.loads(exceptions_path.read_text()) if exceptions_path.exists() else []
approved={(x['package'],x['version'],x['id']) for x in exceptions if x.get('reason') and x.get('reviewed_commit')}
unresolved=[x for x in report if (x['package'],x['version'],x['id']) not in approved]
if unresolved:raise SystemExit(f'{len(unresolved)} dependency advisories require remediation or documented reachability review')
