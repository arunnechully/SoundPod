@file:OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class
)

package com.github.soundpod.ui.screens.home.style

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import com.github.soundpod.ui.navigation.Routes
import com.github.soundpod.ui.screens.home.QuickPicks
import com.github.soundpod.utils.appearanceUpdatedKey
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.updateAvailableKey
import kotlin.math.roundToInt

@Composable
fun AppleHome(
    navController: NavController,
    onSettingsClick: () -> Unit,
) {
    val updateAvailable by rememberPreference(updateAvailableKey, false)
    val appearanceUpdated by rememberPreference(appearanceUpdatedKey, false)
    val showSettingsBadge = updateAvailable || appearanceUpdated

    val navigateToAlbum = { browseId: String ->
        navController.navigate(route = Routes.Album(id = browseId))
    }
    val navigateToArtist = { browseId: String ->
        navController.navigate(route = Routes.Artist(id = browseId))
    }

    val (colorPalette) = LocalAppearance.current

    val density = LocalDensity.current
    val headerHeight = 64.dp
    val headerHeightPx = with(density) { headerHeight.toPx() }

    var headerOffsetPx by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0) {
                    val newOffset = headerOffsetPx + delta
                    val previousOffset = headerOffsetPx
                    headerOffsetPx = newOffset.coerceIn(-headerHeightPx, 0f)
                    val consumed = headerOffsetPx - previousOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 0) {
                    val newOffset = headerOffsetPx + available.y
                    val previousOffset = headerOffsetPx
                    headerOffsetPx = newOffset.coerceIn(-headerHeightPx, 0f)
                    val headerConsumed = headerOffsetPx - previousOffset
                    return Offset(0f, headerConsumed)
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorPalette.background0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                color = colorPalette.background2,
                tonalElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(headerHeight)
                                .offset { IntOffset(0, headerOffsetPx.roundToInt()) }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.music_icon),
                                contentDescription = null,
                                tint = colorPalette.text,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "SoundPod",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorPalette.text
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            OutlinedButton(
                                onClick = { navController.navigate(route = Routes.Search) },
                                shape = RoundedCornerShape(60),
                                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.search),
                                    tint = colorPalette.text,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.search),
                                    color = colorPalette.text,
                                    style = typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedIconButton(
                                onClick = onSettingsClick,
                                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                                modifier = Modifier.size(36.dp)
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (showSettingsBadge) {
                                            Badge(
                                                modifier = Modifier.size(8.dp),
                                                containerColor = Color.Red
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = colorPalette.text,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .offset { IntOffset(0, headerOffsetPx.roundToInt()) }
                        ) {
                            QuickPicks(
                                onAlbumClick = navigateToAlbum,
                                onArtistClick = navigateToArtist,
                                onPlaylistClick = { browseId ->
                                    navController.navigate(route = Routes.Playlist(id = browseId))
                                },
                                onHistoryViewAllClick = {
                                    navController.navigate(route = Routes.History)
                                },
                                onOfflinePlaylistClick = {
                                    navController.navigate(route = Routes.BuiltInPlaylist(index = 1))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
