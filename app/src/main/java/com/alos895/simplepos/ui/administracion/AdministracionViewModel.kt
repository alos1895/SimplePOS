package com.alos895.simplepos.ui.administracion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.repository.BaseProductionRepository
import com.alos895.simplepos.db.BaseProductionTotals
import com.alos895.simplepos.db.entity.BaseProductionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdministracionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BaseProductionRepository(application)

    private val _totals = MutableStateFlow(BaseProductionTotals())
    val totals: StateFlow<BaseProductionTotals> = _totals.asStateFlow()

    init {
        loadTotals()
    }

    fun loadTotals() {
        viewModelScope.launch {
            _totals.value = repository.getTotals()
        }
    }

    fun addProduction(chicas: Int, medianas: Int, grandes: Int) {
        viewModelScope.launch {
            val newEntry = BaseProductionEntity(
                chicas = chicas,
                medianas = medianas,
                grandes = grandes,
                timestamp = System.currentTimeMillis()
            )
            repository.insertBaseProduction(newEntry)
            _totals.value = repository.getTotals()
        }
    }
}
