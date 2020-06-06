package com.funkyradish.funky_radish

import android.app.Application
import android.util.Log
import io.realm.*
import io.realm.SyncUser

class RealmInit : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.d("API", "Initializing Realm")

        Realm.init(this)

        if (SyncUser.current() != null) {
            Log.d("API", "There is a user")

            val currentUser = SyncUser.current()
            val url = Constants.REALM_URL
            val synchConfiguration = currentUser.createConfiguration(url)
                    .fullSynchronization()
                    .build()

            Realm.setDefaultConfiguration(synchConfiguration)
        }
        else {

            Log.d("API", "Configuring default Realm")

            val realmConfiguration = RealmConfiguration.Builder()
                    .name(Constants.REALM_DB_NAME)
                    .build()

            Realm.setDefaultConfiguration(realmConfiguration)
        }

    }



}