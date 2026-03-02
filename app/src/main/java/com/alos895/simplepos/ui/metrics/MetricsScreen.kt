package com.alos895.simplepos.ui.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alos895.simplepos.ui.admin.AdminInventoryViewModel
import com.alos895.simplepos.ui.caja.CajaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MetricsScreen(
    cajaViewModel: CajaViewModel,
    adminInventoryViewModel: AdminInventoryViewModel
) {
    val dailyStats by cajaViewModel.dailyStats.collectAsState()
    val selectedDate by cajaViewModel.selectedDate.collectAsState()
    val pizzaBases by adminInventoryViewModel.pizzaBases.collectAsState()

    val basesForSelectedDate = pizzaBases.filter { isSameDay(it.createdAt, selectedDate.time) }
    val totalBases = basesForSelectedDate.size
    val usedBases = basesForSelectedDate.count { it.usedAt != null }
    val availableBases = totalBases - usedBases

    val baseUsageRate = if (totalBases > 0) {
        (usedBases.toDouble() / totalBases.toDouble()) * 100
    } else {
        0.0
    }

    val ticketPromedio = if (dailyStats.ordenes > 0) {
        dailyStats.totalCaja / dailyStats.ordenes
    } else {
        0.0
    }

    val conversionEnvio = if (dailyStats.ordenes > 0) {
        ((dailyStats.envios + dailyStats.deliverysTOTODO).toDouble() / dailyStats.ordenes.toDouble()) * 100
    } else {
        0.0
    }

    val mixPizzas = listOf(
        "Chicas" to dailyStats.pizzasChicas,
        "Medianas" to dailyStats.pizzasMedianas,
        "Grandes" to dailyStats.pizzasGrandes
    )

    val categoryRevenue = listOf(
        "Pizzas" to dailyStats.ingresosPizzas,
        "Postres" to dailyStats.ingresosPostres,
        "Combos" to dailyStats.ingresosCombos,
        "Bebidas" to dailyStats.ingresosBebidas,
        "Extras" to dailyStats.ingresosExtras,
        "Envíos" to dailyStats.ingresosEnvios
    ).sortedByDescending { it.second }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Métricas del negocio",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Fecha: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        item {
            MetricsCard(
                title = "KPIs diarios",
                lines = listOf(
                    "Órdenes: ${dailyStats.ordenes}",
                    "Ingresos caja: ${formatCurrency(dailyStats.totalCaja)}",
                    "Ticket promedio: ${formatCurrency(ticketPromedio)}",
                    "% órdenes con envío: ${"%.1f".format(conversionEnvio)}%",
                    "No pagado detectado: ${formatCurrency(dailyStats.ordenesNoPagadas)}"
                )
            )
        }

        item {
            MetricsCard(
                title = "Flujo de efectivo",
                lines = listOf(
                    "Órdenes en efectivo: ${formatCurrency(dailyStats.totalOrdenesEfectivo)}",
                    "Órdenes tarjeta/transferencia: ${formatCurrency(dailyStats.totalOrdenesTarjeta)}",
                    "Ingresos manuales: ${formatCurrency(dailyStats.ingresosCapturados)}",
                    "Gastos manuales: ${formatCurrency(dailyStats.egresosCapturados)}",
                    "Descuentos TOTODO: ${formatCurrency(dailyStats.totalDescuentosTOTODO)}"
                )
            )
        }

        item {
            MetricsCard(
                title = "Inventario de bases (día)",
                lines = listOf(
                    "Bases creadas: $totalBases",
                    "Bases usadas: $usedBases",
                    "Bases disponibles: $availableBases",
                    "Uso de bases: ${"%.1f".format(baseUsageRate)}%"
                )
            )
        }

        item {
            Text("Mix de tamaños", style = MaterialTheme.typography.titleMedium)
        }

        items(mixPizzas) { (size, amount) ->
            RatioRow(label = size, value = amount, total = dailyStats.pizzas)
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Ranking ingresos por categoría", style = MaterialTheme.typography.titleMedium)
        }

        items(categoryRevenue) { (name, amount) ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name)
                Text(formatCurrency(amount))
            }
        }
    }
}

@Composable
private fun MetricsCard(title: String, lines: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            lines.forEach { line ->
                Text(line)
            }
        }
    }
}

@Composable
private fun RatioRow(label: String, value: Int, total: Int) {
    val ratio = if (total > 0) (value.toDouble() / total.toDouble()) * 100.0 else 0.0
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text("$value (${String.format("%.1f", ratio)}%)")
    }
}

private fun formatCurrency(value: Double): String = "$${"%,.2f".format(value)}"

private fun isSameDay(sourceMillis: Long, targetMillis: Long): Boolean {
    val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return sdf.format(Date(sourceMillis)) == sdf.format(Date(targetMillis))
}

