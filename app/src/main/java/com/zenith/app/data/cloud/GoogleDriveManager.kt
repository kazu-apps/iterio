package com.zenith.app.data.cloud

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Drive操作管理クラス
 * バックアップファイルのアップロード・ダウンロード
 */
@Singleton
class GoogleDriveManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val APP_NAME = "ZENITH"
        private const val BACKUP_FILE_NAME = "zenith_cloud_backup.json"
        private const val MIME_TYPE_JSON = "application/json"
    }

    private var driveService: Drive? = null

    /**
     * Drive APIを初期化
     */
    fun initialize(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(APP_NAME)
            .build()
    }

    /**
     * 初期化済みかチェック
     */
    fun isInitialized(): Boolean = driveService != null

    /**
     * バックアップをアップロード
     */
    suspend fun uploadBackup(jsonContent: String): Result<CloudBackupInfo> = withContext(Dispatchers.IO) {
        val service = driveService ?: return@withContext Result.failure(
            IllegalStateException("Drive APIが初期化されていません")
        )

        try {
            // 既存のバックアップファイルを検索
            val existingFile = findBackupFile()

            val fileContent = ByteArrayContent.fromString(MIME_TYPE_JSON, jsonContent)

            val uploadedFile = if (existingFile != null) {
                // 既存ファイルを更新
                service.files().update(existingFile.id, null, fileContent)
                    .execute()
            } else {
                // 新規ファイルを作成
                val fileMetadata = File().apply {
                    name = BACKUP_FILE_NAME
                    parents = listOf("appDataFolder") // アプリデータフォルダに保存
                }
                service.files().create(fileMetadata, fileContent)
                    .setFields("id, name, modifiedTime, size")
                    .execute()
            }

            // 更新後のファイル情報を取得
            val fileInfo = service.files().get(uploadedFile.id)
                .setFields("id, name, modifiedTime, size")
                .execute()

            Result.success(
                CloudBackupInfo(
                    fileId = fileInfo.id,
                    fileName = fileInfo.name,
                    modifiedTime = fileInfo.modifiedTime?.value ?: System.currentTimeMillis(),
                    sizeBytes = fileInfo.getSize()?.toLong() ?: 0L
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * バックアップをダウンロード
     */
    suspend fun downloadBackup(): Result<String> = withContext(Dispatchers.IO) {
        val service = driveService ?: return@withContext Result.failure(
            IllegalStateException("Drive APIが初期化されていません")
        )

        try {
            val backupFile = findBackupFile()
                ?: return@withContext Result.failure(
                    NoBackupFoundException("クラウドバックアップが見つかりません")
                )

            val outputStream = ByteArrayOutputStream()
            service.files().get(backupFile.id)
                .executeMediaAndDownloadTo(outputStream)

            val content = outputStream.toString(Charsets.UTF_8.name())
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * バックアップファイル情報を取得
     */
    suspend fun getBackupInfo(): Result<CloudBackupInfo?> = withContext(Dispatchers.IO) {
        val service = driveService ?: return@withContext Result.failure(
            IllegalStateException("Drive APIが初期化されていません")
        )

        try {
            val backupFile = findBackupFile()

            if (backupFile == null) {
                Result.success(null)
            } else {
                Result.success(
                    CloudBackupInfo(
                        fileId = backupFile.id,
                        fileName = backupFile.name,
                        modifiedTime = backupFile.modifiedTime?.value ?: 0L,
                        sizeBytes = backupFile.getSize()?.toLong() ?: 0L
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * バックアップファイルを検索
     */
    private fun findBackupFile(): File? {
        val service = driveService ?: return null

        val result = service.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$BACKUP_FILE_NAME'")
            .setFields("files(id, name, modifiedTime, size)")
            .setPageSize(1)
            .execute()

        return result.files?.firstOrNull()
    }

    /**
     * バックアップを削除
     */
    suspend fun deleteBackup(): Result<Unit> = withContext(Dispatchers.IO) {
        val service = driveService ?: return@withContext Result.failure(
            IllegalStateException("Drive APIが初期化されていません")
        )

        try {
            val backupFile = findBackupFile()
                ?: return@withContext Result.success(Unit) // 既に存在しない

            service.files().delete(backupFile.id).execute()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * クリーンアップ
     */
    fun cleanup() {
        driveService = null
    }
}

/**
 * クラウドバックアップ情報
 */
data class CloudBackupInfo(
    val fileId: String,
    val fileName: String,
    val modifiedTime: Long,
    val sizeBytes: Long
)

/**
 * バックアップが見つからない例外
 */
class NoBackupFoundException(message: String) : Exception(message)
