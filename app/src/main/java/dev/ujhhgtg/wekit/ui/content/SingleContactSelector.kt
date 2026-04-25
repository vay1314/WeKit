package dev.ujhhgtg.wekit.ui.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Search
import dev.ujhhgtg.wekit.hooks.api.core.models.IWeContact

@Composable
fun SingleContactSelector(
    title: String,
    contacts: List<IWeContact>,
    initialSelectedWxId: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedWxId by remember { mutableStateOf(initialSelectedWxId) }

    val filteredContacts = remember(searchQuery, contacts, selectedWxId) {
        contacts
            .filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                        it.wxId.contains(searchQuery, ignoreCase = true)
            }
    }

    AlertDialogContent(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    placeholder = { Text("搜索昵称或微信号") },
                    leadingIcon = { Icon(MaterialSymbols.Outlined.Search, contentDescription = "Search") },
                    singleLine = true
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(
                        items = filteredContacts,
                        key = { it.wxId }
                    ) { contact ->
                        val isSelected = contact.wxId == selectedWxId

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .clickable {
                                    selectedWxId = contact.wxId
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = contact.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = contact.wxId,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedWxId!!) },
                enabled = selectedWxId != null
            ) {
                Text("确定")
            }
        }
    )
}
