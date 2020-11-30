package com.funkyradish.funky_radish

import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.bson.types.ObjectId

@RealmClass
open class Direction : RealmObject() {
    @PrimaryKey
    @Index open var _id: String? = null
    open var author: String = ""

    open var text: String = ""
}