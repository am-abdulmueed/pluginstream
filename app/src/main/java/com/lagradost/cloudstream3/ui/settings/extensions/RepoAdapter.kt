package com.lagradost.cloudstream3.ui.settings.extensions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.RepositoryItemBinding
import com.lagradost.cloudstream3.databinding.RepositoryItemTvBinding
import com.lagradost.cloudstream3.plugins.RepositoryManager.PREBUILT_REPOSITORIES
import com.lagradost.cloudstream3.ui.BaseDiffCallback
import com.lagradost.cloudstream3.ui.NoStateAdapter
import com.lagradost.cloudstream3.ui.ViewHolderState
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.utils.ImageLoader.loadImage
import com.lagradost.cloudstream3.utils.UIHelper.clipboardHelper
import com.lagradost.cloudstream3.utils.getImageFromDrawable
import com.lagradost.cloudstream3.utils.txt
import androidx.core.content.ContextCompat

class RepoAdapter(
    val isSetup: Boolean,
    val clickCallback: RepoAdapter.(RepositoryData) -> Unit,
    val imageClickCallback: RepoAdapter.(RepositoryData) -> Unit,
    /** In setup mode the trash icons will be replaced with download icons */
) :
    NoStateAdapter<RepositoryData>(diffCallback = BaseDiffCallback(itemSame = { a, b ->
        a.url == b.url
    })) {

    override fun onCreateContent(parent: ViewGroup): ViewHolderState<Any> {
        val layout = if (isLayout(TV)) RepositoryItemTvBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ) else RepositoryItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolderState(layout)
    }

    private fun bindAction(
        isPrebuilt: Boolean,
        drawable: Int,
        binding: Any, // Binding
        item: RepositoryData,
    ) {
        val actionButton = when (binding) {
            is RepositoryItemTvBinding -> binding.actionButton
            is RepositoryItemBinding -> binding.actionButton
            else -> return
        }
        val actionProgress = when (binding) {
            is RepositoryItemTvBinding -> binding.actionProgress
            is RepositoryItemBinding -> binding.actionProgress
            else -> return
        }
        val context = actionButton.context
        val isDownloading = PluginsViewModel.currentDownloadingRepos.contains(item.url)
        val isDownloaded = PluginsViewModel.currentDownloadedRepos.contains(item.url)

        if (isDownloaded && isSetup) {
            actionButton.setImageResource(R.drawable.ic_baseline_check_24)
            actionButton.setColorFilter(
                ContextCompat.getColor(context, R.color.colorAccent),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
            actionButton.isVisible = true
            actionProgress.isVisible = false
        } else if (isDownloading) {
            actionButton.isVisible = false
            actionProgress.isVisible = true
        } else {
            actionButton.clearColorFilter()
            if (!isPrebuilt || isSetup) {
                actionButton.setImageResource(drawable)
                actionButton.isVisible = true
            } else {
                actionButton.isVisible = false
            }
            actionProgress.isVisible = false
        }
    }

    override fun onClearView(holder: ViewHolderState<Any>) {
        when (val binding = holder.view) {
            is RepositoryItemBinding -> clearImage(binding.entryIcon)
            is RepositoryItemTvBinding -> clearImage(binding.entryIcon)
        }
    }

    override fun onBindContent(holder: ViewHolderState<Any>, item: RepositoryData, position: Int) {
        val isPrebuilt = PREBUILT_REPOSITORIES.contains(item)
        val drawable =
            if (isSetup) R.drawable.netflix_download else R.drawable.ic_baseline_delete_outline_24
        when (val binding = holder.view) {
            is RepositoryItemTvBinding -> {
                binding.apply {
                    // Only shows icon if on setup or if it isn't a prebuilt repo.
                    // No delete buttons on prebuilt repos.
                    if (!isPrebuilt || isSetup) {
                        actionButton.setImageResource(drawable)
                    }

                    actionButton.setOnClickListener {
                        imageClickCallback(item)
                    }

                    bindAction(isPrebuilt, drawable, this, item)

                    repositoryItemRoot.setOnClickListener {
                        clickCallback(item)
                    }
                    val originalName = item.name ?: ""
                    mainText.text = originalName.replace("moviebox", "Max", ignoreCase = true)
                        .replace("moveibox", "Max", ignoreCase = true)
                        .replace("castel tv ( use vlc)", "PluginStream", ignoreCase = true)
                        .replace("castle", "PluginStream", ignoreCase = true)
                        .replace("castel", "PluginStream", ignoreCase = true)
                        .replace("caslte", "PluginStream", ignoreCase = true)
                    subText.text = item.url
                    if (!item.iconUrl.isNullOrEmpty()) {
                        entryIcon.loadImage(item.iconUrl) {
                            error(
                                getImageFromDrawable(
                                    binding.root.context,
                                    R.drawable.ic_github_logo
                                )
                            )
                        }
                    } else {
                        entryIcon.loadImage(R.drawable.ic_github_logo)
                    }
                }
            }

            is RepositoryItemBinding -> {
                binding.apply {
                    // Only shows icon if on setup or if it isn't a prebuilt repo.
                    // No delete buttons on prebuilt repos.
                    if (!isPrebuilt || isSetup) {
                        actionButton.setImageResource(drawable)
                    }

                    actionButton.setOnClickListener {
                        imageClickCallback(item)
                    }

                    bindAction(isPrebuilt, drawable, this, item)

                    repositoryItemRoot.setOnClickListener {
                        clickCallback(item)
                    }

                    repositoryItemRoot.setOnLongClickListener {
                        val shareableRepoData =
                            "${item.name}$SHAREABLE_REPO_SEPARATOR\n ${item.url}"
                        clipboardHelper(txt(R.string.repo_copy_label), shareableRepoData)
                        true
                    }

                    val originalName = item.name ?: ""
                    mainText.text = originalName.replace("moviebox", "Max", ignoreCase = true)
                        .replace("moveibox", "Max", ignoreCase = true)
                        .replace("castel tv ( use vlc)", "PluginStream", ignoreCase = true)
                        .replace("castle", "PluginStream", ignoreCase = true)
                        .replace("castel", "PluginStream", ignoreCase = true)
                        .replace("caslte", "PluginStream", ignoreCase = true)
                    subText.text = item.url
                    if (!item.iconUrl.isNullOrEmpty()) {
                        entryIcon.loadImage(item.iconUrl) {
                            error(
                                getImageFromDrawable(
                                    binding.root.context,
                                    R.drawable.ic_github_logo
                                )
                            )
                        }
                    } else {
                        entryIcon.loadImage(R.drawable.ic_github_logo)
                    }
                }
            }
        }
    }

    companion object {
        const val SHAREABLE_REPO_SEPARATOR = " : "
    }
}