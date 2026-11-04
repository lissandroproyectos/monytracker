package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private const val TAG = "AuthScreen"

@Composable
fun AuthScreen(
    onSignInSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    fun launchGoogleSignIn() {
        val webClientId = run {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                try { context.getString(resId) } catch (e: Exception) { "" }
            } else {
                "166010243644-q8chp7cgoo1ltng0fi7u3h8gi9jm8hhr.apps.googleusercontent.com"
            }
        }

        if (webClientId.isBlank()) {
            errorMessage = "No se encontró el ID de cliente de Google en los recursos del sistema. Puedes usar Modo Local."
            return
        }

        isLoading = true
        errorMessage = null

        scope.launch {
            try {
                val activityContext = context.findActivity() ?: context
                val credentialManager = CredentialManager.create(activityContext)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .addCredentialOption(signInWithGoogleOption)
                    .build()

                val result = credentialManager.getCredential(activityContext, request)
                val credential = result.credential

                if (credential is CustomCredential &&
                    (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
                     credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL)
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(authCredential)
                        .addOnSuccessListener {
                            isLoading = false
                            onSignInSuccess()
                        }
                        .addOnFailureListener { ex ->
                            isLoading = false
                            errorMessage = "Firebase no validó la credencial: ${ex.localizedMessage ?: "Error de autenticación"}. Verifica conexión o usa Modo Local."
                            Log.e(TAG, "Firebase auth error", ex)
                        }
                } else {
                    isLoading = false
                    errorMessage = "Tipo de credencial no reconocido: ${(credential as? CustomCredential)?.type ?: credential.javaClass.simpleName}. Usa Modo Local."
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-in cancelado por el usuario")
                isLoading = false
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-in failed", e)
                isLoading = false
                val msg = when {
                    e.message?.contains("16") == true || e.message?.contains("Developer") == true ->
                        "Error de configuración (Código 16 / Developer Error): Asegúrate de que el APK esté firmado y los servicios de Google Play actualizados."
                    e.message?.contains("No credential") == true ->
                        "No se seleccionó ninguna cuenta de Google en el dispositivo. Intenta nuevamente."
                    else ->
                        "Google Sign-In: ${e.localizedMessage ?: "Error al conectar con Google Play Services"}. Puedes usar Modo Local."
                }
                errorMessage = msg
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SlateBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Branding Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(EmeraldLight, EmeraldPrimary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Logo",
                        tint = NavySurface,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Finanzas & Cuotas",
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Control inteligente de cuentas, ingresos, egresos y pagos planificados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Highlights
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FeatureRow(
                        icon = Icons.Default.PieChart,
                        title = "Cuentas e Ingresos/Egresos",
                        subtitle = "Saldos actualizados y categorización de movimientos"
                    )
                    FeatureRow(
                        icon = Icons.Default.CheckCircle,
                        title = "Gastos en Cuotas y Parciales",
                        subtitle = "Planifica y abona cuotas con seguimiento detallado"
                    )
                    FeatureRow(
                        icon = Icons.Default.CloudDone,
                        title = "Modo Local o Nube",
                        subtitle = "Usa sin cuenta o respalda en la nube con Google"
                    )
                }
            }

            // Bottom CTA
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = TextPrimaryDark,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = { launchGoogleSignIn() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldLight,
                        contentColor = NavySurface
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = NavySurface,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Iniciando sesión...", fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Continuar con Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onContinueAsGuest,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("continue_guest_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = EmeraldLight
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Continuar sin cuenta (Modo Local)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tus finanzas seguras en tu dispositivo o en la nube",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryDark
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(EmeraldDark.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmeraldLight,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark,
                fontSize = 12.sp
            )
        }
    }
}
