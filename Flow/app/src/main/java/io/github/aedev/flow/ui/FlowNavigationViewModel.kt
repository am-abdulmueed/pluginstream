package io.github.aedev.flow.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FlowNavigationViewModel @Inject constructor() : ViewModel() {
    var lastRoute: String? = null
}
