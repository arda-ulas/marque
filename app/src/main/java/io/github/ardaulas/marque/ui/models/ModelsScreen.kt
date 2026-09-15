package io.github.ardaulas.marque.ui.models

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ardaulas.marque.R
import io.github.ardaulas.marque.core.result.DataError
import io.github.ardaulas.marque.domain.model.Model
import io.github.ardaulas.marque.ui.common.MessageWithRetry
import io.github.ardaulas.marque.ui.common.SavedDataBanner
import io.github.ardaulas.marque.ui.common.toMessage
import io.github.ardaulas.marque.ui.theme.MarqueTheme

/** Stateful entry point used by the navigation graph: owns the ViewModel, delegates rendering. */
@Composable
fun ModelsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModelsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ModelsScreen(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onBack = onBack,
        modifier = modifier,
    )
}

/** Stateless rendering of [ModelsUiState]; previews and UI tests drive it directly. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(
    uiState: ModelsUiState,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = uiState.makeName ?: stringResource(R.string.models_title_fallback)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when (uiState) {
                is ModelsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is ModelsUiState.Empty -> {
                    MessageWithRetry(
                        message = stringResource(R.string.models_empty),
                        onRetry = onRefresh,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is ModelsUiState.Error -> {
                    MessageWithRetry(
                        message = uiState.error.toMessage(),
                        onRetry = onRefresh,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is ModelsUiState.Content -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            uiState.error?.let { error -> SavedDataBanner(error = error, onRetry = onRefresh) }
                            ModelList(models = uiState.models, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelList(
    models: List<Model>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(models, key = { it.id }) { model ->
            Text(
                text = model.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
            )
            HorizontalDivider()
        }
    }
}

private val previewModels = listOf(Model(2206, "Scion xA"), Model(2207, "Scion tC"), Model(2208, "Corolla"))

@Preview(showBackground = true)
@Composable
private fun ModelsScreenContentPreview() {
    MarqueTheme {
        ModelsScreen(
            uiState = ModelsUiState.Content("TOYOTA", previewModels, isRefreshing = false, error = null),
            onRefresh = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ModelsScreenContentWithErrorPreview() {
    MarqueTheme {
        ModelsScreen(
            uiState = ModelsUiState.Content("TOYOTA", previewModels, isRefreshing = false, error = DataError.Timeout),
            onRefresh = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ModelsScreenErrorPreview() {
    MarqueTheme {
        ModelsScreen(
            uiState = ModelsUiState.Error("TOYOTA", DataError.Network),
            onRefresh = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ModelsScreenLoadingPreview() {
    MarqueTheme {
        ModelsScreen(uiState = ModelsUiState.Loading(makeName = null), onRefresh = {}, onBack = {})
    }
}
