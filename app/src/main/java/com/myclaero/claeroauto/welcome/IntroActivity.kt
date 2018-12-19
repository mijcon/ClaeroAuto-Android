package com.myclaero.claeroauto.welcome

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import com.myclaero.claeroauto.AR_CODE_VERIFY_EMAIL
import com.myclaero.claeroauto.MainActivity
import com.myclaero.claeroauto.R
import kotlinx.android.synthetic.main.activity_pager.*
import kotlinx.android.synthetic.main.fragment_intro.view.*
import org.jetbrains.anko.clearTask
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.newTask

class IntroActivity : AppCompatActivity() {

    var sectionsPagerAdapter: SectionsPagerAdapter? = null
    var tintActive: ColorStateList? = null
    var tintInactive: ColorStateList? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pager)

        tintActive = ColorStateList.valueOf(ContextCompat.getColor(applicationContext, R.color.colorWhite))
        tintInactive = ColorStateList.valueOf(ContextCompat.getColor(applicationContext, R.color.colorWhiteFaded))

        imageExit.imageTintList = tintActive

        sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        setupViewPager(viewPager, sectionsPagerAdapter!!)
        (layoutTabs.getChildAt(0) as ImageView).imageTintList = tintActive
    }

    fun exitIntro(v: View) {
        startActivity(intentFor<LoginActivity>().newTask().clearTask())
    }

    // Todo switch verification fragment for a "next steps" fragment
    private fun setupViewPager(viewPager: ViewPager, sectionsPagerAdapter: SectionsPagerAdapter) {

        val fragTechnology = IntroFragment()
        val fragConvenience = IntroFragment()
        val fragPeaceOfMind = IntroFragment()
        val fragEfficiency = IntroFragment()
        val fragContinue = ContinueFragment()

        fragTechnology.arguments = Bundle().apply {
            putString("header", resources.getString(R.string.intro_welcome_header))
            putInt("image", R.drawable.ic_lg_smartphone_black_60dp)
            putString("subheader", resources.getString(R.string.intro_technology_header))
            putString("description", resources.getString(R.string.intro_technology_description))
            putString("swipe", resources.getString(R.string.intro_swipe))
        }
        fragConvenience.arguments = Bundle().apply {
            putInt("image", R.drawable.ic_lg_route_black_60dp)
            putString("subheader", resources.getString(R.string.intro_convenience_subheader))
            putString("description", resources.getString(R.string.intro_convenience_description))
        }
        fragPeaceOfMind.arguments = Bundle().apply {
            putInt("image", R.drawable.ic_lg_umbrella_black_60dp)
            putString("subheader", resources.getString(R.string.intro_peace_subheader))
            putString("description", resources.getString(R.string.intro_peace_description))
        }
        fragEfficiency.arguments = Bundle().apply {
            putInt("image", R.drawable.ic_lg_wind_black_60dp)
            putString("subheader", resources.getString(R.string.intro_efficiency_subheader))
            putString("description", resources.getString(R.string.intro_efficiency_description))
        }
        fragContinue.arguments = Bundle().apply {
            putInt("image", R.drawable.ic_lg_user_black_60dp)
            putString("subheader", resources.getString(R.string.intro_continue_subheader))
            putString("description", resources.getString(R.string.intro_continue_description))
        }

        sectionsPagerAdapter.addFragments(
            fragTechnology,
            fragConvenience,
            fragPeaceOfMind,
            fragEfficiency,
            fragContinue
        )

        viewPager.adapter = sectionsPagerAdapter
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {
            }

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
            }

            override fun onPageSelected(p0: Int) {
                for (p in 0 until layoutTabs.childCount) {
                    (layoutTabs.getChildAt(p) as ImageView).imageTintList = if (p == p0) tintActive else tintInactive
                }
            }
        })
    }

    // Todo move this out
    // Implement the FragmentPagerAdapter interface to accept a List of Fragments
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        private val fragList = mutableListOf<Fragment>()

        fun addFragments(vararg frags: Fragment) {
            for (frag in frags) {

                fragList.add(frag)

                val imageView = ImageView(this@IntroActivity).apply {
                    setImageDrawable(ContextCompat.getDrawable(this@IntroActivity, R.drawable.ic_dot_black_24dp))
                    imageTintList =
                            ColorStateList.valueOf(ContextCompat.getColor(applicationContext, R.color.colorWhiteFaded))
                    scaleX = 0.5f
                    scaleY = 0.5f
                }
                layoutTabs.addView(imageView)
            }
        }

        override fun getItem(p0: Int): Fragment {
            return fragList[p0]
        }

        override fun getCount(): Int {
            return fragList.size
        }
    }

    class IntroFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val rootView = inflater.inflate(R.layout.fragment_intro, container, false)
            val bundle = arguments

            rootView.textHeader.text = bundle?.getString("header", "")
            rootView.imageHeader.apply {
                setImageDrawable(
                    ContextCompat.getDrawable(
                        activity!!.applicationContext,
                        bundle!!.getInt("image", R.drawable.ic_lg_warning_black_60dp)
                    )
                )
                setColorFilter(ContextCompat.getColor(activity!!.applicationContext, R.color.colorWhite))
            }
            rootView.textSubheader.text = bundle?.getString("subheader", "")
            rootView.textDescription.text = bundle?.getString("description", "")
            rootView.textSwipe.text = bundle?.getString("swipe", "")

            return rootView
        }
    }

    class ContinueFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val rootView = inflater.inflate(R.layout.fragment_intro, container, false)
            val bundle = arguments

            rootView.textHeader.text = bundle?.getString("header", "")
            rootView.imageHeader.apply {
                setImageDrawable(
                    ContextCompat.getDrawable(
                        activity!!.applicationContext,
                        bundle!!.getInt("image", R.drawable.ic_lg_warning_black_60dp)
                    )
                )
                setColorFilter(ContextCompat.getColor(activity!!.applicationContext, R.color.colorWhite))
            }
            rootView.textSubheader.text = bundle?.getString("subheader", "")
            rootView.textDescription.text = bundle?.getString("description", "")
            rootView.textSwipe.text = bundle?.getString("swipe", "")

            // The SetupFragment will
            rootView.buttonContinue.visibility = Button.VISIBLE
            rootView.buttonContinue.setOnClickListener {
                val loginIntent = Intent(activity, LoginActivity::class.java)
                loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(loginIntent)
            }

            return rootView
        }
    }
}