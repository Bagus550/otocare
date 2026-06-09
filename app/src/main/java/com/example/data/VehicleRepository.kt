package com.example.data

import kotlinx.coroutines.flow.Flow

class VehicleRepository(private val vehicleDao: VehicleDao) {
    val allVehicles: Flow<List<Vehicle>> = vehicleDao.getAllVehicles()
    val allSchedules: Flow<List<MaintenanceSchedule>> = vehicleDao.getAllSchedules()
    val allLogs: Flow<List<MaintenanceLog>> = vehicleDao.getAllLogs()

    fun getVehicleById(id: Int): Flow<Vehicle?> = vehicleDao.getVehicleById(id)

    fun getSchedulesForVehicle(vehicleId: Int): Flow<List<MaintenanceSchedule>> =
        vehicleDao.getSchedulesForVehicle(vehicleId)

    fun getLogsForVehicle(vehicleId: Int): Flow<List<MaintenanceLog>> =
        vehicleDao.getLogsForVehicle(vehicleId)

    suspend fun insertVehicle(vehicle: Vehicle): Long = vehicleDao.insertVehicle(vehicle)

    suspend fun updateVehicle(vehicle: Vehicle) = vehicleDao.updateVehicle(vehicle)

    suspend fun deleteVehicle(vehicle: Vehicle) = vehicleDao.deleteVehicle(vehicle)

    suspend fun insertSchedule(schedule: MaintenanceSchedule): Long =
        vehicleDao.insertSchedule(schedule)

    suspend fun updateSchedule(schedule: MaintenanceSchedule) =
        vehicleDao.updateSchedule(schedule)

    suspend fun deleteSchedule(schedule: MaintenanceSchedule) =
        vehicleDao.deleteSchedule(schedule)

    suspend fun insertLog(log: MaintenanceLog): Long = vehicleDao.insertLog(log)

    suspend fun updateLog(log: MaintenanceLog) = vehicleDao.updateLog(log)

    suspend fun deleteLog(log: MaintenanceLog) = vehicleDao.deleteLog(log)
}
