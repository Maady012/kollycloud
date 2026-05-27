package com.lagradost.cloudstream3.ui.kollygame

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface KollywoodUiState {
    object Loading : KollywoodUiState
    data class Success(
        val trending: List<TmdbMovie>,
        val topRated: List<TmdbMovie>,
        val upcoming: List<TmdbMovie>,
        val isDemoMode: Boolean
    ) : KollywoodUiState
    data class Error(
        val message: String,
        val fallbackTrending: List<TmdbMovie>? = null,
        val fallbackTopRated: List<TmdbMovie>? = null,
        val fallbackUpcoming: List<TmdbMovie>? = null
    ) : KollywoodUiState
}

class KollyGameViewModel : ViewModel() {

    private val _kollywoodState = MutableStateFlow<KollywoodUiState>(KollywoodUiState.Loading)
    val kollywoodState: StateFlow<KollywoodUiState> = _kollywoodState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val curatedTrailers = mapOf(
        991101L to "h8o0x8i_m2U", // Amaran
        991102L to "Po3jJhY-NBM", // Leo
        991103L to "lhI6o3W4iPE", // GOAT
        991201L to "OInr36v65v8", // Nayagan
        991202L to "6b-3VccHe-Y", // Anbe Sivam
        991203L to "Gc6d_No5_P0", // Jai Bhim
        991001L to "lhI6o3W4iPE", // Thalapathy 69
        991002L to "2Hcln39R7I0", // Vidaamuyarchi
        991003L to "5bSsn6H_8Xk", // Coolie
        991004L to "r9vWfX_7KzU", // Kanguva 2
        991005L to "t2zG260WvV0", // Good Bad Ugly
        991006L to "0h2g-a0C_x4", // Vettaiyan
        991007L to "pTAsf2y8E7I"  // Love Insurance Kompaney
    )

    private val _movieTrailers = MutableStateFlow<Map<Long, String>>(curatedTrailers)
    val movieTrailers: StateFlow<Map<Long, String>> = _movieTrailers.asStateFlow()

    private val _movieCredits = MutableStateFlow<Map<Long, TmdbCreditsResponse>>(emptyMap())
    val movieCredits: StateFlow<Map<Long, TmdbCreditsResponse>> = _movieCredits.asStateFlow()

    private val _creditsLoading = MutableStateFlow<Boolean>(false)
    val creditsLoading: StateFlow<Boolean> = _creditsLoading.asStateFlow()

    private val _personLoading = MutableStateFlow<Boolean>(false)
    val personLoading: StateFlow<Boolean> = _personLoading.asStateFlow()

    private val _selectedPerson = MutableStateFlow<TmdbPersonResponse?>(null)
    val selectedPerson: StateFlow<TmdbPersonResponse?> = _selectedPerson.asStateFlow()

    private val _selectedPersonCredits = MutableStateFlow<TmdbPersonMovieCreditsResponse?>(null)
    val selectedPersonCredits: StateFlow<TmdbPersonMovieCreditsResponse?> = _selectedPersonCredits.asStateFlow()

    // Watchlist
    private val _watchlist = MutableStateFlow<List<TmdbMovie>>(emptyList())
    val watchlist: StateFlow<List<TmdbMovie>> = _watchlist.asStateFlow()

    fun loadWatchlist(ctx: Context) {
        val movies = getCachedMovies(ctx, "watchlist") ?: emptyList()
        _watchlist.value = movies
    }

    fun addToWatchlist(ctx: Context, movie: TmdbMovie) {
        val current = _watchlist.value.toMutableList()
        if (current.none { it.id == movie.id }) {
            current.add(0, movie)
            _watchlist.value = current
            saveCachedJson(ctx, "watchlist", current)
        }
    }

    fun removeFromWatchlist(ctx: Context, movie: TmdbMovie) {
        val current = _watchlist.value.toMutableList()
        current.removeAll { it.id == movie.id }
        _watchlist.value = current
        saveCachedJson(ctx, "watchlist", current)
    }

    fun isInWatchlist(movie: TmdbMovie): Boolean {
        return _watchlist.value.any { it.id == movie.id }
    }

    private val _watchedMovies = MutableStateFlow<Set<Long>>(emptySet())
    val watchedMovies: StateFlow<Set<Long>> = _watchedMovies.asStateFlow()

    private val _watchedListMovies = MutableStateFlow<List<TmdbMovie>>(emptyList())
    val watchedListMovies: StateFlow<List<TmdbMovie>> = _watchedListMovies.asStateFlow()

    fun loadWatchedMovies(ctx: Context) {
        val prefs = ctx.getSharedPreferences("kolly_gaming_secure_prefs", Context.MODE_PRIVATE)
        val stringSet = prefs.getStringSet("watched_movies_ids_v2", emptySet()) ?: emptySet()
        _watchedMovies.value = stringSet.mapNotNull { it.toLongOrNull() }.toSet()
        val movies = getCachedMovies(ctx, "watched_list") ?: emptyList()
        _watchedListMovies.value = movies
    }

    fun toggleWatchedMovie(ctx: Context, movie: TmdbMovie) {
        val currentIds = _watchedMovies.value.toMutableSet()
        val currentMovies = _watchedListMovies.value.toMutableList()
        
        if (currentIds.contains(movie.id)) {
            currentIds.remove(movie.id)
            currentMovies.removeAll { it.id == movie.id }
            Log.d("KollyGameVM", "Removed movie from watched list: ${movie.id}")
        } else {
            currentIds.add(movie.id)
            if (currentMovies.none { it.id == movie.id }) {
                currentMovies.add(0, movie)
            }
            Log.d("KollyGameVM", "Added movie to watched list: ${movie.id}")
        }
        
        _watchedMovies.value = currentIds.toSet()
        _watchedListMovies.value = currentMovies
        
        val prefs = ctx.getSharedPreferences("kolly_gaming_secure_prefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("watched_movies_ids_v2", currentIds.map { it.toString() }.toSet()).apply()
        saveCachedJson(ctx, "watched_list", currentMovies)
    }

    fun isMovieWatched(movieId: Long): Boolean {
        return _watchedMovies.value.contains(movieId)
    }

    // Active Search / Filter State
    private val _searchResultMovies = MutableStateFlow<List<TmdbMovie>>(emptyList())
    val searchResultMovies: StateFlow<List<TmdbMovie>> = _searchResultMovies.asStateFlow()

    private val _isSearching = MutableStateFlow<Boolean>(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    val selectedGenre = MutableStateFlow<String>("All")
    val selectedYear = MutableStateFlow<String>("All")
    val selectedRating = MutableStateFlow<String>("All")
    val selectedLanguage = MutableStateFlow<String>("ta") // Default Tamil
    val searchQuery = MutableStateFlow<String>("")

    val selectedSortOrder = MutableStateFlow<String>("Popularity")
    val selectedArtist = MutableStateFlow<TmdbCastMember?>(null)

    private val _artistSearchResults = MutableStateFlow<List<TmdbCastMember>>(emptyList())
    val artistSearchResults: StateFlow<List<TmdbCastMember>> = _artistSearchResults.asStateFlow()

    private val _isSearchingArtists = MutableStateFlow(false)
    val isSearchingArtists: StateFlow<Boolean> = _isSearchingArtists.asStateFlow()

    private val _isLoadMoreLoading = MutableStateFlow(false)
    val isLoadMoreLoading: StateFlow<Boolean> = _isLoadMoreLoading.asStateFlow()

    var currentFilterPage = 1
    var isFilterPaginationExhausted = false

    // Map genres to TMDB genre IDs
    private val genreMap = mapOf(
        "Action" to 28L,
        "Adventure" to 12L,
        "Animation" to 16L,
        "Comedy" to 35L,
        "Crime" to 80L,
        "Documentary" to 99L,
        "Drama" to 18L,
        "Family" to 10751L,
        "Fantasy" to 14L,
        "History" to 36L,
        "Horror" to 27L,
        "Music" to 10402L,
        "Mystery" to 9648L,
        "Romance" to 10749L,
        "Sci-Fi" to 878L,
        "Thriller" to 53L,
        "War" to 10752L
    )

    fun searchAndFilterMovies(ctx: Context) {
        viewModelScope.launch {
            val query = searchQuery.value.trim()
            val genreName = selectedGenre.value
            val year = selectedYear.value
            val rating = selectedRating.value
            val lang = selectedLanguage.value
            val artist = selectedArtist.value

            // If query is empty and all filters are defaults, reset active search
            if (query.isEmpty() && genreName == "All" && year == "All" && rating == "All" && lang == "ta" && artist == null && selectedSortOrder.value == "Popularity") {
                _isSearching.value = false
                _searchResultMovies.value = emptyList()
                return@launch
            }

            _isSearching.value = true
            currentFilterPage = 1
            isFilterPaginationExhausted = false
            
            try {
                val apiKey = getTmdbKey(ctx)
                if (apiKey.isBlank() || apiKey == "PLACEHOLDER_TMDB_KEY") {
                    // Falls back locally on curated lists
                    val allLocal = getCuratedTrendingMovies() + getCuratedTopRatedMovies() + getCuratedUpcomingMovies()
                    val filtered = allLocal.distinctBy { it.id }.filter { movie ->
                        val matchesQuery = query.isEmpty() || movie.title.contains(query, ignoreCase = true)
                        val matchesYear = year == "All" || movie.releaseDate?.startsWith(year) == true
                        val matchesRating = rating == "All" || (movie.voteAverage ?: 0.0) >= (rating.replace("+", "").toDoubleOrNull() ?: 0.0)
                        val matchesGenre = genreMap[genreName] == null || movie.genreIds?.contains(genreMap[genreName]!!) == true
                        matchesQuery && matchesYear && matchesRating && matchesGenre
                    }
                    _searchResultMovies.value = sortMoviesList(filtered, selectedSortOrder.value)
                    return@launch
                }

                // If query is not empty, use /search/movie first, then filter locally
                if (query.isNotEmpty()) {
                    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                    val searchUrl = "https://api.themoviedb.org/3/search/movie?api_key=$apiKey&query=$encodedQuery&with_original_language=$lang&page=1"
                    val results = withContext(Dispatchers.IO) { fetchMoviesFromApi(searchUrl) }
                    
                    val genreId = genreMap[genreName]
                    val filtered = results.filter { movie ->
                        val matchesYear = year == "All" || movie.releaseDate?.startsWith(year) == true
                        val matchesRating = rating == "All" || (movie.voteAverage ?: 0.0) >= (rating.replace("+", "").toDoubleOrNull() ?: 0.0)
                        val matchesGenre = genreId == null || movie.genreIds?.contains(genreId) == true
                        val matchesArtist = artist == null || checkMovieHasArtistLocal(ctx, movie.id, artist.id)
                        matchesYear && matchesRating && matchesGenre && matchesArtist
                    }
                    _searchResultMovies.value = sortMoviesList(filtered, selectedSortOrder.value)
                } else {
                    // Blank query with selected filters: use advanced /discover/movie API directly!
                    val genreId = genreMap[genreName]
                    val ratingThreshold = rating.replace("+", "").toDoubleOrNull() ?: 0.0
                    val sortParam = when (selectedSortOrder.value) {
                        "Rating" -> "vote_average.desc"
                        "Release Date" -> "primary_release_date.desc"
                        "Title A-Z" -> "title.asc"
                        else -> "popularity.desc"
                    }
                    
                    var discoverUrl = "https://api.themoviedb.org/3/discover/movie?api_key=$apiKey&with_original_language=$lang&sort_by=$sortParam&page=1"
                    if (genreId != null) {
                        discoverUrl += "&with_genres=$genreId"
                    }
                    if (year != "All") {
                        discoverUrl += "&primary_release_year=$year"
                    }
                    if (ratingThreshold > 0.0) {
                        discoverUrl += "&vote_average.gte=$ratingThreshold"
                    }
                    if (artist != null) {
                        discoverUrl += "&with_people=${artist.id}"
                    }

                    val results = withContext(Dispatchers.IO) { fetchMoviesFromApi(discoverUrl) }
                    _searchResultMovies.value = results
                }
            } catch (e: Exception) {
                Log.e("KollyGameVM", "Error searching and filtering movies", e)
                _searchResultMovies.value = emptyList()
            }
        }
    }

    private suspend fun checkMovieHasArtistLocal(ctx: Context, movieId: Long, artistId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = getTmdbKey(ctx)
                val url = "https://api.themoviedb.org/3/movie/$movieId/credits?api_key=$apiKey"
                val json = URL(url).openStream().bufferedReader().readText()
                val response = TmdbCreditsResponse.fromJson(JSONObject(json))
                response.cast.any { it.id == artistId } || response.crew.any { it.id == artistId }
            } catch (e: Exception) {
                false
            }
        }
    }

    fun searchArtists(ctx: Context, query: String) {
        viewModelScope.launch {
            val q = query.trim()
            if (q.isEmpty()) {
                _artistSearchResults.value = emptyList()
                return@launch
            }
            _isSearchingArtists.value = true
            try {
                val apiKey = getTmdbKey(ctx)
                val encodedQuery = java.net.URLEncoder.encode(q, "UTF-8")
                val url = "https://api.themoviedb.org/3/search/person?api_key=$apiKey&query=$encodedQuery&page=1"
                val json = withContext(Dispatchers.IO) { URL(url).openStream().bufferedReader().readText() }
                val root = JSONObject(json)
                val results = root.optJSONArray("results")
                val list = mutableListOf<TmdbCastMember>()
                if (results != null) {
                    for (i in 0 until results.length()) {
                        list.add(TmdbCastMember.fromJson(results.getJSONObject(i)))
                    }
                }
                _artistSearchResults.value = list
            } catch (e: Exception) {
                Log.e("KollyGameVM", "Error searching artists", e)
                _artistSearchResults.value = emptyList()
            } finally {
                _isSearchingArtists.value = false
            }
        }
    }

    fun loadNextPage(ctx: Context) {
        if (isFilterPaginationExhausted || _isLoadMoreLoading.value) return
        
        viewModelScope.launch {
            val query = searchQuery.value.trim()
            val genreName = selectedGenre.value
            val year = selectedYear.value
            val rating = selectedRating.value
            val lang = selectedLanguage.value
            val artist = selectedArtist.value
            
            val apiKey = getTmdbKey(ctx)
            if (apiKey.isBlank() || apiKey == "PLACEHOLDER_TMDB_KEY") {
                isFilterPaginationExhausted = true
                return@launch
            }

            _isLoadMoreLoading.value = true
            try {
                currentFilterPage += 1
                
                if (query.isNotEmpty()) {
                    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                    val searchUrl = "https://api.themoviedb.org/3/search/movie?api_key=$apiKey&query=$encodedQuery&with_original_language=$lang&page=$currentFilterPage"
                    val results = withContext(Dispatchers.IO) { fetchMoviesFromApi(searchUrl) }
                    
                    if (results.isEmpty()) {
                        isFilterPaginationExhausted = true
                    } else {
                        val genreId = genreMap[genreName]
                        val filtered = results.filter { movie ->
                            val matchesYear = year == "All" || movie.releaseDate?.startsWith(year) == true
                            val matchesRating = rating == "All" || (movie.voteAverage ?: 0.0) >= (rating.replace("+", "").toDoubleOrNull() ?: 0.0)
                            val matchesGenre = genreId == null || movie.genreIds?.contains(genreId) == true
                            val matchesArtist = artist == null || checkMovieHasArtistLocal(ctx, movie.id, artist.id)
                            matchesYear && matchesRating && matchesGenre && matchesArtist
                        }
                        
                        val newResults = _searchResultMovies.value + filtered
                        _searchResultMovies.value = sortMoviesList(newResults, selectedSortOrder.value)
                    }
                } else {
                    val genreId = genreMap[genreName]
                    val ratingThreshold = rating.replace("+", "").toDoubleOrNull() ?: 0.0
                    val sortParam = when (selectedSortOrder.value) {
                        "Rating" -> "vote_average.desc"
                        "Release Date" -> "primary_release_date.desc"
                        "Title A-Z" -> "title.asc"
                        else -> "popularity.desc"
                    }
                    
                    var discoverUrl = "https://api.themoviedb.org/3/discover/movie?api_key=$apiKey&with_original_language=$lang&sort_by=$sortParam&page=$currentFilterPage"
                    if (genreId != null) {
                        discoverUrl += "&with_genres=$genreId"
                    }
                    if (year != "All") {
                        discoverUrl += "&primary_release_year=$year"
                    }
                    if (ratingThreshold > 0.0) {
                        discoverUrl += "&vote_average.gte=$ratingThreshold"
                    }
                    if (artist != null) {
                        discoverUrl += "&with_people=${artist.id}"
                    }
                    
                    val results = withContext(Dispatchers.IO) { fetchMoviesFromApi(discoverUrl) }
                    if (results.isEmpty()) {
                        isFilterPaginationExhausted = true
                    } else {
                        _searchResultMovies.value = _searchResultMovies.value + results
                    }
                }
            } catch (e: Exception) {
                Log.e("KollyGameVM", "Error loading next page", e)
            } finally {
                _isLoadMoreLoading.value = false
            }
        }
    }

    private fun sortMoviesList(movies: List<TmdbMovie>, sortOrder: String): List<TmdbMovie> {
        return when (sortOrder) {
            "Rating" -> movies.sortedByDescending { it.voteAverage ?: 0.0 }
            "Release Date" -> movies.sortedByDescending { it.releaseDate ?: "" }
            "Title A-Z" -> movies.sortedBy { it.title.lowercase(Locale.getDefault()) }
            else -> movies.sortedByDescending { it.id } // Default fallback
        }
    }

    private fun getTmdbKey(ctx: Context): String {
        val prefs = ctx.getSharedPreferences("kolly_gaming_secure_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("tmdb_api_key_secure", "") ?: ""
        return saved.ifBlank { "6a466e5332dd8e436b7925a5c9f02ad2" }
    }

    fun fetchKollywoodMovies(ctx: Context) {
        loadWatchlist(ctx)
        loadWatchedMovies(ctx)
        viewModelScope.launch {
            val currentState = _kollywoodState.value
            val hasData = currentState is KollywoodUiState.Success || 
                          (currentState is KollywoodUiState.Error && currentState.fallbackTrending != null)
            
            if (!hasData) {
                _kollywoodState.value = KollywoodUiState.Loading
            } else {
                _isRefreshing.value = true
            }

            try {
                val apiKey = getTmdbKey(ctx)
                if (apiKey.isBlank() || apiKey == "PLACEHOLDER_TMDB_KEY") {
                    val trending = getCuratedTrendingMovies()
                    val topRated = getCuratedTopRatedMovies()
                    val upcoming = getCuratedUpcomingMovies()
                    _kollywoodState.value = KollywoodUiState.Success(
                        trending = trending,
                        topRated = topRated,
                        upcoming = upcoming,
                        isDemoMode = true
                    )
                } else {
                    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    
                    val results = coroutineScope {
                        val trendingDeferred = async(Dispatchers.IO) {
                            fetchMoviesFromApi("https://api.themoviedb.org/3/discover/movie?api_key=$apiKey&with_original_language=ta&region=IN&sort_by=popularity.desc&page=1")
                        }
                        val topRatedDeferred = async(Dispatchers.IO) {
                            fetchMoviesFromApi("https://api.themoviedb.org/3/discover/movie?api_key=$apiKey&with_original_language=ta&region=IN&sort_by=vote_average.desc&vote_count.gte=8&page=1")
                        }
                        val upcomingDeferred = async(Dispatchers.IO) {
                            fetchMoviesFromApi("https://api.themoviedb.org/3/discover/movie?api_key=$apiKey&with_original_language=ta&region=IN&sort_by=primary_release_date.asc&primary_release_date.gte=$todayDate&page=1")
                        }

                        listOf(
                            trendingDeferred.await().ifEmpty { getCuratedTrendingMovies() },
                            topRatedDeferred.await().ifEmpty { getCuratedTopRatedMovies() },
                            upcomingDeferred.await().ifEmpty { getCuratedUpcomingMovies() }
                        )
                    }

                    val trending = results[0]
                    val topRated = results[1]
                    val upcoming = results[2]

                    saveCachedJson(ctx, "trending", trending)
                    saveCachedJson(ctx, "top_rated", topRated)
                    saveCachedJson(ctx, "upcoming", upcoming)

                    _kollywoodState.value = KollywoodUiState.Success(
                        trending = trending,
                        topRated = topRated,
                        upcoming = upcoming,
                        isDemoMode = false
                    )
                }
            } catch (e: Exception) {
                Log.e("KollyGameVM", "TMDB API request failed, loading local secure cache fallback", e)
                val cacheTrending = getCachedMovies(ctx, "trending") ?: getCuratedTrendingMovies()
                val cacheTopRated = getCachedMovies(ctx, "top_rated") ?: getCuratedTopRatedMovies()
                val cacheUpcoming = getCachedMovies(ctx, "upcoming") ?: getCuratedUpcomingMovies()
                
                _kollywoodState.value = KollywoodUiState.Error(
                    message = e.localizedMessage ?: "Failed to connect to TMDB database.",
                    fallbackTrending = cacheTrending,
                    fallbackTopRated = cacheTopRated,
                    fallbackUpcoming = cacheUpcoming
                )
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun searchYouTubeVideoId(query: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val url = "https://www.youtube.com/results?search_query=$encodedQuery"
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                val html = connection.inputStream.bufferedReader().readText()
                
                // Try videoId pattern first (most reliable in ytInitialData)
                val videoIdRegex = "\"videoId\":\"([a-zA-Z0-9_-]{11})\"".toRegex()
                val match1 = videoIdRegex.find(html)
                val id1 = match1?.groups?.get(1)?.value
                if (!id1.isNullOrEmpty()) {
                    Log.d("KollyGameVM", "Found YouTube videoId via regex: $id1")
                    return@withContext id1
                }
                
                // Try watch?v= pattern as fallback
                val watchRegex = "/watch\\?v=([a-zA-Z0-9_-]{11})".toRegex()
                val match2 = watchRegex.find(html)
                val id2 = match2?.groups?.get(1)?.value
                if (!id2.isNullOrEmpty()) {
                    Log.d("KollyGameVM", "Found YouTube videoId via watch?v= regex: $id2")
                    return@withContext id2
                }
                
                Log.w("KollyGameVM", "No video ID matched in YouTube search HTML.")
                null
            } catch (e: Exception) {
                Log.e("KollyGameVM", "Failed to search YouTube video ID", e)
                null
            }
        }
    }

    fun fetchMovieTrailer(ctx: Context, movieId: Long, movieTitle: String) {
        if (_movieTrailers.value.containsKey(movieId)) {
            val current = _movieTrailers.value[movieId]
            if (current != null && !current.startsWith("search:") && current.isNotEmpty()) {
                return
            }
        }

        viewModelScope.launch {
            try {
                if (curatedTrailers.containsKey(movieId)) {
                    val key = curatedTrailers[movieId]!!
                    _movieTrailers.value = _movieTrailers.value + (movieId to key)
                    return@launch
                }

                val apiKey = getTmdbKey(ctx)
                val url = "https://api.themoviedb.org/3/movie/$movieId/videos?api_key=$apiKey"
                val json = withContext(Dispatchers.IO) { URL(url).openStream().bufferedReader().readText() }
                val root = JSONObject(json)
                val results = root.optJSONArray("results")
                var trailerKey: String? = null
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val obj = results.getJSONObject(i)
                        val site = obj.optString("site")
                        val type = obj.optString("type")
                        if (site.equals("YouTube", ignoreCase = true) && (type.equals("Trailer", ignoreCase = true) || type.equals("Teaser", ignoreCase = true))) {
                            trailerKey = obj.optString("key")
                            break
                        }
                    }
                }

                if (trailerKey != null && trailerKey.isNotEmpty()) {
                    _movieTrailers.value = _movieTrailers.value + (movieId to trailerKey)
                } else {
                    val foundId = searchYouTubeVideoId("$movieTitle official trailer")
                    _movieTrailers.value = _movieTrailers.value + (movieId to (foundId ?: ""))
                }
            } catch (e: Exception) {
                Log.e("KollyGameVM", "Failed to fetch trailer for $movieTitle", e)
                val foundId = searchYouTubeVideoId("$movieTitle official trailer")
                _movieTrailers.value = _movieTrailers.value + (movieId to (foundId ?: ""))
            }
        }
    }

    fun fetchMovieCredits(ctx: Context, movieId: Long) {
        if (_movieCredits.value.containsKey(movieId)) return

        viewModelScope.launch {
            _creditsLoading.value = true
            try {
                val apiKey = getTmdbKey(ctx)
                val url = "https://api.themoviedb.org/3/movie/$movieId/credits?api_key=$apiKey"
                val json = withContext(Dispatchers.IO) { URL(url).openStream().bufferedReader().readText() }
                val response = TmdbCreditsResponse.fromJson(JSONObject(json))
                _movieCredits.value = _movieCredits.value + (movieId to response)
            } catch (e: Exception) {
                Log.e("KollyGameVM", "Failed to load credits for $movieId", e)
            } finally {
                _creditsLoading.value = false
            }
        }
    }

    fun fetchPersonDetails(ctx: Context, personId: Long) {
        viewModelScope.launch {
            _personLoading.value = true
            try {
                val apiKey = getTmdbKey(ctx)
                
                val detailsDeferred = async(Dispatchers.IO) {
                    val url = "https://api.themoviedb.org/3/person/$personId?api_key=$apiKey"
                    URL(url).openStream().bufferedReader().readText()
                }

                val creditsDeferred = async(Dispatchers.IO) {
                    val url = "https://api.themoviedb.org/3/person/$personId/movie_credits?api_key=$apiKey"
                    URL(url).openStream().bufferedReader().readText()
                }

                val detailsJson = detailsDeferred.await()
                val creditsJson = creditsDeferred.await()

                _selectedPerson.value = TmdbPersonResponse.fromJson(JSONObject(detailsJson))
                _selectedPersonCredits.value = TmdbPersonMovieCreditsResponse.fromJson(JSONObject(creditsJson))
            } catch (e: Exception) {
                Log.e("KollyGameVM", "Failed to load person details for $personId", e)
            } finally {
                _personLoading.value = false
            }
        }
    }

    fun clearSelectedPerson() {
        _selectedPerson.value = null
        _selectedPersonCredits.value = null
    }

    private fun fetchMoviesFromApi(urlString: String): List<TmdbMovie> {
        return try {
            val json = URL(urlString).openStream().bufferedReader().readText()
            val root = JSONObject(json)
            val results = root.optJSONArray("results")
            val list = mutableListOf<TmdbMovie>()
            if (results != null) {
                for (i in 0 until results.length()) {
                    list.add(TmdbMovie.fromJson(results.getJSONObject(i)))
                }
            }
            list
        } catch (e: Exception) {
            Log.e("KollyGameVM", "API fetch error: $urlString", e)
            emptyList()
        }
    }

    private fun saveCachedJson(ctx: Context, key: String, movies: List<TmdbMovie>) {
        try {
            val prefs = ctx.getSharedPreferences("kolly_gaming_secure_prefs", Context.MODE_PRIVATE)
            val jsonArr = org.json.JSONArray()
            movies.forEach { movie ->
                val obj = JSONObject().apply {
                    put("id", movie.id)
                    put("title", movie.title)
                    put("original_title", movie.originalTitle)
                    put("overview", movie.overview)
                    put("poster_path", movie.posterPath)
                    put("backdrop_path", movie.backdropPath)
                    put("release_date", movie.releaseDate)
                    put("vote_average", movie.voteAverage)
                    put("genre_ids", org.json.JSONArray().apply {
                        movie.genreIds?.forEach { put(it) }
                    })
                }
                jsonArr.put(obj)
            }
            prefs.edit().putString("cache_movies_$key", jsonArr.toString()).apply()
        } catch (e: Exception) {
            Log.e("KollyGameVM", "Failed to cache movies", e)
        }
    }

    private fun getCachedMovies(ctx: Context, key: String): List<TmdbMovie>? {
        return try {
            val prefs = ctx.getSharedPreferences("kolly_gaming_secure_prefs", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("cache_movies_$key", null) ?: return null
            val jsonArr = org.json.JSONArray(jsonStr)
            val list = mutableListOf<TmdbMovie>()
            for (i in 0 until jsonArr.length()) {
                list.add(TmdbMovie.fromJson(jsonArr.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            Log.e("KollyGameVM", "Failed to read cached movies", e)
            null
        }
    }

    private fun getCuratedTrendingMovies(): List<TmdbMovie> {
        return listOf(
            TmdbMovie(
                id = 991101,
                title = "Amaran",
                originalTitle = "அமரன்",
                overview = "The inspiring true story of Major Mukund Varadarajan, capturing his deep valor and relentless defense missions under extreme risk.",
                posterPath = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&auto=format&fit=crop&q=80",
                backdropPath = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1200&auto=format&fit=crop&q=80",
                releaseDate = "2024-10-31",
                voteAverage = 8.8
            ),
            TmdbMovie(
                id = 991102,
                title = "Leo",
                originalTitle = "லியோ",
                overview = "A mild-mannered cafe owner becomes the target of a drug cartel, who suspect him of being a former associate with a dark past.",
                posterPath = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&auto=format&fit=crop&q=80",
                backdropPath = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=1200&auto=format&fit=crop&q=80",
                releaseDate = "2023-10-19",
                voteAverage = 8.2
            ),
            TmdbMovie(
                id = 991103,
                title = "The Greatest Of All Time",
                originalTitle = "கோட்",
                overview = "A top-tier field agent and hostage negotiator faces a ghost from his past that threatens the entire security ecosystem of the nation.",
                posterPath = null,
                backdropPath = null,
                releaseDate = "2024-09-05",
                voteAverage = 7.5
            )
        )
    }

    private fun getCuratedTopRatedMovies(): List<TmdbMovie> {
        return listOf(
            TmdbMovie(
                id = 991201,
                title = "Nayagan",
                originalTitle = "நாயகн",
                overview = "A small boy witnesses his father's murder, flees to Bombay, and rises to become a powerful, beloved underworld don protecting the needy.",
                posterPath = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=500&auto=format&fit=crop&q=80",
                backdropPath = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&auto=format&fit=crop&q=80",
                releaseDate = "1987-10-21",
                voteAverage = 9.5
            ),
            TmdbMovie(
                id = 991202,
                title = "Anbe Sivam",
                originalTitle = "அன்பே சிவம்",
                overview = "Two men with contrasting personalities embark on an unexpected journey from Bhubaneswar to Chennai, finding love, humanity, and faith.",
                posterPath = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500&auto=format&fit=crop&q=80",
                backdropPath = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200&auto=format&fit=crop&q=80",
                releaseDate = "2003-01-15",
                voteAverage = 9.1
            ),
            TmdbMovie(
                id = 991203,
                title = "Jai Bhim",
                originalTitle = "ஜெய் பீம்",
                overview = "A courageous lawyer fights relentlessly for justice when an innocent tribal man is falsely accused and disappears from police custody.",
                posterPath = null,
                backdropPath = null,
                releaseDate = "2021-11-02",
                voteAverage = 9.4
            )
        )
    }

    private fun getCuratedUpcomingMovies(): List<TmdbMovie> {
        return listOf(
            TmdbMovie(
                id = 991001,
                title = "Thalapathy 69",
                originalTitle = "தளபதி 69",
                overview = "The monumental final cinematic outing of actor Vijay directed by H. Vinoth, capturing a highly dramatic narrative expected to set box office records.",
                posterPath = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&auto=format&fit=crop&q=80",
                backdropPath = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=1200&auto=format&fit=crop&q=80",
                releaseDate = "2025-10-16",
                voteAverage = 9.8
            ),
            TmdbMovie(
                id = 991002,
                title = "Vidaamuyarchi",
                originalTitle = "விடாமுயற்சி",
                overview = "An action-thriller directed by Magizh Thirumeni featuring Ajith Kumar. The narrative scales heavy personal risk, mystery, and massive action sequences.",
                posterPath = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&auto=format&fit=crop&q=80",
                backdropPath = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1200&auto=format&fit=crop&q=80",
                releaseDate = "2025-06-20",
                voteAverage = 8.9
            ),
            TmdbMovie(
                id = 991003,
                title = "Coolie",
                originalTitle = "கூலி",
                overview = "Superstar Rajinikanth collaborates with Lokesh Kanagaraj for a vintage action gold smuggling narrative set across industrial shipyards and dense city channels.",
                posterPath = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=500&auto=format&fit=crop&q=80",
                backdropPath = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&auto=format&fit=crop&q=80",
                releaseDate = "2025-09-12",
                voteAverage = 9.3
            ),
            TmdbMovie(
                id = 991004,
                title = "Kanguva: Part II",
                originalTitle = "கங்குவா 2",
                overview = "The epic conclusion of Kanguva, expanding Suriya's historical struggle through centuries to resolve an ancient obligation with a modern resolution.",
                posterPath = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500&auto=format&fit=crop&q=80",
                backdropPath = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200&auto=format&fit=crop&q=80",
                releaseDate = "2026-04-18",
                voteAverage = 7.7
            ),
            TmdbMovie(
                id = 991005,
                title = "Good Bad Ugly",
                originalTitle = "குட் பேட் அக்லி",
                overview = "Adhik Ravichandran directs Ajith Kumar in a dynamic neo-noir entertainer with eccentric characters, heavy music scores, and a stylized crime underground.",
                posterPath = "https://images.unsplash.com/photo-1533928298208-27ff66555d8d?w=500&auto=format&fit=crop&q=80",
                backdropPath = "https://images.unsplash.com/photo-1533928298208-27ff66555d8d?w=1200&auto=format&fit=crop&q=80",
                releaseDate = "2025-05-29",
                voteAverage = 8.5
            ),
            TmdbMovie(
                id = 991006,
                title = "Vettaiyan: The Precursor",
                originalTitle = "வேட்டையன்",
                overview = "An investigative thriller exploring the history of human encounter specialization and structural reform in the judicial departments.",
                posterPath = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=500&auto=format&fit=crop&q=80",
                backdropPath = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=1200&auto=format&fit=crop&q=80",
                releaseDate = "2025-02-14",
                voteAverage = 8.2
            ),
            TmdbMovie(
                id = 991007,
                title = "Love Insurance Kompaney",
                originalTitle = "லவ் இன்சூரன்ஸ் கம்பெனி",
                overview = "Vignesh Shivan directs a quirky futuristic comedy centered around a company that sells insurance packages guaranteeing relationship success and emotional protection.",
                posterPath = "https://images.unsplash.com/photo-1512428559087-560fa5ceab42?w=500&auto=format&fit=crop&q=80",
                backdropPath = "https://images.unsplash.com/photo-1512428559087-560fa5ceab42?w=1200&auto=format&fit=crop&q=80",
                releaseDate = "2025-07-11",
                voteAverage = 8.0
            )
        )
    }
}
