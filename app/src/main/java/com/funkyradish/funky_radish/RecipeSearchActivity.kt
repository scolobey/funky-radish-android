package com.funkyradish.funky_radish

import android.os.Bundle
import android.content.Intent
import android.view.*
import kotlinx.android.synthetic.main.activity_recipe_search.*
import android.text.InputType
import android.widget.EditText
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.toolbox.Volley
import io.realm.*
import io.realm.mongodb.sync.SyncConfiguration
import java.util.*

class RecipeSearchActivity : AppCompatActivity() {
    private lateinit var recipes: RealmResults<Recipe>
    private lateinit var filteredRecipes: RealmResults<Recipe>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_recipe_search)
        setSupportActionBar(findViewById(R.id.toolbar))
        prepareCreateRecipeButton()

        val user: io.realm.mongodb.User? = realmApp.currentUser()

        if (user != null && user.isLoggedIn!!) {
            val partitionValue: String = user.id
            val config = SyncConfiguration.Builder(user, partitionValue).build()
            realm = Realm.getInstance(config)
        }
        else {
            realm = Realm.getDefaultInstance()
        }

        recipes = realm.where(Recipe::class.java).findAll()
        filteredRecipes = recipes

        prepareRecipeListView(filteredRecipes)

        // TODO: check if this can be removed.
        var listener = RealmChangeListener<RealmResults<Recipe>>({
            prepareRecipeListView(filteredRecipes)
        })

        recipes.addChangeListener(listener)

//        if (isConnected(this.applicationContext)) {
//            // TODO: Should probably check if the user really wants to connect. What if offline is off and internet is connected?
//            Log.d("API", "Internet is connected it seems. toggling offline mode. is Offline labeled below.")
//            var state = isOffline(this)
//            Log.d("API", state.toString())
//            toggleOfflineMode(this.applicationContext)
//            state = isOffline(this)
//            Log.d("API", "offline toggled. is offline labeled below.")
//            Log.d("API", state.toString())
//        }

        // If offline mode is toggled on, don't try to download recipes.
//        if(!isOffline(this.applicationContext)) {
//
//            //TODO: check if user is synched.
//            Log.d("API", "Network access approved. Looking for a token.")
//
//            var token = getToken(this.getApplicationContext())
//
//            if (token.length > 0) {
//                Log.d("API", "Token found.")
//                val progressBar: ProgressBar = this.recipeListSpinner
//
//                Thread(Runnable {
//                    this@RecipeSearchActivity.runOnUiThread(java.lang.Runnable {
//                        progressBar.visibility = View.VISIBLE
//                    })
//
//                    try {
//                        //TODO: get rid of the variable
//                        val queue = Volley.newRequestQueue(this)
//                    } catch (e: InterruptedException) {
//                        e.printStackTrace()
//                        val intent = Intent(this, LoginActivity::class.java)
//                        startActivity(intent)
//                    }
//
//                    this@RecipeSearchActivity.runOnUiThread(java.lang.Runnable {
//                        Log.d("API", "Stopping loader.")
//                        // Set up recipes. Is device already logged in? Are there recipes on the device?
//                        prepareRecipeListView(filteredRecipes)
//                        progressBar.visibility = View.INVISIBLE
//                    })
//                }).start()
//            }
//            else {
//                Log.d("API", "Did not find a token.")
//
//                // if you're  intentionally in offline mode, don't do this.
//                showAuthorizationDialog()
//            }
//        }
//        else {
//            Toast.makeText(applicationContext,"Synchronization disabled.", Toast.LENGTH_SHORT).show()
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    fun prepareCreateRecipeButton() {
        createRecipeButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("What would you like to call this recipe?")
            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
            builder.setView(input)

            builder.setPositiveButton("OK") { dialog, which ->
                // Create a recipe with a given title. Pass the recipe's _id to the editor.
                realm.executeTransaction { realm ->
                    val newRecipe = realm.createObject(Recipe::class.java, UUID.randomUUID().toString())
                    newRecipe.title = input.getText().toString()

                    var auth = realmApp.currentUser()

                    if (auth != null) {
                        newRecipe.author = auth.id
                    }

                    Log.d("API", "Recipe: ${newRecipe.toString()}")

                    //TODO: why is this inside the realm transaction?
                    val intent = Intent(this, RecipeViewActivity::class.java)
                    Log.d("API", "accessing recipeId: ${newRecipe._id}")
                    intent.putExtra("rid", newRecipe._id)
                    intent.putExtra("direction", true)
                    startActivity(intent)
                }
            }
            builder.setNegativeButton("Cancel") {
                dialog, which -> dialog.cancel()
            }

            builder.show()
        }
    }

    fun showAuthorizationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("How would you like to get started?")
        val array = arrayOf("Login", "Signup", "Continue offline")

        builder.setItems(array) {_, which ->
            val selected = array[which]
            when (selected) {
                "Login" -> {
                    val intent = Intent(this, LoginActivity::class.java).apply {}
                    startActivity(intent)
                }
                "Signup" -> {
                    val intent = Intent(this, SignupActivity::class.java).apply {}
                    startActivity(intent)
                }
                else -> {
                    toggleOfflineMode(this.getApplicationContext())
                }
            }
        }

        val dialog = builder.create()
        dialog.show()
    }


    fun prepareRecipeListView(recipes: RealmResults<Recipe>) {
        recipe_list_recycler_view.layoutManager = LinearLayoutManager(this)
        recipe_list_recycler_view.adapter = RecipeListAdapter(recipes, this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater

        var token = getToken(this.getApplicationContext())
        var userEmail = getUserEmail(this.getApplicationContext())


        if (token.length > 0) {
            menu.add(1, 1, 1, userEmail)
            menu.add(1, 2, 1, "Logout")
        } else {
            menu.add(2, 3, 2, "Login")
            menu.add(2, 4, 2, "Signup")
        }

        if (isOffline(this.applicationContext)) {
            menu.add(3, 5, 3, "Toggle Online")
        }
        else {
            menu.add(3, 5, 3, "Toggle Offline")
        }

        inflater.inflate(R.menu.menu, menu)

        val searchField = menu.findItem(R.id.search_field)

        if(searchField != null) {
            val searchView = searchField.actionView as SearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText!!.isNotEmpty()) {
                        val searchQuery = newText.toLowerCase()
                        println(searchQuery)
                        filteredRecipes = realm.where(Recipe::class.java).contains("title", searchQuery, Case.INSENSITIVE).findAll()
                        prepareRecipeListView(filteredRecipes)
                    }
                    else {
                        filteredRecipes = recipes
                        prepareRecipeListView(filteredRecipes)
                    }
                    return true
                }
            })
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // Login
            3 -> {
                val intent = Intent(this, LoginActivity::class.java).apply {
                }
                startActivity(intent)
                return true
            }
            // Logout
            2 -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("This may delete recipes that have not been saved to your online account. Continue?")

                builder.setPositiveButton("YES"){dialog, which ->

                    //Todo: put RealmService up top
                    RealmService().logout()

                    setToken(this.getApplicationContext(), "")
                    setUsername(this.getApplicationContext(), "")
                    setUserEmail(this.getApplicationContext(), "")

                    //TODO: I can either return recipes from the function or just set the recipes to an empty list to avoid calling Realm again.
                    realm = Realm.getDefaultInstance()
                    recipes = realm.where(Recipe::class.java).findAll()
                    prepareRecipeListView(recipes)

                    toolbar.menu.removeGroup(1)
                    toolbar.menu.add(2, 3, 2, "Login")
                    toolbar.menu.add(2, 4, 2, "Signup")
                }

                builder.setNegativeButton("No"){dialog,which ->
                    Toast.makeText(applicationContext,"Logout cancelled.", Toast.LENGTH_SHORT).show()
                }

                val dialog = builder.create()
                dialog.show()

                return true
            }
            // Signup
            4 -> {
                val intent = Intent(this, SignupActivity::class.java).apply {
                }
                startActivity(intent)

                return true
            }
            5 -> {
                toolbar.menu.removeGroup(3)

                toggleOfflineMode(this.getApplicationContext())

                if (isOffline(this.applicationContext)) {
                    toolbar.menu.add(3, 5, 3, "Toggle Online")
                }
                else {
                    toolbar.menu.add(3, 5, 3, "Toggle Offline")
                }
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
