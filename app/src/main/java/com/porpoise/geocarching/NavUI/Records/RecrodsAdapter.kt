package com.porpoise.geocarching.NavUI.Records

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.porpoise.geocarching.R
import com.porpoise.geocarching.firebaseObjects.UserVisit

class RecordsAdapter :
        RecyclerView.Adapter<RecordsAdapter.MyViewHolder>() {
    private var userVisitList = emptyList<UserVisit>()


    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.records_list_view, parent, false) as View

        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.name.text = userVisitList[position].name
        holder.itemView.setOnClickListener{
            Log.d("recordsOnItemClick", userVisitList[position].name)
        }
    }

    override fun getItemCount() = userVisitList.size

    fun setUserVisitList(visitList: List<UserVisit>) {
        userVisitList = visitList
        notifyDataSetChanged()
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView = itemView.findViewById(R.id.records_name_text_view)
    }
}