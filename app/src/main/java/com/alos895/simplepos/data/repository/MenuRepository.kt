package com.alos895.simplepos.data.repository

import com.alos895.simplepos.data.datasource.MenuData
import com.alos895.simplepos.db.AppDatabase
import com.alos895.simplepos.db.entity.ExtraEntity
import com.alos895.simplepos.db.entity.IngredientEntity
import com.alos895.simplepos.db.entity.PizzaEntity
import com.alos895.simplepos.db.entity.PizzaSizeEntity
import com.alos895.simplepos.db.entity.PizzaWithSizes
import com.alos895.simplepos.model.ExtraType
import com.alos895.simplepos.model.Ingrediente
import com.alos895.simplepos.model.Pizza
import com.alos895.simplepos.model.PostreOrExtra
import com.alos895.simplepos.model.TamanoPizza
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AdminPizza(
    val id: Long,
    val nombre: String,
    val ingredientesBaseIds: List<Int>,
    val tamanos: List<TamanoPizza>,
    val esCombinable: Boolean
)

data class PizzaUpsert(
    val id: Long? = null,
    val nombre: String,
    val ingredientesBaseIds: List<Int>,
    val tamanos: List<TamanoPizza>,
    val esCombinable: Boolean
)

class MenuRepository(private val database: AppDatabase) {
    private val ingredientDao = database.ingredientDao()
    private val pizzaDao = database.pizzaDao()
    private val extraDao = database.extraDao()

    fun getPizzas(): Flow<List<Pizza>> = pizzaDao.getPizzasWithSizes()
        .map { pizzas -> pizzas.map { it.toPizza() } }

    fun getAdminPizzas(): Flow<List<AdminPizza>> = pizzaDao.getPizzasWithSizes()
        .map { pizzas -> pizzas.map { it.toAdminPizza() } }

    fun getIngredientes(): Flow<List<Ingrediente>> = ingredientDao.getIngredients()
        .map { items -> items.map { it.toIngrediente() } }

    fun getExtras(type: ExtraType): Flow<List<PostreOrExtra>> = extraDao.getExtrasByType(type.name)
        .map { items -> items.map { it.toPostreOrExtra(type) } }

    suspend fun ensureSeeded() {
        if (ingredientDao.countIngredients() == 0L) {
            MenuData.ingredientes.forEach { ingrediente ->
                ingredientDao.insertIngredient(ingrediente.toEntity())
            }
        }
        if (pizzaDao.countPizzas() == 0L) {
            MenuData.pizzas.forEach { pizza ->
                val pizzaId = pizzaDao.insertPizza(pizza.toEntity())
                val sizeEntities = pizza.tamanos.map { size ->
                    PizzaSizeEntity(
                        pizzaId = pizzaId,
                        sizeName = size.nombre,
                        price = size.precioBase
                    )
                }
                pizzaDao.insertPizzaSizes(sizeEntities)
            }
        }
        if (extraDao.countExtras() == 0L) {
            MenuData.postreOrExtras.forEach { extra ->
                val type = if (extra.esPostre) ExtraType.POSTRE else ExtraType.EXTRA
                extraDao.insertExtra(extra.toEntity(type))
            }
            MenuData.comboOptions.forEach { combo ->
                extraDao.insertExtra(combo.toEntity(ExtraType.COMBO))
            }
        }

        if (extraDao.countExtrasByType(ExtraType.BEBIDA.name) == 0L) {
            MenuData.bebidaOptions.forEach { bebida ->
                extraDao.insertExtra(bebida.toEntity(ExtraType.BEBIDA))
            }
        }
    }

    suspend fun upsertIngredient(ingrediente: Ingrediente) {
        if (ingrediente.id == 0) {
            val nextId = (ingredientDao.maxIngredientId() ?: 0) + 1
            ingredientDao.insertIngredient(ingrediente.copy(id = nextId).toEntity())
        } else {
            ingredientDao.updateIngredient(ingrediente.toEntity())
        }
    }

    suspend fun deleteIngredient(ingrediente: Ingrediente) {
        ingredientDao.deleteIngredient(ingrediente.toEntity())
    }

    suspend fun upsertExtra(extra: PostreOrExtra, type: ExtraType) {
        val extraId = if (extra.id == 0) {
            (extraDao.maxExtraId(type.name) ?: 0) + 1
        } else {
            extra.id
        }
        val entity = extra.copy(id = extraId).toEntity(type)
        if (extra.id == 0) {
            extraDao.insertExtra(entity)
        } else {
            extraDao.updateExtra(entity)
        }
    }

    suspend fun deleteExtra(extra: PostreOrExtra, type: ExtraType) {
        extraDao.deleteExtra(extra.toEntity(type))
    }

    suspend fun upsertPizza(pizza: PizzaUpsert) {
        val pizzaEntity = PizzaEntity(
            id = pizza.id ?: 0,
            name = pizza.nombre,
            ingredientIdsCsv = pizza.ingredientesBaseIds.joinToString(","),
            isCombinable = pizza.esCombinable
        )
        val pizzaId = if (pizza.id == null) {
            pizzaDao.insertPizza(pizzaEntity)
        } else {
            pizzaDao.updatePizza(pizzaEntity)
            pizza.id
        }
        if (pizzaId != null) {
            pizzaDao.deleteSizesForPizza(pizzaId)
            val sizeEntities = pizza.tamanos.map { size ->
                PizzaSizeEntity(
                    pizzaId = pizzaId,
                    sizeName = size.nombre,
                    price = size.precioBase
                )
            }
            pizzaDao.insertPizzaSizes(sizeEntities)
        }
    }

    suspend fun deletePizza(pizza: AdminPizza) {
        pizzaDao.deletePizza(
            PizzaEntity(
                id = pizza.id,
                name = pizza.nombre,
                ingredientIdsCsv = pizza.ingredientesBaseIds.joinToString(","),
                isCombinable = pizza.esCombinable
            )
        )
    }

    private fun IngredientEntity.toIngrediente(): Ingrediente = Ingrediente(
        id = id,
        nombre = name,
        preciExtraChica = priceExtraSmall,
        precioExtraMediana = priceExtraMedium,
        precioExtraGrande = priceExtraLarge
    )

    private fun Ingrediente.toEntity(): IngredientEntity = IngredientEntity(
        id = id,
        name = nombre,
        priceExtraSmall = preciExtraChica,
        priceExtraMedium = precioExtraMediana,
        priceExtraLarge = precioExtraGrande
    )

    private fun Pizza.toEntity(): PizzaEntity = PizzaEntity(
        name = nombre,
        ingredientIdsCsv = ingredientesBaseIds.joinToString(","),
        isCombinable = esCombinable
    )

    private fun PizzaWithSizes.toPizza(): Pizza = Pizza(
        nombre = pizza.name,
        ingredientesBaseIds = pizza.ingredientIdsCsv.toIdList(),
        tamanos = sizes.sortedBy { it.id }.map { size ->
            TamanoPizza(
                nombre = size.sizeName,
                precioBase = size.price
            )
        },
        esCombinable = pizza.isCombinable
    )

    private fun PizzaWithSizes.toAdminPizza(): AdminPizza = AdminPizza(
        id = pizza.id,
        nombre = pizza.name,
        ingredientesBaseIds = pizza.ingredientIdsCsv.toIdList(),
        tamanos = sizes.sortedBy { it.id }.map { size ->
            TamanoPizza(size.sizeName, size.price)
        },
        esCombinable = pizza.isCombinable
    )

    private fun ExtraEntity.toPostreOrExtra(type: ExtraType): PostreOrExtra = PostreOrExtra(
        id = id,
        nombre = name,
        precio = price,
        esPostre = type == ExtraType.POSTRE,
        esCombo = type == ExtraType.COMBO
    )

    private fun PostreOrExtra.toEntity(type: ExtraType): ExtraEntity = ExtraEntity(
        id = id,
        name = nombre,
        price = precio,
        type = type.name
    )

    private fun String.toIdList(): List<Int> = split(",")
        .mapNotNull { it.trim().takeIf { value -> value.isNotEmpty() }?.toIntOrNull() }
}
