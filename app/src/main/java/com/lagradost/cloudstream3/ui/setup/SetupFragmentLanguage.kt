package com.lagradost.cloudstream3.ui.setup

import android.view.View
import android.widget.AbsListView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.databinding.FragmentSetupLanguageBinding
import com.lagradost.cloudstream3.mvvm.safe
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.BaseFragment
import com.lagradost.cloudstream3.ui.settings.appLanguages
import com.lagradost.cloudstream3.ui.settings.getCurrentLocale
import com.lagradost.cloudstream3.ui.settings.nameNextToFlagEmoji
import com.lagradost.cloudstream3.plugins.MasterRepo
import com.lagradost.cloudstream3.plugins.RepositoryManager
import com.lagradost.cloudstream3.plugins.RepositoryManager.PREBUILT_REPOSITORIES
import com.lagradost.cloudstream3.ui.settings.extensions.PluginsViewModel
import com.lagradost.cloudstream3.ui.settings.extensions.RepositoryData
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.lagradost.cloudstream3.utils.AppContextUtils.setDefaultFocus
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val HAS_DONE_SETUP_KEY = "HAS_DONE_SETUP"

class SetupFragmentLanguage : BaseFragment<FragmentSetupLanguageBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentSetupLanguageBinding::inflate)
) {

    override fun fixLayout(view: View) {
        fixSystemBarsPadding(view)
    }

    override fun onBindingCreated(binding: FragmentSetupLanguageBinding) {
        // We don't want a crash for all users
        safe {
            val ctx = context ?: return@safe
            val settingsManager = PreferenceManager.getDefaultSharedPreferences(ctx)

            val arrayAdapter =
                ArrayAdapter<String>(ctx, R.layout.sort_bottom_single_choice)

            binding.apply {
                // Icons may crash on some weird android versions?
                safe {
                    val drawable = when {
                        BuildConfig.DEBUG -> R.drawable.cloud_2_gradient_debug
                        BuildConfig.FLAVOR == "prerelease" -> R.drawable.cloud_2_gradient_beta
                        else -> R.drawable.cloud_2_gradient
                    }
                    appIconImage.setImageDrawable(ContextCompat.getDrawable(ctx, drawable))
                }

                val current = getCurrentLocale(ctx)
                val languageTagsIETF = appLanguages.map { it.second }
                val languageNames = appLanguages.map { it.nameNextToFlagEmoji() }
                val currentIndex = languageTagsIETF.indexOf(current)

                arrayAdapter.addAll(languageNames)
                listview1.adapter = arrayAdapter
                listview1.choiceMode = AbsListView.CHOICE_MODE_SINGLE
                listview1.setItemChecked(currentIndex, true)

                ctx.setDefaultFocus(nextBtt)

                listview1.setOnItemClickListener { _, _, selectedLangIndex, _ ->
                    val langTagIETF = languageTagsIETF[selectedLangIndex]
                    CommonActivity.setLocale(activity, langTagIETF)
                    settingsManager.edit {
                        putString(getString(R.string.locale_key), langTagIETF)
                    }
                }

                nextBtt.setOnClickListener {
                    // If no plugins go to plugins page
                    val nextDestination = if (
                        PluginManager.getPluginsOnline().isEmpty()
                        && PluginManager.getPluginsLocal().isEmpty()
                    //&& PREBUILT_REPOSITORIES.isNotEmpty()
                    ) R.id.action_navigation_global_to_navigation_setup_extensions
                    else R.id.action_navigation_setup_language_to_navigation_setup_provider_languages

                    findNavController().navigate(
                        nextDestination,
                        SetupFragmentExtensions.newInstance(true)
                    )
                }

                /*skipBtt.setOnClickListener {
                    setKey(HAS_DONE_SETUP_KEY, true)

                    ioSafe {
                        val pluginstreamJson = try {
                            val client = okhttp3.OkHttpClient.Builder()
                                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                                .build()
                            val request = okhttp3.Request.Builder()
                                .url("https://cdn.jsdelivr.net/gh/am-abdulmueed/repo-json@main/pluginstream.json")
                                .build()

                            val response = withContext(Dispatchers.IO) {
                                client.newCall(request).execute()
                            }

                            if (response.isSuccessful) {
                                response.body?.string()
                            } else {
                                throw Exception("Online fetch failed")
                            }
                        } catch (e: Exception) {
                            try {
                                ctx.assets.open("pluginstream.json").bufferedReader().use { it.readText() }
                            } catch (localError: Exception) {
                                null
                            }
                        }

                        val masterRepoRepos = pluginstreamJson?.let { json ->
                            tryParseJson<MasterRepo>(json)?.repositories?.map { entry ->
                                RepositoryData(null, entry.name, entry.url)
                            }
                        } ?: emptyList()

                        val repositoriesList = (RepositoryManager.getRepositories().toList() + PREBUILT_REPOSITORIES.toList() + masterRepoRepos).distinctBy { it.url }

                        repositoriesList.forEach { repo ->
                            PluginsViewModel.downloadAll(activity, repo.url, null)
                            RepositoryManager.addRepository(repo)
                        }
                    }

                    findNavController().navigate(R.id.navigation_home)
                }*/
            }
        }
    }
}
