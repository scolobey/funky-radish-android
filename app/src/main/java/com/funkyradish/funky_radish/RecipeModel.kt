package com.funkyradish.funky_radish

import io.realm.Realm
import io.realm.RealmResults

class RecipeModel : RecipeInterface {

    override fun addRecipe(realm: Realm, recipe: Recipe): Boolean {
        try {
            realm.beginTransaction()

            if(recipe!=null) {
                realm.copyToRealmOrUpdate(recipe)
            }
            else {
                realm.copyToRealmOrUpdate(Recipe())
            }
            realm.commitTransaction()
            return true
        } catch (e: Exception) {
            println(e)
            return false
        }
    }

    override fun deleteRecipe(realm: Realm, _id: String): Boolean {
        try {
            realm.beginTransaction()
            realm.where(Recipe::class.java).equalTo("_id", _id).findFirst()?.deleteFromRealm()
            realm.commitTransaction()
            return true
        } catch (e: Exception) {
            println(e)
            return false
        }
    }

    override fun editRecipe(realm: Realm, recipe: Recipe): Boolean {
        try {
            realm.beginTransaction()
            realm.copyToRealm(recipe)
            realm.commitTransaction()
            return true
        } catch (e: Exception) {
            println(e)
            return false
        }
    }

    override fun getRecipe(realm: Realm, _id: String): Recipe {
        val recipe = realm.where(Recipe::class.java).equalTo("_id", _id).findFirst()
        return recipe!!
    }

//    fun getRecipes(realm: Realm): RealmResults<Recipe> {
//        return realm.where(Recipe::class.java).findAll()
//    }

    override fun removeRecipes(realm: Realm): Boolean {
        try {
            realm.beginTransaction()
            realm.where(Recipe::class.java).findAll().deleteAllFromRealm()
            realm.commitTransaction()
            return true
        } catch (e: Exception) {
            println(e)
            return false
        }
    }

}