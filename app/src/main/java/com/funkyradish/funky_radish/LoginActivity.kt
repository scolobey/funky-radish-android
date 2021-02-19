package com.funkyradish.funky_radish

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_recipe_search.*

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // enable submit from keypad
        val edit_pwd = findViewById(R.id.loginPasswordField) as EditText

        edit_pwd.onSubmit {
            loginButton(view = findViewById(android.R.id.content))
        }
    }

    fun loginButton(view: View) {

        if(isOffline(this.applicationContext)) {
            toggleOfflineMode(this.applicationContext)
        }

        this.hideKeyboard(view)

        val email = findViewById<EditText>(R.id.loginEmailField).text.toString()
        val password = findViewById<EditText>(R.id.loginPasswordField).text.toString()

        try {
            var validation = ValidationService()

            validation.isValidEmail(email)
            validation.isValidPW(password)
        }
        catch (error: Error) {
            Toast.makeText(applicationContext,"${error.message}", Toast.LENGTH_SHORT).show()
            return
        }

        var recipes = realm.where(Recipe::class.java).findAll()
        var recipeList = realm.copyFromRealm(recipes)

        if (recipeList.count() > 0) {
            var plural = "recipes"

            if (recipeList.count() < 2) {
                plural = "recipe"
            }

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Transfer recipes?")
            builder.setMessage("Detected ${recipeList.count()} $plural on this device. Should we copy this data to your new account?")

            builder.setPositiveButton("yes") { _, _ ->
                Log.d("API", "migrating: $recipeList")
                launchLogin(recipeList, email, password)
            }
            builder.setNegativeButton("cancel") { _, _ ->
                Log.d("API", "canceling signup.")
                val intent = Intent(this, RecipeSearchActivity::class.java).apply {}
                startActivity(intent)
            }
            builder.setNeutralButton("no") { _, _ ->
                recipeList.clear()
                Log.d("API", "Emptying recipe list.")
                launchLogin(recipeList, email, password)
            }

            builder.show()
        }
        else {
            recipeList.clear()
            launchLogin(recipeList, email, password)
        }
    }

    fun launchLogin(recipeList: List<Recipe?>, email: String, password: String) {

        val progressSpinner: ProgressBar = this.recipeListSpinner

        Thread(Runnable {
            this@LoginActivity.runOnUiThread(java.lang.Runnable {
                progressSpinner.visibility = View.VISIBLE
            })

            try {
                val queue = Volley.newRequestQueue(this)

               login(this, queue, email, password, recipeList) { success: Boolean ->
                    if (success) {
                        this@LoginActivity.runOnUiThread(java.lang.Runnable {
                            val intent = Intent(this, RecipeSearchActivity::class.java).apply {}
                            startActivity(intent)
                        })
                    } else {
                        this@LoginActivity.runOnUiThread(java.lang.Runnable {
                            progressSpinner.visibility = View.INVISIBLE
                        })
                    }
                }
            } catch (e: InterruptedException) {
                //TODO: This catch can probably be removed.
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

    // Trigger for the done button on the keyboard
    private fun EditText.onSubmit(func: () -> Unit) {
        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                func()
            }
            true
        }
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
