package com.nexflash.app

import android.app.Application
import com.google.android.material.color.DynamicColors

class NexFlashApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
