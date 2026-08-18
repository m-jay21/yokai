package yokai.presentation.folder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import com.bluelinelabs.conductor.Router
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.ui.library.CollectionLibraryController
import eu.kanade.tachiyomi.util.compose.LocalBackPress
import eu.kanade.tachiyomi.util.compose.LocalRouter
import eu.kanade.tachiyomi.util.compose.currentOrThrow as localCurrentOrThrow
import eu.kanade.tachiyomi.util.isTablet
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import eu.kanade.tachiyomi.widget.EmptyView
import yokai.i18n.MR
import yokai.presentation.AppBarType
import yokai.presentation.YokaiScaffold
import yokai.presentation.component.EmptyScreen
import yokai.presentation.core.enterAlwaysCollapsedAppBarScrollBehavior
import yokai.presentation.folder.component.CollectionItemRow
import yokai.presentation.folder.component.FolderItemRow
import yokai.presentation.folder.component.FolderNameDialog
import yokai.presentation.folder.component.FolderRenameDialog
import yokai.util.Screen

class FolderScreen : Screen() {

    @Composable
    override fun Content() {
        val onBackPress = LocalBackPress.localCurrentOrThrow
        val router: Router = LocalRouter.localCurrentOrThrow
        val folderModel = rememberScreenModel { FolderScreenModel() }
        val collectionModel = rememberScreenModel { CollectionScreenModel() }
        val folderState by folderModel.state.collectAsState()
        val collectionState by collectionModel.state.collectAsState()
        val listState = rememberLazyListState()
        val textFieldState = rememberTextFieldState()
        val query = textFieldState.text.toString()
        var selectedTab by rememberSaveable { mutableIntStateOf(0) }
        var folderToRename by remember { mutableStateOf<FolderScreenModel.FolderItem?>(null) }
        var collectionToRename by remember { mutableStateOf<CollectionScreenModel.CollectionItem?>(null) }
        var showCreateDialog by remember { mutableStateOf(false) }
        val showingCollections = selectedTab == 1

        folderToRename?.let { item ->
            FolderRenameDialog(
                initialName = item.folder.name,
                onDismiss = { folderToRename = null },
                onConfirm = { newName ->
                    folderModel.renameFolder(item.folder.id!!.toLong(), newName)
                    folderToRename = null
                },
            )
        }

        collectionToRename?.let { item ->
            FolderNameDialog(
                title = stringResource(MR.strings.collection_name),
                initialName = item.collection.name,
                label = stringResource(MR.strings.collection_name),
                onDismiss = { collectionToRename = null },
                onConfirm = { newName ->
                    collectionModel.renameCollection(item.collection.id!!.toLong(), newName)
                    collectionToRename = null
                },
            )
        }

        if (showCreateDialog) {
            FolderNameDialog(
                title = stringResource(
                    if (showingCollections) MR.strings.new_collection else MR.strings.new_folder,
                ),
                label = stringResource(
                    if (showingCollections) MR.strings.collection_name else MR.strings.folder_name,
                ),
                confirmEnabled = { it.isNotBlank() },
                onDismiss = { showCreateDialog = false },
                onConfirm = { name ->
                    if (showingCollections) {
                        collectionModel.createCollection(name)
                    } else {
                        folderModel.createFolder(name)
                    }
                    showCreateDialog = false
                },
            )
        }

        YokaiScaffold(
            onNavigationIconClicked = onBackPress,
            title = stringResource(
                if (showingCollections) MR.strings.collections else MR.strings.folders,
            ),
            appBarType = AppBarType.LARGE,
            textFieldState = textFieldState,
            scrollBehavior = enterAlwaysCollapsedAppBarScrollBehavior(
                canScroll = { listState.canScrollForward || listState.canScrollBackward },
                isAtTop = { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 },
            ),
            fab = {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ) {
                    Icon(
                        imageVector = if (showingCollections) {
                            Icons.Filled.LibraryAdd
                        } else {
                            Icons.Filled.CreateNewFolder
                        },
                        contentDescription = stringResource(
                            if (showingCollections) MR.strings.new_collection else MR.strings.new_folder,
                        ),
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(MR.strings.folders)) },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(MR.strings.collections)) },
                    )
                }

                if (showingCollections) {
                    CollectionTab(
                        state = collectionState,
                        query = query,
                        listState = listState,
                        onOpen = { id ->
                            router.pushController(
                                CollectionLibraryController(id).withFadeTransaction(),
                            )
                        },
                        onRename = { collectionToRename = it },
                        onDelete = { collectionModel.deleteCollection(it) },
                        onCreate = { showCreateDialog = true },
                    )
                } else {
                    FolderTab(
                        state = folderState,
                        query = query,
                        listState = listState,
                        onOpen = { id ->
                            router.pushController(
                                FolderDetailsController(id).withFadeTransaction(),
                            )
                        },
                        onRename = { folderToRename = it },
                        onDelete = { folderModel.deleteFolder(it) },
                        onCreate = { showCreateDialog = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderTab(
    state: FolderScreenModel.State,
    query: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onOpen: (Long) -> Unit,
    onRename: (FolderScreenModel.FolderItem) -> Unit,
    onDelete: (Long) -> Unit,
    onCreate: () -> Unit,
) {
    if (state is FolderScreenModel.State.Loading) return

    val success = state as FolderScreenModel.State.Success
    val filtered = remember(success.folders, query) {
        if (query.isBlank()) {
            success.folders
        } else {
            success.folders.filter { it.folder.name.contains(query, ignoreCase = true) }
        }
    }

    if (success.isEmpty) {
        EmptyScreen(
            image = Icons.Outlined.FolderOpen,
            message = stringResource(MR.strings.information_empty_folders),
            isTablet = isTablet(),
            actions = listOf(EmptyView.Action(MR.strings.new_folder) { onCreate() }),
        )
        return
    }

    if (filtered.isEmpty()) {
        EmptyScreen(
            image = Icons.Outlined.FolderOpen,
            message = stringResource(MR.strings.no_results_found),
            isTablet = isTablet(),
        )
        return
    }

    LazyColumn(state = listState) {
        items(
            count = filtered.size,
            key = { index -> filtered[index].folder.id ?: index },
        ) { index ->
            val item = filtered[index]
            FolderItemRow(
                name = item.folder.name,
                chapterCount = item.chapterCount,
                onClick = { onOpen(item.folder.id!!.toLong()) },
                onRenameClick = { onRename(item) },
                onDeleteClick = { item.folder.id?.let { onDelete(it.toLong()) } },
            )
        }
    }
}

@Composable
private fun CollectionTab(
    state: CollectionScreenModel.State,
    query: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onOpen: (Long) -> Unit,
    onRename: (CollectionScreenModel.CollectionItem) -> Unit,
    onDelete: (Long) -> Unit,
    onCreate: () -> Unit,
) {
    if (state is CollectionScreenModel.State.Loading) return

    val success = state as CollectionScreenModel.State.Success
    val filtered = remember(success.collections, query) {
        if (query.isBlank()) {
            success.collections
        } else {
            success.collections.filter { it.collection.name.contains(query, ignoreCase = true) }
        }
    }

    if (success.isEmpty) {
        EmptyScreen(
            image = Icons.Outlined.CollectionsBookmark,
            message = stringResource(MR.strings.information_empty_collections),
            isTablet = isTablet(),
            actions = listOf(EmptyView.Action(MR.strings.new_collection) { onCreate() }),
        )
        return
    }

    if (filtered.isEmpty()) {
        EmptyScreen(
            image = Icons.Outlined.CollectionsBookmark,
            message = stringResource(MR.strings.no_results_found),
            isTablet = isTablet(),
        )
        return
    }

    LazyColumn(state = listState) {
        items(
            count = filtered.size,
            key = { index -> filtered[index].collection.id ?: index },
        ) { index ->
            val item = filtered[index]
            CollectionItemRow(
                name = item.collection.name,
                seriesCount = item.seriesCount,
                onClick = { onOpen(item.collection.id!!.toLong()) },
                onRenameClick = { onRename(item) },
                onDeleteClick = { item.collection.id?.let { onDelete(it.toLong()) } },
            )
        }
    }
}
