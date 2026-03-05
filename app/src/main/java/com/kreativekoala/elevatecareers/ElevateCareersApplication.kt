package com.kreativekoala.elevatecareers

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class ElevateCareersApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isEmpty()) {
            val app = FirebaseApp.initializeApp(this)
            Log.d("FirebaseInit", "initialized=${app != null}")
            if (app == null) Log.e("FirebaseInit", "initializeApp returned null – check plugin & applicationId")
        }
    }
}