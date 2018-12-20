package com.myclaero.claeroauto

import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.ViewGroup
import android.widget.EditText
import com.parse.ParseException
import com.parse.ParseObject
import com.parse.ParseUser
import com.parse.ktx.putOrIgnore
import org.json.JSONArray

/**
 * Shorthand handling of EditText Strings.
 */
fun EditText.getString(): String {
    return text.toString()
}

/**
 * Sends information provided and the Exception itself to the Parse Server for error-reporting
 */
fun Exception.upload(loc: String, note: String? = null) {
    try {
        ParseObject("Error").apply {
            put("codeSection", loc)
            put("owner", ParseUser.getCurrentUser().objectId)
            putOrIgnore("stackTrace", this@upload.stackTrace)
            putOrIgnore("extra", note)
            putOrIgnore(
                "parseCode", try {
                    (this@upload as ParseException).message
                } catch (e: Exception) {
                    null
                }
            )
            saveEventually()
        }
    } catch (e: Exception) {
        Log.e("ClaeroParse", "Parse is having some problems. We can't upload any error messages!", e)
    }
}

/**
 * Returns a List<T>() of values from each JSONObject corresponding to the passed key argument.
 */
fun <T> JSONArray.getListOf(key: String): MutableList<T> {
    val list = mutableListOf<T>()
    for (i in 0..(this.length() - 1)) {
        @Suppress("UNCHECKED_CAST")
        list.add(this.getJSONObject(i).get(key) as T)
    }
    return list
}

/**
 * Eliminates any JSONObjects within the JSONArray that contain the passed key-value pair.
 */
fun JSONArray.filterOut(key: String, value: Any) {
    var end = this.length()
    var i = 0
    while (i < end) run {
        if (this.getJSONObject(i).get(key) == value) {
            this.remove(i)
            end--
        } else {
            i++
        }
    }
}

fun ViewGroup.makeSnack(msg: String, error_type: Int? = null) {
    var textColor: Int = R.color.colorTextSuccess
    var backgroundColor: Int = R.color.colorSuccess

    if (error_type == SNACK_WARNING) {
        textColor = R.color.colorTextWarning
        backgroundColor = R.color.colorWarning
    } else if (error_type == SNACK_ERROR) {
        textColor = R.color.colorTextError
        backgroundColor = R.color.colorError
    }

    Snackbar.make(this, msg, Snackbar.LENGTH_LONG).apply {
        setActionTextColor(ContextCompat.getColor(context, textColor))
        this.view.setBackgroundColor(ContextCompat.getColor(context, backgroundColor))
        show()
    }
}

fun ViewGroup.makeSnack(msg: Int, error_type: Int? = null) {
    this.makeSnack(resources.getString(msg), error_type)
}