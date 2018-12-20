package com.myclaero.claeroauto

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.myclaero.claeroauto.payments.AddBankActivity
import com.myclaero.claeroauto.payments.AddCardActivity
import com.myclaero.claeroauto.scheduling.AddServiceActivity
import com.myclaero.claeroauto.vehicles.AddVehicleDemoActivity
import com.myclaero.claeroauto.vehicles.VehicleListAdapter
import com.myclaero.claeroauto.welcome.IntroActivity
import com.myclaero.claeroauto.welcome.LoginActivity
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
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
                        drawer_layout.makeSnack(R.string.logout_error, SNACK_ERROR)
                    }
                }
                return true
            }
            R.id.delete_acct -> {
                ParseUser.getCurrentUser().deleteInBackground { e ->
                    if (e == null) {
                        startActivity(intentFor<LoginActivity>("login" to true).newTask().clearTask())
                    } else {
                        e.upload("mainAct-deleteUser", "User: ${ParseUser.getCurrentUser().objectId}")
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
                    e.upload("MainAct-RefVeh", null)
                }
                swipeRefresh.isRefreshing = false
            }
        }
    }
}
