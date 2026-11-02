package com.example.data.currency

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "CurrencyManager"

enum class AppCurrency(
    val code: String,
    val displayName: String,
    val symbol: String,
    val flagEmoji: String,
    val symbolWithCode: String
) {
    VES("VES", "Bolívares (Bs.)", "Bs.", "🇻🇪", "Bs. VES"),
    USD_BCV("USD_BCV", "Dólar ($)", "$", "💵", "$ USD"),
    USDT("USDT", "USDT (₮)", "₮", "₮", "₮ USDT"),
    EUR_BCV("EUR_BCV", "Euro (€)", "€", "💶", "€ EUR");

    val shortLabel: String
        get() = when (this) {
            USD_BCV -> "$"
            EUR_BCV -> "€"
            USDT -> "₮"
            VES -> "Bs."
        }

    companion object {
        fun fromCode(code: String?): AppCurrency {
            if (code.isNullOrBlank()) return USD_BCV
            val clean = code.trim().uppercase()
            return when {
                clean == "USD" || clean == "USD_BCV" || clean == "$" -> USD_BCV
                clean == "USDT" || clean == "TETHER" || clean == "₮" -> USDT
                clean == "EUR" || clean == "EUR_BCV" || clean == "€" -> EUR_BCV
                clean == "VES" || clean == "BS" || clean == "BS." -> VES
                else -> entries.find { it.name.equals(clean, ignoreCase = true) || it.code.equals(clean, ignoreCase = true) } ?: USD_BCV
            }
        }
    }
}

data class ExchangeRates(
    val dolarBcv: Double = 888.89,
    val usdtParalelo: Double = 1002.85,
    val euroBcv: Double = 989.39,
    val lastUpdated: String = "Actualizado recientemente",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    fun convert(amount: Double, from: AppCurrency, to: AppCurrency): Double {
        if (from == to || amount == 0.0) return amount

        // 1. Convert source amount to base currency (VES)
        val inBs = when (from) {
            AppCurrency.VES -> amount
            AppCurrency.USD_BCV -> amount * dolarBcv
            AppCurrency.USDT -> amount * usdtParalelo
            AppCurrency.EUR_BCV -> amount * euroBcv
        }

        // 2. Convert base VES to target currency
        return when (to) {
            AppCurrency.VES -> inBs
            AppCurrency.USD_BCV -> if (dolarBcv > 0) inBs / dolarBcv else 0.0
            AppCurrency.USDT -> if (usdtParalelo > 0) inBs / usdtParalelo else 0.0
            AppCurrency.EUR_BCV -> if (euroBcv > 0) inBs / euroBcv else 0.0
        }
    }

    fun format(amount: Double, currency: AppCurrency): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        return "${currency.symbol} ${formatter.format(amount)}"
    }
}

object CurrencyService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun fetchRates(): ExchangeRates = withContext(Dispatchers.IO) {
        var bcv = 888.89
        var usdt = 1002.85
        var eur = 989.39
        var lastUpdate = ""

        try {
            // 1. Dolar Oficial BCV
            val reqBcv = Request.Builder().url("https://ve.dolarapi.com/v1/dolares/oficial").build()
            client.newCall(reqBcv).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val json = JSONObject(body)
                    bcv = json.optDouble("promedio", bcv)
                    lastUpdate = json.optString("fechaActualizacion", "")
                }
            }

            // 2. Dolar Paralelo / USDT
            val reqUsdt = Request.Builder().url("https://ve.dolarapi.com/v1/dolares/paralelo").build()
            client.newCall(reqUsdt).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val json = JSONObject(body)
                    usdt = json.optDouble("promedio", usdt)
                }
            }

            // 3. Euro Oficial BCV
            val reqEur = Request.Builder().url("https://ve.dolarapi.com/v1/euros/oficial").build()
            client.newCall(reqEur).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val json = JSONObject(body)
                    eur = json.optDouble("promedio", eur)
                }
            }

            ExchangeRates(
                dolarBcv = bcv,
                usdtParalelo = usdt,
                euroBcv = eur,
                lastUpdated = if (lastUpdate.isNotBlank()) "Tasas oficiales de hoy" else "Tasas en tiempo real"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching live rates, using fallback: ${e.message}")
            ExchangeRates(
                dolarBcv = bcv,
                usdtParalelo = usdt,
                euroBcv = eur,
                lastUpdated = "Tasas de referencia (sin conexión)",
                error = e.localizedMessage
            )
        }
    }
}
