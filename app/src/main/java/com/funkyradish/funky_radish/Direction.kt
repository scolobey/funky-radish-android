package com.funkyradish.funky_radish

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

@RealmClass
open class Direction : RealmObject() {
    @PrimaryKey
    open var realmID: String = ""

    open var text: String = ""
}