package com.funkyradish.funky_radish

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import com.android.volley.toolbox.Volley

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    fun loginButton(view: View) {
        val email = findViewById<EditText>(R.id.loginEmailField).text.toString()
        val password = findViewById<EditText>(R.id.loginPasswordField).text.toString()

        // setting up a Volley RequestQueue
        val queue = Volley.newRequestQueue(this)
        getToken(this, queue, email, password)
    }

    fun signupSegue(view: View) {
        val intent = Intent(this, SignupActivity::class.java).apply {
        }
        startActivity(intent)
    }
}
