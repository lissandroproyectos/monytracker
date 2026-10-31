package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MarketTemplate
import com.example.ui.FinanceViewModel
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

val DEFAULT_TEMPLATES = listOf(
    MarketTemplate(
        id = "default_cashea",
        title = "Cashea: 3 Cuotas Quincenales",
        description = "Esquema estándar de compras financiadas: pago inicial + 3 cuotas quincenales iguales a tasa oficial BCV.",
        category = "Compras",
        paymentMode = "INSTALLMENTS",
        frequency = "BIWEEKLY",
        installmentsCount = 3L,
        creatorName = "Finanzas VE",
        upvotes = 42L
    ),
    MarketTemplate(
        id = "default_banco_6m",
        title = "Crédito Bancario 6 Meses",
        description = "Préstamo o financiamiento bancario con cuotas mensuales fijas por 6 meses.",
        category = "Tecnología",
        paymentMode = "INSTALLMENTS",
        frequency = "MONTHLY",
        installmentsCount = 6L,
        creatorName = "Banca Personal",
        upvotes = 28L
    ),
    MarketTemplate(
        id = "default_usdt_quincenal",
        title = "Préstamo en USDT / Cripto (4 Cuotas)",
        description = "Préstamo personal en USDT dividido en 4 pagos quincenales.",
        category = "Otros",
        paymentMode = "INSTALLMENTS",
        frequency = "BIWEEKLY",
        installmentsCount = 4L,
        creatorName = "CriptoComunidad",
        upvotes = 19L
    ),
    MarketTemplate(
        id = "default_vehiculo_12m",
        title = "Financiamiento Moto / Vehículo (12 Cuotas)",
        description = "Plan de amortización en 12 cuotas mensuales con abonos a capital.",
        category = "Transporte",
        paymentMode = "INSTALLMENTS",
        frequency = "MONTHLY",
        installmentsCount = 12L,
        creatorName = "Movilidad VE",
        upvotes = 35L
    )
)

@Composable
fun MarketplaceScreen(
    viewModel: FinanceViewModel,
    onUseTemplate: (MarketTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    val cloudTemplates by viewModel.marketplaceTemplates.collectAsStateWithLifecycle()
    val allTemplates = (cloudTemplates + DEFAULT_TEMPLATES).distinctBy { it.id.ifBlank { it.title } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .testTag("marketplace_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Marketplace de Planes",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                fontSize = 22.sp
                            )
                            Text(
                                text = "Plantillas de deudas, créditos y compras en cuotas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Plantillas Populares y Compartidas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        }

        items(allTemplates, key = { it.id.ifBlank { it.title } }) { template ->
            Card(
                modifier = Modifier.fillMaxWidth().testTag("template_card_${template.id}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = EmeraldDark.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = template.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldLight,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (template.id.isNotBlank() && !template.id.startsWith("default_")) {
                                        viewModel.upvoteTemplate(template.id)
                                    }
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Me gusta",
                                    tint = AccentAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "${template.upvotes}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = template.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = template.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${template.installmentsCount} cuotas • ${template.frequency.lowercase()} • Por ${template.creatorName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )

                        Button(
                            onClick = { onUseTemplate(template) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("use_template_${template.id}")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Usar Plan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
