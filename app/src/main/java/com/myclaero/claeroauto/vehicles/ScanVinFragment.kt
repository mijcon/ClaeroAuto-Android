package com.myclaero.claeroauto.vehicles

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import kotlinx.android.synthetic.main.fragment_veh_add_scan.*
import kotlinx.android.synthetic.main.fragment_veh_add_scan.view.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class ScanVinFragment : Fragment() {

	val regexVin = Regex("[^A-HJ-NPR-Z0-9]")
	var inputMgr: InputMethodManager? = null

	var vehVin: String? = ""
	val uriTempVin = listOf("vin", ".jpg")
	var vehNickname: String? = ""
	var vehJson: JSONObject? = null
	var vehSpec: JSONObject? = null
	var vinPhotoUri: Uri? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		inputMgr = activity!!.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val rootView = inflater.inflate(R.layout.fragment_veh_add_scan, container, false)
		val bundle = arguments

		rootView.buttonScan.setOnClickListener { scanVin(buttonScan) }
		rootView.buttonConfirm.setOnClickListener { proceedVin(buttonConfirm) }
		rootView.textInstr2.setOnClickListener { vinLocation(textInstr2) }
		rootView.editVin.addTextChangedListener(object : TextWatcher {
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
						inputMgr?.hideSoftInputFromWindow(editVin.windowToken, 0)
						DecodeVin().execute(vehVin)
					}
				}
			}

			override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
				// Since text is going to change, let's lock the confirm button and clear the vehicle info
				buttonConfirm.setTextColor(
					ContextCompat.getColor(
						activity!!.applicationContext,
						R.color.colorBlueFaded
					)
				)
				buttonConfirm.isEnabled = false
				textTitle.visibility = TextView.GONE
			}

			override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
			}
		})

		return rootView
	}

	private fun vinLocation(v: View) {
		AlertDialog.Builder(activity!!.applicationContext)
			.setView(layoutInflater.inflate(R.layout.dialog_vin_location, null))
			.setNegativeButton("Okay") { dialog, _ -> dialog.cancel() }
			.setTitle("VIN Locations").create().show()
	}

	private fun scanVin(v: View) {
		val rootView = view!!

		// Check permissions first
		if (ContextCompat.checkSelfPermission(
				activity!!,
				Manifest.permission.CAMERA
			) != PackageManager.PERMISSION_GRANTED) {
			// No permission? Let's ask
			requestPermissions(arrayOf(Manifest.permission.CAMERA), PR_CODE_CAM)
		} else {
			// Already have Permission
			try {
				// Prepare Uri Extra
				val vinPhotoFile = File.createTempFile(uriTempVin[0], uriTempVin[1], activity!!.cacheDir)
				vinPhotoUri =
						FileProvider.getUriForFile(activity!!, FILE_PROVIDER_AUTH, vinPhotoFile)

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

	private fun proceedVin(v: View) {
		val rootView = view!!
		inputMgr?.hideSoftInputFromWindow(editVin.windowToken, 0)

		rootView.buttonConfirm.visibility = Button.INVISIBLE
		rootView.progressProceed.visibility = ProgressBar.VISIBLE

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
					rootView.progressProceed.visibility = ProgressBar.GONE
					if (it == null) {
						activity!!.finish()
					} else {
						rootView.buttonConfirm.visibility = Button.VISIBLE
						MainActivity().makeSnack(
							activity!!,
							rootView.layoutAddVehicle,
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
				val vehObject = ParseObject("Vehicle").apply {
					put("owner", ParseUser.getCurrentUser())
					put("nickname", vehNickname!!)
					put("json", vehJson!!)
					put("isActive", true)
					for (datum in data) put(datum, vehSpec!!.get(datum))
				}
				vehObject.saveInBackground {
					rootView.progressProceed.visibility = ProgressBar.GONE
					if (it == null) {
						activity!!.finish()
					} else {
						rootView.buttonConfirm.visibility = Button.VISIBLE
						MainActivity().makeSnack(
							activity!!,
							rootView.layoutAddVehicle,
							R.string.parse_error,
							SNACK_ERROR
						)
						MainActivity().uploadError("AddVehAct-Proceed", it, "VIN: $vehVin")
					}
				}
			}
		} catch (e: Exception) {
			rootView.progressProceed.visibility = ProgressBar.GONE
			rootView.buttonConfirm.visibility = Button.VISIBLE
			MainActivity().uploadError("AddVehAct-Proceed", e, "VIN: $vehVin")
			MainActivity().makeSnack(activity!!, rootView.layoutAddVehicle, R.string.parse_error, SNACK_ERROR)
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
					MainActivity().makeSnack(
						activity!!,
						rootView.layoutAddVehicle,
						R.string.scanner_no_vin,
						SNACK_WARNING
					)
				}
			} catch (e: Exception) {
				MainActivity().makeSnack(activity!!, rootView.layoutAddVehicle, R.string.vin_reader_fail, SNACK_ERROR)
				MainActivity().uploadError("AddVehAct-ScanResult", e, "URI: $vinPhotoUri")
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

	inner class DecodeVin : AsyncTask<String, Unit, Int>() {

		private val rootView = view!!
		private var exceptionHandle: Exception? = null

		override fun onPreExecute() {
			super.onPreExecute()
			rootView.progressVin.visibility = ProgressBar.VISIBLE
		}

		override fun doInBackground(vararg p0: String?): Int {
			try {
				try {
					// Send Parse Query for Vehicle.
					val vehList = ParseQuery
						.getQuery<ParseObject>("Vehicle")
						.whereEqualTo("vin", p0[0])
						.find()
					when {
						vehList == null -> // Query was unsuccessful.
							return DECODE_PARSE_FAIL
						vehList.isNotEmpty() -> { // Query returned an item
							// If one of them is yours, we'll just stop everything and tell you so
							for (veh in vehList) {
								if (veh.getParseUser("owner")?.objectId == ParseUser.getCurrentUser().objectId
								    && veh.get("isActive") == true
								)
									return DECODE_MATCH_FOUND
							}
							// But if none of them are yours, or if you have a inactive record, we'll just proceed
							vehJson = vehList[0].getJSONObject("JSON")
							return DECODE_SUCCESS
						}
						else -> { // Query was successful, and no active vehicles found!
							val vinCxn =
								(URL(String.format(URL_API_VIN, p0[0])).openConnection() as HttpsURLConnection).apply {
									requestMethod = "GET"
									addRequestProperty("X-Mashape-Key", KEY_MASHAPE)
									addRequestProperty("Accept", "application/json")
								}
							when {
								vinCxn.responseCode == 500 -> // Most likely an invalid VIN
									return DECODE_INVALID_VIN
								vinCxn.responseCode != 200 -> // If we didn't get a good response, give up
									return DECODE_SERVER_FAIL
								else                       -> {
									// Good response, keep going
									val reader = BufferedReader(
										InputStreamReader(
											vinCxn.inputStream,
											"UTF-8"
										)
									)
									var result = ""
									var line = reader.readLine()
									while (line != null) {
										result += line
										line = reader.readLine()
									}
									vehJson = JSONObject(result)
									reader.close()
								}
							}
							vinCxn.disconnect()
							return DECODE_SUCCESS
						}
					}
				} catch (e: Exception) {
					exceptionHandle = e
					return DECODE_PARSE_FAIL
				}
			} catch (e: Exception) {
				exceptionHandle = e
				return DECODE_CONNECT_FAIL
			}
		}

		override fun onPostExecute(result: Int) {
			super.onPostExecute(result)

			rootView.progressVin.visibility = ProgressBar.INVISIBLE

			// Generate friendly name for vehicle if we have data
			if (vehJson != null) {
				vehSpec = vehJson!!.getJSONObject("specification")
				vehNickname = ""
				for (data in listOf(
					"year",
					"make",
					"model"
				)) vehNickname += vehSpec?.getString(data) + " "
				val trim = vehSpec?.getString("trim_level")
				if (trim.isNullOrBlank() || trim?.toLowerCase() == "base") {
					vehNickname += trim
				} else {
					vehNickname = vehNickname!!.dropLast(1)
				}
				rootView.textTitle.text = vehNickname
				// Show off that data!
				rootView.textTitle.visibility = TextView.VISIBLE
			}

			// Error builder
			if (result == DECODE_SUCCESS) {
				rootView.buttonConfirm.isEnabled = true
			} else {
				rootView.buttonConfirm.isEnabled = false
				// Build up our SnackBar with the right String and Color Scheme
				MainActivity().makeSnack(
					activity!!.applicationContext,
					rootView.layoutAddVehicle,
					when (result) {
						DECODE_CONNECT_FAIL -> R.string.decode_vin_error
						DECODE_INVALID_VIN  -> R.string.decode_invalid_vin
						DECODE_SERVER_FAIL  -> R.string.decode_vin_error
						DECODE_MATCH_FOUND  -> R.string.decode_has_vin
						else                -> R.string.parse_error
					},
					when (result) {
						DECODE_PARSE_FAIL, DECODE_CONNECT_FAIL -> SNACK_ERROR
						else                                   -> SNACK_WARNING
					}
				)
			}
			rootView.buttonConfirm.setTextColor(
				ContextCompat.getColor(
					activity!!.applicationContext,
					if (result == DECODE_SUCCESS) R.color.colorAccent else R.color.colorBlueFaded
				)
			)
			when (result) {
				DECODE_SERVER_FAIL, DECODE_CONNECT_FAIL ->
					MainActivity().uploadError("AddVehAct-Decode", exceptionHandle, "VIN: $vehVin")
				DECODE_PARSE_FAIL                       ->
					Log.e("ClaeroParse", "Failed to initialize ParseQuery!", exceptionHandle)
			}
		}
	}
}