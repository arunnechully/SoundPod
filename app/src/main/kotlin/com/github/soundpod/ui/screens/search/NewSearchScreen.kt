package com.github.soundpod.ui.screens.search

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.github.core.ui.LocalAppearance
import com.github.innertube.Innertube
import com.github.innertube.requests.searchSuggestions
import com.github.soundpod.Database
import com.github.soundpod.R
import com.github.soundpod.models.SearchQuery
import com.github.soundpod.query
import com.github.soundpod.ui.components.LoadingAnimation
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.navigation.Routes
import com.github.soundpod.utils.pauseSearchHistoryKey
import com.github.soundpod.utils.preferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun NewSearchScreen(
    initialTextInput: String = "",
    navController: NavController,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    BackHandler { navController.popBackStack() }

    var textFieldValue by remember { mutableStateOf(TextFieldValue(initialTextInput)) }
    var confirmedSearchQuery: String? by rememberSaveable { mutableStateOf(null) }

    var history: List<SearchQuery> by remember { mutableStateOf(emptyList()) }
    var suggestionsResult: Result<List<String>?>? by remember { mutableStateOf(null) }

    var isLoadingSuggestions by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current

    val performSearch: (String) -> Unit = { query ->
        if (query.isNotBlank()) {
            textFieldValue = TextFieldValue(query)
            confirmedSearchQuery = query
            if (!context.preferences.getBoolean(pauseSearchHistoryKey, false)) {
                query {
                    Database.insert(SearchQuery(query = query))
                }
            }
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                performSearch(spokenText)
            }
        }
    }

    val launchVoiceSearch = remember {
        {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
            }
            try {
                voiceLauncher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "Voice search not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(textFieldValue.text) {
        if (!context.preferences.getBoolean(pauseSearchHistoryKey, false)) {
            Database.queries("%${textFieldValue.text}%")
                .distinctUntilChanged { old, new -> old.size == new.size }
                .collect { history = it }
        }
    }
    LaunchedEffect(textFieldValue.text) {
        if (textFieldValue.text != confirmedSearchQuery) {
            confirmedSearchQuery = null
        }

        if (textFieldValue.text.isNotEmpty()) {
            isLoadingSuggestions = true
            delay(200)
            suggestionsResult = Innertube.searchSuggestions(input = textFieldValue.text)
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
                onTextChange = {
                    textFieldValue = TextFieldValue(it)
                },
                onSearch = { queryText ->
                    performSearch(queryText)
                },
                placeholderText = stringResource(R.string.search),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        focusRequester.freeFocus()
                    },
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

// Inside NewSearchScreen.kt

        if (confirmedSearchQuery != null) {
            OnlineSearch(
                query = textFieldValue.text,
                onAlbumClick = onAlbumClick,
                onArtistClick = onArtistClick,
                onViewAllClick = { category ->
                    navController.navigate("${Routes.SearchResult}/${textFieldValue.text}/$category")
                }
            )
        }

        else if (textFieldValue.text.isNotEmpty()) {
            SettingsCard {
                if (isLoadingSuggestions) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize(),
                    ) {
                        LoadingAnimation(
                            modifier = Modifier.size(50.dp)
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "Loading...",
                            style = typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn {
                        suggestionsResult?.getOrNull()?.let { suggestions ->
                            items(items = suggestions) { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { performSearch(suggestion) }
                                        .padding(horizontal = 22.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = suggestion,
                                        style = typography.titleMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = colorPalette.text
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                HorizontalDivider(
                                    modifier = Modifier
                                        .padding(horizontal = 18.dp)
                                        .fillMaxWidth(),
                                    color = colorPalette.text.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }
            }
        }
        else if (history.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.search_history),
                    style = typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = colorPalette.text.copy(alpha = 0.5f)
                )
            }

            SettingsCard {
                LazyColumn {
                    items(history) { searchQuery ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { performSearch(searchQuery.query) }
                                .padding(horizontal = 22.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = searchQuery.query,
                                style = typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                color = colorPalette.text
                            )
                            IconButton(
                                onClick = { query { Database.delete(searchQuery) } },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "Delete",
                                    tint = colorPalette.text
                                )
                            }
                        }
                    }

                    item {
                        HorizontalDivider(
                            modifier = Modifier
                                .padding(horizontal = 18.dp)
                                .fillMaxWidth(),
                            color = colorPalette.text.copy(alpha = 0.1f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = { query { Database.clearQueries() } },
                                colors = ButtonDefaults.textButtonColors(contentColor = colorPalette.text)
                            ) {
                                Text("Clear all")
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = stringResource(R.string.no_search_history),
                    style = typography.titleMedium.copy(
                        color = colorPalette.text,
                        fontSize = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}