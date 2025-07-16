package com.alos895.simplepos.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.alos895.simplepos.ui.menu.MenuScreen
import com.alos895.simplepos.ui.theme.SimplePOSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimplePOSTheme {
                MenuScreen()
            }
        }
    }
} 