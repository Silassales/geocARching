package com.porpoise.geocarching.NavUI.Records

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.porpoise.geocarching.R
import com.porpoise.geocarching.firebaseObjects.UserVisit
import com.porpoise.geocarching.MainActivity


class Records : Fragment() {
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.fragment_records, container, false)

        tabLayout = view.findViewById(R.id.records_tab_layout)
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        viewPager = view.findViewById(R.id.records_pager)
        fragmentManager?.let { safeFm ->
            val adapter = RecordsPagerAdapter(safeFm, tabLayout.tabCount)
            viewPager.adapter = adapter
            viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        }

        tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                viewPager.currentItem = tab?.position ?: 0
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }
        })
        return view
    }
}

