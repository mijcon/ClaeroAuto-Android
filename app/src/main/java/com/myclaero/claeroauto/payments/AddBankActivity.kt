package com.myclaero.claeroauto.payments

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.myclaero.claeroauto.R
import com.myclaero.claeroauto.SNACK_WARNING
import com.myclaero.claeroauto.makeSnack
import com.myclaero.claeroauto.upload
import com.parse.ParseUser
import com.parse.ktx.putOrIgnore
import kotlinx.android.synthetic.main.activity_add_bank.*
import org.json.JSONObject
import java.util.*

class AddBankActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_bank)

        // Initialize Link
        val linkInitializeOptions = HashMap<String, String>(
            mutableMapOf(
                "key" to "e4a25ce7d4d19ddbeccb25029b6f9d",
                "product" to "auth",
                "apiVersion" to "v2", // set this to "v1" if using the legacy Plaid API
                "env" to "sandbox",
                "clientName" to "Claero Test",
                "selectAccount" to "true",
                "webhook" to "http://requestb.in",
                "baseUrl" to "https://cdn.plaid.com/link/v2/stable/link.html"
            )
        )
        // If initializing Link in PATCH / update mode, also provide the public_token
        // linkInitializeOptions.put("token", "PUBLIC_TOKEN")

        // Generate the Link initialization URL based off of the configuration options.
        val linkInitializationUrl = generateLinkInitializationUrl(linkInitializeOptions)

        // Modify Webview settings - all of these settings may not be applicable
        // or necesscary for your integration.
        plaidWebview.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        WebView.setWebContentsDebuggingEnabled(true)

        // Initialize Link by loading the Link initiaization URL in the Webview
        plaidWebview.loadUrl(linkInitializationUrl.toString())

        // Override the Webview's handlerVerify for redirects
        // Link communicates success and failure (analogous to the web's onSuccess and onExit
        // callbacks) via redirects.
        plaidWebview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                // Parse the URL to determine if it's a special Plaid Link redirect or a request
                // for a standard URL (typically a forgotten password or account not setup link).
                // Handle Plaid Link redirects and open traditional pages directly in the  user's
                // preferred browser.
                val parsedUri = Uri.parse(url)
                if (parsedUri.scheme == "plaidlink") {
                    val action = parsedUri.host
                    val linkData = parseLinkUriData(parsedUri)

                    when (action) {
                        "connected" -> {
                            ParseUser.getCurrentUser().apply {
                                putOrIgnore("tokenAch", linkData["public_token"])
                                putOrIgnore("achObject", JSONObject(linkData))
                                saveInBackground { e ->
                                    if (e == null) {
                                        plaidLayout.makeSnack(R.string.bank_linked)
                                    } else {
                                        e.upload("AddPlaid-SaveToken", "Institution: ${linkData["institution_name"]}")
                                        plaidLayout.makeSnack(R.string.bank_failed, SNACK_WARNING)
                                    }
                                }
                            }
                        }
                        "exit" -> {
                            this@AddBankActivity.finish()
                        }
                        "event" -> // The event action is fired as the user moves through the Link flow
                            Log.d("Event name: ", linkData["event_name"])
                        else -> Log.d("Link action detected: ", action)
                    }
                    // Override URL loading
                    return true
                } else if (parsedUri.scheme == "https" || parsedUri.scheme == "http") {
                    // Open in browser - this is most  typically for 'account locked' or
                    // 'forgotten password' redirects
                    view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    // Override URL loading
                    return true
                } else {
                    // Unknown case - do not override URL loading
                    return false
                }
            }
        }
    }

    // Generate a Link initialization URL based on a set of configuration options
    fun generateLinkInitializationUrl(linkOptions: HashMap<String, String>): Uri {
        val builder = Uri.parse(linkOptions["baseUrl"])
            .buildUpon()
            .appendQueryParameter("isWebview", "true")
            .appendQueryParameter("isMobile", "true")
        for (key in linkOptions.keys) {
            if (key != "baseUrl") {
                builder.appendQueryParameter(key, linkOptions[key])
            }
        }
        return builder.build()
    }

    // Parse a Link redirect URL querystring into a HashMap for easy manipulation and access
    fun parseLinkUriData(linkUri: Uri): HashMap<String, String> {
        val linkData = HashMap<String, String>()
        for (key in linkUri.queryParameterNames) {
            linkData[key] = linkUri.getQueryParameter(key)
        }
        return linkData
    }
}