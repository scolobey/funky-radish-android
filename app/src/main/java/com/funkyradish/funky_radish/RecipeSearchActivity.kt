package com.funkyradish.funky_radish

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.content.Intent
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.android.volley.toolbox.Volley
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_recipe_search.*

class RecipeSearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_search)
        setSupportActionBar(findViewById(R.id.toolbar))

        prepareRecipeListView()
        prepareCreateRecipeButton()

        var token = checkForToken(this.getApplicationContext())

        if (token.length > 0 && !isOffline(this.applicationContext)) {
            val queue = Volley.newRequestQueue(this)
            loadRecipes(this, queue, token)
        }
        else {
            showAuthorizationDialog()
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

    fun prepareCreateRecipeButton() {
        createRecipeButton.setOnClickListener {
            val intent = Intent(this, RecipeViewActivity::class.java)
            intent.putExtra("rid", "")
            startActivity(intent)
        }
    }

    fun prepareRecipeListView() {
        recipe_list_recycler_view.layoutManager = LinearLayoutManager(this)
        recipe_list_recycler_view.adapter = RecipeListAdapter()
    }

    // Add the options menu to the toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
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

                //remove realm files
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
