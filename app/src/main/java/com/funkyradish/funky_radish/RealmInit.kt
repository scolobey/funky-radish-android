package com.funkyradish.funky_radish

import android.app.Application
import android.util.Log

class RealmInit : Application() {

    override fun onCreate() {
        Log.d("API", "calling realm init")
        super.onCreate()
        RealmService().initialize(this)
    }

}