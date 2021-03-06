package com.myclaero.claeroauto.vehicles

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import com.myclaero.claeroauto.*
import com.parse.ParseObject
import kotlinx.android.synthetic.main.fragment_veh_add_enter.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject
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
                    rootView.buttonConfirm.isEnabled = position != 0
                }
            }
        }

        rootView.buttonConfirm.setOnClickListener { submitYmm(it) }

        return rootView
    }

    override fun onStart() {
        super.onStart()

        // Initiate the first pull: Years
        runCarQuery()
    }

    private fun submitYmm(v: View) {
        val rootView = view!!
        isSubmitting(true)

        doAsync {
            try {
                val listYmm = mapOf(
                    "year" to rootView.spinnerYear.selectedItem.toString(),
                    "make" to rootView.spinnerMake.selectedItem.toString(),
                    "model" to rootView.spinnerModel.selectedItem.toString()
                )
                val nickname = listYmm.values.joinToString(" ")

                ParseObject("Vehicle").apply {
                    put("nickname", nickname)
                    put("isActive", true)
                    listYmm.keys.forEach {
                        put(it, listYmm[it]!!)
                    }
                }.save()
                uiThread { activity!!.finish() }
            } catch (e: Exception) {
                uiThread {
                    isSubmitting(false)
                    rootView.layoutEnterYmm.makeSnack(R.string.parse_error, SNACK_ERROR)
                    e.upload("ScanVinFrag-QueryProceed", "VIN: ${ScanVinFragment.vehVin}")
                }
            }
        }
    }

    private fun runCarQuery(vararg params: String?) {
        val rootView = view!!

        doAsync {
            try {
                val fullUrl = MODEL_DB_URL + when (params.size) {
                    0 ->    // Just getting Available Years from API
                        "cmd=getYears"
                    1 -> {    // Getting all Makes for a given Year
                        val queryYear = if (params[0]!!.toInt() > maxYearApi!!) maxYearApi.toString() else params[0]
                        String.format("cmd=getMakes&sold_in_us=1&year=%s", queryYear)
                    }
                    2 -> {    // Getting all Models for a given Year-Make combination
                        val queryYear = if (params[0]!!.toInt() > maxYearApi!!) maxYearApi.toString() else params[0]
                        String.format("cmd=getModels&sold_in_us=1&year=%s&make=%s", queryYear, params[1]!!.toLowerCase())
                    }
                    else -> // This is not supposed to happen, like, ever.
                        throw Exception("Invalid arguments for CarQueryModels!")
                }

                // Connect, pull, disconnect.
                val modelDbCxn = (URL(fullUrl).openConnection() as HttpsURLConnection)
                modelDbCxn.addRequestProperty("Content-Type", "application/json")

                val resultJson: JSONObject?
                if (modelDbCxn.responseCode == 200) {
                    // Good response, read data and remove jQuery wrapper
                    val result = modelDbCxn.readAll().drop(2).dropLast(2)
                    resultJson = JSONObject(result)
                } else {
                    throw Exception("Failed to connect to CarQuery!")
                }
                modelDbCxn.disconnect()

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
            } catch (e: Exception) {
                uiThread {
                    e.upload("EnterYmmFrag-CarQuery")
                    rootView.layoutEnterYmm.makeSnack(R.string.parse_error, SNACK_WARNING)
                }
            }
        }
    }

    private fun isSubmitting(proceeding: Boolean) {
        val rootView = view!!

        if (proceeding) {
            rootView.buttonConfirm.visibility = Button.INVISIBLE
            rootView.progressProceed.visibility = ProgressBar.VISIBLE
        } else {
            rootView.progressProceed.visibility = ProgressBar.GONE
            rootView.buttonConfirm.visibility = Button.VISIBLE
        }
    }
}