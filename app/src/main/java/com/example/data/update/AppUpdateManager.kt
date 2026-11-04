package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class GitHubReleaseInfo(
    val tagName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val publishedAt: String
)

object AppUpdateManager {

    val CURRENT_VERSION_NAME: String
        get() = com.example.BuildConfig.VERSION_NAME
    const val DEFAULT_GITHUB_REPO = "lissandroproyectos/monytracker"

    fun isVersionUpToDate(currentVersion: String = CURRENT_VERSION_NAME, latestTag: String): Boolean {
        val cleanTag = latestTag.removePrefix("v").removePrefix("V").trim()
        val cleanCur = currentVersion.removePrefix("v").removePrefix("V").trim()
        if (cleanTag.equals(cleanCur, ignoreCase = true)) return true

        // Extract integer segments from version strings (e.g. "1.20" -> [1, 20], "1.18" -> [1, 18])
        val pTag = cleanTag.split(".").map { seg ->
            seg.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }
        val pCur = cleanCur.split(".").map { seg ->
            seg.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }

        val maxLen = maxOf(pTag.size, pCur.size)
        for (i in 0 until maxLen) {
            val tagNum = pTag.getOrElse(i) { 0 }
            val curNum = pCur.getOrElse(i) { 0 }
            if (tagNum > curNum) {
                // Published version on GitHub is higher than current version -> NOT up to date!
                return false
            }
            if (curNum > tagNum) {
                // Current version is higher than published tag -> Up to date
                return true
            }
        }
        return true
    }

    suspend fun checkLatestRelease(githubRepo: String = DEFAULT_GITHUB_REPO): Result<GitHubReleaseInfo?> = withContext(Dispatchers.IO) {
        try {
            val cleanRepo = githubRepo.trim().removePrefix("https://github.com/").trim('/')
            if (cleanRepo.isBlank() || !cleanRepo.contains('/')) {
                return@withContext Result.failure(IllegalArgumentException("Formato de repositorio inválido. Usar 'usuario/repositorio'"))
            }

            val url = URL("https://api.github.com/repos/$cleanRepo/releases/latest")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Finanzas-Android-App")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (conn.responseCode == 200) {
                val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonString)
                val tagName = root.optString("tag_name", "")
                val name = root.optString("name", tagName)
                val body = root.optString("body", "Nueva versión disponible.")
                val publishedAt = root.optString("published_at", "")

                var downloadUrl = ""
                val assets = root.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val assetName = asset.optString("name", "")
                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                if (downloadUrl.isBlank()) {
                    // Fallback to release page if direct APK asset is not attached
                    downloadUrl = root.optString("html_url", "")
                }

                val releaseInfo = GitHubReleaseInfo(
                    tagName = tagName,
                    releaseTitle = name,
                    releaseNotes = body,
                    apkDownloadUrl = downloadUrl,
                    publishedAt = publishedAt
                )
                Result.success(releaseInfo)
            } else if (conn.responseCode == 404) {
                Result.failure(Exception("No se encontraron releases en '$cleanRepo'"))
            } else {
                Result.failure(Exception("Error al consultar GitHub (HTTP ${conn.responseCode})"))
            }
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Check release error", e)
            Result.failure(e)
        }
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("User-Agent", "Finanzas-Android-App")
            }

            // Handle HTTP redirects (GitHub Releases redirect to AWS S3)
            var responseCode = conn.responseCode
            var redirectUrl = downloadUrl
            var currentConn = conn

            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                redirectUrl = currentConn.getHeaderField("Location")
                currentConn = (URL(redirectUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "Finanzas-Android-App")
                }
            }

            val fileLength = currentConn.contentLength.toFloat()
            val input = currentConn.inputStream

            val apkDir = File(context.externalCacheDir ?: context.cacheDir, "updates")
            if (!apkDir.exists()) apkDir.mkdirs()
            val outputFile = File(apkDir, "update_finanzas.apk")
            if (outputFile.exists()) outputFile.delete()

            val output = FileOutputStream(outputFile)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                if (fileLength > 0) {
                    val progress = (totalRead / fileLength).coerceIn(0f, 1f)
                    onProgress(progress)
                }
            }

            output.flush()
            output.close()
            input.close()

            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Download APK error", e)
            Result.failure(e)
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Install APK error", e)
        }
    }
}
