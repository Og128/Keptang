package com.keptang.ui.quickadd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.keptang.R
import com.keptang.data.repository.AppSettings
import com.keptang.di.ServiceLocator
import com.keptang.ui.theme.KeptangTheme
import kotlinx.coroutines.launch

/**
 * A dialog-styled activity (see `Theme.Keptang.Transparent`) launched by [com.keptang.widget.QuickAddWidgetProvider].
 * Parses the typed text exactly like a voice transcript (e.g. "50 coffee" -> amount 50, category
 * Coffee) via the same [com.keptang.capture.CaptureProcessor] pipeline, then closes itself.
 */
class QuickAddActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by ServiceLocator.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            KeptangTheme(colorTheme = settings.colorTheme) {
                QuickAddScreen(
                    onDismiss = { finish() },
                    onSubmit = { text ->
                        lifecycleScope.launch {
                            val captureId = ServiceLocator.captureRepository.createManualEntry(settings.timeZoneId)
                            ServiceLocator.captureProcessor.reprocessEditedTranscript(captureId, text)
                            finish()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickAddScreen(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun submit() {
        if (text.isNotBlank() && !isSubmitting) {
            isSubmitting = true
            onSubmit(text)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(stringResource(R.string.quick_add_title), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.quick_add_placeholder)) },
                    singleLine = true,
                    enabled = !isSubmitting,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .focusRequester(focusRequester)
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = ::submit,
                        enabled = text.isNotBlank() && !isSubmitting,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text(stringResource(R.string.action_save))
                        }
                    }
                }
            }
        }
    }
}
