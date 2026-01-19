package com.zenith.app.domain.usecase

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.zenith.app.data.billing.BillingClientWrapper
import com.zenith.app.data.billing.BillingProducts
import com.zenith.app.data.billing.PurchaseVerifier
import com.zenith.app.domain.model.SubscriptionType
import com.zenith.app.domain.repository.PremiumRepository
import kotlinx.coroutines.flow.SharedFlow
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingUseCase @Inject constructor(
    private val billingClientWrapper: BillingClientWrapper,
    private val purchaseVerifier: PurchaseVerifier,
    private val premiumRepository: PremiumRepository
) {
    val newPurchases: SharedFlow<List<Purchase>> = billingClientWrapper.newPurchases

    suspend fun ensureConnected(): Boolean {
        return billingClientWrapper.startConnection()
    }

    suspend fun getAvailableProducts(): Result<List<ProductInfo>> {
        return try {
            if (!ensureConnected()) {
                return Result.failure(BillingException.ConnectionFailed("Failed to connect to billing service"))
            }

            val productDetails = billingClientWrapper.queryAllProductDetails()
            val products = productDetails.mapNotNull { details ->
                toProductInfo(details)
            }

            Result.success(products)
        } catch (e: Exception) {
            Result.failure(BillingException.QueryFailed(e.message ?: "Unknown error"))
        }
    }

    private fun toProductInfo(details: ProductDetails): ProductInfo? {
        val productId = details.productId
        val subscriptionType = BillingProducts.toSubscriptionType(productId)

        return if (BillingProducts.isSubscription(productId)) {
            // サブスクリプションの場合
            val offer = details.subscriptionOfferDetails?.firstOrNull() ?: return null
            val pricingPhase = offer.pricingPhases.pricingPhaseList.firstOrNull() ?: return null

            ProductInfo(
                type = subscriptionType,
                productId = productId,
                price = pricingPhase.formattedPrice,
                priceMicros = pricingPhase.priceAmountMicros,
                priceCurrencyCode = pricingPhase.priceCurrencyCode,
                billingPeriod = pricingPhase.billingPeriod,
                offerToken = offer.offerToken,
                productDetails = details
            )
        } else {
            // 一回購入の場合
            val oneTimePurchaseOffer = details.oneTimePurchaseOfferDetails ?: return null

            ProductInfo(
                type = subscriptionType,
                productId = productId,
                price = oneTimePurchaseOffer.formattedPrice,
                priceMicros = oneTimePurchaseOffer.priceAmountMicros,
                priceCurrencyCode = oneTimePurchaseOffer.priceCurrencyCode,
                billingPeriod = null,
                offerToken = null,
                productDetails = details
            )
        }
    }

    suspend fun startPurchase(
        activity: Activity,
        subscriptionType: SubscriptionType
    ): Result<Unit> {
        return try {
            if (!ensureConnected()) {
                return Result.failure(BillingException.ConnectionFailed("Failed to connect to billing service"))
            }

            val productId = BillingProducts.toProductId(subscriptionType)
                ?: return Result.failure(BillingException.InvalidProduct("Invalid subscription type"))

            val productDetails = billingClientWrapper.queryAllProductDetails()
                .find { it.productId == productId }
                ?: return Result.failure(BillingException.ProductNotFound("Product not found: $productId"))

            val offerToken = if (BillingProducts.isSubscription(productId)) {
                productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            } else {
                null
            }

            val billingResult = billingClientWrapper.launchBillingFlow(activity, productDetails, offerToken)

            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> Result.success(Unit)
                BillingClient.BillingResponseCode.USER_CANCELED ->
                    Result.failure(BillingException.UserCanceled("User canceled the purchase"))
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                    Result.failure(BillingException.AlreadyOwned("Item already owned"))
                else ->
                    Result.failure(BillingException.PurchaseFailed("Purchase failed: ${billingResult.debugMessage}"))
            }
        } catch (e: Exception) {
            Result.failure(BillingException.PurchaseFailed(e.message ?: "Unknown error"))
        }
    }

    suspend fun processPurchase(purchase: Purchase): ProcessPurchaseResult {
        val verificationResult = purchaseVerifier.verifyPurchase(purchase)

        return when (verificationResult) {
            is PurchaseVerifier.VerificationResult.Verified -> {
                // 購入承認（未承認の場合）
                if (!purchase.isAcknowledged) {
                    val ackResult = billingClientWrapper.acknowledgePurchase(purchase.purchaseToken)
                    if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        return ProcessPurchaseResult.Error("Failed to acknowledge purchase")
                    }
                }

                val productId = purchase.products.firstOrNull()
                val subscriptionType = productId?.let { BillingProducts.toSubscriptionType(it) }
                    ?: SubscriptionType.FREE

                val expiresAt = calculateExpiryDate(purchase, subscriptionType)
                premiumRepository.updateSubscription(subscriptionType, expiresAt)

                ProcessPurchaseResult.Success(subscriptionType)
            }
            is PurchaseVerifier.VerificationResult.Pending -> {
                ProcessPurchaseResult.Pending
            }
            is PurchaseVerifier.VerificationResult.Failed -> {
                ProcessPurchaseResult.Error(verificationResult.reason)
            }
        }
    }

    private fun calculateExpiryDate(purchase: Purchase, type: SubscriptionType): LocalDateTime? {
        val purchaseTime = Instant.ofEpochMilli(purchase.purchaseTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

        return when (type) {
            SubscriptionType.LIFETIME -> null // 無期限
            SubscriptionType.MONTHLY -> purchaseTime.plusMonths(1)
            SubscriptionType.YEARLY -> purchaseTime.plusYears(1)
            SubscriptionType.FREE -> null
        }
    }

    suspend fun restorePurchases(): Result<RestoreResult> {
        return try {
            if (!ensureConnected()) {
                return Result.failure(BillingException.ConnectionFailed("Failed to connect to billing service"))
            }

            val purchases = billingClientWrapper.queryPurchases()
            val activePurchase = purchases
                .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                .maxByOrNull { it.purchaseTime }

            if (activePurchase != null) {
                val processResult = processPurchase(activePurchase)
                when (processResult) {
                    is ProcessPurchaseResult.Success ->
                        Result.success(RestoreResult.Success(processResult.subscriptionType))
                    is ProcessPurchaseResult.Pending ->
                        Result.success(RestoreResult.Pending)
                    is ProcessPurchaseResult.Error ->
                        Result.success(RestoreResult.Error(processResult.message))
                }
            } else {
                Result.success(RestoreResult.NoPurchasesFound)
            }
        } catch (e: Exception) {
            Result.failure(BillingException.RestoreFailed(e.message ?: "Unknown error"))
        }
    }

    suspend fun refreshPurchaseStatus() {
        if (!ensureConnected()) return

        val purchases = billingClientWrapper.queryPurchases()
        val activePurchase = purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .maxByOrNull { it.purchaseTime }

        if (activePurchase != null) {
            processPurchase(activePurchase)
        }
    }

    data class ProductInfo(
        val type: SubscriptionType,
        val productId: String,
        val price: String,
        val priceMicros: Long,
        val priceCurrencyCode: String,
        val billingPeriod: String?,
        val offerToken: String?,
        val productDetails: ProductDetails
    )

    sealed class RestoreResult {
        data class Success(val subscriptionType: SubscriptionType) : RestoreResult()
        data object NoPurchasesFound : RestoreResult()
        data object Pending : RestoreResult()
        data class Error(val message: String) : RestoreResult()
    }

    sealed class ProcessPurchaseResult {
        data class Success(val subscriptionType: SubscriptionType) : ProcessPurchaseResult()
        data object Pending : ProcessPurchaseResult()
        data class Error(val message: String) : ProcessPurchaseResult()
    }

    sealed class BillingException(message: String) : Exception(message) {
        class ConnectionFailed(message: String) : BillingException(message)
        class QueryFailed(message: String) : BillingException(message)
        class InvalidProduct(message: String) : BillingException(message)
        class ProductNotFound(message: String) : BillingException(message)
        class PurchaseFailed(message: String) : BillingException(message)
        class UserCanceled(message: String) : BillingException(message)
        class AlreadyOwned(message: String) : BillingException(message)
        class RestoreFailed(message: String) : BillingException(message)
    }
}
