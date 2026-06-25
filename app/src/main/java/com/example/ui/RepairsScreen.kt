package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.Repair
import com.example.data.SparePartLog
import com.example.data.SyncResult
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepairsScreen(
    viewModel: RepairViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repairs by viewModel.repairs.collectAsStateWithLifecycle()
    val sparePartLogs by viewModel.sparePartLogs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()

    val syncCode by viewModel.syncCode.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncResult by viewModel.syncResult.collectAsStateWithLifecycle()

    var showSyncDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Reparaciones, 1 = Registrar Nuevo, 2 = Control de Repuestos
    var selectedRepairForDetails by remember { mutableStateOf<Repair?>(null) }

    // Repair Form State
    var brandModel by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var issueDescription by remember { mutableStateOf("") }
    var repairPrice by remember { mutableStateOf("") }
    var photoPaths by remember { mutableStateOf<List<String>>(emptyList()) }

    // Spare Part Form State
    var partName by remember { mutableStateOf("") }
    var partQuantity by remember { mutableStateOf("1") }
    var partCost by remember { mutableStateOf("") }
    var partTechnician by remember { mutableStateOf("") }

    // Camera/Gallery Launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val path = viewModel.savePhotoFromBitmap(bitmap)
            if (path != null) {
                photoPaths = photoPaths + path
            } else {
                Toast.makeText(context, "Error al guardar foto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val path = viewModel.savePhotoFromUri(uri)
            if (path != null) {
                photoPaths = photoPaths + path
            } else {
                Toast.makeText(context, "Error al procesar imagen de galería", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107), // Yellow gold
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Movil House Star",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = Color(0xFFFFC107)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF121212)
                ),
                actions = {
                    IconButton(
                        onClick = { showSyncDialog = true },
                        modifier = Modifier.testTag("app_bar_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sincronizar",
                            tint = if (syncCode.isNotBlank()) Color(0xFFFFC107) else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = { showShareDialog = true },
                        modifier = Modifier.testTag("app_bar_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir Aplicación",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(innerPadding)
        ) {
            // HERO ILLUSTRATION HEADER: Smiling Technician with Repair Uniform
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFFFFC107), RoundedCornerShape(16.dp))
            ) {
                // Background Technician Image
                Image(
                    painter = painterResource(id = R.drawable.img_technician),
                    contentDescription = "Técnico especialista Movil House Star",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark & Yellow Tint gradient overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Black.copy(alpha = 0.3f),
                                    Color(0xFFFFC107).copy(alpha = 0.15f)
                                )
                            )
                        )
                )

                // Text overlay with animation
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "¡SERVICIO TÉCNICO ACTIVO!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFC107),
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Movil House Star",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Calidad, Confianza y Rapidez",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            }

            // Tabs row
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color(0xFF1E1E1E),
                contentColor = Color(0xFFFFC107),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = Color(0xFFFFC107),
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Taller", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_workshop")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ingreso", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_entry")
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Repuestos", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_spare_parts")
                )
            }

            // Dynamic view with animated transition
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                modifier = Modifier.weight(1f)
            ) { targetTab ->
                when (targetTab) {
                    0 -> {
                        // TAB: WORKSHOP (Search, Filters, Repairs List)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // Search Box
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.searchQuery.value = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .testTag("search_input"),
                                placeholder = { Text("Buscar cliente, móvil, técnico...", color = Color.Gray) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color(0xFFFFC107)) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color.White)
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFFC107),
                                    unfocusedBorderColor = Color(0xFF333333),
                                    focusedLabelColor = Color(0xFFFFC107),
                                    cursorColor = Color(0xFFFFC107)
                                )
                            )

                            // Status Filters row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val filters = listOf(
                                    FilterType.ALL to "Todos",
                                    FilterType.PENDING to "Pendientes ⚙️",
                                    FilterType.REPAIRED to "Reparados ✅"
                                )
                                filters.forEach { (type, label) ->
                                    val isSelected = filterType == type
                                    val backgroundColor = if (isSelected) Color(0xFFFFC107) else Color(0xFF1E1E1E)
                                    val contentColor = if (isSelected) Color.Black else Color.White
                                    val borderColor = if (isSelected) Color(0xFFFFC107) else Color(0xFF333333)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(backgroundColor)
                                            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.filterType.value = type }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = contentColor
                                        )
                                    }
                                }
                            }

                            if (repairs.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Build,
                                            contentDescription = null,
                                            tint = Color(0xFFFFC107).copy(alpha = 0.4f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = if (searchQuery.isNotEmpty()) "Sin resultados de búsqueda" else "Taller vacío",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (searchQuery.isNotEmpty()) "Revisa si el cliente o móvil está bien escrito." else "No hay teléfonos en reparación. ¡Ingresa uno nuevo!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(repairs, key = { it.id }) { repair ->
                                        RepairListItem(
                                            repair = repair,
                                            onClick = { selectedRepairForDetails = repair },
                                            onCallClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${repair.clientPhone}"))
                                                context.startActivity(intent)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // TAB: REGISTRATION FORM
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Nuevo Ingreso de Móvil",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFC107)
                            )

                            OutlinedTextField(
                                value = brandModel,
                                onValueChange = { brandModel = it },
                                label = { Text("Marca y Modelo del Móvil") },
                                placeholder = { Text("Ej: Samsung Galaxy S23, iPhone 13") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_brand_model"),
                                leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFFFC107)) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFFC107),
                                    unfocusedBorderColor = Color(0xFF333333),
                                    focusedLabelColor = Color(0xFFFFC107)
                                )
                            )

                            OutlinedTextField(
                                value = clientName,
                                onValueChange = { clientName = it },
                                label = { Text("Nombre del Cliente") },
                                placeholder = { Text("Ej: Alejandro Pérez") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_client_name"),
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFFFC107)) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFFC107),
                                    unfocusedBorderColor = Color(0xFF333333),
                                    focusedLabelColor = Color(0xFFFFC107)
                                )
                            )

                            OutlinedTextField(
                                value = clientPhone,
                                onValueChange = { clientPhone = it },
                                label = { Text("Número de Contacto (Celular)") },
                                placeholder = { Text("Ej: 987654321") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_client_phone"),
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFFFFC107)) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFFC107),
                                    unfocusedBorderColor = Color(0xFF333333),
                                    focusedLabelColor = Color(0xFFFFC107)
                                )
                            )

                            OutlinedTextField(
                                value = issueDescription,
                                onValueChange = { issueDescription = it },
                                label = { Text("Problema / Falla Reportada") },
                                placeholder = { Text("Ej: Pantalla rota y cambio de conector de carga") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .testTag("input_issue"),
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFFC107)) },
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFFC107),
                                    unfocusedBorderColor = Color(0xFF333333),
                                    focusedLabelColor = Color(0xFFFFC107)
                                )
                            )

                            OutlinedTextField(
                                value = repairPrice,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.toDoubleOrNull() != null || input.all { it.isDigit() || it == '.' }) {
                                        repairPrice = input
                                    }
                                },
                                label = { Text("Precio de Reparación ($)") },
                                placeholder = { Text("Ej: 150.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_repair_price"),
                                leadingIcon = {
                                    Text(
                                        text = "$",
                                        color = Color(0xFFFFC107),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFFC107),
                                    unfocusedBorderColor = Color(0xFF333333),
                                    focusedLabelColor = Color(0xFFFFC107)
                                )
                            )

                            // Photos Input section
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1E1E1E)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Evidencia Fotográfica (${photoPaths.size}/3)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    if (photoPaths.isNotEmpty()) {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp)
                                        ) {
                                            items(photoPaths) { path ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(80.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .border(1.dp, Color(0xFFFFC107), RoundedCornerShape(8.dp))
                                                ) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(LocalContext.current)
                                                            .data(File(path))
                                                            .crossfade(true)
                                                            .build(),
                                                        contentDescription = "Foto ingresada",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    IconButton(
                                                        onClick = { photoPaths = photoPaths.filter { it != path } },
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .align(Alignment.TopEnd)
                                                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Close,
                                                            contentDescription = "Borrar",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (photoPaths.size < 3) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = { cameraLauncher.launch(null) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF333333),
                                                    contentColor = Color(0xFFFFC107)
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Cámara", fontSize = 13.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    galleryLauncher.launch(
                                                        androidx.activity.result.PickVisualMediaRequest(
                                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                                        )
                                                    )
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF333333),
                                                    contentColor = Color(0xFFFFC107)
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Galería", fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Button(
                                onClick = {
                                    if (brandModel.isBlank() || clientName.isBlank() || clientPhone.isBlank() || issueDescription.isBlank()) {
                                        Toast.makeText(context, "Por favor, complete todos los campos", Toast.LENGTH_LONG).show()
                                    } else {
                                        viewModel.insertRepair(
                                            brandModel = brandModel,
                                            clientName = clientName,
                                            clientPhone = clientPhone,
                                            issueDescription = issueDescription,
                                            photoPaths = photoPaths,
                                            price = repairPrice.toDoubleOrNull() ?: 0.0
                                        )
                                        Toast.makeText(context, "Móvil ingresado con éxito", Toast.LENGTH_SHORT).show()
                                        brandModel = ""
                                        clientName = ""
                                        clientPhone = ""
                                        issueDescription = ""
                                        repairPrice = ""
                                        photoPaths = emptyList()
                                        activeTab = 0
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("button_save_repair"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFC107),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Registrar Ingreso de Móvil", fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }
                    }
                    2 -> {
                        // TAB: CONTROL DE REPUESTOS DIARIOS (Daily Spare Parts Spent control)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Control de Repuestos del Día",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFC107)
                            )

                            // Quick entry form inside clean collapse-expand container or styled card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Registrar Gasto de Repuesto",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFFFFC107)
                                    )

                                    OutlinedTextField(
                                        value = partName,
                                        onValueChange = { partName = it },
                                        label = { Text("Repuesto utilizado") },
                                        placeholder = { Text("Ej: Pantalla Redmi Note 12, Conector tipo C") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFFFFC107),
                                            unfocusedBorderColor = Color(0xFF444444),
                                            focusedLabelColor = Color(0xFFFFC107)
                                        )
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = partQuantity,
                                            onValueChange = { partQuantity = it },
                                            label = { Text("Cantidad") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFFFFC107),
                                                unfocusedBorderColor = Color(0xFF444444),
                                                focusedLabelColor = Color(0xFFFFC107)
                                            )
                                        )

                                        OutlinedTextField(
                                            value = partCost,
                                            onValueChange = { partCost = it },
                                            label = { Text("Costo unitario") },
                                            placeholder = { Text("$ / S/.") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1.2f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFFFFC107),
                                                unfocusedBorderColor = Color(0xFF444444),
                                                focusedLabelColor = Color(0xFFFFC107)
                                            )
                                        )
                                    }

                                    OutlinedTextField(
                                        value = partTechnician,
                                        onValueChange = { partTechnician = it },
                                        label = { Text("Nombre del Técnico") },
                                        placeholder = { Text("Técnico que usó el repuesto") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFFFFC107),
                                            unfocusedBorderColor = Color(0xFF444444),
                                            focusedLabelColor = Color(0xFFFFC107)
                                        )
                                    )

                                    Button(
                                        onClick = {
                                            val qty = partQuantity.toIntOrNull() ?: 1
                                            val costVal = partCost.toDoubleOrNull() ?: 0.0
                                            if (partName.isBlank()) {
                                                Toast.makeText(context, "Escribe el nombre del repuesto", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.insertSparePartLog(
                                                    partName = partName,
                                                    quantity = qty,
                                                    cost = costVal,
                                                    technicianName = partTechnician
                                                )
                                                Toast.makeText(context, "Repuesto registrado", Toast.LENGTH_SHORT).show()
                                                partName = ""
                                                partQuantity = "1"
                                                partCost = ""
                                                partTechnician = ""
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFFC107),
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Agregar al Control del Día", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Compute stats for logged items
                            val totalCost = sparePartLogs.sumOf { it.cost * it.quantity }
                            val totalQty = sparePartLogs.sumOf { it.quantity }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Consumo Total", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text("$totalQty repuestos gastados", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Monto Total Invertido", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text(String.format("$ %.2f", totalCost), fontSize = 18.sp, color = Color(0xFFFFC107), fontWeight = FontWeight.Black)
                                }
                            }

                            // List of logged parts
                            if (sparePartLogs.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No se han registrado repuestos utilizados hoy.",
                                        color = Color.Gray,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(sparePartLogs, key = { it.id }) { log ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = log.partName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Color.White
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Cant: ${log.quantity}",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFFFFC107),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (log.technicianName.isNotEmpty()) {
                                                        Text(
                                                            text = "Téc: ${log.technicianName}",
                                                            fontSize = 12.sp,
                                                            color = Color.LightGray
                                                        )
                                                    }
                                                }
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                modifier = Modifier.padding(end = 8.dp)
                                            ) {
                                                Text(
                                                    text = String.format("$ %.2f", log.cost * log.quantity),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = formatCompactDate(log.date),
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteSparePartLog(log) },
                                                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Red.copy(alpha = 0.8f)),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Elegant footer signature in the bottom-right corner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107).copy(alpha = 0.5f),
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "fabricado por fernando",
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    color = Color.LightGray.copy(alpha = 0.5f)
                )
            }
        }
    }

    // Detail & Management Dialog
    selectedRepairForDetails?.let { repair ->
        RepairDetailsDialog(
            repair = repair,
            onDismiss = { selectedRepairForDetails = null },
            onUpdate = { isCompleted, techName, price ->
                viewModel.updateRepairStatus(repair, isCompleted, techName, price)
                selectedRepairForDetails = null
                Toast.makeText(context, "Estado actualizado con éxito", Toast.LENGTH_SHORT).show()
            },
            onDelete = {
                viewModel.deleteRepair(repair)
                selectedRepairForDetails = null
                Toast.makeText(context, "Registro eliminado", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showSyncDialog) {
        SyncDialog(
            currentCode = syncCode,
            isSyncing = isSyncing,
            syncResult = syncResult,
            onDismiss = { viewModel.syncResult.value = null; showSyncDialog = false },
            onSync = { code -> viewModel.syncWithCloud(code) }
        )
    }

    if (showShareDialog) {
        ShareAppDialog(
            onDismiss = { showShareDialog = false }
        )
    }
}

@Composable
fun RepairListItem(
    repair: Repair,
    onClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val isCompleted = repair.isCompleted
    val firstPhoto = repair.photoPath1 ?: repair.photoPath2 ?: repair.photoPath3

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("repair_item_${repair.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        border = BorderStroke(
            1.dp,
            if (isCompleted) Color(0xFF2E7D32).copy(alpha = 0.5f) else Color(0xFFFFC107).copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left photo thumbnail
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF121212))
                    .border(1.dp, Color(0xFFFFC107).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            ) {
                if (firstPhoto != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(firstPhoto))
                            .crossfade(true)
                            .build(),
                        contentDescription = "Miniatura móvil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.Center),
                        tint = Color(0xFFFFC107).copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = repair.brandModel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Compact status pill
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = if (isCompleted) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                        contentColor = if (isCompleted) Color(0xFF2E7D32) else Color(0xFFE65100)
                    ) {
                        Text(
                            text = if (isCompleted) "Listo" else "Pendiente",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Cliente: ${repair.clientName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Ingreso: ${formatCompactDate(repair.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", repair.price)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFC107)
                        )

                        if (repair.technicianName.isNotEmpty() && isCompleted) {
                            Text(
                                text = "• Téc: ${repair.technicianName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFC107).copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right call client action button
            IconButton(
                onClick = onCallClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xFFFFC107),
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .size(40.dp)
                    .testTag("call_client_button_${repair.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Llamar",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun RepairDetailsDialog(
    repair: Repair,
    onDismiss: () -> Unit,
    onUpdate: (isCompleted: Boolean, technicianName: String, price: Double) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var techNameInput by remember { mutableStateOf(repair.technicianName) }
    var priceInput by remember { mutableStateOf(if (repair.price > 0.0) String.format(Locale.US, "%.2f", repair.price) else "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var expandedPhotoPath by remember { mutableStateOf<String?>(null) }

    val photosList = listOfNotNull(repair.photoPath1, repair.photoPath2, repair.photoPath3)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E1E),
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = repair.brandModel,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Ficha de Ingreso #${repair.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF333333))

                // Customer info details
                Text(
                    text = "INFORMACIÓN DEL CLIENTE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFC107),
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = repair.clientName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Celular: ${repair.clientPhone}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${repair.clientPhone}"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC107),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Llamar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Problem detail
                Text(
                    text = "SÍNTOMA / PROBLEMA REPORTADO",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFC107),
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF121212)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF333333))
                ) {
                    Text(
                        text = repair.issueDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Photos View
                if (photosList.isNotEmpty()) {
                    Text(
                        text = "FOTOS DE INGRESO DEL EQUIPO",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFC107),
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(photosList) { path ->
                            Card(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clickable { expandedPhotoPath = path },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.3f))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(File(path))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Estado de ingreso",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Dates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FECHA DE INGRESO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatFullDate(repair.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }

                    if (repair.isCompleted && repair.completedAt != null) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FECHA DE ENTREGA",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatFullDate(repair.completedAt),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFF333333))

                // Actions inside dialog
                if (!repair.isCompleted) {
                    Text(
                        text = "GESTIONAR ENTREGA DE REPARACIÓN",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFC107),
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = techNameInput,
                        onValueChange = { techNameInput = it },
                        label = { Text("Nombre del Técnico asignado") },
                        placeholder = { Text("Nombre del técnico que reparó") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_technician_name"),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFFFC107)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFC107),
                            unfocusedBorderColor = Color(0xFF444444),
                            focusedLabelColor = Color(0xFFFFC107)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.toDoubleOrNull() != null || input.all { it.isDigit() || it == '.' }) {
                                priceInput = input
                            }
                        },
                        label = { Text("Precio Final de Reparación ($)") },
                        placeholder = { Text("Ej: 150.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_input_repair_price"),
                        leadingIcon = {
                            Text(
                                text = "$",
                                color = Color(0xFFFFC107),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFC107),
                            unfocusedBorderColor = Color(0xFF444444),
                            focusedLabelColor = Color(0xFFFFC107)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (techNameInput.isBlank()) {
                                Toast.makeText(context, "Por favor ingrese el nombre del técnico", Toast.LENGTH_SHORT).show()
                            } else {
                                onUpdate(true, techNameInput, priceInput.toDoubleOrNull() ?: 0.0)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("button_mark_repaired"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Entregar y Marcar Reparado", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Show Completed info box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1B5E20).copy(alpha = 0.3f),
                        contentColor = Color(0xFF81C784),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Móvil Reparado Exitosamente", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Técnico: ${repair.technicianName}", fontSize = 13.sp, color = Color.LightGray)
                                Text("Precio Cobrado: $${String.format(Locale.US, "%.2f", repair.price)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFC107))
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { onUpdate(false, repair.technicianName, repair.price) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("button_reopen_repair"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFFC107)
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reabrir Caso / Devolver a Pendiente", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Delete Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFE53935)
                        ),
                        modifier = Modifier.testTag("button_delete_repair")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eliminar Registro del Taller", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Expanded Photo Viewer Dialog
    expandedPhotoPath?.let { path ->
        Dialog(
            onDismissRequest = { expandedPhotoPath = null }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .border(1.dp, Color(0xFFFFC107), RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(path))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto ampliada",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                IconButton(
                    onClick = { expandedPhotoPath = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar Registro?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Esta acción es irreversible y eliminará todos los datos asociados, incluyendo las fotos.", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            iconContentColor = Color(0xFFFFC107),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }
}

// Helper Functions for Format Dates
fun formatCompactDate(timeInMillis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    return sdf.format(Date(timeInMillis))
}

fun formatFullDate(timeInMillis: Long): String {
    val sdf = SimpleDateFormat("dd 'de' MMMM, yyyy - hh:mm a", Locale.getDefault())
    return sdf.format(Date(timeInMillis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDialog(
    currentCode: String,
    isSyncing: Boolean,
    syncResult: SyncResult?,
    onDismiss: () -> Unit,
    onSync: (String) -> Unit
) {
    var codeInput by remember { mutableStateOf(currentCode) }

    AlertDialog(
        onDismissRequest = { if (!isSyncing) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Sincronización en la Nube",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Sincroniza tus datos de reparaciones y repuestos al instante entre múltiples dispositivos móviles utilizando un código único de taller.",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it.trim().lowercase() },
                    label = { Text("Código de Sincronización") },
                    placeholder = { Text("Ej: taller_central") },
                    singleLine = true,
                    enabled = !isSyncing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_code_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFC107),
                        unfocusedBorderColor = Color(0xFF444444),
                        focusedLabelColor = Color(0xFFFFC107),
                        cursorColor = Color(0xFFFFC107)
                    )
                )

                if (isSyncing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFFC107),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sincronizando...",
                            color = Color(0xFFFFC107),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (syncResult != null) {
                    when (syncResult) {
                        is SyncResult.Success -> {
                            Surface(
                                color = Color(0xFF1B5E20).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "¡Sincronización Exitosa!",
                                            color = Color(0xFF81C784),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Total: ${syncResult.repairsSynced} reparaciones y ${syncResult.partsSynced} repuestos sincronizados con el servidor.",
                                            color = Color.LightGray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                        is SyncResult.Error -> {
                            Surface(
                                color = Color(0xFFB71C1C).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFFF44336)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFF44336),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = syncResult.message,
                                        color = Color(0xFFE57373),
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF333333))

                Text(
                    text = "• Cualquier cambio en esta aplicación se guardará y sincronizará automáticamente en la nube de fondo si tienes un código activo.\n• Introduce exactamente el mismo código en tus otros teléfonos para que compartan la misma información en tiempo real.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSync(codeInput) },
                enabled = !isSyncing && codeInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107),
                    contentColor = Color.Black,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                    disabledContentColor = Color.LightGray.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Sincronizar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSyncing
            ) {
                Text("Cerrar", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF1A1A1A),
        iconContentColor = Color(0xFFFFC107),
        titleContentColor = Color.White,
        textContentColor = Color.LightGray
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareAppDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val appUrl = "https://ais-pre-qfxjhfoutql7r5u3zot7eo-504520486919.europe-west2.run.app"
    val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=512x512&data=$appUrl"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Compartir Movil House",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Beautiful storefront cover photo with rounded corners
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_cover),
                            contentDescription = "Portada Movil House con Técnico",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Gradient overlay for visual depth
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                    )
                                )
                        )
                        Text(
                            text = "Movil House Star - App Oficial",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        )
                    }
                }

                Text(
                    text = "Escanea el código QR para abrir la app directamente en tu celular o descarga el archivo instalable APK.",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // High-contrast clean white card for 100% reliable QR scanning
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .size(170.dp)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFFC107),
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            AsyncImage(
                                model = qrUrl,
                                contentDescription = "Código QR de descarga",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .testTag("download_qr_code")
                            )
                        }
                    }
                }

                Text(
                    text = "Código QR de Acceso y Descarga",
                    color = Color(0xFFFFC107),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Highlighted instructions card on how to download APK directly
                Surface(
                    color = Color(0xFFFFC107).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Guía: ¿Cómo descargar el APK?",
                                color = Color(0xFFFFC107),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "Para obtener el instalador oficial APK de un solo click, pulsa la opción 'Exportar APK' en la esquina superior derecha del panel de control de AI Studio en tu navegador.",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF333333))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy Link Button
                    OutlinedButton(
                        onClick = {
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Enlace Movil House Star", appUrl)
                            clipboardManager.setPrimaryClip(clip)
                            Toast.makeText(context, "¡Enlace copiado al portapapeles!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1.0f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color(0xFF444444))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copiar Link", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    // Native Share Intent Button
                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Descargar Movil House")
                                putExtra(Intent.EXTRA_TEXT, "¡Hola! Instala la aplicación de control de taller 'Movil House Star' escaneando el código QR o ingresando al enlace: $appUrl")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir vía"))
                        },
                        modifier = Modifier.weight(1.0f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC107),
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compartir", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Color(0xFFFFC107), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF1A1A1A),
        iconContentColor = Color(0xFFFFC107),
        titleContentColor = Color.White,
        textContentColor = Color.LightGray
    )
}

