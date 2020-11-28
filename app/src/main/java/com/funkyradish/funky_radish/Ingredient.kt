package com.funkyradish.funky_radish

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.bson.types.ObjectId

@RealmClass
open class Ingredient : RealmObject() {
    @PrimaryKey
    open var _id: ObjectId = ObjectId()
    open var author: String = "recipes"
    open var name: String? = ""
}

