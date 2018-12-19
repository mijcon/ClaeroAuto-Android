package com.myclaero.claeroauto

import android.content.Context
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.myclaero.claeroauto.payments.AddBankActivity
import com.myclaero.claeroauto.payments.AddCardActivity
import com.myclaero.claeroauto.scheduling.AddServiceActivity
import com.myclaero.claeroauto.vehicles.AddVehicleDemoActivity
import com.myclaero.claeroauto.vehicles.VehicleListAdapter
import com.myclaero.claeroauto.welcome.IntroActivity
import com.myclaero.claeroauto.welcome.LoginActivity
import com.parse.ParseException
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import com.parse.ktx.putOrIgnore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val vehAdapter = VehicleListAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ParseUser.getCurrentUser() == null) {
            if (defaultSharedPreferences.getBoolean("logged_in_prev", false)) {
                startActivity(intentFor<LoginActivity>("login" to true).newTask().clearTask())
            } else {
                startActivity(intentFor<IntroActivity>().newTask().clearTask())
            }
        } else {
            setContentView(R.layout.activity_main)
            setSupportActionBar(toolbar)

            /*
            fab.setOnClickListener {
                val toAddVehicleActivity = Intent(this, AddVehicleActivity::class.java)
                startActivity(toAddVehicleActivity)
            }
            */

            /*
            val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
            )
            drawer_layout.addDrawerListener(toggle)
            toggle.syncState()

            nav_view.setNavigationItemSelectedListener(this)
            */

            listVehicles.apply {
                setHasFixedSize(true)
                adapter = vehAdapter
                layoutManager = LinearLayoutManager(this@MainActivity)
            }
            swipeRefresh.apply {
                setOnRefreshListener { refreshVehicles() }
                setColorSchemeResources(R.color.colorBlue)
            }
            refreshVehicles()
        }
    }

    override fun onBackPressed() {
        //if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
        //    drawer_layout.closeDrawer(GravityCompat.START)
        //} else {
        super.onBackPressed()
        //}
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            R.id.action_intro -> {
                startActivity<IntroActivity>()
                return true
            }
            R.id.log_out -> {
                ParseUser.logOutInBackground { e ->
                    if (e == null) {
                        Toast.makeText(applicationContext, "Successfully logged out!", Toast.LENGTH_SHORT).show()
                        startActivity(intentFor<LoginActivity>("login" to true).newTask().clearTask())
                    } else {
                        makeSnack(applicationContext, drawer_layout, R.string.logout_error, SNACK_ERROR)
                    }
                }
                return true
            }
            R.id.delete_acct -> {
                ParseUser.getCurrentUser().deleteInBackground { e ->
                    if (e == null) {
                        startActivity(intentFor<LoginActivity>("login" to true).newTask().clearTask())
                    } else {
                        uploadError("mainAct-deleteUser", e, "User: ${ParseUser.getCurrentUser().objectId}")
                    }
                }
                return true
            }
            R.id.action_schedule -> {
                startActivity<AddServiceActivity>()
                return true
            }
            R.id.action_add_veh -> {
                startActivity<AddVehicleDemoActivity>()
                return true
            }
            R.id.action_add_bank -> {
                startActivity<AddBankActivity>()
                return true
            }
            R.id.action_add_card -> {
                startActivity<AddCardActivity>()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_camera -> {
                // Handle the camera action
            }
            R.id.nav_gallery -> {

            }
            R.id.nav_slideshow -> {

            }
            R.id.nav_manage -> {

            }
            R.id.nav_share -> {

            }
            R.id.nav_send -> {

            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun refreshVehicles() {
        ParseQuery.getQuery<ParseObject>("Vehicle").apply {
            whereEqualTo("isActive", true)
            findInBackground { scoreList, e ->
                if (e == null) {
                    vehAdapter.setList(scoreList)
                    vehAdapter.notifyDataSetChanged()
                } else {
                    // Report error
                    MainActivity().uploadError("MainAct-RefVeh", e, null)
                }
                swipeRefresh.isRefreshing = false
            }
        }
    }

    fun uploadError(point: String, e: Exception?, extra: String?) {
        // If this is a ParseException, let's grab the Code
        try {
            ParseObject("Error").apply {
                put("codeSection", point)
                put("owner", ParseUser.getCurrentUser().objectId)
                putOrIgnore("stackTrace", e?.stackTrace)
                putOrIgnore("extra", extra)
                putOrIgnore("parseCode", (e as ParseException).message)
                saveEventually()
            }
        } catch (e: Exception) {
            Log.e("ClaeroParse", "Parse is having some problems. We can't upload any error messages!", e)
        }
    }

    fun makeSnack(context: Context, view: View, msg: Int, error_type: Int? = null) {
        Snackbar.make(view, msg, Snackbar.LENGTH_LONG).apply {
            setActionTextColor(
                ContextCompat.getColor(
                    context,
                    when (error_type) {
                        SNACK_WARNING -> R.color.colorTextWarning
                        SNACK_ERROR -> R.color.colorTextError
                        else -> R.color.colorTextSuccess
                    }
                )
            )
            this.view.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    when (error_type) {
                        SNACK_WARNING -> R.color.colorWarning
                        SNACK_ERROR -> R.color.colorError
                        else -> R.color.colorSuccess
                    }
                )
            )
            show()
        }
    }
}
