package com.example.data.repository

import com.example.data.Product
import com.example.data.ProductCatalog
import com.example.data.local.CartItem
import com.example.data.local.FavoriteEntity
import com.example.data.local.OrderEntity
import com.example.data.local.OrderProductInfo
import com.example.data.local.ShopDao
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ShopRepository(private val shopDao: ShopDao) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val orderItemsType = Types.newParameterizedType(List::class.java, OrderProductInfo::class.java)
    private val jsonAdapter = moshi.adapter<List<OrderProductInfo>>(orderItemsType)

    // Catalog Products
    val allProducts: List<Product> = ProductCatalog.products

    fun getProductById(id: String): Product? = ProductCatalog.getProductById(id)

    // Cart flows
    val cartItems: Flow<List<CartItem>> = shopDao.getCartItems()

    suspend fun addToCart(productId: String, quantity: Int, size: String, color: String) {
        val existingItem = CartItem(
            productId = productId,
            quantity = quantity,
            selectedSize = size,
            selectedColor = color,
            addedTime = System.currentTimeMillis()
        )
        shopDao.insertCartItem(existingItem)
    }

    suspend fun updateCartQuantity(productId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            shopDao.deleteCartItem(productId)
        } else {
            // Fetch current item details if any to preserve size/color
            // For simplicity, we can query or just update quantity directly in Room.
            // Let's replace the item in Room with the updated quantity
            shopDao.getCartItems().map { list ->
                list.find { it.productId == productId }
            }.collect { current ->
                if (current != null) {
                    shopDao.insertCartItem(current.copy(quantity = newQuantity))
                }
            }
        }
    }

    suspend fun removeFromCart(productId: String) {
        shopDao.deleteCartItem(productId)
    }

    suspend fun clearCart() {
        shopDao.clearCart()
    }

    // Favorites flows
    val favorites: Flow<List<FavoriteEntity>> = shopDao.getFavorites()

    fun isFavorite(productId: String): Flow<Boolean> = shopDao.isFavorite(productId)

    suspend fun toggleFavorite(productId: String) {
        val exists = shopDao.isFavorite(productId).first()
        if (exists) {
            shopDao.deleteFavorite(productId)
        } else {
            shopDao.insertFavorite(FavoriteEntity(productId))
        }
    }

    // Order flows
    val orders: Flow<List<OrderEntity>> = shopDao.getAllOrders()

    suspend fun createOrder(cartItemsList: List<Pair<Product, CartItem>>, total: Double): Boolean {
        if (cartItemsList.isEmpty()) return false

        // 1. Serialize items
        val orderProducts = cartItemsList.map { (product, cartItem) ->
            OrderProductInfo(
                productId = product.id,
                title = product.title,
                price = product.price,
                quantity = cartItem.quantity,
                size = cartItem.selectedSize,
                color = cartItem.selectedColor,
                imageUrl = product.imageUrl
            )
        }
        val itemsJson = jsonAdapter.toJson(orderProducts) ?: "[]"

        // 2. Insert order
        val order = OrderEntity(
            status = "Processing",
            totalAmount = total,
            itemsJson = itemsJson,
            orderDate = System.currentTimeMillis()
        )
        shopDao.insertOrder(order)

        // 3. Clear cart
        shopDao.clearCart()
        return true
    }

    // Utility to parse ordered items
    fun parseOrderItems(json: String): List<OrderProductInfo> {
        return try {
            jsonAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
