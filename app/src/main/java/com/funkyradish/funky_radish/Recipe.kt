package com.funkyradish.funky_radish

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.RealmClass
import io.realm.annotations.PrimaryKey

@RealmClass
open class Recipe : RealmObject() {
    @PrimaryKey
    open var realmID: String = ""

    open var title: String = ""

    open var ingredients: RealmList<Ingredient> = RealmList<Ingredient>()

    open var directions: RealmList<Direction> = RealmList<Direction>()
}

