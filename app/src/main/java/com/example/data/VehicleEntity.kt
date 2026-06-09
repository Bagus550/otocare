package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val plateNumber: String,
    val type: String, // "Mobil" or "Motor"
    val currentOdometer: Int,
    val lastOilChangeOdometer: Int,
    val lastOilChangeDate: Long,
    val oilChangeIntervalKm: Int = 5000,
    val oilBrand: String
)

@Entity(tableName = "maintenance_schedules")
data class MaintenanceSchedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleId: Int,
    val title: String,
    val targetDate: Long, // Epoch millis
    val targetOdometer: Int = 0,
    val isCompleted: Boolean = false,
    val notes: String = ""
)

@Entity(tableName = "maintenance_logs")
data class MaintenanceLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleId: Int,
    val title: String,
    val category: String, // "Servis", "Oli", "Perbaikan", "Lainnya"
    val date: Long, // Epoch millis
    val cost: Double, // Rupiah
    val odometer: Int,
    val notes: String = ""
)
