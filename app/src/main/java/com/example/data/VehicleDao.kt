package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY name ASC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun getVehicleById(id: Int): Flow<Vehicle?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    // Maintenance Schedules
    @Query("SELECT * FROM maintenance_schedules WHERE vehicleId = :vehicleId ORDER BY targetDate ASC")
    fun getSchedulesForVehicle(vehicleId: Int): Flow<List<MaintenanceSchedule>>

    @Query("SELECT * FROM maintenance_schedules ORDER BY targetDate ASC")
    fun getAllSchedules(): Flow<List<MaintenanceSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: MaintenanceSchedule): Long

    @Update
    suspend fun updateSchedule(schedule: MaintenanceSchedule)

    @Delete
    suspend fun deleteSchedule(schedule: MaintenanceSchedule)

    // Maintenance Logs (History & cost records)
    @Query("SELECT * FROM maintenance_logs WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getLogsForVehicle(vehicleId: Int): Flow<List<MaintenanceLog>>

    @Query("SELECT * FROM maintenance_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<MaintenanceLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MaintenanceLog): Long

    @Update
    suspend fun updateLog(log: MaintenanceLog)

    @Delete
    suspend fun deleteLog(log: MaintenanceLog)
}
