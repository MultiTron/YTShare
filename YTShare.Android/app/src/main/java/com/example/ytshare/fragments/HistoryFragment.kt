package com.example.ytshare.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ytshare.Constants
import com.example.ytshare.MainActivity
import com.example.ytshare.R
import com.example.ytshare.adapters.VideoInfoAdapter
import com.example.ytshare.helpers.SharedPrefHelper
import com.google.android.material.button.MaterialButton

class HistoryFragment : Fragment() {

    private lateinit var mainActivity : MainActivity
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VideoInfoAdapter
    private lateinit var sortButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivity = (activity as MainActivity)
        sortButton = view.findViewById(R.id.sort_button)
        recyclerView = view.findViewById(R.id.recycler_view)

        recyclerView.layoutManager = LinearLayoutManager(mainActivity)
        adapter = VideoInfoAdapter(mainActivity.db.getAllLinks())
        recyclerView.adapter = adapter

        initSort()

        view.findViewById<Button>(R.id.delete_button).setOnClickListener {
            AlertDialog.Builder(mainActivity)
                .setMessage(R.string.delete_popup)
                .setPositiveButton("Yes") { _, _ /*button*/ ->
                    mainActivity.db.deleteAll()
                    adapter.clear()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.cancel() }
                .show()
        }

        sortButton.setOnClickListener {
            changeSort()
        }
    }

    private fun initSort() {
        val isDesc = mainActivity.sharedPref.getBoolean(Constants.isHistoryDesc, false)
        changeSort(!isDesc)
    }

    private fun changeSort(isDesc:Boolean) {
        if (isDesc) {
            ascSort()
        }
        else {
            descSort()
        }
    }

    private fun changeSort() {
        val isDesc = mainActivity.sharedPref.getBoolean(Constants.isHistoryDesc, false)
        if (isDesc) {
            ascSort()
        }
        else {
            descSort()
        }
    }

    private fun descSort() {
        adapter.descSort()
        SharedPrefHelper.saveSort(true, mainActivity.sharedPref)
        sortButton.setIconResource(R.drawable.outline_clock_arrow_up_24)
    }

    private fun ascSort() {
        adapter.ascSort()
        SharedPrefHelper.saveSort(false, mainActivity.sharedPref)
        sortButton.setIconResource(R.drawable.outline_clock_arrow_down_24)
    }
}