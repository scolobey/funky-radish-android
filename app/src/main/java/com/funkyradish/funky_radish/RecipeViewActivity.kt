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

            Log.d("API", "recipe view switch")

            saveRecipe(recipe.title)

            directionView = !directionView

            loadRecipe()
            prepareRecipeView()
        }
    }

    private fun loadRecipe() {
        // Load recipe if an id is provided. Create a new recipe if id is not provided.
        val recipeID: String = intent.getStringExtra("rid")
//        val recipeTitle: String = intent.getStringExtra("recipe_title")

        recipe = realm.where(Recipe::class.java).equalTo("realmID", recipeID).findFirst()!!

        Log.d("API", "displaying: ${recipe.toString()} ")

    }

    private fun prepareToolbar() {
        setSupportActionBar(toolbar)
        getSupportActionBar()!!.setTitle(recipe.title);
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        prepareSaveButton()
    }

    private fun saveRecipe(title: String) {
        Log.d("API", "Saving recipe: ${recipe.toString()}")

        var recipeDirectionRealmList = RealmList<String>()
        var recipeIngredientRealmList = RealmList<String>()
        var textBoxContents = recipeViewContent.text.replace("(?m)\\s*$".toRegex(), "").split("\n")

        if(directionView) {
            for (i in textBoxContents) {
                recipeDirectionRealmList.add(i)
            }

            for (i in recipe.ingredients) {
                recipeIngredientRealmList.add(i)
            }
        }
        else {

            for (i in recipe.directions) {
                recipeDirectionRealmList.add(i)
            }

            for (i in textBoxContents) {
                recipeIngredientRealmList.add(i)
            }
        }

        Log.d("API", "saving: ${title}")

        realm.executeTransaction { _ ->
            try {
                recipe.title = title
                recipe.directions = recipeDirectionRealmList
                recipe.ingredients = recipeIngredientRealmList
            } catch (e: Exception) {
                Log.d("API", "such failure.")
                e.printStackTrace()
            }
        }

    }

    private fun prepareSaveButton() {
        val saveButton = findViewById<Button>(R.id.action_save)

        saveButton.setOnClickListener {
            saveRecipe(recipe.title)
            loadRecipe()
            this.hideKeyboard()
        }
    }

    private fun prepareRecipeView() {

        Log.d("API", "Preparing recipe view. _id: ${recipe.realmID}")

        if(directionView) {
            Log.d("API", "direction view: ${recipe.directions}")
            recipeViewTitle.text = "Directions"

            if(recipe.directions.size > 0) {
                val dirs = recipe.directions
                val dirString = StringBuilder()

                for (i in 0 until dirs!!.size) {
                    dirString.append(dirs!![i]).append("\n")
                }

                val finalDirectionString = dirString.toString()

                Log.d("API", "setting directions: ${finalDirectionString}")

                recipeViewContent.setText(finalDirectionString)
            }

            else {
                recipeViewContent.setText("")
            }

        }
        else {
            Log.d("API", "ingredient view: ${recipe.ingredients}")
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

            Log.d("API", "changing title: ${title}")

            builder.setPositiveButton("OK") { dialog, which ->
                val title = input.getText().toString().replace("^\\s*".toRegex(), "").replace("\\s*$".toRegex(), "")
                saveRecipe(title)
                finish()
                startActivity(getIntent())
            }
            builder.setNegativeButton("Cancel") {
                dialog, which -> dialog.cancel()
            }

            builder.show()
        }
    }

//    fun Fragment.hideKeyboard() {
//        activity!!.hideKeyboard(view!!)
//    }

    private fun Activity.hideKeyboard() {
        hideKeyboard(if (currentFocus == null) View(this) else currentFocus)
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
