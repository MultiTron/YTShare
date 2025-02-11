package com.example.ytshare.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ytshare.models.LinkModel
import com.example.ytshare.R
import com.squareup.picasso.Picasso

class VideoInfoAdapter(private var list : List<LinkModel>) : RecyclerView.Adapter<VideoInfoAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.thumbnail)
        val titleView: TextView = itemView.findViewById(R.id.name_text)
        val linkView: TextView = itemView.findViewById(R.id.link_text)
        val dateView: TextView = itemView.findViewById(R.id.date_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_view_history, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun clear() {
        val size = itemCount
        list = listOf()
        notifyItemRangeRemoved(0, size)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        Picasso.get().load(item.thumbnail).into(holder.imageView)
        holder.titleView.text = item.title
        holder.linkView.text = item.link
        holder.dateView.text = item.date
    }
}

