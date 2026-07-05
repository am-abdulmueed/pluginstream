package com.lagradost.cloudstream3.ui.settings

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
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
    private var hasPlayedListEntrance = false

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
            "Movie Plugin Issues & Fix 🎬",
            "1. First try **switching** to a different plugin.\n2. If the problem persists, go to **[Settings > Extensions](app://extensions)**.\n3. Click the **[bin icon](app://extensions)** at the top to remove all plugins.\n4. **Restart the app** (close and reopen) to fresh install all plugins."
        ),
        FAQItem(
            "If Bilibili and other providers not working? 🌐",
            "• **VPN Usage:** Some regional providers or plugins may require a **VPN (USA/Region specific)** to function correctly.\n• **Indian Content:** All Indian plugins work perfectly **without VPN**.\n• **Troubleshooting:** If any provider or plugin is not working, try connecting to a **VPN**. If you are already connected, try **disconnecting** it and refresh."
        ),
        FAQItem(
            "If any provider or plugin is not working? 🌐",
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
            "• Check your **internet connection**.\n• **[Clear Cache](app://clear_cache)** in Settings.\n• Always use the **Latest Version**."
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

        // Staggered entrance animation the first time the list lays out
        binding.faqRecycler.post {
            if (!hasPlayedListEntrance) {
                hasPlayedListEntrance = true
                binding.faqRecycler.scheduleLayoutAnimation()
            }
        }

        // Subtle glow on the search card's border while the user is typing
        val defaultStrokeColor = requireContext().getColor(R.color.grayTextColor)
        val focusedStrokeColor = requireContext().getColor(R.color.colorPrimary)
        binding.faqSearch.setOnQueryTextFocusChangeListener { _, hasFocus ->
            val fromColor = if (hasFocus) defaultStrokeColor else focusedStrokeColor
            val toColor = if (hasFocus) focusedStrokeColor else defaultStrokeColor
            ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor).apply {
                duration = 200
                addUpdateListener { animator ->
                    binding.faqSearchCard.strokeColor = animator.animatedValue as Int
                }
                start()
            }
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

        val emptyView = binding?.faqEmptyView ?: return
        if (filteredList.isEmpty()) {
            if (emptyView.visibility != View.VISIBLE) {
                emptyView.alpha = 0f
                emptyView.visibility = View.VISIBLE
                emptyView.animate().alpha(1f).setDuration(200).start()
            }
        } else {
            emptyView.visibility = View.GONE
        }
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
            holder.arrow.animate().rotation(if (isExpanded) 180f else 0f).setDuration(180).start()

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