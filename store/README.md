# 木瓜漫画 · Komikku 插件商店

提供 ComicBaby 1.6.2 的安装和更新，含网站“喜欢这部的人也在看”推荐。

## 添加商店

需要 Komikku 1.14.0 或更新版本（支持扩展 API 1.6 和新版商店索引）。

在「更多 → 设置 → 浏览 → 插件商店（扩展商店）」中添加：

```text
https://raw.githubusercontent.com/name1e2s/komikku-comicbaby/main/store/index.pb
```

返回「浏览 → 扩展」刷新，启用中文及成人内容显示，搜索 `ComicBaby` 安装并信任，再在书源中选择“木瓜漫画”。

已安装本项目 1.6.1 / 1.6.2 的用户无需迁移书架。此商店保留原包名、书源 ID 和签名；发布更高版本后可在扩展列表更新。当前同为 1.6.2 时不显示更新是正常现象。

也可[手动下载 APK](apk/comicbaby-v1.6.2.apk?raw=true)。JAR 供兼容客户端使用。安装包和索引校验值见 [SHA256SUMS.txt](SHA256SUMS.txt)。

本目录分发索引、图标和安装包，源码位于本公开仓库的 [`src/zh/comicbaby`](../src/zh/comicbaby)。签名私钥、登录凭据和网站内容不在仓库中。软件使用 [Apache-2.0](LICENSE) 许可证。

本商店是用户自行添加的第三方商店，不代表已收录进 Komikku 官方目录。尚未在 Android 真机或模拟器中验证。
