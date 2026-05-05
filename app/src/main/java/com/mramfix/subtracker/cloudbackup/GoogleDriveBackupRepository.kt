package com.mramfix.subtracker.cloudbackup

import com.mramfix.subtracker.importexport.ImportExportRepository
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class GoogleDriveBackupRepository(
    private val importExportRepository: ImportExportRepository,
    private val json: Json,
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun exportToGoogleDrive(accessToken: String): Result<Unit> = runCatching {
        val backupJson = importExportRepository.exportJson()
        val existing = findLatestBackup(accessToken)
        if (existing == null) {
            createBackup(accessToken, backupJson)
        } else {
            updateBackup(accessToken, existing.id, backupJson)
        }
    }

    suspend fun importFromGoogleDrive(accessToken: String): CloudImportResult {
        return try {
            val file = findLatestBackup(accessToken) ?: return CloudImportResult.Error("Резервная копия в Google Drive не найдена")
            val text = downloadBackup(accessToken, file.id)
            when (val result = importExportRepository.importJson(text)) {
                is com.mramfix.subtracker.importexport.ImportResult.Success -> CloudImportResult.Success
                is com.mramfix.subtracker.importexport.ImportResult.Error -> CloudImportResult.Error(result.message)
            }
        } catch (e: Exception) {
            CloudImportResult.Error(e.toUserMessage("Не удалось импортировать из Google Drive"))
        }
    }

    private fun findLatestBackup(accessToken: String): DriveFile? {
        val url = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
            .addQueryParameter("spaces", "appDataFolder")
            .addQueryParameter("q", "name = '$BACKUP_FILE_NAME' and trashed = false")
            .addQueryParameter("fields", "files(id,name,modifiedTime)")
            .addQueryParameter("orderBy", "modifiedTime desc")
            .addQueryParameter("pageSize", "1")
            .build()
        val request = Request.Builder()
            .url(url)
            .authorization(accessToken)
            .get()
            .build()
        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw DriveApiException("list", response.code, response.body?.string())
            response.body?.string() ?: throw IOException("Drive list returned empty body")
        }
        return json.decodeFromString<DriveFileList>(body).files.firstOrNull()
    }

    private fun createBackup(accessToken: String, backupJson: String) {
        val metadata = """{"name":"$BACKUP_FILE_NAME","parents":["appDataFolder"],"mimeType":"application/json"}"""
        val body = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(metadata.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .addPart(backupJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id")
            .authorization(accessToken)
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw DriveApiException("create", response.code, response.body?.string())
        }
    }

    private fun updateBackup(accessToken: String, fileId: String, backupJson: String) {
        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media&fields=id,modifiedTime")
            .authorization(accessToken)
            .patch(backupJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw DriveApiException("update", response.code, response.body?.string())
        }
    }

    private fun downloadBackup(accessToken: String, fileId: String): String {
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
            .authorization(accessToken)
            .get()
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw DriveApiException("download", response.code, response.body?.string())
            response.body?.string() ?: throw IOException("Drive download returned empty body")
        }
    }

    private fun Request.Builder.authorization(accessToken: String): Request.Builder {
        return header("Authorization", "Bearer $accessToken")
    }

    companion object {
        const val BACKUP_FILE_NAME = "subtracker-backup.json"
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}

class DriveApiException(
    val operation: String,
    val code: Int,
    body: String?
) : IOException("Drive $operation failed: HTTP $code ${body.orEmpty()}") {
    val shortBody: String = body.orEmpty()
        .replace('\n', ' ')
        .replace(Regex("\\s+"), " ")
        .take(240)
}

sealed class CloudImportResult {
    data object Success : CloudImportResult()
    data class Error(val message: String) : CloudImportResult()
}

@Serializable
private data class DriveFileList(
    val files: List<DriveFile> = emptyList()
)

@Serializable
private data class DriveFile(
    val id: String,
    val name: String,
    @SerialName("modifiedTime") val modifiedTime: String? = null
)
