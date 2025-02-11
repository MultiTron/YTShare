package com.example.ytshare.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.ytshare.MainActivity
import com.example.ytshare.R
import com.example.ytshare.helpers.SharedPrefHelper
import com.example.ytshare.models.HostModel

class HostAdapter(private var list: List<HostModel>, private var activity: MainActivity) : RecyclerView.Adapter<HostAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val nameView = itemView.findViewById<TextView>(R.id.name_text)
        val addressView = itemView.findViewById<TextView>(R.id.address_text)
        val cardView = itemView.findViewById<CardView>(R.id.host_card_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_view_host, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.nameView.text = item.hostName
        holder.addressView.text = item.toString()

        holder.cardView.setOnClickListener {
            SharedPrefHelper.saveIp(item.toString(), activity.sharedPref)
            Toast.makeText(activity, "Setting selected ip...", Toast.LENGTH_SHORT).show()
        }
    }
}