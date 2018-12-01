package com.funkyradish.funky_radish

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import com.android.volley.*
import com.android.volley.toolbox.*
import com.google.gson.Gson
import org.json.JSONObject
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import io.realm.Realm
import io.realm.RealmList
import org.json.JSONArray
import java.io.UnsupportedEncodingException
import java.lang.reflect.Type
import java.nio.charset.Charset
import java.text.SimpleDateFormat
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

    Log.d("API", "Synchronizing recipes.")

    //Reverse iterate downloaded recipes
    val finalIndex = remoteRecipes.length()-1

    for (i in finalIndex downTo 0) {
        Log.d("API", "Inspecting recipe at index: ${i}")

        val remoteRecipe = remoteRecipes.getJSONObject(i)

        //check for a local copy
        val localRecipe = localRecipes.filter { localInstance -> localInstance.realmID == remoteRecipe["realmID"] }

        if (localRecipe.count() > 0) {

            Log.d("API", "Local copy exists: ${localRecipe.first().title} ${localRecipe.first().updatedAt}")

            var synchMode = 0

            // TODO: Are both really necessary?

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val formatter = DateTimeFormatter.ofPattern("EE MMM dd uuuu HH:mm:ss zZ")
                val localDate = LocalDateTime.parse(localRecipe.first().updatedAt.removeSuffix(" (UTC)"), formatter)
                val remoteDate = LocalDateTime.parse(remoteRecipe["updatedAt"].toString().removeSuffix(" (UTC)"), formatter)

                Log.d("API", "Android new: Which recipe is older? Local: ${localDate.toString()} Remote: ${remoteDate.toString()} other: ${localDate.compareTo(remoteDate)}")

                if (localDate.isAfter(remoteDate)) {
                    synchMode = 1
                    Log.d("API", "remote recipe is older")
                }
                else if (remoteDate.isAfter(localDate)) {
                    synchMode = 2
                    Log.d("API", "local recipe is older")
                }
            }
            else {
                val formatter = SimpleDateFormat("EEE MMM d yyy HH:mm:ss")
                val localDate = formatter.parse(localRecipe.first().updatedAt)
                val remoteDate = formatter.parse(remoteRecipe["updatedAt"].toString())

                Log.d("API", "Android new: Which recipe is older? Local: ${localDate.toString()} Remote: ${remoteDate.toString()} other: ${localDate.compareTo(remoteDate)}")

                if (localDate > remoteDate) {
                    synchMode = 1
                    Log.d("API", "remote recipe is older")
                }
                else if (remoteDate > localDate) {
                    synchMode = 2
                    Log.d("API", "local recipe is older")
                }
            }

            // mode #1 - The recipe on the device has been edited more recently than the online version.
            if (synchMode == 1) {
                Log.d("API", "Local recipe is ahead")

                // Special case where a recipe has been saved, but the callback did not persist recipe._id to local successfully
                if (localRecipe.first()._id.count() == 0) {
                    Log.d("API", "WTF - local doesn't have an id ${remoteRecipe["_id"]}")

                    realm.executeTransaction { realm ->
                        try {
                            localRecipe.first()._id = remoteRecipe["_id"].toString()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                updateRemoteRecipeList.add(localRecipe.first())

                //remove from remoteRecipes so that the local recipe is not updated.
                val output = JSONArray()

                val len = remoteRecipes.length()-1
                for (j in 0..len) {
                    if (j != i) {
                        output.put(remoteRecipes.get(j))
                    }
                }
                remoteRecipes = output

                Log.d("API", "updateList: ${updateRemoteRecipeList.toString()}")
            }

            // mode #2 - The online version has been edited more recently than the version on the device.
            else if (synchMode == 2) {
                //need to save the remote recipe to realm. Just don't remove it from the update list.
                Log.d("API", "Remote recipe is ahead: ${remoteRecipe.toString()}")
            }

            // mode #0 (default) - Online version and device version have the same updatedAt time.
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
                Log.d("API", "Recipe saved: ${body}")
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

        val realmID = it.realmID
        val title = it.title
        val updatedAt = it.updatedAt

        val jsonRecipe = JSONObject().apply({
            put("ingredients", ing)
            put("directions", dir)
            put("title", title)
            put("realmID", realmID)
            put("updatedAt", updatedAt)
        })

        recipeSet.put(jsonRecipe)
    }

    Log.d("API", "Uploading: ${recipeSet.toString()}")

    // Build recipe post request
    val recipePostRequest = object : JsonArrayRequest(Method.POST, ENDPOINT3, recipeSet,
            Response.Listener<JSONArray> { response ->
                Log.d("API", "Recipes uploaded. Updating Realm.")

                val realm = Realm.getDefaultInstance()
                val body = response.toString()

                // Set the _id of each recipe.
                realm.executeTransaction { realm ->
                    try {
                        realm.createOrUpdateAllFromJson(Recipe::class.java, body)
                        Log.d("API", "Realm updated.")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
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

fun isConnectedToInternet(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
    val isConnected: Boolean = activeNetwork?.isConnected == false
    return isConnected
}

fun updateRemoteRecipes(activity: Activity, queue: RequestQueue, recipes: RealmList<Recipe>) {

    Log.d("API", "Updating recipes: ${recipes.toString()}")

    var recipeSet = JSONArray()
    val token = getToken(activity.getApplicationContext())

    recipes.forEach {

        Log.d("API", "Recipe: ${it.toString()}")
        Log.d("API", "id: ${it._id}")

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
        val realmID = it.realmID

        val jsonRecipe = JSONObject().apply({
            put("ingredients", ing)
            put("directions", dir)
            put("title", title)
            put("_id", _id)
            put("realmID", realmID)
            put("updatedAt", updatedAt)
        })

        recipeSet.put(jsonRecipe)
    }

    Log.d("API", "Update json: ${recipeSet.toString()}")

    val recipePutRequest = object: JsonRequest<UpdateResponse>(Request.Method.PUT, ENDPOINT4, recipeSet.toString(),
            Response.Listener<UpdateResponse> { response ->
                Log.d("API", "Update successful.")
                Log.d("API", "${response.toString()}")
            },
            Response.ErrorListener {
                Log.d("API", "Error: ${it.toString()}")
            }
    ){
        private val gson = Gson()

        override fun getHeaders(): Map<String, String> {
            val headers = HashMap<String, String>()
            headers.put("Content-Type", "application/json")
            headers.put("x-access-token", token)
            return headers
        }
        override fun parseNetworkResponse(response: NetworkResponse?): Response<UpdateResponse> {
            return try {
                val json = String(
                        response?.data ?: ByteArray(0),
                        Charset.forName(HttpHeaderParser.parseCharset(response?.headers))
                )
                Log.d("API", "${json}")
                Response.success(
                        gson.fromJson(json, UpdateResponse::class.java),
                        HttpHeaderParser.parseCacheHeaders(response)
                )
            } catch (e: UnsupportedEncodingException) {
                Response.error(ParseError(e))
            } catch (e: JsonSyntaxException) {
                Response.error(ParseError(e))
            }
        }
    }

    //    val headers = HashMap<String, String>()
//    headers.put("Content-Type", "application/json")
//    headers.put("x-access-token", token)

//    val recipePutRequest = GsonRequest<UpdateResponse>(ENDPOINT4, recipeSet, UpdateResponse::class.java, headers,
//            Response.Listener<UpdateResponse> { response ->
//                Log.d("API", "Update successful.")
//                Log.d("API", "${response.toString()}")
////                val body = response.toString()
////                val gson = GsonBuilder().create()
////                val updateResponse = gson.fromJson(body, UpdateResponse::class.java)
////                Log.d("API", updateResponse.toString())
//            },
//            Response.ErrorListener {
//                Log.d("API", "Error: ${it.toString()}")
//            }
//    )

    // Add the request to the RequestQueue.
    queue.add(recipePutRequest)
}

fun createUser(activity: Activity, queue: RequestQueue, username: String, email: String, password: String, callback: (success: Boolean) -> Unit) {

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

                downloadToken(activity, queue, email, password, {
                    Log.d("API", "Logging in.")
                    callback(true)
                })
            },
            Response.ErrorListener { error ->
                Log.d("API", "There was an error creating a user.")
                callback(false)
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
                Log.d("API", "error ${error.toString()}:")
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

class GsonRequest<T>(
        url: String,
        body: JSONArray,
        private val clazz: Class<T>,
        private val headers: MutableMap<String, String>?,
        private val listener: Response.Listener<T>,
        errorListener: Response.ErrorListener
) : Request<T>(Method.PUT, url, errorListener) {
    private val gson = Gson()

    override fun getHeaders(): MutableMap<String, String> = headers ?: super.getHeaders()

    override fun deliverResponse(response: T) = listener.onResponse(response)

    override fun parseNetworkResponse(response: NetworkResponse?): Response<T> {
        return try {
            val json = String(
                    response?.data ?: ByteArray(0),
                    Charset.forName(HttpHeaderParser.parseCharset(response?.headers)))
            Response.success(
                    gson.fromJson(json, clazz),
                    HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: UnsupportedEncodingException) {
            Response.error(ParseError(e))
        } catch (e: JsonSyntaxException) {
            Response.error(ParseError(e))
        }
    }
}

class UserResponse(val message: String, val data: User)

class UpdateResponse(val ok: Int)

//        {
//            "ok":1,
//            "writeErrors":[],
//            "writeConcernErrors":[],
//            "insertedIds":[],
//            "nInserted":0,
//            "nUpserted":0,
//            "nMatched":1,
//            "nModified":1,
//            "nRemoved":0,
//            "upserted":[],
//            "lastOp":{
//              "ts":"6630114502002081793",
//              "t":2
//            }
//        }

class RecipeResponse(val message: String)

class User(val email: String, val name: String, val _id: String)

class TokenResponse(val success: Boolean, val message: String, val token: String)