package eu.kanade.tachiyomi.extension.zh.comicbaby

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import keiyoushi.utils.aesCbc
import keiyoushi.utils.aesDecrypt
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import java.io.IOException

class ImageInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (!response.isSuccessful) return response
        if (encryptedHost.containsMatchIn(request.url.host)) {
            // The site's encrypted representation is already unscrambled.
            return response.aesDecrypt(aesCbc(key, iv), "image/webp".toMediaType())
        }
        val segments = request.url.fragment?.substringAfter("segments=", "")?.toIntOrNull() ?: 0
        if (segments <= 1) return response
        return response.use {
            val input = BitmapFactory.decodeStream(it.body.byteStream()) ?: throw IOException("无法解码漫画图片")
            try {
                if (segments > input.height) throw IOException("图片分块参数无效")
                val output = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
                try {
                    val canvas = Canvas(output)
                    val height = input.height / segments
                    val remainder = input.height % segments
                    var destination = 0
                    for (part in segments - 1 downTo 0) {
                        val size = height + if (part == segments - 1) remainder else 0
                        canvas.drawBitmap(
                            input,
                            Rect(0, part * height, input.width, part * height + size),
                            Rect(0, destination, input.width, destination + size),
                            null,
                        )
                        destination += size
                    }
                    val buffer = Buffer()
                    if (!output.compress(Bitmap.CompressFormat.PNG, 100, buffer.outputStream())) throw IOException("图片还原失败")
                    it.newBuilder()
                        .removeHeader("Content-Encoding")
                        .removeHeader("Content-Length")
                        .header("Content-Type", "image/png")
                        .body(buffer.asResponseBody("image/png".toMediaType(), buffer.size))
                        .build()
                } finally {
                    output.recycle()
                }
            } finally {
                input.recycle()
            }
        }
    }

    companion object {
        private val encryptedHost = Regex("^img2(-[a-z]+)?\\.", RegexOption.IGNORE_CASE)
        private val key = "8Z7X9f2kL3s7m8P9q0R5t6y7u8i9o0p8".encodeToByteArray()
        private val iv = "abcdef0987654321".encodeToByteArray()
    }
}
