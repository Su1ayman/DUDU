package com.example.data

data class Product(
    val id: String,
    val title: String,
    val price: Double,
    val originalPrice: Double,
    val rating: Double,
    val reviewCount: Int,
    val imageUrl: String,
    val description: String,
    val category: String,
    val sizes: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val isFeatured: Boolean = false,
    val deliveryDays: Int = 3,
    val salesCount: String = "10K+ sold"
) {
    val discountPercent: Int
        get() = if (originalPrice > price) {
            (((originalPrice - price) / originalPrice) * 100).toInt()
        } else {
            0
        }
}

object ProductCatalog {
    val categories = listOf("All", "Fashion", "Shoes", "Electronics", "Beauty", "Home")

    val products = listOf(
        Product(
            id = "f1",
            title = "Aesthetic Oversized Graphic Hoodie",
            price = 24.99,
            originalPrice = 59.99,
            rating = 4.8,
            reviewCount = 1204,
            imageUrl = "https://images.unsplash.com/photo-1556821840-3a63f95609a7?auto=format&fit=crop&w=500&q=80",
            description = "Unleash your inner street-style with this premium heavyweight cotton oversized hoodie. Features breathable, durable fleece lining, retro front graphics, and ribbed cuffs. Perfect for casual wear, lounging, or styling with cargo pants.",
            category = "Fashion",
            sizes = listOf("S", "M", "L", "XL"),
            colors = listOf("Sage Green", "Charcoal Black", "Oatmeal Cream"),
            isFeatured = true,
            deliveryDays = 3,
            salesCount = "45K+ sold"
        ),
        Product(
            id = "f2",
            title = "Y2K High-Waist Wide Leg Cargo Pants",
            price = 29.99,
            originalPrice = 49.99,
            rating = 4.6,
            reviewCount = 840,
            imageUrl = "https://images.unsplash.com/photo-1541099649105-f69ad21f3246?auto=format&fit=crop&w=500&q=80",
            description = "Classic tactical cargo utility jeans with dynamic pocket layouts and adjustable toggle cuffs. Crafted from comfortable stretch cotton drill fabric that holds its shape all day long. Easy to pair with baby tees or hoodies.",
            category = "Fashion",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Olive Green", "Midnight Black", "Desert Khaki"),
            deliveryDays = 4,
            salesCount = "18K+ sold"
        ),
        Product(
            id = "s1",
            title = "Retro Streetwear High-Top Sneakers",
            price = 45.99,
            originalPrice = 89.99,
            rating = 4.9,
            reviewCount = 3120,
            imageUrl = "https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=500&q=80",
            description = "Voted best retro sneakers of the year. Designed with color-blocked vegan leather paneling, supportive padded ankle collars, and durable anti-skid rubber vulcanized soles. Walk in comfort while turning heads.",
            category = "Shoes",
            sizes = listOf("7", "8", "9", "10", "11"),
            colors = listOf("Crimson Red", "Royal Blue", "Forest Green"),
            isFeatured = true,
            deliveryDays = 3,
            salesCount = "30K+ sold"
        ),
        Product(
            id = "s2",
            title = "Platform Knit Running Shoes Lite",
            price = 34.99,
            originalPrice = 69.99,
            rating = 4.5,
            reviewCount = 512,
            imageUrl = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=500&q=80",
            description = "Extremely lightweight walking and running shoes with a breathable flyknit mesh upper and responsive cloud-foam thick platform soles. Provides excellent shock absorption and matches any activewear outfit.",
            category = "Shoes",
            sizes = listOf("6", "7", "8", "9", "10"),
            colors = listOf("Orchid Pink", "Pure White", "Jet Black"),
            deliveryDays = 5,
            salesCount = "12K+ sold"
        ),
        Product(
            id = "e1",
            title = "Pro Noise-Canceling Wireless Buds",
            price = 19.99,
            originalPrice = 79.99,
            rating = 4.7,
            reviewCount = 9520,
            imageUrl = "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?auto=format&fit=crop&w=500&q=80",
            description = "Immerse yourself in rich, high-fidelity audio. Features cutting-edge Active Noise Cancellation (ANC), ambient transparency mode, ultra-fast bluetooth 5.3 pairing, and a total of 40 hours of playtime with the sleek matte charging case.",
            category = "Electronics",
            sizes = listOf("Standard"),
            colors = listOf("Soft Ivory", "Matte Black", "Sky Blue"),
            isFeatured = true,
            deliveryDays = 2,
            salesCount = "100K+ sold"
        ),
        Product(
            id = "e2",
            title = "AMOLED Smart Sports Watch Series 5",
            price = 39.99,
            originalPrice = 99.99,
            rating = 4.6,
            reviewCount = 1420,
            imageUrl = "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=500&q=80",
            description = "Stay connected and track your health metrics. Features a beautiful 1.43\" always-on crystal-clear AMOLED display, real-time heart rate monitoring, blood-oxygen sleep analyzer, and 120+ specialized exercise tracking modes with IP68 swim protection.",
            category = "Electronics",
            sizes = listOf("40mm", "44mm"),
            colors = listOf("Space Gray", "Rose Gold", "Silver Chrome"),
            deliveryDays = 3,
            salesCount = "22K+ sold"
        ),
        Product(
            id = "b1",
            title = "Velvet Matte Liquid Lipstick Trio Set",
            price = 14.99,
            originalPrice = 34.99,
            rating = 4.8,
            reviewCount = 2304,
            imageUrl = "https://images.unsplash.com/photo-1586495777744-4413f21062fa?auto=format&fit=crop&w=500&q=80",
            description = "An award-winning trio of velvety matte long-wear liquid lipsticks. Transfer-proof, smudge-resistant, and infused with hydrating vitamin E and Jojoba oil to keep your lips exceptionally soft and moisturized for up to 16 hours.",
            category = "Beauty",
            sizes = listOf("3x 5ml Set"),
            colors = listOf("Nude Romance Set", "Bold Siren Set"),
            isFeatured = true,
            deliveryDays = 4,
            salesCount = "50K+ sold"
        ),
        Product(
            id = "b2",
            title = "Hydrating Hyaluronic Glow Face Serum",
            price = 12.99,
            originalPrice = 27.99,
            rating = 4.7,
            reviewCount = 1105,
            imageUrl = "https://images.unsplash.com/photo-1620916566398-39f1143ab7be?auto=format&fit=crop&w=500&q=80",
            description = "Instantly plump, hydrate, and brighten dry skin. Infused with 2.5% pure hyaluronic acid, soothing niacinamide, and organic green tea extracts. Dermatologist tested, 100% vegan, cruelty-free, and suitable for all skin types.",
            category = "Beauty",
            sizes = listOf("50ml", "100ml"),
            colors = listOf("Original"),
            deliveryDays = 3,
            salesCount = "15K+ sold"
        ),
        Product(
            id = "h1",
            title = "Lavender & Eucalyptus Scented Candles",
            price = 17.99,
            originalPrice = 39.99,
            rating = 4.9,
            reviewCount = 740,
            imageUrl = "https://images.unsplash.com/photo-1603006905003-be475563bc59?auto=format&fit=crop&w=500&q=80",
            description = "Set of two luxury aromatherapy candles hand-poured with 100% natural organic soy wax and organic essential oils. Features calming lavender and refreshing eucalyptus notes to reduce stress, aid sleep, and refresh any room.",
            category = "Home",
            sizes = listOf("2x 8oz Jars"),
            colors = listOf("Lavender & Eucalyptus"),
            deliveryDays = 4,
            salesCount = "8K+ sold"
        ),
        Product(
            id = "h2",
            title = "Minimalist Ceramic Ribbed Plant Pots",
            price = 19.99,
            originalPrice = 35.99,
            rating = 4.5,
            reviewCount = 380,
            imageUrl = "https://images.unsplash.com/photo-1485955900006-10f4d324d411?auto=format&fit=crop&w=500&q=80",
            description = "Add elegance to your houseplant collection. Includes a pair of hand-glazed ceramic ribbed pots with functional drainage holes and matching gold-accented saucers. Excellent centerpiece for your desk or living room.",
            category = "Home",
            sizes = listOf("Small + Medium"),
            colors = listOf("Cream White", "Sage Green", "Nordic Terracotta"),
            deliveryDays = 5,
            salesCount = "5K+ sold"
        )
    )

    fun getProductById(id: String): Product? {
        return products.find { it.id == id }
    }
}
