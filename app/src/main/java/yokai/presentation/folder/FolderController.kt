package yokai.presentation.folder

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.CrossfadeTransition
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController

class FolderController : BaseComposeController() {

    @Composable
    override fun ScreenContent() {
        Navigator(
            screen = FolderScreen(),
            content = {
                CrossfadeTransition(navigator = it)
            },
        )
    }
}
