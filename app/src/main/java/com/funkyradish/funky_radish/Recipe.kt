package com.funkyradish.funky_radish

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

@RealmClass
open class Recipe(): RealmObject() {

    @PrimaryKey
    var title: String? = ""

    fun getName(): String? {
        return title
    }

    fun setName(text: String) {
        title = text
    }
}