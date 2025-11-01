package com.example.miniperplexity

import android.app.Application
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase once when the application starts
        FirebaseApp.initializeApp(this)
    }
}

