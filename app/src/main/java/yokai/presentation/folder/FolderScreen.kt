package yokai.presentation.folder

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import com.bluelinelabs.conductor.Router
import dev.icerock.moko.resources.compose.stringResource
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
import yokai.presentation.folder.component.FolderItemRow
import yokai.presentation.folder.component.FolderNameDialog
import yokai.presentation.folder.component.FolderRenameDialog
import yokai.util.Screen

class FolderScreen : Screen() {

    @Composable
    override fun Content() {
        val onBackPress = LocalBackPress.localCurrentOrThrow
        val router: Router = LocalRouter.localCurrentOrThrow
        val screenModel = rememberScreenModel { FolderScreenModel() }
        val state by screenModel.state.collectAsState()
        val listState = rememberLazyListState()
        val textFieldState = rememberTextFieldState()
        val query = textFieldState.text.toString()
        var folderToRename by remember { mutableStateOf<FolderScreenModel.FolderItem?>(null) }
        var showCreateDialog by remember { mutableStateOf(false) }

        folderToRename?.let { item ->
            FolderRenameDialog(
                initialName = item.folder.name,
                onDismiss = { folderToRename = null },
                onConfirm = { newName ->
                    screenModel.renameFolder(item.folder.id!!.toLong(), newName)
                    folderToRename = null
                },
            )
        }

        if (showCreateDialog) {
            FolderNameDialog(
                title = stringResource(MR.strings.new_folder),
                confirmEnabled = { it.isNotBlank() },
                onDismiss = { showCreateDialog = false },
                onConfirm = { name ->
                    screenModel.createFolder(name)
                    showCreateDialog = false
                },
            )
        }

        YokaiScaffold(
            onNavigationIconClicked = onBackPress,
            title = stringResource(MR.strings.folders),
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
                        imageVector = Icons.Filled.CreateNewFolder,
                        contentDescription = stringResource(MR.strings.new_folder),
                    )
                }
            },
        ) { innerPadding ->
            if (state is FolderScreenModel.State.Loading) return@YokaiScaffold

            val success = state as FolderScreenModel.State.Success
            val filteredFolders = remember(success.folders, query) {
                if (query.isBlank()) {
                    success.folders
                } else {
                    success.folders.filter { it.folder.name.contains(query, ignoreCase = true) }
                }
            }

            if (success.isEmpty) {
                EmptyScreen(
                    modifier = Modifier.padding(innerPadding),
                    image = Icons.Outlined.FolderOpen,
                    message = stringResource(MR.strings.information_empty_folders),
                    isTablet = isTablet(),
                    actions = listOf(
                        EmptyView.Action(MR.strings.new_folder) { showCreateDialog = true },
                    ),
                )
                return@YokaiScaffold
            }

            if (filteredFolders.isEmpty()) {
                EmptyScreen(
                    modifier = Modifier.padding(innerPadding),
                    image = Icons.Outlined.FolderOpen,
                    message = stringResource(MR.strings.no_results_found),
                    isTablet = isTablet(),
                )
                return@YokaiScaffold
            }

            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                state = listState,
            ) {
                items(
                    count = filteredFolders.size,
                    key = { index -> filteredFolders[index].folder.id ?: index },
                ) { index ->
                    val item = filteredFolders[index]
                    FolderItemRow(
                        name = item.folder.name,
                        chapterCount = item.chapterCount,
                        onClick = {
                            router.pushController(
                                FolderDetailsController(item.folder.id!!.toLong()).withFadeTransaction(),
                            )
                        },
                        onRenameClick = { folderToRename = item },
                        onDeleteClick = { item.folder.id?.let { screenModel.deleteFolder(it.toLong()) } },
                    )
                }
            }
        }
    }
}
