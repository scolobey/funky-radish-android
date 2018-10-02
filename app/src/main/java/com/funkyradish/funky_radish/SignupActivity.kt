package com.funkyradish.funky_radish

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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

        // start loading indicator

        // pass a callback to kill the loader and dismiss the view

        val progressBar: ProgressBar = this.progressBar

        this@SignupActivity.runOnUiThread(java.lang.Runnable {
            progressBar.visibility = View.VISIBLE
        })

//        val callback = {
//            println("heynanan")
//        }

        // setting up a Volley RequestQueue
        val queue = Volley.newRequestQueue(this)
        createUser(this, queue, username, email, password)
    }

    fun loginSegue(view: View) {
        val intent = Intent(this, LoginActivity::class.java).apply {
        }
        startActivity(intent)
    }
}

