package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.update.AppUpdateManager
import com.example.data.update.GitHubReleaseInfo
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AppUpdateDialog(
    currentRepo: String = AppUpdateManager.DEFAULT_GITHUB_REPO,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val targetRepo = AppUpdateManager.DEFAULT_GITHUB_REPO

    var isChecking by remember { mutableStateOf(false) }
    var releaseInfo by remember { mutableStateOf<GitHubReleaseInfo?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadedApkFile by remember { mutableStateOf<File?>(null) }

    val checkForUpdates = {
        isChecking = true
        errorMessage = null
        releaseInfo = null
        scope.launch {
            val result = AppUpdateManager.checkLatestRelease(targetRepo)
            isChecking = false
            result.fold(
                onSuccess = { info -> releaseInfo = info },
                onFailure = { err -> errorMessage = err.localizedMessage }
            )
        }
    }

    LaunchedEffect(Unit) {
        checkForUpdates()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("app_update_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = EmeraldLight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Actualizaciones de App",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondaryDark)
                    }
                }

                Surface(
                    color = SlateCardLight,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Versión instalada: v${AppUpdateManager.CURRENT_VERSION_NAME}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Conectado automáticamente con GitHub Releases ($targetRepo).",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }

                if (isChecking) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = EmeraldLight, strokeWidth = 2.5.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Buscando nueva versión en GitHub...", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                    }
                } else {
                    Button(
                        onClick = { checkForUpdates() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("check_update_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Comprobar Actualizaciones")
                    }
                }

                errorMessage?.let { err ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                releaseInfo?.let { info ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCardLight),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Última versión disponible: ${info.tagName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldLight
                                )
                            }

                            if (info.releaseNotes.isNotBlank()) {
                                Text(
                                    text = "Novedades de la versión:\n${info.releaseNotes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimaryDark,
                                    fontSize = 11.sp
                                )
                            }

                            if (isDownloading) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Descargando APK... ${(downloadProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EmeraldLight
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = downloadProgress,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = EmeraldPrimary
                                    )
                                }
                            } else if (downloadedApkFile != null) {
                                Button(
                                    onClick = {
                                        AppUpdateManager.installApk(context, downloadedApkFile!!)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("install_apk_btn")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Instalar Actualización APK ahora")
                                }
                            } else if (info.apkDownloadUrl.isNotBlank()) {
                                Button(
                                    onClick = {
                                        isDownloading = true
                                        downloadProgress = 0f
                                        scope.launch {
                                            val dlResult = AppUpdateManager.downloadApk(context, info.apkDownloadUrl) { prog ->
                                                downloadProgress = prog
                                            }
                                            isDownloading = false
                                            dlResult.fold(
                                                onSuccess = { apkFile ->
                                                    downloadedApkFile = apkFile
                                                    AppUpdateManager.installApk(context, apkFile)
                                                },
                                                onFailure = { err ->
                                                    errorMessage = "Error al descargar APK: ${err.localizedMessage}"
                                                }
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("download_apk_btn")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("🚀 Descargar e Instalar APK (${info.tagName})")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cerrar")
                }
            }
        }
    }
}
