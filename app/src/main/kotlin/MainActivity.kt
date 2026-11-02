package com.achtien.codingtemplate

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.achtien.codingtemplate.map.EarthquakeMapScreen
import com.achtien.codingtemplate.ui.theme.CodingTemplateTheme
import com.achtien.repository.location.UserLocationRepository
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val userLocation: UserLocationRepository by inject()
    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { userLocation.refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            requestLocationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        setContent {
            CodingTemplateTheme {
                EarthquakeMapScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        userLocation.refresh()
    }
}
