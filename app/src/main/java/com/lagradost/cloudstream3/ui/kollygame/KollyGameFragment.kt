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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.MainActivity
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
                        MainActivity.nextSearchQuery = movie.title
                        activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                            com.lagradost.cloudstream3.R.id.nav_view
                        )?.selectedItemId = com.lagradost.cloudstream3.R.id.navigation_search
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
fun FilterChipRow(
    title: String,
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .width(55.dp)
                .padding(start = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                val isSelected = item == selectedItem
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFFFFD700) else Color(0xFF1E1E2C))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFFFFD700) else Color.Gray.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onItemSelected(item) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = item,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
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
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var selectedMovie by remember { mutableStateOf<TmdbMovie?>(null) }
    val context = LocalContext.current

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedRating by viewModel.selectedRating.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

    val isSearching by viewModel.isSearching.collectAsState()
    val searchResultMovies by viewModel.searchResultMovies.collectAsState()

    val langMap = remember { mapOf("Tamil" to "ta", "Telugu" to "te", "Malayalam" to "ml", "Hindi" to "hi", "English" to "en") }
    val reverseLangMap = remember { mapOf("ta" to "Tamil", "te" to "Telugu", "ml" to "Malayalam", "hi" to "Hindi", "en" to "English") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F13))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Spotlights
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1E0B36), Color(0xFF0B1436))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🎬 KollyCloud Spotlight",
                        color = Color(0xFFFFD700),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    IconButton(
                        onClick = { viewModel.fetchKollywoodMovies(context) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                }
            }

            // Search Bar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    viewModel.searchQuery.value = it
                    viewModel.searchAndFilterMovies(context)
                },
                placeholder = { Text("Search Tamil, Hindi, English Movies...", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFFFFD700)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.searchQuery.value = ""
                            viewModel.searchAndFilterMovies(context)
                        }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = null, tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF161622),
                    unfocusedContainerColor = Color(0xFF161622),
                    focusedBorderColor = Color(0xFFFFD700),
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Dynamic Advanced Chips Filter Drawer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F13))
                    .padding(bottom = 8.dp)
            ) {
                FilterChipRow(
                    title = "Genre",
                    items = listOf("All", "Action", "Adventure", "Animation", "Comedy", "Crime", "Drama", "Family", "Fantasy", "History", "Horror", "Music", "Mystery", "Romance", "Sci-Fi", "Thriller"),
                    selectedItem = selectedGenre,
                    onItemSelected = {
                        viewModel.selectedGenre.value = it
                        viewModel.searchAndFilterMovies(context)
                    }
                )
                FilterChipRow(
                    title = "Year",
                    items = listOf("All", "2026", "2025", "2024", "2023", "2022", "2021", "2020", "2018", "2015", "2010", "2005", "2003", "2000"),
                    selectedItem = selectedYear,
                    onItemSelected = {
                        viewModel.selectedYear.value = it
                        viewModel.searchAndFilterMovies(context)
                    }
                )
                FilterChipRow(
                    title = "Rating",
                    items = listOf("All", "8.0+", "7.0+", "6.0+", "5.0+"),
                    selectedItem = selectedRating,
                    onItemSelected = {
                        viewModel.selectedRating.value = it
                        viewModel.searchAndFilterMovies(context)
                    }
                )
                FilterChipRow(
                    title = "Lang",
                    items = listOf("Tamil", "Telugu", "Malayalam", "Hindi", "English"),
                    selectedItem = reverseLangMap[selectedLanguage] ?: "Tamil",
                    onItemSelected = {
                        val code = langMap[it] ?: "ta"
                        viewModel.selectedLanguage.value = code
                        viewModel.searchAndFilterMovies(context)
                    }
                )
            }

            // Divider
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f), thickness = 1.dp)

            // Content Catalog
            if (isSearching) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🔍 Advanced Results (${searchResultMovies.size})",
                            color = Color(0xFFFFD700),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = {
                                viewModel.searchQuery.value = ""
                                viewModel.selectedGenre.value = "All"
                                viewModel.selectedYear.value = "All"
                                viewModel.selectedRating.value = "All"
                                viewModel.selectedLanguage.value = "ta"
                                viewModel.searchAndFilterMovies(context)
                            }
                        ) {
                            Text("Reset", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (searchResultMovies.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No movies match your filters.", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    } else {
                        // Display search result in 2 columns and unlimited rows
                        val chunked = searchResultMovies.chunked(2)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 40.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            chunked.forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        MovieRowCard(
                                            movie = pair[0],
                                            onClick = { selectedMovie = pair[0] },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (pair.size > 1) {
                                            MovieRowCard(
                                                movie = pair[1],
                                                onClick = { selectedMovie = pair[1] },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.fillMaxWidth())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                when (val state = uiState) {
                    is KollywoodUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFFFD700))
                        }
                    }
                    is KollywoodUiState.Success -> {
                        KollywoodCatalog(
                            viewModel = viewModel,
                            trending = state.trending,
                            topRated = state.topRated,
                            upcoming = state.upcoming,
                            isDemoMode = state.isDemoMode,
                            errorMessage = null,
                            onMovieClick = { selectedMovie = it },
                            onRetryClick = { viewModel.fetchKollywoodMovies(context) }
                        )
                    }
                    is KollywoodUiState.Error -> {
                        if (state.fallbackTrending != null) {
                            KollywoodCatalog(
                                viewModel = viewModel,
                                trending = state.fallbackTrending,
                                topRated = state.fallbackTopRated ?: emptyList(),
                                upcoming = state.fallbackUpcoming ?: emptyList(),
                                isDemoMode = true,
                                errorMessage = state.message,
                                onMovieClick = { selectedMovie = it },
                                onRetryClick = { viewModel.fetchKollywoodMovies(context) }
                            )
                        } else {
                            ErrorStateScreen(
                                message = state.message,
                                onRetry = { viewModel.fetchKollywoodMovies(context) }
                            )
                        }
                    }
                }
            }
        }

        val movieToShow = selectedMovie
        AnimatedVisibility(
            visible = movieToShow != null,
            enter = androidx.compose.animation.slideInVertically(
                initialOffsetY = { it },
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 450, easing = androidx.compose.animation.core.EaseOutQuart)
            ) + fadeIn(animationSpec = androidx.compose.animation.core.tween(450)),
            exit = androidx.compose.animation.slideOutVertically(
                targetOffsetY = { it },
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 350, easing = androidx.compose.animation.core.EaseInQuart)
            ) + fadeOut(animationSpec = androidx.compose.animation.core.tween(350)),
            modifier = Modifier.fillMaxSize()
        ) {
            if (movieToShow != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = false) {},
                    color = Color(0xFF0C0C12),
                    contentColor = Color.White
                ) {
                    MovieDetailContent(
                        movie = movieToShow,
                        viewModel = viewModel,
                        onDismiss = { selectedMovie = null },
                        onRelatedMovieClick = {
                            selectedMovie = it
                            viewModel.clearSelectedPerson()
                        },
                        onPlayMovieClick = onPlayMovieClick
                    )
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
                            text = if (errorMessage != null) "Couldn't reach live servers. Displaying cached curated Tamil movies." else "Connected directly to TMDb database. Loading live feeds.",
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

        // Watchlist Horizontal Carousel Section
        if (watchlistMovies.isNotEmpty()) {
            MovieRowSection(title = "📂 My Watchlist", movies = watchlistMovies, onMovieClick = onMovieClick)
            Spacer(modifier = Modifier.height(16.dp))
        }

        MovieRowSection(title = "🔥 Trending Tamil Movies", movies = trending, onMovieClick = onMovieClick)
        Spacer(modifier = Modifier.height(16.dp))
        MovieRowSection(title = "⭐ Top Rated Kollywood", movies = topRated, onMovieClick = onMovieClick)
        Spacer(modifier = Modifier.height(16.dp))
        MovieRowSection(title = "📅 Upcoming Tamil Releases", movies = upcoming, onMovieClick = onMovieClick)

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun MovieRowSection(
    title: String,
    movies: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit
) {
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
                    MovieRowCard(movie = movie, onClick = { onMovieClick(movie) })
                }
            }
        }
    }
}

@Composable
fun MovieRowCard(
    movie: TmdbMovie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(135.dp)
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161622))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF232335))
            ) {
                val posterUrl = movie.fullPosterUrl
                if (!posterUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = movie.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x99000000)),
                                startY = 100f
                            )
                        )
                )

                movie.voteAverage?.let { score ->
                    if (score > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = String.format(Locale.US, "%.1f", score),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(6.dp)) {
                Text(
                    text = movie.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                movie.releaseDate?.let { date ->
                    Text(
                        text = if (date.length >= 4) date.substring(0, 4) else date,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
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

    BackHandler {
        onDismiss()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                val backdropUrl = movie.fullBackdropUrl ?: movie.fullPosterUrl
                if (!backdropUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = backdropUrl,
                        contentDescription = movie.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF232335)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Transparent,
                                    Color(0xFF0C0C12)
                                )
                            )
                        )
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(top = 40.dp, start = 16.dp)
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Surface(
                    color = Color(0x33FFD700),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "KOLLYWOOD SPOTLIGHT",
                        color = Color(0xFFFFD700),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = movie.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onPlayMovieClick(movie) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Play Movie (Free 4K Search)",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }

                val isAdded by remember(movie.id) { derivedStateOf { viewModel.isInWatchlist(movie) } }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        if (isAdded) {
                            viewModel.removeFromWatchlist(context, movie)
                        } else {
                            viewModel.addToWatchlist(context, movie)
                        }
                    },
                    border = BorderStroke(1.5.dp, if (isAdded) Color(0xFFFF6B6B) else Color(0xFFFFD700)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isAdded) Color(0xFFFF6B6B) else Color(0xFFFFD700)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = if (isAdded) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Watchlist",
                        tint = if (isAdded) Color(0xFFFF6B6B) else Color(0xFFFFD700),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAdded) "Remove from Watchlist" else "Add to Watchlist",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Movie Trailer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFFFD700)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF161622))
                ) {
                    when {
                        youtubeVideoId == null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFFFFD700))
                            }
                        }
                        youtubeVideoId.startsWith("search:") -> {
                            YoutubeSearchPlayer(
                                searchQuery = youtubeVideoId.removePrefix("search:"),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            YoutubePlayer(youtubeVideoId = youtubeVideoId, modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Plot Summary",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFFFD700)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = movie.overview.orEmpty().ifEmpty { "Plot details are currently being curated." },
                    fontSize = 13.sp,
                    color = Color(0xFFC4C4D4),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Starring Cast",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFFFD700)
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (creditsLoading && credits == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFFD700))
                    }
                } else {
                    val castList = credits?.cast ?: emptyList()
                    if (castList.isEmpty()) {
                        Text("No cast details available.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(castList) { castMember ->
                                CastMemberBubble(castMember = castMember) {
                                    viewModel.fetchPersonDetails(context, castMember.id)
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedPerson != null,
            enter = androidx.compose.animation.slideInVertically(
                initialOffsetY = { it },
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 450, easing = androidx.compose.animation.core.EaseOutQuart)
            ) + fadeIn(animationSpec = androidx.compose.animation.core.tween(450)),
            exit = androidx.compose.animation.slideOutVertically(
                targetOffsetY = { it },
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 350, easing = androidx.compose.animation.core.EaseInQuart)
            ) + fadeOut(animationSpec = androidx.compose.animation.core.tween(350)),
            modifier = Modifier.fillMaxSize()
        ) {
            val person = selectedPerson
            if (person != null) {
                CastPersonOverlay(
                    person = person,
                    credits = selectedPersonCredits,
                    loading = personLoading,
                    onBack = { viewModel.clearSelectedPerson() },
                    onMovieClick = { selectedRelatedMovie ->
                        onRelatedMovieClick(selectedRelatedMovie)
                    }
                )
            }
        }
    }
}

@Composable
fun CastMemberBubble(
    castMember: TmdbCastMember,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val profileUrl = castMember.fullProfileUrl
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color(0xFF222232))
                .border(2.dp, Color(0xFFFFD700).copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
        ) {
            if (!profileUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = profileUrl,
                    contentDescription = castMember.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = castMember.name.firstOrNull()?.toString() ?: "",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = castMember.name,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
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
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    BackHandler {
        onBack()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF09090D)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 12.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFFFD700)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Cast Bio Profile",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFFD700))
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    val profileUrl = person.fullProfileUrl
                    Box(
                        modifier = Modifier
                            .size(width = 100.dp, height = 135.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF161622))
                            .border(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    ) {
                        if (!profileUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = profileUrl,
                                contentDescription = person.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = person.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        person.knownForDepartment?.let { dept ->
                            Text(
                                text = "Profession: $dept",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFFFFD700)
                            )
                        }
                        person.birthday?.let { bday ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Born: $bday",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Biography",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = person.biography.orEmpty().ifEmpty { "Biography currently not synced online." },
                        fontSize = 12.sp,
                        color = Color(0xFFC5C5D2),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Famous Movie Appearances",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val relatedMovies = credits?.cast?.take(8) ?: emptyList()
                    if (relatedMovies.isEmpty()) {
                        Text("No appearances sync data.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(relatedMovies) { credit ->
                                MovieRowCard(
                                    movie = TmdbMovie(
                                        id = credit.id,
                                        title = credit.title,
                                        originalTitle = null,
                                        overview = null,
                                        posterPath = credit.posterPath,
                                        backdropPath = null,
                                        releaseDate = credit.releaseDate,
                                        voteAverage = credit.voteAverage
                                    ),
                                    onClick = {
                                        onMovieClick(
                                            TmdbMovie(
                                                id = credit.id,
                                                title = credit.title,
                                                originalTitle = null,
                                                overview = null,
                                                posterPath = credit.posterPath,
                                                backdropPath = null,
                                                releaseDate = credit.releaseDate,
                                                voteAverage = credit.voteAverage
                                            )
                                        )
                                    }
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
fun YoutubePlayer(
    youtubeVideoId: String,
    modifier: Modifier = Modifier
) {
    var webViewInstance: android.webkit.WebView? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.let { webView ->
                try {
                    (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    webView.destroy()
                } catch (e: Throwable) {
                    android.util.Log.e("YoutubePlayer", "Error destroying WebView", e)
                }
            }
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp)),
        factory = { ctx ->
            android.webkit.WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    allowFileAccess = true
                    allowContentAccess = true
                    databaseEnabled = true
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                }
                webChromeClient = android.webkit.WebChromeClient()
                webViewClient = android.webkit.WebViewClient()
                webViewInstance = this
            }
        },
        update = { webView ->
            val currentTag = webView.tag as? String
            if (currentTag != youtubeVideoId) {
                webView.tag = youtubeVideoId
                val embedHtml = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <meta name="referrer" content="strict-origin-when-cross-origin">
                    </head>
                    <body style="margin:0;padding:0;background-color:#000000;">
                        <iframe
                            width="100%"
                            height="100%"
                            src="https://www.youtube-nocookie.com/embed/$youtubeVideoId?autoplay=1&mute=1&controls=1&rel=0&showinfo=0"
                            title="Trailer"
                            frameborder="0"
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                            allowfullscreen
                            style="position:fixed;top:0;left:0;width:100%;height:100%;border:none;margin:0;padding:0;overflow:hidden;z-index:999999;">
                        </iframe>
                    </body>
                    </html>
                """.trimIndent()
                webView.loadDataWithBaseURL(
                    "https://www.youtube-nocookie.com",
                    embedHtml,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}

@Composable
fun YoutubeSearchPlayer(
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    var webViewInstance: android.webkit.WebView? by remember { mutableStateOf(null) }
    val encodedQuery = remember(searchQuery) {
        java.net.URLEncoder.encode(searchQuery, "UTF-8")
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.let { webView ->
                try {
                    (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    webView.destroy()
                } catch (e: Throwable) {
                    android.util.Log.e("YoutubeSearchPlayer", "Error destroying WebView", e)
                }
            }
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp)),
        factory = { ctx ->
            android.webkit.WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    allowFileAccess = true
                    allowContentAccess = true
                    databaseEnabled = true
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                }
                webChromeClient = android.webkit.WebChromeClient()
                webViewClient = android.webkit.WebViewClient()
                webViewInstance = this
            }
        },
        update = { webView ->
            val tag = "search:$encodedQuery"
            if (webView.tag as? String != tag) {
                webView.tag = tag
                val embedHtml = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <meta name="referrer" content="strict-origin-when-cross-origin">
                    </head>
                    <body style="margin:0;padding:0;background-color:#000000;">
                        <iframe
                            width="100%"
                            height="100%"
                            src="https://www.youtube-nocookie.com/embed?listType=search&list=$encodedQuery&autoplay=1&mute=1&controls=1&rel=0&showinfo=0"
                            title="Trailer"
                            frameborder="0"
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                            allowfullscreen
                            style="position:fixed;top:0;left:0;width:100%;height:100%;border:none;margin:0;padding:0;overflow:hidden;z-index:999999;">
                        </iframe>
                    </body>
                    </html>
                """.trimIndent()
                webView.loadDataWithBaseURL(
                    "https://www.youtube-nocookie.com",
                    embedHtml,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}

@Composable
fun ErrorStateScreen(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Failed to load movies",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
            ) {
                Text("Retry", color = Color.Black)
            }
        }
    }
}
