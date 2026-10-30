import io.github.keiyoushi.gradle.api.ContentWarning

plugins {
    alias(kei.plugins.extension)
}

keiyoushi {
    name = "ComicBaby"
    versionCode = 2
    contentWarning = ContentWarning.NSFW
    libVersion = "1.6"

    source {
        name = "木瓜漫画"
        baseUrl = "https://comic.baby"
        lang = "zh"
    }

    deeplink {
        path("/work/..*")
    }
}
