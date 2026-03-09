package com.swu.bianwanlu2_0

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.swu.bianwanlu2_0.presentation.navigation.AppNavHost
import com.swu.bianwanlu2_0.ui.theme.Bianwanlu2_0Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Bianwanlu2_0Theme(dynamicColor = false) {
                AppNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
