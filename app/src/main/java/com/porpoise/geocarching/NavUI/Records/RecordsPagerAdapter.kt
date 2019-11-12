package com.porpoise.geocarching.NavUI.Records

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter


class RecordsPagerAdapter(fm: FragmentManager, private val numberOfTabs: Int): FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return when(position) {
            0 -> RecordsVisitsFragment()
            1 -> RecordsPlacedFragment()
            else -> RecordsVisitsFragment()
        }
    }

    override fun getCount(): Int {
        return numberOfTabs
    }


}