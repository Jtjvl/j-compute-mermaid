package com.jcompute.mermaidpocket.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jcompute.mermaidpocket.data.DurableDocuments
import com.jcompute.mermaidpocket.export.ExportDelivery
import com.jcompute.mermaidpocket.export.ExportFormat
import com.jcompute.mermaidpocket.export.ExportOptionOrder
import com.jcompute.mermaidpocket.export.ExportSize

/**
 * Formats and raster options for the single Save or Share action.
 * PNG and JPEG include the width slider and a size estimate.
 * Save of `.mmd` writes the library; "Save a copy…" is the SAF export.
 */
@Composable
fun ExportOptionsDialog(
    delivery: ExportDelivery,
    initialWidth: Int,
    sourceWidth: Int,
    sourceHeight: Int,
    onConfirm: (ExportFormat, Int?) -> Unit,
    onSaveCopy: (ExportFormat) -> Unit,
    onDismiss: () -> Unit,
) {
    var format by remember { mutableStateOf(ExportOptionOrder.first()) }
    var width by remember(initialWidth) {
        mutableIntStateOf(ExportSize.clampWidth(initialWidth))
    }
    var field by remember(initialWidth) { mutableStateOf(width.toString()) }
    val active = field.toIntOrNull()?.takeIf { it in ExportSize.MIN_WIDTH..ExportSize.MAX_WIDTH } ?: width
    val (outW, outH) = ExportSize.outputSize(active, sourceWidth, sourceHeight)
    val estimate = ExportSize.estimateBytes(outW, outH, format)
    val estimateLabel = estimate?.let(ExportSize::approxLabel) ?: ""
    val sharing = delivery == ExportDelivery.Share

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (sharing) "Share" else "Save") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    if (sharing) {
                        "Choose a format for the share sheet."
                    } else {
                        "Choose a format. PNG, JPEG, and SVG open the system picker. .mmd stays in ${DurableDocuments.DISPLAY_PATH}."
                    },
                )
                ExportOptionOrder.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { format = option },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = format == option,
                            onClick = { format = option },
                        )
                        Text(option.menuLabel)
                    }
                }
                if (format.sized) {
                    Text(
                        text = "$outW × $outH",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    if (estimateLabel.isNotEmpty()) {
                        Text(estimateLabel, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                    }
                    Slider(
                        value = width.toFloat(),
                        onValueChange = { next ->
                            width = next.toInt().coerceIn(ExportSize.MIN_WIDTH, ExportSize.MAX_WIDTH)
                            field = width.toString()
                        },
                        valueRange = ExportSize.MIN_WIDTH.toFloat()..ExportSize.MAX_WIDTH.toFloat(),
                    )
                    OutlinedTextField(
                        value = field,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(5)
                            field = digits
                            val parsed = digits.toIntOrNull()
                            if (parsed != null && parsed in ExportSize.MIN_WIDTH..ExportSize.MAX_WIDTH) {
                                width = parsed
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Pixels") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = {
                            Text("${ExportSize.MIN_WIDTH}–${ExportSize.MAX_WIDTH}. Default ${ExportSize.DEFAULT_WIDTH}.")
                        },
                    )
                }
            }
        },
        confirmButton = {
            Row {
                if (!sharing && format == ExportFormat.Mmd) {
                    TextButton(
                        onClick = { onSaveCopy(format) },
                    ) { Text("Save a copy…") }
                }
                TextButton(
                    onClick = {
                        val chosen = if (format.sized) ExportSize.parseWidth(field) ?: width else null
                        onConfirm(format, chosen)
                    },
                ) { Text(if (sharing) "Share" else "Save") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
