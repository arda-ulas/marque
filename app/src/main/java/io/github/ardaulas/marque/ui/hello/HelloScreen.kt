package io.github.ardaulas.marque.ui.hello

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HelloScreen(
    modifier: Modifier = Modifier,
    viewModel: HelloViewModel = hiltViewModel(),
) {
    val greeting by viewModel.greeting.collectAsStateWithLifecycle()
    HelloContent(greeting = greeting, modifier = modifier)
}

@Composable
private fun HelloContent(
    greeting: String,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
