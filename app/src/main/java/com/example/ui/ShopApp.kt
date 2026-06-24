package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.data.Product
import com.example.data.local.CartItem
import com.example.data.local.OrderEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopAppContent(viewModel: ShopViewModel) {
    val navController = rememberNavController()
    val cartCount by viewModel.cartItemCount.collectAsStateWithLifecycle()
    val checkoutSuccess by viewModel.checkoutSuccess.collectAsStateWithLifecycle()

    // Handle showing Checkout Success Modal
    if (checkoutSuccess == true) {
        OrderSuccessDialog(onDismiss = {
            viewModel.resetCheckoutSuccess()
            navController.navigate("orders") {
                popUpTo("home") { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        })
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            // Only show bottom navigation on main tabs (Home, Favorites, Cart, Orders)
            val showBottomBar = currentDestination?.route in listOf("home", "favorites", "cart", "orders")

            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    val navItems = listOf(
                        Triple("home", Icons.Default.Home, Icons.Outlined.Home to "Home"),
                        Triple("favorites", Icons.Default.Favorite, Icons.Outlined.FavoriteBorder to "Wishlist"),
                        Triple("cart", Icons.Default.ShoppingCart, Icons.Outlined.ShoppingCart to "Cart"),
                        Triple("orders", Icons.Default.Receipt, Icons.Outlined.ReceiptLong to "Orders")
                    )

                    navItems.forEach { (route, filledIcon, outlineAndLabel) ->
                        val (outlinedIcon, label) = outlineAndLabel
                        val isSelected = currentDestination?.hierarchy?.any { it.route == route } == true

                        NavigationBarItem(
                            modifier = Modifier.testTag("nav_item_$route"),
                            selected = isSelected,
                            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (route == "cart" && cartCount > 0) {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = Color.White
                                            ) {
                                                Text(cartCount.toString(), fontSize = 10.sp)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) filledIcon else outlinedIcon,
                                        contentDescription = label,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { productId ->
                        navController.navigate("detail/$productId")
                    }
                )
            }
            composable("favorites") {
                FavoritesScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { productId ->
                        navController.navigate("detail/$productId")
                    }
                )
            }
            composable("cart") {
                CartScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { productId ->
                        navController.navigate("detail/$productId")
                    },
                    onBrowseProducts = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }
            composable("orders") {
                OrdersScreen(viewModel = viewModel)
            }
            composable(
                route = "detail/{productId}",
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""
                ProductDetailScreen(
                    productId = productId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// ==========================================
// SCREEN 1: HOME SCREEN
// ==========================================
@Composable
fun HomeScreen(
    viewModel: ShopViewModel,
    onNavigateToDetail: (String) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val products by viewModel.filteredProducts.collectAsStateWithLifecycle()

    val categories = com.example.data.ProductCatalog.categories

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Brand Header Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = "DUDU Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DUDU",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Small Notification Icon and Avatar Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications")
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "D",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Elegant Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 4.dp)
                .testTag("search_input"),
            placeholder = { Text("Search 100,000+ items (Hoodies, Gadgets, Shoes...)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                } else {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Tune, contentDescription = "Filters")
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFFE0E0E0)
            ),
            shape = RoundedCornerShape(24.dp)
        )

        // Main Feed Scrollable List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 16.dp)
        ) {
            // Promo Slider (We render this when search query is empty)
            if (searchQuery.isBlank()) {
                item {
                    PromoBannersSection()
                }
            }

            // Categories list (Horizontal Row of chips)
            item {
                Text(
                    text = "Categories",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .testTag("category_chip_$category")
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color(0xFFEEEEEE)
                                )
                                .clickable { viewModel.selectCategory(category) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) Color.White else Color.Black,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Section title
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedCategory == "All") "Trending Highlights" else "$selectedCategory Collection",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${products.size} Items Found",
                        fontSize = 12.sp,
                        color = SlateTextSecondary
                    )
                }
            }

            // Custom responsive 2-column grid layout built manually to avoid nested LazyColumn-LazyVerticalGrid issues
            val chunkedProducts = products.chunked(2)
            items(chunkedProducts) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProductCard(
                        product = pair[0],
                        viewModel = viewModel,
                        onClick = { onNavigateToDetail(pair[0].id) },
                        modifier = Modifier.weight(1f)
                    )
                    if (pair.size > 1) {
                        ProductCard(
                            product = pair[1],
                            viewModel = viewModel,
                            onClick = { onNavigateToDetail(pair[1].id) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Handle empty states elegantly
            if (products.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = "No products",
                            tint = Color.Gray,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No matching products found",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.DarkGray
                        )
                        Text(
                            "Try searching for another keyword or check categories.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PromoBannersSection() {
    val promoBanners = listOf(
        Triple(
            "UP TO 70% OFF",
            "Super Summer Flash Sale! Use coupon: DUDU70",
            Brush.linearGradient(listOf(Color(0xFFFF5722), Color(0xFFFF9800)))
        ),
        Triple(
            "BUY 1 GET 1 FREE",
            "On premium shoes & accessories today only.",
            Brush.linearGradient(listOf(Color(0xFFE91E63), Color(0xFFFF4081)))
        ),
        Triple(
            "TECH & GADGET EXPO",
            "Get instant $10 off. Free express 3-day shipping.",
            Brush.linearGradient(listOf(Color(0xFF2196F3), Color(0xFF00BCD4)))
        )
    )

    LazyRow(
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(promoBanners) { (title, subtitle, gradient) ->
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(gradient)
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                // Background subtle patterns
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)))
                )

                Column {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = title,
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = subtitle,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Shop Now ➔",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    viewModel: ShopViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFav by viewModel.isProductFavorite(product.id).collectAsStateWithLifecycle()

    Card(
        modifier = modifier
            .testTag("product_card_${product.id}")
            .clickable { onClick() }
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Image Loading
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )

            // Discount Badge
            if (product.discountPercent > 0) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE53935))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = "-${product.discountPercent}%",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Favorite Button Layer
            IconButton(
                onClick = { viewModel.toggleFavorite(product) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.85f), CircleShape)
                    .testTag("favorite_button_${product.id}")
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (isFav) FavoritePink else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Product Information Block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = product.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = SlateTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Rating & Sales Count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Rating",
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${product.rating}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
                Text(
                    text = "•  ${product.salesCount}",
                    fontSize = 11.sp,
                    color = SlateTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pricing details & Add Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${product.price}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (product.originalPrice > product.price) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$${product.originalPrice}",
                            fontSize = 12.sp,
                            textDecoration = TextDecoration.LineThrough,
                            color = Color.Gray
                        )
                    }
                }

                // Instant Plus/Add to Cart Button
                IconButton(
                    onClick = {
                        // Quick Add: selects default first size and color options
                        val defaultSize = if (product.sizes.isNotEmpty()) product.sizes[0] else "Standard"
                        val defaultColor = if (product.colors.isNotEmpty()) product.colors[0] else "Default"
                        viewModel.addToCart(product, 1, defaultSize, defaultColor)
                    },
                    modifier = Modifier
                        .size(30.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag("quick_add_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Quick Add",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 2: PRODUCT DETAIL SCREEN
// ==========================================
@Composable
fun ProductDetailScreen(
    productId: String,
    viewModel: ShopViewModel,
    onBack: () -> Unit
) {
    val product = viewModel.getProductById(productId)
    val isFav by viewModel.isProductFavorite(productId).collectAsStateWithLifecycle()

    if (product == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Product not found.")
        }
        return
    }

    // Selected variants state
    var selectedSize by remember { mutableStateOf(if (product.sizes.isNotEmpty()) product.sizes[0] else "Standard") }
    var selectedColor by remember { mutableStateOf(if (product.colors.isNotEmpty()) product.colors[0] else "Default") }
    var quantity by remember { mutableStateOf(1) }
    var isDescExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(
                    onClick = { onBack() },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                Text(
                    text = "Product Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )

                IconButton(
                    onClick = { viewModel.toggleFavorite(product) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (isFav) FavoritePink else Color.Gray
                    )
                }
            }
        },
        bottomBar = {
            // Sticky Add to Cart control panel
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding(),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quantity adjust buttons on bottom bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFEEEEEE))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("qty_decrement")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease Quantity", modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = quantity.toString(),
                            modifier = Modifier.padding(horizontal = 12.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(
                            onClick = { quantity++ },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("qty_increment")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Quantity", modifier = Modifier.size(16.dp))
                        }
                    }

                    // Large Add to Cart button
                    Button(
                        onClick = {
                            viewModel.addToCart(product, quantity, selectedSize, selectedColor)
                            // Show success popup snackbar
                            scope.launch {
                                snackbarHostState.showSnackbar("Added $quantity item(s) to Cart!")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("add_to_cart_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = "Add Icon")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to Cart  •  $${String.format("%.2f", product.price * quantity)}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Product Large Hero Image
            item {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                )
            }

            // Information details
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Category & Sales Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = product.category.uppercase(),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = product.salesCount,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Title
                    Text(
                        text = product.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 26.sp,
                        color = SlateTextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Prices & Rating
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$${product.price}",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (product.originalPrice > product.price) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "$${product.originalPrice}",
                                    fontSize = 16.sp,
                                    textDecoration = TextDecoration.LineThrough,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${product.discountPercent}% OFF",
                                    color = Color.Red,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }

                        // Rating Card
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFF8E1))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Stars", tint = Color(0xFFFFA000), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${product.rating}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFFFA000))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("(${product.reviewCount})", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp), color = BorderLight)

                    // Description with Expand Toggle
                    Text("Description", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = product.description,
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        lineHeight = 20.sp,
                        maxLines = if (isDescExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isDescExpanded) "Read Less" else "Read More ➔",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { isDescExpanded = !isDescExpanded }
                            .padding(vertical = 6.dp)
                    )

                    Divider(modifier = Modifier.padding(vertical = 16.dp), color = BorderLight)

                    // Size Selection using smooth scrolling LazyRow instead of experimental FlowRow
                    if (product.sizes.isNotEmpty()) {
                        Text("Select Size", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(product.sizes) { size ->
                                val isSelected = selectedSize == size
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(24.dp))
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFCCCCCC),
                                            RoundedCornerShape(24.dp)
                                        )
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            else Color.Transparent
                                        )
                                        .clickable { selectedSize = size }
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = size,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Color Selection using smooth scrolling LazyRow instead of experimental FlowRow
                    if (product.colors.isNotEmpty()) {
                        Text("Select Color", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(product.colors) { colorName ->
                                val isSelected = selectedColor == colorName

                                // Resolve circular display colors
                                val colorValue = when (colorName.lowercase()) {
                                    "sage green" -> Color(0xFF8FBC8F)
                                    "charcoal black", "midnight black", "jet black", "matte black" -> Color(0xFF2C2C2C)
                                    "oatmeal cream", "cream white" -> Color(0xFFF5F5DC)
                                    "olive green" -> Color(0xFF556B2F)
                                    "desert khaki" -> Color(0xFFC3B091)
                                    "crimson red" -> Color(0xFFDC143C)
                                    "royal blue" -> Color(0xFF4169E1)
                                    "forest green" -> Color(0xFF228B22)
                                    "orchid pink", "soft ivory" -> Color(0xFFFFF0F5)
                                    "pure white" -> Color(0xFFFFFFFF)
                                    "space gray" -> Color(0xFF808080)
                                    "rose gold" -> Color(0xFFB76E79)
                                    "silver chrome" -> Color(0xFFC0C0C0)
                                    "lavender & eucalyptus" -> Color(0xFFE6E6FA)
                                    "nordic terracotta" -> Color(0xFFE2725B)
                                    else -> MaterialTheme.colorScheme.primary
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { selectedColor = colorName }
                                        .padding(vertical = 4.dp, horizontal = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(colorValue)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFDDDDDD),
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = colorName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Delivery Guarantee Box (Promotional detail like Temu/Shein)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = "Guarantee", tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("DUDU Delivery Guarantee", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2E7D32))
                                Text("• Free shipping on orders over $50", fontSize = 12.sp, color = Color(0xFF4C8C4A))
                                Text("• Delivered in ${product.deliveryDays} business days directly to your door", fontSize = 12.sp, color = Color(0xFF4C8C4A))
                                Text("• Free returns within 30 days of arrival", fontSize = 12.sp, color = Color(0xFF4C8C4A))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 3: WISHLIST (FAVORITES) SCREEN
// ==========================================
@Composable
fun FavoritesScreen(
    viewModel: ShopViewModel,
    onNavigateToDetail: (String) -> Unit
) {
    val favorites by viewModel.favoriteProducts.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Wishlist Title Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                "My Wishlist",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary
            )
        }

        if (favorites.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "Empty Wishlist",
                    tint = Color.LightGray,
                    modifier = Modifier.size(90.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Your wishlist is empty", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Keep track of items you love! Tap the heart icon on products to save them in this list.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 12.dp, top = 0.dp, end = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(favorites) { product ->
                    ProductCard(
                        product = product,
                        viewModel = viewModel,
                        onClick = { onNavigateToDetail(product.id) }
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 4: SHOPPING CART SCREEN
// ==========================================
@Composable
fun CartScreen(
    viewModel: ShopViewModel,
    onNavigateToDetail: (String) -> Unit,
    onBrowseProducts: () -> Unit
) {
    val cartList by viewModel.cartState.collectAsStateWithLifecycle()
    val subtotal by viewModel.cartSubtotal.collectAsStateWithLifecycle()

    // Configuration values
    val shippingCost = if (subtotal >= 50.0 || subtotal == 0.0) 0.0 else 3.99
    val promoDiscount = if (subtotal > 0.0) 5.00 else 0.00
    val total = if (subtotal > 0.0) (subtotal + shippingCost - promoDiscount) else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Shopping Cart", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (cartList.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearCart() },
                    modifier = Modifier.testTag("clear_cart_button")
                ) {
                    Text("Clear All", color = Color.Gray)
                }
            }
        }

        if (cartList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Empty Cart",
                    tint = Color.LightGray,
                    modifier = Modifier.size(90.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Your Cart is empty!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Find trendy items on DUDU and fill up your shopping cart.", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { onBrowseProducts() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Browse Products Now", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                // Cart Products Scrollable List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cartList) { (product, cartItem) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToDetail(product.id) },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = borderStroke()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Product small thumbnail
                                AsyncImage(
                                    model = product.imageUrl,
                                    contentDescription = product.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                // Information details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Size: ${cartItem.selectedSize}  •  Color: ${cartItem.selectedColor}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "$${product.price}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 15.sp
                                    )
                                }

                                // Adjust Quantity and Delete layout
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = { viewModel.removeFromCart(product.id) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete item", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFEEEEEE))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.updateCartQuantity(product.id, false) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(12.dp))
                                        }
                                        Text(
                                            text = cartItem.quantity.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        IconButton(
                                            onClick = { viewModel.updateCartQuantity(product.id, true) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Billing breakdown & checkout sheet
                Surface(
                    tonalElevation = 8.dp,
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .navigationBarsPadding()
                    ) {
                        // Promotion Coupon Promo Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF3E0))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalActivity, contentDescription = "Promo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Auto Coupon Applied: DUDU5OFF", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            }
                            Text("-$5.00", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Billing lines
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", color = Color.Gray, fontSize = 14.sp)
                            Text("$${String.format("%.2f", subtotal)}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Shipping", color = Color.Gray, fontSize = 14.sp)
                            Text(if (shippingCost == 0.0) "FREE" else "$${String.format("%.2f", shippingCost)}", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = if (shippingCost == 0.0) Color(0xFF2E7D32) else Color.Black)
                        }
                        if (subtotal < 50.0) {
                            Text(
                                "Add $${String.format("%.2f", 50.0 - subtotal)} more to qualify for FREE Shipping!",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Promo Discount", color = Color.Gray, fontSize = 14.sp)
                            Text("-$${String.format("%.2f", promoDiscount)}", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        Divider(modifier = Modifier.padding(vertical = 10.dp), color = BorderLight)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Total", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("$${String.format("%.2f", total)}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.checkout(shippingCost, promoDiscount) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("checkout_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = "Secure Checkout")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PLACE SECURE ORDER", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

fun borderStroke() = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)

// ==========================================
// SCREEN 5: ORDER HISTORY SCREEN
// ==========================================
@Composable
fun OrdersScreen(viewModel: ShopViewModel) {
    val ordersList by viewModel.orders.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("Order History", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        if (ordersList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ListAlt,
                    contentDescription = "No Orders",
                    tint = Color.LightGray,
                    modifier = Modifier.size(90.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("No orders placed yet", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Place your first order on DUDU and track your shopping shipments here!", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ordersList) { order ->
                    OrderHistoryCard(order = order, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun OrderHistoryCard(order: OrderEntity, viewModel: ShopViewModel) {
    var isExpanded by remember { mutableStateOf(false) }
    val items = viewModel.parseOrderItems(order.itemsJson)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = borderStroke()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Order Basic Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ORDER ID: #DUDU-${order.orderId + 12040}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = formatDate(order.orderDate),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Delivery status badge
                val statusColor = when (order.status.lowercase()) {
                    "processing" -> Color(0xFFFF9800)
                    "shipped" -> ShippedBlue
                    else -> SuccessGreen
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = order.status,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = BorderLight)

            // Preview thumbnails row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item thumbnails list (up to 4 item avatars)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items.take(4).forEach { item ->
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    }
                    if (items.size > 4) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+${items.size - 4}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Amount", fontSize = 11.sp, color = Color.Gray)
                    Text("$${String.format("%.2f", order.totalAmount)}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Expanded line-item list
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = BorderLight)
                    Text("Items Purchased", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))

                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Size: ${item.size}  •  Color: ${item.color}  •  Qty: ${item.quantity}", fontSize = 11.sp, color = Color.Gray)
                            }
                            Text("$${String.format("%.2f", item.price * item.quantity)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Expand Arrow Trigger
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(top = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isExpanded) "Collapse Details" else "View Order Details",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand Indicator",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Helper to format timestamps to reader-friendly formats
fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// Beautiful Dialog showing success on Checkout completed!
@Composable
fun OrderSuccessDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success animated icon placeholder
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success check",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Order Placed!", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Your secure payment was processed successfully. Thank you for shopping with DUDU!",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("TRACK SHIPMENT", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
