# Practice 3 — Jetpack Compose Phases (Answers & Code)
**Student Worksheet & Project: FramePhases (`uz.ttpu.framephases`)**

---

## Part 1 — The Frame (Questions 1–3)

### Question 1 — The three phases of a frame (THEORETICAL)

**(a) The three phases in order and their outputs:**
1. **Composition:** Executes composable functions to determine *what* UI to display, producing and updating the UI element tree (`LayoutNode` tree).
2. **Layout:** Measures elements and determines *where* to place them, producing 2D sizes (width, height) and coordinates (x, y) for every node.
3. **Drawing:** Renders elements onto the screen pixel canvas, producing the visual pixels displayed on the device.

**(b) The two steps of the Layout phase:**
1. **Measurement:** Measuring children within given constraints to determine their width and height.
2. **Placement:** Positioning children at specific (x, y) coordinates.

**(c) The `LazyColumn` exception:**
`LazyColumn` (using `SubcomposeLayout`) must know the available viewport height and scroll offset (determined in the parent's **Layout** phase) before it can decide which items are actually visible on screen. Only after this measurement occurs does it compose the visible children.  
*Example:* In a contacts list of 1,000 items, only items 0 through 7 are composed because the screen height (measured during layout) can only fit 8 items at a time.

**(d) Why Compose is fast despite 3 phases:**
Compose uses **Phase Skipping (Smart Recomposition)**. Snapshot state reads are tracked per phase. If a state change only affects drawing or placement, earlier phases (Composition and Measurement) are completely skipped.

---

### Question 2 — Walk the layout algorithm (THEORETICAL)

**Setup:** `Row` contains `Image` (48×48) and `Column`. `Column` contains `Text A` (100×20) and `Text B` (80×20).

**(a) Order of all measure and place events:**
1. `Row` begins layout.
2. `Row` measures `Image`.
3. `Image` measures itself $\rightarrow$ returns 48×48.
4. `Row` measures `Column`.
5. `Column` measures `Text A`.
6. `Text A` measures itself $\rightarrow$ returns 100×20.
7. `Column` measures `Text B`.
8. `Text B` measures itself $\rightarrow$ returns 80×20.
9. `Column` decides its own size: width = $\max(100, 80) = 100\text{ px}$, height = $20 + 20 = 40\text{ px}$.
10. `Row` decides its own size: width = $48 + 100 = 148\text{ px}$, height = $\max(48, 40) = 48\text{ px}$.
11. `Row` places `Image` at $(0, 0)$.
12. `Row` places `Column` at $(48, 0)$.
13. `Column` places `Text A` at $(0, 0)$ relative to `Column` $\rightarrow$ $(48, 0)$ relative to `Row`.
14. `Column` places `Text B` at $(0, 20)$ relative to `Column` $\rightarrow$ $(48, 20)$ relative to `Row`.

**(b) Final sizes and positions:**
- **Sizes:**
  - `Column`: $100 \times 40\text{ px}$
  - `Row`: $148 \times 48\text{ px}$
- **Positions (x, y) relative to Row's top-left:**
  - `Row`: $(0, 0)$
  - `Image`: $(0, 0)$
  - `Column`: $(48, 0)$
  - `Text A`: $(48, 0)$
  - `Text B`: $(48, 20)$

**(c) Visits per node:**
Each node is visited **exactly once** for measurement and once for placement ($O(N)$ single-pass layout). If nodes were measured twice at each level (like older nested Android Views with `layout_weight`), layout time would grow exponentially ($O(2^N)$), causing dropped frames and severe UI jank.

**(d) Produced by which phase:**
The tree was produced by the **Composition** phase. Each node is a `LayoutNode` containing layout policies, modifier chains, child nodes, and draw instructions.

---

### Question 3 — State reads and restart scopes (THEORETICAL)

**(a) Snippet 1 vs Snippet 2:**
Yes, they are functionally equivalent (`by` is Kotlin delegation for `.getValue`). The state read happens **when the modifier chain is built (used)** during the Composition phase, *not* when the state is created with `mutableStateOf`.

**(b) Snippet 3:**
- `Text("A", Modifier.offset { IntOffset(offsetX.roundToPx(), 0) })`: `offsetX` is read inside the lambda during the **Layout phase (placement step)**. When `offsetX` changes, **only placement re-runs**; Composition is skipped!
- `val offsetPx = with(LocalDensity.current) { offsetX.roundToPx() } Text("B", Modifier.offset { IntOffset(offsetPx, 0) })`: `offsetX` is read directly in the composable function body during **Composition**. When `offsetX` changes, the composable **recomposes**, then measures, places, and redraws.

**(c) Restart Scope:**
A **restart scope** is the smallest block of composable code tracked by the Compose runtime that can be re-executed when a snapshot state read inside it changes. For a read directly in a composable function body, the restart scope is that **enclosing composable function itself**.

---

## Part 2 — Seeing the phases in Android Studio (Questions 4–8)

### Question 4 — Make Compose talk: the first frame (HANDS-ON)

**Helper File (`PhaseLog.kt`):**
```kotlin
package uz.ttpu.framephases

import android.util.Log
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.layout

fun Modifier.logPhases(name: String): Modifier = this
    .layout { measurable, constraints ->
        Log.d("Phases", "$name: layout/measure")
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            Log.d("Phases", "$name: layout/place")
            placeable.place(0, 0)
        }
    }
    .drawBehind { Log.d("Phases", "$name: draw") }
```

**Composable File (`LectureTree.kt`):**
```kotlin
package uz.ttpu.framephases

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LectureTree() {
    SideEffect { Log.d("Phases", "LectureTree: composition") }

    Row(Modifier.logPhases("Row")) {
        Box(Modifier.size(40.dp).background(Color.Gray).logPhases("Image"))
        Column(Modifier.logPhases("Column")) {
            Text("Title text", Modifier.logPhases("Text A"))
            Text("Subtitle", Modifier.logPhases("Text B"))
        }
    }
}
```

**Logcat Output (`tag:Phases`):**
```text
LectureTree: composition
Row: layout/measure
Image: layout/measure
Column: layout/measure
Text A: layout/measure
Text B: layout/measure
Row: layout/place
Image: layout/place
Column: layout/place
Text A: layout/place
Text B: layout/place
Image: draw
Text A: draw
Text B: draw
```
- **Comparison:** Matches Question 2 trace exactly. Measurement occurs children-first; placement occurs parent-first.
- **Rotating the emulator:** A configuration change destroys and recreates the Activity. All three phases (**Composition**, **Layout**, and **Drawing**) run again from scratch.

---

### Question 5 — Reading state in the composition phase (HANDS-ON)

**File: `PaddingDemo.kt`**
```kotlin
package uz.ttpu.framephases

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PaddingDemo() {
    var padding by remember { mutableStateOf(8.dp) }
    SideEffect { Log.d("Phases", "PaddingDemo: composition") }

    Column {
        Button(onClick = { padding = if (padding == 8.dp) 32.dp else 8.dp }) {
            Text("Toggle padding")
        }
        Text(
            text = "Padding demo",
            modifier = Modifier
                .padding(padding)               // Read during composition
                .logPhases("padding demo"),
        )
    }
}
```

**Prediction & Logcat Output on Tap:**
1. `PaddingDemo: composition` (state read inside function body during composition)
2. `padding demo: layout/measure` (padding alters size constraints)
3. `padding demo: layout/place`
4. `padding demo: draw`

---

### Question 6 — Reading state in the layout phase (HANDS-ON)

**File: `OffsetDemo.kt`**
```kotlin
package uz.ttpu.framephases

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun OffsetDemo() {
    var offsetX by remember { mutableStateOf(0.dp) }
    SideEffect { Log.d("Phases", "OffsetDemo: composition") }

    Column {
        Button(onClick = { offsetX = if (offsetX == 0.dp) 80.dp else 0.dp }) {
            Text("Move")
        }
        Text(
            text = "Offset demo",
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToPx(), 0) }   // Read during layout placement
                .logPhases("offset demo"),
        )
    }
}
```

**Tap "Move" Logcat Observations:**
- Does `composition` appear? **NO**, Composition is skipped.
- Does `layout/measure` appear? **NO**, measurement is skipped (offset doesn't alter element bounds).
- Only `offset demo: layout/place` and `offset demo: draw` run!

**Experiment (Moving read above Text):**
```kotlin
val offsetPx = with(LocalDensity.current) { offsetX.roundToPx() }
Text(
    text = "Offset demo",
    modifier = Modifier
        .offset { IntOffset(offsetPx, 0) }
        .logPhases("offset demo"),
)
```
- **Result:** Now `offsetX` is read in the composable body during composition. Tapping "Move" causes `OffsetDemo: composition` to appear again on every click.

---

### Question 7 — Reading state in the drawing phase (HANDS-ON)

**File: `ColorDemo.kt`**
```kotlin
package uz.ttpu.framephases

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ColorDemo() {
    var color by remember { mutableStateOf(Color.Red) }
    SideEffect { Log.d("Phases", "ColorDemo: composition") }

    Column {
        Button(onClick = { color = if (color == Color.Red) Color.Blue else Color.Red }) {
            Text("Change color")
        }
        Canvas(Modifier.size(120.dp).logPhases("color demo")) {
            drawRect(color)                    // Read during draw
        }
    }
}
```

**Observations:**
- **Canvas read:** When clicking "Change color", *only* `color demo: draw` appears. Composition and layout are skipped completely.
- **Replacing with `Box(Modifier.background(color))`:**
  `background(color)` reads the color during modifier construction in Composition. Tapping causes `ColorDemo: composition` to run again.
- **Takeaway:** Where state is created (`remember { mutableStateOf(...) }`) does not determine the phase; the exact phase where `.value` is **read** determines what re-runs.

---

### Question 8 — Which phase re-runs? (THEORETICAL)

| State Read Expression | Phase Read | Composition | Layout | Drawing |
| :--- | :--- | :--- | :--- | :--- |
| **(a)** `Modifier.padding(p)` | **Composition** | **runs** | **runs** | **runs** |
| **(b)** `Modifier.offset { IntOffset(p, 0) }` | **Layout (placement)** | **skipped** | **runs** (placement only) | **runs** |
| **(c)** `Modifier.offset(p)` | **Composition** | **runs** | **runs** | **runs** |
| **(d)** `Canvas(Modifier.size(50.dp)) { drawRect(c) }` | **Drawing** | **skipped** | **skipped** | **runs** |
| **(e)** `Layout(content) { m, cs -> /* uses s */ }` | **Layout (measure)** | **skipped** | **runs** | **runs** |
| **(f)** `Modifier.drawWithContent { /* uses c */ }` | **Drawing** | **skipped** | **skipped** | **runs** |

**Recommendation for high-frequency animation:**
- Use **(b)** for position changes or **(d)/(f)** for color/visual changes.
- Never use (a) or (c) for animations because re-running Composition at 60–120 FPS causes heavy CPU and garbage collector churn, causing dropped frames (jank).

---

## Part 3 — Optimizing state reads (Questions 9–11)

### Question 9 — Parallax: composition read versus layout read (HANDS-ON)

**File: `ParallaxDemo.kt`**
```kotlin
package uz.ttpu.framephases

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun ParallaxSlow() {
    val listState = rememberLazyListState()
    val compositions = remember { intArrayOf(0) }
    SideEffect { Log.d("Parallax", "slow: composition #${++compositions[0]}") }

    Box {
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color(0xFF6650A4))
                .offset(
                    y = with(LocalDensity.current) {
                        (listState.firstVisibleItemScrollOffset / 2).toDp()
                    }
                )
        )
        LazyColumn(state = listState) {
            items(100) { index ->
                Text("Item $index", Modifier.fillMaxWidth().padding(16.dp))
            }
        }
    }
}

@Composable
fun ParallaxFast() {
    val listState = rememberLazyListState()
    val compositions = remember { intArrayOf(0) }
    SideEffect { Log.d("Parallax", "fast: composition #${++compositions[0]}") }

    Box {
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color(0xFF6650A4))
                .offset {
                    IntOffset(x = 0, y = listState.firstVisibleItemScrollOffset / 2)
                }
        )
        LazyColumn(state = listState) {
            items(100) { index ->
                Text("Item $index", Modifier.fillMaxWidth().padding(16.dp))
            }
        }
    }
}
```

- **Logcat Comparison:**
  - `ParallaxSlow`: Recomposition counter reaches hundreds (`slow: composition #350`).
  - `ParallaxFast`: Recomposition counter remains strictly at `fast: composition #1`!
- **One-sentence summary:**
  > The slow version triggers Composition, Layout, and Drawing on every scroll event, whereas the fast version skips Composition entirely and only triggers the Layout placement and Drawing phases.

---

### Question 10 — derivedStateOf: fewer recompositions (HANDS-ON)

**File: `ScrollToTopDemo.kt`**
```kotlin
package uz.ttpu.framephases

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ScrollToTopNaive() {
    val listState = rememberLazyListState()
    val compositions = remember { intArrayOf(0) }
    SideEffect { Log.d("Derived", "naive: composition #${++compositions[0]}") }

    val showButton = listState.firstVisibleItemIndex > 0

    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = listState) {
            items(200) { index ->
                Text("Item $index", Modifier.fillMaxWidth().padding(16.dp))
            }
        }
        if (showButton) {
            Button(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Text("Top")
            }
        }
    }
}

@Composable
fun ScrollToTopOptimized() {
    val listState = rememberLazyListState()
    val compositions = remember { intArrayOf(0) }
    SideEffect { Log.d("Derived", "optimized: composition #${++compositions[0]}") }

    val showButton by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = listState) {
            items(200) { index ->
                Text("Item $index", Modifier.fillMaxWidth().padding(16.dp))
            }
        }
        if (showButton) {
            Button(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Text("Top")
            }
        }
    }
}
```

- **Why is the difference so large?**
  In the naive version, `listState.firstVisibleItemIndex` changes with almost every item scrolled past (0, 1, 2, 3...), triggering recomposition on every index change even though `showButton` remains `true`. With `derivedStateOf`, Compose only triggers recomposition when the resulting boolean changes (`false` $\leftrightarrow$ `true`), which only happens twice.

---

### Question 11 — Skipping and deferred reads (HANDS-ON)

**File: `SkipDemo.kt`**
```kotlin
package uz.ttpu.framephases

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*

@Composable
fun Parent() {
    var count by remember { mutableIntStateOf(0) }
    SideEffect { Log.d("Skip", "Parent: composition") }

    Column {
        Button(onClick = { count++ }) { Text("Add") }
        Child(count = { count }) // Passing lambda defers the state read
        Sibling()
    }
}

@Composable
fun Child(count: () -> Int) {
    SideEffect { Log.d("Skip", "Child: composition") }
    Text("Count: ${count()}") // Read occurs inside Child's restart scope
}

@Composable
fun Sibling() {
    SideEffect { Log.d("Skip", "Sibling: composition") }
    Text("I never change")
}
```

- **Explanation in two sentences:**
  > Because `count` is read inside `Child`'s body via the lambda `count()`, Compose registers `Child` as the restart scope for that state read instead of `Parent`. When `count` changes, only `Child`'s scope is invalidated, allowing `Parent` and `Sibling` to skip recomposition entirely.

---

## Part 4 — Cyclic phase dependency (Questions 12–14)

### Question 12 — Two frames for one layout (THEORETICAL)

**Frame 1:**
- Composition reads `imageHeightPx = 0`.
- Text top padding is set to `0.dp`.
- During the Layout phase, `Image` is measured at 360 px, and `onSizeChanged` fires, updating `imageHeightPx = 360`.
- Mutating state during layout schedules a new recomposition for the next frame.
- Frame 1 is drawn with Text placed at $(0, 0)$ overlapping the image.

**Frame 2:**
- Composition runs reading `imageHeightPx = 360`.
- Text top padding is set to `360.toDp()`.
- Layout places Text below the 360 px image.
- `onSizeChanged` measures 360 px again (no state change, so no further recomposition).
- Frame 2 is drawn with Text placed correctly below the image.

**(a) Why is `toDp()` a warning sign?**
Pixel measurements come from the Layout phase. Converting pixels back to `Dp` to feed a modifier parameter passes data backward from Layout into Composition, reversing the unidirectional pipeline and causing multi-frame lag.

**(b) General 3-step pattern to avoid:**
1. Measuring an element's size in `onSizeChanged` / `onGloballyPositioned`.
2. Storing that size in a Compose `State`.
3. Reading that state in Composition to size or position another element.

**(c) Broken Principle:**
Breaks **Unidirectional Data Flow across phases (Composition $\rightarrow$ Layout $\rightarrow$ Drawing)** and the **single-pass layout guarantee**.

---

### Question 13 — Reproduce the loop and fix it with a layout primitive (HANDS-ON)

**File: `CyclicDemo.kt`**
```kotlin
package uz.ttpu.framephases

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun CyclicDemo() {
    var imageHeightPx by remember { mutableIntStateOf(0) }
    val heightInThisComposition = imageHeightPx
    val compositions = remember { intArrayOf(0) }
    SideEffect {
        Log.d("Cycle", "composition #${++compositions[0]}: imageHeightPx=$heightInThisComposition")
    }

    Box {
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color(0xFF6650A4))
                .onSizeChanged { size ->
                    Log.d("Cycle", "onSizeChanged: height=${size.height}")
                    imageHeightPx = size.height
                }
        )
        Text(
            text = "I'm below the image",
            modifier = Modifier.padding(
                top = with(LocalDensity.current) { heightInThisComposition.toDp() }
            ),
        )
    }
}

@Composable
fun CyclicFixed() {
    val compositions = remember { intArrayOf(0) }
    SideEffect { Log.d("Cycle", "fixed: composition #${++compositions[0]}") }

    Column {
        Box(Modifier.fillMaxWidth().height(120.dp).background(Color(0xFF6650A4)))
        Text("I'm below the image")
    }
}
```

- **Log analysis:**
  `CyclicDemo` logs 2 compositions on launch (`imageHeightPx=0` then `imageHeightPx=360`), whereas `CyclicFixed` logs only 1.
- **Single Source of Truth:**
  The **`Column` layout** itself acts as the single source of truth. It measures each child and calculates the cumulative vertical position in a single pass without needing any intermediate state.

---

### Question 14 — A custom layout: `StackBelow` (HANDS-ON)

**File: `StackBelow.kt`**
```kotlin
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
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp

@Composable
fun StackBelow(
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        // Step 1: measure children, each exactly once, with loose constraints
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { measurable ->
            Log.d("StackBelow", "measure child") // TODO 1
            measurable.measure(loose)
        }

        // Step 2: decide own size: widest child and sum of heights plus spacing
        val spacingPx = spacing.roundToPx()
        val totalSpacing = if (placeables.size > 1) (placeables.size - 1) * spacingPx else 0
        val width = placeables.maxOfOrNull { it.width } ?: 0       // TODO 2
        val height = placeables.sumOf { it.height } + totalSpacing // TODO 2

        // Step 3: place children, one below the other
        val constrainedWidth = width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val constrainedHeight = height.coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(constrainedWidth, constrainedHeight) {
            Log.d("StackBelow", "place children") // TODO 3
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
        Box(Modifier.fillMaxWidth().height(120.dp).background(Color(0xFF6650A4)))
        Text("I'm below the image")
    }
}
```

- **Logcat output (`tag:StackBelow`):**
  ```text
  measure child
  measure child
  place children
  ```
- **Analysis:** All children are measured first in one batch before placement begins, demonstrating the single-pass layout rule. Adding a third child produces three `measure child` lines before the single `place children` line.
