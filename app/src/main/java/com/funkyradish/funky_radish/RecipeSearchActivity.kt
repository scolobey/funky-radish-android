package com.funkyradish.funky_radish

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.content.Intent
import android.view.*
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import com.android.volley.toolbox.Volley
import io.realm.Realm

class RecipeSearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_recipe_search)
        setSupportActionBar(findViewById(R.id.toolbar))

        //Load recipes from realm and place them in a list
        val listView = findViewById<ListView>(R.id.recipe_listview)

        listView.adapter = RecipeListAdapter(this)

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
            R.id.action_reload -> {
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private class RecipeListAdapter(context: Context): BaseAdapter() {
        private val mContext: Context

        val realm = Realm.getDefaultInstance()
        private val recipes = realm.where(Recipe::class.java).findAll()

//        private val recipes = arrayListOf<String>("boners", "doobies", "titties")

        init {
            mContext = context
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItem(position: Int): Any {
            return "test string"
        }

        override fun getCount(): Int {
            return recipes.size
        }

        override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
            val layoutInflater = LayoutInflater.from(mContext)
            val recipeRow = layoutInflater.inflate(R.layout.recipe_row, viewGroup, false)

            val recipeTitleView = recipeRow.findViewById<TextView>(R.id.recipeTitle)
            recipeTitleView.text = recipes.get(position)!!.title

            val recipeIndexView = recipeRow.findViewById<TextView>(R.id.recipeIndex)
            recipeIndexView.text = "index is: $position"

            return recipeRow
        }
    }
}
