package com.github.musicyou.ui.screens.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.musicyou.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchInputField(
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    searchText: String?,
    onSearch: (String) -> Unit,
    pop: () -> Unit,
    scope: CoroutineScope
) {
    SearchBarDefaults.InputField(
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        onSearch = onSearch,
        placeholder = {
            Text(text = stringResource(id = R.string.search))
        },
        leadingIcon = {
            IconButton(
                onClick = {
                    when (searchBarState.currentValue) {
                        SearchBarValue.Collapsed -> pop()
                        SearchBarValue.Expanded ->
                            if (searchText == null) pop()
                            else scope.launch {
                                searchBarState.animateToCollapsed()
                            }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null
                )
            }
        },
        trailingIcon = {
            if (textFieldState.text.isNotBlank() && searchBarState.currentValue == SearchBarValue.Expanded) {
                IconButton(onClick = { textFieldState.clearText() }) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = null
                    )
                }
            }
        }
    )
}