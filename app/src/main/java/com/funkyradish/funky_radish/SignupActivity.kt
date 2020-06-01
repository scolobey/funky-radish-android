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
import io.realm.RealmList
import kotlinx.android.synthetic.main.activity_recipe_search.*
import java.util.Arrays.asList



class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
    }

    /** Called when the user taps the Send button */
    fun sendMessage(view: View) {
        val username = findViewById<EditText>(R.id.editText).text.toString()
        val email = findViewById<EditText>(R.id.editText2).text.toString()
        val password = findViewById<EditText>(R.id.editText3).text.toString()

        if(isOffline(this.applicationContext)) {
            toggleOfflineMode(this.applicationContext)
        }

        val progressBar: ProgressBar = this.recipeListSpinner

        Thread(Runnable {
            this@SignupActivity.runOnUiThread(java.lang.Runnable {
                progressBar.visibility = View.VISIBLE
            })

            // create user
            try {
                val queue = Volley.newRequestQueue(this)

                createUser(this, queue, username, email, password) { success: Boolean ->
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

