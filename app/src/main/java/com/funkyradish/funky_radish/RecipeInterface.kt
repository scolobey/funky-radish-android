package com.funkyradish.funky_radish

import io.realm.Realm

interface RecipeInterface {
    fun addRecipe(realm: Realm, recipe: Recipe): Boolean
    fun deleteRecipe(realm: Realm, _id: String): Boolean
    fun editRecipe(realm: Realm, recipe: Recipe): Boolean
    fun getRecipe(realm: Realm, _id: String): Recipe
    fun removeRecipes(realm: Realm): Boolean
}