package com.lagradost.cloudstream3.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentFaqBinding
import com.lagradost.cloudstream3.ui.BaseFragment
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.setUpToolbar
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.setSystemBarsPadding

import com.lagradost.cloudstream3.ui.settings.Globals.isLandscape
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding

import io.noties.markwon.Markwon
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin

import io.noties.markwon.LinkResolver
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonConfiguration
import com.lagradost.cloudstream3.utils.UIHelper.navigate
import android.provider.Settings as AndroidSettings

class SettingsFAQFragment : BaseFragment<FragmentFaqBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentFaqBinding::inflate)
) {
    private lateinit var markwon: Markwon

    override fun fixLayout(view: View) {
        fixSystemBarsPadding(
            view,
            padBottom = isLandscape(),
            padLeft = isLayout(TV or EMULATOR)
        )
    }

    data class FAQItem(val question: String, val answer: String, var isExpanded: Boolean = false)

    private val fullFaqList = listOf(
        FAQItem(
            "🚀 What is PluginStream?",
            "PluginStream is a modular media player that allows you to stream content from various sources using community-built extensions. It's ad-free, open-source, and privacy-focused."
        ),
        FAQItem(
            "🔌 How do I add content?",
            "You need to install extensions. Go to **Settings > Extensions**, click on **Add Repository**, and enter a repository URL. You can find popular repositories in our [Telegram channel](https://t.me/pluginstreamofficial)."
        ),
        FAQItem(
            "📺 Does it work on Android TV?",
            "Yes! PluginStream has a dedicated UI for Android TV and Firestick. It supports D-pad navigation and leanback experience."
        ),
        FAQItem(
            "🛠 A provider is not working, what to do?",
            "Providers are maintained by the community. If one stops working, check for updates in **Settings > Extensions**. If it's still broken, you can report it via **Settings > Report a Bug**."
        ),
        FAQItem(
            "📥 Can I download movies?",
            "Yes, PluginStream supports downloading. Just click the download icon on any episode or movie page. You can manage your downloads in the **Downloads** tab."
        ),
        FAQItem(
            "🛡 Is it safe to use?",
            "PluginStream is open-source and doesn't collect personal data. However, the safety of content depends on the extensions you use. Always use trusted repository URLs."
        )
    )

    private var filteredList = fullFaqList.toList()
    private lateinit var faqAdapter: FAQAdapter

    override fun onBindingCreated(binding: FragmentFaqBinding) {
        setUpToolbar("Support & FAQ")
        setSystemBarsPadding()

        markwon = Markwon.builder(requireContext())
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                    builder.linkResolver(object : LinkResolver {
                        override fun resolve(view: View, link: String) {
                            when {
                                link == "app://clear_cache" -> {
                                    try {
                                        val intent = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", view.context.packageName, null)
                                        }
                                        view.context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Fallback to default behavior if possible
                                    }
                                }
                                link == "app://extensions" -> {
                                    try {
                                        activity?.navigate(R.id.action_navigation_global_to_navigation_settings_extensions)
                                    } catch (e: Exception) {
                                        // Fallback
                                    }
                                }
                                else -> {
                                    // Handle other links normally
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                    view.context.startActivity(intent)
                                }
                            }
                        }
                    })
                }
            })
            .build()

        faqAdapter = FAQAdapter(filteredList)
        binding.faqRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = faqAdapter
        }

        binding.faqSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterFaq(newText ?: "")
                return true
            }
        })

        binding.faqSupportCard.setOnClickListener {
            CommonActivity.getSocialLinks { json ->
                var telegramUrl = "https://t.me/pluginstreamofficial"
                val handles = json?.optJSONArray("social_handles")
                if (handles != null) {
                    for (i in 0 until handles.length()) {
                        val handle = handles.getJSONObject(i)
                        if (handle.optString("platform").lowercase() == "telegram") {
                            telegramUrl = handle.optString("url").trim().removeSurrounding("`")
                            break
                        }
                    }
                }
                
                try {
                    // Try to open in Telegram app if possible, else fallback to URL
                    val uri = Uri.parse(telegramUrl)
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to browser
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl))
                    startActivity(intent)
                }
            }
        }
    }

    private fun filterFaq(query: String) {
        filteredList = if (query.isEmpty()) {
            fullFaqList
        } else {
            fullFaqList.filter {
                it.question.contains(query, ignoreCase = true) || 
                it.answer.contains(query, ignoreCase = true)
            }
        }
        faqAdapter.updateList(filteredList)
        binding?.faqEmptyView?.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }

    inner class FAQAdapter(private var items: List<FAQItem>) : RecyclerView.Adapter<FAQAdapter.ViewHolder>() {
        private var expandedPosition = -1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_faq, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.question.text = item.question
            markwon.setMarkdown(holder.answer, item.answer)
            holder.answer.movementMethod = android.text.method.LinkMovementMethod.getInstance()
            
            val isExpanded = position == expandedPosition
            holder.answerLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
            holder.arrow.rotation = if (isExpanded) 180f else 0f

            holder.questionLayout.setOnClickListener {
                val previousExpanded = expandedPosition
                if (position == expandedPosition) {
                    expandedPosition = -1
                    notifyItemChanged(position)
                } else {
                    expandedPosition = position
                    notifyItemChanged(previousExpanded)
                    notifyItemChanged(expandedPosition)
                }
            }
        }

        override fun getItemCount() = items.size

        fun updateList(newList: List<FAQItem>) {
            items = newList
            notifyDataSetChanged()
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val question: TextView = view.findViewById(R.id.faq_question)
            val answer: TextView = view.findViewById(R.id.faq_answer)
            val arrow: ImageView = view.findViewById(R.id.faq_arrow)
            val questionLayout: LinearLayout = view.findViewById(R.id.faq_question_layout)
            val answerLayout: LinearLayout = view.findViewById(R.id.faq_answer_layout)
        }
    }
}
