package com.funkyradish.funky_radish

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_recipe_search.*

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    fun loginButton(view: View) {
        val email = findViewById<EditText>(R.id.loginEmailField).text.toString()
        val password = findViewById<EditText>(R.id.loginPasswordField).text.toString()

        if(isOffline(this.applicationContext)) {
            toggleOfflineMode(this.applicationContext)
        }

        val progressSpinner: ProgressBar = this.recipeListSpinner

        // start loading indicator
        Thread(Runnable {
            this@LoginActivity.runOnUiThread(java.lang.Runnable {
                Log.d("API", "Starting progress bar.")
                progressSpinner.visibility = View.VISIBLE
            })

            // login
            try {
                Log.d("API", "Calling for a token.")
                val queue = Volley.newRequestQueue(this)
                downloadToken(this, queue, email, password, false, {
                    Log.d("API", "Executing login callback")

//  This was causing a crash on login.
//                    toolbar.menu.removeGroup(2)

                    this@LoginActivity.runOnUiThread(java.lang.Runnable {
                        Log.d("API", "Redirecting to main view.")

                        // Set up recipes. Is device already logged in? Are there recipes on the device?
                        val intent = Intent(this, RecipeSearchActivity::class.java).apply {}
                        startActivity(intent)
                    })
                })

            } catch (e: InterruptedException) {
                Log.d("API", "Some kinda error.")
                e.printStackTrace()
            }
        }).start()
    }

    fun signupSegue(view: View) {
        val intent = Intent(this, SignupActivity::class.java).apply {
        }
        startActivity(intent)
    }
}
