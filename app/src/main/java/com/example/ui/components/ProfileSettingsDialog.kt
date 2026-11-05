package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.currency.AppCurrency
import com.example.data.update.AppUpdateManager
import androidx.compose.runtime.remember
import com.example.ui.theme.AccentRed
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileSettingsDialog(
    displayCurrency: AppCurrency,
    paymentCurrency: AppCurrency,
    onOpenCurrencySettings: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenUpdate: () -> Unit,
    onSignOutClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val firebaseUser = remember {
        try {
            FirebaseAuth.getInstance().currentUser
        } catch (e: Exception) {
            null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .testTag("profile_settings_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ajustes y Perfil",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondaryDark)
                    }
                }

                // Profile Summary Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = EmeraldDark.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val userName = firebaseUser?.displayName
                            val profileTitle = if (!userName.isNullOrBlank()) {
                                userName
                            } else if (firebaseUser != null) {
                                "Usuario de Google"
                            } else {
                                "Modo Local (Sin Cuenta)"
                            }
                            Text(
                                text = profileTitle,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = firebaseUser?.email ?: "Datos guardados en tu dispositivo",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldLight
                            )
                        }
                        if (firebaseUser != null) {
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
                                        text = "Google Sync",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldLight,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Ajustes Principales",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryDark
                )

                // Option 1: Currency Settings
                SettingsOptionItem(
                    icon = Icons.Default.CurrencyExchange,
                    title = "Configuración Monetaria",
                    subtitle = "Ver en: ${displayCurrency.displayName} • Pagar en: ${paymentCurrency.displayName}",
                    onClick = {
                        onDismiss()
                        onOpenCurrencySettings()
                    }
                )

                // Option 2: Backups & Cloud Sync
                SettingsOptionItem(
                    icon = Icons.Default.Backup,
                    title = "Respaldos y Datos (Nube / Offline)",
                    subtitle = "Exportar, importar o guardar puntos de restauración en la nube",
                    onClick = {
                        onDismiss()
                        onOpenBackup()
                    }
                )

                // Option 3: Check App Updates
                SettingsOptionItem(
                    icon = Icons.Default.SystemUpdate,
                    title = "Actualizaciones de la App",
                    subtitle = "Versión actual v${AppUpdateManager.CURRENT_VERSION_NAME} (Verificar GitHub Releases)",
                    onClick = {
                        onDismiss()
                        onOpenUpdate()
                    }
                )

                HorizontalDivider(color = TextSecondaryDark.copy(alpha = 0.2f))

                // Option 4: Sign Out
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onSignOutClick()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("profile_sign_out_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingsOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = SlateCardLight,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EmeraldDark.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 14.sp)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark, fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondaryDark)
        }
    }
}
