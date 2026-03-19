package com.alos895.simplepos

import android.Manifest
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.alos895.simplepos.data.datasource.MenuData
import com.alos895.simplepos.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas de UI que recorren los flujos críticos de la app con Espresso/Compose.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class LoginFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH
    )

    @Test
    fun appLaunchesShowingMenuAsLoginFlow() {
        // La app inicia directamente en el menú (login implícito) y debe mostrar el resumen de carrito.
        composeRule.onNodeWithText("Menú").assertExists()
        composeRule.onNodeWithText("Total a pagar").assertExists()
    }

    @Test
    fun switchingToDessertsFiltersVisibleItems() {
        // Cambia a la sección de postres y verifica que aparezca un elemento real del catálogo.
        composeRule.onNodeWithText("Postres").performClick()
        val dessertName = MenuData.postreOrExtras.first { it.esPostre }.nombre
        composeRule.onNodeWithText(dessertName).assertExists()
    }

    @Test
    fun addingDessertEnablesCheckoutButton() {
        // El flujo de compra se habilita al agregar un producto.
        composeRule.onNodeWithText("Guardar orden").assertIsNotEnabled()
        composeRule.onNodeWithText("Postres").performClick()
        composeRule.onAllNodesWithText("Agregar").onFirst().performClick()
        composeRule.onNodeWithText("Guardar orden").assertIsEnabled()
        composeRule.onNodeWithText("1 artículo", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun printTabShowsPermissionSensitiveUi() {
        // Navega a la pantalla de impresión que requiere permisos de Bluetooth.
        composeRule.onNodeWithText("Impresión").performClick()
        composeRule.onNodeWithText("Impresora Bluetooth").assertExists()
    }

    @Test
    fun transactionsTabShowsEmptyStateForOfflineRefresh() {
        // Cambia a transacciones y confirma que se ofrece feedback cuando no hay datos (offline/recarga).
        composeRule.onNodeWithText("Transacciones").performClick()
        composeRule.onNodeWithText("No hay transacciones registradas.").assertExists()
    }
}
