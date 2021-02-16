package com.funkyradish.funky_radish

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import androidx.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import com.android.volley.*
import com.android.volley.toolbox.*
import com.google.gson.GsonBuilder
import io.realm.*
import org.json.JSONObject
import java.util.*
import io.realm.kotlin.createObject
import io.realm.mongodb.Credentials
import io.realm.mongodb.sync.SyncConfiguration
import org.json.JSONException

fun getToken(context: Context): String? {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    var str = preferences.getString(Constants.FR_TOKEN, "")
    if (str == null) {
        str = ""
    }
    return str
}

fun setToken(context: Context, token: String ) {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val editor = preferences.edit()
    editor.putString(Constants.FR_TOKEN, token)
    editor.apply()
}

fun getUserEmail(context: Context): String? {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    return preferences.getString(Constants.FR_USER_EMAIL, "")
}

fun setUserEmail(context: Context, userEmail: String) {
    Log.d("API", "Email: ${userEmail}")
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val editor = preferences.edit()
    editor.putString(Constants.FR_USER_EMAIL, userEmail)
    editor.apply()
}

fun setUsername(context: Context, username: String) {
    Log.d("API", "Username: ${username}")
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

//fun isConnected(context: Context): Boolean {
//    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//    val networkInfo = connectivityManager.activeNetworkInfo
//    return networkInfo != null && networkInfo.isConnected
//}

//TODO: get rid of the importRecipes
fun register(activity: Activity, queue: RequestQueue, email: String, password: String, callback: (success: Boolean) -> Unit) {

    // Structure user data
    val json = JSONObject().apply {
        put("email", email)
        put("password", password)
        put("admin", false)
    }

    // User request
    val userRequest = object : JsonObjectRequest(Method.POST, Constants.ENDPOINT, json,
            Response.Listener<JSONObject> { response ->
                val body = response.toString()
                val userResponse = GsonBuilder().create().fromJson(body, UserResponse::class.java)

                // If message = Verification email sent.
                if (userResponse.message == "Verification email sent.") {
                    callback(true)
                }
                else {
                    Toast.makeText(
                            activity.applicationContext,
                            userResponse.message,
                            Toast.LENGTH_SHORT).show()

                    callback(false)
                }
            },
            Response.ErrorListener { error ->
                Log.d("API", "error on createUser: ${error}")
                error.printStackTrace()

                var errorMessage = error.toString()

                val response = error.networkResponse
                if (error is ServerError && response != null) {
                    try {
                        val jsonResponse = String(response.data)
                        val userErrorResponse = GsonBuilder().create().fromJson(jsonResponse, UserErrorResponse::class.java)
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

    queue.add(userRequest)

//    if(isConnected(activity.applicationContext)) {
//        queue.add(userRequest)
//    }
//    else {
//        Toast.makeText(
//                activity.applicationContext,
//                "Unable to connect to internet",
//                Toast.LENGTH_SHORT).show()
//    }

}

fun login(activity: Activity, queue: RequestQueue, email: String, password: String, importRecipes: List<Recipe?>, callback: (success: Boolean) -> Unit) {

    // Structure user data
    val json = JSONObject().apply {
        put("email", email)
        put("password", password)
    }

    val authRequest = object : JsonObjectRequest(Method.POST, Constants.AuthEndpoint, json,
            Response.Listener<JSONObject> { resp ->
                val body = resp.toString()
                val authResponse = GsonBuilder().create().fromJson(body, UserResponse::class.java)

                if (authResponse.message == "Enjoy your token, ya filthy animal!") {
                    setToken(activity.applicationContext, authResponse.token)
                    createRealmUser(authResponse.token, importRecipes, callback, activity)
                }
                else {
                    Toast.makeText(
                            activity.applicationContext,
                            authResponse.message,
                            Toast.LENGTH_SHORT).show()

                    callback(false)
                }
            },
            Response.ErrorListener { error ->
                Log.d("API", "Authorization error: $error")
                error.printStackTrace()

                var errorMessage = error.toString()

                val authResponse = error.networkResponse
                if (error is ServerError && authResponse != null) {
                    try {
                        val jsonResp = String(authResponse.data)
                        val userErrorResponse = GsonBuilder().create().fromJson(jsonResp, UserErrorResponse::class.java)
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

    queue.add(authRequest)

//    //TODO: Do we really need to check connection?
//    if(isConnected(activity.applicationContext)) {
//        queue.add(authRequest)
//    }
//    else {
//        Toast.makeText(
//                activity.applicationContext,
//                "Unable to connect to internet",
//                Toast.LENGTH_SHORT).show()
//    }

}

fun createRealmUser(token: String, recipeList: List<Recipe?>, callback: (success: Boolean) -> Unit, activity: Activity) {
    val credentials: Credentials = Credentials.jwt(token)
    Log.v("API", "Logging In. ${token}")

    realmLogin(activity, credentials, recipeList, callback)
}

fun realmLogin(activity: Activity, credentials: Credentials, recipeList: List<Recipe?>, callback: (success: Boolean) -> Unit) {

    Log.v("API", "Realm Login")

    realmApp.loginAsync(credentials) {
        if (it.isSuccess) {
            Log.v("API", "Successfully authenticated")

            val user: io.realm.mongodb.User? = realmApp.currentUser()

            if (user != null) {
                setUsername(activity, user.name)
                setUserEmail(activity, user.name)

                //remove recipes from realm
                var recipes = realm.where(Recipe::class.java).findAll()

                realm.executeTransaction { _ ->
                    try {
                        recipes.deleteAllFromRealm()
                        Log.v("API", "Inner thread called.")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                realm.close()

                //Switch to new realm
                val partitionValue: String = user.id
                val config = SyncConfiguration.Builder(user, partitionValue).build()
                realm = Realm.getInstance(config)

                // Add recipes to new realm
                bulkInsertRecipes(recipeList)
            }

            Log.v("API", "There are ${realmApp.allUsers().count()} users")
            callback(true)
        } else {
            Log.e("API", "Error logging in: ${it.error.toString()}")
            callback(false)
        }
    }
}

fun bulkInsertRecipes(recipeList: List<Recipe?>) {
    if (recipeList.count() > 0) {

        realm.executeTransaction { _ ->
            try {

                for (i in recipeList) {
                    var recipe = realm.createObject<Recipe>(UUID.randomUUID().toString())
                    recipe.title = i!!.title

                    var user = realmApp.currentUser()

                    if (user != null) {
                        recipe.author = user.id
                    }

                    if (i.directions.count() > 0) {
                        for (j in i.directions) {
                            val dir = realm.createObject<Direction>(UUID.randomUUID().toString())
                            dir.author = recipe.author
                            dir.text = j.text

                            recipe.directions.add(dir)
                        }
                    }

                    if (i.ingredients.count() > 0) {
                        for (k in i.ingredients) {
                            val ing = realm.createObject<Ingredient>(UUID.randomUUID().toString())
                            ing.author = recipe.author
                            ing.name = k.name
                            recipe.ingredients.add(ing)
                        }
                    }

                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
//        realm.close()
    }
}

class UserErrorResponse(val message: String, val name: String)

class UserResponse(val message: String, val token: String, val error: String)

class User(val email: String, val name: String, val _id: String)
