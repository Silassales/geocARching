package com.porpoise.geocarching.NavUI.Records

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.porpoise.geocarching.R
import com.porpoise.geocarching.firebaseObjects.UserPlacedCache

class RecordsPlacedAdapter :
        RecyclerView.Adapter<RecordsPlacedAdapter.MyViewHolder>() {

    private var userPlacedList = emptyList<UserPlacedCache>()


    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.records_list_view, parent, false) as View

        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.name.text = userPlacedList[position].name
        holder.itemView.setOnClickListener{
            Log.d("recordsPlacedOnItemClick", userPlacedList[position].name)
        }
    }

    override fun getItemCount() = userPlacedList.size

    fun setUserPlacedList(placedCacheList: List<UserPlacedCache>) {
        userPlacedList = placedCacheList
        notifyDataSetChanged()
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView = itemView.findViewById(R.id.records_name_text_view)
    }
}