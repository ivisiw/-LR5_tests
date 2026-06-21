package com.example.lr3.data

import com.example.lr3.data.local.FavouriteDao
import com.example.lr3.data.local.FavouriteEntity
import com.example.lr3.data.remote.CharacterApi
import com.example.lr3.data.remote.CharacterDto
import com.example.lr3.data.remote.CharacterResponse
import com.example.lr3.data.remote.LocationDto
import com.example.lr3.data.remote.PageInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

class CharacterRepositoryTest {

    private lateinit var api: CharacterApi
    private lateinit var dao: FavouriteDao
    private lateinit var repository: CharacterRepository

    private val rickDto = CharacterDto(
        id = 1,
        name = "Rick Sanchez",
        status = "Alive",
        species = "Human",
        type = "",
        gender = "Male",
        origin = LocationDto("Earth"),
        location = LocationDto("Earth"),
        episode = listOf("ep1", "ep2"),
        created = "2017-11-04T18:48:46.250Z"
    )

    @Before
    fun setUp() {
        api = mockk()
        dao = mockk()
        repository = CharacterRepository(api, dao)
    }

    @Test
    fun `searchCharacters maps dto to domain and marks favourites correctly`() = runTest {
        coEvery { dao.getAll() } returns listOf(
            FavouriteEntity(1, "Rick Sanchez", "Alive", "Human", "", "Male", "Earth", "Earth", 2, "2017-11-04")
        )
        coEvery { api.getCharacters("", 1, "") } returns CharacterResponse(
            info = PageInfo(next = null),
            results = listOf(rickDto)
        )

        val result = repository.searchCharacters("", 1, "")

        assertEquals(1, result.characters.size)
        assertTrue(result.characters[0].isFavourite)
        assertFalse(result.hasNextPage)
        assertEquals("Rick Sanchez", result.characters[0].name)
        assertEquals(2, result.characters[0].episodeCount)
        assertEquals("2017-11-04", result.characters[0].created)
    }

    @Test
    fun `searchCharacters returns empty result on 404 instead of throwing`() = runTest {
        coEvery { dao.getAll() } returns emptyList()
        val httpException = HttpException(
            Response.error<Any>(404, "".toResponseBody("application/json".toMediaTypeOrNull()))
        )
        coEvery { api.getCharacters(any(), any(), any()) } throws httpException

        val result = repository.searchCharacters("xyz", 1, "")

        assertTrue(result.characters.isEmpty())
        assertFalse(result.hasNextPage)
    }

    @Test
    fun `searchCharacters rethrows non-404 http errors`() = runTest {
        coEvery { dao.getAll() } returns emptyList()
        val httpException = HttpException(
            Response.error<Any>(500, "".toResponseBody("application/json".toMediaTypeOrNull()))
        )
        coEvery { api.getCharacters(any(), any(), any()) } throws httpException

        try {
            repository.searchCharacters("", 1, "")
            fail("Expected HttpException to be thrown")
        } catch (e: HttpException) {
            assertEquals(500, e.code())
        }
    }

    @Test
    fun `searchCharacters does not refetch favourites on page greater than 1`() = runTest {
        coEvery { dao.getAll() } returns emptyList()
        coEvery { api.getCharacters(any(), any(), any()) } returns CharacterResponse(
            info = PageInfo(next = null),
            results = listOf(rickDto)
        )

        repository.searchCharacters("", 1, "")
        repository.searchCharacters("", 2, "")

        coVerify(exactly = 1) { dao.getAll() }
    }

    @Test
    fun `removeFavourite deletes by id`() = runTest {
        coEvery { dao.deleteById(7) } returns Unit

        repository.removeFavourite(7)

        coVerify(exactly = 1) { dao.deleteById(7) }
    }

    @Test
    fun `getFavourites maps entities to domain characters with isFavourite true`() = runTest {
        coEvery { dao.getAll() } returns listOf(
            FavouriteEntity(3, "Summer Smith", "Alive", "Human", "", "Female", "Earth", "Earth", 5, "2017-11-04")
        )

        val result = repository.getFavourites()

        assertEquals(1, result.size)
        assertTrue(result[0].isFavourite)
        assertEquals("Summer Smith", result[0].name)
    }

    @Test
    fun `getCharacter combines api result with favourite flag from dao`() = runTest {
        coEvery { dao.isFavourite(1) } returns true
        coEvery { api.getCharacterById(1) } returns rickDto

        val result = repository.getCharacter(1)

        assertTrue(result.isFavourite)
        assertEquals("Rick Sanchez", result.name)
    }
}