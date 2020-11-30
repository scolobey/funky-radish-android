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
import io.realm.Realm
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

//      TODO: Might need to check if there's already a user and then message to logout first.
//        val realm = Realm.getDefaultInstance()
        var recipes = realm.where(Recipe::class.java).findAll()
        var recipeList = realm.copyFromRealm(recipes)
        realm.close()

        if (recipes.count() > 0) {
            var plural = "recipes"
            if (recipeList.count() < 2) {
                plural = "recipe"
            }

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Transfer recipes?")
            builder.setMessage("Detected ${recipeList.count()} ${plural} on this device. Should we copy this data to your new account?")

            builder.setPositiveButton("yes") { dialog, which ->
                Log.d("API", "migrating: ${recipeList}")
                launchLogin(recipeList, email, password, view)
            }
            builder.setNegativeButton("cancel") { dialog, which ->
                Log.d("API", "canceling signup.")
                val intent = Intent(this, RecipeSearchActivity::class.java).apply {}
                startActivity(intent)
            }
            builder.setNeutralButton("no") { dialog, which ->
                recipeList.clear()
                Log.d("API", "Emptying recipe list.")
                launchLogin(recipeList, email, password, view)
            }

            builder.show()
        }
        else {
            recipeList.clear()
            launchLogin(recipeList, email, password, view)
        }
    }

    fun launchLogin(recipeList: List<Recipe?>, email: String, password: String, view: View) {

        val progressSpinner: ProgressBar = this.recipeListSpinner

        // start loading indicator
        Thread(Runnable {
            this@LoginActivity.runOnUiThread(java.lang.Runnable {
                Log.d("API", "Starting progress bar.")
                progressSpinner.visibility = View.VISIBLE
            })

            // login
            try {
                val queue = Volley.newRequestQueue(this)

                downloadToken(this, queue, email, password, recipeList) {
                    this@LoginActivity.runOnUiThread(java.lang.Runnable {
                        Log.d("API", "Redirecting to main view.")

                        // Set up recipes. Is device already logged in? Are there recipes on the device?
                        val intent = Intent(this, RecipeSearchActivity::class.java).apply {}
                        startActivity(intent)
                    })
                }

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

    // Trigger for the done button on the keyboard
    fun EditText.onSubmit(func: () -> Unit) {
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
