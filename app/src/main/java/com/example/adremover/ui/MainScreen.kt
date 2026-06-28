package com.example.adremover.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.adremover.MainViewModel
import com.example.adremover.core.AdRemoverEngine
import com.example.adremover.model.AppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val apps by viewModel.apps.collectAsState()
    val processState by viewModel.processState.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("APK去广告工具") },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshApps() },
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.Refresh, "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索应用...") },
                leadingIcon = { Icon(Icons.Default.Search, "搜索") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, "清除")
                        }
                    }
                },
                singleLine = true
            )
            
            if (processState !is AdRemoverEngine.ProcessState.Idle) {
                ProcessStateCard(
                    state = processState,
                    onDismiss = { viewModel.clearState() }
                )
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = apps.filter { app ->
                        searchQuery.isEmpty() ||
                        app.appName.contains(searchQuery, ignoreCase = true) ||
                        app.packageName.contains(searchQuery, ignoreCase = true)
                    },
                    key = { it.packageName }
                ) { app ->
                    AppListItem(
                        app = app,
                        isProcessing = isProcessing && viewModel.currentPackage == app.packageName,
                        onRemoveAd = { viewModel.removeAd(app.packageName) },
                        onAnalyze = { viewModel.analyzeApp(app.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProcessStateCard(
    state: AdRemoverEngine.ProcessState,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                is AdRemoverEngine.ProcessState.Success -> MaterialTheme.colorScheme.primaryContainer
                is AdRemoverEngine.ProcessState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (state) {
                is AdRemoverEngine.ProcessState.Success -> {
                    Icon(Icons.Default.CheckCircle, "成功", tint = MaterialTheme.colorScheme.primary)
                }
                is AdRemoverEngine.ProcessState.Error -> {
                    Icon(Icons.Default.Error, "错误", tint = MaterialTheme.colorScheme.error)
                }
                else -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (state) {
                        is AdRemoverEngine.ProcessState.Extracting -> "正在提取APK..."
                        is AdRemoverEngine.ProcessState.Analyzing -> "正在分析"
                        is AdRemoverEngine.ProcessState.Patching -> "正在修改"
                        is AdRemoverEngine.ProcessState.Signing -> "正在签名..."
                        is AdRemoverEngine.ProcessState.Success -> "处理完成"
                        is AdRemoverEngine.ProcessState.Error -> "处理失败"
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                when (state) {
                    is AdRemoverEngine.ProcessState.Analyzing -> {
                        Text(text = state.message, style = MaterialTheme.typography.bodySmall)
                    }
                    is AdRemoverEngine.ProcessState.Patching -> {
                        Text(text = state.message, style = MaterialTheme.typography.bodySmall)
                    }
                    is AdRemoverEngine.ProcessState.Success -> {
                        Text(text = "${state.appName} - 已保存到下载/AdRemover/", style = MaterialTheme.typography.bodySmall)
                    }
                    is AdRemoverEngine.ProcessState.Error -> {
                        Text(text = state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    else -> {}
                }
            }
            
            if (state is AdRemoverEngine.ProcessState.Success || state is AdRemoverEngine.ProcessState.Error) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "关闭")
                }
            }
        }
    }
}

@Composable
fun AppListItem(
    app: AppInfo,
    isProcessing: Boolean,
    onRemoveAd: () -> Unit,
    onAnalyze: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = app.icon.toBitmap(128, 128).asImageBitmap(),
                contentDescription = app.appName,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = app.appName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(text = app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "v${app.versionName} | ${formatFileSize(app.apkSize)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onAnalyze) {
                    Icon(Icons.Default.Info, "分析", tint = MaterialTheme.colorScheme.secondary)
                }
                Button(onClick = onRemoveAd) {
                    Text("去广告")
                }
            }
        }
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
    }
}
