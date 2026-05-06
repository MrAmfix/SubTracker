package com.mramfix.subtracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.mramfix.subtracker.domain.model.AppThemeMode
import com.mramfix.subtracker.domain.model.CurrencyCode
import com.mramfix.subtracker.domain.model.NotificationLead
import com.mramfix.subtracker.presentation.SettingsViewModel
import com.mramfix.subtracker.ui.components.MenuField
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var pendingGoogleAction by remember { mutableStateOf<GoogleAction?>(null) }
    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        val json = pendingExport ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
        }
        pendingExport = null
    }
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (text != null) viewModel.importJson(text)
        }
    }
    val googleSignIn = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (pendingGoogleAction) {
            GoogleAction.EXPORT -> viewModel.exportToGoogle(result.data)
            GoogleAction.IMPORT -> viewModel.importFromGoogle(result.data)
            null -> Unit
        }
        pendingGoogleAction = null
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Внешний вид", style = MaterialTheme.typography.titleMedium)
            MenuField(
                label = "Тема",
                value = state.settings.themeMode,
                options = AppThemeMode.entries,
                optionLabel = {
                    when (it) {
                        AppThemeMode.SYSTEM -> "Как в системе"
                        AppThemeMode.LIGHT -> "Светлая"
                        AppThemeMode.DARK -> "Темная"
                    }
                },
                onSelected = viewModel::updateTheme,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Валюта", style = MaterialTheme.typography.titleMedium)
            MenuField(
                label = "Основная валюта",
                value = state.settings.baseCurrency,
                options = CurrencyCode.entries,
                onSelected = viewModel::updateBaseCurrency,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = viewModel::refreshRates, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text("Обновить курсы валют")
            }
            Text("Уведомления", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Включить уведомления")
                Switch(
                    checked = state.settings.notificationsEnabled,
                    onCheckedChange = viewModel::updateNotificationsEnabled
                )
            }
            MenuField(
                label = "Предупреждать",
                value = state.settings.notificationLead,
                options = NotificationLead.entries,
                optionLabel = {
                    when (it) {
                        NotificationLead.SAME_DAY -> "В день оплаты"
                        NotificationLead.ONE_DAY -> "За 1 день"
                        NotificationLead.THREE_DAYS -> "За 3 дня"
                        NotificationLead.SEVEN_DAYS -> "За 7 дней"
                    }
                },
                onSelected = viewModel::updateNotificationLead,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Импорт и экспорт", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = {
                    scope.launch {
                        pendingExport = viewModel.exportJson()
                        createDocument.launch("subtracker-backup.json")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Text("Сохранить в JSON")
            }
            OutlinedButton(
                onClick = { openDocument.launch(arrayOf("application/json", "text/*", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Text("Загрузить из JSON")
            }
            Button(
                onClick = {
                    pendingGoogleAction = GoogleAction.EXPORT
                    googleSignIn.launch(viewModel.googleSignInIntent())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                GoogleFavicon()
                Text("Сохранить в Google")
            }
            OutlinedButton(
                onClick = {
                    pendingGoogleAction = GoogleAction.IMPORT
                    googleSignIn.launch(viewModel.googleSignInIntent())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                GoogleFavicon()
                Text("Загрузить из Google")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Автоматическая синхронизация", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (state.googleSignedIn) {
                            "Экспортировать backup в Google Drive после изменений подписок"
                        } else {
                            "Доступно после входа через Google"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = state.settings.autoGoogleSyncEnabled && state.googleSignedIn,
                    onCheckedChange = viewModel::updateAutoGoogleSyncEnabled,
                    enabled = state.googleSignedIn
                )
            }
            HorizontalDivider(modifier = Modifier.padding(top = 10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                Text("О приложении", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Версия 1.1.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(
                    onClick = { uriHandler.openUri("https://github.com/MrAmfix/SubTracker") },
                ) {
                    Text("Исходный код на GitHub")
                }
            }
        }
    }
}

@Composable
private fun GoogleFavicon() {
    val context = LocalContext.current
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data("https://www.google.com/favicon.ico")
            .crossfade(true)
            .build(),
        contentDescription = null,
        modifier = Modifier
            .padding(end = 8.dp)
            .size(18.dp),
        contentScale = ContentScale.Fit
    )
}

private enum class GoogleAction {
    EXPORT,
    IMPORT
}
