package org.mayad.fame_life.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.common_keep
import famelife.shared.generated.resources.common_sign_out
import famelife.shared.generated.resources.settings_confirm_google
import famelife.shared.generated.resources.settings_delete_body
import famelife.shared.generated.resources.settings_delete_title
import famelife.shared.generated.resources.settings_deleting
import famelife.shared.generated.resources.settings_sign_out_body
import famelife.shared.generated.resources.settings_sign_out_title
import famelife.shared.generated.resources.settings_stay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.ui.messageRes


/** Tier 1: connects settings to its ViewModel and shows the sign-out and delete confirmations. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onPaywall: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsContent(
        state = state,
        onBack = onBack,
        onEdit = onEditProfile,
        onPlan = onPaywall,
        onSignOut = { viewModel.onIntent(SettingsIntent.AskSignOut) },
        onDelete = { viewModel.onIntent(SettingsIntent.AskDelete) },
        onToggleSimulation = { viewModel.onIntent(SettingsIntent.ToggleSimulation) },
        onToggleData = { viewModel.onIntent(SettingsIntent.ToggleData) },
    )
    when (state.dialog) {
        SettingsDialog.NONE -> Unit
        SettingsDialog.SIGN_OUT -> ConfirmDialog(
            title = stringResource(Res.string.settings_sign_out_title),
            body = stringResource(Res.string.settings_sign_out_body),
            confirm = stringResource(Res.string.common_sign_out),
            dismiss = stringResource(Res.string.settings_stay),
            onConfirm = { viewModel.onIntent(SettingsIntent.ConfirmSignOut) },
            onDismiss = { viewModel.onIntent(SettingsIntent.Dismiss) },
        )
        SettingsDialog.DELETE -> DeleteAccountDialog(
            busy = state.deleting,
            failure = state.deleteError?.messageRes,
            onConfirm = { viewModel.onIntent(SettingsIntent.ConfirmDelete) },
            onDismiss = { viewModel.onIntent(SettingsIntent.Dismiss) },
        )
    }
}

/** Account deletion re-confirms the Google account: Firebase only deletes accounts with a recent sign-in. */
@Composable
internal fun DeleteAccountDialog(busy: Boolean, failure: StringResource?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = Pulse.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceElevated,
        title = { Text(stringResource(Res.string.settings_delete_title), style = Pulse.type.title, color = colors.textPrimary) },
        text = {
            Column {
                Text(stringResource(Res.string.settings_delete_body), style = Pulse.type.body, color = colors.textSecondary)
                failure?.let {
                    Spacer(Modifier.height(Pulse.dimens.md))
                    Text(stringResource(it), style = Pulse.type.bodySmall, color = colors.error)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = onConfirm) {
                Text(stringResource(if (busy) Res.string.settings_deleting else Res.string.settings_confirm_google), style = Pulse.type.label, color = colors.error)
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(Res.string.common_keep), style = Pulse.type.label, color = colors.textPrimary) }
        },
    )
}

@Composable
internal fun ConfirmDialog(title: String, body: String, confirm: String, dismiss: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = Pulse.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceElevated,
        title = { Text(title, style = Pulse.type.title, color = colors.textPrimary) },
        text = { Text(body, style = Pulse.type.body, color = colors.textSecondary) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, style = Pulse.type.label, color = colors.textPrimary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismiss, style = Pulse.type.label, color = colors.textSecondary) } },
    )
}
