package com.alos895.simplepos

import com.alos895.simplepos.data.datasource.MenuData
import com.alos895.simplepos.data.repository.MenuRepository
import com.alos895.simplepos.model.CartItemPortion
import com.alos895.simplepos.model.PizzaFractionType
import com.alos895.simplepos.ui.clients.UserViewModel
import com.alos895.simplepos.ui.common.CartItemFormatter
import com.alos895.simplepos.ui.menu.MenuViewModel
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Suite de pruebas unitarias que cubre lógica de viewmodels, repositorios y utilitarios.
 */
class ExampleViewModelTest {

    @AfterTest
    fun tearDown() {
        // Limpia cualquier mock estático que se haya configurado en las pruebas.
        runCatching { unmockkObject(MenuData) }
    }

    @Test
    fun menuViewModelLoadsMenuOnInit() {
        val viewModel = MenuViewModel()

        val pizzas = viewModel.pizzas.value

        assertTrue(pizzas.isNotEmpty(), "Se espera que el menú se cargue al iniciar el ViewModel")
        assertEquals(MenuData.pizzas.first().nombre, pizzas.first().nombre)
    }

    @Test
    fun userViewModelAddsUserWithGeneratedId() {
        val viewModel = UserViewModel()
        val newUser = TestUtils.createUser(nombre = "Ana")

        viewModel.addUser(newUser)

        val storedUser = viewModel.users.value.single()
        assertEquals("Ana", storedUser.nombre)
        assertNotEquals(0, storedUser.id, "El ViewModel debe asignar un id generado al usuario")
    }

    @Test
    fun userViewModelRemovesUserById() {
        val viewModel = UserViewModel()
        val firstUser = TestUtils.createUser(nombre = "Juan")
        val secondUser = TestUtils.createUser(nombre = "Maria")

        viewModel.addUser(firstUser)
        viewModel.addUser(secondUser)
        val ids = viewModel.users.value.map { it.id }

        viewModel.removeUser(ids.first())

        val remaining = viewModel.users.value
        assertEquals(1, remaining.size)
        assertEquals("Maria", remaining.first().nombre)
    }

    @Test
    fun cartItemFormatterBuildsGoldenComboSummary() {
        val pizzaName = "Cuatro Quesos"
        val portion = CartItemPortion(pizzaName = pizzaName, fraction = PizzaFractionType.QUARTER)
        val cartItem = TestUtils.createCartItem(
            pizza = TestUtils.createPizza(nombre = pizzaName),
            sizeName = "Grande",
            unitPrice = 220.0,
            quantity = 2,
            isGolden = true,
            portions = listOf(portion)
        )

        val lines = CartItemFormatter.toCustomerLines(cartItem)

        val expectedAmount = String.format(Locale.getDefault(), "%.2f", cartItem.subtotal)
        assertTrue(lines.first().contains("2x Pizza Grande combinada"))
        assertTrue(lines.first().contains(expectedAmount))
        assertTrue(lines.any { it.contains("Doradita") })
        assertTrue(lines.any { it.contains("1/4") && it.contains(pizzaName) })
    }

    @Test
    fun cartItemFormatterKitchenLinesIncludeIngredients() {
        val customPizza = TestUtils.createPizza(
            nombre = "Vegana",
            ingredientes = listOf(99)
        )
        mockkObject(MenuData)
        every { MenuData.pizzas } returns listOf(customPizza)
        every { MenuData.ingredientes } returns listOf(com.alos895.simplepos.model.Ingrediente(99, "Champiñón", 0.0, 0.0, 0.0))

        val cartItem = TestUtils.createCartItem(
            pizza = customPizza,
            portions = listOf(TestUtils.createPortion(pizzaName = "Vegana"))
        )

        val lines = CartItemFormatter.toKitchenLines(cartItem)

        assertTrue(lines.any { it.contains("Vegana") })
        assertTrue(lines.any { it.contains("Champiñón") })
    }

    @Test
    fun menuRepositoryDelegatesToDataSource() {
        val expectedName = "Especial Test"
        val fakePizza = TestUtils.createPizza(nombre = expectedName)
        mockkObject(MenuData)
        every { MenuData.pizzas } returns listOf(fakePizza)

        val repository = MenuRepository()

        val pizzas = repository.getPizzas()

        assertEquals(1, pizzas.size)
        assertEquals(expectedName, pizzas.first().nombre)
    }
}
