package com.myclaero.claeroauto.vehicles

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import com.myclaero.claeroauto.*
import kotlinx.android.synthetic.main.fragment_veh_add_enter.view.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection

class EnterYmmFragment : Fragment() {

    companion object {
        var inputMgr: InputMethodManager? = null

        private var maxYearApi: Int? = null
        private var stringMakes: List<String>? = null
        private var stringModels: List<String>? = null

        private const val MODEL_DB_URL = "https://www.carqueryapi.com/api/0.3/?callback=?&"
        private const val CQ_FAIL = -1
        private const val CQ_SUCCESS_YR = 1
        private const val CQ_SUCCESS_MK = 2
        private const val CQ_SUCCESS_ML = 3
        private const val OLDEST_MODEL_YEAR = 2000
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_veh_add_enter, container, false)
        val bundle = arguments

        inputMgr = activity!!.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        when (CarQueryModels().execute("2008").get()) {
            CQ_SUCCESS_YR -> {
                val modelYears = mutableListOf<String>()
                for (year in (Calendar.getInstance().get(Calendar.YEAR) + 1).downTo(OLDEST_MODEL_YEAR)) {
                    modelYears.add(year.toString())
                }
                rootView.spinnerYear.adapter =
                        ArrayAdapter<String>(activity!!, android.R.layout.simple_spinner_dropdown_item, modelYears)
            }
            CQ_SUCCESS_MK -> {
                rootView.spinnerMake.adapter =
                        ArrayAdapter<String>(activity!!, android.R.layout.simple_spinner_dropdown_item, stringMakes)
            }
            CQ_SUCCESS_ML -> {
                rootView.spinnerModel.adapter =
                        ArrayAdapter<String>(activity!!, android.R.layout.simple_spinner_dropdown_item, stringModels)
            }
            else -> {
                // Throw error
            }
        }

        return rootView
    }

    /*
    private fun proceedVin(v: View) {
        inputMgr?.hideSoftInputFromWindow(editVin.windowToken, 0)

        buttonConfirm.visibility = Button.INVISIBLE
        progressProceed.visibility = ProgressBar.VISIBLE

        try {
            // See if User has an inactive Vehicle that matches
            val vehList = ParseQuery.getQuery<ParseObject>("Vehicle").apply {
                whereEqualTo("vin", vehVin)
                whereEqualTo("owner", ParseUser.getCurrentUser())
                whereEqualTo("isActive", false)
            }.find()
            if (vehList.isNotEmpty()) {
                vehList[0].put("isActive", true)
                vehList[0].saveInBackground {
                    progressProceed.visibility = ProgressBar.GONE
                    if (it == null) {
                        activity!!.finish()
                    } else {
                        buttonConfirm.visibility = Button.VISIBLE
                        MainActivity().makeSnack(activity!!, layoutAddVehicle, R.string.parse_error, SNACK_ERROR)
                        MainActivity().uploadError("AddVehAct", it, "VIN: $vehVin")
                    }
                }
            } else {
                val data = listOf(
                    "vin",
                    "drive_type",
                    "year",
                    "trim_level",
                    "transmission",
                    "engine",
                    "model",
                    "style",
                    "fuel_type",
                    "make"
                )
                val vehObject = ParseObject("Vehicle").apply {
                    put("owner", ParseUser.getCurrentUser())
                    put("nickname", vehNickname!!)
                    put("json", vehJson!!)
                    put("isActive", true)
                    for (datum in data) put(datum, vehSpec!!.get(datum))
                }
                vehObject.saveInBackground {
                    progressProceed.visibility = ProgressBar.GONE
                    if (it == null) {
                        activity!!.finish()
                    } else {
                        buttonConfirm.visibility = Button.VISIBLE
                        MainActivity().makeSnack(activity!!, layoutAddVehicle, R.string.parse_error, SNACK_ERROR)
                        MainActivity().uploadError("AddVehAct-Proceed", it, "VIN: $vehVin")
                    }
                }
            }
        } catch (e: Exception) {
            progressProceed.visibility = ProgressBar.GONE
            buttonConfirm.visibility = Button.VISIBLE
            MainActivity().uploadError("AddVehAct-Proceed", e, "VIN: $vehVin")
            MainActivity().makeSnack(activity!!, layoutAddVehicle, R.string.parse_error, SNACK_ERROR)
        }
    }
    */

    inner class CarQueryModels : AsyncTask<String?, Unit, Int>() {

        override fun doInBackground(vararg params: String?): Int {
            try {
                val fullUrl = MODEL_DB_URL + when (params.size) {
                    0 ->      // Just getting Available Years from API
                        "cmd=getYears"
                    1 ->      // Getting all Makes for a given Year
                        String.format(
                            "cmd=getMakes&sold_in_us=1&year=%s",
                            if (params[0]!!.toInt() > maxYearApi!!) maxYearApi else params[0]
                        )
                    2 ->      // Getting all Models for a given Year-Make combination
                        String.format(
                            "cmd=getModels&sold_in_us=1&year=%s&make=%s",
                            if (params[0]!!.toInt() > maxYearApi!!) maxYearApi else params[0],
                            params[1]!!.toLowerCase()
                        )
                    else -> {   // This is not supposed to happen, like, ever.
                        Exception("Invalid arguments for CarQueryModels!").upload(
                            "AddVehYmm-CarQueryModels",
                            params.joinToString()
                        )
                        return CQ_FAIL
                    }
                }

                // Connect, pull, disconnect.
                val modelDbCxn = (URL(fullUrl).openConnection() as HttpsURLConnection)
                modelDbCxn.addRequestProperty("Content-Type", "application/json")
                var result = ""
                if (modelDbCxn.responseCode == 200) {
                    // Good response, keep going
                    val reader = BufferedReader(
                        InputStreamReader(
                            modelDbCxn.inputStream,
                            "UTF-8"
                        )
                    )

                    var line = reader.readLine()
                    while (line != null) {
                        result += line
                        line = reader.readLine()
                    }
                    reader.close()

                    // Remove unnecessary jQuery JSON wrappers
                    result = result.drop(2).dropLast(2)
                } else {
                    Exception("Failed to connect to CarQuery!").upload(
                        "AddVehYmm-CarQueryModels",
                        "Response Code: ${modelDbCxn.responseCode}, Message: ${modelDbCxn.responseMessage}"
                    )
                    return CQ_FAIL
                }
                modelDbCxn.disconnect()

                val resultJson = JSONObject(result)
                when (params.size) {
                    0 -> {  // Save the highest year and chip out.
                        maxYearApi = resultJson.getString("max_year").toInt()
                        return CQ_SUCCESS_YR
                    }
                    1 -> {  // Remove any uncommon Makes and return
                        val makesArray = resultJson.getJSONArray("Makes")
                        makesArray.filterOut("make_is_common", "0")
                        stringMakes = makesArray!!.getListOf("make_display")
                        return CQ_SUCCESS_MK
                    }
                    2 -> {  // Return Models
                        stringModels = resultJson.getJSONArray("Models")!!.getListOf("model_name")
                        return CQ_SUCCESS_ML
                    }
                }
            } catch (e: Exception) {
                e.upload("AddVehYmm-CarQueryModels")
                return CQ_FAIL
            }
            return 0
        }
    }
}