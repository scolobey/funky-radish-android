package com.funkyradish.funky_radish

import android.util.Log

class ValidationService {
//    fun isValidUsername(username: String) {
//        if (username.count() > 0) {
//            return
//        }
//        else {
//            throw Error("Invalid Username.")
//        }
//    }

    fun isValidEmail(email: String) {
        val emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        val emailQualifies = emailRegex.toRegex().matches(email);

        if (emailQualifies) return
        else {
            throw Error("Invalid Email.")
        }
    }

    fun isValidPW(password: String) {
        Log.d("API", "calling pw check")
        val passwordRegex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$"
        val passwordQualifies = passwordRegex.toRegex().matches(password)

        if (passwordQualifies) return
        else {
            throw Error("Password requires 8 characters, including a number.")
        }
    }
}

