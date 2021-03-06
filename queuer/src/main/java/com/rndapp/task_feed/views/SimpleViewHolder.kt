package com.rndapp.task_feed.views

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.rndapp.task_feed.R
import com.rndapp.task_feed.listeners.OnSimpleItemClickedListener


class SimpleViewHolder(itemView: View, private val listener: OnSimpleItemClickedListener): RecyclerView.ViewHolder(itemView) {
    private val tv: TextView
    private var id: Int? = -1

    init {
        tv = itemView.findViewById<TextView>(R.id.textView) as TextView

        itemView.setOnClickListener { listener.onSimpleItemClicked(id!!) }
    }

    fun setText(text: String?, id: Int) {
        tv.text = text
        this.id = id
    }
}