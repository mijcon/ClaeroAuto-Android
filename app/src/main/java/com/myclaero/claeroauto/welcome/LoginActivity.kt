package com.myclaero.claeroauto.welcome

import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.myclaero.claeroauto.*
import com.parse.ParseACL
import com.parse.ParseException
import com.parse.ParseUser
import kotlinx.android.synthetic.main.activity_login_email.*
import org.jetbrains.anko.*

class LoginActivity : AppCompatActivity() {

	// Login/Signup state on application start
	private var isLogin = false
	private var inputMgr: InputMethodManager? = null

	// Layout Animation Constants
	private val vTranslation: Float = 300.0f
	private val fadeDuration: Long = 800
	private val initDelay: Long = 2000

	// Login info Booleans
	private var isValidEmail = false
	private var isValidPass = false

	// Regex Patterns
	// At least one lowercase letter, one uppercase letter and one number, no less than 8 characters. LastPass rules.
	private val regexPass = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])[a-zA-Z0-9@!\$/%#]{8,}$")

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_login_email)

		inputMgr = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

		// Set a listener on each EditText that tracks whether each respective text is a valid entry
		editEmail.addTextChangedListener(object : TextWatcher {
			override fun afterTextChanged(p0: Editable?) {
				isValidEmail = !editEmail.text.isNullOrBlank() &&
						Patterns.EMAIL_ADDRESS.matcher(editEmail.text).matches()
				checkFields()
			}

			override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
			}

			override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
			}
		})
		editPass.addTextChangedListener(object : TextWatcher {
			override fun afterTextChanged(p0: Editable?) {
				isValidPass = !editPass.text.isNullOrBlank() && editPass?.text!!.length > 7
				checkFields()
			}

			override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
			}

			override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
			}
		})
		editConf.addTextChangedListener(object : TextWatcher {
			override fun afterTextChanged(p0: Editable?) {
				checkFields()
			}

			override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
			}

			override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
			}
		})

		// Set OnClickListeners for each "Button"
		buttonLogin.setOnClickListener { p0 -> onLogin(p0!!) }
		textForgot.setOnClickListener { p0 -> forgotPass(p0!!) }
		textLoginSwap.setOnClickListener { p0 -> onSwap(p0!!) }

		// To set the initial state, we'll invert the state so it'll "swap" to the desired state
		isLogin = !isLogin
		onSwap(textLoginSwap)

		welcomeAnimation()
	}

	private fun onSwap(v: View?) {
		if (isLogin) {
			// If our current layout is for Login, we'll switch everything to Signup.
			buttonLogin.text = getString(R.string.sign_up)
			(v as TextView).text = getString(R.string.or_log_in)
			textForgot.visibility = TextView.GONE
			editConf.visibility = EditText.VISIBLE
			isLogin = false
		} else {
			// If current layout is not Login, let's make it Login.
			buttonLogin.text = getString(R.string.log_in)
			(v as TextView).text = getString(R.string.or_sign_up)
			textForgot.visibility = TextView.VISIBLE
			editConf.visibility = EditText.GONE
			isLogin = true
		}

		// Important: re-evaluate the EditTexts based on whether or not we need the confirmation EditText.
		checkFields()
	}

	private fun onLogin(v: View) {
		// Hide the keyboard, freeze both EditTexts, and start a ProgressBar
		inputMgr!!.hideSoftInputFromWindow(v.windowToken, 0)
		lockChanges(true)

		if (isLogin) {
			// If Login Button was clicked, we'll attempt to log in on a separate thread
			ParseUser.logInInBackground(
				editEmail.getString(),
				editPass.getString()
			) { user, e ->
				if (e == null) {
					// No errors
					Toast.makeText(
						this@LoginActivity,
						String.format(getString(R.string.welcome_back), user.getString("givenName")),
						Toast.LENGTH_LONG
					).show()
					this@LoginActivity.defaultSharedPreferences.edit().putBoolean("logged_in_prev", true).apply()
					startActivity(intentFor<MainActivity>().newTask().clearTask())
				} else {
					when (e.code) {
						// For security, we won't say if it was the Email or Password that was wrong...
						ParseException.EMAIL_NOT_FOUND,
						ParseException.PASSWORD_MISSING,
						ParseException.MUST_CREATE_USER_THROUGH_SIGNUP,
						ParseException.OBJECT_NOT_FOUND ->
							MainActivity().makeSnack(
								this@LoginActivity, layoutLogin,
								R.string.login_invalid,
								SNACK_WARNING
							)
						else ->
							MainActivity().makeSnack(
								this@LoginActivity, layoutLogin,
								R.string.login_error,
								SNACK_ERROR
							)
					}
				}
				lockChanges(false)
			}
		} else {
			// Must be trying to sign up
			// Let's make sure passwords match and meets password minimums.
			when {
				editConf.getString() != editPass.getString() -> {
					// The two strings don't match. Make a Snackbar and unlock the EditTexts
					lockChanges(false)
					MainActivity().makeSnack(
						this, layoutLogin,
						R.string.password_mismatch,
						SNACK_WARNING
					)
				}
				editPass.getString().matches(regexPass) -> {
					// Both passwords match. Build and save a new user!
					ParseUser.logOutInBackground()
					ParseUser().apply {
						username = editEmail.getString()
						email = editEmail.getString()
						setPassword(editPass.getString())
						acl = ParseACL().apply {
							publicReadAccess = false
							publicWriteAccess = false
						}
						signUpInBackground { e ->
							if (e == null) {
								defaultSharedPreferences.edit().putBoolean("logged_in_prev", true).apply()
								startActivity<ProfileActivity>()
							} else {
								// Something went wrong. Unlock the EditTexts.
								val myWarn = listOf(
									ParseException.EMAIL_TAKEN,
									ParseException.USERNAME_TAKEN,
									ParseException.INVALID_EMAIL_ADDRESS,
									ParseException.PASSWORD_MISSING,
									ParseException.USERNAME_MISSING
								)
								MainActivity().makeSnack(
									this@LoginActivity, layoutLogin,
									when (e.code) {
										myWarn[0], myWarn[1] ->
											R.string.email_taken
										myWarn[2], myWarn[3], myWarn[4] ->
											R.string.signup_invalid
										else ->
											R.string.login_error
									},
									if (e.code in myWarn) SNACK_WARNING else SNACK_ERROR
								)
							}
							lockChanges(false)
						}
					}
				}
				else -> {
					MainActivity().makeSnack(
						this,
						layoutLogin,
						R.string.password_regex,
						SNACK_WARNING
					)
					lockChanges(false)
				}
			}
		}
	}

	private fun forgotPass(v: View) {
		inputMgr!!.hideSoftInputFromWindow(v.windowToken, 0)

		when {
			editEmail.text.isNullOrBlank() ->
				MainActivity().makeSnack(this, layoutLogin, R.string.enter_email, SNACK_WARNING)
			isValidEmail -> {
				ParseUser.requestPasswordResetInBackground(editEmail.getString()) { e ->
					if (e == null || e.code == ParseException.EMAIL_NOT_FOUND) {
						MainActivity().makeSnack(this, layoutLogin, R.string.password_reset)
					} else {
						MainActivity().makeSnack(
							this,
							layoutLogin,
							R.string.password_reset_fail,
							SNACK_WARNING
						)
					}
				}
			}
			else ->
				MainActivity().makeSnack(this, layoutLogin, R.string.error_invalid_email, SNACK_WARNING)
		}
	}

	private fun lockChanges(lock: Boolean) {
		if (lock) {
			buttonLogin.visibility = Button.INVISIBLE
			editEmail.isEnabled = false
			editPass.isEnabled = false
			editConf.isEnabled = false
			progressSubmit.visibility = ProgressBar.VISIBLE
		} else {
			buttonLogin.visibility = Button.VISIBLE
			editEmail.isEnabled = true
			editPass.isEnabled = true
			editConf.isEnabled = true
			progressSubmit.visibility = ProgressBar.GONE
		}
	}

	private fun welcomeAnimation() {
		// Make everything but Welcome invisible
		layoutFields.alpha = 0.0f
		layoutButtons.alpha = 0.0f

		// Move Welcome and EditTexts down
		textWelcome.translationY = vTranslation
		layoutFields.translationY = vTranslation

		// First, show welcome briefly in middle then move up. Have EditTexts fade in and glide up with Welcome
		textWelcome.animate().translationYBy(-vTranslation).setDuration(fadeDuration)
			.setStartDelay(initDelay)
			.start()
		layoutFields.animate().translationYBy(-vTranslation).alphaBy(1.0f).setDuration(fadeDuration)
			.setStartDelay(initDelay).start()

		// Then after everything has faded into place, have Buttons fade into their spots.
		layoutButtons.animate().alphaBy(1.0f).setDuration(fadeDuration)
			.setStartDelay(initDelay + fadeDuration)
			.start()
	}

	fun checkFields() {
		buttonLogin.isEnabled = isValidEmail && isValidPass &&
				if (!isLogin) editConf.text!!.length == editPass.text!!.length else true
		buttonLogin.setTextColor(
			ContextCompat.getColor(
				this,
				if (buttonLogin.isEnabled) R.color.colorWhite else R.color.colorWhiteFaded
			)
		)
	}
}