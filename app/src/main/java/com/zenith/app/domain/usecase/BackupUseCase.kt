package com.zenith.app.domain.usecase

import android.net.Uri
import com.zenith.app.domain.model.BackupData
import com.zenith.app.domain.model.ImportResult
import com.zenith.app.domain.model.PremiumFeature
import com.zenith.app.domain.repository.BackupRepository
import com.zenith.app.domain.repository.PremiumRepository
import javax.inject.Inject

/**
 * バックアップ機能のユースケース
 * Premium機能チェックとバックアップ処理を統合
 */
class BackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val premiumRepository: PremiumRepository
) {
    /**
     * バックアップ機能が利用可能か確認
     */
    suspend fun canUseBackup(): Boolean {
        return premiumRepository.canAccessFeature(PremiumFeature.BACKUP)
    }

    /**
     * データをエクスポート
     */
    suspend fun exportBackup(uri: Uri): Result<Unit> {
        if (!canUseBackup()) {
            return Result.failure(PremiumRequiredException())
        }

        return try {
            val data = backupRepository.exportBackup()
            backupRepository.writeToFile(data, uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * データをインポート
     */
    suspend fun importBackup(uri: Uri): Result<ImportResult> {
        if (!canUseBackup()) {
            return Result.failure(PremiumRequiredException())
        }

        return try {
            val dataResult = backupRepository.readFromFile(uri)
            dataResult.fold(
                onSuccess = { data ->
                    val result = backupRepository.importBackup(data)
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
     * ファイルからバックアップデータを読み込み（プレビュー用）
     */
    suspend fun readBackupPreview(uri: Uri): Result<BackupData> {
        if (!canUseBackup()) {
            return Result.failure(PremiumRequiredException())
        }
        return backupRepository.readFromFile(uri)
    }
}

/**
 * Premium機能が必要な場合の例外
 */
class PremiumRequiredException : Exception("この機能を使用するにはPremiumへのアップグレードが必要です")
