package com.example.lr3.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lr3.data.CharacterRepository
import com.example.lr3.model.Character
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

sealed class CharacterUiState {
    object Loading : CharacterUiState()
    data class Success(
        val characters: List<Character>,
        val endReached: Boolean = false,
        val paginationError: Boolean = false
    ) : CharacterUiState()
    object Empty : CharacterUiState()
    data class Error(val message: String) : CharacterUiState()
}

sealed class CharacterDetailUiState {
    object Loading : CharacterDetailUiState()
    data class Success(val character: Character) : CharacterDetailUiState()
    data class Error(val message: String) : CharacterDetailUiState()
}

private data class ListLoadState(
    val query: String = "",
    val status: String = "",
    val page: Int = 1,
    val characters: List<Character> = emptyList(),
    val endReached: Boolean = false
)

@HiltViewModel
class CharacterViewModel @Inject constructor(
    private val repository: CharacterRepository
) : ViewModel() {

    var uiState by mutableStateOf<CharacterUiState>(CharacterUiState.Loading)
        private set

    var detailState by mutableStateOf<CharacterDetailUiState>(CharacterDetailUiState.Loading)
        private set

    var searchQuery by mutableStateOf("")
        private set

    var selectedStatus by mutableStateOf("")
        private set

    var favourites by mutableStateOf<List<Character>>(emptyList())
        private set

    var favouritesError by mutableStateOf<String?>(null)
        private set

    private var loadState = ListLoadState()
    private var loadJob: Job? = null
    private var detailRequestId = 0
    private var isLoadingMore = false

    init {
        loadInitial()
    }

    fun loadInitial() {
        loadState = ListLoadState(query = searchQuery, status = selectedStatus)
        startLoad(loadMore = false)
    }

    fun onSearchChange(query: String) {
        searchQuery = query
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            delay(500)
            loadState = ListLoadState(query = query, status = selectedStatus)
            startLoad(loadMore = false)
        }
    }

    fun onStatusChange(status: String) {
        selectedStatus = status
        loadState = ListLoadState(query = searchQuery, status = status)
        startLoad(loadMore = false)
    }

    fun loadNextPage() {
        if (loadState.endReached || isLoadingMore) return
        startLoad(loadMore = true)
    }

    fun retry() {
        loadInitial()
    }

    private fun startLoad(loadMore: Boolean) {
        if (!loadMore) {
            loadJob?.cancel()
        } else {
            isLoadingMore = true
        }
        loadJob = viewModelScope.launch {
            if (!loadMore) {
                loadFavourites()
                uiState = CharacterUiState.Loading
            }
            try {
                val page = if (loadMore) loadState.page else 1
                val result = repository.searchCharacters(
                    query = loadState.query,
                    page = page,
                    status = loadState.status
                )

                if (result.characters.isEmpty()) {
                    loadState = loadState.copy(endReached = true)
                    uiState = if (loadMore && loadState.characters.isNotEmpty()) {
                        CharacterUiState.Success(
                            characters = loadState.characters,
                            endReached = true
                        )
                    } else {
                        CharacterUiState.Empty
                    }
                } else {
                    val newCharacters = if (loadMore) loadState.characters + result.characters
                    else result.characters
                    val endReached = !result.hasNextPage
                    loadState = loadState.copy(
                        characters = newCharacters,
                        page = page + 1,
                        endReached = endReached
                    )
                    uiState = CharacterUiState.Success(
                        characters = newCharacters,
                        endReached = endReached
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!loadMore) {
                    uiState = CharacterUiState.Error("Ошибка загрузки")
                } else {
                    uiState = CharacterUiState.Success(
                        characters = loadState.characters,
                        endReached = loadState.endReached,
                        paginationError = true
                    )
                }
            } finally {
                if (loadMore) isLoadingMore = false
            }
        }
    }

    private suspend fun loadFavourites() {
        try {
            favourites = repository.getFavourites()
            favouritesError = null
        } catch (e: Exception) {
            favouritesError = "Не удалось загрузить избранное"
        }
    }

    fun loadCharacter(id: Int) {
        viewModelScope.launch {
            val currentRequest = ++detailRequestId
            detailState = CharacterDetailUiState.Loading
            try {
                val result = repository.getCharacter(id)
                if (currentRequest != detailRequestId) return@launch
                detailState = CharacterDetailUiState.Success(result)
            } catch (e: Exception) {
                if (currentRequest != detailRequestId) return@launch
                detailState = CharacterDetailUiState.Error("Ошибка загрузки")
            }
        }
    }

    fun onFavouriteClick(character: Character) {
        viewModelScope.launch {
            try {
                val isFavInDb = repository.isFavourite(character.id)
                if (isFavInDb) {
                    repository.removeFavourite(character.id)
                } else {
                    repository.addFavourite(character)
                }
                val newIsFav = !isFavInDb

                val updatedCharacters = loadState.characters.map {
                    if (it.id == character.id) it.copy(isFavourite = newIsFav) else it
                }
                loadState = loadState.copy(characters = updatedCharacters)

                val state = uiState
                if (state is CharacterUiState.Success) {
                    uiState = state.copy(characters = updatedCharacters)
                }

                val det = detailState
                if (det is CharacterDetailUiState.Success && det.character.id == character.id) {
                    detailState = CharacterDetailUiState.Success(
                        det.character.copy(isFavourite = newIsFav)
                    )
                }

                loadFavourites()
            } catch (e: Exception) {
                favouritesError = "Не удалось обновить избранное"
            }
        }
    }
}