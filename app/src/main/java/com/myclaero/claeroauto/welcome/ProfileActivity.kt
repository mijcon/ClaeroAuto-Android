package com.myclaero.claeroauto.welcome

import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ProgressBar
import com.myclaero.claeroauto.*
import com.myclaero.claeroauto.utilities.DecodeZip
import com.parse.ParseUser
import kotlinx.android.synthetic.main.activity_profile.*
import org.jetbrains.anko.clearTask
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.newTask

class ProfileActivity : AppCompatActivity() {

    companion object {
        // Login/Signup state on application start
        private var inputMgr: InputMethodManager? = null

        // Login info Booleans
        private var isValidNameFirst = false
        private var isValidNameLast = false
        private var isValidPhone = false
        private var isValidZip = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        inputMgr = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // Set a listener on each EditText that tracks whether each respective text is a valid entry
        val nameTextWatcher = object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                isValidNameLast = !editNameLast.text.isNullOrBlank()
                isValidNameFirst = !editNameFirst.text.isNullOrBlank()
                checkFields()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        }
        editNameFirst.addTextChangedListener(nameTextWatcher)
        editNameLast.addTextChangedListener(nameTextWatcher)

        editZip.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                // Automatically calls API if ZIP reaches 5 digits
                textCityState.text = ""
                if (p0?.length == 5) {
                    isValidZip = true
                    textCityState.text = DecodeZip().execute(p0.toString()).get()
                } else {
                    isValidZip = false
                }
                checkFields()
            }
        })

        // Set OnClickListeners for each "Button"
        buttonSubmit.setOnClickListener { p0 -> submit(p0!!) }
    }

    private fun submit(v: View) {
        // Hide the keyboard, freeze both EditTexts, and start a ProgressBar
        inputMgr!!.hideSoftInputFromWindow(v.windowToken, 0)
        lockChanges(true)

        ParseUser.getCurrentUser().apply {
            put("givenName", editNameFirst.getString())
            put("familyName", editNameLast.getString())
            put("phone", editPhone.getString())
            put("zip", editZip.getString())
        }.saveInBackground { e ->
            lockChanges(false)
            if (e == null) {
                startActivity(intentFor<VerifyActivity>().newTask().clearTask())
            } else {
                MainActivity().uploadError(
                    "ProfAct-Save",
                    e,
                    "First: ${editNameFirst.getString()}, " +
                            "Last: ${editNameLast.getString()}, " +
                            "Zip: ${editZip.getString()}"
                )
                layoutProfile.makeSnack(R.string.parse_error, SNACK_ERROR)
                buttonSubmit.text = "Proceed"
                buttonSubmit.setOnClickListener { startActivity(intentFor<VerifyActivity>()) }
            }
        }
    }

    private fun lockChanges(lock: Boolean) {
        if (lock) {
            buttonSubmit.visibility = Button.INVISIBLE
            editNameFirst.isEnabled = false
            editNameLast.isEnabled = false
            editPhone.isEnabled = false
            editZip.isEnabled = false
            progressSubmit.visibility = ProgressBar.VISIBLE
        } else {
            buttonSubmit.visibility = Button.VISIBLE
            editNameFirst.isEnabled = true
            editNameLast.isEnabled = true
            editPhone.isEnabled = true
            editZip.isEnabled = true
            progressSubmit.visibility = ProgressBar.GONE
        }
    }

    fun checkFields() {
        buttonSubmit.isEnabled = isValidNameFirst && isValidNameLast && isValidZip
        buttonSubmit.setTextColor(
            ContextCompat.getColor(
                this,
                if (buttonSubmit.isEnabled) R.color.colorWhite else R.color.colorWhiteFaded
            )
        )
    }
}