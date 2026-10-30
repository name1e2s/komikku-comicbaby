"""Generate the Komikku store using the upstream protobuf schema and build metadata."""
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET

import cairosvg
from google.protobuf import json_format
import index_pb2

SIGNATURE = 'd1b8287cb0f1e80b99a1ae60e38734bd5ebbdf62469bf665ad6eddb939ce115e'
SOURCE_ID = 4184193314158057816


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--framework', type=Path, required=True)
    parser.add_argument('--apksigner', type=Path, required=True)
    parser.add_argument('--repository', default='name1e2s/komikku-comicbaby')
    parser.add_argument('--output', type=Path, default=Path(__file__).resolve().parents[1] / 'store')
    args = parser.parse_args()
    if not re.fullmatch(r'[\w.-]+/[\w.-]+', args.repository):
        parser.error('repository must be owner/name')
    module = args.framework / 'src/zh/comicbaby'
    info = json.loads((module / 'build/keiyoushi-source-info.json').read_text(encoding='utf-8'))
    assert info['packageName'] == 'eu.kanade.tachiyomi.extension.zh.comicbaby'
    assert info['sources'][0]['id'] == SOURCE_ID
    version = info['versionName']
    assert re.fullmatch(r'\d+(?:\.\d+)+', version)
    apk = module / f'build/outputs/apk/release/tachiyomi-zh.comicbaby-v{version}.apk'
    jar = module / f'build/outputs/jar/release/tachiyomi-zh.comicbaby-v{version}.jar'
    signature = subprocess.check_output([str(args.apksigner), 'verify', '--print-certs', str(apk)], text=True)
    if f'Signer #1 certificate SHA-256 digest: {SIGNATURE}' not in signature:
        raise ValueError('APK signature changed: cannot publish as an update of this store')
    if not jar.is_file():
        raise FileNotFoundError(jar)
    out = args.output
    for directory in ('apk', 'jar', 'icon'):
        (out / directory).mkdir(parents=True, exist_ok=True)
    apk_path = f'apk/comicbaby-v{version}.apk'
    jar_path = f'jar/comicbaby-v{version}.jar'
    icon_path = 'icon/eu.kanade.tachiyomi.extension.zh.comicbaby.png'
    shutil.copy2(apk, out / apk_path)
    shutil.copy2(jar, out / jar_path)
    # Convert the source's existing Android vector paths to SVG, then PNG.
    vector = ET.parse(module / 'res/mipmap-anydpi/ic_launcher.xml').getroot()
    ns = '{http://schemas.android.com/apk/res/android}'
    svg = ET.Element('svg', xmlns='http://www.w3.org/2000/svg', viewBox='0 0 32 32')
    for path in vector.findall('path'):
        ET.SubElement(svg, 'path', fill=path.attrib[ns + 'fillColor'], d=path.attrib[ns + 'pathData'])
    cairosvg.svg2png(bytestring=ET.tostring(svg), write_to=str(out / icon_path), output_width=192, output_height=192)
    base = f'https://raw.githubusercontent.com/{args.repository}/main/store'
    index = index_pb2.Index(
        name='木瓜漫画 ComicBaby', badgeLabel='木瓜', signingKey=SIGNATURE,
        contact=index_pb2.Contact(website=f'https://github.com/{args.repository}'),
        extensionList=index_pb2.ExtensionList(extensions=[index_pb2.Extension(
            name=info['name'], packageName=info['packageName'],
            resources=index_pb2.Resources(apkUrl=f'{base}/{apk_path}', iconUrl=f'{base}/{icon_path}', jarUrl=f'{base}/{jar_path}'),
            extensionLib=info['extensionLib'], versionCode=info['versionCode'], versionName=version,
            contentWarning=info['contentWarning'],
            sources=[index_pb2.Source(id=s['id'], name=s['name'], language=s['lang'], homeUrl=s['baseUrl'], mirrorUrls=s.get('mirrorUrls', [])) for s in info['sources']],
        )]),
    )
    (out / 'index.pb').write_bytes(gzip.compress(index.SerializeToString(deterministic=True), mtime=0))
    (out / 'index.json').write_text(json_format.MessageToJson(index, preserving_proto_field_name=True) + '\n', encoding='utf-8', newline='\n')
    # Komikku's legacy entry point uses the wire name index_v2.
    (out / 'repo.json').write_text(json.dumps({'meta': {'name': index.name, 'shortName': index.badgeLabel, 'website': index.contact.website, 'signingKeyFingerprint': SIGNATURE}, 'index_v2': f'{base}/index.pb'}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')
    shutil.copy2(Path(__file__).resolve().parents[1] / 'LICENSE', out / 'LICENSE')
    (out / 'README.md').write_text(f'''# 木瓜漫画 · Komikku 插件商店

提供 ComicBaby {version} 的安装和更新，含网站“喜欢这部的人也在看”推荐。

## 添加商店

需要 Komikku 1.14.0 或更新版本（支持扩展 API 1.6 和新版商店索引）。

在「更多 → 设置 → 浏览 → 插件商店（扩展商店）」中添加：

```text
{base}/index.pb
```

返回「浏览 → 扩展」刷新，启用中文及成人内容显示，搜索 `ComicBaby` 安装并信任，再在书源中选择“木瓜漫画”。

已安装本项目 1.6.1 / 1.6.2 的用户无需迁移书架。此商店保留原包名、书源 ID 和签名；发布更高版本后可在扩展列表更新。当前同为 1.6.2 时不显示更新是正常现象。

也可[手动下载 APK]({apk_path}?raw=true)。JAR 供兼容客户端使用。安装包和索引校验值见 [SHA256SUMS.txt](SHA256SUMS.txt)。

本目录分发索引、图标和安装包，源码位于本公开仓库的 [`src/zh/comicbaby`](../src/zh/comicbaby)。签名私钥、登录凭据和网站内容不在仓库中。软件使用 [Apache-2.0](LICENSE) 许可证。

本商店是用户自行添加的第三方商店，不代表已收录进 Komikku 官方目录。尚未在 Android 真机或模拟器中验证。
''', encoding='utf-8', newline='\n')
    files = [apk_path, jar_path, icon_path, 'index.pb', 'index.json', 'repo.json']
    (out / 'SHA256SUMS.txt').write_text(''.join(f'{hashlib.sha256((out / p).read_bytes()).hexdigest()}  {p}\n' for p in files), encoding='utf-8', newline='\n')
    decoded = index_pb2.Index.FromString(gzip.decompress((out / 'index.pb').read_bytes()))
    assert decoded == index
    assert decoded.extensionList.extensions[0].sources[0].id == SOURCE_ID
    assert decoded.signingKey == SIGNATURE
    decoded_json = json_format.Parse((out / 'index.json').read_text(encoding='utf-8'), index_pb2.Index())
    assert decoded_json == decoded
    print(json.dumps({'output': str(out), 'version': version, 'sourceId': str(SOURCE_ID), 'files': files, 'validation': 'protobuf/json roundtrip and APK signature passed'}, ensure_ascii=False))


if __name__ == '__main__':
    main()
