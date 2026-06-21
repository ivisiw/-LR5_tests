package com.example.lr3.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.lr3.data.local.AppDatabase
import com.example.lr3.data.remote.CharacterApi
import com.example.lr3.model.Character
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CharacterRepositoryIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: CharacterRepository
    private val api: CharacterApi = mockk()

    private val rick = Character(
        id = 1, name = "Rick Sanchez", status = "Alive", species = "Human",
        type = "", gender = "Male", originName = "Earth", locationName = "Earth",
        episodeCount = 10, created = "2017-11-04"
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = CharacterRepository(api, db.favouriteDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `addFavourite then getFavourites returns the same character from real Room`() = runTest {
        repository.addFavourite(rick)

        val favourites = repository.getFavourites()

        assertEquals(1, favourites.size)
        assertEquals("Rick Sanchez", favourites[0].name)
        assertTrue(favourites[0].isFavourite)
    }

    @Test
    fun `adding the same favourite twice does not create a duplicate`() = runTest {
        repository.addFavourite(rick)
        repository.addFavourite(rick)

        val favourites = repository.getFavourites()

        assertEquals(1, favourites.size)
    }

    @Test
    fun `removeFavourite actually deletes from real Room database`() = runTest {
        repository.addFavourite(rick)
        assertTrue(repository.isFavourite(1))

        repository.removeFavourite(1)

        assertFalse(repository.isFavourite(1))
        assertTrue(repository.getFavourites().isEmpty())
    }
}