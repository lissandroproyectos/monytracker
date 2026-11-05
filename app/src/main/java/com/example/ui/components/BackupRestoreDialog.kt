package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CloudBackupItem
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.FinanceViewModel
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun BackupRestoreDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Exportar, 1 = Importar, 2 = Guardar Nube
    val context = LocalContext.current
    val jsonExport = remember { viewModel.exportBackupJson() }
    var importJsonText by remember { mutableStateOf("") }
    var backupNote by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp)
                .testTag("backup_restore_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Respaldos y Datos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondaryDark)
                    }
                }

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SlateCardLight,
                    contentColor = EmeraldLight,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Exportar (Offline)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Importar (Offline)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Nube (Google)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
                    )
                }

                when (selectedTab) {
                    0 -> { // EXPORT & MANUAL SYNC (OFFLINE - NO GOOGLE REQUIRED)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = EmeraldDark.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✅ No requiere cuenta de Google ni internet. Exporta tus datos para guardarlos en un archivo o transferirlos directamente a otro teléfono.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldLight,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SlateCardLight)
                                .border(1.dp, TextSecondaryDark.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = jsonExport,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimaryDark,
                                fontSize = 11.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Respaldo Finanzas", jsonExport)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Código de sincronización copiado", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).testTag("copy_backup_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copiar Código Sync", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_SUBJECT, "Sincronizar Finanzas")
                                        putExtra(Intent.EXTRA_TEXT, jsonExport)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Enviar Sync a otro dispositivo"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enviar a Otro Equipo", fontSize = 11.sp)
                            }
                        }
                    }

                    1 -> { // IMPORT & MANUAL SYNC (OFFLINE - NO GOOGLE REQUIRED)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = EmeraldDark.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✅ No requiere cuenta de Google ni internet. Pega el código exportado desde otro equipo y tus cuentas, cuotas y movimientos se restaurarán de inmediato.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldLight,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        OutlinedTextField(
                            value = importJsonText,
                            onValueChange = { importJsonText = it },
                            label = { Text("Código de Sincronización JSON") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .testTag("import_json_input"),
                            maxLines = 10
                        )

                        Button(
                            onClick = {
                                if (importJsonText.isNotBlank()) {
                                    viewModel.importBackup(importJsonText.trim())
                                    onDismiss()
                                }
                            },
                            enabled = importJsonText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().testTag("confirm_import_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sincronizar e Importar Datos")
                        }
                    }

                    2 -> { // CLOUD SNAPSHOT (REQUIRES GOOGLE ACCOUNT)
                        val cloudBackups by viewModel.cloudBackups.collectAsStateWithLifecycle()
                        val firebaseUser = remember {
                            try {
                                FirebaseAuth.getInstance().currentUser
                            } catch (e: Exception) {
                                null
                            }
                        }
                        var selectedSnapshotToRestore by remember { mutableStateOf<CloudBackupItem?>(null) }

                        if (viewModel.currentUserId == "local_guest" || firebaseUser == null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCardLight),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "ℹ️ Estás en Modo Local (Sin Cuenta)",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Para exportar o importar datos entre dispositivos de forma manual NO necesitas cuenta de Google (usa las pestañas Exportar e Importar de arriba). Para guardar y restaurar puntos automáticos en la nube de Firebase, inicia sesión con Google.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                        } else {
                            // User Info Header Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = EmeraldDark.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = EmeraldLight,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = firebaseUser.displayName.takeIf { !it.isNullOrBlank() } ?: "Cuenta de Google",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = firebaseUser.email ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = EmeraldLight
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(EmeraldLight.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = EmeraldLight,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Conectado",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldLight,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Section 1: Save Backup
                            Text(
                                text = "☁️ Guardar nuevo punto en la Nube",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                style = MaterialTheme.typography.titleSmall
                            )

                            OutlinedTextField(
                                value = backupNote,
                                onValueChange = { backupNote = it },
                                label = { Text("Nota o descripción (ej. Cierre Quincena)") },
                                modifier = Modifier.fillMaxWidth().testTag("backup_note_input"),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    viewModel.saveBackupSnapshot(backupNote.trim())
                                    backupNote = ""
                                },
                                modifier = Modifier.fillMaxWidth().testTag("save_cloud_backup_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Guardar Punto en la Nube")
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = TextSecondaryDark.copy(alpha = 0.2f)
                            )

                            // Section 2: Restore Backups List
                            Text(
                                text = "🔄 Puntos de Restauración en la Nube",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                style = MaterialTheme.typography.titleSmall
                            )

                            if (cloudBackups.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SlateCardLight,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Aún no has guardado puntos de respaldo en la nube. Presiona 'Guardar Punto en la Nube' para crear tu primer respaldo.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondaryDark,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    cloudBackups.forEach { item ->
                                        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(item.timestamp))
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = SlateCardLight),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = item.note.ifBlank { "Respaldo automático" },
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimaryDark,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    IconButton(
                                                        onClick = { viewModel.deleteCloudBackup(item) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Eliminar",
                                                            tint = TextSecondaryDark,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Fecha: $dateStr",
                                                    fontSize = 11.sp,
                                                    color = EmeraldLight
                                                )
                                                Text(
                                                    text = "${item.accountsCount} cuentas, ${item.commitmentsCount} deudas/compromisos, ${item.transactionsCount} movimientos",
                                                    fontSize = 11.sp,
                                                    color = TextSecondaryDark
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))
                                                Button(
                                                    onClick = { selectedSnapshotToRestore = item },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                                                ) {
                                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Restaurar este Punto", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Confirmation Dialog for Cloud Restore
                            selectedSnapshotToRestore?.let { itemToRestore ->
                                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(itemToRestore.timestamp))
                                AlertDialog(
                                    onDismissRequest = { selectedSnapshotToRestore = null },
                                    title = { Text("Restaurar Punto de la Nube", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
                                    text = {
                                        Text(
                                            text = "¿Deseas restaurar el respaldo '${itemToRestore.note.ifBlank { "Respaldo" }}' creado el $dateStr?\n\nEsta acción reemplazará las cuentas y deudas actuales con los datos guardados en esa fecha.",
                                            color = TextSecondaryDark
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                viewModel.restoreCloudBackup(itemToRestore)
                                                selectedSnapshotToRestore = null
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                        ) {
                                            Text("Confirmar Restauración")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { selectedSnapshotToRestore = null }) {
                                            Text("Cancelar", color = TextSecondaryDark)
                                        }
                                    },
                                    containerColor = SlateCard
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
