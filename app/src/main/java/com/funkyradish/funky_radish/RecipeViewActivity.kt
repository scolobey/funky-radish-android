package com.funkyradish.funky_radish

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_recipe_view.*
import com.android.volley.toolbox.Volley
import io.realm.RealmList


class RecipeViewActivity : AppCompatActivity() {

    var directionView: Boolean = true
    var ingredientArray = listOf<String>()
    var directionArray = listOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_view)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val saveButton = findViewById(R.id.action_save) as Button

        var recipeModel = RecipeModel()
        val realm = Realm.getDefaultInstance()
        val recipeID: String = intent.getStringExtra("rid")
        var recipe = Recipe()

        if(recipeID.length > 0) {
            recipe = recipeModel.getRecipe(realm, recipeID)
        }
        else {
            try {
                realm.beginTransaction()
                recipe = realm.createObject(Recipe::class.java)
                realm.commitTransaction()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        prepareRecipeView(recipe)

        recipeViewSwitch.setOnClickListener {
            directionView = !directionView
            prepareRecipeView(recipe)
        }

        saveButton.setOnClickListener {

            val realm = Realm.getDefaultInstance()
            val queue = Volley.newRequestQueue(this)

            if(directionView) {
                // convert text to realmList and set recipe.directions
                directionArray = recipeViewContent.text.split("\n")
                var recipeRealmList = RealmList<String>()

                for (i in directionArray) {
                    recipeRealmList.add(i)
                }

                realm.executeTransaction { realm ->
                    try {
                        recipe.directions = recipeRealmList
                        saveRecipe(this, queue, recipe.title, ingredientArray, directionArray)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            else {
                // convert text to realmList and set recipe.ingredients
                ingredientArray = recipeViewContent.text.split("\n")
                var recipeRealmList = RealmList<String>()

                for (i in ingredientArray) {
                    recipeRealmList.add(i)
                }

                realm.executeTransaction { realm ->
                    try {
                        recipe.ingredients = recipeRealmList
                        saveRecipe(this, queue, recipe.title, ingredientArray, directionArray)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun prepareRecipeView(recipe: Recipe) {

        if(directionView) {
            recipeViewTitle.text = "Directions"

            val dirs = recipe.directions
            val dirString = StringBuilder()

            for (i in 0 until dirs!!.size) {
                println(dirs!![i])
                dirString.append(dirs!![i]).append("\n")
            }

            val finalIngredientString = dirString.toString()
            recipeViewContent.setText(finalIngredientString)
        }
        else {
            recipeViewTitle.text = "Ingredients"

            val ings = recipe.ingredients
            val ingString = StringBuilder()

            for (i in 0 until ings!!.size) {
                println(ings!![i])
                ingString.append(ings!![i]).append("\n")
            }

            val finalIngredientString = ingString.toString()
            recipeViewContent.setText(finalIngredientString)
        }

    }
}
