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
import com.funkyradish.funky_radish.Constants.AUTH_URL
import io.realm.SyncUser
import io.realm.kotlin.createObject
import io.realm.kotlin.syncSession

//val ENDPOINT = "https://funky-radish-api.herokuapp.com/users"
//val ENDPOINT2 = "https://funky-radish-api.herokuapp.com/authenticate"

val ENDPOINT = "http://10.0.2.2:8080/users"
val ENDPOINT2 = "http://10.0.2.2:8080/authenticate"

val FR_TOKEN = "fr_token"
val FR_USERNAME = "fr_username"
val FR_USER_EMAIL = "fr_user_email"
val OFFLINE = "fr_offline"

fun getToken(context: Context): String {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    return preferences.getString(FR_TOKEN, "")
}

fun setToken(context: Context, token: String) {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val editor = preferences.edit()
    editor.putString(FR_TOKEN, token)
    editor.apply()
}

fun getUserEmail(context: Context): String {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    return preferences.getString(FR_USER_EMAIL, "")
}

fun setUserEmail(context: Context, userEmail: String) {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val editor = preferences.edit()
    editor.putString(FR_USER_EMAIL, userEmail)
    editor.apply()
}

fun setUsername(context: Context, username: String) {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val editor = preferences.edit()
    editor.putString(FR_USERNAME, username)
    editor.apply()
}

fun isOffline(context: Context): Boolean {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    return preferences.getBoolean(OFFLINE, false)
}

fun toggleOfflineMode(context: Context) {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val offline = preferences.getBoolean(OFFLINE, false)
    val editor = preferences.edit()
    editor.putBoolean(OFFLINE, !offline)
    editor.apply()
}

fun isConnected(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = connectivityManager.activeNetworkInfo
    return networkInfo != null && networkInfo.isConnected
}

fun createUser(activity: Activity, queue: RequestQueue, username: String, email: String, password: String, importRecipes: RealmList<Recipe?>, callback: (success: Boolean) -> Unit) {
    
    Log.d("API", "rec to add: ${importRecipes.toString()}")


    // Structure user data
    val json = JSONObject().apply({
        put("name", username)
        put("email", email)
        put("password", password)
        put("admin", false)
    })

    // User request
    val userRequest = object : JsonObjectRequest(Method.POST, ENDPOINT, json,
            Response.Listener<JSONObject> { response ->

                val body = response.toString()
                val userResponse = GsonBuilder().create().fromJson(body, UserResponse::class.java)

                setToken(activity.applicationContext, userResponse.token)
                setUsername(activity.applicationContext, userResponse.userData.name)
                setUserEmail(activity.applicationContext, userResponse.userData.email)

                Log.d("API", "token: ${userResponse.token}")

                createRealmUser(userResponse.token, importRecipes, callback, activity)
            },
            Response.ErrorListener { error ->

                Log.d("API", "error on createUser")

                Toast.makeText(
                        activity.applicationContext,
                        error.toString(),
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
        // Add the request to the Volley queue
        queue.add(userRequest)
    }
    else {
        Toast.makeText(
                activity.applicationContext,
                "Unable to connect to internet",
                Toast.LENGTH_SHORT).show()
    }

}

fun downloadToken(activity: Activity, queue: RequestQueue, email: String, password: String, callback: (success: Boolean) -> Unit) {
    Log.d("API", "Requesting authorization token.")

    // Prepare a space for your token in preferences.
    val FR_TOKEN = "fr_token"
    val preferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext())

    val tokenRequest = object : StringRequest(Method.POST, ENDPOINT2,
            Response.Listener { response ->
                val body = response.toString()
                val gson = GsonBuilder().create()
                val userResponse = gson.fromJson(body, TokenResponse::class.java)

                val token = userResponse.token
                val editor = preferences.edit()
                editor.putString(FR_TOKEN, token)
                editor.apply()

                Log.d("API", "Token stored: ${token}")

                var recipeList = RealmList<Recipe?>()
                createRealmUser(token, recipeList, callback, activity)
            },
            Response.ErrorListener { error ->
                Log.d("API", "error ${error.toString()}:")
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

fun createRealmUser(token: String, recipeList: RealmList<Recipe?>, callback: (success: Boolean) -> Unit, activity: Activity) {

    var credentials = SyncCredentials.jwt(token)

    Log.d("API", "Gonna try and execute jwt synch.")

    val callback2 = object : SyncUser.Callback<SyncUser> {

        override fun onSuccess(user: SyncUser) {
            Log.d("API", "Realm access successful: Realm User: ${user}")

            val url = Constants.REALM_URL
            val synchConfiguration = user.createConfiguration(url)
                    .fullSynchronization()
                    .build()

            Realm.setDefaultConfiguration(synchConfiguration)

            Log.d("API", "Inserting recipes: ${recipeList}")

            if (recipeList.count() > 0) {
                bulkInsertRecipes(recipeList)
            }

            callback(true)
        }

        override fun onError(error: ObjectServerError) {
            Log.d("API", "Realm connection failed")

            val errorMsg: String = when (error.errorCode) {
                ErrorCode.UNKNOWN_ACCOUNT -> "unknown account"
                ErrorCode.INVALID_CREDENTIALS -> "invalid credentials"
                else -> error.toString()
            }
            Toast.makeText(
                    activity.applicationContext,
                    errorMsg,
                    Toast.LENGTH_SHORT).show()

            Log.d("API", "error: ${errorMsg}")

            callback(false)
        }
    }

    SyncUser.logInAsync(credentials, AUTH_URL, callback2)
}

fun bulkInsertRecipes(recipeList: RealmList<Recipe?>) {
    if (recipeList.count() > 0) {

        Log.d("API", "bulk insert called")

        val realm = Realm.getDefaultInstance()

        realm.executeTransaction { _ ->
            try {

                for (i in recipeList) {
                    Log.d("API", "Inserting recipe: ${i}")
                    var recipe = realm.createObject<Recipe>(UUID.randomUUID().toString())
                    recipe.title = i!!.title

                    if (i.directions.count() > 0) {
                        for (j in i.directions) {
                            Log.d("API", "Inserting direction: ${j}")
                            val dir = realm.createObject<Direction>(UUID.randomUUID().toString())
                            dir.text = j.text
                            recipe.directions.add(dir)
                        }
                    }

                    if (i.ingredients.count() > 0) {
                        for (k in i.ingredients) {
                            Log.d("API", "Inserting ingredient: ${k}")
                            val ing = realm.createObject<Ingredient>(UUID.randomUUID().toString())
                            ing.name = k.name
                            recipe.ingredients.add(ing)
                        }
                    }

                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }
}

class UserResponse(val message: String, val token: String, val userData: User)

class User(val email: String, val name: String, val _id: String)

class TokenResponse(val success: Boolean, val message: String, val token: String)