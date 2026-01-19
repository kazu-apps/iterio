package com.zenith.app.data.billing

import com.zenith.app.domain.model.SubscriptionType

object BillingProducts {
    // サブスクリプション商品ID
    const val SUBSCRIPTION_MONTHLY = "zenith_premium_monthly"
    const val SUBSCRIPTION_YEARLY = "zenith_premium_yearly"

    // 一回購入（In-App Product）商品ID
    const val INAPP_LIFETIME = "zenith_premium_lifetime"

    // 商品ID一覧
    val SUBSCRIPTION_SKUS = listOf(SUBSCRIPTION_MONTHLY, SUBSCRIPTION_YEARLY)
    val INAPP_SKUS = listOf(INAPP_LIFETIME)
    val ALL_SKUS = SUBSCRIPTION_SKUS + INAPP_SKUS

    // SubscriptionTypeとの相互変換
    fun toSubscriptionType(productId: String): SubscriptionType = when (productId) {
        SUBSCRIPTION_MONTHLY -> SubscriptionType.MONTHLY
        SUBSCRIPTION_YEARLY -> SubscriptionType.YEARLY
        INAPP_LIFETIME -> SubscriptionType.LIFETIME
        else -> SubscriptionType.FREE
    }

    fun toProductId(type: SubscriptionType): String? = when (type) {
        SubscriptionType.MONTHLY -> SUBSCRIPTION_MONTHLY
        SubscriptionType.YEARLY -> SUBSCRIPTION_YEARLY
        SubscriptionType.LIFETIME -> INAPP_LIFETIME
        SubscriptionType.FREE -> null
    }

    fun isSubscription(productId: String): Boolean = productId in SUBSCRIPTION_SKUS
    fun isInApp(productId: String): Boolean = productId in INAPP_SKUS
}
