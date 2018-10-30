package com.funkyradish.funky_radish

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat.startActivity
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

fun synchRecipes(activity: Activity, queue: RequestQueue, recipes: JSONArray) {

    val realm = Realm.getDefaultInstance()
    var localRecipes = realm.where(Recipe::class.java).findAll()
    var remoteRecipes = recipes

    var updateRecipes = RealmList<Recipe>()
    var uploadRecipeList = RealmList<Recipe>()
    var updateRemoteRecipeList = RealmList<Recipe>()

    uploadRecipeList.addAll(localRecipes)

    // Compare local recipes to remote recipes.

    //Iterate the list of recipes you just downloaded
    val finalIndex = remoteRecipes.length() - 1
    for (i in 0..finalIndex) {
        val remoteRecipe = remoteRecipes.getJSONObject(finalIndex - i)
        println(remoteRecipe.toString())

        //check if the recipe has a local copy
        val localInstance = localRecipes.filter { localRecipe -> localRecipe._id == remoteRecipe["_id"] }
        if (localInstance.size > 0) {
            println("there's a recipe already. We should check if it's older than the remote version.")

            //remove this recipe from the upload list because it already exists remotely.
            uploadRecipeList.drop(uploadRecipeList.indexOf(localInstance.first()))

            //If({ thisRecipe -> thisRecipe._id == localInstance.first()._id})

            val formatter = DateTimeFormatter.ofPattern("EE MMM dd uuuu HH:mm:ss zZ")
            val localDate = LocalDateTime.parse(localInstance.first().updatedAt.removeSuffix(" (UTC)"), formatter)
            val remoteDate = LocalDateTime.parse(remoteRecipe["updatedAt"].toString().removeSuffix(" (UTC)"), formatter)

            // if the local copy is older, update the local. If it's fresher, update the remote copy.
            // Otherwise, remove it from the remoteRecipesList so it doesn't get copied into Realm again.
            if (remoteDate > localDate) {
                println("we should update the local recipe.")
                updateRecipes.add(localInstance.first())
            }
            else if (localDate > remoteDate) {
                // Add it to the remote update list
                updateRemoteRecipeList.add(localInstance.first())

                // Remove from the download list.
                val output = JSONArray()
                val len = remoteRecipes.length()-1
                for (j in 0..len) {
                    if (j != i) {
                        output.put(remoteRecipes.get(j));
                    }
                }
                remoteRecipes = output
            }
            else {
                // Recipes have the same update time, so we can just remove it from the download list.
                val output = JSONArray()
                val len = remoteRecipes.length()-1
                for (j in 0..len) {
                    if (j != i) {
                        output.put(remoteRecipes.get(j));
                    }
                }
                remoteRecipes = output
            }
        }

        else {
            println("there's not a recipe already, so we should def download this... However, if the id is on the delete list . . . We should delete it from the remote copy.")
        }
    }

    // push new recipes to API
    uploadRecipes(activity, queue, uploadRecipeList)

    // push updated recipes to API
    updateRemoteRecipes(activity, queue, updateRemoteRecipeList)

    // TODO: Recipes without a local copy (deleted recipes) have not been removed from the list of remote recipes.
    if(remoteRecipes.length() > 0) {
        val body = remoteRecipes.toString()

        realm.executeTransaction { realm ->
            try {
                realm.createOrUpdateAllFromJson(Recipe::class.java, body)
            } catch (e: Exception) {
                e.printStackTrace()
            }
//        TODO: Should reload the recipe search view
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

    //TODO: Do I need to also update the updatedAt on the recipe after recipes are saved?

    println(recipes.toString())
//    val token = getToken(activity.getApplicationContext())
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
        for (i in 0..(it.ingredients.size-1)) {
            println(it.ingredients[i])
            dirArray.put(it.ingredients[i])
        }
        val dir = dirArray

        val title = it.title

        val jsonRecipe = JSONObject().apply({
            put("ingredients", ing)
            put("directions", dir)
            put("title", title)
        })

        recipeSet.put(jsonRecipe)
    }

    println("upload")
    println(recipeSet.toString())

    // Build recipe post request
    val recipePostRequest = object : JsonArrayRequest(Method.POST, ENDPOINT3, recipeSet,
            Response.Listener<JSONArray> { response ->
                val body = response.toString()
                println(body)

                Log.d("API", "Recipes uploaded.")
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
            headers.put("x-access-token", token)
            return headers
        }
    }

    // Add the request to the Volley queue
    queue.add(recipePostRequest)

}

fun updateRemoteRecipes(activity: Activity, queue: RequestQueue, recipes: RealmList<Recipe>) {
   println(recipes.toString())
//    val token = getToken(activity.getApplicationContext())
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
        for (i in 0..(it.ingredients.size-1)) {
            println(it.ingredients[i])
            dirArray.put(it.ingredients[i])
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

    println(recipeSet.toString())
    println("update")

    // Build user request
    val recipePostRequest = object : JsonArrayRequest(Method.POST, ENDPOINT4, recipeSet,
            Response.Listener<JSONArray> { response ->
                val body = response.toString()
                println(body)

                Log.d("API", "Recipes saved.")
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
            headers.put("x-access-token", token)
            return headers
        }
    }

    // Add the request to the Volley queue
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

class RecipeResponse(val message: String)

class User(val email: String, val name: String, val _id: String)

class TokenResponse(val success: Boolean, val message: String, val token: String)