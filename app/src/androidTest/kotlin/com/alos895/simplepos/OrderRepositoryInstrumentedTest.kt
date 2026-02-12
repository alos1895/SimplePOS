package com.alos895.simplepos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.alos895.simplepos.db.AppDatabase
import com.alos895.simplepos.db.OrderDao
import com.alos895.simplepos.db.entity.OrderEntity
import com.alos895.simplepos.model.PaymentMethod
import com.alos895.simplepos.model.PaymentPart
import com.google.gson.Gson
import java.util.Calendar
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pruebas de integración en memoria para el flujo de órdenes con Room.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class OrderRepositoryInstrumentedTest {

    private lateinit var database: AppDatabase
    private lateinit var orderDao: OrderDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        orderDao = database.orderDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryOrderByDate() = runBlocking {
        val timestamp = System.currentTimeMillis()
        val order = OrderEntity(
            itemsJson = "[]",
            total = 250.0,
            timestamp = timestamp,
            userJson = "{}"
        )

        val orderId = orderDao.insertOrder(order)

        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val orders = orderDao.getOrdersByDate(calendar.timeInMillis)

        assertEquals(1, orders.size)
        assertEquals(orderId, orders.first().id)
        assertEquals(250.0, orders.first().total)
    }

    @Test
    fun updatePaymentBreakdownPersistsJson() = runBlocking {
        val timestamp = System.currentTimeMillis()
        val orderId = orderDao.insertOrder(
            OrderEntity(
                itemsJson = "[]",
                total = 100.0,
                timestamp = timestamp,
                userJson = "{}"
            )
        )
        val paymentParts = listOf(PaymentPart(PaymentMethod.EFECTIVO, 60.0))
        val json = Gson().toJson(paymentParts)

        orderDao.updatePaymentBreakdown(orderId, json)
        val updated = orderDao.getOrderById(orderId)

        assertTrue(updated != null)
        assertEquals(json, updated?.paymentBreakdownJson)
    }
}
