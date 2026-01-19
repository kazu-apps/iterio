package com.zenith.app.data.billing

import android.util.Base64
import android.util.Log
import com.zenith.app.BuildConfig
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Play 署名検証クラス
 * 購入データの署名をRSA公開鍵で検証する
 */
@Singleton
class SignatureVerifier @Inject constructor() {

    companion object {
        private const val TAG = "SignatureVerifier"

        /**
         * Google Play Console → 収益化の設定 → ライセンス から取得
         * Base64エンコードされたRSA公開鍵
         *
         * gradle.propertiesに以下を設定:
         * BILLING_PUBLIC_KEY=MIIBIjANBg...（実際の公開鍵）
         */
        private val BASE64_ENCODED_PUBLIC_KEY: String = BuildConfig.BILLING_PUBLIC_KEY

        private const val KEY_ALGORITHM = "RSA"
        private const val SIGNATURE_ALGORITHM = "SHA256withRSA"
    }

    private val publicKey: PublicKey? by lazy {
        try {
            if (BASE64_ENCODED_PUBLIC_KEY.isEmpty()) {
                // 公開鍵が設定されていない場合はnullを返す
                null
            } else {
                val decodedKey = Base64.decode(BASE64_ENCODED_PUBLIC_KEY, Base64.DEFAULT)
                val keySpec = X509EncodedKeySpec(decodedKey)
                KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(keySpec)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 署名を検証する
     *
     * @param signedData 署名対象のデータ（Purchase.originalJson）
     * @param signature Base64エンコードされた署名（Purchase.signature）
     * @return 署名が有効な場合true、無効またはエラーの場合false
     */
    fun verify(signedData: String, signature: String): Boolean {
        val key = publicKey ?: run {
            // 公開鍵が設定されていない場合は検証失敗とする
            // セキュリティのため、未設定時は購入を拒否する
            logError("Public key not configured")
            return false
        }

        if (signedData.isEmpty() || signature.isEmpty()) {
            logWarn("Empty signedData or signature")
            return false
        }

        return try {
            val sig = Signature.getInstance(SIGNATURE_ALGORITHM)
            sig.initVerify(key)
            sig.update(signedData.toByteArray(Charsets.UTF_8))
            val result = sig.verify(Base64.decode(signature, Base64.DEFAULT))
            if (!result) {
                logWarn("Signature verification failed")
            }
            result
        } catch (e: Exception) {
            logError("Signature verification error: ${e.message}")
            false
        }
    }

    // ユニットテスト環境ではログが使用できないため、安全にラップ
    private fun logError(message: String) {
        try {
            Log.e(TAG, message)
        } catch (_: Exception) {
            // ユニットテスト環境では無視
        }
    }

    private fun logWarn(message: String) {
        try {
            Log.w(TAG, message)
        } catch (_: Exception) {
            // ユニットテスト環境では無視
        }
    }

    /**
     * 公開鍵が設定されているかチェック
     */
    fun isKeyConfigured(): Boolean {
        return BASE64_ENCODED_PUBLIC_KEY.isNotEmpty() && publicKey != null
    }
}
