package com.funkyradish.funky_radish

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
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
import io.realm.kotlin.createObject
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
            saveRecipe(recipe.title)
            directionView = !directionView
            loadRecipe()
            prepareRecipeView()
        }
    }

    private fun loadRecipe() {
        val recipeID: String = intent.getStringExtra("rid")
        recipe = realm.where(Recipe::class.java).equalTo("realmID", recipeID).findFirst()!!
    }

    private fun prepareToolbar() {
        setSupportActionBar(toolbar)
        getSupportActionBar()!!.setTitle(recipe.title);
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        prepareSaveButton()
    }

    private fun saveRecipe(title: String) {

//        var recipeDirectionRealmList = RealmList<Direction>()
//        var recipeIngredientRealmList = RealmList<Ingredient>()
        var textBoxContents = recipeViewContent.text.replace("(?m)\\s*$".toRegex(), "").split("\n")

//        if(directionView) {
//            Log.d("API", "Directions saving")
//
//            for (i in textBoxContents) {
//
//                realm.executeTransaction { _ ->
//                    try {
//                        val direction = realm.createObject(Direction::class.java)
//                        direction.text = i
//                        recipeDirectionRealmList.add(direction)
//
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    }
//                }
//
//
//            }
//
//            recipeIngredientRealmList = recipe.ingredients
//        }
//        else {
//
//            Log.d("API", "Ingredients saving")
//            recipeDirectionRealmList = recipe.directions
//
//            for (i in textBoxContents) {
//                var ingredient = Ingredient()
//                ingredient.name = i
//                recipeIngredientRealmList.add(ingredient)
//            }
//
//        }

        realm.executeTransaction { _ ->
            try {
                recipe.title = title

                for (i in textBoxContents) {
                    val direction = realm.createObject<Direction>(UUID.randomUUID().toString())
                    direction.text = "fido"
                    recipe.directions.add(direction)
                }


            } catch (e: Exception) {
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
        val contentString = StringBuilder()

        if(directionView) {
            recipeViewTitle.text = "Directions"

            val dirs = recipe.directions

            Log.d("API", "Directions: ${recipe.directions}")

            for (i in 0 until dirs!!.size) {
                contentString.append(dirs!![i]!!.text).append("\n")
            }
        }
        else {
            recipeViewTitle.text = "Ingredients"

            val ings = recipe.ingredients

            for (i in 0 until ings!!.size) {
                contentString.append(ings!![i]!!.name).append("\n")
            }
        }

        val finalContentString = contentString.toString()
        recipeViewContent.setText(finalContentString)
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

    private fun Activity.hideKeyboard() {
        hideKeyboard(if (currentFocus == null) View(this) else currentFocus)
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
