package com.porpoise.geocarching.NavUI.CacheDetails

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.porpoise.geocarching.R

class CacheDetailsAdapter: RecyclerView.Adapter<CacheDetailsAdapter.MyViewHolder>() {
    private var cacheVisitsList = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.visits_list_view, parent, false) as View

        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.name.text = cacheVisitsList[position]
    }

    override fun getItemCount() = cacheVisitsList.size

    fun addVisitToList(user: String) {
        cacheVisitsList.add(user)
        notifyDataSetChanged()
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView = itemView.findViewById(R.id.visits_text_view)
    }
}