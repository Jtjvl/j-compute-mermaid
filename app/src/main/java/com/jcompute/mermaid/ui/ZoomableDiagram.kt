package com.jcompute.mermaid.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale

@Composable
fun ZoomableDiagram(
    png: ByteArray?,
    paper: Color,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(png) {
        png?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
    }

    var scale by remember(png) { mutableFloatStateOf(1f) }
    var offset by remember(png) { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .background(paper)
            .pointerInput(png) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.4f, 8f)
                    offset += pan
                }
            }
            .pointerInput(png) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null && !bitmap.isRecycled) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Rendered diagram",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
        }
    }
}
