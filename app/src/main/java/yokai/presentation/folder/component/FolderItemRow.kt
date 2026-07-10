package yokai.presentation.folder.component

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.icerock.moko.resources.compose.pluralStringResource
import eu.kanade.tachiyomi.util.compose.textHint
import yokai.i18n.MR
import yokai.presentation.component.Gap
import yokai.presentation.theme.Size

@Composable
fun FolderItemRow(
    name: String,
    chapterCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.padding(horizontal = 8.dp),
            imageVector = Icons.Outlined.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
        )
        Column(
            modifier = Modifier
                .weight(1.0f)
                .clickable(onClick = onClick),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(),
                text = name,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
            )
            Gap(Size.tiny)
            Text(
                text = pluralStringResource(MR.plurals.chapters_plural, chapterCount, chapterCount.toString()),
                color = MaterialTheme.colorScheme.textHint,
                fontSize = 14.sp,
            )
        }
        IconButton(onClick = onRenameClick) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
