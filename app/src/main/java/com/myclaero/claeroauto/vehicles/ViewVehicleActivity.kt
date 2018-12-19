package com.myclaero.claeroauto.vehicles

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.myclaero.claeroauto.R
import kotlinx.android.synthetic.main.activity_view_vehicle.*


class ViewVehicleActivity : AppCompatActivity() {

    var isExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_vehicle)

        //Start closed
        textVehExp.visibility = TextView.GONE
        buttonVehEdit.visibility = Button.GONE
        buttonVehDel.visibility = Button.GONE
    }

    fun expandCard(v: View) {
        if (isExpanded) {
            textVehExp.visibility = TextView.GONE
            buttonVehEdit.visibility = Button.GONE
            buttonVehDel.visibility = Button.GONE
        } else {
            textVehExp.visibility = TextView.VISIBLE
            buttonVehEdit.visibility = Button.VISIBLE
            buttonVehDel.visibility = Button.VISIBLE
        }
        isExpanded = !isExpanded
    }
}
