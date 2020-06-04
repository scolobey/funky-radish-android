package com.funkyradish.funky_radish

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import com.android.volley.toolbox.Volley
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_recipe_search.*

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
    }

    /** Called when the user taps the Send button */
    fun sendMessage(view: View) {

        if(isOffline(this.applicationContext)) {
            toggleOfflineMode(this.applicationContext)
        }

//        TODO: Might need to check if there's already a user and then message to logout first.

        val realm = Realm.getDefaultInstance()
        var recipes = realm.where(Recipe::class.java).findAll()
        var recipeList = realm.copyFromRealm(recipes)
        
        realm.close()

        if (recipes.count() > 0) {

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Transfer recipes?")
            builder.setMessage("There are ${recipeList.count()} recipes on this device. Should we move them to your new account?")

            builder.setPositiveButton("yes") { dialog, which ->

                val realm2 = Realm.getDefaultInstance()
                realm2.executeTransaction { _ ->
                    try {
                        recipes.deleteAllFromRealm()

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                realm.close()

                Log.d("API", "migrating: ${recipeList}")
                launchSignup(recipeList, view)
            }
            builder.setNegativeButton("cancel") { dialog, which ->
                Log.d("API", "canceling signup.")
                val intent = Intent(this, RecipeSearchActivity::class.java).apply {}
                startActivity(intent)
            }
            builder.setNeutralButton("no") { dialog, which ->
                recipeList.clear()
                Log.d("API", "Emptying recipe list.")
                launchSignup(recipeList, view)
            }

            builder.show()
        }


    }

    fun launchSignup(recipeList: List<Recipe?>, view: View) {
        val username = findViewById<EditText>(R.id.editText).text.toString()
        val email = findViewById<EditText>(R.id.editText2).text.toString()
        val password = findViewById<EditText>(R.id.editText3).text.toString()

        val progressBar: ProgressBar = this.recipeListSpinner

        Thread(Runnable {
            this@SignupActivity.runOnUiThread(java.lang.Runnable {
                progressBar.visibility = View.VISIBLE
            })


            try {
                val queue = Volley.newRequestQueue(this)

                createUser(this, queue, username, email, password, recipeList) { success: Boolean ->
                    if (success) {
                        this@SignupActivity.runOnUiThread(java.lang.Runnable {
                            val intent = Intent(this, RecipeSearchActivity::class.java).apply {}
                            startActivity(intent)
                        })
                    } else {
                        this@SignupActivity.runOnUiThread(java.lang.Runnable {
                            progressBar.visibility = View.INVISIBLE
                        })
                    }
                }

            } catch (e: InterruptedException) {
                Log.d("API", "Some kinda error.")
                e.printStackTrace()
            }

        }).start()

    }


    fun loginSegue(view: View) {
        val intent = Intent(this, LoginActivity::class.java).apply {}
        startActivity(intent)
    }
}

