package com.example.cronicasmarca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.cronicasmarca.ui.navigation.AppNavigation
import com.example.cronicasmarca.ui.theme.CronicasMarcaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CronicasMarcaTheme {
                AppNavigation()
            }
        }
    }
}
