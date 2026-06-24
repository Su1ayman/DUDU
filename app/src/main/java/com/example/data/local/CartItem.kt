package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey val productId: String,
    val quantity: Int,
    val selectedSize: String,
    val selectedColor: String,
    val addedTime: Long = System.currentTimeMillis()
)
