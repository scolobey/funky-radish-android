package com.funkyradish.funky_radish

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_recipe_view.*
import kotlinx.android.synthetic.main.recipe_row.view.*


class RecipeListAdapter: RecyclerView.Adapter<RecipeViewHolder>() {

    var recipeModel = RecipeModel()
    val realm = Realm.getDefaultInstance()

    val recipes = recipeModel.getRecipes(realm)


    override fun getItemCount(): Int {
        return recipes.size
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecipeViewHolder {
        val layoutInflater = LayoutInflater.from(p0.context)
        val recipeCell = layoutInflater.inflate(R.layout.recipe_row, p0, false)
        return RecipeViewHolder(recipeCell, recipes.get(p1)!!)
    }

    override fun onBindViewHolder(p0: RecipeViewHolder, p1: Int) {
        p0.view.recipeViewTitle.text = p0.recipe!!.title

        val ings = p0.recipe!!.ingredients!!
        val ingString = StringBuilder()

        for (i in 0 until ings!!.size) {
            println(ings!![i])
            ingString.append(ings!![i]).append("\n")

            // need to trim
        }

        val finalIngredientString = ingString.toString()
        p0.view.ingredientContainer.text = finalIngredientString
    }

}

class RecipeViewHolder(val view: View, val recipe: Recipe): RecyclerView.ViewHolder(view) {

    init {
        view.setOnClickListener {
            println(recipe.toString())
            val intent = Intent(view.context, RecipeViewActivity::class.java)
            intent.putExtra("rid", recipe._id)
            view.context.startActivity(intent)
        }
    }

}