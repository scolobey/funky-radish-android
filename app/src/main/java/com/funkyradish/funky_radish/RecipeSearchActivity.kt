package com.funkyradish.funky_radish

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.content.Intent
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_recipe_search.*
import android.text.InputType
import android.widget.EditText
import android.support.v7.widget.SearchView
import com.android.volley.toolbox.Volley
import io.realm.Case
import io.realm.RealmResults
import java.util.*

class RecipeSearchActivity : AppCompatActivity() {
    private lateinit var realm: Realm
    private lateinit var recipes: RealmResults<Recipe>
    private lateinit var filteredRecipes: RealmResults<Recipe>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Assemble the view
        setContentView(R.layout.activity_recipe_search)
        setSupportActionBar(findViewById(R.id.toolbar))

        realm = Realm.getDefaultInstance()
        // TODO: may be more performant to pass the search query here and let realm do the quick filtering work
        // instead of maintaining a filtered list.
        recipes = realm.where(Recipe::class.java).findAll()
        filteredRecipes = recipes

        prepareRecipeListView(filteredRecipes)
        prepareCreateRecipeButton()

        if(isOffline(this.applicationContext)) {

        }
        else {
            var token = getToken(this.getApplicationContext())

            if (token.length > 0) {
                val queue = Volley.newRequestQueue(this)
                loadRecipes(this, queue, token)
            }
            else {
                showAuthorizationDialog()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    fun prepareRecipeListView(recipes: RealmResults<Recipe>) {
        println(recipes.toString())
        recipe_list_recycler_view.layoutManager = LinearLayoutManager(this)
        recipe_list_recycler_view.adapter = RecipeListAdapter(recipes, this)
    }

    fun prepareCreateRecipeButton() {
        createRecipeButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("What would you like to call this recipe?")
            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
            builder.setView(input)

            builder.setPositiveButton("OK") { dialog, which ->

                realm.executeTransaction { realm ->
                    // Add a person
                    val newRecipe = realm.createObject(Recipe::class.java, UUID.randomUUID().toString())
                    newRecipe.title = input.getText().toString()

                    val intent = Intent(this, RecipeViewActivity::class.java)
                    intent.putExtra("rid", newRecipe._id)

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
        builder.setTitle("Hi. How would you like to get started?")
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

    // Add the options menu and actions
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
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
            R.id.action_login -> {
                val intent = Intent(this, LoginActivity::class.java).apply {
                }
                startActivity(intent)
                return true
            }
            R.id.action_logout -> {
                setToken(this.getApplicationContext(), "")

                var recipeModel = RecipeModel()
                val realm = Realm.getDefaultInstance()
                recipeModel.removeRecipes(realm)

                return true
            }
            R.id.action_signup -> {
                val intent = Intent(this, SignupActivity::class.java).apply {
                }
                startActivity(intent)

                return true
            }
            R.id.toggle_offline_mode -> {
                toggleOfflineMode(this.getApplicationContext())

                return true
            }
            R.id.action_reload -> {
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
