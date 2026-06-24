package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Product
import com.example.data.local.AppDatabase
import com.example.data.local.CartItem
import com.example.data.local.OrderEntity
import com.example.data.local.OrderProductInfo
import com.example.data.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShopViewModel(
    application: Application,
    private val repository: ShopRepository
) : AndroidViewModel(application) {

    // Filter states
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")

    // Reactive products list filtered by category and search query
    val filteredProducts: StateFlow<List<Product>> = combine(
        searchQuery,
        selectedCategory
    ) { query, category ->
        var list = repository.allProducts
        if (category != "All") {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Reactive cart item state: List of Pair<Product, CartItem>
    val cartState: StateFlow<List<Pair<Product, CartItem>>> = repository.cartItems
        .map { dbCartItems ->
            val allProds = repository.allProducts
            dbCartItems.mapNotNull { cartItem ->
                val product = allProds.find { it.id == cartItem.productId }
                if (product != null) {
                    product to cartItem
                } else {
                    null
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactive favorite products
    val favoriteProducts: StateFlow<List<Product>> = repository.favorites
        .map { dbFavorites ->
            val allProds = repository.allProducts
            dbFavorites.mapNotNull { fav ->
                allProds.find { it.id == fav.productId }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactive order history
    val orders: StateFlow<List<OrderEntity>> = repository.orders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Cart calculations
    val cartSubtotal: StateFlow<Double> = cartState
        .combine(MutableStateFlow(0.0)) { cartList, _ ->
            cartList.sumOf { (product, cartItem) -> product.price * cartItem.quantity }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val cartItemCount: StateFlow<Int> = cartState
        .combine(MutableStateFlow(0)) { cartList, _ ->
            cartList.sumOf { (_, cartItem) -> cartItem.quantity }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Checkout operation
    val checkoutSuccess = MutableStateFlow<Boolean?>(null)

    // Actions
    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectCategory(category: String) {
        selectedCategory.value = category
    }

    fun addToCart(product: Product, quantity: Int, size: String, color: String) {
        viewModelScope.launch {
            repository.addToCart(product.id, quantity, size, color)
        }
    }

    fun updateCartQuantity(productId: String, increment: Boolean) {
        viewModelScope.launch {
            // Find current quantity
            val currentCart = cartState.value
            val item = currentCart.find { it.first.id == productId } ?: return@launch
            val newQty = if (increment) item.second.quantity + 1 else item.second.quantity - 1
            if (newQty <= 0) {
                repository.removeFromCart(productId)
            } else {
                repository.addToCart(productId, newQty, item.second.selectedSize, item.second.selectedColor)
            }
        }
    }

    fun getProductById(id: String): Product? {
        return repository.getProductById(id)
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
        }
    }

    fun removeFromCart(productId: String) {
        viewModelScope.launch {
            repository.removeFromCart(productId)
        }
    }

    fun toggleFavorite(product: Product) {
        viewModelScope.launch {
            repository.toggleFavorite(product.id)
        }
    }

    fun isProductFavorite(productId: String): StateFlow<Boolean> {
        val favFlow = MutableStateFlow(false)
        viewModelScope.launch {
            repository.isFavorite(productId).collect {
                favFlow.value = it
            }
        }
        return favFlow
    }

    fun checkout(shippingCost: Double = 3.99, promoDiscount: Double = 5.00) {
        viewModelScope.launch {
            val subtotal = cartSubtotal.value
            if (subtotal <= 0.0) return@launch
            val total = subtotal + shippingCost - promoDiscount
            val success = repository.createOrder(cartState.value, if (total > 0) total else 0.0)
            checkoutSuccess.value = success
        }
    }

    fun resetCheckoutSuccess() {
        checkoutSuccess.value = null
    }

    fun parseOrderItems(itemsJson: String): List<OrderProductInfo> {
        return repository.parseOrderItems(itemsJson)
    }

    // ViewModel Factory
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = AppDatabase.getDatabase(application)
            val repository = ShopRepository(database.shopDao())
            return ShopViewModel(application, repository) as T
        }
    }
}
