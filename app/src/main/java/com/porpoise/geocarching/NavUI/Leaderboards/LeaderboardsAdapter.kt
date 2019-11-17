package com.porpoise.geocarching.NavUI.Leaderboards

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.porpoise.geocarching.R
import com.porpoise.geocarching.firebaseObjects.LeaderboardUser
import org.w3c.dom.Text

class LeaderboardsAdapter :
        RecyclerView.Adapter<LeaderboardsAdapter.MyViewHolder>() {
    private var leaderboardList = emptyList<LeaderboardUser>()


    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.leaderboards_list_view, parent, false) as View

        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.position.text = (position + 1).toString()
        holder.username.text = leaderboardList[position].username
        holder.level.text = leaderboardList[position].level.toString()
    }

    override fun getItemCount() = leaderboardList.size

    fun setLeaderboardList(list: List<LeaderboardUser>) {
        leaderboardList = list
        notifyDataSetChanged()
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var position: TextView = itemView.findViewById(R.id.leaderboards_position_text_view)
        var username: TextView = itemView.findViewById(R.id.leaderboards_username_text_view)
        var level: TextView = itemView.findViewById(R.id.leaderboards_level_text_view)
    }
}