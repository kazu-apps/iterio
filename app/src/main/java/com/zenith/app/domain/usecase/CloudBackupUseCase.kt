package com.zenith.app.domain.usecase

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.zenith.app.data.cloud.CloudBackupInfo
import com.zenith.app.data.cloud.GoogleDriveManager
import com.zenith.app.domain.model.ImportResult
import com.zenith.app.domain.model.PremiumFeature
import com.zenith.app.domain.repository.BackupRepository
import com.zenith.app.domain.repository.PremiumRepository
import javax.inject.Inject

/**
 * クラウドバックアップのユースケース
 * Google Driveへのバックアップ・復元を管理
 */
class CloudBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val googleDriveManager: GoogleDriveManager,
    private val premiumRepository: PremiumRepository
) {
    /**
     * クラウドバックアップ機能が利用可能か確認
     */
    suspend fun canUseCloudBackup(): Boolean {
        return premiumRepository.canAccessFeature(PremiumFeature.BACKUP)
    }

    /**
     * Driveを初期化
     */
    fun initializeDrive(account: GoogleSignInAccount) {
        googleDriveManager.initialize(account)
    }

    /**
     * Driveが初期化済みか確認
     */
    fun isDriveInitialized(): Boolean {
        return googleDriveManager.isInitialized()
    }

    /**
     * クラウドにバックアップをアップロード
     */
    suspend fun uploadToCloud(): Result<CloudBackupInfo> {
        if (!canUseCloudBackup()) {
            return Result.failure(PremiumRequiredException())
        }

        if (!googleDriveManager.isInitialized()) {
            return Result.failure(IllegalStateException("Googleアカウントに接続されていません"))
        }

        return try {
            // ローカルバックアップデータを取得
            val backupData = backupRepository.exportBackup()

            // JSONに変換
            val jsonContent = backupRepository.serializeToJson(backupData)

            // クラウドにアップロード
            googleDriveManager.uploadBackup(jsonContent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * クラウドからバックアップを復元
     */
    suspend fun downloadFromCloud(): Result<ImportResult> {
        if (!canUseCloudBackup()) {
            return Result.failure(PremiumRequiredException())
        }

        if (!googleDriveManager.isInitialized()) {
            return Result.failure(IllegalStateException("Googleアカウントに接続されていません"))
        }

        return try {
            // クラウドからダウンロード
            val jsonResult = googleDriveManager.downloadBackup()

            jsonResult.fold(
                onSuccess = { jsonContent ->
                    // JSONをパース
                    val backupData = backupRepository.deserializeFromJson(jsonContent)
                        .getOrThrow()

                    // インポート実行
                    val result = backupRepository.importBackup(backupData)
                    Result.success(result)
                },
                onFailure = { e ->
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * クラウドバックアップ情報を取得
     */
    suspend fun getCloudBackupInfo(): Result<CloudBackupInfo?> {
        if (!googleDriveManager.isInitialized()) {
            return Result.failure(IllegalStateException("Googleアカウントに接続されていません"))
        }

        return googleDriveManager.getBackupInfo()
    }

    /**
     * クラウドバックアップを削除
     */
    suspend fun deleteCloudBackup(): Result<Unit> {
        if (!canUseCloudBackup()) {
            return Result.failure(PremiumRequiredException())
        }

        if (!googleDriveManager.isInitialized()) {
            return Result.failure(IllegalStateException("Googleアカウントに接続されていません"))
        }

        return googleDriveManager.deleteBackup()
    }

    /**
     * クリーンアップ
     */
    fun cleanup() {
        googleDriveManager.cleanup()
    }
}
