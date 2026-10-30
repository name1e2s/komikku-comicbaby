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

需要支持扩展 API 1.6 的 Komikku。此仓库提供源码与可手动安装的 APK，尚未提供 Komikku 扩展仓库订阅和自动更新。

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
