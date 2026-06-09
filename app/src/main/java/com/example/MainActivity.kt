package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.VehicleRepository
import com.example.ui.screens.OtoMainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.OtoViewModel
import com.example.ui.viewmodel.OtoViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = AppDatabase.getDatabase(applicationContext)
    val repository = VehicleRepository(database.vehicleDao())
    val factory = OtoViewModelFactory(application, repository)
    val viewModel = ViewModelProvider(this, factory)[OtoViewModel::class.java]

    setContent {
      MyApplicationTheme {
        OtoMainScreen(viewModel = viewModel)
      }
    }
  }
}
