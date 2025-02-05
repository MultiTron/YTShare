package com.example.ytshare

import android.content.DialogInterface
import android.content.res.Resources
import android.content.res.Resources.Theme
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryFragment : Fragment() {

    private lateinit var mainActivity : MainActivity
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CustomAdapter

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
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(mainActivity)
        adapter = CustomAdapter(mainActivity.db.getAllLinks())
        recyclerView.adapter = adapter

        view.findViewById<Button>(R.id.delete_button).setOnClickListener {
            AlertDialog.Builder(mainActivity)
                .setMessage(R.string.delete_popup)
                .setPositiveButton("Yes") { dialog, button ->
                    mainActivity.db.deleteAll()
                    adapter.clear()
                }
                .setNegativeButton("No") { dialog, button ->
                    dialog.cancel() }
                .show()
        }
    }
}