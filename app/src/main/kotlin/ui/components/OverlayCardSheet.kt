package com.achtien.codingtemplate.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.achtien.codingtemplate.ui.theme.OverlayCardHeightFraction
import com.achtien.codingtemplate.ui.theme.OverlayCardMargin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * An [OverlayCard] as a bottom sheet over [content]: collapsed at [OverlayCardHeightFraction] of the
 * window height, draggable to full height, and back collapses it. [content] gets the padding the collapsed
 * card covers, for a map's content padding. While [expandable] is false the card is held collapsed,
 * with no drag handle, and when it becomes true again the card starts out collapsed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayCardSheet(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    expandable: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize().clipToBounds()) {
        // A fraction of the window, not of this box, so the card keeps its height when the screen
        // around it changes (the map screen hides its top bar while details are open).
        val density = LocalDensity.current
        val windowHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
        val peekHeight = (windowHeight * OverlayCardHeightFraction + OverlayCardMargin * 2).coerceAtMost(maxHeight)
        val peekHeightPx = with(density) { peekHeight.roundToPx() }
        val layoutHeightPx = constraints.maxHeight

        val scaffoldState = rememberBottomSheetScaffoldState()
        val sheetState = scaffoldState.bottomSheetState
        KeepCollapsed(sheetState, expandable)
        val scope = rememberCoroutineScope()
        BackHandler(enabled = sheetState.currentValue == SheetValue.Expanded) {
            scope.launch { sheetState.partialExpand() }
        }

        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = peekHeight,
            sheetMaxWidth = Dp.Unspecified,
            sheetShape = RectangleShape,
            sheetContainerColor = Color.Transparent,
            sheetShadowElevation = 0.dp,
            sheetDragHandle = null,
            sheetSwipeEnabled = expandable,
            sheetContent = {
                Box(Modifier.fillMaxSize()) {
                    OverlayCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .sheetVisibleHeight(sheetState, layoutHeightPx, peekHeightPx),
                        dragHandle = if (expandable) ({ OverlayCardDragHandle(sheetState) }) else null,
                        content = sheetContent,
                    )
                }
            },
            content = content,
        )
    }
}

/**
 * Holds the sheet collapsed while not [expandable], and collapses it once when it becomes expandable.
 * A tap can set the sheet moving (a finger that drifts drags it), and a collapse requested meanwhile
 * loses to that drag, so this collapses whenever the sheet comes to rest anywhere else.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeepCollapsed(
    sheetState: SheetState,
    expandable: Boolean,
) {
    LaunchedEffect(expandable) {
        // The sheet's value each time it comes to rest.
        val restingValues = snapshotFlow { sheetState.currentValue.takeUnless { sheetState.isAnimationRunning } }
            .filterNotNull()
        // Once when expandable (after that it's the user's to expand); every time otherwise.
        (if (expandable) restingValues.take(1) else restingValues).collect { value ->
            if (value != SheetValue.PartiallyExpanded) sheetState.collapseUnlessDragged()
        }
    }
}

/** [SheetState.partialExpand], except that a drag interrupting it isn't an error: the next rest collapses again. */
@OptIn(ExperimentalMaterial3Api::class)
private suspend fun SheetState.collapseUnlessDragged() {
    try {
        partialExpand()
    } catch (_: CancellationException) {
        currentCoroutineContext().ensureActive()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverlayCardDragHandle(sheetState: SheetState) {
    val scope = rememberCoroutineScope()
    val expanded = sheetState.currentValue == SheetValue.Expanded
    val expandLabel = stringResource(androidx.compose.material3.R.string.m3c_bottom_sheet_expand_description)
    val collapseLabel = stringResource(androidx.compose.material3.R.string.m3c_bottom_sheet_collapse_description)
    Box(
        Modifier
            .clickable { scope.launch { if (expanded) sheetState.partialExpand() else sheetState.expand() } }
            .semantics(mergeDescendants = true) {
                if (expanded) {
                    collapse(collapseLabel) {
                        scope.launch { sheetState.partialExpand() }
                        true
                    }
                } else {
                    expand(expandLabel) {
                        scope.launch { sheetState.expand() }
                        true
                    }
                }
            },
    ) {
        BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.4f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun Modifier.sheetVisibleHeight(
    sheetState: SheetState,
    layoutHeightPx: Int,
    collapsedHeightPx: Int,
) = layout { measurable, constraints ->
    val offset = sheetState.requireOffsetOrNull()
    val visible = offset?.let { (layoutHeightPx - it).roundToInt() } ?: collapsedHeightPx
    val height = visible.coerceIn(0, constraints.maxHeight)
    val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

// requireOffset throws before the sheet's first layout.
@OptIn(ExperimentalMaterial3Api::class)
private fun SheetState.requireOffsetOrNull(): Float? = try {
    requireOffset()
} catch (_: IllegalStateException) {
    null
}
