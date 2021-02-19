package com.funkyradish.funky_radish

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_recipe_search.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // enable submit from keypad
        val editPwd = findViewById<EditText>(R.id.editText3)

        editPwd.onSubmit {
            sendMessage(view = findViewById(android.R.id.content))
        }
    }

    /** Called when the user taps the Send button */
    fun sendMessage(view: View) {
        if(isOffline(this.applicationContext)) {
            toggleOfflineMode(this.applicationContext)
        }

        this.hideKeyboard(view)

        val email = findViewById<EditText>(R.id.editText2).text.toString()
        val password = findViewById<EditText>(R.id.editText3).text.toString()

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

        //      TODO: Move this into the login section.

//        var recipes = realm.where(Recipe::class.java).findAll()
//        var recipeList = realm.copyFromRealm(recipes)
//
//        if (recipes.count() > 0) {
//            var plural = "recipes"
//            if (recipeList.count() < 2) {
//                plural = "recipe"
//            }
//
//            val builder = AlertDialog.Builder(this)
//            builder.setTitle("Transfer recipes?")
//            builder.setMessage("Detected ${recipeList.count()} $plural on this device. Should we copy this data to your new account?")
//
//            builder.setPositiveButton("yes") { _, _ ->
//                Log.d("API", "migrating: $recipeList")
//                launchSignup(email, password)
//            }
//            builder.setNegativeButton("cancel") { _, _ ->
//                Log.d("API", "canceling sign up.")
//                val intent = Intent(this, RecipeSearchActivity::class.java).apply {}
//                startActivity(intent)
//            }
//            builder.setNeutralButton("no") { _, _ ->
//                recipeList.clear()
//                Log.d("API", "Emptying recipe list.")
//                launchSignup(email, password)
//            }
//
//            builder.show()
//        }
//        else {
            launchSignup(email, password)
//        }
    }

    private fun launchSignup(email: String, password: String) {
        val progressSpinner: ProgressBar = this.recipeListSpinner

        Thread(Runnable {
            this@SignupActivity.runOnUiThread(java.lang.Runnable {
                progressSpinner.visibility = View.VISIBLE
            })

            try {
                val queue = Volley.newRequestQueue(this)

                register(this, queue, email, password) { success: Boolean ->
                    if (success) {
                        this@SignupActivity.runOnUiThread(java.lang.Runnable {

                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Check your email")
                            builder.setMessage("We've sent you a link to verify your email and complete registration.")

                            builder.setPositiveButton("dismiss") { _, _ ->
                                val intent = Intent(this, RecipeSearchActivity::class.java).apply {}
                                startActivity(intent)
                            }

                            builder.show()
                        })
                    } else {
                        this@SignupActivity.runOnUiThread(java.lang.Runnable {
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


    fun loginSegue(view: View) {
        val intent = Intent(this, LoginActivity::class.java).apply {}
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



