package com.example.turnoffapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FocusModesAdapter(
    private val onStartFocusMode: (FocusMode) -> Unit,
    private val onEditFocusMode: (FocusMode) -> Unit
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
        private val tvFocusModeAppsCount: TextView = itemView.findViewById(R.id.tv_focus_mode_apps_count)
        private val btnStartFocusMode: Button = itemView.findViewById(R.id.btn_start_focus_mode)

        fun bind(focusMode: FocusMode) {
            tvFocusModeName.text = focusMode.name
            tvFocusModeDuration.text = focusMode.getDurationText()
            
            val appsCount = focusMode.blockedApps.size
            val websitesCount = focusMode.blockedWebsites.size
            tvFocusModeAppsCount.text = "차단 앱 ${appsCount}개, 웹사이트 ${websitesCount}개"

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