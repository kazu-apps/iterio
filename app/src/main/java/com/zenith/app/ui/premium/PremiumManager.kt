package com.zenith.app.ui.premium

import com.zenith.app.domain.model.PremiumFeature
import com.zenith.app.domain.model.SubscriptionStatus
import com.zenith.app.domain.repository.PremiumRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PremiumManager @Inject constructor(
    private val premiumRepository: PremiumRepository
) {
    val subscriptionStatus: Flow<SubscriptionStatus> = premiumRepository.subscriptionStatus

    suspend fun isPremium(): Boolean = premiumRepository.getSubscriptionStatus().isPremium

    suspend fun getSubscriptionStatus(): SubscriptionStatus = premiumRepository.getSubscriptionStatus()

    suspend fun canAccessFeature(feature: PremiumFeature): Boolean {
        return premiumRepository.canAccessFeature(feature)
    }

    suspend fun startTrial() {
        premiumRepository.startTrial()
    }

    suspend fun markTrialOfferSeen() {
        premiumRepository.markTrialOfferSeen()
    }

    fun getReviewIntervals(isPremium: Boolean): List<Int> {
        return if (isPremium) {
            listOf(1, 3, 7, 14, 30, 60) // 全6回
        } else {
            listOf(1, 3) // 無料版は2回まで
        }
    }

    companion object {
        val PREMIUM_REVIEW_INTERVALS = listOf(1, 3, 7, 14, 30, 60)
        val FREE_REVIEW_INTERVALS = listOf(1, 3)
    }
}
