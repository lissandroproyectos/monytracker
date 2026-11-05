package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class FinanzasApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ensureFirebase(this)
    }

    companion object {
        private const val TAG = "FinanzasApplication"

        fun ensureFirebase(context: Context): FirebaseApp? {
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    return FirebaseApp.getInstance()
                }

                // 1. Try default auto-initialization from resources
                val defaultApp = FirebaseApp.initializeApp(context)
                if (defaultApp != null) {
                    Log.i(TAG, "Firebase initialized via default resource configuration: ${defaultApp.name}")
                    return defaultApp
                }
            } catch (e: Exception) {
                Log.w(TAG, "Default FirebaseApp.initializeApp threw exception: ${e.message}")
            }

            // 2. Explicit fallback with FirebaseOptions from google-services.json
            return try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:166010243644:android:3a6b97d46cc88ec4a15139")
                        .setApiKey("AIzaSyAn5JBiam28MdxM5B9b6vK-D4vzu6Gs0HU")
                        .setProjectId("caribbou-net-storage-manager")
                        .setStorageBucket("caribbou-net-storage-manager.firebasestorage.app")
                        .setGcmSenderId("166010243644")
                        .build()
                    val app = FirebaseApp.initializeApp(context, options)
                    Log.i(TAG, "Firebase initialized successfully with explicit FirebaseOptions fallback: ${app.name}")
                    app
                } else {
                    FirebaseApp.getInstance()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Explicit FirebaseApp initialization fallback failed", e)
                null
            }
        }
    }
}

