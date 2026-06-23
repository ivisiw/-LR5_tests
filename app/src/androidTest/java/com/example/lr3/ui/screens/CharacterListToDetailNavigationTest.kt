package com.example.lr3.ui.screens

import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lr3.model.Character
import com.example.lr3.ui.CharacterDetailUiState
import com.example.lr3.ui.CharacterUiState
import org.junit.Rule
import org.junit.Test

class CharacterListToDetailNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val rick = Character(
        id = 1, name = "Rick Sanchez", status = "Alive", species = "Human",
        type = "", gender = "Male", originName = "Earth", locationName = "Earth",
        episodeCount = 10, created = "2017-11-04"
    )
    private val morty = Character(
        id = 2, name = "Morty Smith", status = "Alive", species = "Human",
        type = "", gender = "Male", originName = "Earth", locationName = "Earth",
        episodeCount = 8, created = "2017-11-04"
    )

    @Test
    fun clickingCharacterCardNavigatesToDetailScreenForThatCharacter() {
        composeRule.setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "list") {
                composable("list") {
                    CharacterListScreen(
                        uiState = CharacterUiState.Success(characters = listOf(rick, morty)),
                        searchQuery = "",
                        selectedStatus = "",
                        favourites = emptyList(),
                        favouritesError = null,
                        onSearchChange = {},
                        onStatusChange = {},
                        onCharacterClick = { id -> navController.navigate("detail/$id") },
                        onRetry = {},
                        onLoadMore = {},
                        onFavouriteClick = {}
                    )
                }
                composable("detail/{characterId}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("characterId")?.toIntOrNull()
                    val character = remember(id) { listOf(rick, morty).first { it.id == id } }

                    CharacterDetailScreen(
                        uiState = CharacterDetailUiState.Success(character),
                        onRetry = {},
                        onBack = { navController.navigateUp() },
                        onFavouriteClick = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("Morty Smith").performClick()

        composeRule.onNodeWithText("Добавить в избранное").assertExists()
    }
}