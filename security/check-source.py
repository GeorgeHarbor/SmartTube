#!/usr/bin/env python3
"""Focused regression checks; runtime tests remain mandatory."""
from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]
errors=[]
roots=['common/src/main','smarttubetv/src/main','SharedModules/sharedutils/src/main','SharedModules/appupdatechecker2/src/main','MediaServiceCore/youtubeapi/src/main']
patterns={
 'external QR service':r'api\.qrserver\.com|toQrCodeLink',
 'password bypass':r'SUPER_PASSWD|equalsIgnoreCase\("smarttube"\)',
 'insecure hostname verifier':r'HostnameVerifier\s*\{[^}]*true|hostnameVerifier\([^;]*->\s*true',
 'certificate bypass':r'configureToIgnoreCertificate|trustAllCerts',
 'empty certificate validation':r'checkServerTrusted\s*\([^)]*\)[^{]*\{\s*\}',
}
for directory in roots:
 for f in (root/directory).rglob('*'):
  if f.suffix not in ('.java','.kt','.xml'):continue
  content=f.read_text()
  for label,pattern in patterns.items():
   if re.search(pattern,content,re.S):errors.append(f'{label}: {f.relative_to(root)}')
build=(root/'smarttubetv/build.gradle').read_text()
if re.search(r'firebase|crashlytics|google-services|new URL\(',build,re.I):errors.append('App build retains telemetry or a configuration-time network fetch')
for f in (root/'common/src').glob('st*/res/values/update_urls.xml'):
 for url in re.findall(r'<item>(.*?)</item>',f.read_text()):
  if not url.startswith('https://github.com/uroge/SmartTube/releases/download/'):errors.append(f'External update feed: {f}')
if 'minSdkVersion = 23' not in (root/'SharedModules/constants.gradle').read_text():errors.append('Minimum SDK changed')
if "../SharedModules').exists()" in (root/'settings.gradle').read_text():errors.append('Sibling module substitution restored')
for file in [root/'.gitmodules',root/'MediaServiceCore/.gitmodules']:
 for url in re.findall(r'url\s*=\s*(\S+)',file.read_text()):
  if not url.startswith('https://github.com/uroge/'):errors.append(f'External module origin: {file}')
if errors:raise SystemExit('\n'.join(errors))
print('Security source regression checks passed')
