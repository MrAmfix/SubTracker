package com.mramfix.subtracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.mramfix.subtracker.domain.model.SortMode
import com.mramfix.subtracker.domain.model.SubscriptionStatus
import com.mramfix.subtracker.image.subscriptionIconCacheKey
import com.mramfix.subtracker.presentation.HomeViewModel
import com.mramfix.subtracker.presentation.PaymentCountdownUrgency
import com.mramfix.subtracker.presentation.HomeStatsSummaryUi
import com.mramfix.subtracker.presentation.SubscriptionListItemUi
import com.mramfix.subtracker.presentation.isPaymentTodayOrTomorrow
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onSettings: () -> Unit,
    onStats: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SubTracker") },
                actions = {
                    IconButton(onClick = viewModel::refreshRates) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить курсы")
                    }
                    IconButton(onClick = onStats) {
                        Icon(Icons.Default.QueryStats, contentDescription = "Статистика")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.refreshingRates) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            SummaryCard(summary = state.statsSummary)
            SortRow(selected = state.sortMode, onSelected = viewModel::setSortMode)
            if (state.items.isEmpty()) {
                EmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.items, key = { it.subscription.id }) { item ->
                        SubscriptionCard(
                            item = item,
                            onEdit = { onEdit(item.subscription.id) },
                            onDelete = { viewModel.delete(item.subscription.id) },
                            onActiveChange = { viewModel.setActive(item.subscription.id, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: HomeStatsSummaryUi) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryValue(
                title = "Активные",
                value = summary.activeTotalText,
                modifier = Modifier.weight(1f)
            )
            SummaryValue(
                title = "Все",
                value = summary.allTotalText,
                modifier = Modifier.weight(1f)
            )
        }
        if (summary.hasMissingRates) {
            Text(
                text = "Часть сумм недоступна без курсов валют",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun SummaryValue(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SortRow(selected: SortMode, onSelected: (SortMode) -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        SortChip("Дата оплаты", SortMode.NEXT_PAYMENT, selected, onSelected)
        SortChip("День оплаты", SortMode.PAYMENT_DAY, selected, onSelected)
        SortChip("Стоимость", SortMode.COST, selected, onSelected)
        SortChip("Алфавит", SortMode.NAME, selected, onSelected)
    }
}

@Composable
private fun SortChip(label: String, mode: SortMode, selected: SortMode, onSelected: (SortMode) -> Unit) {
    FilterChip(
        selected = selected == mode,
        onClick = { onSelected(mode) },
        label = { Text(label) }
    )
}

@Composable
private fun SubscriptionCard(
    item: SubscriptionListItemUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onActiveChange: (Boolean) -> Unit
) {
    val subscription = item.subscription
    val isDueTodayOrTomorrow = isPaymentTodayOrTomorrow(
        paymentDate = LocalDate.ofEpochDay(subscription.nextPaymentEpochDay),
        today = LocalDate.now()
    )
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = if (isDueTodayOrTomorrow) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SubscriptionIcon(
                id = subscription.id,
                name = subscription.name,
                iconUri = subscription.iconUri,
                modifier = Modifier.size(44.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    subscription.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.nextPaymentText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                item.paymentCountdown?.let { countdown ->
                    Text(
                        text = countdown.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = countdown.urgency.color(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                subscription.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(
                modifier = Modifier.widthIn(min = 84.dp, max = 130.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    item.priceText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                item.convertedPriceText?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Switch(
                    checked = subscription.status == SubscriptionStatus.ACTIVE,
                    onCheckedChange = onActiveChange
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionIcon(
    id: Long,
    name: String,
    iconUri: String?,
    modifier: Modifier = Modifier
) {
    val cleanUri = iconUri?.trim().orEmpty()
    if (cleanUri.isBlank()) {
        InitialAvatar(name = name, modifier = modifier)
        return
    }
    val context = LocalContext.current
    val cacheKey = remember(id, cleanUri) { subscriptionIconCacheKey(id, cleanUri) }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(cleanUri)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .build(),
        contentDescription = "Иконка подписки",
        modifier = modifier.clip(CircleShape),
        contentScale = ContentScale.Crop,
        loading = { InitialAvatar(name = name, modifier = Modifier.fillMaxSize()) },
        error = { InitialAvatar(name = name, modifier = Modifier.fillMaxSize()) }
    )
}

@Composable
private fun PaymentCountdownUrgency.color(): Color {
    return when (this) {
        PaymentCountdownUrgency.RED -> MaterialTheme.colorScheme.error
        PaymentCountdownUrgency.ORANGE -> Color(0xFFE8710A)
        PaymentCountdownUrgency.YELLOW -> Color(0xFFB68A00)
    }
}

@Composable
private fun InitialAvatar(name: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clip(CircleShape),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Подписок пока нет", style = MaterialTheme.typography.headlineSmall)
            Text("Добавьте первую подписку, чтобы видеть платежи и напоминания.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
