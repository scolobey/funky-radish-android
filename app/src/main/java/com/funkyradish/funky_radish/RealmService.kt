package com.funkyradish.funky_radish

import android.content.Context
import android.util.Log
import io.realm.Realm
import io.realm.RealmConfiguration
//import io.realm.SyncUser

class RealmService {

    fun initialize(context: Context) {
        Realm.init(context)

        Log.d("API", "initializing realm")
//        if (SyncUser.current() != null) {
//            configureSynchedRealm()
//        }
//        else {
//            configureDefaultRealm()
//        }
    }

    fun configureSynchedRealm() {
        Log.d("API", "configuring synched realm")
//        val currentUser = SyncUser.current()
//        val url = Constants.REALM_URL
//        val synchConfiguration = currentUser.createConfiguration(url)
//                .fullSynchronization()
//                .schemaVersion(1)
//                .build()
//
//        Realm.setDefaultConfiguration(synchConfiguration)
    }

    fun configureDefaultRealm() {
        Log.d("API", "Configuring default Realm")

        val realmConfiguration = RealmConfiguration.Builder()
                .name(Constants.REALM_DB_NAME)
                .schemaVersion(1)
                .migration(Migration())
                .build()

        Realm.setDefaultConfiguration(realmConfiguration)
    }

    fun logout() {

        //TODO: What if there are several users logged in?
        Log.d("API", "logging out")

//        var realm = Realm.getDefaultInstance()
//        SyncUser.current().logOut()
//        realm.close()
//
//        val realmConfiguration = RealmConfiguration.Builder()
//                .name(Constants.REALM_DB_NAME)
//                .schemaVersion(1)
//                .migration(Migration())
//                .build()
//
//        Realm.setDefaultConfiguration(realmConfiguration)
    }

//    fun getRecipes() {
//        realm = Realm.getDefaultInstance()
//        var recipes = realm.where(Recipe::class.java).findAll()
//        realm.close()
//    }

}


