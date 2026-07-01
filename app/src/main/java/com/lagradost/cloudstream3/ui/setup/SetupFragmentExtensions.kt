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
    private lateinit var repoAdapter: RepoAdapter

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
        ioSafe {
            val ctx = context ?: return@ioSafe
            
            // 1. Show local/cached repositories first for instant feedback
            if (cachedRepositoriesList == null) {
                val localRepos = (RepositoryManager.getRepositories().toList() + PREBUILT_REPOSITORIES.toList()).distinctBy { it.url }
                if (localRepos.isNotEmpty()) {
                    main {
                        binding?.loadingSpinner?.isVisible = true // Still show spinner for online part
                        binding?.repoRecyclerView?.isVisible = true
                        binding?.blankRepoScreen?.isVisible = false
                        repoAdapter.submitList(localRepos)
                    }
                } else {
                    main {
                        binding?.loadingSpinner?.isVisible = true
                    }
                }

                // 2. Fetch online repositories
                val pluginstreamJson = try {
                    val client = okhttp3.OkHttpClient.Builder()
                        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                    val request = okhttp3.Request.Builder()
                        .url("https://cdn.jsdelivr.net/gh/am-abdulmueed/repo-json@main/pluginstream.json")
                        .build()
                    
                    val response = client.newCall(request).execute()
                    
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

                cachedRepositoriesList = (localRepos + masterRepoRepos).distinctBy { it.url }
            }
            
            val repositoriesList = cachedRepositoriesList ?: emptyList()
            val hasRepos = repositoriesList.isNotEmpty()
            
            main {
                binding?.loadingSpinner?.isVisible = false
                binding?.repoRecyclerView?.isVisible = hasRepos
                binding?.blankRepoScreen?.isVisible = !hasRepos

                if (hasRepos) {
                    repoAdapter.submitList(repositoriesList)

                    // Only trigger automatic downloads once
                    if (!isDownloadingStarted) {
                        isDownloadingStarted = true
                        repositoriesList.forEach { repo ->
                            PluginsViewModel.downloadAll(activity, repo, null)
                            repo.ioSafe {
                                RepositoryManager.addRepository(repo)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onBindingCreated(binding: FragmentSetupExtensionsBinding) {
        val isSetup = arguments?.getBoolean(SETUP_EXTENSION_BUNDLE_IS_SETUP) ?: false

        safe {
            val ctx = context ?: return@safe
            repoAdapter = RepoAdapter(true, { item ->
                val builder = AlertDialog.Builder(ctx)
                builder.setTitle(item.name)
                builder.setMessage(item.url)
                builder.setPositiveButton(R.string.dismiss) { dialog, _ ->
                    dialog.dismiss()
                }
                builder.show()
            }, { item ->
                PluginsViewModel.downloadAll(activity, item, null)
            })

            binding.repoRecyclerView.apply {
                adapter = repoAdapter
                setHasFixedSize(true)
                setItemViewCacheSize(20)
            }

            PluginsViewModel.downloadingRepos.observe(viewLifecycleOwner) {
                repoAdapter.notifyDataSetChanged()
            }
            PluginsViewModel.downloadedRepos.observe(viewLifecycleOwner) {
                repoAdapter.notifyDataSetChanged()
            }

            setRepositories()
            context?.setDefaultFocus(binding.nextBtt)
            binding.apply {
                nextBtt.isEnabled = true
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
