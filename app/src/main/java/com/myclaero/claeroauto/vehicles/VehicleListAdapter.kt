package com.myclaero.claeroauto.vehicles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.myclaero.claeroauto.MainActivity
import com.myclaero.claeroauto.R
import com.myclaero.claeroauto.getString
import com.parse.ParseFile
import com.parse.ParseObject
import com.parse.ktx.getIntOrNull
import kotlinx.android.synthetic.main.card_vehicle.view.*
import kotlinx.android.synthetic.main.dialog_vehicle_edit.view.*

class VehicleListAdapter(context: Context) : RecyclerView.Adapter<VehicleListAdapter.ViewHolder>() {

	private var thisContext = context

	companion object {
		private val TAG = "VehicleListAdapter"
		private var vehicles = mutableListOf<ParseObject>()
		private var thumbnails = mutableListOf<Bitmap?>()
	}

	override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
		val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.card_vehicle, viewGroup, false)

		view.buttonVehNewServ.setOnClickListener(object : View.OnClickListener {
			override fun onClick(v: View?) {
				Toast.makeText(thisContext, "This feature is under development.", Toast.LENGTH_SHORT).show()
			}
		})

		view.buttonVehDetails.setOnClickListener(object : View.OnClickListener {
			override fun onClick(v: View?) {
				Toast.makeText(thisContext, "This feature is under development.", Toast.LENGTH_SHORT).show()
			}
		})

		return ViewHolder(view)
	}

	override fun onBindViewHolder(vh: ViewHolder, i: Int) {
		Log.i(TAG, "onBindViewHolder called!")

		val veh = vehicles!![i]

		vh.imageVehThumb.setImageBitmap(thumbnails[i])
		vh.textVehName.text = veh.getString("nickname")
		vh.textVehVin.text = veh.getString("vin")
		vh.textVehYmmt.text = String.format(
			"%s %s %s %s",
			veh.getString("year"),
			veh.getString("make"),
			veh.getString("model"),
			veh.getString("trim_level") ?: ""
		)

		vh.buttonVehEdit.setOnClickListener { editVeh(thisContext, veh, i) }
		vh.buttonVehDel.setOnClickListener { delVeh(thisContext, veh, i) }
	}

	override fun getItemCount(): Int { return vehicles?.size ?: 0 }

	// Whenever we want to refresh data, we can either build a new Adapter, or just refresh the data in the adapter.
	// I chose to do the later.
	fun setList(newList: MutableList<ParseObject>) {
		vehicles = newList
		thumbnails.clear()

		for (veh in vehicles) {
			// We're already in a background thread. Calling "getDataInBackground" lets other
			// functions begin too soon, leading to a possible IndexOutOfBoundsException.
			try {
				val thumb: ByteArray? = (veh.get("thumb") as ParseFile?)?.data
				thumbnails.add(
					if (thumb != null) BitmapFactory.decodeByteArray(thumb, 0, thumb.size) else null
				)
			} catch (e: Exception) {
				MainActivity().uploadError("VehList-Thumb", e, "Failed to download thumbnail.")
				thumbnails.add(null)
			}
		}
	}

	private fun delVeh(context: Context, vehicle: ParseObject, i: Int) {
		AlertDialog.Builder(context)
			.setMessage("Are you sure you want to delete your vehicle?")
			.setPositiveButton("Cancel") { dialog, _ -> dialog.cancel() }
			.setNegativeButton("Yes, delete") { _, _ ->
				// Set ProgressBar to visible.
				try {
					vehicle.put("isActive", false)
					vehicle.save()
					vehicles.removeAt(i)
					thumbnails.removeAt(i)
					this.notifyItemRemoved(i)
				} catch (e: Exception) {
					MainActivity().uploadError("EditVeh-Save", e, "VehId: ${vehicle.objectId}")
					Toast.makeText(context, "Details couldn't be saved. Sorry about that!", Toast.LENGTH_SHORT).show()
				}
			}
			.setTitle("Delete Vehicle").create().show()
	}

	private fun editVeh(context: Context, vehicle: ParseObject, i: Int) {
		val view = LayoutInflater.from(context).inflate(R.layout.dialog_vehicle_edit, null)

		view.editNickname.setText(vehicle.getString("nickname") ?: "")
		view.editLicense.setText(vehicle.getString("license") ?: "")
		view.spinnerState.setSelection(vehicle.getIntOrNull("licenseState") ?: 0)

		AlertDialog.Builder(context)
			.setView(view)
			.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
			.setPositiveButton("Confirm") { _, _ ->
				// Set ProgressBar to visible.
				try {
					vehicle.apply {
						put("nickname", view.editNickname.getString())
						put("license", view.editLicense.getString())
						put("licenseState", view.spinnerState.selectedItemId)
					}.save()
					this.notifyItemChanged(i)
				} catch (e: Exception) {
					MainActivity().uploadError("EditVeh-Save", e, "VehId: ${vehicle.objectId}")
					Toast.makeText(context, "Details couldn't be saved. Sorry about that!", Toast.LENGTH_SHORT).show()
				}
			}
			.setTitle("Update Vehicle").create().show()
	}

	inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

		val textVehName = itemView.textVehName!!
		val textVehVin = itemView.textVehVin!!
		val textVehYmmt = itemView.textVehYmmt!!
		val textVehPlan = itemView.textVehPlan!!
		val buttonVehNewServ = itemView.buttonVehNewServ!!
		val buttonVehEdit = itemView.buttonVehEdit!!
		val buttonVehDetails = itemView.buttonVehDetails!!
		val buttonVehDel = itemView.buttonVehDel!!
		val imageVehThumb = itemView.imageVehThumb!!

	}
}