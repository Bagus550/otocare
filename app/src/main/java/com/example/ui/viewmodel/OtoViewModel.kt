package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.notification.OtoNotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class OtoViewModel(
    private val app: Application,
    private val repository: VehicleRepository
) : AndroidViewModel(app) {

    init {
        viewModelScope.launch {
            repository.allVehicles.first().let { currentList ->
                if (currentList.isEmpty()) {
                    val vId = repository.insertVehicle(
                        Vehicle(
                            name = "Toyota Avanza",
                            plateNumber = "B 1234 SFI",
                            type = "Mobil",
                            currentOdometer = 42500,
                            lastOilChangeOdometer = 40000,
                            lastOilChangeDate = System.currentTimeMillis() - (86400000L * 25),
                            oilChangeIntervalKm = 5000,
                            oilBrand = "Shell Helix HX8"
                        )
                    ).toInt()

                    val vId2 = repository.insertVehicle(
                        Vehicle(
                            name = "Honda Vario 160",
                            plateNumber = "DK 5678 AA",
                            type = "Motor",
                            currentOdometer = 12200,
                            lastOilChangeOdometer = 11000,
                            lastOilChangeDate = System.currentTimeMillis() - (86400000L * 15),
                            oilChangeIntervalKm = 2000,
                            oilBrand = "AHM SPX2"
                        )
                    ).toInt()

                    repository.insertSchedule(
                        MaintenanceSchedule(
                            vehicleId = vId,
                            title = "Servis Rem Belakang",
                            targetDate = System.currentTimeMillis() + (86400000L * 3),
                            targetOdometer = 45000,
                            isCompleted = false,
                            notes = "Periksa bunyi berdecit"
                        )
                    )

                    repository.insertSchedule(
                        MaintenanceSchedule(
                            vehicleId = vId,
                            title = "Ganti Oli Transmisi Gardan",
                            targetDate = System.currentTimeMillis() + (86400000L * 14),
                            targetOdometer = 46000,
                            isCompleted = false,
                            notes = "Ganti oli transmisi matik / manual"
                        )
                    )

                    val oneDay = 86400000L
                    repository.insertLog(
                        MaintenanceLog(
                            vehicleId = vId,
                            title = "Ganti Oli Shell Helix HX8",
                            category = "Oli",
                            date = System.currentTimeMillis() - (oneDay * 25),
                            cost = 450000.0,
                            odometer = 40000,
                            notes = "Sudah diganti di bengkel resmi"
                        )
                    )

                    repository.insertLog(
                        MaintenanceLog(
                            vehicleId = vId,
                            title = "Servis Kampas Rem",
                            category = "Servis",
                            date = System.currentTimeMillis() - (oneDay * 12),
                            cost = 180000.0,
                            odometer = 41500,
                            notes = "Pembersihan cakram depan belakang"
                        )
                    )

                    repository.insertLog(
                        MaintenanceLog(
                            vehicleId = vId,
                            title = "Ganti Aki GS Astra",
                            category = "Perbaikan",
                            date = System.currentTimeMillis() - (oneDay * 42),
                            cost = 850000.0,
                            odometer = 38000,
                            notes = "Menggunakan aki kering orisinil"
                        )
                    )

                    _selectedVehicleId.value = vId
                } else {
                    _selectedVehicleId.value = currentList.firstOrNull()?.id
                }
            }
        }
    }

    // All data streams
    val vehicles: StateFlow<List<Vehicle>> = repository.allVehicles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSchedules: StateFlow<List<MaintenanceSchedule>> = repository.allSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<MaintenanceLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently selected vehicle state
    private val _selectedVehicleId = MutableStateFlow<Int?>(null)
    val selectedVehicleId: StateFlow<Int?> = _selectedVehicleId.asStateFlow()

    // Observable selected vehicle
    val selectedVehicle: StateFlow<Vehicle?> = combine(vehicles, _selectedVehicleId) { list, id ->
        if (id == null) list.firstOrNull() else list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Filtered schedules for selected vehicle
    val vehicleSchedules: StateFlow<List<MaintenanceSchedule>> = combine(allSchedules, _selectedVehicleId) { sList, vId ->
        val idToUse = vId ?: vehicles.value.firstOrNull()?.id ?: -1
        sList.filter { it.vehicleId == idToUse }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered history logs for selected vehicle
    val vehicleLogs: StateFlow<List<MaintenanceLog>> = combine(allLogs, _selectedVehicleId) { lList, vId ->
        val idToUse = vId ?: vehicles.value.firstOrNull()?.id ?: -1
        lList.filter { it.vehicleId == idToUse }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Set selected vehicle ID
    fun selectVehicle(id: Int) {
        _selectedVehicleId.value = id
    }

    // Insert new vehicle
    fun addVehicle(
        name: String,
        plateNumber: String,
        type: String,
        currentOdometer: Int,
        lastOilChangeOdometer: Int,
        lastOilChangeDate: Long,
        oilChangeIntervalKm: Int,
        oilBrand: String
    ) {
        viewModelScope.launch {
            val vehicle = Vehicle(
                name = name,
                plateNumber = plateNumber,
                type = type,
                currentOdometer = currentOdometer,
                lastOilChangeOdometer = lastOilChangeOdometer,
                lastOilChangeDate = lastOilChangeDate,
                oilChangeIntervalKm = oilChangeIntervalKm,
                oilBrand = oilBrand
            )
            val insertedId = repository.insertVehicle(vehicle)
            // Select the newly added vehicle if none selected
            if (_selectedVehicleId.value == null) {
                _selectedVehicleId.value = insertedId.toInt()
            }
        }
    }

    // Update vehicle mileage / current odometer
    fun updateVehicleMileage(vehicle: Vehicle, newOdo: Int) {
        viewModelScope.launch {
            val updated = vehicle.copy(currentOdometer = newOdo)
            repository.updateVehicle(updated)
        }
    }

    // Update generic vehicle attributes
    fun updateVehicle(updatedVehicle: Vehicle) {
        viewModelScope.launch {
            repository.updateVehicle(updatedVehicle)
        }
    }

    // Delete a vehicle
    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
            // If the deleted vehicle was the currently selected one, switch selection
            if (_selectedVehicleId.value == vehicle.id) {
                val remaining = vehicles.value.filter { it.id != vehicle.id }
                _selectedVehicleId.value = remaining.firstOrNull()?.id
            }
        }
    }

    // Log routine schedule / service rutin
    fun addSchedule(
        title: String,
        targetDate: Long,
        targetOdometer: Int,
        notes: String
    ) {
        val currentVehicle = selectedVehicle.value ?: return
        viewModelScope.launch {
            val schedule = MaintenanceSchedule(
                vehicleId = currentVehicle.id,
                title = title,
                targetDate = targetDate,
                targetOdometer = targetOdometer,
                isCompleted = false,
                notes = notes
            )
            val scheduleId = repository.insertSchedule(schedule).toInt()

            // Automatically schedule standard notification reminder at the targetDate
            // (e.g. 1 day before the targetDate, or if targetDate is near, at targetDate)
            // Notification is standardly set to trigger at targetDate or 10 seconds from now for immediate testing
            OtoNotificationHelper.scheduleNotification(
                context = app.applicationContext,
                scheduleId = scheduleId,
                vehicleName = currentVehicle.name,
                serviceTitle = title,
                triggerTimeMillis = targetDate
            )
        }
    }

    // Helper to complete a schedule (moves it to the logs/history and marks completed)
    fun completeSchedule(schedule: MaintenanceSchedule, completionOdometer: Int, cost: Double, notes: String) {
        val currentVehicle = selectedVehicle.value ?: return
        viewModelScope.launch {
            // Update schedule to completed
            repository.updateSchedule(schedule.copy(isCompleted = true))

            // Cancel any future alarms for this schedule
            OtoNotificationHelper.cancelNotification(app.applicationContext, schedule.id)

            // Add to maintenance log
            val log = MaintenanceLog(
                vehicleId = currentVehicle.id,
                title = schedule.title,
                category = if (schedule.title.contains("oli", ignoreCase = true)) "Oli" else "Servis",
                date = System.currentTimeMillis(),
                cost = cost,
                odometer = completionOdometer,
                notes = if (notes.isEmpty()) schedule.notes else notes
            )
            repository.insertLog(log)

            // If it was an oil change, update last oil change odometer
            if (schedule.title.contains("oli", ignoreCase = true)) {
                val updatedVehicle = currentVehicle.copy(
                    currentOdometer = maxOf(currentVehicle.currentOdometer, completionOdometer),
                    lastOilChangeOdometer = completionOdometer,
                    lastOilChangeDate = System.currentTimeMillis()
                )
                repository.updateVehicle(updatedVehicle)
            } else {
                // Otherwise just update vehicle current odometer if higher
                if (completionOdometer > currentVehicle.currentOdometer) {
                    val updatedVehicle = currentVehicle.copy(currentOdometer = completionOdometer)
                    repository.updateVehicle(updatedVehicle)
                }
            }
        }
    }

    // Complete manual log
    fun addMaintenanceLog(
        title: String,
        category: String,
        date: Long,
        cost: Double,
        odometer: Int,
        notes: String
    ) {
        val currentVehicle = selectedVehicle.value ?: return
        viewModelScope.launch {
            val log = MaintenanceLog(
                vehicleId = currentVehicle.id,
                title = title,
                category = category,
                date = date,
                cost = cost,
                odometer = odometer,
                notes = notes
            )
            repository.insertLog(log)

            // Save odometer as vehicle's current odometer if it's larger
            var updatedVehicle = currentVehicle
            if (odometer > currentVehicle.currentOdometer) {
                updatedVehicle = updatedVehicle.copy(currentOdometer = odometer)
            }

            // If category is Oil ("Oli"), update the oil parameters
            if (category == "Oli") {
                updatedVehicle = updatedVehicle.copy(
                    lastOilChangeOdometer = odometer,
                    lastOilChangeDate = date
                )
            }
            repository.updateVehicle(updatedVehicle)
        }
    }

    // Delete a schedule
    fun deleteSchedule(schedule: MaintenanceSchedule) {
        viewModelScope.launch {
            repository.deleteSchedule(schedule)
            OtoNotificationHelper.cancelNotification(app.applicationContext, schedule.id)
        }
    }

    // Delete a log
    fun deleteLog(log: MaintenanceLog) {
        viewModelScope.launch {
            repository.deleteLog(log)
        }
    }

    // Instant notification test
    fun testNotificationPreview(title: String) {
        val vehicle = selectedVehicle.value ?: return
        OtoNotificationHelper.triggerInstantNotification(
            context = app.applicationContext,
            vehicleName = vehicle.name,
            serviceTitle = title
        )
    }

    // STATS GENERATOR Flow
    // Groups cost logs by Month-Year (e.g. "Jan 2026", "Feb 2026")
    val monthlyStats: StateFlow<Map<String, Double>> = combine(allLogs, _selectedVehicleId) { logs, vId ->
        // Generate stats for ALL logs or selected vehicle logs
        // Let's filter by selected vehicle logs for precise tracking, or all to show consolidated summary!
        // We will show stats for current selected vehicle if present, otherwise all!
        val targetLogs = if (vId != null) logs.filter { it.vehicleId == vId } else logs
        val sdf = SimpleDateFormat("MMM yyyy", Locale("id", "ID"))
        
        targetLogs.groupBy {
            val date = Date(it.date)
            sdf.format(date)
        }.mapValues { entry ->
            entry.value.sumOf { it.cost }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val categoryStats: StateFlow<Map<String, Double>> = combine(allLogs, _selectedVehicleId) { logs, vId ->
        val targetLogs = if (vId != null) logs.filter { it.vehicleId == vId } else logs
        targetLogs.groupBy { it.category }.mapValues { entry ->
            entry.value.sumOf { it.cost }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
}

class OtoViewModelFactory(
    private val app: Application,
    private val repository: VehicleRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OtoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OtoViewModel(app, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
