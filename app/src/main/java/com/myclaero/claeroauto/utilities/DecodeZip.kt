package com.myclaero.claeroauto.utilities

import android.os.AsyncTask
import com.myclaero.claeroauto.KEY_MASHAPE
import com.myclaero.claeroauto.URL_API_ZIP
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class DecodeZip : AsyncTask<String, Void, String?>() {

	var zipJson: JSONObject? = null

	override fun doInBackground(vararg p0: String?): String? {
		try {
			// Open connection the ZIP Decoder API
			val zipCxn = URL(String.format(URL_API_ZIP, p0[0])).openConnection() as HttpsURLConnection
			zipCxn.apply {
				requestMethod = "GET"
				addRequestProperty("X-Mashape-Key", KEY_MASHAPE)
				addRequestProperty("Accept", "application/json")
			}

			if (zipCxn.responseCode == 200) {
				// Good response, keep going
				val reader = BufferedReader(
					InputStreamReader(
						zipCxn.inputStream,
						"UTF-8"
					)
				)
				var result = ""
				var line = reader.readLine()
				while (line != null) {
					result += line
					line = reader.readLine()
				}
				zipJson = JSONObject(result)
				reader.close()
				zipCxn.disconnect()

				// Return City and State in "City, ST" format.
				return zipJson!!.getString("city") + ", " + zipJson!!.getString("state")
			} else {
				// Bad response, give up
				zipCxn.disconnect()
				return null
			}
		} catch (e: Exception) {
			// Something went wrong. Give up.
			e.printStackTrace()
			return null
		}
	}
}