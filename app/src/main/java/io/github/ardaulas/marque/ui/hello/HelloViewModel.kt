package io.github.ardaulas.marque.ui.hello

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ardaulas.marque.data.HelloRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HelloViewModel
    @Inject
    constructor(
        repository: HelloRepository,
    ) : ViewModel() {
        private val _greeting = MutableStateFlow(repository.greeting())
        val greeting: StateFlow<String> = _greeting.asStateFlow()
    }
