package com.funkyradish.funky_radish

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AbsListView
import android.widget.EditText
import com.android.volley.toolbox.Volley

class AuthorizationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authorization)
    }

    /** Called when the user taps the Send button */
    fun sendMessage(view: View) {
        val username = findViewById<EditText>(R.id.editText).text.toString()
        val email = findViewById<EditText>(R.id.editText2).text.toString()
        val password = findViewById<EditText>(R.id.editText3).text.toString()

        // setting up a Volley RequestQueue
        val queue = Volley.newRequestQueue(this)
        createUser(this, queue, username, email, password)
    }
}

