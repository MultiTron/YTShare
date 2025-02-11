package com.example.ytshare.fragments

import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ytshare.Constants
import com.example.ytshare.MainActivity
import com.example.ytshare.R
import com.example.ytshare.adapters.HostAdapter
import com.example.ytshare.helpers.SharedPrefHelper

class SettingsFragment : Fragment() {

    private lateinit var mainActivity : MainActivity
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HostAdapter
    private lateinit var isTrackingCheckBox: CheckBox

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivity = (activity as MainActivity)
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(mainActivity)
        adapter = HostAdapter(mainActivity.nsd.addresses, mainActivity)
        recyclerView.adapter = adapter

        isTrackingCheckBox = view.findViewById(R.id.remove_tracking_checkbox)

        isTrackingCheckBox.isChecked = mainActivity.sharedPref.getBoolean(Constants.isTracking, false)

        isTrackingCheckBox.setOnClickListener {
            SharedPrefHelper.savePref(isTrackingCheckBox.isChecked, mainActivity.sharedPref)
        }
    }
}