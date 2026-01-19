package com.zenith.app.data.encryption

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * バックアップデータの暗号化/復号化を管理
 * AES-256-GCM方式を使用
 */
@Singleton
class EncryptionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "EncryptionManager"

        // KeyStore
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "zenith_backup_key"

        // AES-GCM パラメータ
        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        // GCM タグ長 (128bits)
        private const val GCM_TAG_LENGTH = 128

        // NONCE長 (96bits = 12bytes、GCM推奨)
        private const val NONCE_LENGTH = 12
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    /**
     * バックアップJSON文字列を暗号化
     *
     * @param plainJson 平文のJSON
     * @return 暗号化されたバイト配列 (NONCE + 暗号化データ + 認証タグ)
     */
    fun encryptBackup(plainJson: String): ByteArray {
        return try {
            val secretKey = getOrCreateKey()

            // 乱数NONCEを生成 (毎回異なるNONCE)
            val nonce = ByteArray(NONCE_LENGTH)
            SecureRandom().nextBytes(nonce)

            // Cipherを初期化
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, nonce)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

            // 暗号化
            val encryptedData = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))

            // NONCE + 暗号化データを結合して返す
            logDebug("Encrypted backup: ${plainJson.length} bytes -> ${nonce.size + encryptedData.size} bytes")
            nonce + encryptedData
        } catch (e: Exception) {
            logError("Encryption failed", e)
            throw EncryptionException("バックアップの暗号化に失敗しました", e)
        }
    }

    /**
     * 暗号化されたバックアップを復号化
     *
     * @param encryptedBytes 暗号化されたバイト配列 (NONCE + 暗号化データ + 認証タグ)
     * @return 復号化されたJSON文字列
     */
    fun decryptBackup(encryptedBytes: ByteArray): String {
        return try {
            if (encryptedBytes.size <= NONCE_LENGTH) {
                throw EncryptionException("暗号化データが短すぎます")
            }

            val secretKey = getOrCreateKey()

            // NONCEと暗号化データを分離
            val nonce = encryptedBytes.copyOfRange(0, NONCE_LENGTH)
            val cipherText = encryptedBytes.copyOfRange(NONCE_LENGTH, encryptedBytes.size)

            // Cipherを初期化
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, nonce)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            // 復号化
            val decryptedData = cipher.doFinal(cipherText)

            logDebug("Decrypted backup: ${encryptedBytes.size} bytes -> ${decryptedData.size} bytes")
            String(decryptedData, Charsets.UTF_8)
        } catch (e: EncryptionException) {
            throw e
        } catch (e: Exception) {
            logError("Decryption failed", e)
            throw EncryptionException("バックアップの復号化に失敗しました", e)
        }
    }

    /**
     * データが暗号化されているか判定
     * JSONは '{' (0x7B) で始まるため、それ以外は暗号化と判定
     *
     * @param bytes バイト配列
     * @return 暗号化されていればtrue
     */
    fun isEncryptedData(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        // JSONの開始バイト ('{' = 0x7B) でない場合は暗号化
        return bytes[0] != 0x7B.toByte()
    }

    /**
     * 暗号化キーを取得または生成
     * Android KeyStoreにセキュアに保存
     */
    private fun getOrCreateKey(): SecretKey {
        // 既存キーの確認
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        // キーを生成
        logDebug("Generating new encryption key")
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, ANDROID_KEYSTORE)
        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }

    // ログヘルパー（ユニットテスト環境対応）
    private fun logDebug(message: String) {
        try {
            Log.d(TAG, message)
        } catch (_: Exception) {
            // ユニットテスト環境では無視
        }
    }

    private fun logError(message: String, e: Exception) {
        try {
            Log.e(TAG, message, e)
        } catch (_: Exception) {
            // ユニットテスト環境では無視
        }
    }
}

/**
 * 暗号化エラーの例外
 */
class EncryptionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
