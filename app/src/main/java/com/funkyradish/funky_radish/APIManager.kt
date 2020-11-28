package com.funkyradish.funky_radish

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import com.android.volley.*
import com.android.volley.toolbox.*
import com.google.gson.GsonBuilder
import io.realm.*
import org.json.JSONObject
import java.util.*
//import io.realm.SyncUser
import io.realm.kotlin.createObject
import io.realm.mongodb.Credentials
import io.realm.mongodb.sync.SyncConfiguration
import org.json.JSONException

fun getToken(context: Context): String {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    return preferences.getString(Constants.FR_TOKEN, "")
}

fun setToken(context: Context, token: String) {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val editor = preferences.edit()
    editor.putString(Constants.FR_TOKEN, token)
    editor.apply()
}

fun getUserEmail(context: Context): String {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    return preferences.getString(Constants.FR_USER_EMAIL, "")
}

fun setUserEmail(context: Context, userEmail: String) {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val editor = preferences.edit()
    editor.putString(Constants.FR_USER_EMAIL, userEmail)
    editor.apply()
}

fun setUsername(context: Context, username: String) {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val editor = preferences.edit()
    editor.putString(Constants.FR_USERNAME, username)
    editor.apply()
}

fun isOffline(context: Context): Boolean {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    return preferences.getBoolean(Constants.OFFLINE, false)
}

fun toggleOfflineMode(context: Context) {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val offline = preferences.getBoolean(Constants.OFFLINE, false)
    val editor = preferences.edit()
    editor.putBoolean(Constants.OFFLINE, !offline)
    editor.apply()
}

fun isConnected(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = connectivityManager.activeNetworkInfo
    return networkInfo != null && networkInfo.isConnected
}

fun register(activity: Activity, queue: RequestQueue, email: String, password: String, importRecipes: List<Recipe?>, callback: (success: Boolean) -> Unit) {

    // Structure user data
    val json = JSONObject().apply({
        put("email", email)
        put("password", password)
        put("admin", false)
    })

    // User request
    val userRequest = object : JsonObjectRequest(Method.POST, Constants.ENDPOINT, json,
            Response.Listener<JSONObject> { response ->

                val body = response.toString()
                val userResponse = GsonBuilder().create().fromJson(body, UserResponse::class.java)

                setToken(activity.applicationContext, userResponse.token)
//                setUsername(activity.applicationContext, userResponse.userData.email)
//                setUserEmail(activity.applicationContext, userResponse.userData.email)

                Log.d("API", "token: ${userResponse.token}")

                createRealmUser(userResponse.token, importRecipes, callback, activity)

            },
            Response.ErrorListener { error ->
                Log.d("API", "error on createUser: ${error.message}")
                error.printStackTrace()

                var errorMessage = error.toString()

                val response = error.networkResponse
                if (error is ServerError && response != null) {
                    try {
                        val json = String(response.data)
                        val userErrorResponse = GsonBuilder().create().fromJson(json, UserErrorResponse::class.java)
                        errorMessage = userErrorResponse.message
                    } catch (e2: JSONException) {
                        e2.printStackTrace()
                    }
                }

                Toast.makeText(
                        activity.applicationContext,
                        errorMessage,
                        Toast.LENGTH_SHORT).show()

                callback(false)

            }
    ) {
        override fun getHeaders(): Map<String, String> {
            val headers = HashMap<String, String>()
            headers.put("Content-Type", "application/json; charset=utf-8")
            return headers
        }
    }

    if(isConnected(activity.applicationContext)) {
        queue.add(userRequest)
    }
    else {
        Toast.makeText(
                activity.applicationContext,
                "Unable to connect to internet",
                Toast.LENGTH_SHORT).show()
    }

}

fun downloadToken(activity: Activity, queue: RequestQueue, email: String, password: String, importRecipes: List<Recipe?>, callback: (success: Boolean) -> Unit) {

    val preferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext())

    val tokenRequest = object : StringRequest(Method.POST, Constants.ENDPOINT2,
            Response.Listener { response ->
                val body = response.toString()

                val gson = GsonBuilder().create()
                val userResponse = gson.fromJson(body, TokenResponse::class.java)

                if (userResponse.success) {
                    setUserEmail(activity.applicationContext, email)
                    val token = userResponse.token
                    val editor = preferences.edit()
                    editor.putString(Constants.FR_TOKEN, token)
                    editor.apply()

                    createRealmUser(token, importRecipes, callback, activity)
                }
                else {
                    Toast.makeText(
                            activity.applicationContext,
                            "${userResponse.message}",
                            Toast.LENGTH_SHORT).show()
                    callback(false)
                }

            },
            Response.ErrorListener { error ->
                Log.d("API", "error on token download ${error.toString()}:")
                Toast.makeText(
                        activity.applicationContext,
                        error.toString(),
                        Toast.LENGTH_SHORT).show()

                callback(false)
            }

    ) {
        override fun getBodyContentType(): String {
            return "application/x-www-form-urlencoded; charset=UTF-8"
        }

        override fun getParams(): MutableMap<String, String> {
            val paramMap :HashMap<String,String> = HashMap<String, String>()
            paramMap.put("email", email);
            paramMap.put("password", password);
            return paramMap
        }
    }

    queue.add(tokenRequest)
}

fun createRealmUser(token: String, recipeList: List<Recipe?>, callback: (success: Boolean) -> Unit, activity: Activity) {

    val credentials: Credentials = Credentials.jwt(token)
    Log.v("API", "Logging In. ${token}")

//    var user: User? = null
    realmApp.loginAsync(credentials) {
        if (it.isSuccess) {
            Log.v("API", "Successfully authenticated using a custom JWT. ${token}")

            //Set the user
            realmApp.currentUser()?.name?.let { it1 ->
                setUsername(activity, it1)
                setUserEmail(activity, it1)
            }

            val user: io.realm.mongodb.User? = realmApp.currentUser()
            val partitionValue: String = "recipes"
            val config = SyncConfiguration.Builder(user, partitionValue)
                    .build()

            val backgroundThreadRealm : Realm = Realm.getInstance(config)

            callback(true)
        } else {
            Log.e("API", "Error logging in: ${it.error.toString()}")
        }
    }

//    val callback2 = object : SyncUser.Callback<SyncUser> {
//
//        override fun onSuccess(user: SyncUser) {
//            Log.d("API", "Realm access successful: Realm User: ${user}")
//
//            //remove recipes from the old
//            val realm = Realm.getDefaultInstance()
//            var recipes = realm.where(Recipe::class.java).findAll()
//            realm.executeTransaction { _ ->
//                try {
//                    recipes.deleteAllFromRealm()
//
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            }
//            realm.close()
//
//            val url = Constants.REALM_URL
//            val synchConfiguration = user.createConfiguration(url)
//                    .fullSynchronization()
//                    .build()
//
//            Realm.setDefaultConfiguration(synchConfiguration)
//
//            Log.d("API", "Inserting recipes: ${recipeList}")
//
//            if (recipeList.count() > 0) {
//                bulkInsertRecipes(recipeList)
//            }
//
//            callback(true)
//        }
//
//        override fun onError(error: ObjectServerError) {
//            Log.d("API", "Realm connection failed")
//
//            val errorMsg: String = when (error.errorCode) {
//                ErrorCode.UNKNOWN_ACCOUNT -> "unknown account"
//                ErrorCode.INVALID_CREDENTIALS -> "invalid credentials"
//                else -> error.toString()
//            }
//            Toast.makeText(
//                    activity.applicationContext,
//                    errorMsg,
//                    Toast.LENGTH_SHORT).show()
//
//            Log.d("API", "error: ${errorMsg}")
//
//            callback(false)
//        }
//    }

//    Log.d("API", "error: ${Constants.AUTH_URL}")
//
//    SyncUser.logInAsync(credentials, Constants.AUTH_URL, callback2)
}

//fun bulkInsertRecipes(recipeList: List<Recipe?>) {
//    if (recipeList.count() > 0) {
//        val realm = Realm.getDefaultInstance()
//
//        realm.executeTransaction { _ ->
//            try {
//
//                for (i in recipeList) {
//                    var recipe = realm.createObject<Recipe>(UUID.randomUUID().toString())
//                    recipe.title = i!!.title
//
//                    if (i.directions.count() > 0) {
//                        for (j in i.directions) {
//                            val dir = realm.createObject<Direction>(UUID.randomUUID().toString())
//                            dir.text = j.text
//                            recipe.directions.add(dir)
//                        }
//                    }
//
//                    if (i.ingredients.count() > 0) {
//                        for (k in i.ingredients) {
//                            val ing = realm.createObject<Ingredient>(UUID.randomUUID().toString())
//                            ing.name = k.name
//                            recipe.ingredients.add(ing)
//                        }
//                    }
//
//                }
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//        realm.close()
//    }
//}

class UserErrorResponse(val message: String, val name: String)

class UserResponse(val message: String, val token: String, val userData: User)

class User(val email: String, val name: String, val _id: String)

class TokenResponse(val success: Boolean, val message: String, val token: String)