package com.funkyradish.funky_radish

import android.app.Application
import android.util.Log
import io.realm.*

class RealmInit : Application() {

    override fun onCreate() {

        super.onCreate()
        RealmService().initialize(this)
    }

}