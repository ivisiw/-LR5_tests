package com.example.lr3.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.lr3.model.Character
import com.example.lr3.ui.CharacterUiState
import org.junit.Rule
import org.junit.Test

class CharacterListScreenTest {

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
    fun clickingCharacterCardTriggersNavigationWithCorrectId() {
        var clickedId: Int? = null

        composeRule.setContent {
            CharacterListScreen(
                uiState = CharacterUiState.Success(characters = listOf(rick, morty)),
                searchQuery = "",
                selectedStatus = "",
                favourites = emptyList(),
                favouritesError = null,
                onSearchChange = {},
                onStatusChange = {},
                onCharacterClick = { id -> clickedId = id },
                onRetry = {},
                onLoadMore = {},
                onFavouriteClick = {}
            )
        }

        composeRule.onNodeWithText("Morty Smith").performClick()

        assert(clickedId == 2) { "Ожидался клик по id=2 (Morty), получено: $clickedId" }
    }

    @Test
    fun errorStateShowsRetryButtonAndClickingItCallsOnRetry() {
        var retried = false

        composeRule.setContent {
            CharacterListScreen(
                uiState = CharacterUiState.Error("Ошибка загрузки"),
                searchQuery = "",
                selectedStatus = "",
                favourites = emptyList(),
                favouritesError = null,
                onSearchChange = {},
                onStatusChange = {},
                onCharacterClick = {},
                onRetry = { retried = true },
                onLoadMore = {},
                onFavouriteClick = {}
            )
        }

        composeRule.onNodeWithText("Повторить").performClick()

        assert(retried) { "onRetry не был вызван после клика на кнопку" }
    }
}