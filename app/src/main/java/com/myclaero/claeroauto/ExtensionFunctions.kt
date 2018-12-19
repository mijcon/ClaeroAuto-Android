package com.myclaero.claeroauto

import android.util.Log
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
            putOrIgnore("parseCode", (this@upload as ParseException).message)
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
    for (i in 0..(this.length()-1)) {
        @Suppress("UNCHECKED_CAST")
        list.add(this.getJSONObject(i).get(key) as T)
    }
    return list
}

/**
 * Eliminates any JSONObjects within the JSONArray that contain the passed key-value pair.
 */
fun JSONArray.filterOut(key: String, value: Any) {
    for (i in 0..this.length()) {
        if (this.getJSONObject(i).get(key) == value) {
            this.remove(i)
        }
    }
}