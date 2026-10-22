@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapp

import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView

import android.content.Context
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val APP_NAME = "BargainFits"

/* =====================================================================
 *  ACTIVITY
 * ===================================================================== */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = UserStore(applicationContext)
        setContent {
            BargainFitsTheme {
                var user by remember { mutableStateOf(store.currentUser()) }
                val current = user
                if (current == null) {
                    AuthScreen(
                        store = store,
                        onAuthenticated = { user = it }
                    )
                } else {
                    MainApp(
                        user = current,
                        onLogout = {
                            store.logout()
                            user = null
                        }
                    )
                }
            }
        }
    }
}

/* =====================================================================
 *  THEME
 * ===================================================================== */

private val AppColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCF0EA),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFFF59E0B),
    onSecondary = Color(0xFF2B1B00),
    secondaryContainer = Color(0xFFFFEFC7),
    onSecondaryContainer = Color(0xFF3D2900),
    tertiary = Color(0xFFE11D48),
    onTertiary = Color.White,
    background = Color(0xFFF6F8F7),
    onBackground = Color(0xFF191C1B),
    surface = Color.White,
    surfaceVariant = Color(0xFFE6ECEA),
    onSurfaceVariant = Color(0xFF3F4946),
    outline = Color(0xFF6F7976)
)

@Composable
fun BargainFitsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, content = content)
}

/* =====================================================================
 *  DATA
 * ===================================================================== */

data class User(val name: String, val email: String)

data class ClothingItem(
    val id: Int,
    val name: String,
    val category: String,
    val price: Int,
    val size: String,
    val condition: String,
    val location: String,
    val seller: String,
    val description: String,
    val ownerEmail: String? = null,
    val sold: Boolean = false,
    val imageUri: String? = null
)

val allCategories = listOf("Shirts", "Pants", "Jackets", "Shoes", "Watches", "Caps")

fun emojiFor(category: String): String = when (category) {
    "Jackets" -> "🧥"
    "Pants" -> "👖"
    "Shoes" -> "👟"
    "Watches" -> "⌚"
    "Caps" -> "🧢"
    else -> "👕"
}

fun tileColor(category: String): Color = when (category) {
    "Shirts" -> Color(0xFFDCEBFA)
    "Pants" -> Color(0xFFE8E4F8)
    "Jackets" -> Color(0xFFFFE3D1)
    "Shoes" -> Color(0xFFDFF3E3)
    "Watches" -> Color(0xFFFFF0C2)
    "Caps" -> Color(0xFFFBDDE4)
    else -> Color(0xFFE6ECEA)
}

fun sampleItems(): List<ClothingItem> = listOf(
    ClothingItem(
        1, "Vintage Denim Jacket", "Jackets", 350, "M", "Good", "Makati City", "Alex",
        "Classic washed denim with no tears. Slightly faded for that vintage look."
    ),
    ClothingItem(
        2, "Casual White Shirt", "Shirts", 120, "L", "Like New", "Taguig City", "Jamie",
        "Crisp cotton shirt, worn twice. Great for school or casual Fridays."
    ),
    ClothingItem(
        3, "Baggy Cargo Pants", "Pants", 280, "32", "Good", "Pasig City", "Chris",
        "Loose fit cargo pants with big side pockets. Waist 32."
    ),
    ClothingItem(
        4, "Classic Hoodie", "Jackets", 250, "L", "Good", "Parañaque City", "Sam",
        "Soft fleece hoodie, no stains or holes. Very comfy."
    ),
    ClothingItem(
        5, "Graphic Print Tee", "Shirts", 100, "M", "Like New", "Manila", "Taylor",
        "Black tee with a cool graphic print. Print is still sharp."
    ),
    ClothingItem(
        6, "Canvas Sneakers", "Shoes", 450, "US 8", "Good", "Quezon City", "Mia",
        "White canvas sneakers, cleaned and ready to wear. Minor sole wear."
    ),
    ClothingItem(
        7, "Digital Sports Watch", "Watches", 300, "One size", "Like New", "Makati City", "Ken",
        "Water resistant digital watch with stopwatch and alarm. Fresh battery."
    ),
    ClothingItem(
        8, "Black Baseball Cap", "Caps", 90, "Free size", "Good", "Pasig City", "Jamie",
        "Adjustable strap cap. Keeps its shape well."
    ),
    ClothingItem(
        9, "Slim Fit Jeans", "Pants", 320, "30", "Like New", "Taguig City", "Alex",
        "Dark blue slim fit jeans with a little stretch. Waist 30."
    ),
    ClothingItem(
        10, "Running Shoes", "Shoes", 600, "US 9", "Fair", "Parañaque City", "Chris",
        "Lightweight running shoes. Some wear on the soles but still lots of life left."
    )
)

/* ---------- Chat models ---------- */

class Message(val text: String, val fromMe: Boolean, val time: Long)

class Conversation(
    val itemId: Int,
    val itemName: String,
    val itemCategory: String,
    val sellerName: String
) {
    val messages = mutableStateListOf<Message>()
    var unread by mutableIntStateOf(0)
    val last: Message? get() = messages.lastOrNull()
}

fun seedConversations(): List<Conversation> {
    val now = System.currentTimeMillis()
    val min = 60_000L
    return listOf(
        Conversation(1, "Vintage Denim Jacket", "Jackets", "Alex").apply {
            messages += Message("Hi! Is this still available?", true, now - 50 * min)
            messages += Message("Yes, still available! 😊", false, now - 45 * min)
            messages += Message("Can you do ₱300?", true, now - 40 * min)
            messages += Message("Sure, ₱300 works. Meet up in Makati?", false, now - 12 * min)
            unread = 1
        },
        Conversation(5, "Graphic Print Tee", "Shirts", "Taylor").apply {
            messages += Message("Hello! Is the tee still for sale?", true, now - 130 * min)
            messages += Message("Yes po, it is!", false, now - 125 * min)
            messages += Message("I can meet you in Manila today if you want.", false, now - 124 * min)
            unread = 2
        },
        Conversation(4, "Classic Hoodie", "Jackets", "Sam").apply {
            messages += Message("What's the chest width of the hoodie?", true, now - 300 * min)
            messages += Message("About 22 inches flat. Fits a medium to large.", false, now - 290 * min)
            messages += Message("Thanks, I'll think about it!", true, now - 285 * min)
        }
    )
}

fun replyFor(text: String, count: Int): String {
    val lower = text.lowercase()
    return when {
        "available" in lower -> "Yes, it's still available! 😊"
        "price" in lower || "₱" in lower || "lower" in lower || "discount" in lower ->
            "I can lower it a little if you can meet up today."
        "meet" in lower || "where" in lower -> "I can meet in my area. What time works for you?"
        else -> listOf(
            "Okay, noted! 👍",
            "Thanks for your interest!",
            "Sure, let me know if you want to proceed."
        )[count % 3]
    }
}

fun formatTime(ms: Long): String {
    val now = System.currentTimeMillis()
    val dayFmt = SimpleDateFormat("yyyyMMdd", Locale.US)
    val sameDay = dayFmt.format(Date(ms)) == dayFmt.format(Date(now))
    return SimpleDateFormat(if (sameDay) "h:mm a" else "MMM d", Locale.getDefault()).format(Date(ms))
}

/* =====================================================================
 *  ACCOUNTS (stored on the phone; prototype only)
 * ===================================================================== */

class UserStore(context: Context) {
    private val prefs = context.getSharedPreferences("bargainfits_auth", Context.MODE_PRIVATE)

    private fun hash(email: String, password: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("$email:$password".toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun emailTaken(email: String): Boolean = prefs.contains("hash_${email.trim().lowercase()}")


    fun register(name: String, email: String, password: String) {
        val e = email.trim().lowercase()

        prefs.edit()
            .putString("name_$e", name.trim())
            .putString("hash_$e", hash(e, password))
            .apply()

    }


    fun login(email: String, password: String): User? {
        val e = email.trim().lowercase()
        val stored = prefs.getString("hash_$e", null) ?: return null
        if (stored != hash(e, password)) return null
        prefs.edit().putString("session", e).apply()
        return User(prefs.getString("name_$e", e) ?: e, e)
    }

    fun currentUser(): User? {
        val e = prefs.getString("session", null) ?: return null
        val name = prefs.getString("name_$e", null) ?: return null
        return User(name, e)
    }

    fun logout() {
        prefs.edit().remove("session").apply()
    }
}

/* =====================================================================
 *  SMALL SHARED UI PIECES
 * ===================================================================== */

@Composable
fun Avatar(name: String, size: Dp = 44.dp) {
    val palette = listOf(
        Color(0xFF0F766E), Color(0xFF7C3AED), Color(0xFFE11D48),
        Color(0xFFD97706), Color(0xFF2563EB)
    )
    val color = palette[(name.hashCode() and 0x7fffffff) % palette.size]
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp
        )
    }
}

@Composable
fun Tag(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ItemTile(item: ClothingItem, height: Dp, emojiSize: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(tileColor(item.category)),
        contentAlignment = Alignment.Center
    ) {
        if (!item.imageUri.isNullOrBlank()) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                },
                update = { imageView ->
                    imageView.setImageURI(Uri.parse(item.imageUri))
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(emojiFor(item.category), fontSize = emojiSize.sp)
        }

        if (item.sold) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "SOLD",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
            }
        }
    }
}

/* =====================================================================
 *  AUTH SCREEN (login + register)
 * ===================================================================== */


@Composable
fun AuthScreen(store: UserStore, onAuthenticated: (User) -> Unit) {
    var isLogin by remember { mutableStateOf(true) }

    // Separate login form state
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    // Separate registration form state
    var registerName by remember { mutableStateOf("") }
    var registerEmail by remember { mutableStateOf("") }
    var registerPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var success by remember { mutableStateOf("") }

    fun submit() {
        if (isLogin) {
            val user = store.login(loginEmail.trim(), loginPassword)

            if (user == null) {
                error = "Incorrect email or password."
                success = ""
            } else {
                onAuthenticated(user)
            }
        } else {
            val email = registerEmail.trim()

            error = when {
                registerName.isBlank() ->
                    "Please enter your name."

                !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    "Enter a valid email address."

                registerPassword.length < 6 ->
                    "Password must be at least 6 characters."

                registerPassword != confirmPassword ->
                    "Passwords do not match."

                store.emailTaken(email) ->
                    "That email is already registered. Try logging in."

                else -> ""
            }

            if (error.isEmpty()) {
                // Register the account, but DO NOT log the user in.
                store.register(
                    registerName,
                    email,
                    registerPassword
                )

                // Clear registration fields.
                registerName = ""
                registerEmail = ""
                registerPassword = ""
                confirmPassword = ""
                loginEmail = email
                loginPassword = ""

                // Return to the login tab.
                isLogin = true
                showPassword = false
                error = ""
                success = "Account created successfully! Please log in."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Text("🛍️", fontSize = 56.sp)

        Text(
            APP_NAME,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onPrimary
        )

        Text(
            "Great clothes. Affordable prices.",
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
        )

        Spacer(Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(Modifier.padding(bottom = 20.dp)) {
                TabRow(
                    selectedTabIndex = if (isLogin) 0 else 1,
                    containerColor = Color.White
                ) {
                    Tab(
                        selected = isLogin,
                        onClick = {
                            isLogin = true
                            error = ""
                            success = ""
                            showPassword = false
                        },
                        text = { Text("Log in") }
                    )

                    Tab(
                        selected = !isLogin,
                        onClick = {
                            isLogin = false
                            error = ""
                            success = ""
                            showPassword = false
                        },
                        text = { Text("Register") }
                    )
                }

                Column(
                    modifier = Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isLogin) {
                        OutlinedTextField(
                            value = registerName,
                            onValueChange = { registerName = it },
                            label = { Text("Full name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = if (isLogin) loginEmail else registerEmail,
                        onValueChange = {
                            if (isLogin) {
                                loginEmail = it
                            } else {
                                registerEmail = it
                            }
                        },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = if (isLogin) loginPassword else registerPassword,
                        onValueChange = {
                            if (isLogin) {
                                loginPassword = it
                            } else {
                                registerPassword = it
                            }
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        trailingIcon = {
                            TextButton(
                                onClick = { showPassword = !showPassword }
                            ) {
                                Text(if (showPassword) "Hide" else "Show")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!isLogin) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (error.isNotEmpty()) {
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }

                    if (success.isNotEmpty()) {
                        Text(
                            success,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = { submit() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            if (isLogin) "Log in" else "Create account",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


/* =====================================================================
 *  MAIN APP (tabs + overlays)
 * ===================================================================== */

@Composable
fun MainApp(user: User, onLogout: () -> Unit) {
    val products = remember { mutableStateListOf<ClothingItem>().apply { addAll(sampleItems()) } }
    val savedIds = remember { mutableStateListOf<Int>() }
    val conversations = remember {
        mutableStateListOf<Conversation>().apply { addAll(seedConversations()) }
    }

    var tab by remember { mutableIntStateOf(0) }
    var detailId by remember { mutableStateOf<Int?>(null) }
    var chatId by remember { mutableStateOf<Int?>(null) }
    var nextId by remember { mutableIntStateOf(100) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = chatId != null) { chatId = null }
    BackHandler(enabled = chatId == null && detailId != null) { detailId = null }
    BackHandler(enabled = chatId == null && detailId == null && tab != 0) { tab = 0 }

    fun openChat(item: ClothingItem) {
        if (conversations.none { it.itemId == item.id }) {
            conversations.add(Conversation(item.id, item.name, item.category, item.seller))
        }
        chatId = item.id
    }

    fun send(conv: Conversation, text: String) {
        conv.messages.add(Message(text, true, System.currentTimeMillis()))
        scope.launch {
            delay(1500)
            conv.messages.add(
                Message(replyFor(text, conv.messages.size), false, System.currentTimeMillis())
            )
            if (chatId != conv.itemId) conv.unread = conv.unread + 1
        }
    }

    fun toggleSaved(item: ClothingItem) {
        if (item.id in savedIds) savedIds.remove(item.id) else savedIds.add(item.id)
    }

    val chat = conversations.firstOrNull { it.itemId == chatId }
    val detailItem = products.firstOrNull { it.id == detailId }

    when {
        chat != null -> ChatThreadScreen(
            conv = chat,
            onBack = { chatId = null },
            onSend = { text -> send(chat, text) }
        )

        detailItem != null -> ItemDetailScreen(
            item = detailItem,
            isSaved = detailItem.id in savedIds,
            isMine = detailItem.ownerEmail == user.email,
            onBack = { detailId = null },
            onToggleSave = { toggleSaved(detailItem) },
            onChat = { openChat(detailItem) }
        )

        else -> {
            val totalUnread = conversations.sumOf { it.unread }
            Scaffold(
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    NavigationBar(containerColor = Color.White) {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text("Home") }
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                            label = { Text("Saved") }
                        )
                        NavigationBarItem(
                            selected = tab == 2,
                            onClick = { tab = 2 },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            label = { Text("Sell") }
                        )
                        NavigationBarItem(
                            selected = tab == 3,
                            onClick = { tab = 3 },
                            icon = {
                                BadgedBox(badge = {
                                    if (totalUnread > 0) Badge { Text("$totalUnread") }
                                }) {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                }
                            },
                            label = { Text("Messages") }
                        )
                        NavigationBarItem(
                            selected = tab == 4,
                            onClick = { tab = 4 },
                            icon = { Icon(Icons.Default.Person, contentDescription = null) },
                            label = { Text("Profile") }
                        )
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    when (tab) {
                        0 -> HomeScreen(
                            user = user,
                            products = products,
                            savedIds = savedIds,
                            onOpen = { detailId = it.id },
                            onToggleSave = { toggleSaved(it) }
                        )

                        1 -> SavedScreen(
                            products = products.filter { it.id in savedIds },
                            savedIds = savedIds,
                            onOpen = { detailId = it.id },
                            onToggleSave = { toggleSaved(it) }
                        )

                        2 -> SellScreen(
                            user = user,
                            onPublish = { draft ->
                                products.add(0, draft.copy(id = nextId))
                                nextId++
                                tab = 0
                                scope.launch { snackbar.showSnackbar("Listing published!") }
                            }
                        )

                        3 -> InboxScreen(
                            conversations = conversations,
                            onOpen = { chatId = it.itemId }
                        )

                        4 -> ProfileScreen(
                            user = user,
                            myListings = products.filter { it.ownerEmail == user.email },
                            savedCount = savedIds.size,
                            onMarkSold = { item ->
                                val i = products.indexOfFirst { it.id == item.id }
                                if (i >= 0) products[i] = item.copy(sold = true)
                            },
                            onDelete = { item ->
                                products.removeAll { it.id == item.id }
                                savedIds.remove(item.id)
                            },
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }
}

/* =====================================================================
 *  HOME + SAVED
 * ===================================================================== */

@Composable
fun HomeScreen(
    user: User,
    products: List<ClothingItem>,
    savedIds: SnapshotStateList<Int>,
    onOpen: (ClothingItem) -> Unit,
    onToggleSave: (ClothingItem) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    val q = search.trim()

    val filtered = products.filter {
        (category == "All" || it.category == category) &&
                (q.isEmpty() ||
                        it.name.contains(q, ignoreCase = true) ||
                        it.location.contains(q, ignoreCase = true) ||
                        it.category.contains(q, ignoreCase = true))
    }

    ItemGrid(
        list = filtered,
        savedIds = savedIds,
        onOpen = onOpen,
        onToggleSave = onToggleSave,
        emptyMessage = "No items match your search.",
        header = {
            Column {
                Text(
                    "Hello, ${user.name.substringBefore(' ')} 👋",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    "Find your next bargain fit",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search items or location") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("All") + allCategories) { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = {
                                Text(if (c == "All") "✨ All" else "${emojiFor(c)} $c")
                            }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Affordable finds",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
fun SavedScreen(
    products: List<ClothingItem>,
    savedIds: SnapshotStateList<Int>,
    onOpen: (ClothingItem) -> Unit,
    onToggleSave: (ClothingItem) -> Unit
) {
    ItemGrid(
        list = products,
        savedIds = savedIds,
        onOpen = onOpen,
        onToggleSave = onToggleSave,
        emptyMessage = "Nothing saved yet.\nTap the heart on a bargain you like!",
        header = {
            Text(
                "Saved items",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
        }
    )
}

@Composable
fun ItemGrid(
    list: List<ClothingItem>,
    savedIds: SnapshotStateList<Int>,
    onOpen: (ClothingItem) -> Unit,
    onToggleSave: (ClothingItem) -> Unit,
    emptyMessage: String,
    header: @Composable () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { header() }

        if (list.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        emptyMessage,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        items(list, key = { it.id }) { item ->
            ItemCard(
                item = item,
                isSaved = item.id in savedIds,
                onClick = { onOpen(item) },
                onToggleSave = { onToggleSave(item) }
            )
        }
    }
}

@Composable
fun ItemCard(
    item: ClothingItem,
    isSaved: Boolean,
    onClick: () -> Unit,
    onToggleSave: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box {
            ItemTile(item, height = 130.dp, emojiSize = 60)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f))
                    .clickable(onClick = onToggleSave),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isSaved) "Unsave" else "Save",
                    tint = if (isSaved) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        Column(Modifier.padding(10.dp)) {
            Text(
                item.name,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "₱${item.price}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${item.size} • ${item.condition}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Text(
                    item.location,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/* =====================================================================
 *  ITEM DETAIL
 * ===================================================================== */

@Composable
fun ItemDetailScreen(
    item: ClothingItem,
    isSaved: Boolean,
    isMine: Boolean,
    onBack: () -> Unit,
    onToggleSave: () -> Unit,
    onChat: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.category) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleSave) {
                        Icon(
                            if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Save",
                            tint = if (isSaved) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, modifier = Modifier.navigationBarsPadding()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    when {
                        isMine -> Text(
                            "This is your listing",
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        item.sold -> Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Sold") }

                        else -> Button(
                            onClick = onChat,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("💬  Chat with ${item.seller}", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            ItemTile(item, height = 260.dp, emojiSize = 110)

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "₱${item.price}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(item.name, style = MaterialTheme.typography.titleLarge)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tag("Size ${item.size}")
                    Tag(item.condition)
                    Tag(item.category)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(item.location, color = MaterialTheme.colorScheme.outline)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(item.seller)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(item.seller, fontWeight = FontWeight.Bold)
                            Text(
                                "Seller",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Text("Description", fontWeight = FontWeight.Bold)
                Text(item.description.ifBlank { "No description provided." })
            }
        }
    }
}

/* =====================================================================
 *  SELL
 * ===================================================================== */

@Composable
fun SellScreen(user: User, onPublish: (ClothingItem) -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Shirts") }
    var size by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("Good") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Sell your clothes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Listing as ${user.name}",
                color = MaterialTheme.colorScheme.outline
            )
        }

        item {
            Text("Product photo", fontWeight = FontWeight.Bold)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(tileColor(category)),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AndroidView(
                        factory = { context ->
                            ImageView(context).apply {
                                scaleType = ImageView.ScaleType.CENTER_CROP
                            }
                        },
                        update = { imageView ->
                            imageView.setImageURI(selectedImageUri)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(emojiFor(category), fontSize = 56.sp)
                        Text(
                            "No photo selected",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Button(
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (selectedImageUri == null) "Choose photo from gallery"
                    else "Change photo"
                )
            }
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = price,
                onValueChange = { price = it.filter { c -> c.isDigit() } },
                label = { Text("Price") },
                prefix = { Text("₱") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text("Category", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allCategories) { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { category = c },
                        label = { Text("${emojiFor(c)} $c") }
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = size,
                onValueChange = { size = it },
                label = { Text("Size (e.g. M, L, 32, US 8)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text("Condition", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Like New", "Good", "Fair").forEach { c ->
                    FilterChip(
                        selected = condition == c,
                        onClick = { condition = c },
                        label = { Text(c) }
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("City or pickup area") },
                placeholder = { Text("e.g. Makati City") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            if (error.isNotEmpty()) {
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
            }
            Button(
                onClick = {
                    val amount = price.toIntOrNull()
                    if (name.isBlank() || amount == null || amount <= 0 ||
                        size.isBlank() || location.isBlank()
                    ) {
                        error = "Enter a name, price, size, and location."
                    } else {
                        error = ""
                        onPublish(
                            ClothingItem(
                                id = 0, // real id is assigned by the app
                                name = name.trim(),
                                category = category,
                                price = amount,
                                size = size.trim(),
                                condition = condition,
                                location = location.trim(),
                                seller = user.name,
                                description = description.trim(),
                                ownerEmail = user.email,
                                imageUri = selectedImageUri?.toString()
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Publish listing", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* =====================================================================
 *  MESSAGES: INBOX + CHAT THREAD
 * ===================================================================== */

@Composable
fun InboxScreen(conversations: List<Conversation>, onOpen: (Conversation) -> Unit) {
    val inbox = conversations
        .filter { it.messages.isNotEmpty() }
        .sortedByDescending { it.last?.time ?: 0L }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Messages",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        if (inbox.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No messages yet.\nOpen a listing and tap Chat to get started.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(inbox, key = { it.itemId }) { conv ->
                    InboxRow(conv) { onOpen(conv) }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 76.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun InboxRow(conv: Conversation, onClick: () -> Unit) {
    val last = conv.last
    val hasUnread = conv.unread > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (hasUnread) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(conv.sellerName, size = 48.dp)
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    conv.sellerName,
                    fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (last != null) {
                    Text(
                        formatTime(last.time),
                        fontSize = 12.sp,
                        color = if (hasUnread) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                }
            }
            Text(
                "${emojiFor(conv.itemCategory)} ${conv.itemName}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (if (last?.fromMe == true) "You: " else "") + (last?.text ?: ""),
                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (hasUnread) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${conv.unread}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatThreadScreen(
    conv: Conversation,
    onBack: () -> Unit,
    onSend: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Opening the thread (or receiving a message while it's open) marks it as read.
    LaunchedEffect(conv.messages.size) {
        conv.unread = 0
        if (conv.messages.isNotEmpty()) {
            listState.animateScrollToItem(conv.messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(conv.sellerName, size = 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(conv.sellerName, fontWeight = FontWeight.Bold)
                            Text(
                                "${emojiFor(conv.itemCategory)} ${conv.itemName}",
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
        ) {
            if (conv.messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("👋", fontSize = 48.sp)
                    Text(
                        "Say hi to ${conv.sellerName}",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    listOf(
                        "Is this still available?",
                        "Can you lower the price?",
                        "Where can we meet?"
                    ).forEach { quick ->
                        AssistChip(
                            onClick = { onSend(quick) },
                            label = { Text(quick) },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(conv.messages) { msg -> MessageBubble(msg) }
                }
            }

            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text("Type a message") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val text = draft.trim()
                            if (text.isNotEmpty()) {
                                onSend(text)
                                draft = ""
                            }
                        },
                        enabled = draft.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (draft.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (draft.isNotBlank()) Color.White
                            else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(msg: Message) {
    val mine = msg.fromMe
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomEnd = if (mine) 4.dp else 16.dp,
                    bottomStart = if (mine) 16.dp else 4.dp
                ),
                color = if (mine) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    msg.text,
                    color = if (mine) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
            Text(
                formatTime(msg.time),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

/* =====================================================================
 *  PROFILE
 * ===================================================================== */

@Composable
fun ProfileScreen(
    user: User,
    myListings: List<ClothingItem>,
    savedCount: Int,
    onMarkSold: (ClothingItem) -> Unit,
    onDelete: (ClothingItem) -> Unit,
    onLogout: () -> Unit
) {
    var deleteTarget by remember { mutableStateOf<ClothingItem?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Avatar(user.name, size = 72.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        user.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(user.email, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatBox("Listings", myListings.size)
                        StatBox("Sold", myListings.count { it.sold })
                        StatBox("Saved", savedCount)
                    }
                }
            }
        }

        item {
            Text(
                "My listings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (myListings.isEmpty()) {
            item {
                Text(
                    "You haven't listed anything yet. Tap Sell to post your first item!",
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        items(myListings, key = { it.id }) { item ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tileColor(item.category)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emojiFor(item.category), fontSize = 28.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.name,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "₱${item.price}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        if (item.sold) Tag("Sold")
                    }
                    if (!item.sold) {
                        TextButton(onClick = { onMarkSold(item) }) { Text("Mark sold") }
                    }
                    IconButton(onClick = { deleteTarget = item }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Log out") }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete listing?") },
            text = { Text("\"${target.name}\" will be removed from the marketplace.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun StatBox(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "$value",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
    }
}
