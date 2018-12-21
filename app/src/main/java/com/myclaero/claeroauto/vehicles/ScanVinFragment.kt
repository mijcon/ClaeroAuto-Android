package com.myclaero.claeroauto.vehicles

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.myclaero.claeroauto.*
import com.myclaero.claeroauto.utilities.PostTextWatcher
import com.parse.ParseException
import com.parse.ParseObject
import com.parse.ParseQuery
import kotlinx.android.synthetic.main.fragment_veh_add_scan.*
import kotlinx.android.synthetic.main.fragment_veh_add_scan.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class ScanVinFragment : Fragment() {

    companion object {
        val regexVin = Regex("[^A-HJ-NPR-Z0-9]")
        var inputMgr: InputMethodManager? = null

        var vehVin: String? = ""
        val uriTempVin = listOf("vin", ".jpg")
        var vehNickname: String? = ""
        var vehJson: JSONObject? = null
        var vinPhotoUri: Uri? = null

        const val URL_API_VIN = "https://vindecoder.p.mashape.com/v2.0/decode_vin?vin=%s"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inputMgr = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_veh_add_scan, container, false)
        val bundle = arguments

        rootView.buttonScan.setOnClickListener { scanVin(buttonScan) }
        rootView.buttonConfirm.setOnClickListener { submitVeh(buttonConfirm) }
        rootView.textInstr2.setOnClickListener {
            AlertDialog.Builder(activity!!.applicationContext)
            .setView(layoutInflater.inflate(R.layout.dialog_vin_location, null))
            .setNegativeButton("Okay") { dialog, _ -> dialog.cancel() }
            .setTitle("VIN Locations").create().show()
        }
        rootView.editVin.addTextChangedListener(object : PostTextWatcher() {
            var cursorPosition = 0
            var isReplaced = false

            override fun afterTextChanged(p0: Editable?) {
                // Since text is going to change, let's lock the confirm button and clear the vehicle info
                buttonConfirm.isEnabled = false
                textTitle.visibility = TextView.GONE


                // We'll start to correct the VIN
                if (p0 != null) {
                    if (isReplaced) {
                        // If we just replaced the Text, we'll just move the cursor back to where it should be.
                        editVin.setSelection(cursorPosition)
                        isReplaced = false
                    } else {
                        // If it wasn't just replaced, we're going to compare pre- and post-regex strings
                        val vinString = p0.toString()
                        val vinCleaned = vinString
                            .toUpperCase()
                            .replace(Regex("I"), "1")
                            .replace(Regex("[QO]"), "0")
                            .replace(regexVin, "")
                        if (vinString != vinCleaned) {
                            // They don't match. But before we change it, we'll save where the cursor is.
                            cursorPosition = editVin.selectionStart
                            // Let the loop know that WE are the ones that made this specific change.
                            isReplaced = true
                            // And replace the Text
                            editVin.setText(vinCleaned)
                        }
                    }

                    if (p0.length == 17) {
                        // And if it happens to be a full VIN, we'll go ahead and process it.
                        vehVin = p0.toString()
                        inputMgr?.hideSoftInputFromWindow(editVin.windowToken, 0)
                        decodeVin(p0.toString())
                    }
                }
            }
        })

        return rootView
    }

    private fun scanVin(v: View) {
        val rootView = view!!

        // Check permissions first
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // No permission? Let's ask
            requestPermissions(arrayOf(Manifest.permission.CAMERA), PR_CODE_CAM)
        } else {
            // Already have Permission
            try {
                // Prepare Uri Extra
                val vinPhotoFile = File.createTempFile(uriTempVin[0], uriTempVin[1], activity!!.cacheDir)
                vinPhotoUri = FileProvider.getUriForFile(activity!!, FILE_PROVIDER_AUTH, vinPhotoFile)

                // Start up camera Intent
                val camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                camIntent.putExtra(MediaStore.EXTRA_OUTPUT, vinPhotoUri)
                startActivityForResult(camIntent, AR_CODE_ADD_VEH_CAM)
            } catch (e: Exception) {
                rootView.layoutAddVehicle.makeSnack(R.string.cam_fail_error, SNACK_ERROR)
                e.upload("ScanVin-TempDir", null)
            }
        }
    }

    private fun submitVeh(v: View) {
        val rootView = view!!
        inputMgr?.hideSoftInputFromWindow(editVin.windowToken, 0)
        isSubmitting(true)

        doAsync {
            try {
                // Look for a deactivated vehicle and activate it.
                ParseQuery<ParseObject>("Vehicle")
                    .whereEqualTo("vin", vehVin)
                    .first
                    .apply {
                        put("isActive", true)
                        save()
                    }
                uiThread { activity!!.finish() }
            } catch (e: ParseException) {
                if (e.code == ParseException.OBJECT_NOT_FOUND) {
                    // Never had this vehicle
                    val vehSpecs = vehJson!!.getJSONObject("specification")

                    try {
                        ParseObject("Vehicle").apply {
                            put("nickname", vehNickname!!)
                            put("isActive", true)
                            vehSpecs.keys().forEach {
                                put(it, vehSpecs.get(it))
                            }
                        }.save()
                        uiThread { activity!!.finish() }
                    } catch (e: Exception) {
                        uiThread {
                            isSubmitting(false)
                            rootView.layoutAddVehicle.makeSnack(R.string.parse_error, SNACK_ERROR)
                            e.upload("ScanVinFrag-AddVeh", "VIN: $vehVin")
                        }
                    }
                } else {
                    uiThread {
                        isSubmitting(false)
                        rootView.layoutAddVehicle.makeSnack(R.string.parse_error, SNACK_ERROR)
                        e.upload("ScanVinFrag-QueryProceed", "VIN: $vehVin")
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val rootView = view!!

        if (requestCode == AR_CODE_ADD_VEH_CAM && resultCode == Activity.RESULT_OK) {
            try {
                val vinDetector = BarcodeDetector.Builder(activity)
                    .setBarcodeFormats(Barcode.CODE_39)
                    .build()

                activity!!.contentResolver.notifyChange(vinPhotoUri!!, null)

                val scanBitmap =
                    android.provider.MediaStore.Images.Media.getBitmap(activity!!.contentResolver, vinPhotoUri)
                val scanFrame = Frame.Builder().setBitmap(scanBitmap).build()
                val vinBarcodes = vinDetector.detect(scanFrame)

                if (vinBarcodes.size() > 0) {
                    // TODO make a for-loop to skip until it finds a valid code
                    vehVin = vinBarcodes.valueAt(0).rawValue.replace(regexVin, "")
                    rootView.editVin.setText(vehVin)
                } else {
                    rootView.layoutAddVehicle.makeSnack(R.string.scanner_no_vin, SNACK_WARNING)
                }
            } catch (e: Exception) {
                rootView.layoutAddVehicle.makeSnack(R.string.vin_reader_fail, SNACK_ERROR)
                e.upload("AddVehAct-ScanResult", "URI: $vinPhotoUri")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val rootView = view!!

        if (requestCode == PR_CODE_CAM) {
            // If request is cancelled, the result arrays are empty.
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                scanVin(rootView.buttonScan)
            }
        }
    }

    fun decodeVin(vin: String) {
        val rootView = view!!
        isDecoding(true)

        doAsync {
            try {
                // Check Parse to see if this Client already has this vehicle registered previously
                val vehQuery = ParseQuery<ParseObject>("Vehicle")
                    .whereEqualTo("vin", vin)
                    .first

                // If it didn't throw an Exception, we got a result. Make the result's JSON our new JSON.
                vehJson = vehQuery.getJSONObject("json")

                // Now, we'll decide how to "present" this information.
                uiThread {
                    if (vehQuery.getBoolean("isActive")) {
                        // You already have this vehicle registered, ya dingus!
                        rootView.layoutAddVehicle.makeSnack(R.string.decode_has_vin, SNACK_WARNING)
                        isDecoding(false)
                    } else {
                        // We have it on our end, but it's inactive. Act like it's a new vehicle.
                        isDecoding(false, true)
                    }
                }

            } catch (e: ParseException) {
                // ParseSDK tells us if the Exception is simply "no results"
                if (e.code == ParseException.OBJECT_NOT_FOUND) {
                    try {
                        val vinUrl = URL(String.format(URL_API_VIN, vin))
                        val vinCxn = (vinUrl.openConnection() as HttpsURLConnection).apply {
                            requestMethod = "GET"
                            addRequestProperty("X-Mashape-Key", KEY_MASHAPE)
                            addRequestProperty("Accept", "application/json")
                        }

                        when (vinCxn.responseCode) {
                            200 ->      // Good response, keep going
                                vehJson = JSONObject(vinCxn.readAll())
                            500 ->      // Seems to be invalid
                                uiThread {
                                    e.upload("ScanVinFrag-VinCxn")
                                    rootView.layoutAddVehicle.makeSnack(R.string.decode_invalid_vin, SNACK_WARNING)
                                    isDecoding(false)
                                }
                            else ->     // Something went wrong
                                throw Exception("VinDecoder Failure. Response: ${vinCxn.responseCode}")
                        }
                        vinCxn.disconnect()
                    } catch (e: Exception) {
                        uiThread {
                            e.upload("ScanVinFrag-VinCxn")
                            rootView.layoutAddVehicle.makeSnack(R.string.decode_vin_error, SNACK_ERROR)
                            isDecoding(false)
                        }
                    }
                } else {
                    uiThread {
                        e.upload("ScanVinFrag-QueryProcess", vin)
                        rootView.layoutAddVehicle.makeSnack(R.string.decode_vin_error, SNACK_WARNING)
                        isDecoding(false)
                    }
                }
            }

            buildNickname(vehJson!!)
            uiThread { isDecoding(false) }
        }
    }

    private fun buildNickname(vehJson: JSONObject) {
        val vehSpec = vehJson.getJSONObject("specification")

        listOf("year", "make", "model").forEach { vehNickname += vehSpec?.getString(it) + " " }

        val trim = vehSpec?.getString("trim_level")
        if (trim.isNullOrBlank() || trim?.toLowerCase() == "base") {
            vehNickname = vehNickname!!.dropLast(1)
        } else {
            vehNickname += trim
        }
    }

    private fun isDecoding(processing: Boolean, unlock: Boolean = false) {
        val rootView = view!!

        if (processing) {
            vehJson = null
            vehNickname = ""
            rootView.textTitle.text = vehNickname
            rootView.progressVin.visibility = ProgressBar.VISIBLE
            buttonConfirm.isEnabled = false
        } else {
            rootView.textTitle.text = vehNickname
            rootView.progressVin.visibility = ProgressBar.GONE
            if (unlock) rootView.buttonConfirm.isEnabled = true
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