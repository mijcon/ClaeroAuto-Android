package com.myclaero.claeroauto.vehicles

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.myclaero.claeroauto.*
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import kotlinx.android.synthetic.main.activity_add_vehicle.*
import org.json.JSONObject
import java.io.File

class AddVehicleActivity : AppCompatActivity() {

    private val uriTempVin = listOf("vin", ".jpg")
    val regexVin = Regex("[^A-HJ-NPR-Z0-9]")

    private var vehVin: String? = ""
    private var vehNickname: String? = ""
    private var vehJson: JSONObject? = null
    private var vehSpec: JSONObject? = null
    private var vinPhotoUri: Uri? = null
    private var inputMgr: InputMethodManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_vehicle)

        inputMgr = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        editVin.addTextChangedListener(object : TextWatcher {
            var cursorPosition = 0
            var isReplaced = false
            override fun afterTextChanged(p0: Editable?) {
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
                        inputMgr!!.hideSoftInputFromWindow(editVin.windowToken, 0)
                        //DecodeVin().execute(vehVin)
                    }
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // Since text is going to change, let's lock the confirm button and clear the vehicle info
                buttonConfirm.setTextColor(
                    ContextCompat.getColor(
                        applicationContext,
                        R.color.colorBlueFaded
                    )
                )
                buttonConfirm.isEnabled = false
                textTitle.visibility = TextView.GONE
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })
    }

    fun scanVin(v: View?) {
        // Check permissions first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // No permission? Let's ask
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PR_CODE_CAM)
        } else {
            // Already have Permission
            try {
                // Prepare Uri Extra
                val vinPhotoFile = File.createTempFile(uriTempVin[0], uriTempVin[1], cacheDir)
                vinPhotoUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTH, vinPhotoFile)

                // Start up camera Intent
                val camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                camIntent.putExtra(MediaStore.EXTRA_OUTPUT, vinPhotoUri)
                startActivityForResult(camIntent, AR_CODE_ADD_VEH_CAM)
            } catch (e: Exception) {
                MainActivity().makeSnack(this, layoutAddVehicle, R.string.cam_fail_error, SNACK_ERROR)
                MainActivity().uploadError("scanVin-tempDir", e, null)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AR_CODE_ADD_VEH_CAM && resultCode == Activity.RESULT_OK) {
            try {
                val vinDetector = BarcodeDetector.Builder(this)
                    .setBarcodeFormats(Barcode.CODE_39)
                    .build()
                this.contentResolver.notifyChange(vinPhotoUri!!, null)
                val vinBarcodes = vinDetector
                    .detect(
                        Frame.Builder().setBitmap(
                            android.provider.MediaStore.Images.Media.getBitmap(this.contentResolver, vinPhotoUri)
                        ).build()
                    )
                if (vinBarcodes.size() > 0) {
                    vehVin = vinBarcodes.valueAt(0).rawValue.replace(regexVin, "")
                    editVin.setText(vehVin)
                } else {
                    MainActivity().makeSnack(this, layoutAddVehicle, R.string.scanner_no_vin, SNACK_WARNING)
                }
            } catch (e: Exception) {
                MainActivity().makeSnack(this, layoutAddVehicle, R.string.vin_reader_fail, SNACK_ERROR)
                MainActivity().uploadError("AddVehAct-ScanResult", e, "URI: $vinPhotoUri")
            }
        }
    }

    fun proceedVin(v: View) {
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
                        this@AddVehicleActivity.finish()
                    } else {
                        buttonConfirm.visibility = Button.VISIBLE
                        MainActivity().makeSnack(
                            this,
                            layoutAddVehicle,
                            R.string.parse_error,
                            SNACK_ERROR
                        )
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
                ParseObject("Vehicle").apply {
                    put("owner", ParseUser.getCurrentUser())
                    put("nickname", vehNickname!!)
                    put("json", vehJson!!)
                    put("isActive", true)
                    for (datum in data) put(datum, vehSpec!!.get(datum))
                    saveInBackground {
                        progressProceed.visibility = ProgressBar.GONE
                        if (it == null) {
                            this@AddVehicleActivity.finish()
                        } else {
                            buttonConfirm.visibility = Button.VISIBLE
                            MainActivity().makeSnack(
                                applicationContext,
                                layoutAddVehicle,
                                R.string.parse_error,
                                SNACK_ERROR
                            )
                            MainActivity().uploadError("AddVehAct-Proceed", it, "VIN: $vehVin")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            progressProceed.visibility = ProgressBar.GONE
            buttonConfirm.visibility = Button.VISIBLE
            MainActivity().uploadError("AddVehAct-Proceed", e, "VIN: $vehVin")
            MainActivity().makeSnack(this, layoutAddVehicle, R.string.parse_error, SNACK_ERROR)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PR_CODE_CAM) {
            // If request is cancelled, the result arrays are empty.
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                scanVin(findViewById(R.id.buttonScan))
            }
        }
    }
}