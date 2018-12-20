package com.myclaero.claeroauto.vehicles

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.myclaero.claeroauto.R
import com.myclaero.claeroauto.filterOut
import com.myclaero.claeroauto.getListOf
import com.myclaero.claeroauto.upload
import kotlinx.android.synthetic.main.fragment_veh_add_enter.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
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
        private val stringYears = mutableListOf("Year")
        private val stringMakes = mutableListOf("Make")
        private val stringModels = mutableListOf("Model")

        private const val MODEL_DB_URL = "https://www.carqueryapi.com/api/0.3/?callback=?&"
        private const val OLDEST_MODEL_YEAR = 2000
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_veh_add_enter, container, false)
        val bundle = arguments

        inputMgr = activity!!.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // Set all three to blank lists.
        rootView.spinnerYear.apply {
            adapter = ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, listOf("Year"))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    rootView.spinnerMake.apply {
                        setSelection(0)
                        isEnabled = false
                    }
                    if (position != 0) runCarQuery(this@apply.selectedItem.toString())
                }
            }
        }
        rootView.spinnerMake.apply {
            adapter = ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, listOf("Make"))
            isEnabled = false
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    rootView.spinnerModel.apply {
                        setSelection(0)
                        isEnabled = false
                    }
                    if (position != 0) runCarQuery(
                        rootView.spinnerYear.selectedItem.toString(),
                        this@apply.selectedItem.toString()
                    )
                }
            }
        }
        rootView.spinnerModel.apply {
            adapter = ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, listOf("Model"))
            isEnabled = false
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    rootView.buttonConfirm.apply {
                        if (position == 0) {
                            isEnabled = false
                            setTextColor(ContextCompat.getColor(activity!!, R.color.colorBlueFaded))
                        } else {
                            isEnabled = true
                            setTextColor(ContextCompat.getColor(activity!!, R.color.colorBlue))
                        }
                    }
                }
            }
        }

        rootView.buttonConfirm.setOnClickListener { submitYmm(it) }

        // Initiate the first pull: Years
        runCarQuery()

        return rootView
    }

    private fun submitYmm(v: View) {

    }

    private fun runCarQuery(vararg params: String?) {
        val rootView = view!!

        doAsync {
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
                    // TODO Some Ui error-handling...
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
                // TODO UiError...
            }
            modelDbCxn.disconnect()

            val resultJson = JSONObject(result)
            when (params.size) {
                0 -> {  // Save the highest year and chip out.
                    maxYearApi = resultJson.getJSONObject("Years").getString("max_year").toInt()
                    stringYears.apply {
                        clear()
                        add("Year")
                        for (year in (Calendar.getInstance().get(Calendar.YEAR) + 1).downTo(OLDEST_MODEL_YEAR)) {
                            add(year.toString())
                        }
                    }
                    uiThread {
                        rootView.spinnerYear.adapter =
                                ArrayAdapter<String>(
                                    activity!!,
                                    android.R.layout.simple_spinner_dropdown_item,
                                    stringYears
                                )
                    }
                }
                1 -> {  // Remove any uncommon Makes and return
                    val makesArray = resultJson.getJSONArray("Makes")
                    makesArray.filterOut("make_is_common", "0")
                    stringMakes.apply {
                        clear()
                        add("Make")
                        addAll(makesArray!!.getListOf("make_display"))
                    }
                    uiThread {
                        rootView.spinnerMake.apply {
                            adapter =
                                    ArrayAdapter<String>(
                                        activity!!,
                                        android.R.layout.simple_spinner_dropdown_item,
                                        stringMakes
                                    )
                            isEnabled = true
                        }
                    }
                }
                2 -> {  // Return Models
                    stringModels.apply {
                        clear()
                        add("Model")
                        addAll(resultJson.getJSONArray("Models")!!.getListOf("model_name"))
                    }
                    uiThread {
                        rootView.spinnerModel.apply {
                            adapter =
                                    ArrayAdapter<String>(
                                        activity!!,
                                        android.R.layout.simple_spinner_dropdown_item,
                                        stringModels
                                    )
                            isEnabled = true
                        }
                    }
                }
            }
        }
    }

    /*inner class CarQueryModels : AsyncTask<String?, Unit, Int>() {

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
                        maxYearApi = resultJson.getJSONObject("Years").getString("max_year").toInt()
                        stringYears.apply {
                            clear()
                            add("Year")
                            for (year in (Calendar.getInstance().get(Calendar.YEAR) + 1).downTo(OLDEST_MODEL_YEAR)) {
                                add(year.toString())
                            }
                        }
                        return CQ_SUCCESS_YR
                    }
                    1 -> {  // Remove any uncommon Makes and return
                        val makesArray = resultJson.getJSONArray("Makes")
                        makesArray.filterOut("make_is_common", "0")
                        stringMakes.apply {
                            clear()
                            add("Make")
                            addAll(makesArray!!.getListOf("make_display"))
                        }
                        return CQ_SUCCESS_MK
                    }
                    2 -> {  // Return Models
                        stringModels.apply {
                            clear()
                            add("Model")
                            addAll(resultJson.getJSONArray("Models")!!.getListOf("model_name"))
                        }
                        return CQ_SUCCESS_ML
                    }
                }
            } catch (e: Exception) {
                e.upload("AddVehYmm-CarQueryModels")
                return CQ_FAIL
            }
            return 0
        }

        override fun onPostExecute(result: Int?) {
            super.onPostExecute(result)

            val rootView = view!!

            when (result) {
                CQ_SUCCESS_YR -> {
                    rootView.spinnerYear.adapter =
                            ArrayAdapter<String>(activity!!, android.R.layout.simple_spinner_dropdown_item, stringYears)
                }
                CQ_SUCCESS_MK -> {
                    rootView.spinnerMake.apply {
                        adapter =
                                ArrayAdapter<String>(
                                    activity!!,
                                    android.R.layout.simple_spinner_dropdown_item,
                                    stringMakes
                                )
                        isEnabled = true
                    }
                }
                CQ_SUCCESS_ML -> {
                    rootView.spinnerModel.apply {
                        adapter =
                                ArrayAdapter<String>(
                                    activity!!,
                                    android.R.layout.simple_spinner_dropdown_item,
                                    stringModels
                                )
                        isEnabled = true
                    }
                }
                else -> {
                    // Throw error
                }
            }
        }
    }*/
}