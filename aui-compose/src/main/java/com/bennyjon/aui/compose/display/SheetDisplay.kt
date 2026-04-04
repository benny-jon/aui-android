package com.bennyjon.aui.compose.display

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.bennyjon.aui.compose.internal.BlockRenderer
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiFeedback
import kotlinx.coroutines.launch

/**
 * Renders content blocks inside a [ModalBottomSheet].
 *
 * The sheet opens automatically and closes when the user triggers feedback (e.g., taps a
 * submit button) or, if [sheetDismissable] is true, when they drag it down.
 *
 * @param blocks The content blocks to render inside the sheet.
 * @param sheetTitle Optional title shown at the top of the sheet.
 * @param sheetDismissable Whether drag-to-dismiss is enabled. When false, the sheet can only
 *   be closed by submitting feedback.
 * @param onFeedback Called with feedback when the user interacts with a block inside the sheet.
 *   The sheet auto-dismisses before invoking this callback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SheetDisplay(
    blocks: List<AuiBlock>,
    sheetTitle: String?,
    sheetDismissable: Boolean,
    onFeedback: (AuiFeedback) -> Unit,
) {
    val theme = LocalAuiTheme.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { value ->
            sheetDismissable || value != SheetValue.Hidden
        },
    )
    var showSheet by remember { mutableStateOf(true) }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                if (sheetDismissable) {
                    showSheet = false
                }
            },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = theme.spacing.medium),
            ) {
                if (sheetTitle != null) {
                    Text(
                        text = sheetTitle,
                        style = theme.typography.heading.copy(fontWeight = FontWeight.SemiBold),
                        color = theme.colors.onSurface,
                    )
                    Spacer(modifier = Modifier.height(theme.spacing.medium))
                }

                BlockRenderer(
                    blocks = blocks,
                    onFeedback = { feedback ->
                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            showSheet = false
                            onFeedback(feedback)
                        }
                    },
                )

                Spacer(modifier = Modifier.height(theme.spacing.large))
            }
        }
    }
}
