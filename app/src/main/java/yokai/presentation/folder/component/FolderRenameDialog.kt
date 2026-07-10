package yokai.presentation.folder.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import dev.icerock.moko.resources.compose.stringResource as mokoStringResource
import yokai.i18n.MR
import android.R as AR

@Composable
fun FolderNameDialog(
    title: String,
    initialName: String = "",
    confirmEnabled: (String) -> Boolean = { it.isNotBlank() && it != initialName },
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialName))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                label = { Text(text = mokoStringResource(MR.strings.folder_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = confirmEnabled(textFieldValue.text.trim()),
                onClick = { onConfirm(textFieldValue.text.trim()) },
            ) {
                Text(text = stringResource(AR.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(AR.string.cancel))
            }
        },
    )
}

@Composable
fun FolderRenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    FolderNameDialog(
        title = mokoStringResource(MR.strings.folder_name),
        initialName = initialName,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}
