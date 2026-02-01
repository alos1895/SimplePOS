package com.alos895.simplepos.ui.print

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.db.AppDatabase
import com.alos895.simplepos.db.entity.BaseInventoryEntity
import com.alos895.simplepos.db.entity.OrderEntity
import com.alos895.simplepos.model.CartItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class BaseInventoryUiState(
    val dateKey: String = "",
    val dateLabel: String = "",
    val selectedDateMillis: Long = 0L,
    val baseGrandesInput: String = "0",
    val baseMedianasInput: String = "0",
    val baseChicasInput: String = "0",
    val soldGrandes: Int = 0,
    val soldMedianas: Int = 0,
    val soldChicas: Int = 0,
    val soldTotal: Int = 0,
    val remainingGrandes: Int = 0,
    val remainingMedianas: Int = 0,
    val remainingChicas: Int = 0,
    val remainingTotal: Int = 0,
    val totalBases: Int = 0,
    val absoluteGrandes: Int = 0,
    val absoluteMedianas: Int = 0,
    val absoluteChicas: Int = 0,
    val absoluteTotal: Int = 0,
    val absoluteSoldGrandes: Int = 0,
    val absoluteSoldMedianas: Int = 0,
    val absoluteSoldChicas: Int = 0,
    val absoluteSoldTotal: Int = 0,
    val absoluteRemainingGrandes: Int = 0,
    val absoluteRemainingMedianas: Int = 0,
    val absoluteRemainingChicas: Int = 0,
    val absoluteRemainingTotal: Int = 0,
    val errorMessage: String? = null
)

class BaseInventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val baseInventoryDao = db.baseInventoryDao()
    private val orderDao = db.orderDao()
    private val gson = Gson()

    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateLabelFormat = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES"))

    private val _uiState = MutableStateFlow(BaseInventoryUiState())
    val uiState: StateFlow<BaseInventoryUiState> = _uiState.asStateFlow()

    init {
        setDate(Date())
    }

    fun onBaseGrandesChange(value: String) {
        _uiState.update { it.copy(baseGrandesInput = value.filter { char -> char.isDigit() }) }
    }

    fun onBaseMedianasChange(value: String) {
        _uiState.update { it.copy(baseMedianasInput = value.filter { char -> char.isDigit() }) }
    }

    fun onBaseChicasChange(value: String) {
        _uiState.update { it.copy(baseChicasInput = value.filter { char -> char.isDigit() }) }
    }

    fun onDateSelected(selectedMillis: Long) {
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCalendar.timeInMillis = selectedMillis
        val localCalendar = Calendar.getInstance()
        localCalendar.set(
            utcCalendar.get(Calendar.YEAR),
            utcCalendar.get(Calendar.MONTH),
            utcCalendar.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
        localCalendar.set(Calendar.MILLISECOND, 0)
        setDate(localCalendar.time)
    }

    fun goToToday() {
        setDate(Date())
    }

    fun refreshCurrentDate() {
        refreshForDateKey(_uiState.value.dateKey)
    }

    fun saveBaseCounts() {
        val baseGrandes = _uiState.value.baseGrandesInput.toIntOrNull()
        val baseMedianas = _uiState.value.baseMedianasInput.toIntOrNull()
        val baseChicas = _uiState.value.baseChicasInput.toIntOrNull()
        if (baseGrandes == null || baseMedianas == null || baseChicas == null) {
            _uiState.update { it.copy(errorMessage = "Ingresa cantidades válidas para las bases.") }
            return
        }
        val dateKey = _uiState.value.dateKey
        viewModelScope.launch {
            baseInventoryDao.upsert(
                BaseInventoryEntity(
                    dateKey = dateKey,
                    baseGrandes = baseGrandes,
                    baseMedianas = baseMedianas,
                    baseChicas = baseChicas
                )
            )
            refreshForDateKey(dateKey)
        }
    }

    private fun setDate(date: Date) {
        val dateKey = dateKeyFormat.format(date)
        _uiState.update {
            it.copy(
                dateKey = dateKey,
                dateLabel = dateLabelFormat.format(date).replaceFirstChar { char -> char.uppercase() },
                selectedDateMillis = date.time,
                errorMessage = null
            )
        }
        refreshForDateKey(dateKey)
    }

    private fun refreshForDateKey(dateKey: String) {
        viewModelScope.launch {
            val base = baseInventoryDao.getByDateKey(dateKey)
            val dayStart = getDayStartMillis(dateKey)
            val orders = if (dayStart != null) orderDao.getOrdersByDate(dayStart) else emptyList()
            val allOrders = orderDao.getAllOrders()
            val sold = calculateSoldBases(orders)
            val soldAll = calculateSoldBases(allOrders)
            val baseGrandes = base?.baseGrandes ?: 0
            val baseMedianas = base?.baseMedianas ?: 0
            val baseChicas = base?.baseChicas ?: 0
            val totals = baseInventoryDao.getTotals()
            val totalBases = baseGrandes + baseMedianas + baseChicas
            val soldTotal = sold.first + sold.second + sold.third
            val absoluteTotal = totals.totalGrandes + totals.totalMedianas + totals.totalChicas
            val absoluteSoldTotal = soldAll.first + soldAll.second + soldAll.third
            _uiState.update {
                it.copy(
                    baseGrandesInput = baseGrandes.toString(),
                    baseMedianasInput = baseMedianas.toString(),
                    baseChicasInput = baseChicas.toString(),
                    soldGrandes = sold.first,
                    soldMedianas = sold.second,
                    soldChicas = sold.third,
                    soldTotal = soldTotal,
                    remainingGrandes = baseGrandes - sold.first,
                    remainingMedianas = baseMedianas - sold.second,
                    remainingChicas = baseChicas - sold.third,
                    remainingTotal = totalBases - soldTotal,
                    totalBases = totalBases,
                    absoluteGrandes = totals.totalGrandes,
                    absoluteMedianas = totals.totalMedianas,
                    absoluteChicas = totals.totalChicas,
                    absoluteTotal = absoluteTotal,
                    absoluteSoldGrandes = soldAll.first,
                    absoluteSoldMedianas = soldAll.second,
                    absoluteSoldChicas = soldAll.third,
                    absoluteSoldTotal = absoluteSoldTotal,
                    absoluteRemainingGrandes = totals.totalGrandes - soldAll.first,
                    absoluteRemainingMedianas = totals.totalMedianas - soldAll.second,
                    absoluteRemainingChicas = totals.totalChicas - soldAll.third,
                    absoluteRemainingTotal = absoluteTotal - absoluteSoldTotal
                )
            }
        }
    }

    private fun getDayStartMillis(dateKey: String): Long? {
        return try {
            val parsed = dateKeyFormat.parse(dateKey) ?: return null
            val calendar = Calendar.getInstance()
            calendar.time = parsed
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateSoldBases(orders: List<OrderEntity>): Triple<Int, Int, Int> {
        var soldGrandes = 0
        var soldMedianas = 0
        var soldChicas = 0
        orders.forEach { order ->
            getCartItems(order).forEach { item ->
                when (item.sizeLabel.trim().lowercase(Locale.getDefault())) {
                    "chica" -> soldChicas += item.cantidad
                    "mediana" -> soldMedianas += item.cantidad
                    "grande", "extra grande" -> soldGrandes += item.cantidad
                }
            }
        }
        return Triple(soldGrandes, soldMedianas, soldChicas)
    }

    private fun getCartItems(order: OrderEntity): List<CartItem> {
        return try {
            val type = object : TypeToken<List<CartItem>>() {}.type
            gson.fromJson(order.itemsJson, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
