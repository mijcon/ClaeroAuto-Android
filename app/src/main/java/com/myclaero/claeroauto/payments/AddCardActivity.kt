package com.myclaero.claeroauto.payments

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.myclaero.claeroauto.*
import com.myclaero.claeroauto.utilities.DecodeZip
import com.parse.ParseUser
import com.parse.ktx.putOrIgnore
import com.stripe.android.Stripe
import com.stripe.android.TokenCallback
import com.stripe.android.model.Token
import com.stripe.android.view.CardInputListener
import kotlinx.android.synthetic.main.activity_add_card.*

class AddCardActivity : AppCompatActivity() {

    var isValidCard = false
    var isValidName = false
    var isValidZip = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_card)

        checkFields()

        // Set up each of our EditTexts to listen for changes.
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
        editCardName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                // As long as the cardholder's name is not blank, we're good
                isValidName = !p0.isNullOrBlank()
                checkFields()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })
        val cardTextWatcher = object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // When they go to edit the text, we'll automatically say the card is invalid
                isValidCard = false
                checkFields()
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        }
        stripeWidget.apply {
            setCardNumberTextWatcher(cardTextWatcher)
            setCvcNumberTextWatcher(cardTextWatcher)
            setExpiryDateTextWatcher(cardTextWatcher)
        }

        // Whenever any of the card's fields are completed, we'll check the card as a whole
        stripeWidget.setCardInputListener(object : CardInputListener {
            override fun onFocusChange(focusField: String?) {
            }

            override fun onPostalCodeComplete() {
            }

            override fun onCardComplete() {
                isValidCard = stripeWidget.card != null
                checkFields()
            }

            override fun onExpirationComplete() {
                isValidCard = stripeWidget.card != null
                checkFields()
            }

            override fun onCvcComplete() {
                isValidCard = stripeWidget.card != null
                checkFields()
            }
        })
    }

    private fun checkFields() {
        // Pretty obvious. If ZIP is good, name isn't blank card card is valid, we'll turn on the button
        if (isValidZip && isValidName && isValidCard) {
            buttonSubmit.apply {
                setTextColor(ContextCompat.getColor(this@AddCardActivity, R.color.colorBlue))
                isEnabled = true
            }
        } else {
            buttonSubmit.apply {
                setTextColor(ContextCompat.getColor(this@AddCardActivity, R.color.colorBlueFaded))
                isEnabled = false
            }
        }
    }

    fun clearFields(v: View) {
        // Wipe her clean
        stripeWidget.clear()
        editZip.text.clear()
        editCardName.text.clear()
    }

    fun submitCard(v: View) {
        progressSubmit.visibility = ProgressBar.VISIBLE

        // Grab this card. Can't be null, we already checked!
        val newCard = stripeWidget.card!!.apply {
            addressZip = editZip.text.toString()
            name = editCardName.text.toString()
        }

        // We'll start generating a Token
        Stripe(this, KEY_STRIPE).createToken(newCard, object : TokenCallback {
            override fun onSuccess(token: Token?) {
                // If we get a good Token, upload it to the user's profile
                ParseUser.getCurrentUser().apply {
                    putOrIgnore("tokenCard", token?.id)
                    saveInBackground { e ->
                        if (e == null) {
                            // Successfully saved.
                            progressSubmit.visibility = ProgressBar.GONE
                            Toast.makeText(this@AddCardActivity, R.string.token_valid, Toast.LENGTH_SHORT)
                                .show()
                            this@AddCardActivity.finish()
                        } else {
                            progressSubmit.visibility = ProgressBar.GONE
                            e.upload("AddCardAct-SaveTok", "Token: ${token?.id}")
                            layoutPayment.makeSnack(R.string.token_parse, SNACK_WARNING)
                        }
                    }
                }
            }

            override fun onError(error: java.lang.Exception?) {
                progressSubmit.visibility = ProgressBar.GONE
                layoutPayment.makeSnack(R.string.token_invalid, SNACK_WARNING)
            }
        })
    }
}
