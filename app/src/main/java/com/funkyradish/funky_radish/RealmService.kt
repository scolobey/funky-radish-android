package com.funkyradish.funky_radish

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.volley.RequestQueue
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.Credentials

//import io.realm.SyncUser

lateinit var realmApp: App

class RealmService {

    fun initialize(context: Context) {
        Realm.init(context)
        realmApp = App(AppConfiguration.Builder(Constants.REALM_APP_ID).build())

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

//    fun configureDefaultRealm() {
//        Log.d("API", "Configuring default Realm")
//
//        val realmConfiguration = RealmConfiguration.Builder()
//                .name(Constants.REALM_DB_NAME)
//                .schemaVersion(1)
//                .migration(Migration())
//                .build()
//
//        Realm.setDefaultConfiguration(realmConfiguration)
//    }

    fun register(activity: Activity, queue: RequestQueue, email: String, password: String, importRecipes: List<Recipe?>, callback: (success: Boolean) -> Unit) {

        Log.d("API", "here we are registering.")

//        val customJWTCredentials: Credentials = Credentials.jwt("<token>")

//        realmApp.emailPasswordAuth.registerUserAsync(email, password) {
//            if (it.isSuccess) {
//                Log.v("AUTH", "Successfully authenticated using a custom JWT.")
//                // Set the user.
//            } else {
//                Log.e("AUTH", "Error logging in: ${it.error.toString()}")
//            }
//        }

//
//        realmApp.emailPasswordAuth.registerUserAsync(username, password) {
//            // re-enable the buttons after user registration returns a result
//            createUserButton.isEnabled = true
//            loginButton.isEnabled = true
//            if (!it.isSuccess) {
//                onLoginFailed("Could not register user.")
//                Log.e(TAG(), "Error: ${it.error}")
//            } else {
//                Log.i(TAG(), "Successfully registered user.")
//                // when the account has been created successfully, log in to the account
//                login(false)
//            }
//        }
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


