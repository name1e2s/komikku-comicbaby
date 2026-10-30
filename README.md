# 木瓜漫画 · Komikku 扩展

为 [comic.baby](https://comic.baby/) 制作的 Android Komikku 书源，包含网站“喜欢这部的人也在看”的相关推荐。

| 项目 | 值 |
| --- | --- |
| 扩展名称 | ComicBaby |
| 书源名称 | 木瓜漫画 |
| 版本 | 1.6.2 |
| 语言 | 中文 |
| 最低系统 | Android 8.0 |
| 扩展 API | 1.6 |

## 安装

1. [下载 1.6.2 APK](apk/comicbaby-v1.6.2.apk?raw=true)，传到手机并安装。已安装本项目 1.6.1 的用户可以直接覆盖更新。
2. 在 Komikku 扩展列表中找到 `ComicBaby`，按提示信任扩展。
3. 在书源列表搜索“木瓜漫画”；需要启用中文和成人内容显示。
4. 更新后重启 Komikku，在漫画详情页打开相关推荐。

需要 Komikku 1.14.0 或更新版本，支持扩展 API 1.6 和新版插件商店。

## 插件商店

已生成 [`store/`](store) 分发目录，包含 gzip Protobuf `index.pb`、可读索引、兼容入口、图标、APK 和 JAR。索引保留现有签名、包名和书源 ID，无需迁移书架。

本公开仓库直接提供插件商店。在 Komikku「更多 → 设置 → 浏览 → 插件商店（扩展商店）」中添加：

```text
https://raw.githubusercontent.com/name1e2s/komikku-comicbaby/main/store/index.pb
```

返回「浏览 → 扩展」刷新，启用中文及成人内容显示，搜索 `ComicBaby` 安装并信任，然后在书源中选择“木瓜漫画”。这是可自行添加的第三方商店，不代表收录进官方目录。

商店格式依据 [Komikku 商店读取实现](https://github.com/komikku-app/komikku/blob/master/data/src/main/java/mihon/data/extension/service/ExtensionStoreService.kt) 和固定版本扩展框架的 `index.proto`。协议文件和 Python 绑定位于 `tools/`，来源为上文框架版本。

维护更新时，先增加模块 `versionCode`、用原签名构建 APK/JAR，再生成分发目录：

```powershell
python -m pip install -r tools/requirements.txt
python tools/build_store.py --framework '扩展框架目录' --apksigner 'Android SDK/build-tools/36.0.0/apksigner.bat'
```

脚本从构建元数据读取版本，校验 APK 签名，保留 64 位书源 ID 精度，并验证 Protobuf/JSON 往返解析。将生成的 `store/` 目录提交并推送至本仓库 `main` 分支即可发布；发布更高版本后，Komikku 可在扩展列表检查并安装更新。仅添加商店不会让现有 1.6.2 出现同版本更新提示。

## 功能

- 热门列表、最新更新和分页。
- 中文关键词、作者、JM 编号搜索，以及 `/work/...` 漫画链接搜索。
- 点阅、更新时间、爱心、评论排序与标签筛选。
- 漫画封面、作者、简介、别名、分类、状态、章节和发布时间。
- 普通图片条带还原与网站 `img2` 图片的 AES-CBC 解密。
- Komikku 相关推荐接入网站“喜欢这部的人也在看”：请求 18 条，保留网站顺序，排除当前漫画，按标题去重后显示最多 12 部。

推荐由网站生成，可能随时间变化。公开章节无需登录；尚未接入账户和会员章节。网站某些无匹配搜索会返回兜底推荐。图片还原未加入网站用于隐藏分块边缘的有损平滑处理。

## 验证

- Release APK 构建成功，Android lint 为 0 错误、2 项构建框架生成清单警告。
- APK 签名验证通过；保留 1.6.1 的签名、包名和书源 ID。
- [基础接口测试结果](verification.json)：热门、更新、分页、搜索、标签、详情和完整章节。
- [推荐接口测试结果](related-verification.json)：三部漫画的推荐列表、推荐漫画详情与章节，以及最新更新的数据结构。

尚未在 Android 真机或模拟器中实测，实际表现取决于手机网络、网站接口和 Komikku 版本。

本地 release APK 使用 Android debug 证书签名，但未开启 debuggable。签名私钥不在仓库中。校验值见 [SHA256SUMS.txt](SHA256SUMS.txt)。

## 源码与构建

模块位于 [`src/zh/comicbaby`](src/zh/comicbaby)，基于 [Yuzono 扩展框架](https://github.com/yuzono/tachiyomi-extensions)，固定版本 `c9fd7bc452e75587c2ad0d62838190018817b83f`。

使用 JDK 21 和 Android SDK 37。将本仓库的 `src/zh/comicbaby` 复制到该版本框架的相同路径，在框架根目录执行：

```powershell
$env:ANDROID_HOME = '你的 Android SDK 目录'
.\gradlew.bat :src:zh:comicbaby:spotlessApply :src:zh:comicbaby:assembleRelease :src:zh:comicbaby:lintRelease
```

输出为 `src/zh/comicbaby/build/outputs/apk/release/tachiyomi-zh.comicbaby-v1.6.2.apk`。重新发布更新时需保留自己的签名密钥；使用不同签名构建的 APK 无法直接覆盖这里的安装包。

Windows 下复测公开接口，需要 Node.js 和 `curl.exe`：

```powershell
node verify-live.mjs
node verify-related.mjs
```

## 版本记录

- **1.6.2**：接入网站“喜欢这部的人也在看”的 Komikku 相关推荐。
- **1.6.1**：首版，提供浏览、搜索、详情、章节和图片还原。

## 许可证

源码按 [Apache-2.0](LICENSE) 提供。网站及其内容不属于本项目。本项目未与网站或 Komikku 官方建立关联。
