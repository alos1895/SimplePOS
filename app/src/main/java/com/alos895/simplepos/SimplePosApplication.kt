package com.alos895.simplepos

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import com.alos895.simplepos.di.DefaultSimplePosContainer
import com.alos895.simplepos.di.SimplePosContainer
import com.alos895.simplepos.di.SimplePosViewModelFactory

class SimplePosApplication : Application() {

    lateinit var container: SimplePosContainer
        private set

    lateinit var viewModelFactory: ViewModelProvider.Factory
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultSimplePosContainer(this)
        viewModelFactory = SimplePosViewModelFactory(container)
    }
}
