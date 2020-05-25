package com.funkyradish.funky_radish

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
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

//val ENDPOINT = "https://funky-radish-api.herokuapp.com/users"
//val ENDPOINT2 = "https://funky-radish-api.herokuapp.com/authenticate"
val ENDPOINT3 = "https://funky-radish-api.herokuapp.com/recipes"
val ENDPOINT4 = "https://funky-radish-api.herokuapp.com/updateRecipes"

val ENDPOINT = "http://10.0.2.2:8080/users"
val ENDPOINT2 = "http://10.0.2.2:8080/authenticate"
//val ENDPOINT3 = "http://10.0.2.2:8080/recipes"
//val ENDPOINT4 = "hhttp://10.0.2.2:8080/updateRecipes"

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

fun getUsername(context: Context): String {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    return preferences.getString(FR_USERNAME, "")
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

//fun isConnectedToInternet(context: Context): Boolean {
//    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//    val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
//
//    val isConnected: Boolean = activeNetwork?.isConnected == false
//    return isConnected
//}

fun createUser(activity: Activity, queue: RequestQueue, username: String, email: String, password: String, callback: (success: Boolean) -> Unit) {

    // Structure user data
    val json = JSONObject().apply({
        put("name", username)
        put("email", email)
        put("password", password)
        put("admin", false)
    })

    Log.d("API", "Params start.")
    Log.d("API", username)
    Log.d("API", email)
    Log.d("API", password)
    Log.d("API", "Params end.")

    // Build user request
    val userRequest = object : JsonObjectRequest(Method.POST, ENDPOINT, json,
            Response.Listener<JSONObject> { response ->
                Log.d("API", "User created.")
                val body = response.toString()

                Log.d("API", "response string: ${body}:")

                val gson = GsonBuilder().create()
                val userResponse = gson.fromJson(body, UserResponse::class.java)
                Log.d("API", userResponse.message)

                Log.d("API", "user response: ${userResponse.toString()}:")


                val FR_TOKEN = "fr_token"
                val FR_USERNAME = "fr_username"
                val FR_USER_EMAIL = "fr_user_email"
                val preferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext())

                val token = userResponse.token
                val username = userResponse.userData.name
                val email = userResponse.userData.email

                val editor = preferences.edit()

                editor.putString(FR_TOKEN, token)
                editor.putString(FR_USERNAME, username)
                editor.putString(FR_USER_EMAIL, email)
                editor.apply()

                createRealmUser(token, callback, activity)


//                downloadToken(activity, queue, email, password, false, {
//                    Log.d("API", "Logging in.")
//
//                    callback(true)
//                })
            },
            Response.ErrorListener { error ->
                Log.d("API", "There was an error creating a user.")
                error.printStackTrace()
                Log.d("API", error.toString())


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

                createRealmUser(token, callback, activity)

//                callback()
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

fun createRealmUser(token: String, callback: (success: Boolean) -> Unit, activity: Activity) {
    Log.d("API", "Time to access the Realm.")

//    var credentials = SyncCredentials.usernamePassword(username, password, true)

    var credentials = SyncCredentials.jwt(token)

    val callback2 = object : SyncUser.Callback<SyncUser> {
        override fun onSuccess(user: SyncUser) {
            Log.d("API", "realm access successful.")
            callback(true)
        }

        override fun onError(error: ObjectServerError) {
            Log.d("API", "the realm connection has failed")
            Log.d("API", error.toString())
            val errorMsg: String = when (error.errorCode) {
                ErrorCode.UNKNOWN_ACCOUNT -> "unknown account"
                ErrorCode.INVALID_CREDENTIALS -> "invalid credentials"
                else -> error.toString()
            }
            Toast.makeText(
                    activity.applicationContext,
                    errorMsg,
                    Toast.LENGTH_SHORT).show()

            callback(false)
        }
    }

    SyncUser.logInAsync(credentials, AUTH_URL, callback2)
}

class UserResponse(val message: String, val token: String, val userData: User)

class User(val email: String, val name: String, val _id: String)

class TokenResponse(val success: Boolean, val message: String, val token: String)