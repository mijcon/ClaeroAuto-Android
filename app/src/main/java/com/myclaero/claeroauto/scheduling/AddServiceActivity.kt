package com.myclaero.claeroauto.scheduling

import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.myclaero.claeroauto.MainActivity
import com.myclaero.claeroauto.R
import com.parse.FindCallback
import com.parse.ParseException
import com.parse.ParseObject
import com.parse.ParseQuery
import kotlinx.android.synthetic.main.activity_add_service.*
import java.util.*

class AddServiceActivity : AppCompatActivity() {

    val msInDay = 86400000

    var timesMorning = mutableListOf<String>()
    var timesAfternoon = mutableListOf<String>()
    var timesEvening = mutableListOf<String>()
    var serviceDur = 60

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_service)

        val currentTime = Calendar.getInstance().timeInMillis
        calendarView.minDate = currentTime
        calendarView.maxDate = currentTime + (7L * 4L * msInDay)
        calendarView.setOnDateChangeListener { p0, p1, p2, p3 ->
            val currentDay = Calendar.getInstance()
            currentDay.set(p1, p2, p3)
            CheckDate().execute(currentDay)
        }
    }

    inner class CheckDate : AsyncTask<Calendar, Void, List<String>?>() {
        override fun onPreExecute() {
            super.onPreExecute()

            layoutHeader.visibility = LinearLayout.GONE
        }

        override fun doInBackground(vararg p0: Calendar): List<String>? {
            var listTimes: List<String>? = mutableListOf()
            val ticketQParse = ParseQuery.getQuery<ParseObject>("Openings")
            ticketQParse.whereGreaterThanOrEqualTo("date", p0[0].timeInMillis)
            ticketQParse.whereLessThan("date", p0[0].timeInMillis + msInDay)
            /*
            ticketQParse.findInBackground { openings, e ->
            ticketQParse.findInBackground { openings, e ->
                if (e == null) {
                    if (!openings.isNullOrEmpty()) {
                        for (opening in 0..openings.size) {
                            val blocks = serviceDur / 30  // Total number of half-hour blocks needed to book this appointment
                            for (block in 1..blocks) {
                                if
                            }
                        }
                    }
                } else {
                    MainActivity().uploadError("CheckDate", e, "Date: ${p0[0].timeInMillis}")
                    listTimes = null
                }
            }*/
            return listTimes
        }

        override fun onPostExecute(result: List<String>?) {
            super.onPostExecute(result)

            // TODO make it kick out into three ListViews

            layoutHeader.visibility = LinearLayout.VISIBLE
        }
    }
}
