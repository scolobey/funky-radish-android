package com.funkyradish.funky_radish

import android.app.Application

class RealmInit : Application() {

    override fun onCreate() {
        super.onCreate()
        RealmService().initialize(this)
    }

}