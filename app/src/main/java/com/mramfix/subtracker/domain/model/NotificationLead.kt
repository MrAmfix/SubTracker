package com.mramfix.subtracker.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class NotificationLead(val daysBefore: Int) {
    SAME_DAY(0),
    ONE_DAY(1),
    THREE_DAYS(3),
    SEVEN_DAYS(7)
}
