package com.funkyradish.funky_radish

import android.util.Log
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.RealmClass
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import java.lang.reflect.Type

@RealmClass
open class Recipe : RealmObject() {

    open var title: String? = null

    open var _id: String? = null

    open var updatedAt: String? = null

    open var ingredients: RealmList<String>? = null

    open var directions: RealmList<String>? = null

    // Standard getters and setters

//    fun getTitle(): String? {
//        return title
//    }
//
//    fun setTitle(title: String) {
//        this.title = title
//    }
//
//    fun getId(): String? {
//        return _id
//    }
//
//    fun setId(id: String) {
//        this._id = id
//    }
//
//    fun getUpdatedAt(): String? {
//        return updatedAt
//    }
//
//    fun setUpdatedAt(updatedAt: String) {
//        this.updatedAt = updatedAt
//    }
//
//    fun getIngredients(): RealmList<Ingredient>? {
//        return ingredients
//    }
//
//    fun setIngredients(ingredients: RealmList<Ingredient>) {
//        this.ingredients = ingredients
//    }

}

//open class RealmStringDeserializer : JsonDeserializer<RealmList<Ingredient>> {
//
//    @Throws(JsonParseException::class)
//    override fun deserialize(json: JsonElement, typeOfT: Type,
//                             context: JsonDeserializationContext): RealmList<Ingredient> {
//
//
//        Log.d("This", "time its for real")
//        val realmIngredients = RealmList<Ingredient>()
//        val ingredientList = json.asJsonArray
//
//        for (ingElement in ingredientList) {
//            var ing = Ingredient()
//            ing.name = ingElement.asString
//            realmIngredients.add(ing)
//        }
//
//        return realmIngredients
//    }
//}

