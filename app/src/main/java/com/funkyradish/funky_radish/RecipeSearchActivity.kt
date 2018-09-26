package com.funkyradish.funky_radish

import android.content.DialogInterface
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.widget.Toast
import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.android.volley.toolbox.Volley


class RecipeSearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_recipe_search)
        setSupportActionBar(findViewById(R.id.toolbar))
//        val FR_TOKEN = "fr_token"
//        val preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext())
//        var token = preferences.getString(FR_TOKEN, "")

        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhZG1pbiI6ZmFsc2UsInVzZXIiOiI1YmFiMDZiNjQ1YjY4ZDAwMTMxYzY3ZWYiLCJpYXQiOjE1Mzc5MzUwMzEsImV4cCI6MTUzODAyMTQzMX0.pN2oaymyjqX5cUubUh50phuETZKq5x7ZGVEHABeneHo"

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
                println("logout")
                val editor = preferences.edit()
                editor.putString(FR_TOKEN, "")
                editor.apply()
                return true
            }
            R.id.action_signup -> {
                val intent = Intent(this, SignupActivity::class.java).apply {
                }
                startActivity(intent)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
