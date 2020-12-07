package com.funkyradish.funky_radish

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import io.realm.*
import io.realm.mongodb.sync.SyncConfiguration
import kotlinx.android.synthetic.main.activity_recipe_search.*
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
            builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel()
            }

            builder.show()
        }
    }

//    fun showAuthorizationDialog() {
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("How would you like to get started?")
//        val array = arrayOf("Login", "Signup", "Continue offline")
//
//        builder.setItems(array) {_, which ->
//            val selected = array[which]
//            when (selected) {
//                "Login" -> {
//                    val intent = Intent(this, LoginActivity::class.java).apply {}
//                    startActivity(intent)
//                }
//                "Signup" -> {
//                    val intent = Intent(this, SignupActivity::class.java).apply {}
//                    startActivity(intent)
//                }
//                else -> {
//                    toggleOfflineMode(this.getApplicationContext())
//                }
//            }
//        }
//
//        val dialog = builder.create()
//        dialog.show()
//    }


    fun prepareRecipeListView(recipes: RealmResults<Recipe>) {
        recipe_list_recycler_view.layoutManager = LinearLayoutManager(this)
        recipe_list_recycler_view.adapter = RecipeListAdapter(recipes, this)
    }

    fun logout() {
        val progressSpinner: ProgressBar = this.recipeListSpinner
        progressSpinner.visibility = View.VISIBLE


        try {
            RealmService().logout() { success: Boolean ->
                if (success) {
                    //TODO: Do we need th UIthread runner?
                    this@RecipeSearchActivity.runOnUiThread(java.lang.Runnable {
                        setToken(this.getApplicationContext(), "")
                        setUsername(this.getApplicationContext(), "")
                        setUserEmail(this.getApplicationContext(), "")

                        realm = Realm.getDefaultInstance()
                        recipes = realm.where(Recipe::class.java).findAll()
                        prepareRecipeListView(recipes)

                        toolbar.menu.removeGroup(1)
                        toolbar.menu.add(2, 3, 1, "Login")
                        toolbar.menu.add(2, 4, 2, "Signup")

                        progressSpinner.visibility = View.INVISIBLE
                    })
                } else {
                    this@RecipeSearchActivity.runOnUiThread(java.lang.Runnable {
                        progressSpinner.visibility = View.INVISIBLE
                    })
                }
            }
        } catch (e: InterruptedException) {
            //TODO: This catch can probably be removed.
            Log.d("API", "Some kinda error.")
            this@RecipeSearchActivity.runOnUiThread(java.lang.Runnable {
                progressSpinner.visibility = View.INVISIBLE
            })
            e.printStackTrace()
        }
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
                    } else {
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

                if (recipes.count() > 0) {
                    builder.setTitle("This may delete recipes that have not been saved to your online account. Continue?")

                    builder.setPositiveButton("YES") { dialog, which ->
                        logout()
                    }

                    builder.setNegativeButton("No") { dialog, which ->
                        Toast.makeText(applicationContext, "Logout cancelled.", Toast.LENGTH_SHORT).show()
                    }

                    val dialog = builder.create()
                    dialog.show()
                } else {
                    logout()
                }

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
                } else {
                    toolbar.menu.add(3, 5, 3, "Toggle Offline")
                }
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
