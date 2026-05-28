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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
                        val year = if (!movie.releaseDate.isNullOrBlank() && movie.releaseDate.length >= 4) {
                            " " + movie.releaseDate.substring(0, 4)
                        } else ""
                        MainActivity.nextSearchQuery = "${movie.title}$year"
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

enum class FilterType {
    GENRE, YEAR, RATING, LANGUAGE, SORT, ARTIST
}

private val curatedArtists = listOf(
    TmdbCastMember(819L, "Rajinikanth", null, "/zP1gVjF0P6GqL12V1v7Z8p3xP1e.jpg"),
    TmdbCastMember(30784L, "Kamal Haasan", null, "/51wR2j81B2TzN1Yx7J1z6y9qP2b.jpg"),
    TmdbCastMember(58197L, "Vijay", null, "/6T8V4jF2V1y6N7v8V3z1xP3b.jpg"),
    TmdbCastMember(75510L, "Ajith Kumar", null, "/2g7V4jF2V1y6N7v8V3z1xP4b.jpg"),
    TmdbCastMember(118595L, "Suriya", null, "/3g7V4jF2V1y6N7v8V3z1xP5b.jpg"),
    TmdbCastMember(1251347L, "Dhanush", null, "/4g7V4jF2V1y6N7v8V3z1xP6b.jpg"),
    TmdbCastMember(173873L, "Vikram", null, "/5g7V4jF2V1y6N7v8V3z1xP7b.jpg"),
    TmdbCastMember(989100L, "Lokesh Kanagaraj", null, "/6g7V4jF2V1y6N7v8V3z1xP8b.jpg"),
    TmdbCastMember(1682855L, "Nelson Dilipkumar", null, "/7g7V4jF2V1y6N7v8V3z1xP9b.jpg"),
    TmdbCastMember(236053L, "Mani Ratnam", null, "/21m7N41lYshs6H38L21s6y9qP5b.jpg")
)

@Composable
fun FilterBadge(
    label: String,
    onClick: () -> Unit,
    isHighlight: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isHighlight) Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
                else Brush.horizontalGradient(listOf(Color(0xFF1E1E2C), Color(0xFF161622)))
            )
            .border(
                width = 1.dp,
                color = if (isHighlight) Color.Transparent else Color.Gray.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = if (isHighlight) Color.Black else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = if (isHighlight) Color.Black else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun PremiumFilterRow(
    viewModel: KollyGameViewModel,
    onOpenFilterDialog: (FilterType) -> Unit
) {
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedRating by viewModel.selectedRating.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val selectedSortOrder by viewModel.selectedSortOrder.collectAsState()
    val selectedArtist by viewModel.selectedArtist.collectAsState()

    val reverseLangMap = remember { mapOf("ta" to "Tamil", "te" to "Telugu", "ml" to "Malayalam", "hi" to "Hindi", "en" to "English") }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            FilterBadge(
                label = "Genre: $selectedGenre",
                onClick = { onOpenFilterDialog(FilterType.GENRE) },
                isHighlight = selectedGenre != "All"
            )
        }
        item {
            FilterBadge(
                label = "Year: $selectedYear",
                onClick = { onOpenFilterDialog(FilterType.YEAR) },
                isHighlight = selectedYear != "All"
            )
        }
        item {
            FilterBadge(
                label = "Rating: $selectedRating",
                onClick = { onOpenFilterDialog(FilterType.RATING) },
                isHighlight = selectedRating != "All"
            )
        }
        item {
            FilterBadge(
                label = "Lang: ${reverseLangMap[selectedLanguage] ?: "Tamil"}",
                onClick = { onOpenFilterDialog(FilterType.LANGUAGE) },
                isHighlight = selectedLanguage != "ta"
            )
        }
        item {
            FilterBadge(
                label = "Sort: $selectedSortOrder",
                onClick = { onOpenFilterDialog(FilterType.SORT) },
                isHighlight = selectedSortOrder != "Popularity"
            )
        }
        item {
            FilterBadge(
                label = "Artist: ${selectedArtist?.name ?: "All"}",
                onClick = { onOpenFilterDialog(FilterType.ARTIST) },
                isHighlight = selectedArtist != null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FilterSelectionDialog(
    title: String,
    searchPlaceholder: String,
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    itemLabel: (T) -> String,
    onSearchQueryChange: ((String) -> Unit)? = null,
    isSearching: Boolean = false,
    itemImage: @Composable ((T) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredItems = remember(searchQuery, items) {
        if (onSearchQueryChange != null) {
            items
        } else {
            if (searchQuery.isBlank()) items
            else items.filter { itemLabel(it).contains(searchQuery, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        onSearchQueryChange?.invoke(it)
                    },
                    placeholder = { Text(searchPlaceholder, color = Color.Gray, fontSize = 13.sp) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                onSearchQueryChange?.invoke("")
                            }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E1E2C),
                        unfocusedContainerColor = Color(0xFF1E1E2C),
                        focusedBorderColor = Color(0xFFFFD700),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFFD700))
                    }
                } else if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No matching results found.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredItems) { item ->
                            val isSelected = item == selectedItem
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFFFD700).copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        onItemSelected(item)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (itemImage != null) {
                                        itemImage(item)
                                    }
                                    Text(
                                        text = itemLabel(item),
                                        color = if (isSelected) Color(0xFFFFD700) else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0F0F16),
        shape = RoundedCornerShape(16.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(16.dp)
    )
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
    var showWatchlistOverlay by remember { mutableStateOf(false) }
    var showWatchedOverlay by remember { mutableStateOf(false) }
    val watchlistMovies by viewModel.watchlist.collectAsState()
    val watchedMovies by viewModel.watchedMovies.collectAsState()
    val watchedListMovies by viewModel.watchedListMovies.collectAsState()
    val context = LocalContext.current

    val socialTrendingMovies by viewModel.socialTrendingMovies.collectAsState()
    val nowRunningMovies by viewModel.nowRunningMovies.collectAsState()
    val audienceBuzz by viewModel.audienceBuzz.collectAsState()
    var expandedReview by remember { mutableStateOf<TmdbReview?>(null) }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedRating by viewModel.selectedRating.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val selectedSortOrder by viewModel.selectedSortOrder.collectAsState()
    val selectedArtist by viewModel.selectedArtist.collectAsState()
    val artistSearchResults by viewModel.artistSearchResults.collectAsState()
    val isSearchingArtists by viewModel.isSearchingArtists.collectAsState()

    val isSearching by viewModel.isSearching.collectAsState()
    val searchResultMovies by viewModel.searchResultMovies.collectAsState()

    val activeTrailer by viewModel.activeTrailerVideoId.collectAsState()
    val isMinimized by viewModel.isTrailerMinimized.collectAsState()
    var showAiCurator by remember { mutableStateOf(false) }
    var curatorPrompt by remember { mutableStateOf("") }

    val langMap = remember { mapOf("Tamil" to "ta", "Telugu" to "te", "Malayalam" to "ml", "Hindi" to "hi", "English" to "en") }
    val reverseLangMap = remember { mapOf("ta" to "Tamil", "te" to "Telugu", "ml" to "Malayalam", "hi" to "Hindi", "en" to "English") }
    var activeFilterDialog by remember { mutableStateOf<FilterType?>(null) }

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
                    .statusBarsPadding()
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showWatchedOverlay = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            EyeIcon(
                                isWatched = true,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(
                            onClick = { showWatchlistOverlay = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Watchlist",
                                tint = Color(0xFFFF6B6B)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(
                            onClick = { viewModel.fetchKollywoodMovies(context, forceRefresh = true) },
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (showAiCurator) "🤖 AI Recommendations Curator" else "💡 Try AI Smart Search",
                    color = Color(0xFFFFD700),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { showAiCurator = !showAiCurator },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (showAiCurator) "Close Curator" else "Activate Curator",
                        color = Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
                    if (showAiCurator) {
                val chatMessages by viewModel.curatorChatMessages.collectAsState()
                val isCurating by viewModel.isCurating.collectAsState()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161622)),
                    border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        Text(
                            text = "🤖 KollyAI Curator Chat",
                            color = Color(0xFFFFD700),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Messages Stream
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chatMessages) { msg ->
                                val isAi = msg.sender == "KollyAI"
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    horizontalAlignment = if (isAi) Alignment.Start else Alignment.End
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (isAi) 0.dp else 12.dp,
                                                bottomEnd = if (isAi) 12.dp else 0.dp
                                            ))
                                            .background(if (isAi) Color(0xFF2E2E3A) else Color(0x33FFFFD700))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isAi && msg.recommendedMovies.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            items(msg.recommendedMovies) { movie ->
                                                Card(
                                                    modifier = Modifier
                                                        .width(100.dp)
                                                        .clickable {
                                                            selectedMovie = movie
                                                            showAiCurator = false
                                                        },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C))
                                                ) {
                                                    Column(modifier = Modifier.padding(6.dp)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(100.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(Color.DarkGray)
                                                        ) {
                                                            if (!movie.posterPath.isNullOrBlank()) {
                                                                AsyncImage(
                                                                    model = "https://image.tmdb.org/t/p/w185${movie.posterPath}",
                                                                    contentDescription = movie.title,
                                                                    modifier = Modifier.fillMaxSize(),
                                                                    contentScale = ContentScale.Crop
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = movie.title,
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (isCurating) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF2E2E3A))
                                            .padding(10.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color(0xFFFFD700),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Input Area
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = curatorPrompt,
                                onValueChange = { curatorPrompt = it },
                                placeholder = { Text("Ask KollyAI...", color = Color.Gray, fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF0F0F16),
                                    unfocusedContainerColor = Color(0xFF0F0F16),
                                    focusedBorderColor = Color(0xFFFFD700),
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (curatorPrompt.isNotBlank()) {
                                        viewModel.curateSearch(context, curatorPrompt)
                                        curatorPrompt = ""
                                    }
                                },
                                enabled = curatorPrompt.isNotBlank() && !isCurating,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Send", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            PremiumFilterRow(
                viewModel = viewModel,
                onOpenFilterDialog = { activeFilterDialog = it }
            )

            // Divider
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f), thickness = 1.dp)

            // Content Catalog
            if (isSearching) {
                val searchScrollState = rememberScrollState()
                val isAtEnd = searchScrollState.value >= searchScrollState.maxValue - 200 && searchScrollState.maxValue > 0
                val isLoadMoreLoading by viewModel.isLoadMoreLoading.collectAsState()

                LaunchedEffect(isAtEnd) {
                    if (isAtEnd && !isLoadMoreLoading && !viewModel.isFilterPaginationExhausted) {
                        viewModel.loadNextPage(context)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(searchScrollState)
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
                                viewModel.selectedSortOrder.value = "Popularity"
                                viewModel.selectedArtist.value = null
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
                                .padding(bottom = 20.dp),
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
                                            modifier = Modifier.fillMaxWidth(),
                                            isWatchlisted = viewModel.isInWatchlist(pair[0]),
                                            isWatched = watchedMovies.contains(pair[0].id),
                                            onWatchlistToggle = {
                                                if (viewModel.isInWatchlist(pair[0])) {
                                                    viewModel.removeFromWatchlist(context, pair[0])
                                                } else {
                                                    viewModel.addToWatchlist(context, pair[0])
                                                }
                                            },
                                            onWatchedToggle = { viewModel.toggleWatchedMovie(context, pair[0]) }
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (pair.size > 1) {
                                            MovieRowCard(
                                                movie = pair[1],
                                                onClick = { selectedMovie = pair[1] },
                                                modifier = Modifier.fillMaxWidth(),
                                                isWatchlisted = viewModel.isInWatchlist(pair[1]),
                                                isWatched = watchedMovies.contains(pair[1].id),
                                                onWatchlistToggle = {
                                                    if (viewModel.isInWatchlist(pair[1])) {
                                                        viewModel.removeFromWatchlist(context, pair[1])
                                                    } else {
                                                        viewModel.addToWatchlist(context, pair[1])
                                                    }
                                                },
                                                onWatchedToggle = { viewModel.toggleWatchedMovie(context, pair[1]) }
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.fillMaxWidth())
                                        }
                                    }
                                }
                            }
                        }

                        if (isLoadMoreLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFFFFD700),
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            } else {
                val pullToRefreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.fetchKollywoodMovies(context, forceRefresh = true) },
                    state = pullToRefreshState,
                    modifier = Modifier.fillMaxSize()
                ) {
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
                                socialTrending = socialTrendingMovies,
                                nowRunning = nowRunningMovies,
                                audienceBuzz = audienceBuzz,
                                isDemoMode = state.isDemoMode,
                                errorMessage = null,
                                onMovieClick = { selectedMovie = it },
                                onPlayClick = { movie ->
                                    selectedMovie = movie
                                },
                                onRetryClick = { viewModel.fetchKollywoodMovies(context, forceRefresh = true) },
                                onReviewClick = { expandedReview = it }
                            )
                        }
                        is KollywoodUiState.Error -> {
                            if (state.fallbackTrending != null) {
                                KollywoodCatalog(
                                    viewModel = viewModel,
                                    trending = state.fallbackTrending,
                                    topRated = state.fallbackTopRated ?: emptyList(),
                                    upcoming = state.fallbackUpcoming ?: emptyList(),
                                    socialTrending = socialTrendingMovies,
                                    nowRunning = nowRunningMovies,
                                    audienceBuzz = audienceBuzz,
                                    isDemoMode = true,
                                    errorMessage = state.message,
                                    onMovieClick = { selectedMovie = it },
                                    onPlayClick = { movie ->
                                        selectedMovie = movie
                                    },
                                    onRetryClick = { viewModel.fetchKollywoodMovies(context, forceRefresh = true) },
                                    onReviewClick = { expandedReview = it }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ErrorStateScreen(
                                        message = state.message,
                                        onRetry = { viewModel.fetchKollywoodMovies(context, forceRefresh = true) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        ItemOverlay(
            visible = showWatchlistOverlay,
            title = "📂 My Watchlist",
            emptyText = "Your watchlist is empty.",
            emptyIcon = {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
            },
            movies = watchlistMovies,
            viewModel = viewModel,
            onBack = { showWatchlistOverlay = false },
            onMovieClick = { movie ->
                selectedMovie = movie
                showWatchlistOverlay = false
            }
        )

        ItemOverlay(
            visible = showWatchedOverlay,
            title = "🎬 Watched Movies",
            emptyText = "Your watched list is empty.",
            emptyIcon = {
                EyeIcon(
                    isWatched = false,
                    modifier = Modifier.size(64.dp)
                )
            },
            movies = watchedListMovies,
            viewModel = viewModel,
            onBack = { showWatchedOverlay = false },
            onMovieClick = { movie ->
                selectedMovie = movie
                showWatchedOverlay = false
            }
        )

        expandedReview?.let { review ->
            AlertDialog(
                onDismissRequest = { expandedReview = null },
                title = {
                    Column {
                        Text(
                            text = review.movieTitle,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Review by ${review.author}",
                            color = Color(0xFFFFD700),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .heightIn(max = 300.dp)
                    ) {
                        Text(
                            text = review.content,
                            color = Color(0xFFC5C5D2),
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { expandedReview = null }) {
                        Text("Close", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF0F0F16),
                shape = RoundedCornerShape(16.dp)
            )
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

        // Filter Selection Dialogs display
        activeFilterDialog?.let { filterType ->
            val context = LocalContext.current
            when (filterType) {
                FilterType.GENRE -> {
                    FilterSelectionDialog(
                        title = "Select Genre",
                        searchPlaceholder = "Search Genre...",
                        items = listOf("All") + viewModel.genreMap.keys.toList(),
                        selectedItem = selectedGenre,
                        onItemSelected = {
                            viewModel.selectedGenre.value = it
                            viewModel.searchAndFilterMovies(context)
                        },
                        onDismiss = { activeFilterDialog = null },
                        itemLabel = { it }
                    )
                }
                FilterType.YEAR -> {
                    FilterSelectionDialog(
                        title = "Select Year",
                        searchPlaceholder = "Search Year or Decade...",
                        items = listOf("All", "2020s", "2010s", "2000s", "90s", "80s") + (2026 downTo 1970).map { it.toString() },
                        selectedItem = selectedYear,
                        onItemSelected = {
                            viewModel.selectedYear.value = it
                            viewModel.searchAndFilterMovies(context)
                        },
                        onDismiss = { activeFilterDialog = null },
                        itemLabel = { it }
                    )
                }
                FilterType.RATING -> {
                    FilterSelectionDialog(
                        title = "Select Rating",
                        searchPlaceholder = "Search Rating...",
                        items = listOf("All", "8.5+", "8.0+", "7.5+", "7.0+", "6.5+", "6.0+", "5.0+"),
                        selectedItem = selectedRating,
                        onItemSelected = {
                            viewModel.selectedRating.value = it
                            viewModel.searchAndFilterMovies(context)
                        },
                        onDismiss = { activeFilterDialog = null },
                        itemLabel = { it }
                    )
                }
                FilterType.LANGUAGE -> {
                    FilterSelectionDialog(
                        title = "Select Language",
                        searchPlaceholder = "Search Language...",
                        items = listOf("ta", "te", "ml", "hi", "en", "All"),
                        selectedItem = selectedLanguage,
                        onItemSelected = {
                            viewModel.selectedLanguage.value = it
                            viewModel.searchAndFilterMovies(context)
                        },
                        onDismiss = { activeFilterDialog = null },
                        itemLabel = { reverseLangMap[it] ?: "All" }
                    )
                }
                FilterType.SORT -> {
                    FilterSelectionDialog(
                        title = "Sort Order",
                        searchPlaceholder = "Search Sorting...",
                        items = listOf("Popularity", "Rating", "Release Date", "Title A-Z"),
                        selectedItem = selectedSortOrder,
                        onItemSelected = {
                            viewModel.selectedSortOrder.value = it
                            viewModel.searchAndFilterMovies(context)
                        },
                        onDismiss = { activeFilterDialog = null },
                        itemLabel = { it }
                    )
                }
                FilterType.ARTIST -> {
                    FilterSelectionDialog(
                        title = "Select Artist",
                        searchPlaceholder = "Search Cast/Crew Name...",
                        items = listOf(TmdbCastMember(-1L, "All", null, null)) + curatedArtists + artistSearchResults,
                        selectedItem = selectedArtist,
                        onItemSelected = { artist ->
                            viewModel.selectedArtist.value = if (artist.id == -1L) null else artist
                            viewModel.searchAndFilterMovies(context)
                        },
                        onDismiss = { activeFilterDialog = null },
                        itemLabel = { it.name },
                        onSearchQueryChange = { query ->
                            viewModel.searchArtists(context, query)
                        },
                        isSearching = isSearchingArtists,
                        itemImage = { item ->
                            if (item.id != -1L) {
                                val profileUrl = item.fullProfileUrl
                                if (!profileUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = profileUrl,
                                        contentDescription = item.name,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(Color.Gray),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(Color(0xFF222232)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.name.firstOrNull()?.toString() ?: "",
                                            color = Color(0xFFFFD700),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
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
    socialTrending: List<TmdbMovie>,
    nowRunning: List<TmdbMovie>,
    audienceBuzz: List<TmdbReview>,
    isDemoMode: Boolean,
    errorMessage: String?,
    onMovieClick: (TmdbMovie) -> Unit,
    onPlayClick: (TmdbMovie) -> Unit,
    onRetryClick: () -> Unit,
    onReviewClick: (TmdbReview) -> Unit
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

        // Spotlight Pager Carousel
        if (socialTrending.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SpotlightPager(
                    movies = socialTrending.take(5),
                    viewModel = viewModel,
                    onMovieClick = onMovieClick,
                    onPlayClick = onPlayClick
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Watchlist Horizontal Carousel Section
        if (watchlistMovies.isNotEmpty()) {
            MovieRowSection(title = "📂 My Watchlist", movies = watchlistMovies, viewModel = viewModel, onMovieClick = onMovieClick)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 1. Trending in Socials & Discussions
        if (socialTrending.isNotEmpty()) {
            MovieRowSection(title = "🔥 Trending in Socials & Discussions", movies = socialTrending, viewModel = viewModel, onMovieClick = onMovieClick)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. Now Playing in Theaters
        if (nowRunning.isNotEmpty()) {
            MovieRowSection(title = "🎬 Now Playing in Theaters", movies = nowRunning, viewModel = viewModel, onMovieClick = onMovieClick)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. Trending Tamil Movies
        MovieRowSection(title = "🔥 Trending Tamil Movies", movies = trending, viewModel = viewModel, onMovieClick = onMovieClick)
        Spacer(modifier = Modifier.height(16.dp))

        // 4. Top Rated Kollywood
        MovieRowSection(title = "⭐ Top Rated Kollywood", movies = topRated, viewModel = viewModel, onMovieClick = onMovieClick)
        Spacer(modifier = Modifier.height(16.dp))

        // 5. Upcoming Tamil Releases
        MovieRowSection(title = "📅 Upcoming Tamil Releases", movies = upcoming, viewModel = viewModel, onMovieClick = onMovieClick)
        Spacer(modifier = Modifier.height(16.dp))

        // 6. Audience Buzz
        if (audienceBuzz.isNotEmpty()) {
            AudienceBuzzRow(reviews = audienceBuzz, onReviewClick = onReviewClick)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun ItemOverlay(
    visible: Boolean,
    title: String,
    emptyText: String,
    emptyIcon: @Composable () -> Unit,
    movies: List<TmdbMovie>,
    viewModel: KollyGameViewModel,
    onBack: () -> Unit,
    onMovieClick: (TmdbMovie) -> Unit
) {
    val context = LocalContext.current
    val watchedMovies by viewModel.watchedMovies.collectAsState()

    AnimatedVisibility(
        visible = visible,
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
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0C0C12),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFFFFD700)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (movies.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            emptyIcon()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(emptyText, color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    // 2-column grid
                    val chunked = movies.chunked(2)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
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
                                    val isMovieWatchlisted = viewModel.isInWatchlist(pair[0])
                                    val isMovieWatched = watchedMovies.contains(pair[0].id)
                                    MovieRowCard(
                                        movie = pair[0],
                                        onClick = {
                                            onMovieClick(pair[0])
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        isWatchlisted = isMovieWatchlisted,
                                        isWatched = isMovieWatched,
                                        onWatchlistToggle = {
                                            if (isMovieWatchlisted) {
                                                viewModel.removeFromWatchlist(context, pair[0])
                                            } else {
                                                viewModel.addToWatchlist(context, pair[0])
                                            }
                                        },
                                        onWatchedToggle = { viewModel.toggleWatchedMovie(context, pair[0]) }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    if (pair.size > 1) {
                                        val isMovieWatchlisted = viewModel.isInWatchlist(pair[1])
                                        val isMovieWatched = watchedMovies.contains(pair[1].id)
                                        MovieRowCard(
                                            movie = pair[1],
                                            onClick = {
                                                onMovieClick(pair[1])
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            isWatchlisted = isMovieWatchlisted,
                                            isWatched = isMovieWatched,
                                            onWatchlistToggle = {
                                                if (isMovieWatchlisted) {
                                                    viewModel.removeFromWatchlist(context, pair[1])
                                                } else {
                                                    viewModel.addToWatchlist(context, pair[1])
                                                }
                                            },
                                            onWatchedToggle = { viewModel.toggleWatchedMovie(context, pair[1]) }
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
        }
    }
}

@Composable
fun SpotlightPager(
    movies: List<TmdbMovie>,
    viewModel: KollyGameViewModel,
    onMovieClick: (TmdbMovie) -> Unit,
    onPlayClick: (TmdbMovie) -> Unit
) {
    if (movies.isEmpty()) return

    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { movies.size })

    // Auto-scroll logic
    LaunchedEffect(key1 = pagerState) {
        while (true) {
            kotlinx.coroutines.delay(5000)
            val nextPage = (pagerState.currentPage + 1) % movies.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    val watchedMovies by viewModel.watchedMovies.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161622))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val movie = movies[page]
            val backdropUrl = movie.fullBackdropUrl ?: movie.fullPosterUrl
            val isWatchlisted = viewModel.isInWatchlist(movie)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onMovieClick(movie) }
            ) {
                if (!backdropUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = backdropUrl,
                        contentDescription = movie.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                                startY = 100f
                            )
                        )
                )

                // Rating Overlay in upper right
                movie.voteAverage?.let { score ->
                    if (score > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = String.format(Locale.US, "%.1f", score),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Content at the bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Surface(
                        color = Color(0xFFFFD700).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "🔥 SOCIAL TRENDING SPOTLIGHT",
                            color = Color(0xFFFFD700),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = movie.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play Button
                        Button(
                            onClick = { onPlayClick(movie) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Watchlist Button
                        OutlinedButton(
                            onClick = {
                                if (isWatchlisted) {
                                    viewModel.removeFromWatchlist(context, movie)
                                } else {
                                    viewModel.addToWatchlist(context, movie)
                                }
                            },
                            border = BorderStroke(1.dp, if (isWatchlisted) Color(0xFFFF6B6B) else Color.White),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isWatchlisted) Color(0xFFFF6B6B) else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isWatchlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isWatchlisted) Color(0xFFFF6B6B) else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isWatchlisted) "Saved" else "Watchlist",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Pager indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(movies.size) { index ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (active) 12.dp else 6.dp, 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (active) Color(0xFFFFD700) else Color.White.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
fun AudienceBuzzRow(
    reviews: List<TmdbReview>,
    onReviewClick: (TmdbReview) -> Unit
) {
    if (reviews.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "💬 Audience Buzz (User Reviews)",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFFFFD700),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(reviews) { review ->
                Card(
                    modifier = Modifier
                        .width(280.dp)
                        .height(130.dp)
                        .clickable { onReviewClick(review) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161622)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Mini Poster
                            val posterUrl = review.moviePoster?.let {
                                if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w92$it"
                            }
                            if (!posterUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = posterUrl,
                                    contentDescription = review.movieTitle,
                                    modifier = Modifier
                                        .size(32.dp, 48.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = review.movieTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "by ${review.author}",
                                    fontSize = 10.sp,
                                    color = Color(0xFFFFD700),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            review.rating?.let { rating ->
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF2E2E3A), RoundedCornerShape(4.dp))
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
                                            text = String.format(Locale.US, "%.0f", rating),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = review.content,
                            fontSize = 11.sp,
                            color = Color(0xFFC5C5D2),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
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
    modifier: Modifier = Modifier.width(135.dp),
    isWatchlisted: Boolean = false,
    isWatched: Boolean = false,
    onWatchlistToggle: (() -> Unit)? = null,
    onWatchedToggle: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .alpha(if (isWatched) 0.4f else 1.0f),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWatched) Color(0xFF0F0F16) else Color(0xFF161622)
        )
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

                // Overlays: Eye & Heart Icons (Watchlist & Seen)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onWatchedToggle != null) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .clickable { onWatchedToggle() },
                            contentAlignment = Alignment.Center
                        ) {
                            EyeIcon(
                                isWatched = isWatched,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(1.dp))
                    }

                    if (onWatchlistToggle != null) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .clickable { onWatchlistToggle() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isWatchlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Watchlist",
                                tint = if (isWatchlisted) Color(0xFFFF6B6B) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                movie.voteAverage?.let { score ->
                    if (score > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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

                Spacer(modifier = Modifier.height(24.dp))

                // Reddit Community Lounge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💬 Reddit Community Lounge",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFFFFD700),
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = Color(0x33FF4500),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "r/kollywood",
                            color = Color(0xFFFF4500),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Live fan discussions, memes, and community reviews synced from Reddit.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(10.dp))

                val redditThreads by viewModel.redditDiscussionThreads.collectAsState()
                val redditLoading by viewModel.redditLoading.collectAsState()

                LaunchedEffect(movie.id) {
                    viewModel.fetchRedditDiscussions(movie.title)
                }

                if (redditLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFFD700))
                    }
                } else if (redditThreads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .background(Color(0xFF161622), RoundedCornerShape(8.dp))
                            .border(0.5.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No live discussions found on Reddit yet. Be the first to start a thread!",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        redditThreads.forEach { thread ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (thread.link.isNotEmpty()) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(thread.link))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161622)),
                                border = BorderStroke(0.5.dp, Color.Gray.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = thread.title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = Color(0xFFFF4500),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "View Discussion",
                                            color = Color(0xFFFFD700),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                            src="https://www.youtube-nocookie.com/embed/$youtubeVideoId?autoplay=1&mute=0&controls=1&rel=0&showinfo=0"
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
                            src="https://www.youtube-nocookie.com/embed?listType=search&list=$encodedQuery&autoplay=1&mute=0&controls=1&rel=0&showinfo=0"
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

@Composable
fun EyeIcon(
    isWatched: Boolean,
    modifier: Modifier = Modifier
) {
    val tint = if (isWatched) Color(0xFFFFD700) else Color.White.copy(alpha = 0.8f)
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Draw eye shape outer path
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.1f, h * 0.5f)
            cubicTo(w * 0.35f, h * 0.2f, w * 0.65f, h * 0.2f, w * 0.9f, h * 0.5f)
            cubicTo(w * 0.65f, h * 0.8f, w * 0.35f, h * 0.8f, w * 0.1f, h * 0.5f)
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
        )
        
        // Draw pupil (inner circle)
        drawCircle(
            color = tint,
            radius = w * 0.18f,
            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.5f),
            style = if (isWatched) androidx.compose.ui.graphics.drawscope.Fill else androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
        )
    }
}
