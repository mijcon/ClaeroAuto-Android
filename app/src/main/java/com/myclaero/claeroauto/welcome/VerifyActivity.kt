package com.myclaero.claeroauto.welcome

import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.myclaero.claeroauto.MainActivity
import com.myclaero.claeroauto.R
import com.parse.ParseUser
import kotlinx.android.synthetic.main.activity_verify.*
import org.jetbrains.anko.clearTask
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.newTask

class VerifyActivity : AppCompatActivity() {

    // Interval between verification checks, in milliseconds
    private val verifyDelay: Long = 3000

    // Verification objects
    var handlerVerify = Handler()
    private var runnableVerify: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify)

        runnableVerify = Runnable {
            try {
                if (ParseUser.getCurrentUser().fetch().getBoolean("emailVerified")) {
                    textVerifySpam.visibility = TextView.INVISIBLE
                    progressVerify.visibility = ProgressBar.INVISIBLE
                    imageVerified.setImageDrawable(
                        ContextCompat.getDrawable(
                            this.applicationContext,
                            R.drawable.ic_security_96dp
                        )
                    )
                    imageVerified.visibility = ImageView.VISIBLE
                    buttonSkip.text = "Continue"
                } else {
                    handlerVerify.postDelayed(runnableVerify, verifyDelay)
                }
            } catch (e: Exception) {
                progressVerify.visibility = ProgressBar.INVISIBLE
                textVerifySpam.visibility = TextView.INVISIBLE
                imageVerified.setImageDrawable(
                    ContextCompat.getDrawable(
                        this.applicationContext,
                        R.drawable.ic_danger_symbol
                    )
                )
                imageVerified.visibility = ImageView.VISIBLE
            }
        }

        handlerVerify.post(runnableVerify)
    }

    fun skipVerify(v: View) {
        startActivity(intentFor<MainActivity>().newTask().clearTask())
    }

    override fun onResume() {
        super.onResume()

        handlerVerify.post(runnableVerify)
    }

    override fun onPause() {
        super.onPause()

        handlerVerify.removeCallbacks(runnableVerify)
    }
}