package com.lagradost.cloudstream3.ui.kollygame

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.MainActivity
import java.util.Calendar
import java.util.Locale

class KollyGameFragment : Fragment() {

    private val viewModel: KollyGameViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                KollywoodScreen(
                    viewModel = viewModel,
                    onPlayMovieClick = { movie ->
                        val year = if (!movie.releaseDate.isNullOrBlank() && movie.releaseDate.length >= 4) {
                            " " + movie.releaseDate.substring(0, 4)
                        } else ""
                        MainActivity.nextSearchQuery = "${movie.title}$year"
                        (activity as? MainActivity)?.navController?.navigate(com.lagradost.cloudstream3.R.id.navigation_search)
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.fetchKollywoodMovies(requireContext())
    }
}

@Composable
fun SearchableDropdown( 
    title: String,
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    Box(modifier = Modifier.padding(vertical = 4.dp)) {
        TextButton(
            onClick = { expanded = true }, 
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
        ) {
            Text(text = "$title: $selectedItem", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        if (expanded) {
            Dialog(onDismissRequest = { expanded = false }) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E1E2C),
                    contentColor = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFFFD700))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            placeholder = { Text("Search...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val filteredItems = items.filter { it.contains(searchText, ignoreCase = true) }
                        LazyColumn {
                            items(filteredItems) { item ->
                                Text(
                                    text = item,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            onItemSelected(item)
                                            expanded = false
                                        }
                                        .padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KollywoodScreen(
    viewModel: KollyGameViewModel,
    onPlayMovieClick: (TmdbMovie) -> Unit
) {
    val uiState by viewModel.kollywoodState.collectAsState()
    var selectedMovie by remember { mutableStateOf<TmdbMovie?>(null) }
    var showWatchlistOverlay by remember { mutableStateOf(false) }
    var showWatchedOverlay by remember { mutableStateOf(false) }
    val watchlistMovies by viewModel.watchlist.collectAsState()
    val watchedMovies by viewModel.watchedMovies.collectAsState()
    val watchedListMovies by viewModel.watchedListMovies.collectAsState()
    val context = LocalContext.current

    val searchQuery by viewModel.searchQuery.collectAsState()
    val actorQuery by viewModel.actorQuery.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedRating by viewModel.selectedRating.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

    val isSearching by viewModel.isSearching.collectAsState()
    val searchResultMovies by viewModel.searchResultMovies.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()

    val langMap = remember { mapOf("Tamil" to "ta", "Telugu" to "te", "Malayalam" to "ml", "Hindi" to "hi", "English" to "en") }
    val reverseLangMap = remember { mapOf("ta" to "Tamil", "te" to "Telugu", "ml" to "Malayalam", "hi" to "Hindi", "en" to "English") }
    val years = remember { (Calendar.getInstance().get(Calendar.YEAR) downTo 1950).map { it.toString() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F13))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
             Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A2A)).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                 Text("KollyCloud", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD700))
                 Row {
                    IconButton(onClick = { showWatchedOverlay = true }) { EyeIcon(isWatched = true) }
                    IconButton(onClick = { showWatchlistOverlay = true }) { Icon(Icons.Default.Favorite, contentDescription = "Watchlist", tint = Color(0xFFFF6B6B)) }
                    IconButton(onClick = { viewModel.fetchKollywoodMovies(context) }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White) }
                 }
            }
            
            // Search & Filters
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it; viewModel.searchAndFilterMovies(context) },
                    placeholder = { Text("Search Movies...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFFFD700)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                 OutlinedTextField(
                    value = actorQuery,
                    onValueChange = { viewModel.actorQuery.value = it; viewModel.searchAndFilterMovies(context) },
                    placeholder = { Text("Search by Actor...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFFFD700)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        SearchableDropdown("Genre", listOf("All", "Action", "Adventure", "Animation", "Comedy", "Crime", "Drama", "Family", "Fantasy", "History", "Horror", "Music", "Mystery", "Romance", "Sci-Fi", "Thriller"), selectedGenre) {
                            viewModel.selectedGenre.value = it
                            viewModel.searchAndFilterMovies(context)
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SearchableDropdown("Year", listOf("All") + years, selectedYear) {
                            viewModel.selectedYear.value = it
                            viewModel.searchAndFilterMovies(context)
                        }
                    }
                }
                 Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                         SearchableDropdown("Rating", listOf("All", "9.0+", "8.0+", "7.0+", "6.0+", "5.0+"), selectedRating) {
                            viewModel.selectedRating.value = it
                            viewModel.searchAndFilterMovies(context)
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SearchableDropdown("Language", langMap.keys.toList(), reverseLangMap[selectedLanguage] ?: "Tamil") { 
                            viewModel.selectedLanguage.value = langMap[it] ?: "ta"
                            viewModel.searchAndFilterMovies(context)
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))

            // Content
            if (isSearching) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(searchResultMovies) { movie ->
                        MovieRowCard(
                            movie = movie, 
                            onClick = { selectedMovie = movie }, 
                            isWatchlisted = viewModel.isInWatchlist(movie),
                            isWatched = watchedMovies.contains(movie.id),
                            onWatchlistToggle = { 
                                if(viewModel.isInWatchlist(movie)) viewModel.removeFromWatchlist(context, movie) 
                                else viewModel.addToWatchlist(context, movie) 
                            }
                        )
                    }
                    if (isLoadingMore) {
                        item { CircularProgressIndicator(color = Color(0xFFFFD700)) }
                        item { CircularProgressIndicator(color = Color(0xFFFFD700)) }
                    }
                     if (!isLoadingMore && searchResultMovies.isNotEmpty()) {
                        item {LaunchedEffect(true) { viewModel.loadMoreMovies(context) } }
                    }
                }
            } else {
                 when (val state = uiState) {
                    is KollywoodUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFFD700)) }
                    is KollywoodUiState.Success -> KollywoodCatalog(viewModel, state.trending, state.topRated, state.upcoming, state.isDemoMode, null, { selectedMovie = it }, { viewModel.fetchKollywoodMovies(context) })
                    is KollywoodUiState.Error -> {
                        if (state.fallbackTrending != null) {
                            KollywoodCatalog(viewModel, state.fallbackTrending, state.fallbackTopRated ?: emptyList(), state.fallbackUpcoming ?: emptyList(), true, state.message, { selectedMovie = it }, { viewModel.fetchKollywoodMovies(context) })
                        } else {
                            ErrorStateScreen(state.message) { viewModel.fetchKollywoodMovies(context) }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(showWatchlistOverlay, enter = fadeIn(), exit = fadeOut()) {
            ItemOverlay(title = "My Watchlist", movies = watchlistMovies, onDismiss = { showWatchlistOverlay = false }, onMovieClick = { selectedMovie = it }, viewModel = viewModel)
        }
        
        AnimatedVisibility(showWatchedOverlay, enter = fadeIn(), exit = fadeOut()) {
            ItemOverlay(title = "Watched Movies", movies = watchedListMovies, onDismiss = { showWatchedOverlay = false }, onMovieClick = { selectedMovie = it }, viewModel = viewModel)
        }

        AnimatedVisibility(selectedMovie != null, enter = fadeIn(), exit = fadeOut()) {
            movieToShow?.let { 
                 MovieDetailContent(it, viewModel, { selectedMovie = null }, { selectedMovie = it }, onPlayMovieClick)
            }
        }
    }
}

@Composable
fun ItemOverlay(title: String, movies: List<TmdbMovie>, onDismiss: () -> Unit, onMovieClick: (TmdbMovie) -> Unit, viewModel: KollyGameViewModel) {
    val context = LocalContext.current
    Surface(color = Color(0xEE0F0F13)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
             Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, null, tint = Color(0xFFFFD700)) }
                Spacer(Modifier.width(8.dp))
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            if (movies.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("This list is empty", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(movies) { movie ->
                        MovieRowCard(movie, { onMovieClick(movie) }, isWatchlisted = viewModel.isInWatchlist(movie), isWatched = viewModel.isMovieWatched(movie.id), onWatchlistToggle = {
                            if (viewModel.isInWatchlist(movie)) viewModel.removeFromWatchlist(context, movie)
                            else viewModel.addToWatchlist(context, movie)
                        })
                    }
                }
            }
        }
    }
}


@Composable
fun KollywoodCatalog(
    viewModel: KollyGameViewModel,
    trending: List<TmdbMovie>,
    topRated: List<TmdbMovie>,
    upcoming: List<TmdbMovie>,
    isDemoMode: Boolean,
    errorMessage: String?,
    onMovieClick: (TmdbMovie) -> Unit,
    onRetryClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val watchlistMovies by viewModel.watchlist.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        if (isDemoMode || errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161622)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (errorMessage != null) Color(0xFFFF6B6B).copy(alpha = 0.4f) else Color(0xFFFFD700).copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (errorMessage != null) Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = "Status",
                        tint = if (errorMessage != null) Color(0xFFFF6B6B) else Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (errorMessage != null) "Secure Offline Cache Active" else "Premium TMDB Database Sync",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (errorMessage != null) "Couldn\'t reach live servers. Displaying cached curated Tamil movies." else "Connected directly to TMDb database. Loading live feeds.",
                            fontSize = 11.sp,
                            color = Color(0xFFC5C5D2)
                        )
                    }
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onRetryClick) {
                            Text("Retry", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (watchlistMovies.isNotEmpty()) {
            MovieRowSection(title = "My Watchlist", movies = watchlistMovies, viewModel = viewModel, onMovieClick = onMovieClick)
            Spacer(modifier = Modifier.height(16.dp))
        }

        MovieRowSection(title = "Trending Tamil Movies", movies = trending, viewModel = viewModel, onMovieClick = onMovieClick)
        Spacer(modifier = Modifier.height(16.dp))
        MovieRowSection(title = "Top Rated Kollywood", movies = topRated, viewModel = viewModel, onMovieClick = onMovieClick)
        Spacer(modifier = Modifier.height(16.dp))
        MovieRowSection(title = "Upcoming Tamil Releases", movies = upcoming, viewModel = viewModel, onMovieClick = onMovieClick)

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun MovieRowSection(
    title: String,
    movies: List<TmdbMovie>,
    viewModel: KollyGameViewModel,
    onMovieClick: (TmdbMovie) -> Unit
) {
    val context = LocalContext.current
    val watchedMovies by viewModel.watchedMovies.collectAsState()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFFFFD700),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (movies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No movies loaded", color = Color.Gray)
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(movies) { movie ->
                    MovieRowCard(
                        movie = movie,
                        onClick = { onMovieClick(movie) },
                        isWatchlisted = viewModel.isInWatchlist(movie),
                        isWatched = watchedMovies.contains(movie.id),
                        onWatchlistToggle = {
                            if (viewModel.isInWatchlist(movie)) {
                                viewModel.removeFromWatchlist(context, movie)
                            } else {
                                viewModel.addToWatchlist(context, movie)
                            }
                        },
                        onWatchedToggle = { viewModel.toggleWatchedMovie(context, movie) }
                    )
                }
            }
        }
    }
}

@Composable
fun MovieRowCard(
    movie: TmdbMovie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isWatchlisted: Boolean,
    isWatched: Boolean,
    onWatchlistToggle: (() -> Unit)?
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .alpha(if (isWatched) 0.6f else 1.0f),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161622))
    ) {
        Box(modifier = Modifier.height(200.dp).fillMaxWidth()) {
            AsyncImage(
                model = movie.fullPosterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))

            if (onWatchlistToggle != null) {
                IconButton(onClick = onWatchlistToggle, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                    Icon(
                        if (isWatchlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Watchlist",
                        tint = if (isWatchlisted) Color(0xFFFF6B6B) else Color.White
                    )
                }
            }

             movie.voteAverage?.let { score ->
                if (score > 0) {
                     Row(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(String.format(Locale.US, "%.1f", score), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Text(movie.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(8.dp))
    }
}

@Composable
fun MovieDetailContent(
    movie: TmdbMovie,
    viewModel: KollyGameViewModel,
    onDismiss: () -> Unit,
    onRelatedMovieClick: (TmdbMovie) -> Unit,
    onPlayMovieClick: (TmdbMovie) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(movie.id) {
        viewModel.fetchMovieTrailer(context, movie.id, movie.title)
        viewModel.fetchMovieCredits(context, movie.id)
    }

    val trailers by viewModel.movieTrailers.collectAsState()
    val youtubeVideoId = trailers[movie.id]

    val creditsMap by viewModel.movieCredits.collectAsState()
    val credits = creditsMap[movie.id]
    val creditsLoading by viewModel.creditsLoading.collectAsState()

    val selectedPerson by viewModel.selectedPerson.collectAsState()
    val selectedPersonCredits by viewModel.selectedPersonCredits.collectAsState()
    val personLoading by viewModel.personLoading.collectAsState()

    val scrollState = rememberScrollState()

    BackHandler { onDismiss() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                AsyncImage(model = movie.fullBackdropUrl ?: movie.fullPosterUrl, contentDescription = movie.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent, Color(0xFF0C0C12)))))
                IconButton(onClick = onDismiss, modifier = Modifier.padding(top = 40.dp, start = 16.dp).align(Alignment.TopStart).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(movie.title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onPlayMovieClick(movie) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play Movie (Free 4K Search)", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
                val isAdded by remember(movie.id) { derivedStateOf { viewModel.isInWatchlist(movie) } }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { 
                        if (isAdded) viewModel.removeFromWatchlist(context, movie)
                        else viewModel.addToWatchlist(context, movie)
                    },
                    border = BorderStroke(1.5.dp, if (isAdded) Color(0xFFFF6B6B) else Color(0xFFFFD700)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(if (isAdded) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Watchlist", tint = if (isAdded) Color(0xFFFF6B6B) else Color(0xFFFFD700))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isAdded) "Remove from Watchlist" else "Add to Watchlist", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))

                 Text("Movie Trailer", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFFFD700))
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF161622))) {
                    when {
                        youtubeVideoId == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFFD700)) }
                        youtubeVideoId.startsWith("search:") -> YoutubeSearchPlayer(youtubeVideoId.removePrefix("search:"), Modifier.fillMaxSize())
                        else -> YoutubePlayer(youtubeVideoId, Modifier.fillMaxSize())
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("Plot Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFFFD700))
                Spacer(modifier = Modifier.height(6.dp))
                Text(movie.overview.orEmpty().ifEmpty { "Plot details are currently being curated." }, fontSize = 13.sp, color = Color(0xFFC4C4D4), lineHeight = 20.sp)

                Spacer(modifier = Modifier.height(24.dp))

                Text("Starring Cast", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFFFD700))
                Spacer(modifier = Modifier.height(10.dp))

                if (creditsLoading && credits == null) {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFFD700)) }
                } else {
                    val castList = credits?.cast ?: emptyList()
                    if (castList.isEmpty()) {
                        Text("No cast details available.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                            items(castList) { castMember ->
                                CastMemberBubble(castMember = castMember) { viewModel.fetchPersonDetails(context, castMember.id) }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(selectedPerson != null, enter = fadeIn(), exit = fadeOut()) {
            selectedPerson?.let { person ->
                CastPersonOverlay(person, selectedPersonCredits, personLoading, { viewModel.clearSelectedPerson() }) { onRelatedMovieClick(it) }
            }
        }
    }
}

@Composable
fun CastMemberBubble(castMember: TmdbCastMember, onClick: () -> Unit) {
    Column(Modifier.width(80.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(model = castMember.fullProfileUrl, contentDescription = castMember.name, modifier = Modifier.size(68.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFF222232)).border(2.dp, Color(0xFFFFD700).copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape), contentScale = ContentScale.Crop)
        Spacer(modifier = Modifier.height(4.dp))
        Text(castMember.name, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
fun CastPersonOverlay(
    person: TmdbPersonResponse,
    credits: TmdbPersonMovieCreditsResponse?,
    loading: Boolean,
    onBack: () -> Unit,
    onMovieClick: (TmdbMovie) -> Unit
) {
    val scrollState = rememberScrollState()

    BackHandler { onBack() }

    Surface(color = Color(0xFF09090D)) {
        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            Row(Modifier.fillMaxWidth().padding(top = 40.dp, start = 12.dp, end = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color(0xFFFFD700)) }
                Spacer(Modifier.width(12.dp))
                Text("Cast Bio Profile", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }

            if (loading) {
                Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFFD700)) }
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
                    AsyncImage(model = person.fullProfileUrl, contentDescription = person.name, modifier = Modifier.size(width = 100.dp, height = 135.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF161622)).border(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.3f), RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(person.name, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        person.knownForDepartment?.let { Text("Profession: $it", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFFFFD700)) }
                        person.birthday?.let { Spacer(modifier = Modifier.height(4.dp)); Text("Born: $it", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f)) }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text("Biography", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFFFD700))
                    Spacer(Modifier.height(6.dp))
                    Text(person.biography.orEmpty().ifEmpty { "Biography currently not synced online." }, fontSize = 12.sp, color = Color(0xFFC5C5D2), lineHeight = 18.sp)

                    Spacer(Modifier.height(24.dp))

                    Text("Famous Movie Appearances", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFFFD700))
                    Spacer(Modifier.height(10.dp))

                    val relatedMovies = credits?.cast?.take(8) ?: emptyList()
                    if (relatedMovies.isEmpty()) {
                        Text("No appearances sync data.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                            items(relatedMovies) { credit ->
                                MovieRowCard(
                                    movie = TmdbMovie(credit.id, credit.title, null, null, credit.posterPath, null, credit.releaseDate, credit.voteAverage),
                                    onClick = { onMovieClick(TmdbMovie(credit.id, credit.title, null, null, credit.posterPath, null, credit.releaseDate, credit.voteAverage)) },
                                    isWatchlisted = false, isWatched = false, onWatchlistToggle = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YoutubePlayer(youtubeVideoId: String, modifier: Modifier = Modifier) {
    var webViewInstance: WebView? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                webViewInstance = this
            }
        },
        update = { 
            val embedHtml = "<html><body><iframe width=\"100%\" height=\"100%\" src=\"https://www.youtube-nocookie.com/embed/$youtubeVideoId?autoplay=1\" frameborder=\"0\" allowfullscreen></iframe></body></html>"
            it.loadData(embedHtml, "text/html", "utf-8")
        }
    )
}

@Composable
fun YoutubeSearchPlayer(searchQuery: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
             WebView(ctx).apply {
                settings.javaScriptEnabled = true
                webChromeClient = WebChromeClient()
                webViewClient = object: WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        // Auto-click the first video once the search results page loads
                        view?.evaluateJavascript("(function() { document.querySelector(\'a#video-title\').click(); })();", null)
                    }
                }
            }
        },
        update = { 
            it.loadUrl("https://www.youtube.com/results?search_query=$searchQuery")
         }
    )
}

@Composable
fun ErrorStateScreen(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Failed to load movies", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Text(message, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))) { Text("Retry", color = Color.Black) }
        }
    }
}

@Composable
fun EyeIcon(isWatched: Boolean, modifier: Modifier = Modifier) {
    Icon(if (isWatched) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = "Watched", tint = if (isWatched) Color(0xFFFFD700) else Color.White, modifier = modifier)
}
