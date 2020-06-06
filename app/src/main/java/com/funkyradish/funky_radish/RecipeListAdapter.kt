package com.funkyradish.funky_radish

import android.content.Context
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.realm.RealmResults
import kotlinx.android.synthetic.main.recipe_row.view.*

class RecipeListAdapter(val recipes: RealmResults<Recipe>, val context: Context): RecyclerView.Adapter<RecipeViewHolder>() {

    override fun getItemCount(): Int {
        return recipes.size
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecipeViewHolder {
        println(p0)
        val layoutInflater = LayoutInflater.from(p0.context)
        val recipeCell = layoutInflater.inflate(R.layout.recipe_row, p0, false)
        return RecipeViewHolder(recipeCell)
    }

    override fun onBindViewHolder(p0: RecipeViewHolder, p1: Int) {
        (p0).bind(recipes.get(p1)!!)
    }

}

class RecipeViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    fun bind(recipe: Recipe) {
        //TODO: Clean this up with a map
        val contentString = StringBuilder()
        val ings = recipe.ingredients

        for (i in 0 until ings!!.size) {
            contentString.append(ings!![i]!!.name).append("\n")
        }

        itemView.recipeViewTitle.text = recipe.title
        itemView.ingredientContainer.text = contentString

        itemView.setOnClickListener {
            val intent = Intent(itemView.context, RecipeViewActivity::class.java)
            intent.putExtra("rid", recipe.realmID)
            intent.putExtra("direction", true)
            itemView.context.startActivity(intent)
        }
    }

}