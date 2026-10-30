package com.kreativekoala.elevatecareers

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class ElevateCareersApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Crash + handled-failure reporting. Needs a reporter key for app "elevatecareers" (placeholder until issued;
        // reports are rejected harmlessly until then). Keys ship in apps: they are identifiers, not secrets.
        val versionName = try { packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown" } catch (_: Exception) { "unknown" }
        FailureReporter.init(this, "elevatecareers", REPORTER_KEY, versionName)
        if (FirebaseApp.getApps(this).isEmpty()) {
            val app = FirebaseApp.initializeApp(this)
            Log.d("FirebaseInit", "initialized=${app != null}")
            if (app == null) Log.e("FirebaseInit", "initializeApp returned null – check plugin & applicationId")
        }
    }

    companion object {
        private const val REPORTER_KEY = "elevatecareers"
    }
}
