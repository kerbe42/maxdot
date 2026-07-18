package com.maxdot.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxdot.app.GameViewModel
import com.maxdot.app.MainViewModel
import com.maxdot.app.data.Book
import com.maxdot.app.ui.screens.GameScreen
import com.maxdot.app.ui.screens.LibraryScreen
import com.maxdot.app.ui.screens.RewardsScreen
import com.maxdot.app.ui.screens.SettingsScreen
import com.maxdot.app.ui.screens.WorldMapScreen
import com.maxdot.app.ui.theme.GameThemes
import com.maxdot.app.ui.theme.MaxDotTheme

sealed class Screen {
    data object Library : Screen()
    data class WorldMap(val book: Book) : Screen()
    data class Game(val book: Book, val boss: Boolean = false) : Screen()
    data object Rewards : Screen()
    data object Settings : Screen()
}

@Composable
fun App(mainViewModel: MainViewModel) {
    val settings by mainViewModel.profiles.settings.collectAsStateWithLifecycle()

    var screen by remember { mutableStateOf<Screen>(Screen.Library) }
    var gameSessionKey by rememberSaveable { mutableStateOf(0) }

    MaxDotTheme(settings) {
        val theme = GameThemes.resolve(settings.selectedTheme)
        val bgStops = theme.colors.bg.let { if (it.size == 1) it + it else it }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(bgStops)),
        ) {
            when (val current = screen) {
                is Screen.Library -> LibraryScreen(
                    mainViewModel = mainViewModel,
                    onPlay = { book -> screen = Screen.WorldMap(book) },
                    onOpenRewards = { screen = Screen.Rewards },
                    onOpenSettings = { screen = Screen.Settings },
                )

                is Screen.WorldMap -> {
                    BackHandler { screen = Screen.Library }
                    WorldMapScreen(
                        book = current.book,
                        mainViewModel = mainViewModel,
                        onPlay = { boss ->
                            gameSessionKey++
                            screen = Screen.Game(current.book, boss)
                        },
                        onBack = { screen = Screen.Library },
                    )
                }

                is Screen.Game -> {
                    BackHandler { screen = Screen.WorldMap(current.book) }
                    val gameViewModel: GameViewModel = viewModel(
                        key = "game_${current.book.id}_$gameSessionKey",
                        factory = viewModelFactory {
                            initializer {
                                GameViewModel(
                                    book = current.book,
                                    bookRepo = mainViewModel.books,
                                    profileRepo = mainViewModel.profiles,
                                    boss = current.boss,
                                )
                            }
                        },
                    )
                    GameScreen(
                        book = current.book,
                        viewModel = gameViewModel,
                        mainViewModel = mainViewModel,
                        onExit = { screen = Screen.WorldMap(current.book) },
                    )
                }

                is Screen.Rewards -> {
                    BackHandler { screen = Screen.Library }
                    RewardsScreen(
                        mainViewModel = mainViewModel,
                        onBack = { screen = Screen.Library },
                    )
                }

                is Screen.Settings -> {
                    BackHandler { screen = Screen.Library }
                    SettingsScreen(
                        mainViewModel = mainViewModel,
                        onBack = { screen = Screen.Library },
                    )
                }
            }
        }
    }
}
