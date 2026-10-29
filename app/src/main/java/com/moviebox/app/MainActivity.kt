package com.moviebox.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.moviebox.app.data.repository.UserPresenceManager
import com.moviebox.app.ui.user.InAppUpdateDialog
import com.moviebox.app.ui.user.NotificationsScreen
import com.moviebox.app.ui.user.OfflineDownloadsScreen
import com.moviebox.app.util.FcmPushSender
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import com.moviebox.app.security.NhNativeSecurityVault
import com.moviebox.app.util.OfflineDownloadManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moviebox.app.data.model.CatalogItem
import com.moviebox.app.ui.MovieBoxViewModel
import com.moviebox.app.ui.admin.AdminShellScreen
import com.moviebox.app.ui.theme.CrimsonLight
import com.moviebox.app.ui.theme.CrimsonRed
import com.moviebox.app.ui.theme.MovieBoxTheme
import com.moviebox.app.ui.theme.TextSecondary
import com.moviebox.app.ui.theme.TextWhite
import com.moviebox.app.ui.user.DetailsScreen
import com.moviebox.app.ui.user.HomeScreen
import com.moviebox.app.ui.user.MoviesScreen
import com.moviebox.app.ui.user.SearchScreen
import com.moviebox.app.ui.user.SplashScreen
import com.moviebox.app.ui.user.UserSettingsScreen
import com.moviebox.app.ui.user.VideoPlayerScreen
import com.moviebox.app.ui.user.WatchlistScreen
import com.moviebox.app.ui.user.WebSeriesScreen

private enum class UserRootTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    MOVIES("Movies", Icons.Filled.Movie),
    WEB_SERIES("Web Series", Icons.Filled.LiveTv),
    MY_LIST("My List", Icons.Filled.Bookmark),
    PROFILE("Profile", Icons.Filled.Person)
}

private sealed class ActiveOverlay {
    data object None : ActiveOverlay()
    data object Search : ActiveOverlay()
    data object Notifications : ActiveOverlay()
    data object OfflineDownloads : ActiveOverlay()
    data class Details(
        val item: CatalogItem,
        val previousItem: CatalogItem? = null
    ) : ActiveOverlay()
    data class Player(
        val title: String,
        val streamUrl: String,
        val movieItem: CatalogItem? = null,
        val seriesItem: CatalogItem? = null,
        val seasonNum: Int? = null,
        val episodeNum: Int? = null
    ) : ActiveOverlay()
    data object AdminPanel : ActiveOverlay()
}

class MainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        UserPresenceManager.setForeground(true)
    }

    override fun onStop() {
        UserPresenceManager.setForeground(false)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        setContent {
            val vm: MovieBoxViewModel = viewModel()
            val prefs by vm.preferences.collectAsStateWithLifecycle()

            MovieBoxTheme(variant = prefs.themeVariant) {
                MovieBoxRootApp(vm = vm)
            }
        }
    }
}

@Composable
fun MovieBoxRootApp(vm: MovieBoxViewModel) {
    var showSplash by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf(UserRootTab.HOME) }
    var activeOverlay by remember { mutableStateOf<ActiveOverlay>(ActiveOverlay.None) }

    val catalogState by vm.catalogState.collectAsStateWithLifecycle()
    val authState by vm.authState.collectAsStateWithLifecycle()
    val currentUserRecord by vm.currentUserRecord.collectAsStateWithLifecycle()
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val cacheSizeBytes by vm.cacheSizeBytes.collectAsStateWithLifecycle()
    val toastMsg by vm.toast.collectAsStateWithLifecycle()
    val enrichedItemMap by vm.enrichedItemMap.collectAsStateWithLifecycle()
    val actorDetailsMap by vm.actorDetailsMap.collectAsStateWithLifecycle()
    val indianPosterUrls by vm.indianPosterUrls.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var dismissedUpdateTimestamp by rememberSaveable { mutableStateOf(0L) }
    var lastShownNotificationId by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        OfflineDownloadManager.ensureInitialized(context)
    }
    val offlineDownloadsMap by OfflineDownloadManager.downloadsMap.collectAsStateWithLifecycle()
    val offlineDownloadsList = remember(offlineDownloadsMap) {
        offlineDownloadsMap.values.toList()
    }
    val isDeviceOnline by OfflineDownloadManager.observeNetworkOnline(context)
        .collectAsStateWithLifecycle(initialValue = OfflineDownloadManager.isNetworkAvailable(context))

    LaunchedEffect(isDeviceOnline) {
        if (!isDeviceOnline) {
            showSplash = false
            if (activeOverlay !is ActiveOverlay.Player) {
                activeOverlay = ActiveOverlay.OfflineDownloads
            }
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            runCatching { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
    }

    LaunchedEffect(catalogState.latestNotification?.id) {
        val notif = catalogState.latestNotification
        if (notif != null && notif.id.isNotBlank()) {
            if (lastShownNotificationId.isEmpty()) {
                lastShownNotificationId = notif.id
            } else if (notif.id != lastShownNotificationId) {
                lastShownNotificationId = notif.id
                FcmPushSender.showSystemNotification(
                    context = context,
                    title = notif.title,
                    body = notif.body,
                    imageUrl = notif.imageUrl
                )
            }
        }
    }

    LaunchedEffect(toastMsg) {
        val msg = toastMsg
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            vm.clearToast()
        }
    }

    val nativeSecurityInfo = remember(context) {
        NhNativeSecurityVault.getDeveloperInfo(context)
    }

    if (nativeSecurityInfo.isTampered) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color(0xFF0A0C10))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "SECURITY TAMPER DETECTED",
                    color = CrimsonRed,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = nativeSecurityInfo.tamperReason
                        ?: "Native library libnh_security.so was modified or removed. Unauthorized APK modification is blocked.",
                    color = TextSecondary,
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    if (showSplash) {
        SplashScreen(
            indianPosterUrls = indianPosterUrls,
            onFinished = { showSplash = false }
        )
        return
    }

    val updateCfg = catalogState.appUpdateConfig
    val isUpdateAvailable = updateCfg != null &&
        updateCfg.apkUrl.isNotBlank() &&
        (updateCfg.versionCode > BuildConfig.VERSION_CODE ||
            !updateCfg.versionName.equals(BuildConfig.VERSION_NAME, ignoreCase = true)) &&
        updateCfg.updatedAt != dismissedUpdateTimestamp

    if (isUpdateAvailable && updateCfg != null && activeOverlay !is ActiveOverlay.AdminPanel) {
        InAppUpdateDialog(
            updateConfig = updateCfg,
            onDismiss = { dismissedUpdateTimestamp = updateCfg.updatedAt }
        )
    }

    BackHandler(enabled = activeOverlay == ActiveOverlay.None && currentTab != UserRootTab.HOME) {
        currentTab = UserRootTab.HOME
    }

    val allPublished = remember(catalogState.movies, catalogState.series) {
        (catalogState.movies + catalogState.series)
            .filter { it.published }
            .sortedByDescending { it.updatedAt }
    }

    val publishedMoviesOnly = remember(catalogState.movies) {
        catalogState.movies.filter { it.published }.sortedByDescending { it.updatedAt }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = TextWhite,
                    actionColor = CrimsonLight
                )
            }
        },
        bottomBar = {
            if (activeOverlay == ActiveOverlay.None) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    UserRootTab.entries.forEach { tab ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CrimsonRed,
                                selectedTextColor = CrimsonRed,
                                indicatorColor = CrimsonRed.copy(alpha = 0.16f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val boxModifier = if (activeOverlay is ActiveOverlay.Player) {
            Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black)
        } else if (activeOverlay is ActiveOverlay.Details) {
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(bottom = innerPadding.calculateBottomPadding())
        } else {
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        }
        Box(modifier = boxModifier) {
            when (val overlay = activeOverlay) {
                is ActiveOverlay.AdminPanel -> {
                    AdminShellScreen(
                        vm = vm,
                        catalogState = catalogState,
                        authState = authState,
                        preferences = prefs,
                        cacheSizeBytes = cacheSizeBytes,
                        onExitAdminToUserApp = { activeOverlay = ActiveOverlay.None }
                    )
                }

                is ActiveOverlay.Player -> {
                    val similarForMovie = remember(overlay.movieItem, publishedMoviesOnly) {
                        val cur = overlay.movieItem
                        if (cur == null) publishedMoviesOnly.take(10)
                        else publishedMoviesOnly.filter { it.id != cur.id }.sortedByDescending { other ->
                            other.genres.count { g -> cur.genres.any { it.equals(g, ignoreCase = true) } }
                        }.take(10)
                    }

                    VideoPlayerScreen(
                        title = overlay.title,
                        streamUrl = overlay.streamUrl,
                        currentMovieItem = overlay.movieItem,
                        similarMovies = similarForMovie,
                        seriesItem = overlay.seriesItem,
                        currentSeasonNumber = overlay.seasonNum,
                        currentEpisodeNumber = overlay.episodeNum,
                        autoPlayNext = prefs.autoPlayNextEpisode,
                        onBack = {
                            val returnTarget = overlay.seriesItem ?: overlay.movieItem
                            activeOverlay = when {
                                !isDeviceOnline || overlay.streamUrl.startsWith(OfflineDownloadManager.OFFLINE_URI_PREFIX) -> {
                                    if (!isDeviceOnline) ActiveOverlay.OfflineDownloads
                                    else if (returnTarget != null) ActiveOverlay.Details(returnTarget)
                                    else ActiveOverlay.OfflineDownloads
                                }
                                returnTarget != null -> ActiveOverlay.Details(returnTarget)
                                else -> ActiveOverlay.None
                            }
                        },
                        onSwitchEpisode = { nextEp ->
                            val sTitle = overlay.seriesItem?.title ?: overlay.title
                            activeOverlay = ActiveOverlay.Player(
                                title = "$sTitle S${nextEp.seasonNumber} E${nextEp.episodeNumber}",
                                streamUrl = nextEp.primaryStreamUrl,
                                seriesItem = overlay.seriesItem,
                                seasonNum = nextEp.seasonNumber,
                                episodeNum = nextEp.episodeNumber
                            )
                        },
                        onSelectSimilarMovie = { simMovie ->
                            val canPlaySim = vm.unlockVipItemIfEligible(simMovie)
                            if (simMovie.primaryStreamUrl.isNotBlank() && canPlaySim) {
                                activeOverlay = ActiveOverlay.Player(
                                    title = simMovie.title,
                                    streamUrl = simMovie.primaryStreamUrl,
                                    movieItem = simMovie
                                )
                            } else {
                                activeOverlay = ActiveOverlay.Details(
                                    item = simMovie,
                                    previousItem = overlay.movieItem
                                )
                            }
                        }
                    )
                }

                is ActiveOverlay.Details -> {
                    val liveItem = allPublished.find { it.id == overlay.item.id } ?: overlay.item
                    DetailsScreen(
                        item = liveItem,
                        enrichedItem = enrichedItemMap[liveItem.id],
                        allPublishedItems = allPublished,
                        isInWatchlist = liveItem.id in prefs.watchlistIds,
                        userRecord = currentUserRecord,
                        vipConfig = catalogState.vipAccessConfig,
                        actorDetailsMap = actorDetailsMap,
                        onRequestCastEnrichment = vm::ensureCastImagesLoaded,
                        onRequestActorDetails = vm::loadActorProfileDetails,
                        onRedeemShareCode = { code -> vm.redeemShareCode(code) },
                        onBack = {
                            val prev = overlay.previousItem
                            activeOverlay = if (prev != null) {
                                ActiveOverlay.Details(prev)
                            } else {
                                ActiveOverlay.None
                            }
                        },
                        onToggleWatchlist = { vm.toggleWatchlist(liveItem.id) },
                        onPlayVideo = { pTitle, pUrl, sItem, sNum, eNum ->
                            vm.unlockVipItemIfEligible(liveItem)
                            activeOverlay = ActiveOverlay.Player(
                                title = pTitle,
                                streamUrl = pUrl,
                                movieItem = liveItem.takeIf { !it.isSeries },
                                seriesItem = sItem ?: liveItem.takeIf { it.isSeries },
                                seasonNum = sNum,
                                episodeNum = eNum
                            )
                        },
                        onOpenDetails = { sim ->
                            activeOverlay = ActiveOverlay.Details(
                                item = sim,
                                previousItem = liveItem
                            )
                        },
                        onOpenOfflineDownloads = {
                            activeOverlay = ActiveOverlay.OfflineDownloads
                        }
                    )
                }

                is ActiveOverlay.OfflineDownloads -> {
                    OfflineDownloadsScreen(
                        downloads = offlineDownloadsList,
                        isDeviceOnline = isDeviceOnline,
                        onPlayOfflineItem = { entry ->
                            val matchedItem = allPublished.find { it.id == entry.itemId }
                            activeOverlay = ActiveOverlay.Player(
                                title = entry.title,
                                streamUrl = entry.offlineStreamUri,
                                movieItem = matchedItem?.takeIf { !it.isSeries },
                                seriesItem = matchedItem?.takeIf { it.isSeries },
                                seasonNum = entry.seasonNumber,
                                episodeNum = entry.episodeNumber
                            )
                        },
                        onDeleteOfflineItem = { dlId ->
                            OfflineDownloadManager.cancelOrDeleteDownload(context, dlId)
                        },
                        onBrowseOnlineCatalog = {
                            activeOverlay = ActiveOverlay.None
                        }
                    )
                }

                is ActiveOverlay.Search -> {
                    SearchScreen(
                        allPublishedItems = allPublished,
                        onBack = { activeOverlay = ActiveOverlay.None },
                        onOpenDetails = { item -> activeOverlay = ActiveOverlay.Details(item) }
                    )
                }

                is ActiveOverlay.Notifications -> {
                    val allNotifs = remember(catalogState.notificationsHistory, catalogState.latestNotification) {
                        (catalogState.notificationsHistory + listOfNotNull(catalogState.latestNotification))
                            .distinctBy { it.id }
                            .sortedByDescending { it.sentAt }
                    }
                    NotificationsScreen(
                        notifications = allNotifs,
                        allPublishedItems = allPublished,
                        onOpenItemDetails = { item -> activeOverlay = ActiveOverlay.Details(item) },
                        onBack = { activeOverlay = ActiveOverlay.None }
                    )
                }

                ActiveOverlay.None -> {
                    when (currentTab) {
                        UserRootTab.HOME -> HomeScreen(
                            catalogState = catalogState,
                            watchlistIds = prefs.watchlistIds,
                            onOpenDetails = { item -> activeOverlay = ActiveOverlay.Details(item) },
                            onToggleWatchlist = vm::toggleWatchlist,
                            onOpenSearch = { activeOverlay = ActiveOverlay.Search },
                            onOpenMoviesTab = { currentTab = UserRootTab.MOVIES },
                            onOpenSeriesTab = { currentTab = UserRootTab.WEB_SERIES },
                            onOpenAdmin = { activeOverlay = ActiveOverlay.AdminPanel },
                            onOpenNotifications = { activeOverlay = ActiveOverlay.Notifications }
                        )

                        UserRootTab.MOVIES -> MoviesScreen(
                            movies = catalogState.movies,
                            categories = catalogState.categories,
                            onOpenDetails = { item -> activeOverlay = ActiveOverlay.Details(item) },
                            onOpenSearch = { activeOverlay = ActiveOverlay.Search }
                        )

                        UserRootTab.WEB_SERIES -> WebSeriesScreen(
                            seriesList = catalogState.series,
                            categories = catalogState.categories,
                            onOpenDetails = { item -> activeOverlay = ActiveOverlay.Details(item) },
                            onOpenSearch = { activeOverlay = ActiveOverlay.Search }
                        )

                        UserRootTab.MY_LIST -> WatchlistScreen(
                            allItems = allPublished,
                            watchlistIds = prefs.watchlistIds,
                            onOpenDetails = { item -> activeOverlay = ActiveOverlay.Details(item) },
                            onRemoveFromWatchlist = vm::removeFromWatchlist,
                            onClearAll = vm::clearWatchlist
                        )

                        UserRootTab.PROFILE -> UserSettingsScreen(
                            preferences = prefs,
                            userRecord = currentUserRecord,
                            vipConfig = catalogState.vipAccessConfig,
                            onSelectTheme = vm::setThemeVariant,
                            onSelectQuality = vm::setDefaultQuality,
                            onToggleAutoPlay = vm::setAutoPlayNextEpisode,
                            onToggleNotifications = vm::setNotificationsEnabled,
                            onRedeemShareCode = { code -> vm.redeemShareCode(code) },
                            onOpenOfflineDownloads = { activeOverlay = ActiveOverlay.OfflineDownloads }
                        )
                    }
                }
            }
        }
    }
}
