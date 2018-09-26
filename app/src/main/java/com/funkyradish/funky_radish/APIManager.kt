package com.funkyradish.funky_radish

import android.app.Activity
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject
import com.google.gson.GsonBuilder
import org.json.JSONArray

val ENDPOINT = "https://funky-radish-api.herokuapp.com/users"
val ENDPOINT2 = "https://funky-radish-api.herokuapp.com/authenticate"
val ENDPOINT3 = "https://funky-radish-api.herokuapp.com/recipes"

fun loadRecipes(activity: Activity, queue: RequestQueue, token: String) {

    // Build recipe request
    val recipeRequest = object : JsonArrayRequest(Method.GET, ENDPOINT3, null,
            Response.Listener<JSONArray> { response ->
                val body = response.toString()
//                val gson = GsonBuilder().create()
//                val userResponse = gson.fromJson(body, RecipeResponse::class.java)
                Log.d("API", body)
            },
            Response.ErrorListener { error ->
                Log.d("API", "There was an error getting your recipes.")
                Toast.makeText(
                        activity.applicationContext,
                        error.toString(),
                        Toast.LENGTH_SHORT).show()
            }
    ) {
        override fun getHeaders(): Map<String, String> {
            val headers = HashMap<String, String>()
            headers.put("x-access-token", token)
            return headers
        }
    }

    // Add the request to the Volley queue
    queue.add(recipeRequest)

}

fun createUser(activity: Activity, queue: RequestQueue, username: String, email: String, password: String) {

    // Structure user data
    val json = JSONObject().apply({
        put("name", username)
        put("email", email)
        put("password", password)
        put("admin", false)
    })

    // Build user request
    val userRequest = object : JsonObjectRequest(Method.POST, ENDPOINT, json,
            Response.Listener<JSONObject> { response ->
                Log.d("API", "User created.")
                val body = response.toString()
                val gson = GsonBuilder().create()
                val userResponse = gson.fromJson(body, UserResponse::class.java)
                Log.d("API", userResponse.message)

                getToken(activity, queue, email, password)
            },
            Response.ErrorListener { error ->
                Log.d("API", "There was an error creating a user.")
                Toast.makeText(
                        activity.applicationContext,
                        error.toString(),
                        Toast.LENGTH_SHORT).show()
            }
    ) {
        override fun getHeaders(): Map<String, String> {
            val headers = HashMap<String, String>()
            headers.put("Content-Type", "application/json")
            return headers
        }
    }

    // Add the request to the Volley queue
    queue.add(userRequest)
}

fun getToken(activity: Activity, queue: RequestQueue, email: String, password: String) {
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

                Log.d("API", "Token stored.")

                // Then we should load recipes, followed by the main recipe view.
            },
            Response.ErrorListener { error ->
                Toast.makeText(
                        activity.applicationContext,
                        error.toString(),
                        Toast.LENGTH_SHORT).show()
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

class UserResponse(val message: String, val data: User)

class User(val email: String, val name: String, val _id: String)

class TokenResponse(val success: Boolean, val message: String, val token: String)

class RecipeResponse: JSONArray()

//class Recipe(val title: String, val _id: String)