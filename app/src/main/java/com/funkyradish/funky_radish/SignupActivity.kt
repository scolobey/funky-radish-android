package com.funkyradish.funky_radish

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_signup.*

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

        val progressBar: ProgressBar = this.progressBar

        Thread(Runnable {
            this@SignupActivity.runOnUiThread(java.lang.Runnable {
                progressBar.visibility = View.VISIBLE
            })

            // create user
            try {
                val queue = Volley.newRequestQueue(this)
                createUser(this, queue, username, email, password)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            this@SignupActivity.runOnUiThread(java.lang.Runnable {
//                Set up recipes. Is device already logged in? Are there recipes on the device?
                val intent = Intent(this, RecipeSearchActivity::class.java).apply {}
                startActivity(intent)
            })
        }).start()
    }

    fun loginSegue(view: View) {
        val intent = Intent(this, LoginActivity::class.java).apply {
        }
        startActivity(intent)
    }
}

