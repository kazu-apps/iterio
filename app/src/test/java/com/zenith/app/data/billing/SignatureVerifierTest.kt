package com.zenith.app.data.billing

import org.junit.Assert.*
import org.junit.Test

/**
 * SignatureVerifier のユニットテスト
 *
 * 注意: 実際の署名検証テストは Android Instrumented Test で行う必要があります。
 * このテストは基本的なロジックの検証のみを行います。
 */
class SignatureVerifierTest {

    @Test
    fun `verify returns false when signedData is empty`() {
        val verifier = SignatureVerifier()

        val result = verifier.verify("", "validSignature")

        assertFalse("空のsignedDataでは検証失敗すべき", result)
    }

    @Test
    fun `verify returns false when signature is empty`() {
        val verifier = SignatureVerifier()

        val result = verifier.verify("validData", "")

        assertFalse("空のsignatureでは検証失敗すべき", result)
    }

    @Test
    fun `verify returns false when both parameters are empty`() {
        val verifier = SignatureVerifier()

        val result = verifier.verify("", "")

        assertFalse("両方空では検証失敗すべき", result)
    }

    @Test
    fun `isKeyConfigured returns false when public key is not set`() {
        val verifier = SignatureVerifier()

        // BuildConfig.BILLING_PUBLIC_KEYが空の場合
        // この状態では isKeyConfigured() は false を返すべき
        val result = verifier.isKeyConfigured()

        assertFalse("公開鍵未設定時はfalseを返すべき", result)
    }

    @Test
    fun `verify returns false when public key is not configured`() {
        val verifier = SignatureVerifier()

        // 公開鍵が設定されていない状態でverifyを呼び出す
        val result = verifier.verify("someData", "someSignature")

        // セキュリティ上、公開鍵未設定時は検証失敗とすべき
        assertFalse("公開鍵未設定時は検証失敗すべき（セキュリティ）", result)
    }
}
