package com.mramfix.subtracker.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mramfix.subtracker.SubTrackerApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as SubTrackerApplication).container
    private val draftDays = MutableStateFlow<SalaryDraft?>(null)

    val uiState = combine(
        container.subscriptionRepository.observeAll(),
        container.settingsRepository.settings,
        container.currencyRepository.rates,
        draftDays
    ) { subscriptions, settings, rates, draft ->
        val stats = StatisticsCalculator.detailed(subscriptions, settings, rates)
        val currentDraft = draft ?: SalaryDraft(stats.salaryDayText, stats.advanceDayText)
        stats.copy(
            salaryDayText = currentDraft.salaryDayText,
            advanceDayText = currentDraft.advanceDayText,
            inputError = validateDraft(currentDraft) ?: stats.inputError
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailedStatsUi())

    fun updateSalaryDay(value: String) {
        draftDays.update { (it ?: currentDraft()).copy(salaryDayText = value.filterDigits(maxLength = 2)) }
    }

    fun updateAdvanceDay(value: String) {
        draftDays.update { (it ?: currentDraft()).copy(advanceDayText = value.filterDigits(maxLength = 2)) }
    }

    fun saveDays() {
        val draft = draftDays.value ?: currentDraft()
        val error = validateDraft(draft)
        if (error != null) {
            draftDays.value = draft
            return
        }
        viewModelScope.launch {
            container.settingsRepository.updateSalaryDay(draft.salaryDayText.toInt())
            container.settingsRepository.updateAdvanceDay(draft.advanceDayText.toInt())
            draftDays.value = null
        }
    }

    private fun currentDraft(): SalaryDraft {
        val state = uiState.value
        return SalaryDraft(state.salaryDayText, state.advanceDayText)
    }

    private fun validateDraft(draft: SalaryDraft): String? {
        val salaryDay = draft.salaryDayText.toIntOrNull()
        val advanceDay = draft.advanceDayText.toIntOrNull()
        return when {
            salaryDay == null || advanceDay == null -> "Заполните оба дня"
            salaryDay !in 1..31 || advanceDay !in 1..31 -> "Дни должны быть от 1 до 31"
            salaryDay == advanceDay -> "Дни зарплаты и аванса не должны совпадать"
            else -> null
        }
    }

    private fun String.filterDigits(maxLength: Int): String = filter { it.isDigit() }.take(maxLength)

    private data class SalaryDraft(
        val salaryDayText: String,
        val advanceDayText: String
    )
}
