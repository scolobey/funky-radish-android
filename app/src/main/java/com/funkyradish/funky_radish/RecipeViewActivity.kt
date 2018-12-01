package com.funkyradish.funky_radish

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_recipe_view.*
import io.realm.RealmList
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class RecipeViewActivity : AppCompatActivity() {

    var directionView: Boolean = true

    val realm = Realm.getDefaultInstance()
    var recipe = Recipe()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_view)

        loadRecipe()
        prepareToolbar()

        prepareRecipeView()
        prepareTrashButton()
        prepareTitleEditButton()

        recipeViewSwitch.setOnClickListener {
            directionView = !directionView
            prepareRecipeView()
        }
    }

    private fun loadRecipe() {
        // Load recipe if an id is provided. Create a new recipe if id is not provided.
        val recipeID: String = intent.getStringExtra("rid")
//        val recipeTitle: String = intent.getStringExtra("recipe_title")

        recipe = realm.where(Recipe::class.java).equalTo("realmID", recipeID).findFirst()!!

        Log.d("API", "displaying: ${recipe.toString()} ")

//        if(recipeID.isNotEmpty()) {
//
//        }
//        else {
//            recipe.title = recipeTitle
//
//            try {
//                realm.beginTransaction()
//                realm.copyToRealmOrUpdate(recipe)
//                realm.commitTransaction()
//            } catch (e: Exception) {
//                println(e)
//            }
//
//            // if the app is online, we should save the recipe right away?
//            //        val queue = Volley.newRequestQueue(this)
//            //        saveRecipe(this, queue, recipeTitle, ingredientArray, directionArray)
//        }
//        realm.close()
    }

    private fun prepareToolbar() {
        setSupportActionBar(toolbar)
        getSupportActionBar()!!.setTitle(recipe.title);
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        prepareSaveButton()
    }

    private fun prepareSaveButton() {
        val saveButton = findViewById<Button>(R.id.action_save)

        saveButton.setOnClickListener {

            val currentTime = Calendar.getInstance()
            val formattedDate = SimpleDateFormat("EE MMM dd yyyy HH:mm:ss zZ").format(currentTime.time)

            Log.d("API", "saving ${formattedDate}, _id: ${recipe.toString()} ")

            if(directionView) {
                // convert text to realmList and set recipe.directions
                // TODO: clear whitespace
                var directionArray = recipeViewContent.text.replace("(?m)\\s*$".toRegex(), "").split("\n")
                var recipeDirectionRealmList = RealmList<String>()

                for (i in directionArray) {
                    recipeDirectionRealmList.add(i)
                }

                //  TODO: saveRecipe(this, queue, recipe.title, ingredientArray, directionArray)
                realm.executeTransaction { _ ->
                    try {
                        recipe.directions = recipeDirectionRealmList
                        recipe.updatedAt = formattedDate + " (UTC)"
                        Log.d("API", "such success. ${recipeDirectionRealmList}, _id: ${recipe._id} ")
                    } catch (e: Exception) {
                        Log.d("API", "such failure.")
                        e.printStackTrace()
                    }
                }
            }
            else {
                // convert text to realmList and set recipe.ingredients
                var ingredientArray = recipeViewContent.text.replace("(?m)\\s*$".toRegex(), "").split("\n")
                var recipeIngredientRealmList = RealmList<String>()

                for (i in ingredientArray) {
                    recipeIngredientRealmList.add(i)
                }

                realm.executeTransaction { realm ->
                    try {
                        recipe.ingredients = recipeIngredientRealmList
                        recipe.updatedAt = formattedDate + " (UTC)"

                        Log.d("API", "such success. ${recipeIngredientRealmList}")
// TODO                       saveRecipe(this, queue, recipe.title, ingredientArray, directionArray)
                    } catch (e: Exception) {
                        Log.d("API", "such failure.")
                        e.printStackTrace()
                    }
                }
            }

            this.hideKeyboard()
        }
    }

    private fun prepareRecipeView() {
        if(directionView) {
            recipeViewTitle.text = "Directions"

            if(recipe.directions.size > 0) {
                val dirs = recipe.directions
                val dirString = StringBuilder()

                for (i in 0 until dirs!!.size) {
                    dirString.append(dirs!![i]).append("\n")
                }

                val finalIngredientString = dirString.toString()
                recipeViewContent.setText(finalIngredientString)
            }

            else {
                recipeViewContent.setText("")
            }

        }
        else {
            recipeViewTitle.text = "Ingredients"

            val ings = recipe.ingredients
            val ingString = StringBuilder()

            for (i in 0 until ings!!.size) {
                ingString.append(ings!![i]).append("\n")
            }

            val finalIngredientString = ingString.toString()
            recipeViewContent.setText(finalIngredientString)
        }
    }

    private fun prepareTrashButton() {
        trashButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Are you sure you want to permanently delete this recipe?")

            builder.setPositiveButton("YES"){dialog, which ->
                realm.executeTransaction {
                    recipe.deleteFromRealm()
                    val intent = Intent(this, RecipeSearchActivity::class.java).apply {}
                    startActivity(intent)
                    Toast.makeText(applicationContext,"Recipe deleted.",Toast.LENGTH_SHORT).show()
                }
            }

            builder.setNegativeButton("No"){dialog,which ->
                Toast.makeText(applicationContext,"Cancelled delete.",Toast.LENGTH_SHORT).show()
            }

            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun prepareTitleEditButton() {
        editTitleButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("What would you like to call this recipe?")

            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
            input.setText(recipe.title)
            builder.setView(input)

            builder.setPositiveButton("OK") { dialog, which ->
                realm.executeTransaction { realm ->
                    // change the recipe's title
                    //TODO: more efficient regex
                    recipe.title = input.getText().toString().replace("^\\s*".toRegex(), "").replace("\\s*$".toRegex(), "")
                }

                finish()
                startActivity(getIntent())
            }
            builder.setNegativeButton("Cancel") {
                dialog, which -> dialog.cancel()
            }

            builder.show()
        }
    }

    fun Fragment.hideKeyboard() {
        activity!!.hideKeyboard(view!!)
    }

    fun Activity.hideKeyboard() {
        hideKeyboard(if (currentFocus == null) View(this) else currentFocus)
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
