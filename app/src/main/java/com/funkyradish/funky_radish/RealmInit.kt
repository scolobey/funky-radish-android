package com.funkyradish.funky_radish

import android.app.Application
import io.realm.*
import io.realm.SyncUser

class RealmInit : Application() {

    override fun onCreate() {
        super.onCreate()

        Realm.init(this)

//        var users = SyncUser.all()
//
//        if (users.size > 0) {
//            for (entry in users) {
//                entry.value.logOut()
//            }
//        }

        if (SyncUser.current() != null) {
            val user = SyncUser.current()
            val url = Constants.REALM_URL
            val synchConfiguration = user.createConfiguration(url)
                    .fullSynchronization()
                    .build()

            Realm.setDefaultConfiguration(synchConfiguration)
        }
        else {
            val realmConfiguration = RealmConfiguration.Builder()
                    .name(REALM_DB_NAME)
                    .build()

            Realm.setDefaultConfiguration(realmConfiguration)
        }

    }

    companion object {
        private val REALM_DB_NAME = "fr_realm_db"
    }


}