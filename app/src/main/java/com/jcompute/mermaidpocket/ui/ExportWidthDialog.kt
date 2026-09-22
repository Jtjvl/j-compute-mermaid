package com.jcompute.mermaidpocket.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jcompute.mermaidpocket.export.ExportFormat
import com.jcompute.mermaidpocket.export.ExportSize

@Composable
fun ExportWidthDialog(
    format: ExportFormat,
    initialWidth: Int,
    sourceWidth: Int,
    sourceHeight: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var width by remember(initialWidth) {
        mutableIntStateOf(ExportSize.clampWidth(initialWidth))
    }
    var field by remember(initialWidth) { mutableStateOf(width.toString()) }
    val active = field.toIntOrNull()?.takeIf { it in ExportSize.MIN_WIDTH..ExportSize.MAX_WIDTH } ?: width
    val (outW, outH) = ExportSize.outputSize(active, sourceWidth, sourceHeight)
    val estimate = ExportSize.estimateBytes(outW, outH, format)
    val estimateLabel = estimate?.let(ExportSize::approxLabel) ?: ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${format.menuLabel} width") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text("Width in pixels. Height keeps the diagram’s shape.")
                Text(
                    text = "$outW × $outH",
                    modifier = Modifier.padding(top = 12.dp),
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
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val chosen = ExportSize.parseWidth(field) ?: width
                    onConfirm(chosen)
                },
            ) { Text("Export") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
