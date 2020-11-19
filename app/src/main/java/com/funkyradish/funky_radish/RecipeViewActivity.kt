package com.funkyradish.funky_radish

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_recipe_view.*
import io.realm.kotlin.createObject
import java.util.*

class RecipeViewActivity : AppCompatActivity() {

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

            val dir = intent.extras.getBoolean("direction")
            intent.putExtra("direction", !dir)

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

    private fun saveRecipe(title: String?) {

        val directionView = intent.extras.getBoolean("direction")
        var textBoxContents = recipeViewContent.text.replace("(?m)\\s*$".toRegex(), "").split("\n")

        realm.executeTransaction { _ ->
            try {
                recipe.title = title

                if(directionView) {
                    recipe.directions.deleteAllFromRealm()

                    for (i in textBoxContents) {
                        val dir = realm.createObject<Direction>(UUID.randomUUID().toString())
                        dir.text = i
                        recipe.directions.add(dir)
                    }
                }
                else {
                    recipe.ingredients.deleteAllFromRealm()

                    for (i in textBoxContents) {
                        val ing = realm.createObject<Ingredient>(UUID.randomUUID().toString())
                        ing.name = i
                        recipe.ingredients.add(ing)
                    }
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

        val directionView = intent.extras.getBoolean("direction")

        if(directionView) {
            recipeViewTitle.text = "Directions"

            val finalContentString = buildContentString(true)
            recipeViewContent.setText(finalContentString)
        }
        else {
            recipeViewTitle.text = "Ingredients"

            val finalContentString = buildContentString(false)
            recipeViewContent.setText(finalContentString)
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
                val title = input.getText().toString().replace("^\\s*".toRegex(), "").replace("\\s*$".toRegex(), "")

                saveRecipe(title)

                finish()

                var intent = getIntent()
                startActivity(intent)
            }
            builder.setNegativeButton("Cancel") {
                dialog, which -> dialog.cancel()
            }

            builder.show()
        }
    }

    // Override Home action.
    override fun onSupportNavigateUp(): Boolean {
        saveRecipe(recipe.title)
        finish()
        return true
    }

    private fun buildContentString(isDirectionView: Boolean): String{
        val contentString = StringBuilder()

        if(isDirectionView) {
            val dirs = recipe.directions

            for (i in 0 until dirs!!.size) {
                contentString.append(dirs!![i]!!.text).append("\n")
            }
        }
        else {
            val ings = recipe.ingredients

            for (i in 0 until ings!!.size) {
                contentString.append(ings!![i]!!.name).append("\n")
            }
        }

        return contentString.toString()
    }

    private fun Activity.hideKeyboard() {
        hideKeyboard(if (currentFocus == null) View(this) else currentFocus)
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
