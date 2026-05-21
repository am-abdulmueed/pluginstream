package com.lagradost.cloudstream3.ui.changelog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.databinding.FragmentChangelogBinding
import com.lagradost.cloudstream3.databinding.ChangelogItemBinding
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import java.io.InputStream

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class ChangelogFragment : BottomSheetDialogFragment() {
    private var binding: FragmentChangelogBinding? = null

    override fun onStart() {
        super.onStart()
        // Make the bottom sheet full height expanded by default
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    data class Changelog(
        @JsonProperty("version") val version: String,
        @JsonProperty("title") val title: String,
        @JsonProperty("logs") val logs: List<LogItem>
    )

    data class LogItem(
        @JsonProperty("icon") val icon: String,
        @JsonProperty("title") val title: String,
        @JsonProperty("description") val description: String
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChangelogBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val changelogs = loadChangelogs()
        if (changelogs.isNullOrEmpty()) {
            dismiss()
            return
        }

        val latest = changelogs.first()
        binding?.changelogTitle?.text = latest.title
        
        binding?.changelogRecycler?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ChangelogAdapter(latest.logs)
        }

        binding?.changelogClose?.setOnClickListener {
            dismiss()
        }
    }

    private fun loadChangelogs(): List<Changelog>? {
        return try {
            val inputStream: InputStream = requireContext().assets.open("changelogs.json")
            val json = inputStream.bufferedReader().use { it.readText() }
            parseJson<List<Changelog>>(json)
        } catch (e: Exception) {
            logError(e)
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    class ChangelogAdapter(private val items: List<LogItem>) :
        RecyclerView.Adapter<ChangelogAdapter.ViewHolder>() {

        class ViewHolder(val binding: ChangelogItemBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ChangelogItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.logIcon.text = item.icon
            holder.binding.logTitle.text = item.title
            holder.binding.logDescription.text = item.description
        }

        override fun getItemCount() = items.size
    }
}