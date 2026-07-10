package yokai.presentation.folder.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.compose.stringResource
import yokai.i18n.MR

@Composable
fun FolderEditDialog(
    initialName: String,
    initialDescription: String?,
    initialAuthor: String?,
    initialArtist: String?,
    initialGenre: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, author: String, artist: String, genre: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var description by rememberSaveable { mutableStateOf(initialDescription.orEmpty()) }
    var author by rememberSaveable { mutableStateOf(initialAuthor.orEmpty()) }
    var artist by rememberSaveable { mutableStateOf(initialArtist.orEmpty()) }
    var genre by rememberSaveable { mutableStateOf(initialGenre.orEmpty()) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(MR.strings.edit_folder_details)) },
        text = {
            Column(
                modifier = androidx.compose.ui.Modifier.verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(MR.strings.title)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(MR.strings.author)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text(stringResource(MR.strings.artist)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text(stringResource(MR.strings.genres)) },
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(MR.strings.description)) },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description, author, artist, genre) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(MR.strings.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(MR.strings.cancel))
            }
        },
    )
}
