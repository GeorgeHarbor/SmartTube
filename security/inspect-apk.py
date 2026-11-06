#!/usr/bin/env python3
import argparse,hashlib,json,os,re,subprocess,zipfile
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('apk',type=Path);p.add_argument('--signed',action='store_true');p.add_argument('--package');p.add_argument('--version-code',type=int);p.add_argument('--version-name');p.add_argument('--abi');a=p.parse_args()
sdk=Path(os.environ['ANDROID_HOME']);tools=sdk/'build-tools/30.0.3'
badging=subprocess.check_output([str(tools/'aapt'),'dump','badging',str(a.apk)],text=True)
package=re.search(r"package: name='([^']+)' versionCode='([0-9]+)'",badging)
assert package and package[1] in {'io.github.uroge.smarttube.'+x for x in ['beta','stable','fdroid']},'Unexpected package'
version_name=re.search(r"versionName='([^']+)'",badging)[1]
if a.package:assert package[1]==a.package,'Wrong release flavor'
if a.version_code is not None:assert int(package[2])==a.version_code,'Manifest/APK version code mismatch'
if a.version_name:assert version_name==a.version_name,'Manifest/APK version name mismatch'
assert "sdkVersion:'23'" in badging,'Unexpected minimum API' 
manifest=subprocess.check_output([str(tools/'aapt'),'dump','xmltree',str(a.apk),'AndroidManifest.xml'],text=True)
assert not re.search(r'android:debuggable[^\n]*0xffffffff',manifest),'Debuggable release'
assert re.search(r'android:allowBackup[^\n]*0x0',manifest),'Automatic backup enabled'
assert re.search(r'android:usesCleartextTraffic[^\n]*0x0',manifest),'Cleartext enabled'
with zipfile.ZipFile(a.apk) as archive:
 abis=sorted({name.split('/')[1] for name in archive.namelist() if name.startswith('lib/') and name.endswith('.so')})
 if a.abi:assert abis==[a.abi],'Wrong APK architecture'
 for name in archive.namelist():
  assert not name.endswith(('.jks','.keystore','.p12')), 'Signing material in APK'
  if name.endswith('.dex'):
   data=archive.read(name)
   assert b'Lcom/google/firebase/' not in data,'Firebase bundled'
   assert b'api.qrserver.com' not in data,'Remote QR endpoint bundled'
if a.signed:subprocess.run([str(tools/'apksigner'),'verify','--verbose','--print-certs',str(a.apk)],check=True)
print(json.dumps({'apk':a.apk.name,'packageName':package[1],'versionCode':int(package[2]),'versionName':version_name,'abis':abis,'sha256':hashlib.file_digest(a.apk.open('rb'),'sha256').hexdigest()}))
