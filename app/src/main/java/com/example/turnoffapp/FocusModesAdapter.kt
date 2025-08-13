package com.example.turnoffapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FocusModesAdapter(
    private val onStartFocusMode: (FocusMode) -> Unit,
    private val onEditFocusMode: (FocusMode) -> Unit,
    private val getAppName: (String) -> String
) : RecyclerView.Adapter<FocusModesAdapter.FocusModeViewHolder>() {

    private var focusModes = listOf<FocusMode>()

    fun updateFocusModes(newFocusModes: List<FocusMode>) {
        focusModes = newFocusModes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FocusModeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_focus_mode, parent, false)
        return FocusModeViewHolder(view)
    }

    override fun onBindViewHolder(holder: FocusModeViewHolder, position: Int) {
        holder.bind(focusModes[position])
    }

    override fun getItemCount(): Int = focusModes.size

    inner class FocusModeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFocusModeName: TextView = itemView.findViewById(R.id.tv_focus_mode_name)
        private val tvFocusModeDuration: TextView = itemView.findViewById(R.id.tv_focus_mode_duration)
        private val tvBlockedItems: TextView = itemView.findViewById(R.id.tv_focus_mode_blocked_items)
        private val btnStartFocusMode: Button = itemView.findViewById(R.id.btn_start_focus_mode)

        fun bind(focusMode: FocusMode) {
            tvFocusModeName.text = focusMode.name
            tvFocusModeDuration.text = focusMode.getDurationText()

            val blockedApps = focusMode.blockedApps.map(getAppName).joinToString(", ")
            val blockedWebsites = focusMode.blockedWebsites.joinToString(", ")

            val blockedItemsText = mutableListOf<String>()
            if (blockedApps.isNotEmpty()) {
                blockedItemsText.add("앱: $blockedApps")
            }
            if (blockedWebsites.isNotEmpty()) {
                blockedItemsText.add("사이트: $blockedWebsites")
            }

            tvBlockedItems.text = if (blockedItemsText.isEmpty()) {
                "차단된 항목 없음"
            } else {
                blockedItemsText.joinToString(" / ")
            }

            btnStartFocusMode.setOnClickListener {
                onStartFocusMode(focusMode)
            }
            
            // 전체 아이템 클릭 시 수정 화면으로 이동
            itemView.setOnClickListener {
                onEditFocusMode(focusMode)
            }
        }
    }
}