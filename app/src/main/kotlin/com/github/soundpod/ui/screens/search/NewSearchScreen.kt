package com.github.soundpod.ui.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.core.ui.LocalAppearance
import com.github.innertube.Innertube
import com.github.innertube.requests.searchSuggestions
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.models.SearchQuery
import com.github.soundpod.query
import com.github.soundpod.ui.appearance.LoadingAnimation
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.navigation.Routes
import com.github.soundpod.utils.pauseSearchHistoryKey
import com.github.soundpod.utils.preferences
import com.github.soundpod.utils.rememberVoiceSearchLauncher
import com.github.soundpod.viewmodels.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun NewSearchScreen(
    initialTextInput: String = "",
    navController: NavController,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val (colorPalette) = LocalAppearance.current

    val searchViewModel: SearchViewModel = viewModel()

    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialTextInput))
    }

    var confirmedSearchQuery: String? by rememberSaveable { mutableStateOf(null) }
    var history: List<SearchQuery> by remember { mutableStateOf(emptyList()) }
    var suggestionsResult: Result<List<String>?>? by remember { mutableStateOf(null) }
    var isLoadingSuggestions by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    val performSearch: (String) -> Unit = { queryText ->
        if (queryText.isNotBlank()) {
            keyboardController?.hide()
            focusManager.clearFocus()

            textFieldValue = TextFieldValue(queryText)
            confirmedSearchQuery = queryText

            searchViewModel.performSearch(queryText)

            if (!context.preferences.getBoolean(pauseSearchHistoryKey, false)) {
                query {
                    db.insert(SearchQuery(query = queryText))
                }
            }
        }
    }

    val launchVoiceSearch = rememberVoiceSearchLauncher(
        context = context,
        onSpeechResult = { spokenText -> performSearch(spokenText) }
    )

    BackHandler {
        if (confirmedSearchQuery != null) {
            confirmedSearchQuery = null
        } else {
            navController.popBackStack()
        }
    }

    LaunchedEffect(textFieldValue.text) {
        if (!context.preferences.getBoolean(pauseSearchHistoryKey, false)) {
            db.queries("%${textFieldValue.text}%")
                .distinctUntilChanged { old, new -> old.size == new.size }
                .collect { history = it }
        }
    }

    LaunchedEffect(textFieldValue.text) {
        if (textFieldValue.text != confirmedSearchQuery) {
            confirmedSearchQuery = null
        }

        if (textFieldValue.text.isNotEmpty() && confirmedSearchQuery == null) {
            isLoadingSuggestions = true
            delay(300)
            suggestionsResult = withContext(Dispatchers.IO) {
                Innertube.searchSuggestions(input = textFieldValue.text)
            }
            isLoadingSuggestions = false
        } else {
            isLoadingSuggestions = false
            suggestionsResult = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorPalette.background4)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.statusBarsPadding()
        ) {
            NewSearchBar(
                text = textFieldValue.text,
                onTextChange = { textFieldValue = TextFieldValue(it) },
                onSearch = { performSearch(it) },
                placeholderText = stringResource(R.string.search),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                onClear = {
                    textFieldValue = TextFieldValue("")
                    confirmedSearchQuery = null
                    isLoadingSuggestions = false
                },
                onMicClick = { launchVoiceSearch() },
                onBackClick = { navController.popBackStack() },
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        when {
            confirmedSearchQuery != null -> {
                OnlineSearch(
                    searchResults = searchViewModel.searchResults,
                    isLoading = searchViewModel.isLoading,
                    onAlbumClick = onAlbumClick,
                    onArtistClick = onArtistClick,
                    onViewAllClick = { category ->
                        navController.navigate("${Routes.SearchResult}/${confirmedSearchQuery}/$category")
                    }
                )
            }

            textFieldValue.text.isNotEmpty() -> {
                SettingsCard(
                    shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
                ){
                    if (isLoadingSuggestions) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LoadingAnimation(modifier = Modifier.size(50.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.loading),
                                    style = typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn {
                            suggestionsResult?.getOrNull()?.let { suggestions ->
                                items(items = suggestions) { suggestion ->
                                    SuggestionItem(
                                        text = suggestion,
                                        onClick = { performSearch(suggestion) },
                                        color = colorPalette.text
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 18.dp),
                                        color = colorPalette.text.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            history.isNotEmpty() -> {
                HistorySection(
                    history = history,
                    onHistoryClick = { performSearch(it) },
                    onDeleteClick = { query { db.delete(it) } },
                    onClearAllClick = { query { db.clearQueries() } },
                    colorPalette = colorPalette
                )
            }

            else -> {
                EmptySearchPlaceholder(color = colorPalette.text)
            }
        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun SuggestionItem(text: String, onClick: () -> Unit, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = typography.titleMedium.copy(fontWeight = FontWeight.Medium, color = color),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HistorySection(
    history: List<SearchQuery>,
    onHistoryClick: (String) -> Unit,
    onDeleteClick: (SearchQuery) -> Unit,
    onClearAllClick: () -> Unit,
    colorPalette: com.github.core.ui.ColorPalette
) {
    Column {
        Text(
            text = stringResource(R.string.search_history),
            style = typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = colorPalette.text.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 22.dp)
        )

        SettingsCard {
            LazyColumn {
                items(history) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onHistoryClick(item.query) }
                            .padding(horizontal = 22.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.query,
                            style = typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            color = colorPalette.text
                        )
                        IconButton(
                            onClick = { onDeleteClick(item) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Outlined.Clear, "Delete", tint = colorPalette.text)
                        }
                    }
                }
                item {
                    TextButton(
                        onClick = onClearAllClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = colorPalette.text)
                    ) {
                        Text(stringResource(R.string.clear))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySearchPlaceholder(color: androidx.compose.ui.graphics.Color) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.no_search_history),
            style = typography.titleMedium.copy(color = color, fontSize = 20.sp),
            textAlign = TextAlign.Center
        )
    }
}