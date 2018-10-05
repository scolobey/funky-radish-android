package com.funkyradish.funky_radish

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.content.Intent
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import android.widget.BaseAdapter
import android.widget.TextView
import com.android.volley.toolbox.Volley
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_recipe_search.*

class RecipeSearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_recipe_search)
        setSupportActionBar(findViewById(R.id.toolbar))

        recipe_list_recycler_view.layoutManager =LinearLayoutManager(this)
        recipe_list_recycler_view.adapter = RecipeListAdapter()

        val FR_TOKEN = "fr_token"
        val preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext())
        var token = preferences.getString(FR_TOKEN, "")

        val queue = Volley.newRequestQueue(this)

        if (token.length > 0) {
            loadRecipes(this, queue, token)
        }
        else {

            val array = arrayOf("Login", "Signup", "Continue offline")

            // Initialize a new instance of alert dialog builder object
            val builder = AlertDialog.Builder(this)

            // Set a title for alert dialog
            builder.setTitle("Hi. How would you like to get started?")

            builder.setItems(array,{_, which ->
                // Get the dialog selected item
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
                        println("I dunno.")
                    }
                }
            })

            // Create a new AlertDialog using builder object
            val dialog = builder.create()

            // Finally, display the alert dialog
            dialog.show()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val FR_TOKEN = "fr_token"
        val preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext())

        when (item.itemId) {
            R.id.action_login -> {
                val intent = Intent(this, LoginActivity::class.java).apply {
                }
                startActivity(intent)
                return true
            }
            R.id.action_logout -> {
                val editor = preferences.edit()
                editor.putString(FR_TOKEN, "")
                editor.apply()

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
            R.id.action_reload -> {
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
