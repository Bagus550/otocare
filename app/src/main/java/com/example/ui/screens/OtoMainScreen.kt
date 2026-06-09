package com.example.ui.screens

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.viewmodel.OtoViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtoMainScreen(
    viewModel: OtoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val selectedVehicle by viewModel.selectedVehicle.collectAsStateWithLifecycle()
    val schedules by viewModel.vehicleSchedules.collectAsStateWithLifecycle()
    val logs by viewModel.vehicleLogs.collectAsStateWithLifecycle()

    val monthlyStats by viewModel.monthlyStats.collectAsStateWithLifecycle()
    val categoryStats by viewModel.categoryStats.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(0) }

    // Dialog sheets state
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showUpdateKmsDialog by remember { mutableStateOf(false) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var showAddLogDialog by remember { mutableStateOf(false) }
    var activeCompletionSchedule by remember { mutableStateOf<MaintenanceSchedule?>(null) }
    var vehicleToDelete by remember { mutableStateOf<Vehicle?>(null) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var showResetOilDialog by remember { mutableStateOf(false) }
    var showEditVehicleDialog by remember { mutableStateOf(false) }

    // Android 13+ Notification Permission launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Izin notifikasi diberikan!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Izin notifikasi ditolak. Anda tidak akan melihat pengingat di status bar.", Toast.LENGTH_LONG).show()
        }
    }

    // Single trigger of permission request on startup if needed
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Monitoring Kendaraan",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "OtoCare",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                },
                actions = {
                    // Decorative high-density notification badge card
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            )
                            .clickable {
                                viewModel.testNotificationPreview("Oli Mesin Berumur 80%")
                                Toast.makeText(context, "Mengirim Pengingat Notifikasi...", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // FAQ & Panduan Button
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape
                            )
                            .clickable {
                                showFaqDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = "FAQ & Panduan",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Decorative high-density user avatar
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (vehicles.isNotEmpty() && selectedVehicle != null) {
                        // Vehicle Picker Button
                        var expandedPicker by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { expandedPicker = true },
                            modifier = Modifier.testTag("vehicle_picker_button")
                        ) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = "Pilih Kendaraan")
                        }
                        DropdownMenu(
                            expanded = expandedPicker,
                            onDismissRequest = { expandedPicker = false }
                        ) {
                            vehicles.forEach { valVeh ->
                                DropdownMenuItem(
                                    text = { Text("${valVeh.name} (${valVeh.plateNumber})") },
                                    onClick = {
                                        viewModel.selectVehicle(valVeh.id)
                                        expandedPicker = false
                                        Toast.makeText(context, "Memilih ${valVeh.name}", Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (valVeh.type == "Motor") Icons.Default.TwoWheeler else Icons.Default.DirectionsCar,
                                            contentDescription = null
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                vehicleToDelete = valVeh
                                                expandedPicker = false
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Kendaraan",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("+ Tambah Kendaraan") },
                                onClick = {
                                    showAddVehicleDialog = true
                                    expandedPicker = false
                                },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { showAddVehicleDialog = true },
                            modifier = Modifier.testTag("add_vehicle_shortcut")
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Tambah Kendaraan")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dasbor") },
                    label = { Text("Dasbor") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Jadwal") },
                    label = { Text("Jadwal") }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Riwayat") },
                    label = { Text("Riwayat") }
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Statistik") },
                    label = { Text("Statistik") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (vehicles.isEmpty()) {
                // Empty state requesting a vehicle to be added
                OtoEmptyState(
                    title = "Belum Ada Kendaraan",
                    description = "Silakan tambahkan kendaraan pertama Anda agar dapat memantau jadwal perawatan & pengeluaran ganti oli.",
                    buttonText = "Tambah Kendaraan Pertama",
                    onClickButton = { showAddVehicleDialog = true }
                )
            } else {
                selectedVehicle?.let { vehicle ->
                    Crossfade(
                        targetState = currentTab,
                        label = "TabTransition"
                    ) { tab ->
                        when (tab) {
                            0 -> OtoDashboardTab(
                                vehicle = vehicle,
                                schedules = schedules,
                                logs = logs,
                                onUpdateKmClick = { showUpdateKmsDialog = true },
                                onResetOilClick = { showResetOilDialog = true },
                                onEditVehicleClick = { showEditVehicleDialog = true },
                                onTestNotification = { viewModel.testNotificationPreview("Oli Mesin Berumur 80%") },
                                onDeleteVehicleClick = { vehicleToDelete = vehicle },
                                onHelpClick = { showFaqDialog = true }
                            )
                            1 -> OtoSchedulesTab(
                                schedules = schedules,
                                onAddScheduleClick = { showAddScheduleDialog = true },
                                onCompleteClick = { activeCompletionSchedule = it },
                                onDeleteClick = { viewModel.deleteSchedule(it) }
                            )
                            2 -> OtoHistoryLogsTab(
                                logs = logs,
                                onAddLogClick = { showAddLogDialog = true },
                                onDeleteLogClick = { viewModel.deleteLog(it) }
                            )
                            3 -> OtoBeautyStatsTab(
                                monthlyStats = monthlyStats,
                                categoryStats = categoryStats,
                                logs = logs
                            )
                        }
                    }
                }
            }

            // DIALOGS LISTS
            if (showAddVehicleDialog) {
                OtoAddVehicleDialog(
                    onDismiss = { showAddVehicleDialog = false },
                    onConfirm = { name, plat, type, odo, lastOilOdo, lastOilDate, interval, oilBrand ->
                        viewModel.addVehicle(name, plat, type, odo, lastOilOdo, lastOilDate, interval, oilBrand)
                        showAddVehicleDialog = false
                        Toast.makeText(context, "$name berhasil didaftarkan!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            if (showUpdateKmsDialog) {
                selectedVehicle?.let { vehicle ->
                    OtoUpdateOdometerDialog(
                        currentOdo = vehicle.currentOdometer,
                        onDismiss = { showUpdateKmsDialog = false },
                        onConfirm = { newOdo ->
                            viewModel.updateVehicleMileage(vehicle, newOdo)
                            showUpdateKmsDialog = false
                            Toast.makeText(context, "Mileage/KM diperbarui ke $newOdo km!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            if (showAddScheduleDialog) {
                OtoAddScheduleDialog(
                    onDismiss = { showAddScheduleDialog = false },
                    onConfirm = { title, targetDate, targetOdo, notes ->
                        viewModel.addSchedule(title, targetDate, targetOdo, notes)
                        showAddScheduleDialog = false
                        Toast.makeText(context, "Jadwal '$title' dijadwalkan!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            if (showAddLogDialog) {
                OtoAddLogDialog(
                    onDismiss = { showAddLogDialog = false },
                    onConfirm = { title, category, date, cost, odo, notes ->
                        viewModel.addMaintenanceLog(title, category, date, cost, odo, notes)
                        showAddLogDialog = false
                        Toast.makeText(context, "Log perbaikan berhasil dicatat!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            activeCompletionSchedule?.let { schedule ->
                OtoCompleteScheduleDialog(
                    schedule = schedule,
                    onDismiss = { activeCompletionSchedule = null },
                    onConfirm = { odo, cost, notes ->
                        viewModel.completeSchedule(schedule, odo, cost, notes)
                        activeCompletionSchedule = null
                        Toast.makeText(context, "Jadwal diselesaikan dan dimasukkan ke riwayat!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            vehicleToDelete?.let { vehicle ->
                AlertDialog(
                    onDismissRequest = { vehicleToDelete = null },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteVehicle(vehicle)
                                vehicleToDelete = null
                                Toast.makeText(context, "${vehicle.name} berhasil dihapus!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Hapus", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { vehicleToDelete = null }) {
                            Text("Batal")
                        }
                    },
                    title = { Text("Hapus Kendaraan") },
                    text = { Text("Apakah Anda yakin ingin menghapus ${vehicle.name}? Semua riwayat servis dan jadwal perawatan terkait akan terhapus.") }
                )
            }

            if (showFaqDialog) {
                OtoFaqAndGuideDialog(
                    onDismiss = { showFaqDialog = false }
                )
            }

            selectedVehicle?.let { vehicle ->
                if (showResetOilDialog) {
                    OtoResetOilDialog(
                        vehicle = vehicle,
                        onDismiss = { showResetOilDialog = false },
                        onConfirm = { targetOdo, brand, cost, notes ->
                            viewModel.addMaintenanceLog(
                                title = "Ganti Oli - $brand",
                                category = "Oli",
                                date = System.currentTimeMillis(),
                                cost = cost,
                                odometer = targetOdo,
                                notes = if (notes.isEmpty()) "Ganti oli divalidasi langsung via tombol pengingat" else notes
                            )
                            showResetOilDialog = false
                            Toast.makeText(context, "Odometer oli disetel ulang & riwayat tercatat!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                if (showEditVehicleDialog) {
                    OtoEditVehicleDialog(
                        vehicle = vehicle,
                        onDismiss = { showEditVehicleDialog = false },
                        onConfirm = { updated ->
                            viewModel.updateVehicle(updated)
                            showEditVehicleDialog = false
                            Toast.makeText(context, "Atribut kendaraan berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

// FORMAT UTIL
fun formatRupiah(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount).replace(",00", "")
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}

@Composable
fun OtoFixedProgressIndicator(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(5.dp))
                .background(color)
        )
    }
}

// CUSTOM EMPTY STATE
@Composable
fun OtoEmptyState(
    title: String,
    description: String,
    buttonText: String,
    onClickButton: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .wrapContentHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onClickButton,
                modifier = Modifier.testTag("add_vehicle_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = buttonText)
            }
        }
    }
}

// TAB 1: DASBOR
@Composable
fun OtoDashboardTab(
    vehicle: Vehicle,
    schedules: List<MaintenanceSchedule>,
    logs: List<MaintenanceLog>,
    onUpdateKmClick: () -> Unit,
    onResetOilClick: () -> Unit,
    onEditVehicleClick: () -> Unit,
    onTestNotification: () -> Unit,
    onDeleteVehicleClick: () -> Unit,
    onHelpClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Vehicle Card Details (High Density Style)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (vehicle.type == "Motor") Icons.Default.TwoWheeler else Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = vehicle.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${vehicle.plateNumber} • ${vehicle.currentOdometer} km",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "UTAMA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = onEditVehicleClick,
                            modifier = Modifier.size(32.dp).testTag("edit_vehicle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Atribut Kendaraan",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDeleteVehicleClick,
                            modifier = Modifier.size(32.dp).testTag("delete_vehicle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Kendaraan",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // New User Guide Hero Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHelpClick() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Baru di OtoCare? Lihat Panduan Pintar",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pelajari cara melacak sisa umur oli, mengelola jadwal servis, dan mencatat riwayat pembiayaan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Buka",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // STATISTICS GRID (High Density Layout)
        item {
            // Compute dynamic monthly expenses from logs
            val currentCalendar = Calendar.getInstance()
            val currentMonth = currentCalendar.get(Calendar.MONTH)
            val currentYear = currentCalendar.get(Calendar.YEAR)
            val thisMonthLogs = logs.filter { log ->
                val logCalendar = Calendar.getInstance().apply { timeInMillis = log.date }
                logCalendar.get(Calendar.MONTH) == currentMonth && logCalendar.get(Calendar.YEAR) == currentYear
            }
            val thisMonthExpense = thisMonthLogs.sumOf { it.cost }
            val formattedExpense = if (thisMonthExpense >= 1_000_000) {
                String.format(Locale.US, "Rp%.1fJT", thisMonthExpense / 1_000_000.0)
            } else if (thisMonthExpense >= 1000) {
                "Rp${(thisMonthExpense / 1000).toInt()}k"
            } else {
                formatRupiah(thisMonthExpense)
            }

            // Compute dynamic health status based on oil progress
            val distanceRemaining = (vehicle.lastOilChangeOdometer + vehicle.oilChangeIntervalKm) - vehicle.currentOdometer
            val progressPercent = if (distanceRemaining <= 0) 0f else (distanceRemaining.toFloat() / vehicle.oilChangeIntervalKm.toFloat()).coerceIn(0f, 1f)
            val healthPercent = (progressPercent * 100).toInt()
            val isUrgent = distanceRemaining <= 500

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Col 1: Monthly Cost
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "BIAYA (BULAN INI)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedExpense,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(if (thisMonthExpense > 500_000) 1.0f else (thisMonthExpense / 500_000.0).toFloat().coerceIn(0f, 1f))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }

                // Col 2: Condition / Health State
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "KONDISI KENDARAAN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$healthPercent%",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isUrgent) MaterialTheme.colorScheme.error else Color(0xFF43A047)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isUrgent) "Butuh perhatian segera" else "Kondisi sangat prima",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        // PENGINGAT GANTI OLI CARD (OIL MONITOR)
        item {
            val odometerGap = vehicle.currentOdometer - vehicle.lastOilChangeOdometer
            val oilTargetOdometer = vehicle.lastOilChangeOdometer + vehicle.oilChangeIntervalKm
            val distanceRemaining = oilTargetOdometer - vehicle.currentOdometer

            // Percentage remaining calculation
            val progressPercent = if (distanceRemaining <= 0) 0f else (distanceRemaining.toFloat() / vehicle.oilChangeIntervalKm.toFloat()).coerceIn(0f, 1f)
            val isUrgent = distanceRemaining <= 500

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pengingat Ganti Oli",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        if (isUrgent) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.error
                            ) {
                                Text(
                                    text = "Oli Menipis!",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF2E7D32)
                            ) {
                                Text(
                                    text = "Oli Aman",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Progress Radial Arc adaptive to onPrimary
                        Box(
                            modifier = Modifier.size(70.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val emptyIndicator = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                            val activeIndicator = MaterialTheme.colorScheme.onPrimary
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = emptyIndicator,
                                    startAngle = 135f,
                                    sweepAngle = 270f,
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = activeIndicator,
                                    startAngle = 135f,
                                    sweepAngle = 270f * progressPercent,
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${(progressPercent * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "Sisa",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Oli: ${vehicle.oilBrand}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "Ganti Terakhir: ${vehicle.lastOilChangeOdometer} km (${formatDate(vehicle.lastOilChangeDate)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "Target Ganti: $oilTargetOdometer km",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (distanceRemaining <= 0) {
                                    "Segera Ganti Oli! Lewat ${-distanceRemaining} km"
                                } else {
                                    "Sisa $distanceRemaining km lagi atau dalam 4 hari."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onUpdateKmClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "BOOKING / PERBARUI ODOMETER SEKARANG",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (isUrgent) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onResetOilClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SAYA SUDAH GANTI OLI",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // DOCK SHORTCUTS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onUpdateKmClick,
                    modifier = Modifier
                        .weight(1.0f)
                        .testTag("update_miles_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Perbarui Odo")
                }

                Button(
                    onClick = onTestNotification,
                    modifier = Modifier
                        .weight(1.0f)
                        .testTag("test_notif_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.NotificationAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Tes Notif")
                }
            }
        }

        // SERVICE STATUS SUMMARIES
        item {
            val pendingSchedules = schedules.filter { !it.isCompleted }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Jadwal Servis Terdekat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (pendingSchedules.isEmpty()) {
                        Text(
                            text = "Tidak ada jadwal servis mendesak saat ini. Klik tab 'Jadwal' untuk membuat.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        pendingSchedules.take(3).forEach { schedule ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = schedule.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = formatDate(schedule.targetDate),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (schedule.targetOdometer > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "${schedule.targetOdometer} km",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB 2: JADWAL SERVIS
@Composable
fun OtoSchedulesTab(
    schedules: List<MaintenanceSchedule>,
    onAddScheduleClick: () -> Unit,
    onCompleteClick: (MaintenanceSchedule) -> Unit,
    onDeleteClick: (MaintenanceSchedule) -> Unit
) {
    val completedSchedules = schedules.filter { it.isCompleted }
    val pendingSchedules = schedules.filter { !it.isCompleted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rencana Jadwal Servis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(
                onClick = onAddScheduleClick,
                modifier = Modifier.testTag("add_schedule_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tambah")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pendingSchedules.isEmpty() && completedSchedules.isEmpty()) {
            OtoEmptyState(
                title = "Belum Ada Jadwal",
                description = "Buat pengingat service rutin (misal ganti kampas rem, servis CVT/mesin bulanan) baru untuk memulainya.",
                buttonText = "Tambah Jadwal Servis",
                onClickButton = onAddScheduleClick
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pendingSchedules.isNotEmpty()) {
                    item {
                        Text(
                            text = "Mendatang (${pendingSchedules.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(pendingSchedules) { schedule ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("schedule_item_${schedule.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1.0f)) {
                                        Text(
                                            text = schedule.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Event,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = formatDate(schedule.targetDate),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (schedule.targetOdometer > 0) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Speed,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Target Mileage: ${schedule.targetOdometer} km",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    IconButton(onClick = { onDeleteClick(schedule) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                if (schedule.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = schedule.notes,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { onCompleteClick(schedule) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("complete_schedule_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Selesaikan & Catat Riwayat")
                                }
                            }
                        }
                    }
                }

                if (completedSchedules.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Selesai (${completedSchedules.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(completedSchedules) { schedule ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = schedule.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "Target: ${formatDate(schedule.targetDate)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selesai",
                                    tint = Color(0xFF43A047),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB 3: RIWAYAT PERBAIKAN
@Composable
fun OtoHistoryLogsTab(
    logs: List<MaintenanceLog>,
    onAddLogClick: () -> Unit,
    onDeleteLogClick: (MaintenanceLog) -> Unit
) {
    var searchCategory by remember { mutableStateOf("Semua") }
    val filteredLogs = if (searchCategory == "Semua") logs else logs.filter { it.category == searchCategory }
    val totalCost = filteredLogs.sumOf { it.cost }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Riwayat & Pengeluaran",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(
                onClick = onAddLogClick,
                modifier = Modifier.testTag("add_log_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tambah Log")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Total Cost Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Biaya ($searchCategory)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = formatRupiah(totalCost),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Filter Chips
        val categories = listOf("Semua", "Oli", "Servis", "Perbaikan", "Ban", "Lainnya")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.take(3).forEach { cat ->
                InputChip(
                    selected = searchCategory == cat,
                    onClick = { searchCategory = cat },
                    label = { Text(cat) },
                    modifier = Modifier.weight(1.0f)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.drop(3).forEach { cat ->
                InputChip(
                    selected = searchCategory == cat,
                    onClick = { searchCategory = cat },
                    label = { Text(cat) },
                    modifier = Modifier.weight(1.0f)
                )
            }
        }

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada riwayat perawatan untuk kategori ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLogs) { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("log_item_${log.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        when (log.category) {
                                            "Oli" -> Color(0xFFFFEBEE)
                                            "Servis" -> Color(0xFFE8F5E9)
                                            "Perbaikan" -> Color(0xFFE3F2FD)
                                            else -> Color(0xFFF3E5F5)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (log.category) {
                                        "Oli" -> Icons.Default.WaterDrop
                                        "Servis" -> Icons.Default.Build
                                        "Perbaikan" -> Icons.Default.Settings
                                        "Ban" -> Icons.Default.Circle
                                        else -> Icons.Default.Pin
                                    },
                                    contentDescription = null,
                                    tint = when (log.category) {
                                        "Oli" -> Color(0xFFC62828)
                                        "Servis" -> Color(0xFF2E7D32)
                                        "Perbaikan" -> Color(0xFF1565C0)
                                        else -> Color(0xFF6A1B9A)
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1.0f)) {
                                Text(
                                    text = log.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatDate(log.date)} • ${log.odometer} km",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (log.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = log.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatRupiah(log.cost),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { onDeleteLogClick(log) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB 4: BEAUTIFUL STATISTICS (DASBOR BIAYA OPERASIONAL)
@Composable
fun OtoBeautyStatsTab(
    monthlyStats: Map<String, Double>,
    categoryStats: Map<String, Double>,
    logs: List<MaintenanceLog>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Statistik Biaya Operasional",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // MONTHLY EXPENSE HISTOGRAM
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Grafik Pengeluaran Bulanan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (monthlyStats.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada transaksi di riwayat.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val maxExpense = monthlyStats.values.maxOrNull() ?: 1.0
                        // Vertical bar layout for last months sorted
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            monthlyStats.forEach { (month, totalCost) ->
                                val barFraction = (totalCost / maxExpense).toFloat()
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = month, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = formatRupiah(totalCost),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OtoFixedProgressIndicator(
                                        progress = barFraction,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // CATEGORY BREAKDOWN
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pengeluaran Berdasarkan Kategori",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (categoryStats.isEmpty()) {
                        Text(
                            text = "Belum ada pengeluaran per kategori.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        val total = categoryStats.values.sum()
                        categoryStats.forEach { (cat, cost) ->
                            val pct = if (total > 0) (cost / total).toFloat() else 0f
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (cat) {
                                                    "Oli" -> Color(0xFFC62828)
                                                    "Servis" -> Color(0xFF2E7D32)
                                                    "Perbaikan" -> Color(0xFF1565C0)
                                                    else -> Color(0xFF6A1B9A)
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$cat (${(pct * 100).toInt()}%)",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Text(
                                    text = formatRupiah(cost),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                color = when (cat) {
                                    "Oli" -> Color(0xFFC62828)
                                    "Servis" -> Color(0xFF2E7D32)
                                    "Perbaikan" -> Color(0xFF1565C0)
                                    else -> Color(0xFF6A1B9A)
                                }
                            )
                        }
                    }
                }
            }
        }

        // ACTIONABLE TIPS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFFBC02D),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Automotive Tips!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Melakukan ganti oli mesin secara rutin setiap 5.000 km dapat mencegah gesekan komponen mesin dan mempertahankan performa bahan bakar agar menghemat pengeluaran Anda hingga 8%.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

// ====================== DIALOG RENDERING ENGINE ======================

@Composable
fun OtoAddVehicleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int, Int, Long, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Mobil") } // "Mobil" or "Motor"
    var currentOdo by remember { mutableStateOf("") }
    var lastOilOdo by remember { mutableStateOf("") }
    var interval by remember { mutableStateOf("5000") }
    var oilBrand by remember { mutableStateOf("Shell Helix") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tambah Kendaraan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kendaraan (Contoh: Avanza)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_vehicle_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it },
                    label = { Text("Plat Nomor (Contoh: B 1234 ABC)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_vehicle_plate_input"),
                    singleLine = true
                )

                Text(text = "Jenis Kendaraan:", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElevatedButton(
                        onClick = { type = "Mobil" },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (type == "Mobil") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mobil")
                    }
                    ElevatedButton(
                        onClick = { type = "Motor" },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (type == "Motor") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.TwoWheeler, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Motor")
                    }
                }

                OutlinedTextField(
                    value = currentOdo,
                    onValueChange = { currentOdo = it },
                    label = { Text("Odometer Saat Ini (KM)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("add_vehicle_odo_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = lastOilOdo,
                    onValueChange = { lastOilOdo = it },
                    label = { Text("Odometer Terakhir Ganti Oli (KM)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("add_vehicle_oil_odo_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = oilBrand,
                    onValueChange = { oilBrand = it },
                    label = { Text("Merk Oli Yang Digunakan") },
                    modifier = Modifier.fillMaxWidth().testTag("add_vehicle_oil_brand_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = interval,
                    onValueChange = { interval = it },
                    label = { Text("Interval Ganti Oli (Default 5000 KM)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("add_vehicle_interval_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotEmpty() && currentOdo.isNotEmpty()) {
                                val odoVal = currentOdo.toIntOrNull() ?: 0
                                val lastOilVal = lastOilOdo.toIntOrNull() ?: odoVal
                                val intervalVal = interval.toIntOrNull() ?: 5000
                                onConfirm(
                                    name,
                                    plate,
                                    type,
                                    odoVal,
                                    lastOilVal,
                                    System.currentTimeMillis() - (86400000L * 30), // Default 1 month ago
                                    intervalVal,
                                    oilBrand.ifEmpty { "Standar" }
                                )
                            }
                        },
                        modifier = Modifier.testTag("add_vehicle_confirm_button")
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun OtoUpdateOdometerDialog(
    currentOdo: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var odo by remember { mutableStateOf(currentOdo.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Update Odometer Kendaraan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = odo,
                    onValueChange = { odo = it },
                    label = { Text("Odometer Baru (KM)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_odo_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val targetOdo = odo.toIntOrNull()
                            if (targetOdo != null && targetOdo >= currentOdo) {
                                onConfirm(targetOdo)
                            }
                        },
                        modifier = Modifier.testTag("dialog_odo_confirm")
                    ) {
                        Text("Perbarui")
                    }
                }
            }
        }
    }
}

@Composable
fun OtoAddScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var targetOdo by remember { mutableStateOf("") }
    var daysAhead by remember { mutableStateOf("7") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tambah Jadwal Servis Rutin",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Servis (Contoh: Ganti Kampas Rem)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_schedule_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = daysAhead,
                    onValueChange = { daysAhead = it },
                    label = { Text("Berapa Hari Lagi Dari Sekarang?") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("add_schedule_days_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = targetOdo,
                    onValueChange = { targetOdo = it },
                    label = { Text("Target KM Servis (Opsional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("add_schedule_odo_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan Tambahan (Spesifikasi oli/part)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_schedule_notes_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotEmpty()) {
                                val dAhead = daysAhead.toLongOrNull() ?: 7
                                val timeMillis = System.currentTimeMillis() + (dAhead * 86400000L)
                                val odoVal = targetOdo.toIntOrNull() ?: 0
                                onConfirm(title, timeMillis, odoVal, note)
                            }
                        },
                        modifier = Modifier.testTag("add_schedule_confirm")
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun OtoAddLogDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, Double, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var odo by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Servis") } // "Servis", "Oli", "Perbaikan", "Lainnya"

    val categories = listOf("Servis", "Oli", "Perbaikan", "Lainnya")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Catat Riwayat Pengeluaran",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Deskripsi Perbaikan (Contoh: Ganti Ban IRC)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_log_title"),
                        singleLine = true
                    )
                }

                item {
                    Text(text = "Kategori:", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.forEach { cat ->
                            InputChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = cost,
                        onValueChange = { cost = it },
                        label = { Text("Biaya / Pengeluaran (Rupiah)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("add_log_cost"),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = odo,
                        onValueChange = { odo = it },
                        label = { Text("Odometer Terkini (KM)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("add_log_odo"),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan / Merek Komponen (Opsional)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_log_notes")
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (title.isNotEmpty() && cost.isNotEmpty() && odo.isNotEmpty()) {
                                    onConfirm(
                                        title,
                                        category,
                                        System.currentTimeMillis(),
                                        cost.toDoubleOrNull() ?: 0.0,
                                        odo.toIntOrNull() ?: 0,
                                        notes
                                    )
                                }
                            },
                            modifier = Modifier.testTag("add_log_confirm")
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OtoCompleteScheduleDialog(
    schedule: MaintenanceSchedule,
    onDismiss: () -> Unit,
    onConfirm: (Int, Double, String) -> Unit
) {
    var odo by remember { mutableStateOf(schedule.targetOdometer.takeIf { it > 0 }?.toString() ?: "") }
    var cost by remember { mutableStateOf("") }
    var completionNote by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Selesaikan Jadwal Servis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Menyelesaikan '${schedule.title}' akan otomatis mencatat biaya dan meng-update odometer kendaraan Anda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = odo,
                    onValueChange = { odo = it },
                    label = { Text("Odometer Aktual Penyelesaian (KM)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("complete_odo"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = { Text("Biaya Aktual Penyelesaian (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("complete_cost"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = completionNote,
                    onValueChange = { completionNote = it },
                    label = { Text("Catatan Hasil Servis (Opsional)") },
                    modifier = Modifier.fillMaxWidth().testTag("complete_notes")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (odo.isNotEmpty() && cost.isNotEmpty()) {
                                onConfirm(
                                    odo.toIntOrNull() ?: 0,
                                    cost.toDoubleOrNull() ?: 0.0,
                                    completionNote
                                )
                            }
                        },
                        modifier = Modifier.testTag("complete_confirm")
                    ) {
                        Text("Simpan & Selesai")
                    }
                }
            }
        }
    }
}

// ============================================
// COMPONENT: FAQ & APP GUIDE OVERLAY
// ============================================

@Composable
fun OtoFaqAndGuideDialog(
    onDismiss: () -> Unit
) {
    var faqTab by remember { mutableStateOf(0) } // 0 = Panduan Pintar, 1 = Tanya Jawab

    Dialog(
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Group
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Help,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Panduan & FAQ",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Petunjuk lengkap penggunaan aplikasi OtoCare",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Segmented Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val activeColor = MaterialTheme.colorScheme.primary
                    val activeTextColor = MaterialTheme.colorScheme.onPrimary
                    val inactiveColor = Color.Transparent
                    val inactiveTextColor = MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (faqTab == 0) activeColor else inactiveColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { faqTab = 0 }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = if (faqTab == 0) activeTextColor else inactiveTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Panduan Pintar",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (faqTab == 0) activeTextColor else inactiveTextColor
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (faqTab == 1) activeColor else inactiveColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { faqTab = 1 }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Help,
                                contentDescription = null,
                                tint = if (faqTab == 1) activeTextColor else inactiveTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tanya Jawab",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (faqTab == 1) activeTextColor else inactiveTextColor
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Content
                Box(modifier = Modifier.weight(1f)) {
                    if (faqTab == 0) {
                        OtoGuideContent()
                    } else {
                        OtoFaqContent()
                    }
                }
            }
        }
    }
}

@Composable
fun OtoGuideContent() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Selamat Datang di OtoCare!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Aplikasi asisten cerdas untuk memantau kesehatan roda kendaraan Anda secara berkala lewat kalkulasi taksiran masa pakai oli (odometer) & pengingat servis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Langkah Pengoperasian Aplikasi:",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        val steps = listOf(
            GuideStepItem(
                number = "1",
                title = "Daftarkan Kendaraan Anda",
                description = "Masukkan nama kendaraan, plat nomor, status odometer saat ini, tanggal pengerjaan ganti oli mesin terakhir, posisi kilometer ganti oli terakhir, serta target interval penggantian olinya. Aplikasi akan menghitung sisa masa pakai oli Anda.",
                icon = Icons.Default.AddCircle,
                badgeColor = Color(0xFF1E88E5)
            ),
            GuideStepItem(
                number = "2",
                title = "Perbarui Odometer Secara Rutin",
                description = "Agar perkiraan masa pakai oli tetap presisi, biasakan memperbarui angka KM/jarak tempuh kendaraan secara berkala lewat tombol 'BOOKING / PERBARUI ODOMETER' yang ada di tengah halaman Beranda/Dasbor.",
                icon = Icons.Default.Speed,
                badgeColor = Color(0xFF43A047)
            ),
            GuideStepItem(
                number = "3",
                title = "Kelola & Setel Jadwal Perawatan",
                description = "Gunakan tab kedua 'Jadwal' untuk mendaftarkan janji servis berkala berikutnya, penggantian filter udara/busi/aki, atau pembayaran STNK. Pengingat status bar akan muncul melindung Anda secara otomatis.",
                icon = Icons.Default.DateRange,
                badgeColor = Color(0xFFE65100)
            ),
            GuideStepItem(
                number = "4",
                title = "Selesaikan Jadwal untuk Simpan Histori",
                description = "Setelah Anda melakukan servis berkala atau ganti oli di dunia nyata, ketuk tombol Centang/Selesai pada jadwal tersebut. Aplikasi akan otomatis merekam biaya riwayat perawatan dan memperbarui odometer oli terakhir Anda.",
                icon = Icons.Default.CheckCircle,
                badgeColor = Color(0xFF8E24AA)
            ),
            GuideStepItem(
                number = "5",
                title = "Pantau Keuangan di Tab Statistik",
                description = "Semua histori biaya yang tercatat akan langsung terklasifikasi secara cantik dalam grafik biaya mingguan/bulanan. Gunakan infografis ini untuk mengontrol biaya pemeliharaan roda Anda.",
                icon = Icons.Default.BarChart,
                badgeColor = Color(0xFF00ACC1)
            )
        )

        items(steps) { step ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(step.badgeColor, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = step.number,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = step.badgeColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = step.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OtoFaqContent() {
    val faqs = remember {
        listOf(
            FaqItemData(
                question = "Bagaimana persentase kondisi oli dihitung?",
                answer = "Kondisi oli dihitung secara real-time berdasarkan jarak berjalan kendaraan Anda sejak penggantian oli terakhir.\n\nRumus:\nPersentase Oli = ((Odometer Ganti Terakhir + Interval Rekomendasi) - Odometer Sekarang) / Interval Rekomendasi * 100%\n\nApabila kilometer sisa habis atau mencapai minus, persentase menjadi 0% dan memicu notifikasi darurat."
            ),
            FaqItemData(
                question = "Kapan saya harus memperbarui odometer/KM?",
                answer = "Idealnya perbarui setiap kali Anda selesai menempuh perjalanan jauh, atau secara mingguan (tiap akhir pekan).\n\nPembaruan teratur menjamin kalkulasi masa kedaluwarsa oli mesin berjalan presisi sehingga mesin kendaraan tidak telanjur aus akibat oli kotor."
            ),
            FaqItemData(
                question = "Bagaimana menguji/test notifikasi status bar?",
                answer = "Agar notifikasi bekerja, pastikan Anda memberikan izin pemberitahuan saat aplikasi meminta di awal peluncuran.\n\nAnda dapat menguji/test tampilan notifikasi kapan saja menggunakan tombol lonceng yang ada di pojok kanan atas layar utama."
            ),
            FaqItemData(
                question = "Bagaimana cara menghapus data kendaraan atau riwayat?",
                answer = "- Menghapus Kendaraan: Ketuk tombol pemilih kendaraan (bentuk mobil) di pojok kanan atas, lalu ketuk ikon tempat sampah merah pada kendaraan yang mau didelete.\n- Menghapus Jadwal: Di tab 'Jadwal', ketuk ikon tong sampah merah pada kartu jadwal.\n- Menghapus Riwayat: Di tab 'Riwayat', ketuk ikon tong sampah merah pada baris catatan servis terkait."
            ),
            FaqItemData(
                question = "Apakah data saya aman dan bersifat offline?",
                answer = "Ya, 100% Aman! Semua database dicatat secara luring (offline) di internal smartphone Anda menggunakan Room Database SQLite Android.\n\nAplikasi OtoCare menghargai privasi Anda sepenuhnya dan tidak memiliki koneksi server backend luar untuk mengunggah aktivitas kendaraan Anda."
            )
        )
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Pertanyaan Sering Diajukan (FAQ):",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            )
        }

        items(faqs) { faq ->
            var isExpanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                colors = CardDefaults.cardColors(
                    containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = faq.question,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Sembunyikan" else "Tampilkan",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = faq.answer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

data class GuideStepItem(
    val number: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val badgeColor: Color
)

data class FaqItemData(
    val question: String,
    val answer: String
)

// ============================================
// COMPONENT: RESET OIL CHANGE DIALOG
// ============================================

@Composable
fun OtoResetOilDialog(
    vehicle: Vehicle,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, Double, String) -> Unit
) {
    var odo by remember { mutableStateOf(vehicle.currentOdometer.toString()) }
    var oilBrand by remember { mutableStateOf(vehicle.oilBrand) }
    var cost by remember { mutableStateOf("150000") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = "Konfirmasi Ganti Oli Baru",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Layanan ini akan menyetel ulang odometer ganti oli terakhir dan menyimpan riwayat servis ganti oli secara otomatis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = odo,
                    onValueChange = { odo = it },
                    label = { Text("Odometer Penggantian (KM)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_reset_oil_odo"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = oilBrand,
                    onValueChange = { oilBrand = it },
                    label = { Text("Merek Oli Baru") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_reset_oil_brand"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = { Text("Biaya Penggantian (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_reset_oil_cost"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Tambahan (Opsional)") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_reset_oil_notes"),
                    maxLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val targetOdo = odo.toIntOrNull() ?: vehicle.currentOdometer
                            val finalCost = cost.toDoubleOrNull() ?: 0.0
                            onConfirm(targetOdo, oilBrand, finalCost, notes)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("dialog_reset_oil_confirm")
                    ) {
                        Text("Simpan & Reset")
                    }
                }
            }
        }
    }
}

// ============================================
// COMPONENT: VEHICLE ATTRIBUTES EDIT DIALOG
// ============================================

@Composable
fun OtoEditVehicleDialog(
    vehicle: Vehicle,
    onDismiss: () -> Unit,
    onConfirm: (Vehicle) -> Unit
) {
    var name by remember { mutableStateOf(vehicle.name) }
    var plate by remember { mutableStateOf(vehicle.plateNumber) }
    var type by remember { mutableStateOf(vehicle.type) } // "Mobil" or "Motor"
    var oilBrand by remember { mutableStateOf(vehicle.oilBrand) }
    var rawInterval by remember { mutableStateOf(vehicle.oilChangeIntervalKm.toString()) }

    val presets = listOf(2000, 3000, 5000, 8000, 10000)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = "Atur Atribut Kendaraan",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Perbarui detail konfigurasi dan batas kilometer (rentang ganti) oli untuk kendaraan ${vehicle.name}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kendaraan") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_vehicle_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it },
                    label = { Text("Plat Nomor") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_vehicle_plate_input"),
                    singleLine = true
                )

                Text(
                    text = "Jenis Kendaraan",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElevatedButton(
                        onClick = { type = "Mobil" },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (type == "Mobil") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (type == "Mobil") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mobil", fontSize = 12.sp)
                    }
                    ElevatedButton(
                        onClick = { type = "Motor" },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (type == "Motor") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (type == "Motor") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Motor", fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = oilBrand,
                    onValueChange = { oilBrand = it },
                    label = { Text("Merek Oli Standar") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_vehicle_oil_brand_input"),
                    singleLine = true
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Rentang KM Penggantian Oli",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Tentukan estimasi interval penggantian oli berkala:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = rawInterval,
                        onValueChange = { rawInterval = it },
                        label = { Text("Interval (KM)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("edit_vehicle_interval_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Preset Cepat (KM):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Recommendation / Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        presets.forEach { kmValue ->
                            val isSelected = rawInterval == kmValue.toString()
                            SuggestionChip(
                                onClick = { rawInterval = kmValue.toString() },
                                label = { Text("${kmValue / 1000}k", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                    labelColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val intervalKm = rawInterval.toIntOrNull() ?: vehicle.oilChangeIntervalKm
                            val updated = vehicle.copy(
                                name = name,
                                plateNumber = plate,
                                type = type,
                                oilBrand = oilBrand,
                                oilChangeIntervalKm = intervalKm
                            )
                            onConfirm(updated)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("edit_vehicle_save_button")
                    ) {
                        Text("Simpan Perubahan")
                    }
                }
            }
        }
    }
}
