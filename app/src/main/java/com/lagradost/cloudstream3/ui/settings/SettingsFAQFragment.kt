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
            "New Features & Suggestions 💡",
            "We're always looking for your ideas! If you want something new added to PluginStream, you can **contact us** through the Contact Us button or **DM us** on our official Instagram/Telegram."
        ),
        FAQItem(
            "Movie Plugin Issues & Repo Fix 🎬",
            "1. First try **switching** to a different plugin.\n2. If the problem persists, go to **Settings > Extensions** and delete old plugins.\n3. On the same screen, click **'Add Repository'**.\n4. Click on the second option and enter **code: 3737**."
        ),
        FAQItem(
            "If Bilibili and other providers not working? 🌐",
            "• **VPN Usage:** Some providers like **Bilibili** or other regional plugins may require a **VPN (USA/Region specific)** to function correctly.\n• **Indian Content:** All Indian plugins work perfectly **without VPN**.\n• If a provider doesn't load or shows an error, try **connecting to a VPN** and refresh."
        ),
        FAQItem(
            "Why some providers show regional content issues? 🌍",
            "• **Regional Restrictions:** Some providers restrict content based on your location. This is why you might see **'Content not available'** or 'Failed to load' errors.\n• **Solution:** Using a **VPN** is the best way to bypass these regional issues. Connect to a server in the **USA or UK** for the best results with English providers."
        ),
        FAQItem(
            "App Crashing & Bug Reports ⚠️",
            "We strive to keep PluginStream stable. If the app crashes, please click **'Report Crash'** in the dialog that appears on screen. This sends **error reports** directly to our developers."
        ),
        FAQItem(
            "Offers Tab & Supporting PluginStream 💎",
            "PluginStream is ad-free. When you complete tasks from the **Offers Tab**, it helps developers earn a small income that is used to **improve servers and hardware**."
        ),
        FAQItem(
            "ProTube (YouTube Integration) 📺",
            "**ProTube** is PluginStream's own feature where you can enjoy **YouTube and YouTube Music** without going to any other app."
        ),
        FAQItem(
            "App running slow, what to do? 🔌",
            "• Check your **internet connection**.\n• **Clear Cache** in Settings.\n• Always use the **Latest Version**."
        ),
        FAQItem(
            "Account & Privacy 🔒",
            "Absolutely! PluginStream takes your **privacy** very seriously. We've designed the app with a **'Safety-First'** approach to keep your data secure."
        ),
        FAQItem(
            "Download Issues (Movies/Games) 📥",
            "1. Check your **phone storage**.\n2. **Disable VPN**.\n3. **Update or reinstall** the plugin."
        ),
        FAQItem(
            "Game Performance & Lag 🕹️",
            "• **Close background apps**.\n• Set graphics to **'Medium' or 'Low'**.\n• Check for phone **overheating**."
        ),
        FAQItem(
            "Subscription & Payments 💸",
            "PluginStream is **completely free**. We don't charge any monthly fees. You can help us by **completing tasks** in the 'Offer Tab'."
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
