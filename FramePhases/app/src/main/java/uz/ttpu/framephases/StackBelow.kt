package uz.ttpu.framephases

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun StackBelow(
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        // Step 1: measure the children, each exactly once, with loose minimum constraints
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { measurable ->
            // TODO 1: log "measure child", then measure it with 
            Log.d("StackBelow", "measure child")
            measurable.measure(loose)
        }

        // Step 2: decide our own size: the widest child, and the heights
        // plus the gaps between them.
        val spacingPx = spacing.roundToPx()
        val totalSpacing = if (placeables.size > 1) (placeables.size - 1) * spacingPx else 0
        // TODO 2: calculate width and height
        val width = placeables.maxOfOrNull { it.width } ?: 0
        val height = placeables.sumOf { it.height } + totalSpacing

        // Step 3: place the children, one below the other.
        val constrainedWidth = width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val constrainedHeight = height.coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(constrainedWidth, constrainedHeight) {
            // TODO 3: log "place children", then place every child below the previous one
            Log.d("StackBelow", "place children")
            var yPosition = 0
            for (placeable in placeables) {
                placeable.placeRelative(x = 0, y = yPosition)
                yPosition += placeable.height + spacingPx
            }
        }
    }
}

@Composable
fun StackBelowDemo() {
    StackBelow(Modifier.fillMaxWidth(), spacing = 8.dp) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color(0xFF6650A4))
        )
        Text("I'm below the image")
    }
}
