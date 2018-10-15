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

}