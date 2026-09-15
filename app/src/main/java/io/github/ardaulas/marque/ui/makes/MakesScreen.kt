package io.github.ardaulas.marque.ui.makes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ardaulas.marque.R
import io.github.ardaulas.marque.core.result.DataError
import io.github.ardaulas.marque.domain.model.Make

/**
 * Minimal rendering of [MakesUiState] to make the data layer visible end to end. Navigation,
 * the models screen, and visual polish are out of scope here (Block 4).
 */
@Composable
fun MakesScreen(
    modifier: Modifier = Modifier,
    viewModel: MakesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MakesContent(uiState = uiState, onRetry = viewModel::refresh, modifier = modifier)
}

@Composable
private fun MakesContent(
    uiState: MakesUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when (uiState) {
                MakesUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                MakesUiState.Empty -> {
                    Text(
                        text = stringResource(R.string.makes_empty),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is MakesUiState.Error -> {
                    ErrorBlock(
                        error = uiState.error,
                        onRetry = onRetry,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is MakesUiState.Content -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (uiState.isRefreshing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        uiState.error?.let { error ->
                            ErrorBlock(
                                error = error,
                                onRetry = onRetry,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                            )
                        }
                        MakeList(makes = uiState.makes, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
private fun MakeList(
    makes: List<Make>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(makes, key = { it.id }) { make ->
            Text(
                text = make.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun ErrorBlock(
    error: DataError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = errorMessage(error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.makes_retry))
        }
    }
}

@Composable
private fun errorMessage(error: DataError): String =
    when (error) {
        DataError.Network -> stringResource(R.string.error_network)
        DataError.Timeout -> stringResource(R.string.error_timeout)
        is DataError.Http -> stringResource(R.string.error_http, error.code)
        DataError.Serialization -> stringResource(R.string.error_serialization)
        DataError.Unknown -> stringResource(R.string.error_unknown)
    }
