// Copyright 2023–2026 Skip
// SPDX-License-Identifier: MPL-2.0
#if SKIP
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

extension Modifier {
    /// Fill available width similar to `.frame(maxWidth: .infinity)`.
    ///
    /// - Seealso: `flexibleWidth(ideal:min:max:)`
    @Composable public func fillWidth() -> Modifier {
        return flexibleWidth(max: Float.flexibleFill)
    }

    /// Fill available height similar to `.frame(maxHeight: .infinity)`.
    ///
    /// - Seealso: `flexibleHeight(ideal:min:max:)`
    @Composable public func fillHeight() -> Modifier {
        return flexibleHeight(max: Float.flexibleFill)
    }

    /// Fill available size similar to `.frame(maxWidth: .infinity, maxHeight: .infinity)`.
    ///
    /// - Seealso: `flexibleWidth(ideal:min:max:)`
    /// - Seealso: `flexibleHeight(ideal:min:max:)`
    @Composable public func fillSize() -> Modifier {
        return fillWidth().fillHeight()
    }

    /// SwiftUI-like flexible layout.
    ///
    /// - Seealso: `.frame(minWidth:idealWidth:maxWidth:)`
    /// - Warning: Containers with child content should use the `ComposeContainer` composable instead.
    @Composable public func flexibleWidth(ideal: Float? = nil, min: Float? = nil, max: Float? = nil) -> Modifier {
        if let flexible = EnvironmentValues.shared._flexibleWidth {
            return then(flexible(ideal, min, max))
        } else {
            let modifier = max == Float.flexibleFill ? fillMaxWidth() : self
            return modifier.applyNonExpandingFlexibleWidth(ideal: ideal, min: min, max: max)
        }
    }

    /// SwiftUI-like flexible layout.
    ///
    ///
    /// - Seealso: `.frame(minHeight:idealHeight:maxHeight:)`
    /// - Warning: Containers with child content should use the `ComposeContainer` composable instead.
    @Composable public func flexibleHeight(ideal: Float? = nil, min: Float? = nil, max: Float? = nil) -> Modifier {
        if let flexible = EnvironmentValues.shared._flexibleHeight {
            return then(flexible(ideal, min, max))
        } else {
            let modifier = max == Float.flexibleFill ? fillMaxHeight() : self
            return modifier.applyNonExpandingFlexibleHeight(ideal: ideal, min: min, max: max)
        }
    }

    /// For internal use.
    func applyNonExpandingFlexibleWidth(ideal: Float? = nil, min: Float? = nil, max: Float? = nil) -> Modifier {
        if let min, min! > Float(0), let max, max! >= Float(0) {
            return parentBoundedWidth(minDp: min!, maxDp: max!)
        } else if let min, min! > Float(0) {
            return requiredWidthIn(min: min!.dp)
        } else if let max, max! >= Float(0) {
            return parentBoundedWidth(minDp: nil, maxDp: max!)
        } else {
            return self
        }
    }

    /// For internal use.
    func applyNonExpandingFlexibleHeight(ideal: Float? = nil, min: Float? = nil, max: Float? = nil) -> Modifier {
        if let min, min! > Float(0), let max, max! >= Float(0) {
            return parentBoundedHeight(minDp: min!, maxDp: max!)
        } else if let min, min! > Float(0) {
            return requiredHeightIn(min: min!.dp)
        } else if let max, max! >= Float(0) {
            return parentBoundedHeight(minDp: nil, maxDp: max!)
        } else {
            return self
        }
    }

    /// For internal use.
    ///
    /// SwiftUI's `.frame(maxWidth: N)` proposes `min(N, proposed width)`; it never offers more than its parent
    /// does. Compose's `requiredWidthIn(max:)` ignores the incoming bound, so content that fills (a `Spacer`, a
    /// `fixedSize(horizontal: false)` `Text`) was measured at N even inside a narrower parent, and a page framed
    /// to `maxWidth: 720` came out wider than a phone. A `minWidth` still overflows the parent, as in SwiftUI.
    func parentBoundedWidth(minDp: Float?, maxDp: Float) -> Modifier {
        self.layout { measurable, constraints in
            let minPx = minDp == nil ? 0 : minDp!.dp.roundToPx()
            let maxPx = maxDp.dp.roundToPx()
            let upper = constraints.hasBoundedWidth ? max(minPx, min(maxPx, constraints.maxWidth)) : max(minPx, maxPx)
            let placeable = measurable.measure(constraints.copy(minWidth: minPx, maxWidth: upper))
            let width = min(max(placeable.width, constraints.minWidth), constraints.maxWidth)
            return layout(width: width, height: placeable.height) {
                placeable.placeRelative(x: (width - placeable.width) / 2, y: 0)
            }
        }
    }

    /// For internal use. The height counterpart of `parentBoundedWidth`.
    func parentBoundedHeight(minDp: Float?, maxDp: Float) -> Modifier {
        self.layout { measurable, constraints in
            let minPx = minDp == nil ? 0 : minDp!.dp.roundToPx()
            let maxPx = maxDp.dp.roundToPx()
            let upper = constraints.hasBoundedHeight ? max(minPx, min(maxPx, constraints.maxHeight)) : max(minPx, maxPx)
            let placeable = measurable.measure(constraints.copy(minHeight: minPx, maxHeight: upper))
            let height = min(max(placeable.height, constraints.minHeight), constraints.maxHeight)
            return layout(width: placeable.width, height: height) {
                placeable.placeRelative(x: 0, y: (height - placeable.height) / 2)
            }
        }
    }

    /// For internal use.
    func ignoreHorizontalContentPadding(start leadingPadding: Dp, end trailingPadding: Dp) -> Modifier {
        self.layout { measurable, constraints in
            let overriddenWidth = constraints.maxWidth + leadingPadding.roundToPx() + trailingPadding.roundToPx()
            let placeable = measurable.measure(constraints.copy(maxWidth: overriddenWidth))
            return layout(width: placeable.width, height: placeable.height) {
                placeable.place(x: 0, y: 0)
            }
        }
    }

    /// For internal use.
    ///
    /// When a scroll container is offered unbounded space along an axis it scrolls on, for example under
    /// `.fixedSize()` or inside another view that measures its children unbounded, size it to its content's
    /// ideal extent on that axis, as SwiftUI does. Compose's scroll modifiers throw on an unbounded maximum.
    func idealSizeOnUnboundedScrollAxes(vertical: Bool, horizontal: Bool) -> Modifier {
        guard vertical || horizontal else {
            return self
        }
        return self.layout { measurable, constraints in
            var scrollConstraints = constraints
            if vertical && !constraints.hasBoundedHeight {
                let idealHeight = max(constraints.minHeight, measurable.maxIntrinsicHeight(constraints.maxWidth))
                scrollConstraints = scrollConstraints.copy(maxHeight: idealHeight)
            }
            if horizontal && !constraints.hasBoundedWidth {
                let idealWidth = max(constraints.minWidth, measurable.maxIntrinsicWidth(constraints.maxHeight))
                scrollConstraints = scrollConstraints.copy(maxWidth: idealWidth)
            }
            let placeable = measurable.measure(scrollConstraints)
            return layout(width: placeable.width, height: placeable.height) {
                placeable.placeRelative(x: 0, y: 0)
            }
        }
    }

    /// Add padding equivalent to the given safe area.
    @Composable func padding(safeArea: SafeArea) -> Modifier {
        let density = LocalDensity.current
        let layoutDirection = LocalLayoutDirection.current
        let top = with(density) { (safeArea.safeBoundsPx.top - safeArea.presentationBoundsPx.top).toDp() }
        let left = with(density) { (safeArea.safeBoundsPx.left - safeArea.presentationBoundsPx.left).toDp() }
        let bottom = with(density) { (safeArea.presentationBoundsPx.bottom - safeArea.safeBoundsPx.bottom).toDp() }
        let right = with(density) { (safeArea.presentationBoundsPx.right - safeArea.safeBoundsPx.right).toDp() }
        let start = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl ? right : left
        let end = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl ? left : right
        return self.padding(top: top, start: start, bottom: bottom, end: end)
    }

    /// Invoke the given closure with the modified view's root bounds.
    @Composable func onGloballyPositionedInRoot(perform: (Rect) -> Void) -> Modifier {
        return self.onGloballyPositioned {
            let bounds = $0.boundsInRoot()
            if bounds != Rect.Zero {
                perform(bounds)
            }
        }
    }

    /// Invoke the given closure with the modified view's window bounds.
    @Composable func onGloballyPositionedInWindow(perform: (Rect) -> Void) -> Modifier {
        return self.onGloballyPositioned {
            let bounds = $0.boundsInWindow()
            if bounds != Rect.Zero {
                perform(bounds)
            }
        }
    }

    @Composable func scrollDismissesKeyboardMode(_ mode: ScrollDismissesKeyboardMode) -> Modifier {
        guard mode == .immediately || mode == .interactively else {
            return self
        }
        let keyboardController = rememberUpdatedState(LocalSoftwareKeyboardController.current)
        let focusManager = rememberUpdatedState(LocalFocusManager.current)
        let imeInsets = WindowInsets.ime
        let density = LocalDensity.current
        let isUserScrollActive = remember { mutableStateOf(false) }
        let nestedScrollConnection = remember {
            KeyboardDismissingNestedScrollConnection(keyboardController: keyboardController, focusManager: focusManager, imeInsets: imeInsets, density: density, isUserScrollActive: isUserScrollActive)
        }
        return self.pointerInput(mode) {
            let slop = viewConfiguration.touchSlop
            awaitEachGesture {
                isUserScrollActive.value = false
                let downEvent = awaitPointerEvent(pass: PointerEventPass.Initial)
                guard let start = downEvent.changes.firstOrNull({ $0.pressed })?.position else {
                    return
                }
                var pressed = true
                var hasDeterminedAxis = false
                while pressed {
                    let event = awaitPointerEvent(pass: PointerEventPass.Initial)
                    if let change = event.changes.firstOrNull() {
                        pressed = change.pressed
                        if !hasDeterminedAxis {
                            let dx = abs(change.position.x - start.x)
                            let dy = abs(change.position.y - start.y)
                            if dx > slop || dy > slop {
                                hasDeterminedAxis = true
                                isUserScrollActive.value = dy > dx
                            }
                        }
                    } else {
                        pressed = false
                    }
                }
                isUserScrollActive.value = false
            }
        }.nestedScroll(nestedScrollConnection)
    }
}

extension PaddingValues {
    /// Convert padding values to edge insets in `dp` units.
    @Composable public func asEdgeInsets() -> EdgeInsets {
        let layoutDirection = EnvironmentValues.shared.layoutDirection == .rightToLeft ?     androidx.compose.ui.unit.LayoutDirection.Rtl : androidx.compose.ui.unit.LayoutDirection.Ltr
        let top = Double(calculateTopPadding().value)
        let left = Double(calculateLeftPadding(layoutDirection).value)
        let bottom = Double(calculateBottomPadding().value)
        let right = Double(calculateRightPadding(layoutDirection).value)
        return EdgeInsets(top: top, leading: left, bottom: bottom, trailing: right)
    }

    /// Add two PaddingValues together, returning a new PaddingValues with combined insets.
    @Composable public func adding(_ other: PaddingValues) -> PaddingValues {
        let insets1 = self.asEdgeInsets()
        let insets2 = other.asEdgeInsets()
        return PaddingValues(
            start: (insets1.leading + insets2.leading).dp,
            top: (insets1.top + insets2.top).dp,
            end: (insets1.trailing + insets2.trailing).dp,
            bottom: (insets1.bottom + insets2.bottom).dp
        )
    }
}

#endif
