package com.decathlon.smartnutristock.data.validation

import android.util.Patterns
import com.decathlon.smartnutristock.domain.validation.EmailValidator
import javax.inject.Inject

/**
 * Android implementation of EmailValidator using android.util.Patterns.EMAIL_ADDRESS.
 *
 * This class lives in the data layer where Android framework dependencies are allowed.
 */
class EmailValidatorImpl @Inject constructor() : EmailValidator {
    override fun isValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
