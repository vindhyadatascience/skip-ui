// Copyright 2023–2026 Skip
// SPDX-License-Identifier: MPL-2.0
#if !SKIP_BRIDGE
#if SKIP
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
#elseif canImport(CoreGraphics)
import struct CoreGraphics.CGFloat
#endif

// SKIP @bridge
public struct LazyVGrid: View, Renderable {
    let columns: [GridItem]
    let alignment: HorizontalAlignment
    let spacing: CGFloat?
    let content: ComposeBuilder

    public init(columns: [GridItem], alignment: HorizontalAlignment = .center, spacing: CGFloat? = nil, pinnedViews: PinnedScrollableViews = [], @ViewBuilder content: () -> any View) {
        self.columns = columns
        self.alignment = alignment
        self.spacing = spacing
        self.content = ComposeBuilder.from(content)
    }

    // SKIP @bridge
    public init(columns: [GridItem], alignmentKey: String, spacing: CGFloat?, bridgedPinnedViews: Int, bridgedContent: any View) {
        self.columns = columns
        self.alignment = HorizontalAlignment(key: alignmentKey)
        self.spacing = spacing
        self.content = ComposeBuilder.from { bridgedContent }
        // Note: `bridgedPinnedViews` is ignored
    }

    #if SKIP
    @Composable override func Render(context: ComposeContext) {
        if EnvironmentValues.shared._intrinsicGridAxes.contains(.vertical) {
            GridItem.RenderIntrinsic(items: columns, content: content, vertical: true, spacing: spacing ?? 8.0,
                horizontalAlignment: alignment, verticalAlignment: .center, context: context)
            return
        }

        // Let any parent scroll view know about our builtin scrolling. If there is a parent scroll
        // view that didn't already know, abort and wait for recompose to avoid fatal nested scroll error
        PreferenceValues.shared.contribute(context: context, key: BuiltinScrollAxisSetPreferenceKey.self, value: Axis.Set.vertical)
        guard !EnvironmentValues.shared._scrollAxes.contains(Axis.Set.vertical) else {
            return
        }

        let gridCells = GridItem.asGridCells(items: columns)
        let horizontalArrangement = Arrangement.spacedBy(0.dp, alignment: alignment.asComposeAlignment())
        let verticalArrangement = Arrangement.spacedBy((spacing ?? 8.0).dp)
        let isScrollEnabled = EnvironmentValues.shared._scrollViewAxes.contains(.vertical)
        let scrollAxes: Axis.Set = isScrollEnabled ? Axis.Set.vertical : []
        let scrollTargetBehavior = EnvironmentValues.shared._scrollTargetBehavior

        let searchableState = EnvironmentValues.shared._searchableState
        let isSearchable = searchableState?.isOnNavigationStack == false

        let renderables = content.EvaluateLazyItems(level: 0, context: context)
        let itemCollector = remember { mutableStateOf(LazyItemCollector()) }
        ComposeContainer(axis: .vertical, scrollAxes: scrollAxes, modifier: context.modifier, fillWidth: true) { modifier in
            let gridState = rememberLazyGridState(initialFirstVisibleItemIndex = isSearchable ? 1 : 0)
            let flingBehavior = scrollTargetBehavior is ViewAlignedScrollTargetBehavior ? rememberSnapFlingBehavior(gridState, SnapPosition.Start) : ScrollableDefaults.flingBehavior()
            let coroutineScope = rememberCoroutineScope()
            if scrollAxes.contains(Axis.Set.vertical) {
                PreferenceValues.shared.contribute(context: context, key: ToolbarPreferenceKey.self, value: ToolbarPreferences(scrollableState: gridState, for: [ToolbarPlacement.bottomBar]))
                PreferenceValues.shared.contribute(context: context, key: TabBarPreferenceKey.self, value: ToolbarBarPreferences(scrollableState: gridState))
            }
            IgnoresSafeAreaLayout(expandInto: [], checkEdges: [.bottom], modifier: modifier, logTag: "LazyVGrid") { _, _ in
                // Integrate with our scroll-to-top and ScrollViewReader
                PreferenceValues.shared.contribute(context: context, key: ScrollToTopPreferenceKey.self, value: ScrollToTopAction(key: gridState) {
                    coroutineScope.launch {
                        gridState.animateScrollToItem(0)
                    }
                })
                let scrollToID = ScrollToIDAction(key: gridState) { id in
                    if let itemIndex = itemCollector.value.index(for: id) {
                        coroutineScope.launch {
                            if Animation.isInWithAnimation {
                                gridState.animateScrollToItem(itemIndex)
                            } else {
                                gridState.scrollToItem(itemIndex)
                            }
                        }
                        return true
                    }
                    return false
                }
                PreferenceValues.shared.contribute(context: context, key: ScrollToIDPreferenceKey.self, value: scrollToID)

                // Observe scroll position changes and contribute them via preferences
                let scrollPositionState = remember { mutableStateOf<ScrollPositionState?>(nil) }
                let currentItemCollector = rememberUpdatedState(itemCollector.value)
                LaunchedEffect(gridState) {
                    snapshotFlow { gridState.firstVisibleItemIndex }
                    .distinctUntilChanged()
                    .collect { index in
                        let id = currentItemCollector.value.id(for: index)
                        scrollPositionState.value = ScrollPositionState(id: id)
                    }
                }
                if let scrollPosition = scrollPositionState.value {
                    PreferenceValues.shared.contribute(context: context, key: ScrollPositionPreferenceKey.self, value: scrollPosition)
                }

                EnvironmentValues.shared.setValues {
                    $0.set_scrollTargetBehavior(nil)
                    return ComposeResult.ok
                } in: {
                    // Combine contentPadding with contentMargins additively
                    var contentPadding = EnvironmentValues.shared._contentPadding.asPaddingValues()
                    if let contentMargins = EnvironmentValues.shared._contentMargins?.asComposePaddingValues(for: .automatic) {
                        contentPadding = contentPadding.adding(contentMargins)
                    }
                    EnvironmentValues.shared.setValues {
                        $0.set_contentPadding(EdgeInsets())
                        return ComposeResult.ok
                    } in: {
                    LazyVerticalGrid(state: gridState, modifier: Modifier.fillMaxWidth(), columns: gridCells, horizontalArrangement: horizontalArrangement, verticalArrangement: verticalArrangement, contentPadding: contentPadding, userScrollEnabled: isScrollEnabled, flingBehavior: flingBehavior) {
                        var sectionItemIndex = 0
                        var sectionRowGroup = 0
                        itemCollector.value.initialize(
                            startItemIndex: isSearchable ? 1 : 0,
                            item: { renderable, _ in
                                let index = sectionItemIndex
                                let rowGroup = sectionRowGroup
                                sectionItemIndex += 1
                                item {
                                    NativeGridCell(cells: gridCells, index: index, vertical: true, rowGroup: rowGroup) {
                                        renderable.Render(context: context.content(scope: self))
                                    }
                                }
                            },
                            indexedItems: { range, identifier, _, _, _, _, factory in
                                let count = range.endExclusive - range.start
                                let firstColumnIndex = sectionItemIndex
                                let rowGroup = sectionRowGroup
                                sectionItemIndex += count
                                let key: ((Int) -> String)? = identifier == nil ? nil : { composeBundleString(for: identifier!($0 + range.start)) }
                                items(count: count, key: key) { index in
                                    NativeGridCell(cells: gridCells, index: firstColumnIndex + index, vertical: true, rowGroup: rowGroup) {
                                        let scopedContext = context.content(scope: self)
                                        factory(index + range.start, scopedContext).Render(context: scopedContext)
                                    }
                                }
                            },
                            objectItems: { objects, identifier, _, _, _, _, factory in
                                let key: (Int) -> String = { composeBundleString(for: identifier(objects[$0])) }
                                let firstColumnIndex = sectionItemIndex
                                let rowGroup = sectionRowGroup
                                sectionItemIndex += objects.count
                                items(count: objects.count, key: key) { index in
                                    NativeGridCell(cells: gridCells, index: firstColumnIndex + index, vertical: true, rowGroup: rowGroup) {
                                        let scopedContext = context.content(scope: self)
                                        factory(objects[index], scopedContext).Render(context: scopedContext)
                                    }
                                }
                            },
                            objectBindingItems: { objectsBinding, identifier, _, _, _, _, _, factory in
                                let key: (Int) -> String = { composeBundleString(for: identifier(objectsBinding.wrappedValue[$0])) }
                                let firstColumnIndex = sectionItemIndex
                                let rowGroup = sectionRowGroup
                                sectionItemIndex += objectsBinding.wrappedValue.count
                                items(count: objectsBinding.wrappedValue.count, key: key) { index in
                                    NativeGridCell(cells: gridCells, index: firstColumnIndex + index, vertical: true, rowGroup: rowGroup) {
                                        let scopedContext = context.content(scope: self)
                                        factory(objectsBinding, index, scopedContext).Render(context: scopedContext)
                                    }
                                }
                            },
                            sectionHeader: { content, _ in
                                sectionItemIndex = 0
                                sectionRowGroup += 1
                                for renderable in content {
                                    item(span: { GridItemSpan(maxLineSpan) }) {
                                        Box(contentAlignment: androidx.compose.ui.Alignment.Center) {
                                            renderable.Render(context: context.content(scope: self))
                                        }
                                    }
                                }
                                return max(1, content.size)
                            },
                            sectionFooter: { content, _, _ in
                                sectionItemIndex = 0
                                sectionRowGroup += 1
                                for renderable in content {
                                    item(span: { GridItemSpan(maxLineSpan) }) {
                                        Box(contentAlignment: androidx.compose.ui.Alignment.Center) {
                                            renderable.Render(context: context.content(scope: self))
                                        }
                                    }
                                }
                                return max(1, content.size)
                            }
                        )
                        if isSearchable {
                            item(span: { GridItemSpan(maxLineSpan) }) {
                                // We use the same logic in LazyVStack
                                let modifier = Modifier
                                    .ignoreHorizontalContentPadding(
                                        start: contentPadding.asEdgeInsets().leading.dp,
                                        end: contentPadding.asEdgeInsets().trailing.dp
                                    )
                                    .padding(start: 16.dp, end: 16.dp, top: 16.dp, bottom: 8.dp)
                                    .fillMaxWidth()
                                SearchField(state: searchableState!, context: context.content(modifier: modifier, scope: self))
                            }
                        }
                        for renderable in renderables {
                            if let factory = renderable as? LazyItemFactory, factory.shouldProduceLazyItems() {
                                factory.produceLazyItems(collector: itemCollector.value, modifiers: listOf(), level: 0)
                            } else {
                                itemCollector.value.item(renderable, 0)
                            }
                        }
                    }
                    }
                }
            }
        }
    }
    #else
    public var body: some View {
        stubView()
    }
    #endif
}

#endif
