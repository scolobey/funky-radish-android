package com.funkyradish.funky_radish

import android.content.Context
import android.util.Log
import io.realm.Realm
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration

lateinit var realmApp: App

//TODO: Hide in private variable to only access through functions.
lateinit var realm: Realm

class RealmService {

    fun initialize(context: Context) {
        Realm.init(context)
        realmApp = App(AppConfiguration.Builder(Constants.REALM_APP_ID).build())

        Log.d("API", "initializing realm")
    }

    fun logout(callback: (success: Boolean) -> Unit) {
        Log.d("API", "logging out")

        realmApp.currentUser()?.logOutAsync {
            if (it.isSuccess) {
                callback(true)
            } else {
                callback(false)
            }
        }
    }

}


