package com.myclaero.claeroauto.vehicles

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.app.AppCompatActivity
import com.myclaero.claeroauto.R
import kotlinx.android.synthetic.main.activity_add_vehicle_demo.*

class AddVehicleDemoActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_add_vehicle_demo)

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
		val mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

		// Add our two new
		mSectionsPagerAdapter.addFragment(ScanVinFragment())
		mSectionsPagerAdapter.addFragment(EnterYmmFragment())

		// Add Tabs to the TabLayout
		tabs.addTab(tabs.newTab().setText("Enter VIN"))
		tabs.addTab(tabs.newTab().setText("Select Model"))

		// Set up the ViewPager with the sections adapter.
		container.adapter = mSectionsPagerAdapter
		container.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabs))
		tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(container))
	}

	inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

		private val fragList = mutableListOf<Fragment>()

		fun addFragment(frag: Fragment) {
			fragList.add(frag)
		}

		override fun getItem(p0: Int): Fragment {
			return fragList[p0]
		}

		override fun getCount(): Int {
			return fragList.size
		}
	}
}
