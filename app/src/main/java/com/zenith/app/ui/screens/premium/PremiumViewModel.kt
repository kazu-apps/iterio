package com.zenith.app.ui.screens.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenith.app.domain.model.SubscriptionStatus
import com.zenith.app.domain.model.SubscriptionType
import com.zenith.app.domain.usecase.BillingUseCase
import com.zenith.app.ui.premium.PremiumManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val premiumManager: PremiumManager,
    private val billingUseCase: BillingUseCase
) : ViewModel() {

    val subscriptionStatus: StateFlow<SubscriptionStatus> = premiumManager.subscriptionStatus
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SubscriptionStatus()
        )

    private val _products = MutableStateFlow<List<BillingUseCase.ProductInfo>>(emptyList())
    val products: StateFlow<List<BillingUseCase.ProductInfo>> = _products.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private val _billingError = MutableSharedFlow<BillingError>()
    val billingError: SharedFlow<BillingError> = _billingError.asSharedFlow()

    init {
        loadProducts()
        observePurchases()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _purchaseState.value = PurchaseState.Loading
            billingUseCase.getAvailableProducts()
                .onSuccess { productList ->
                    _products.value = productList
                    _purchaseState.value = PurchaseState.Idle
                }
                .onFailure { error ->
                    _purchaseState.value = PurchaseState.Idle
                    _billingError.emit(BillingError.ProductLoadFailed(error.message))
                }
        }
    }

    private fun observePurchases() {
        viewModelScope.launch {
            billingUseCase.newPurchases.collect { purchases ->
                purchases.forEach { purchase ->
                    val result = billingUseCase.processPurchase(purchase)
                    when (result) {
                        is BillingUseCase.ProcessPurchaseResult.Success -> {
                            _purchaseState.value = PurchaseState.Success(result.subscriptionType)
                        }
                        is BillingUseCase.ProcessPurchaseResult.Pending -> {
                            _purchaseState.value = PurchaseState.Pending
                        }
                        is BillingUseCase.ProcessPurchaseResult.Error -> {
                            _purchaseState.value = PurchaseState.Error(result.message)
                        }
                    }
                }
            }
        }
    }

    fun startTrial() {
        viewModelScope.launch {
            premiumManager.startTrial()
        }
    }

    fun purchase(activity: Activity, subscriptionType: SubscriptionType) {
        viewModelScope.launch {
            _purchaseState.value = PurchaseState.Processing
            billingUseCase.startPurchase(activity, subscriptionType)
                .onSuccess {
                    // 購入フローが開始された
                    // 実際の結果はobservePurchasesで処理される
                }
                .onFailure { error ->
                    when (error) {
                        is BillingUseCase.BillingException.UserCanceled -> {
                            _purchaseState.value = PurchaseState.Idle
                        }
                        is BillingUseCase.BillingException.AlreadyOwned -> {
                            _purchaseState.value = PurchaseState.AlreadyOwned
                            _billingError.emit(BillingError.AlreadyOwned)
                        }
                        else -> {
                            _purchaseState.value = PurchaseState.Error(error.message ?: "Unknown error")
                            _billingError.emit(BillingError.PurchaseFailed(error.message))
                        }
                    }
                }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            _purchaseState.value = PurchaseState.Restoring
            billingUseCase.restorePurchases()
                .onSuccess { result ->
                    when (result) {
                        is BillingUseCase.RestoreResult.Success -> {
                            _purchaseState.value = PurchaseState.Restored(result.subscriptionType)
                        }
                        is BillingUseCase.RestoreResult.NoPurchasesFound -> {
                            _purchaseState.value = PurchaseState.NoPurchasesFound
                        }
                        is BillingUseCase.RestoreResult.Pending -> {
                            _purchaseState.value = PurchaseState.Pending
                        }
                        is BillingUseCase.RestoreResult.Error -> {
                            _purchaseState.value = PurchaseState.Error(result.message)
                        }
                    }
                }
                .onFailure { error ->
                    _purchaseState.value = PurchaseState.Error(error.message ?: "Unknown error")
                    _billingError.emit(BillingError.RestoreFailed(error.message))
                }
        }
    }

    fun resetPurchaseState() {
        _purchaseState.value = PurchaseState.Idle
    }

    fun refreshProducts() {
        loadProducts()
    }

    sealed class PurchaseState {
        data object Idle : PurchaseState()
        data object Loading : PurchaseState()
        data object Processing : PurchaseState()
        data object Restoring : PurchaseState()
        data object Pending : PurchaseState()
        data object AlreadyOwned : PurchaseState()
        data object NoPurchasesFound : PurchaseState()
        data class Success(val type: SubscriptionType) : PurchaseState()
        data class Restored(val type: SubscriptionType) : PurchaseState()
        data class Error(val message: String) : PurchaseState()
    }

    sealed class BillingError {
        data class ProductLoadFailed(val message: String?) : BillingError()
        data class PurchaseFailed(val message: String?) : BillingError()
        data class RestoreFailed(val message: String?) : BillingError()
        data object AlreadyOwned : BillingError()
    }
}
