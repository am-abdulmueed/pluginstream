package com.lagradost.cloudstream3.ui.setup

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.appcompat.app.AlertDialog
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.APIHolder.apis
import com.lagradost.cloudstream3.MainActivity.Companion.afterRepositoryLoadedEvent
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentSetupExtensionsBinding
import com.lagradost.cloudstream3.mvvm.safe
import com.lagradost.cloudstream3.plugins.RepositoryManager
import com.lagradost.cloudstream3.plugins.RepositoryManager.PREBUILT_REPOSITORIES
import com.lagradost.cloudstream3.ui.BaseFragment
import com.lagradost.cloudstream3.ui.settings.extensions.PluginsViewModel
import com.lagradost.cloudstream3.ui.settings.extensions.RepoAdapter
import com.lagradost.cloudstream3.ui.settings.extensions.RepositoryData
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.Coroutines.main
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MasterRepo(
    @JsonProperty("name") val name: String,
    @JsonProperty("version") val version: Int,
    @JsonProperty("last_updated") val last_updated: String,
    @JsonProperty("repositories") val repositories: List<MasterRepoEntry>
)

data class MasterRepoEntry(
    @JsonProperty("name") val name: String,
    @JsonProperty("url") val url: String,
    @JsonProperty("description") val description: String?
)

class SetupFragmentExtensions : BaseFragment<FragmentSetupExtensionsBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentSetupExtensionsBinding::inflate)
) {
    companion object {
        const val SETUP_EXTENSION_BUNDLE_IS_SETUP = "isSetup"

        /**
         * If false then this is treated a singular screen with a done button
         * */
        fun newInstance(isSetup: Boolean): Bundle {
            return Bundle().apply {
                putBoolean(SETUP_EXTENSION_BUNDLE_IS_SETUP, isSetup)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        afterRepositoryLoadedEvent += ::setRepositories
    }

    override fun onStop() {
        super.onStop()
        afterRepositoryLoadedEvent -= ::setRepositories
    }

    override fun fixLayout(view: View) {
        fixSystemBarsPadding(view)
    }

    private fun setRepositories(success: Boolean = true) {
        main {
            val ctx = context ?: return@main
            
            val pluginstreamJson = try {
                // Try to fetch from GitHub first for fresh repositories
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = okhttp3.Request.Builder()
                    .url("https://cdn.jsdelivr.net/gh/am-abdulmueed/repo-json@main/pluginstream.json")
                    .build()
                
                val response = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    client.newCall(request).execute()
                }
                
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    throw Exception("Online fetch failed")
                }
            } catch (e: Exception) {
                // Fallback to local assets if offline or error
                try {
                    ctx.assets.open("pluginstream.json").bufferedReader().use { it.readText() }
                } catch (localError: Exception) {
                    null
                }
            }

            val masterRepoRepos = pluginstreamJson?.let {
                tryParseJson<MasterRepo>(it)?.repositories?.map { entry ->
                    RepositoryData(null, entry.name, entry.url, entry.description)
                }
            } ?: emptyList()

            val repositories = RepositoryManager.getRepositories() + PREBUILT_REPOSITORIES + masterRepoRepos
            val hasRepos = repositories.isNotEmpty()
            binding?.repoRecyclerView?.isVisible = hasRepos
            binding?.blankRepoScreen?.isVisible = !hasRepos

            if (hasRepos) {
                val repos = repositories.distinctBy { it.url }
                binding?.repoRecyclerView?.adapter = RepoAdapter(true, { item ->
                    val builder = AlertDialog.Builder(ctx)
                    builder.setTitle(item.name)
                    builder.setMessage(item.description ?: item.url)
                    builder.setPositiveButton(R.string.dismiss) { dialog, _ ->
                        dialog.dismiss()
                    }
                    builder.show()
                }, { item ->
                    PluginsViewModel.downloadAll(activity, item.url, null)
                }).apply { submitList(repos) }

                repos.forEach {
                    PluginsViewModel.downloadAll(activity, it.url, null)
                    RepositoryManager.addRepository(it)
                }
            }
        }
    }

    override fun onBindingCreated(binding: FragmentSetupExtensionsBinding) {
        val isSetup = arguments?.getBoolean(SETUP_EXTENSION_BUNDLE_IS_SETUP) ?: false

        safe {
            PluginsViewModel.downloadingRepos.observe(viewLifecycleOwner) {
                binding.repoRecyclerView.adapter?.notifyDataSetChanged()
            }
            PluginsViewModel.downloadedRepos.observe(viewLifecycleOwner) {
                binding.repoRecyclerView.adapter?.notifyDataSetChanged()
            }

            setRepositories()
            binding.apply {
                if (!isSetup) {
                    nextBtt.setText(R.string.setup_done)
                }
                prevBtt.isVisible = isSetup

                nextBtt.setOnClickListener {
                    // Continue setup
                    if (isSetup)
                        if (
                        // If any available languages
                            synchronized(apis) { apis.distinctBy { it.lang }.size > 1 }
                        ) {
                            findNavController().navigate(R.id.action_navigation_setup_extensions_to_navigation_setup_provider_languages)
                        } else {
                            findNavController().navigate(R.id.action_navigation_setup_extensions_to_navigation_setup_media)
                        }
                    else
                        findNavController().navigate(R.id.navigation_home)
                }

                prevBtt.setOnClickListener {
                    findNavController().navigate(R.id.navigation_setup_language)
                }
            }
        }
    }
}
