package com.alos895.simplepos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.alos895.simplepos.SimplePosApplication

@Composable
fun simplePosViewModelFactory(): ViewModelProvider.Factory {
    val context = LocalContext.current.applicationContext
    val application = context as SimplePosApplication
    return remember(application) { application.viewModelFactory }
}
