package com.example.app

import android.app.Application

class SampleApplication : Application() {
    internal val dependencies by lazy { AppDependencies(this) }
}
