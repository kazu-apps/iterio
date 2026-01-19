package com.zenith.app.domain.repository

import android.net.Uri
import com.zenith.app.domain.model.BackupData
import com.zenith.app.domain.model.ImportResult

/**
 * バックアップ機能のリポジトリインターフェース
 */
interface BackupRepository {
    /**
     * 全データをエクスポート用のBackupDataに変換
     */
    suspend fun exportBackup(): BackupData

    /**
     * BackupDataからデータをインポート（既存データを上書き）
     */
    suspend fun importBackup(data: BackupData): ImportResult

    /**
     * BackupDataをファイルに書き込み
     */
    suspend fun writeToFile(data: BackupData, uri: Uri): Result<Unit>

    /**
     * ファイルからBackupDataを読み込み
     */
    suspend fun readFromFile(uri: Uri): Result<BackupData>

    /**
     * BackupDataをJSON文字列にシリアライズ
     */
    fun serializeToJson(data: BackupData): String

    /**
     * JSON文字列からBackupDataにデシリアライズ
     */
    fun deserializeFromJson(json: String): Result<BackupData>
}
