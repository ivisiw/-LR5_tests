package com.example.lr3.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lr3.model.Character
import com.example.lr3.ui.CharacterUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    uiState: CharacterUiState,
    searchQuery: String,
    selectedStatus: String,
    favourites: List<Character>,
    favouritesError: String?,
    onSearchChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onFavouriteClick: (Character) -> Unit
) {
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Rick & Morty", fontWeight = FontWeight.Bold) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Поиск по имени") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    listOf("" to "Все", "alive" to "Живые", "dead" to "Мёртвые", "unknown" to "Неизвестно")
                ) { (value, label) ->
                    FilterChip(
                        selected = selectedStatus == value,
                        onClick = { onStatusChange(value) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (favouritesError != null) {
                Text(
                    text = favouritesError,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            when (uiState) {
                is CharacterUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Загрузка")
                        }
                    }
                }

                is CharacterUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(uiState.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onRetry) { Text("Повторить") }
                        }
                    }
                }

                is CharacterUiState.Empty -> {
                    val showFavourites = searchQuery.isBlank() &&
                            selectedStatus.isBlank() &&
                            favourites.isNotEmpty()

                    if (showFavourites) {
                        LazyColumn {
                            item {
                                Text(
                                    "Избранные",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(favourites, key = { "fav_${it.id}" }) { character ->
                                CharacterCard(
                                    character = character,
                                    onClick = { onCharacterClick(character.id) },
                                    onFavouriteClick = onFavouriteClick
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Ничего не найдено")
                        }
                    }
                }

                is CharacterUiState.Success -> {
                    val favouriteIds = favourites.map { it.id }.toSet()

                    val shouldLoadMore = remember {
                        derivedStateOf {
                            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                            val total = listState.layoutInfo.totalItemsCount
                            lastVisible != null && lastVisible.index >= total - 3
                        }
                    }
                    LaunchedEffect(shouldLoadMore.value) {
                        if (shouldLoadMore.value && !uiState.endReached) onLoadMore()
                    }

                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (searchQuery.isBlank() && selectedStatus.isBlank() && favourites.isNotEmpty()) {
                            item {
                                Text(
                                    "Избранные",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(favourites, key = { "fav_${it.id}" }) { character ->
                                CharacterCard(
                                    character = character,
                                    onClick = { onCharacterClick(character.id) },
                                    onFavouriteClick = onFavouriteClick
                                )
                            }
                            item {
                                Text(
                                    "Все персонажи",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }

                        val nonFavouriteCharacters = if (searchQuery.isBlank() && selectedStatus.isBlank()) {
                            uiState.characters.filter { it.id !in favouriteIds }
                        } else {
                            uiState.characters
                        }

                        items(nonFavouriteCharacters, key = { it.id }) { character ->
                            CharacterCard(
                                character = character,
                                onClick = { onCharacterClick(character.id) },
                                onFavouriteClick = onFavouriteClick
                            )
                        }

                        if (uiState.paginationError) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Не удалось загрузить страницу",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(onClick = onLoadMore) { Text("Повторить") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterCard(
    character: Character,
    onClick: () -> Unit,
    onFavouriteClick: (Character) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(character.name, fontWeight = FontWeight.Bold)
                Text("${character.species} · ${character.status}")
                if (character.locationName.isNotBlank()) {
                    Text(
                        "${character.locationName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = { onFavouriteClick(character) }) {
                Text(if (character.isFavourite) "★" else "☆")
            }
        }
    }
}