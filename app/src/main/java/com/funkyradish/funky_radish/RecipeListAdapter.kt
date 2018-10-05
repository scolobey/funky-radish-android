package com.funkyradish.funky_radish

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.realm.Realm
import kotlinx.android.synthetic.main.recipe_row.view.*

class RecipeListAdapter: RecyclerView.Adapter<RecipeViewHolder>() {

    var recipeModel = RecipeModel()
    val realm = Realm.getDefaultInstance()

    private val recipes = recipeModel.getRecipes(realm)

    override fun getItemCount(): Int {
        return recipes.size
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecipeViewHolder {
        val layoutInflater = LayoutInflater.from(p0.context)
        val recipeCell = layoutInflater.inflate(R.layout.recipe_row, p0, false)
        return RecipeViewHolder(recipeCell)
    }

    override fun onBindViewHolder(p0: RecipeViewHolder, p1: Int) {
        val recipe = recipes.get(p1)
        p0.view.recipeTitle.text = recipe!!.title
        p0.view.recipeIndex.text = p1.toString()
        p0.view.recipeId.text = recipe!!._id
        p0.view.recipeUpdatedAt.text = recipe!!.updatedAt
        p0.view.recipeIngredients.text = recipe!!.ingredients!!.first()
        p0.view.recipeDirections.text = recipe!!.directions!!.first()
    }
}

class RecipeViewHolder(val view: View): RecyclerView.ViewHolder(view) {

}