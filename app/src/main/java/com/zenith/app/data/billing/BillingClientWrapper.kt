package com.zenith.app.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BillingClientWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var billingClient: BillingClient? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _purchases = MutableSharedFlow<List<Purchase>>(replay = 1)
    val purchases: SharedFlow<List<Purchase>> = _purchases.asSharedFlow()

    private val _newPurchases = MutableSharedFlow<List<Purchase>>()
    val newPurchases: SharedFlow<List<Purchase>> = _newPurchases.asSharedFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            _newPurchases.tryEmit(purchases)
            _purchases.tryEmit(purchases)
        }
    }

    suspend fun startConnection(): Boolean {
        if (_connectionState.value == ConnectionState.Connected) {
            return true
        }

        _connectionState.value = ConnectionState.Connecting

        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .enablePrepaidPlans()
                    .build()
            )
            .build()

        return suspendCancellableCoroutine { continuation ->
            billingClient?.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _connectionState.value = ConnectionState.Connected
                        continuation.resume(true)
                    } else {
                        _connectionState.value = ConnectionState.Error(billingResult.responseCode)
                        continuation.resume(false)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _connectionState.value = ConnectionState.Disconnected
                }
            })
        }
    }

    fun endConnection() {
        billingClient?.endConnection()
        billingClient = null
        _connectionState.value = ConnectionState.Disconnected
    }

    suspend fun querySubscriptionDetails(): List<ProductDetails> {
        val client = billingClient ?: return emptyList()
        if (_connectionState.value != ConnectionState.Connected) return emptyList()

        val productList = BillingProducts.SUBSCRIPTION_SKUS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = client.queryProductDetails(params)
        return if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            result.productDetailsList ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun queryInAppProductDetails(): List<ProductDetails> {
        val client = billingClient ?: return emptyList()
        if (_connectionState.value != ConnectionState.Connected) return emptyList()

        val productList = BillingProducts.INAPP_SKUS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = client.queryProductDetails(params)
        return if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            result.productDetailsList ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun queryAllProductDetails(): List<ProductDetails> {
        val subscriptions = querySubscriptionDetails()
        val inAppProducts = queryInAppProductDetails()
        return subscriptions + inAppProducts
    }

    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String? = null
    ): BillingResult {
        val client = billingClient ?: return BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
            .build()

        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        // サブスクリプションの場合はofferTokenが必要
        if (offerToken != null) {
            productDetailsParamsBuilder.setOfferToken(offerToken)
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))
            .build()

        return client.launchBillingFlow(activity, billingFlowParams)
    }

    suspend fun queryPurchases(): List<Purchase> {
        val client = billingClient ?: return emptyList()
        if (_connectionState.value != ConnectionState.Connected) return emptyList()

        val allPurchases = mutableListOf<Purchase>()

        // サブスクリプションの購入履歴
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val subsResult = client.queryPurchasesAsync(subsParams)
        if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            allPurchases.addAll(subsResult.purchasesList)
        }

        // 一回購入の購入履歴
        val inappParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val inappResult = client.queryPurchasesAsync(inappParams)
        if (inappResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            allPurchases.addAll(inappResult.purchasesList)
        }

        _purchases.emit(allPurchases)
        return allPurchases
    }

    suspend fun acknowledgePurchase(purchaseToken: String): BillingResult {
        val client = billingClient ?: return BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
            .build()

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        return client.acknowledgePurchase(params)
    }

    fun isReady(): Boolean = billingClient?.isReady == true

    sealed class ConnectionState {
        data object Disconnected : ConnectionState()
        data object Connecting : ConnectionState()
        data object Connected : ConnectionState()
        data class Error(val responseCode: Int) : ConnectionState()
    }
}
