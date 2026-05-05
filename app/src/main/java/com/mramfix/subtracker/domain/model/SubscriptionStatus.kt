package com.mramfix.subtracker.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SubscriptionStatus {
    ACTIVE, INACTIVE
}
