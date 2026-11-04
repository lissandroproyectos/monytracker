package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class FinanzasApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = FirebaseApp.initializeApp(this)
                if (app == null) {
                    Log.w("FinanzasApplication", "FirebaseApp.initializeApp returned null (google-services.json missing/invalid)")
                } else {
                    Log.i("FinanzasApplication", "Firebase initialized successfully: ${app.name}")
                }
            }
        } catch (e: Exception) {
            Log.e("FinanzasApplication", "Error initializing FirebaseApp on application start", e)
        }
    }
}
