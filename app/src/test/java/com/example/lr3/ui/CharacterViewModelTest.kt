package com.example.lr3.ui

import com.example.lr3.data.CharacterRepository
import com.example.lr3.data.CharactersResult
import com.example.lr3.model.Character
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class CharacterViewModelTest {

    @get:Rule
    val testDispatcherRule = TestDispatcherRule()

    private lateinit var repository: CharacterRepository
    private lateinit var viewModel: CharacterViewModel

    private val rick = Character(
        id = 1, name = "Rick Sanchez", status = "Alive", species = "Human",
        type = "", gender = "Male", originName = "Earth", locationName = "Earth",
        episodeCount = 10, created = "2017-11-04", isFavourite = false
    )
    private val morty = Character(
        id = 2, name = "Morty Smith", status = "Alive", species = "Human",
        type = "", gender = "Male", originName = "Earth", locationName = "Earth",
        episodeCount = 8, created = "2017-11-04", isFavourite = false
    )

    @Before
    fun setUp() {
        repository = mockk()
        coEvery { repository.getFavourites() } returns emptyList()
    }

    @Test
    fun `initial state loads characters and ends in Success`() = runTest {
        coEvery { repository.searchCharacters("", 1, "") } returns
                CharactersResult(listOf(rick, morty), hasNextPage = false)

        viewModel = CharacterViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState
        assertTrue(state is CharacterUiState.Success)
        assertEquals(2, (state as CharacterUiState.Success).characters.size)
    }

    @Test
    fun `initial state is Loading before data arrives`() = runTest {
        coEvery { repository.searchCharacters("", 1, "") } coAnswers {
            delay(100)
            CharactersResult(listOf(rick, morty), hasNextPage = false)
        }

        viewModel = CharacterViewModel(repository)

        assertTrue(viewModel.uiState is CharacterUiState.Loading)

        advanceUntilIdle()
        assertTrue(viewModel.uiState is CharacterUiState.Success)
    }

    @Test
    fun `successful search updates state with characters`() = runTest {
        coEvery { repository.searchCharacters("", 1, "") } returns
                CharactersResult(emptyList(), hasNextPage = false)
        viewModel = CharacterViewModel(repository)
        advanceUntilIdle()

        coEvery { repository.searchCharacters("Rick", 1, "") } returns
                CharactersResult(listOf(rick), hasNextPage = false)

        viewModel.onSearchChange("Rick")
        advanceTimeBy(600)
        advanceUntilIdle()

        val state = viewModel.uiState
        assertTrue(state is CharacterUiState.Success)
        assertEquals(1, (state as CharacterUiState.Success).characters.size)
        assertEquals("Rick Sanchez", state.characters[0].name)
    }

    @Test
    fun `error during load updates state to Error`() = runTest {
        coEvery { repository.searchCharacters("", 1, "") } throws IOException("Network error")

        viewModel = CharacterViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState is CharacterUiState.Error)
    }

    @Test
    fun `retry after error re-fetches data and clears error`() = runTest {
        coEvery { repository.searchCharacters("", 1, "") } throws IOException("Error")
        viewModel = CharacterViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState is CharacterUiState.Error)

        coEvery { repository.searchCharacters("", 1, "") } returns
                CharactersResult(listOf(rick), hasNextPage = false)

        viewModel.retry()
        advanceUntilIdle()

        assertTrue(viewModel.uiState is CharacterUiState.Success)
        coVerify(exactly = 2) { repository.searchCharacters("", 1, "") }
    }
    @Test
    fun `empty search result produces Empty state, not Success with empty list`() = runTest {
        coEvery { repository.searchCharacters("", 1, "") } returns
                CharactersResult(emptyList(), hasNextPage = false)
        viewModel = CharacterViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState is CharacterUiState.Empty)
        assertFalse(viewModel.uiState is CharacterUiState.Success)
    }

    @Test
    fun `loadCharacter updates detailState to Success`() = runTest {
        coEvery { repository.searchCharacters("", 1, "") } returns
                CharactersResult(emptyList(), hasNextPage = false)
        coEvery { repository.getCharacter(1) } returns rick
        viewModel = CharacterViewModel(repository)
        advanceUntilIdle()

        viewModel.loadCharacter(1)
        advanceUntilIdle()

        val detailState = viewModel.detailState
        assertTrue(detailState is CharacterDetailUiState.Success)
        assertEquals("Rick Sanchez", (detailState as CharacterDetailUiState.Success).character.name)
    }


    @Test
    fun `rapid search ignores stale response from previous query`() = runTest {
        coEvery { repository.searchCharacters("", 1, "") } returns
                CharactersResult(emptyList(), hasNextPage = false)
        viewModel = CharacterViewModel(repository)
        advanceUntilIdle()


        coEvery { repository.searchCharacters("Rick", 1, "") } coAnswers {
            delay(1000)
            CharactersResult(listOf(rick), hasNextPage = false)
        }

        coEvery { repository.searchCharacters("Morty", 1, "") } returns
                CharactersResult(listOf(morty), hasNextPage = false)

        viewModel.onSearchChange("Rick")
        advanceTimeBy(550)
        viewModel.onSearchChange("Morty")
        advanceUntilIdle()

        val state = viewModel.uiState
        assertTrue(state is CharacterUiState.Success)
        val characters = (state as CharacterUiState.Success).characters
        assertEquals(1, characters.size)
        assertEquals("Morty Smith", characters[0].name)
    }
    @Test
    fun `clicking favourite twice toggles state without duplicating`() = runTest {
        coEvery { repository.searchCharacters("", 1, "") } returns
                CharactersResult(listOf(rick), hasNextPage = false)
        coEvery { repository.isFavourite(1) } returns false andThen true
        coEvery { repository.addFavourite(any()) } returns Unit
        coEvery { repository.removeFavourite(1) } returns Unit

        viewModel = CharacterViewModel(repository)
        advanceUntilIdle()

        viewModel.onFavouriteClick(rick)
        advanceUntilIdle()
        viewModel.onFavouriteClick(rick)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.addFavourite(any()) }
        coVerify(exactly = 1) { repository.removeFavourite(1) }
    }
}