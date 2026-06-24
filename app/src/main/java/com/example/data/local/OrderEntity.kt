package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val orderId: Int = 0,
    val orderDate: Long = System.currentTimeMillis(),
    val status: String, // "Processing", "Shipped", "Delivered"
    val totalAmount: Double,
    val itemsJson: String // Serialized JSON string of the purchased products
)

data class OrderProductInfo(
    val productId: String,
    val title: String,
    val price: Double,
    val quantity: Int,
    val size: String,
    val color: String,
    val imageUrl: String
)
