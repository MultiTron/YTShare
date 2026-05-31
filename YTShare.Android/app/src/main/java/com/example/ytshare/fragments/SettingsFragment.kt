package com.example.ytshare.fragments

import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import com.example.ytshare.Constants
import com.example.ytshare.MainActivity
import com.example.ytshare.R
import com.example.ytshare.helpers.SharedPrefHelper

class SettingsFragment : Fragment() {

    private lateinit var mainActivity : MainActivity
    private lateinit var isTrackingCheckBox: CheckBox

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivity = (activity as MainActivity)

        isTrackingCheckBox = view.findViewById(R.id.remove_tracking_checkbox)

        isTrackingCheckBox.isChecked = mainActivity.sharedPref.getBoolean(Constants.isTracking, false)

        isTrackingCheckBox.setOnClickListener {
            SharedPrefHelper.savePref(isTrackingCheckBox.isChecked, mainActivity.sharedPref)
        }
    }
}
