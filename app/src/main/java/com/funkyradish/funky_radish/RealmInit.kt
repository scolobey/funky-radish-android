package com.funkyradish.funky_radish

import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration


class RealmInit : Application() {

    override fun onCreate() {
        super.onCreate()

        //Config Realm for the application
        Realm.init(this)
        val realmConfiguration = RealmConfiguration.Builder()
                .name(REALM_DB_NAME)
                .build()

        Realm.setDefaultConfiguration(realmConfiguration)
    }

    companion object {
        private val REALM_DB_NAME = "fr_realm_db"
    }
}