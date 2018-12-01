package com.funkyradish.funky_radish

import android.util.Log
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.RealmClass
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import io.realm.annotations.PrimaryKey
import java.lang.reflect.Type

@RealmClass
open class Recipe : RealmObject() {

    @PrimaryKey
    open var realmID: String = ""

    open var _id: String = ""

    open var title: String = ""

    open var updatedAt: String = ""

    open var ingredients: RealmList<String> = RealmList<String>()

    open var directions: RealmList<String> = RealmList<String>()

}