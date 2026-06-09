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
import com.lagradost.cloudstream3.plugins.MasterRepo
import com.lagradost.cloudstream3.plugins.RepositoryManager
import com.lagradost.cloudstream3.plugins.RepositoryManager.PREBUILT_REPOSITORIES
import com.lagradost.cloudstream3.ui.BaseFragment
import com.lagradost.cloudstream3.ui.settings.extensions.PluginsViewModel
import com.lagradost.cloudstream3.ui.settings.extensions.RepoAdapter
import com.lagradost.cloudstream3.ui.settings.extensions.RepositoryData
import com.lagradost.cloudstream3.utils.AppContextUtils.setDefaultFocus
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.lagradost.cloudstream3.utils.Coroutines.main
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding
import kotlinx.coroutines.withContext

class SetupFragmentExtensions : BaseFragment<FragmentSetupExtensionsBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentSetupExtensionsBinding::inflate)
) {
    private var isDownloadingStarted = false
    private var cachedRepositoriesList: List<RepositoryData>? = null

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
        if (cachedRepositoriesList == null) {
            afterRepositoryLoadedEvent += ::setRepositories
        } else {
            setRepositories()
        }
    }

    override fun onStop() {
        super.onStop()
        afterRepositoryLoadedEvent -= ::setRepositories
    }

    override fun fixLayout(view: View) {
        fixSystemBarsPadding(view)
    }

    private fun setRepositories(success: Boolean = true) {
        main { _ ->
            val ctx = context ?: return@main
            
            // Only show spinner if we don't have cached data
            if (cachedRepositoriesList == null) {
                binding?.loadingSpinner?.isVisible = true
            }
            
            val masterRepoRepos = if (cachedRepositoriesList == null) {
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

                pluginstreamJson?.let { json ->
                    tryParseJson<MasterRepo>(json)?.repositories?.map { entry ->
                        RepositoryData(null, entry.name, entry.url)
                    }
                } ?: emptyList()
            } else {
                emptyList()
            }

            if (cachedRepositoriesList == null) {
                cachedRepositoriesList = (RepositoryManager.getRepositories().toList() + PREBUILT_REPOSITORIES.toList() + masterRepoRepos).distinctBy { it.url }
            }
            
            val repositoriesList = cachedRepositoriesList ?: emptyList()
            val hasRepos = repositoriesList.isNotEmpty()
            
            binding?.loadingSpinner?.isVisible = false
            binding?.repoRecyclerView?.isVisible = hasRepos
            binding?.blankRepoScreen?.isVisible = !hasRepos

            if (hasRepos) {
                // Always setup adapter and observer
                val adapter = RepoAdapter(true, { item ->
                    val builder = AlertDialog.Builder(ctx)
                    builder.setTitle(item.name)
                    builder.setMessage(item.url)
                    builder.setPositiveButton(R.string.dismiss) { dialog, _ ->
                        dialog.dismiss()
                    }
                    builder.show()
                }, { item ->
                    PluginsViewModel.downloadAll(activity, item.url, null)
                })
                binding?.repoRecyclerView?.adapter = adapter
                adapter.submitList(repositoriesList)

                // Only trigger automatic downloads once
                if (!isDownloadingStarted) {
                    isDownloadingStarted = true
                    repositoriesList.forEach { repo ->
                        PluginsViewModel.downloadAll(activity, repo.url, null)
                        repo.ioSafe {
                            RepositoryManager.addRepository(repo)
                        }
                    }
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
            context?.setDefaultFocus(binding.nextBtt)
            binding.apply {
                if (!isSetup) {
                    nextBtt.setText(R.string.setup_done)
                }
                prevBtt.isVisible = isSetup

                nextBtt.setOnClickListener {
                    // Continue setup
                    if (isSetup) {
                        val currentDestination = findNavController().currentDestination?.id
                        if (currentDestination == R.id.navigation_setup_extensions) {
                            if (
                            // If any available languages
                                apis.distinctBy { it.lang }.size > 1
                            ) {
                                findNavController().navigate(R.id.action_navigation_setup_extensions_to_navigation_setup_provider_languages)
                            } else {
                                findNavController().navigate(R.id.action_navigation_setup_extensions_to_navigation_setup_media)
                            }
                        }
                    } else {
                        findNavController().navigate(R.id.navigation_home)
                    }
                }

                prevBtt.setOnClickListener {
                    val currentDestination = findNavController().currentDestination?.id
                    if (currentDestination == R.id.navigation_setup_extensions) {
                        findNavController().navigate(R.id.navigation_setup_language)
                    }
                }
            }
        }
    }
}
