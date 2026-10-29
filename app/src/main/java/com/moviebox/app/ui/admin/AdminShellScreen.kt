package com.moviebox.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviebox.app.data.model.*
import com.moviebox.app.data.repository.AdminAuthState
import com.moviebox.app.data.repository.CatalogState
import com.moviebox.app.ui.MovieBoxViewModel
import com.moviebox.app.ui.components.MovieBoxLogo
import com.moviebox.app.ui.theme.CrimsonLight
import com.moviebox.app.ui.theme.CrimsonRed
import com.moviebox.app.ui.theme.StatusAmber
import com.moviebox.app.ui.theme.StatusGreen
import com.moviebox.app.ui.theme.TextSecondary
import com.moviebox.app.ui.theme.TextWhite
import kotlinx.coroutines.launch

enum class AdminSection(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard),
    CATEGORIES("Categories", Icons.Filled.Category),
    MOVIES("Movies", Icons.Filled.Movie),
    WEB_SERIES("Web Series", Icons.Filled.LiveTv),
    TMDB_IMPORT("Import TMDB", Icons.Filled.CloudDownload),
    PUSH_NOTIFICATION("Push Notification", Icons.Filled.NotificationsActive),
    APP_UPDATE("In-App Update", Icons.Filled.SystemUpdate),
    USER_MANAGEMENT("User Management", Icons.Filled.People),
    SETTINGS("Settings", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminShellScreen(
    vm: MovieBoxViewModel,
    catalogState: CatalogState,
    authState: AdminAuthState,
    preferences: AppPreferences,
    cacheSizeBytes: Long,
    onExitAdminToUserApp: () -> Unit
) {
    val tmdbState by vm.tmdbState.collectAsState()
    val enrichedItemMap by vm.enrichedItemMap.collectAsState()
    val serviceAccountCreds by vm.serviceAccountCreds.collectAsState()
    val serviceAccountFileName by vm.serviceAccountFileName.collectAsState()
    val isSendingPush by vm.isSendingPush.collectAsState()

    if (authState !is AdminAuthState.VerifiedAdmin) {
        AdminLoginScreen(
            authState = authState,
            onSignIn = vm::signInAdmin,
            onSignOut = vm::signOutAdmin,
            onBackToUserApp = onExitAdminToUserApp
        )
        return
    }

    var currentSection by rememberSaveable { mutableStateOf(AdminSection.DASHBOARD) }
    var pendingEditMovie by remember { mutableStateOf<CatalogItem?>(null) }
    var openMovieEditorOnLaunch by remember { mutableStateOf(false) }
    var pendingEditSeries by remember { mutableStateOf<CatalogItem?>(null) }
    var openSeriesEditorOnLaunch by remember { mutableStateOf(false) }
    var pushTargetItem by remember { mutableStateOf<CatalogItem?>(null) }

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 720
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            currentSection != AdminSection.DASHBOARD -> currentSection = AdminSection.DASHBOARD
            else -> onExitAdminToUserApp()
        }
    }

    val sidebarContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .width(235.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MovieBoxLogo(subtitle = "Admin Panel")
                }

                Spacer(modifier = Modifier.height(6.dp))

                AdminSection.entries.forEach { item ->
                    val selected = currentSection == item
                    Surface(
                        color = if (selected) CrimsonRed else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                pendingEditMovie = null
                                openMovieEditorOnLaunch = false
                                pendingEditSeries = null
                                openSeriesEditorOnLaunch = false
                                currentSection = item
                                scope.launch { drawerState.close() }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (selected) Color.White else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = item.label,
                                color = if (selected) Color.White else TextSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { vm.signOutAdmin() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Logout",
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }

                TextButton(
                    onClick = onExitAdminToUserApp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Switch to User App", fontSize = 12.sp)
                }
            }

            // Bottom-left User Presence Summary Card
            val totalUsers = catalogState.users.size
            val onlineUsers = catalogState.users.count { it.online }
            val offlineUsers = (totalUsers - onlineUsers).coerceAtLeast(0)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SidebarStatLine(label = "Total Users", value = totalUsers.toString(), dotColor = CrimsonRed)
                    SidebarStatLine(label = "Online", value = onlineUsers.toString(), dotColor = StatusGreen)
                    SidebarStatLine(label = "Offline", value = offlineUsers.toString(), dotColor = StatusAmber)
                }
            }
        }
    }

    val defaultVipPts = catalogState.vipAccessConfig.vipPointsToAccess

    val mainBody: @Composable () -> Unit = {
        when (currentSection) {
            AdminSection.DASHBOARD -> AdminDashboardScreen(
                catalogState = catalogState,
                onAddMovie = {
                    pendingEditMovie = null
                    openMovieEditorOnLaunch = true
                    currentSection = AdminSection.MOVIES
                },
                onAddSeries = {
                    pendingEditSeries = null
                    openSeriesEditorOnLaunch = true
                    currentSection = AdminSection.WEB_SERIES
                },
                onOpenTmdbImport = {
                    currentSection = AdminSection.TMDB_IMPORT
                },
                onEditItem = { item ->
                    if (item.isSeries) {
                        pendingEditSeries = item
                        openSeriesEditorOnLaunch = true
                        currentSection = AdminSection.WEB_SERIES
                    } else {
                        pendingEditMovie = item
                        openMovieEditorOnLaunch = true
                        currentSection = AdminSection.MOVIES
                    }
                },
                onSendPushForItem = { item ->
                    pushTargetItem = item
                    currentSection = AdminSection.PUSH_NOTIFICATION
                },
                onTogglePublish = vm::togglePublish,
                onDeleteItem = { item -> vm.deleteCatalogItem(item) },
                onSeedDemoCatalog = vm::seedOpenDemoCatalog
            )

            AdminSection.CATEGORIES -> AdminCategoriesScreen(
                categories = catalogState.categories,
                countUsage = vm::countItemsUsingCategory,
                onSaveCategory = { cat -> vm.saveCategory(cat) },
                onDeleteCategory = vm::deleteCategory
            )

            AdminSection.MOVIES -> AdminMoviesScreen(
                movies = catalogState.movies,
                categories = catalogState.categories,
                defaultVipPoints = defaultVipPts,
                initialEditingMovie = pendingEditMovie,
                openEditorInitially = openMovieEditorOnLaunch,
                onQuickFetchTmdb = { idOrTitle, onResult ->
                    vm.fetchTmdbQuickFill(idOrTitle, isSeries = false, onResult = onResult)
                },
                onSaveMovie = { item ->
                    vm.saveCatalogItem(item)
                    pendingEditMovie = null
                    openMovieEditorOnLaunch = false
                },
                onDeleteMovie = { item -> vm.deleteCatalogItem(item) },
                onSendPushForMovie = { movie ->
                    pushTargetItem = movie
                    currentSection = AdminSection.PUSH_NOTIFICATION
                },
                onNavigateToTmdbImport = { currentSection = AdminSection.TMDB_IMPORT }
            )

            AdminSection.WEB_SERIES -> AdminWebSeriesScreen(
                webSeries = catalogState.series,
                categories = catalogState.categories,
                defaultVipPoints = defaultVipPts,
                initialEditingSeries = pendingEditSeries,
                openEditorInitially = openSeriesEditorOnLaunch,
                onSaveSeries = { item ->
                    vm.saveCatalogItem(item)
                    pendingEditSeries = null
                    openSeriesEditorOnLaunch = false
                },
                onDeleteSeries = { item -> vm.deleteCatalogItem(item) },
                onSendPushForSeries = { series ->
                    pushTargetItem = series
                    currentSection = AdminSection.PUSH_NOTIFICATION
                },
                onNavigateToTmdbImport = { currentSection = AdminSection.TMDB_IMPORT }
            )

            AdminSection.TMDB_IMPORT -> AdminTmdbImportScreen(
                isTmdbConfigured = vm.hasTmdbKeyConfigured,
                tmdbState = tmdbState,
                categories = catalogState.categories,
                defaultVipPoints = defaultVipPts,
                onSearch = vm::searchTmdb,
                onSelectPreview = vm::selectTmdbPreview,
                findExistingDuplicate = vm::findExistingByTmdbId,
                onCommitImport = { preview, selection, manualVideoUrl, selectedCategoryIds, existing ->
                    vm.applySelectionAndSaveTmdb(
                        preview = preview,
                        selection = selection,
                        manualVideoUrl = manualVideoUrl,
                        selectedCategoryIds = selectedCategoryIds,
                        existingToOverwrite = existing
                    )
                },
                onConfigureApiKey = vm::saveTmdbApiKeyOverride
            )

            AdminSection.PUSH_NOTIFICATION -> AdminPushNotificationScreen(
                targetItem = pushTargetItem,
                enrichedItem = pushTargetItem?.let { enrichedItemMap[it.id] },
                allCatalogItems = catalogState.movies + catalogState.series,
                users = catalogState.users,
                serviceAccountCreds = serviceAccountCreds,
                serviceAccountFileName = serviceAccountFileName,
                isSending = isSendingPush,
                onSelectTargetItem = { pushTargetItem = it },
                onRequestTmdbBackdrops = vm::ensureCastImagesLoaded,
                onServiceAccountLoaded = vm::setServiceAccountCredentials,
                onFetchAllUsers = vm::refreshUsersForPush,
                onSendPushNotification = { creds, item, title, body, img ->
                    vm.sendPushNotificationToAllUsers(creds, item, title, body, img)
                },
                onBack = { currentSection = AdminSection.DASHBOARD }
            )

            AdminSection.APP_UPDATE -> AdminAppUpdateScreen(
                currentConfig = catalogState.appUpdateConfig,
                onSaveAndSendUpdate = vm::saveAndPublishAppUpdate
            )

            AdminSection.USER_MANAGEMENT -> AdminUsersScreen(
                users = catalogState.users,
                vipConfig = catalogState.vipAccessConfig,
                onSaveVipConfig = vm::saveVipAccessConfig,
                onUpdateUser = { u, pts, shares, isVip ->
                    vm.adminUpdateUserRecord(u, pts, shares, isVip)
                }
            )

            AdminSection.SETTINGS -> AdminSettingsScreen(
                preferences = preferences,
                cacheSizeBytes = cacheSizeBytes,
                isFirebaseConnected = catalogState.isConnectedToFirebase,
                isTmdbConfigured = vm.hasTmdbKeyConfigured,
                adminEmail = authState.email,
                onSetThemeVariant = vm::setThemeVariant,
                onSetDefaultQuality = vm::setDefaultQuality,
                onSetAutoPlayNext = vm::setAutoPlayNextEpisode,
                onSetNotifications = vm::setNotificationsEnabled,
                onSetTmdbApiKey = vm::saveTmdbApiKeyOverride,
                onClearCache = vm::clearCache,
                onSignOut = vm::signOutAdmin
            )
        }
    }

    if (isWideScreen) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            sidebarContent()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                mainBody()
            }
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) {
                    sidebarContent()
                }
            }
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        title = { MovieBoxLogo(subtitle = "Admin Panel") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Open Admin Menu", tint = TextWhite)
                            }
                        },
                        actions = {
                            TextButton(onClick = onExitAdminToUserApp) {
                                Text("User App", color = CrimsonLight, fontSize = 12.sp)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    mainBody()
                }
            }
        }
    }
}

@Composable
private fun SidebarStatLine(
    label: String,
    value: String,
    dotColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, color = TextSecondary, fontSize = 11.sp)
        }
        Text(text = value, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
