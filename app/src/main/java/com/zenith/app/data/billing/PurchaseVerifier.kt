package com.zenith.app.data.billing

import com.android.billingclient.api.Purchase
import javax.inject.Inject
import javax.inject.Singleton

interface PurchaseVerifier {
    suspend fun verifyPurchase(purchase: Purchase): VerificationResult

    sealed class VerificationResult {
        data class Verified(val purchase: Purchase) : VerificationResult()
        data class Failed(val reason: String) : VerificationResult()
        data object Pending : VerificationResult()
    }
}

/**
 * ローカル検証実装
 * 署名検証と購入状態の検証を行う
 * 将来的にはサーバーサイド検証を追加予定
 */
@Singleton
class LocalPurchaseVerifier @Inject constructor(
    private val signatureVerifier: SignatureVerifier
) : PurchaseVerifier {
    override suspend fun verifyPurchase(purchase: Purchase): PurchaseVerifier.VerificationResult {
        // 署名検証
        val signature = purchase.signature
        val originalJson = purchase.originalJson

        if (!signatureVerifier.verify(originalJson, signature)) {
            return PurchaseVerifier.VerificationResult.Failed("Signature verification failed")
        }

        // 購入状態チェック
        return when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                // 購入完了
                PurchaseVerifier.VerificationResult.Verified(purchase)
            }
            Purchase.PurchaseState.PENDING -> {
                // 支払い保留中（コンビニ払い等）
                PurchaseVerifier.VerificationResult.Pending
            }
            else -> {
                // 未購入またはその他の状態
                PurchaseVerifier.VerificationResult.Failed("Invalid purchase state: ${purchase.purchaseState}")
            }
        }
    }
}

/**
 * サーバーサイド検証実装（将来対応）
 * Google Play Developer APIを使用した検証
 */
// class ServerPurchaseVerifier @Inject constructor(
//     private val apiService: ZenithApiService
// ) : PurchaseVerifier {
//     override suspend fun verifyPurchase(purchase: Purchase): PurchaseVerifier.VerificationResult {
//         return try {
//             val response = apiService.verifyPurchase(
//                 productId = purchase.products.firstOrNull() ?: "",
//                 purchaseToken = purchase.purchaseToken
//             )
//             if (response.isValid) {
//                 PurchaseVerifier.VerificationResult.Verified(purchase)
//             } else {
//                 PurchaseVerifier.VerificationResult.Failed(response.errorMessage ?: "Verification failed")
//             }
//         } catch (e: Exception) {
//             PurchaseVerifier.VerificationResult.Failed(e.message ?: "Network error")
//         }
//     }
// }
