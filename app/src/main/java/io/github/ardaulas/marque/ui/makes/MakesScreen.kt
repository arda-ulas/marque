package io.github.ardaulas.marque.ui.makes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import io.github.ardaulas.marque.domain.model.Make
import io.github.ardaulas.marque.ui.common.MessageWithRetry
import io.github.ardaulas.marque.ui.common.SavedDataBanner
import io.github.ardaulas.marque.ui.common.toMessage
import io.github.ardaulas.marque.ui.theme.MarqueTheme

/** Stateful entry point used by the navigation graph: owns the ViewModel, delegates rendering. */
@Composable
fun MakesRoute(
    onMakeClick: (makeId: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MakesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MakesScreen(
        uiState = uiState,
        onQueryChange = viewModel::setQuery,
        onRefresh = viewModel::refresh,
        onMakeClick = onMakeClick,
        modifier = modifier,
    )
}

/** Stateless rendering of [MakesUiState]; previews and UI tests drive it directly. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakesScreen(
    uiState: MakesUiState,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onMakeClick: (makeId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(text = stringResource(R.string.app_name)) }) },
    ) { innerPadding ->
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
                    MessageWithRetry(
                        message = stringResource(R.string.makes_empty),
                        onRetry = onRefresh,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is MakesUiState.Error -> {
                    MessageWithRetry(
                        message = uiState.error.toMessage(),
                        onRetry = onRefresh,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is MakesUiState.Content -> {
                    MakesContent(
                        content = uiState,
                        onQueryChange = onQueryChange,
                        onRefresh = onRefresh,
                        onMakeClick = onMakeClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun MakesContent(
    content: MakesUiState.Content,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onMakeClick: (makeId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SearchField(
            query = content.query,
            onQueryChange = onQueryChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        PullToRefreshBox(
            isRefreshing = content.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                content.error?.let { error -> SavedDataBanner(error = error, onRetry = onRefresh) }
                MakeList(makes = content.makes, onMakeClick = onMakeClick, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text(text = stringResource(R.string.makes_search_hint)) },
        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.makes_search_clear),
                    )
                }
            }
        },
        singleLine = true,
    )
}

/**
 * Always a LazyColumn, even for the no-match case, so the pull-to-refresh gesture keeps working
 * when a query filters everything out. Scroll position is restored by the default saveable
 * `rememberLazyListState()` inside LazyColumn.
 */
@Composable
private fun MakeList(
    makes: List<Make>,
    onMakeClick: (makeId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        if (makes.isEmpty()) {
            item(key = "no-match") {
                Text(
                    text = stringResource(R.string.makes_no_match),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                )
            }
        }
        items(makes, key = { it.id }) { make ->
            Text(
                text = make.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onMakeClick(make.id) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
            )
            HorizontalDivider()
        }
    }
}

private val previewMakes = listOf(Make(440, "ASTON MARTIN"), Make(441, "TESLA"), Make(448, "TOYOTA"))

@Preview(showBackground = true)
@Composable
private fun MakesScreenContentPreview() {
    MarqueTheme {
        MakesScreen(
            uiState = MakesUiState.Content(previewMakes, query = "", isRefreshing = false, error = null),
            onQueryChange = {},
            onRefresh = {},
            onMakeClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MakesScreenContentWithErrorPreview() {
    MarqueTheme {
        MakesScreen(
            uiState = MakesUiState.Content(previewMakes, query = "t", isRefreshing = false, error = DataError.Network),
            onQueryChange = {},
            onRefresh = {},
            onMakeClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MakesScreenNoMatchPreview() {
    MarqueTheme {
        MakesScreen(
            uiState = MakesUiState.Content(emptyList(), query = "zzz", isRefreshing = false, error = null),
            onQueryChange = {},
            onRefresh = {},
            onMakeClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MakesScreenErrorPreview() {
    MarqueTheme {
        MakesScreen(
            uiState = MakesUiState.Error(DataError.Http(503)),
            onQueryChange = {},
            onRefresh = {},
            onMakeClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MakesScreenEmptyPreview() {
    MarqueTheme {
        MakesScreen(uiState = MakesUiState.Empty, onQueryChange = {}, onRefresh = {}, onMakeClick = {})
    }
}
