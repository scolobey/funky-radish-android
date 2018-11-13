package com.funkyradish.funky_radish

import android.app.Activity
import android.content.Context
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
import io.realm.Realm
import io.realm.RealmList
import org.json.JSONArray
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

val ENDPOINT = "https://funky-radish-api.herokuapp.com/users"
val ENDPOINT2 = "https://funky-radish-api.herokuapp.com/authenticate"
val ENDPOINT3 = "https://funky-radish-api.herokuapp.com/recipes"
val ENDPOINT4 = "https://funky-radish-api.herokuapp.com/updateRecipes"

val FR_TOKEN = "fr_token"
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

fun loadRecipes(activity: Activity, queue: RequestQueue, token: String) {

    Log.d("API", "downloading recipes.")

    // Build recipe request
    val recipeRequest = object : JsonArrayRequest(Method.GET, ENDPOINT3, null,
            Response.Listener<JSONArray> { response ->
                synchRecipes(activity, queue, response)
            },
            Response.ErrorListener { error ->
                Log.d("API", "There was an error loading your recipes.")

                val warning = "Authorization failed. Please login."
                Toast.makeText(
                        activity.applicationContext,
                        warning,
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

fun synchRecipes(activity: Activity, queue: RequestQueue, recipes: JSONArray) {

    val realm = Realm.getDefaultInstance()

    var remoteRecipes = recipes
    var localRecipes = realm.where(Recipe::class.java).findAll()

    // This is the list of new recipes that need to be uploaded. Better name? : recipePost, uploadList
    var uploadRecipeList = RealmList<Recipe>()
    uploadRecipeList.addAll(localRecipes)

    // This is the list of recipes that have been changed locally and should be pushed to the api. Better name? : recipePut, updateList
    var updateRemoteRecipeList = RealmList<Recipe>()

    Log.d("API", "Synchronizing recipes}")

    //Reverse iterate downloaded recipes
    val finalIndex = remoteRecipes.length()-1

    for (i in finalIndex downTo 0) {
        Log.d("API", "Checking recipe at index. ${i}")

        val remoteRecipe = remoteRecipes.getJSONObject(i)

        //check for a local copy
        val localRecipe = localRecipes.filter { localInstance -> localInstance._id == remoteRecipe["_id"] }

        if (localRecipe.count() > 0) {

            Log.d("API", "Local copy exists: ${localRecipe.first().title} ${localRecipe.first().updatedAt}")

            val formatter = DateTimeFormatter.ofPattern("EE MMM dd uuuu HH:mm:ss zZ")
            val localDate = LocalDateTime.parse(localRecipe.first().updatedAt.removeSuffix(" (UTC)"), formatter)
            val remoteDate = LocalDateTime.parse(remoteRecipe["updatedAt"].toString().removeSuffix(" (UTC)"), formatter)

            Log.d("API", "Which recipe is older? Local: ${localDate.toString()} Remote: ${remoteDate.toString()}")

            if (localDate > remoteDate) {
                Log.d("API", "Local recipe is ahead")

                updateRemoteRecipeList.add(localRecipe.first())

                val output = JSONArray()
                val len = remoteRecipes.length()-1
                for (j in 0..len) {
                    if (j != i) {
                        output.put(remoteRecipes.get(j))
                    }
                }
                remoteRecipes = output

                Log.d("API", "uploadList: ${updateRemoteRecipeList.toString()}")
                Log.d("API", "updateList: ${remoteRecipes.toString()}")
            }
            else if (remoteDate > localDate) {
                //need to save the remote recipe to realm. Just don't remove it from the update list.
                Log.d("API", "Remote recipe is ahead: ${remoteRecipe.toString()}")
            }
            else {
                Log.d("API", "Recipes have the same date.")

                val output = JSONArray()
                val len = remoteRecipes.length()-1
                for (j in 0..len) {
                    if (j != i) {
                        output.put(remoteRecipes.get(j))
                    }
                }
                remoteRecipes = output
            }

            // Remove the recipe from the uploadRecipeList list.
            uploadRecipeList.remove(localRecipe.first())
        }

        else {
            Log.d("API", "Local copy not found.")
        }
    }

    if(uploadRecipeList.count() > 0) {
        uploadRecipes(activity, queue, uploadRecipeList)
    }

    if(updateRemoteRecipeList.count() > 0) {
        updateRemoteRecipes(activity, queue, updateRemoteRecipeList)
    }

    if(remoteRecipes.length() > 0) {
        Log.d("API", "Adding ${remoteRecipes.toString()} recipes to local Realm.")

        val body = remoteRecipes.toString()

        realm.executeTransaction { realm ->
            try {
                realm.createOrUpdateAllFromJson(Recipe::class.java, body)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

fun saveRecipe(activity: Activity, queue: RequestQueue, title: String?, ingredients: List<String>, directions: List<String>) {
    Log.d("API", "Saving recipe.")

    val token = getToken(activity.getApplicationContext())

    // What if the token is expired?
    val json = JSONObject().apply({
        put("title", title)
        put("ingredients", ingredients)
        put("directions", directions)
    })

    val recipePost = object : JsonObjectRequest(Method.POST, ENDPOINT3, json,
            Response.Listener { response ->
                val body = response.toString()
                println(body)

                Log.d("API", "Recipe saved.")
            },
            Response.ErrorListener { error ->
                Toast.makeText(
                        activity.applicationContext,
                        "It looks like your token is expired. Please login.",
                        Toast.LENGTH_SHORT).show()
            }
    ) {
        override fun getHeaders(): Map<String, String> {
            val headers = HashMap<String, String>()
            headers.put("x-access-token", token)
            return headers
        }
    }

    queue.add(recipePost)
}

fun uploadRecipes(activity: Activity, queue: RequestQueue, recipes: RealmList<Recipe>) {
    // TODO: Update the updatedAt on the recipe after recipes are saved? Need to adjust the API for this.

    var recipeSet = JSONArray()
    val token = getToken(activity.getApplicationContext())

    recipes.forEach {

        var ingArray = JSONArray()
        for (i in 0..(it.ingredients.size-1)) {
            println(it.ingredients[i])
            ingArray.put(it.ingredients[i])
        }
        val ing = ingArray

        var dirArray = JSONArray()
        for (i in 0..(it.directions.size-1)) {
            println(it.directions[i])
            dirArray.put(it.directions[i])
        }
        val dir = dirArray

        val _id = it._id
        val title = it.title

        val jsonRecipe = JSONObject().apply({
            put("ingredients", ing)
            put("directions", dir)
            put("title", title)
            put("_id", _id)
        })

        recipeSet.put(jsonRecipe)
    }

    Log.d("API", "Uploading: ${recipeSet.toString()}")

    // Build recipe post request
    val recipePostRequest = object : JsonArrayRequest(Method.POST, ENDPOINT3, recipeSet,
            Response.Listener<JSONArray> { response ->
                Log.d("API", "Recipes uploaded.")
            },
            Response.ErrorListener { error ->
                Toast.makeText(
                        activity.applicationContext,
                        error.toString(),
                        Toast.LENGTH_SHORT).show()
            }
    ) {
        override fun getHeaders(): Map<String, String> {
            val headers = HashMap<String, String>()
            headers.put("Content-Type", "application/json")
            headers.put("x-access-token", token)
            return headers
        }
    }

    // Add the request to the Volley queue
    queue.add(recipePostRequest)
}

fun updateRemoteRecipes(activity: Activity, queue: RequestQueue, recipes: RealmList<Recipe>) {

    Log.d("API", "Updating recipes: ${recipes.toString()}")

    var recipeSet = JSONArray()
    val token = getToken(activity.getApplicationContext())

    recipes.forEach {

        var ingArray = JSONArray()
        for (i in 0..(it.ingredients.size-1)) {
            ingArray.put(it.ingredients[i])
        }
        val ing = ingArray

        var dirArray = JSONArray()
        for (i in 0..(it.directions.size-1)) {
            dirArray.put(it.directions[i])
        }
        val dir = dirArray

        val title = it.title
        val _id = it._id
        val updatedAt = it.updatedAt

        val jsonRecipe = JSONObject().apply({
            put("ingredients", ing)
            put("directions", dir)
            put("title", title)
            put("_id", _id)
            put("updatedAt", updatedAt)
        })

        recipeSet.put(jsonRecipe)
    }

    Log.d("API", "Update json: ${recipeSet.toString()}")

    // Build user request
    val recipePostRequest = object : JsonArrayRequest(Method.PUT, ENDPOINT4, recipeSet,
            Response.Listener<JSONArray> { response ->
                Log.d("API", "Recipes saved.")
            },
            Response.ErrorListener { error ->
                Log.d("API", "Error updating remote recipes.")
                Toast.makeText(
                        activity.applicationContext,
                        error.toString(),
                        Toast.LENGTH_SHORT).show()
            }
    ) {
        override fun getHeaders(): Map<String, String> {
            val headers = HashMap<String, String>()
            headers.put("Content-Type", "application/json")
            headers.put("x-access-token", token)
            return headers
        }
    }

    queue.add(recipePostRequest)
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

                downloadToken(activity, queue, email, password, { println("login called.")})

                // if there are recipes in realm, upload them.
                synchRecipes(activity, queue, JSONArray())
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

fun loginUser(activity: Activity, queue: RequestQueue, email: String, password: String) {
    downloadToken(activity, queue, email, password, { println("login called.")})
}

fun downloadToken(activity: Activity, queue: RequestQueue, email: String, password: String, callback: () -> Unit) {
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

                synchRecipes(activity, queue, JSONArray())

                callback()
            },
            Response.ErrorListener { error ->
                Toast.makeText(
                        activity.applicationContext,
                        error.toString(),
                        Toast.LENGTH_SHORT).show()

                callback()
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

class RecipeResponse(val message: String)

class User(val email: String, val name: String, val _id: String)

class TokenResponse(val success: Boolean, val message: String, val token: String)