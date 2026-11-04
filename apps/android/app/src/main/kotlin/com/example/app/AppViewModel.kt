package com.example.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.corenavigation.Navigator

internal class AppViewModel(application: Application) : AndroidViewModel(application) {
    val navigator = Navigator()
    val dependencies = getApplication<SampleApplication>().dependencies
}
