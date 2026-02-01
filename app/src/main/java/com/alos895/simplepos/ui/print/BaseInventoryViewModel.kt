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

data class BaseInventoryUiState(
    val dateKey: String = "",
    val dateInput: String = "",
    val dateLabel: String = "",
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
    val errorMessage: String? = null
)

class BaseInventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val baseInventoryDao = db.baseInventoryDao()
    private val orderDao = db.orderDao()
    private val gson = Gson()

    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateInputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dateLabelFormat = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES"))

    private val _uiState = MutableStateFlow(BaseInventoryUiState())
    val uiState: StateFlow<BaseInventoryUiState> = _uiState.asStateFlow()

    init {
        setDate(Date())
    }

    fun onDateInputChange(value: String) {
        _uiState.update { it.copy(dateInput = value, errorMessage = null) }
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

    fun loadForInputDate() {
        val parsedDate = parseInputDate(_uiState.value.dateInput)
        if (parsedDate == null) {
            _uiState.update { it.copy(errorMessage = "Fecha inválida. Usa el formato dd/MM/yyyy.") }
            return
        }
        setDate(parsedDate)
    }

    fun goToPreviousDay() {
        shiftDateBy(-1)
    }

    fun goToNextDay() {
        shiftDateBy(1)
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
                dateInput = dateInputFormat.format(date),
                dateLabel = dateLabelFormat.format(date).replaceFirstChar { char -> char.uppercase() },
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
            val sold = calculateSoldBases(orders)
            val baseGrandes = base?.baseGrandes ?: 0
            val baseMedianas = base?.baseMedianas ?: 0
            val baseChicas = base?.baseChicas ?: 0
            val totalBases = baseGrandes + baseMedianas + baseChicas
            val soldTotal = sold.first + sold.second + sold.third
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
                    totalBases = totalBases
                )
            }
        }
    }

    private fun parseInputDate(input: String): Date? {
        return try {
            dateInputFormat.isLenient = false
            dateInputFormat.parse(input.trim())
        } catch (_: Exception) {
            null
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

    private fun shiftDateBy(days: Int) {
        val currentKey = _uiState.value.dateKey
        val parsed = dateKeyFormat.parse(currentKey) ?: return
        val calendar = Calendar.getInstance()
        calendar.time = parsed
        calendar.add(Calendar.DAY_OF_YEAR, days)
        setDate(calendar.time)
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
