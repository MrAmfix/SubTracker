package com.mramfix.subtracker.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mramfix.subtracker.domain.model.BillingPeriodType
import com.mramfix.subtracker.domain.model.CurrencyCode
import com.mramfix.subtracker.presentation.EditSubscriptionViewModel
import com.mramfix.subtracker.presentation.parseDisplayDate
import com.mramfix.subtracker.ui.components.MenuField
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSubscriptionScreen(
    onBack: () -> Unit,
    viewModel: EditSubscriptionViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.updateIconUri(it.toString())
        }
    }
    if (showDatePicker) {
        val selectedDate = parseDisplayDate(state.nextPaymentDate) ?: LocalDate.now()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toUtcMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = datePickerState.selectedDateMillis?.toLocalDateUtc()
                        if (picked != null) viewModel.updateNextPaymentDate(picked)
                        showDatePicker = false
                    }
                ) {
                    Text("Готово")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.saved.collect { onBack() }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.id == 0L) "Новая подписка" else "Редактирование") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::save) {
                        Icon(Icons.Default.Save, contentDescription = "Сохранить")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text("Название") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.iconUri,
                    onValueChange = viewModel::updateIconUri,
                    label = { Text("Иконка/фото URI") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { imagePicker.launch(arrayOf("image/*")) }) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Text("Выбрать")
                }
            }
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Описание") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.cost,
                    onValueChange = viewModel::updateCost,
                    label = { Text("Стоимость") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                MenuField(
                    label = "Валюта",
                    value = state.currency,
                    options = CurrencyCode.entries,
                    onSelected = viewModel::updateCurrency,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.nextPaymentDate,
                    onValueChange = {},
                    label = { Text("Ближайшая дата оплаты") },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Text("Дата")
                }
            }
            MenuField(
                label = "Период оплаты",
                value = state.billingType,
                options = BillingPeriodType.entries,
                optionLabel = {
                    when (it) {
                        BillingPeriodType.WEEKLY -> "Еженедельно"
                        BillingPeriodType.MONTHLY_FIXED -> "Каждые N дней"
                        BillingPeriodType.MONTHLY_CALENDAR_DAY -> "Каждый месяц в число"
                        BillingPeriodType.YEARLY -> "Ежегодно"
                        BillingPeriodType.CUSTOM -> "Произвольный интервал"
                    }
                },
                onSelected = viewModel::updateBillingType,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.billingType == BillingPeriodType.MONTHLY_FIXED || state.billingType == BillingPeriodType.CUSTOM) {
                OutlinedTextField(
                    value = state.intervalDays,
                    onValueChange = viewModel::updateIntervalDays,
                    label = { Text("Интервал в днях") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (state.billingType == BillingPeriodType.MONTHLY_CALENDAR_DAY) {
                OutlinedTextField(
                    value = state.calendarDay,
                    onValueChange = viewModel::updateCalendarDay,
                    label = { Text("День месяца, 1-31") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Активная подписка", style = MaterialTheme.typography.titleMedium)
                    Text("Неактивные подписки не создают уведомления", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = state.active,
                    onCheckedChange = viewModel::updateActive,
                    modifier = Modifier.widthIn(max = 64.dp)
                )
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Save, contentDescription = null)
                Text("Сохранить")
            }
        }
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDateUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
