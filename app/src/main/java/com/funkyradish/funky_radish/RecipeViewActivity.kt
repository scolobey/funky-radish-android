package com.funkyradish.funky_radish

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_recipe_view.*
import io.realm.RealmList

class RecipeViewActivity : AppCompatActivity() {

    var directionView: Boolean = true

    val realm = Realm.getDefaultInstance()
    var recipe = Recipe()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_view)

        println(recipe._id)

        loadRecipe()
        prepareToolbar()

        prepareRecipeView()

        trashButton.setOnClickListener {
            println(recipe._id)
            realm.executeTransaction {
                recipe.deleteFromRealm()
            }
        }

        recipeViewSwitch.setOnClickListener {
            directionView = !directionView
            prepareRecipeView()
        }
    }

    private fun loadRecipe() {
        // Load recipe if an id is provided. Create a new recipe if id is not provided.
        val recipeID: String = intent.getStringExtra("rid")
//        val recipeTitle: String = intent.getStringExtra("recipe_title")

        recipe = realm.where(Recipe::class.java).equalTo("_id", recipeID).findFirst()!!

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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        prepareSaveButton()
    }

    private fun prepareSaveButton() {
        val saveButton = findViewById<Button>(R.id.action_save)

        saveButton.setOnClickListener {

            if(directionView) {
                // convert text to realmList and set recipe.directions
                var directionArray = recipeViewContent.text.split("\n")
                var recipeDirectionRealmList = RealmList<String>()

                for (i in directionArray) {
                    recipeDirectionRealmList.add(i)
                }

                realm.executeTransaction { _ ->
                    try {
                        recipe.directions = recipeDirectionRealmList
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            else {
                // convert text to realmList and set recipe.ingredients
                var ingredientArray = recipeViewContent.text.split("\n")
                var recipeIngredientRealmList = RealmList<String>()

                for (i in ingredientArray) {
                    recipeIngredientRealmList.add(i)
                    //                        saveRecipe(this, queue, recipe.title, ingredientArray, directionArray)
                }

                realm.executeTransaction { realm ->
                    try {
                        recipe.ingredients = recipeIngredientRealmList
//                        saveRecipe(this, queue, recipe.title, ingredientArray, directionArray)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun prepareRecipeView() {
        if(directionView) {
            recipeViewTitle.text = "Directions"

            if(recipe.directions.size > 0) {
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
                recipeViewContent.setText("")
            }

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
