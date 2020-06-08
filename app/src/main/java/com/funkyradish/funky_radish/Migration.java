package com.funkyradish.funky_radish;

import io.realm.DynamicRealm;
import io.realm.DynamicRealmObject;
import io.realm.FieldAttribute;
import io.realm.RealmList;
import io.realm.RealmMigration;
import io.realm.RealmObjectSchema;
import io.realm.RealmSchema;
import java.util.UUID;

/**
 * Examples: https://github.com/realm/realm-java/blob/master/examples/migrationExample/src/main/java/io/realm/examples/realmmigrationexample/model/Migration.java
 */

public class Migration implements RealmMigration {

    @Override
    public void migrate(final DynamicRealm realm, long oldVersion, long newVersion) {

        RealmSchema schema = realm.getSchema();

        /************************************************
         * // Version 0
         *
         * class Recipe
         * @Required
         * String realmId;
         * String title;
         * RealmList<String> ingredients;
         * RealmList<String> directions;
         *
         * // Version 1
         * class Recipe
         * @Required
         * String realmId;
         * String title;
         * RealmList<Ingredient> ingredients;
         * RealmList<Direction> directions;
         * --------------
         * class Ingredient
         * @Required
         * String realmId;
         * String name;
         * --------------
         * class Direction
         * @Required
         * String realmId;
         * String text;
         */

        // Migrate from version 0 to version 1
        if (oldVersion == 0) {
            // Add Ingredient class
            RealmObjectSchema ingredientSchema = schema.create("Ingredient")
                    .addField("realmID", String.class, FieldAttribute.PRIMARY_KEY)
                    .setNullable("realmID", false)
                    .addField("name", String.class, FieldAttribute.REQUIRED);

            // Add Direction class
            RealmObjectSchema directionSchema = schema.create("Direction")
                    .addField("realmID", String.class, FieldAttribute.PRIMARY_KEY)
                    .setNullable("realmID", false)
                    .addField("text", String.class, FieldAttribute.REQUIRED);

            // Add Ingredients and Directions to Recipe
            schema.get("Recipe")
                    .setNullable("title", true)
                    .addRealmListField("temp_ingredients", ingredientSchema)
                    .addRealmListField("temp_directions", directionSchema)
                    .transform(new RealmObjectSchema.Function() {
                        @Override
                        public void apply(DynamicRealmObject obj) {

                            RealmList<String> ingList = obj.get("ingredients");
                            RealmList<String> dirList = obj.get("directions");

                            for (String ing : ingList) {
                                DynamicRealmObject newIng = realm.createObject("Ingredient");
                                newIng.setString("realmID", UUID.randomUUID().toString());
                                newIng.setString("name", ing);
                                obj.getList("temp_ingredients").add(newIng);
                            }

                            for (String dir : dirList) {
                                DynamicRealmObject newDir = realm.createObject("Direction");
                                newDir.setString("realmID", UUID.randomUUID().toString());
                                newDir.setString("text", dir);
                                obj.getList("temp_directions").add(newDir);
                            }

                        }
                    })
                    .removeField("_id")
                    .removeField("updatedAt")
                    .removeField("ingredients")
                    .removeField("directions")
                    .renameField("temp_ingredients", "ingredients")
                    .renameField("temp_directions", "directions");

            oldVersion++;
        }


    }
}