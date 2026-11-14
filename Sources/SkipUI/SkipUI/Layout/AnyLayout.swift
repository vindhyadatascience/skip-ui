// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
#if !SKIP_BRIDGE
#if SKIP
import androidx.compose.runtime.Composable
#elseif canImport(CoreGraphics)
import struct CoreGraphics.CGFloat
#endif

// VDS: the stack subset of SwiftUI's conditional layouts (RNABrowser-gd1n.3.7). Switching an
// `AnyLayout` between `HStackLayout` and `VStackLayout` keeps every child's identity, so state
// inside them (an open sheet, an expanded disclosure, a text field's focus) survives the switch,
// where an `if`/`else` between an `HStack` and a `VStack` rebuilds the children. The custom
// `Layout` protocol is still unsupported.

/// A horizontal stack for `AnyLayout`: an `HStack`'s alignment and spacing.
public struct HStackLayout {
    public var alignment: VerticalAlignment
    public var spacing: CGFloat?

    public init(alignment: VerticalAlignment = .center, spacing: CGFloat? = nil) {
        self.alignment = alignment
        self.spacing = spacing
    }
}

/// A vertical stack for `AnyLayout`: a `VStack`'s alignment and spacing.
public struct VStackLayout {
    public var alignment: HorizontalAlignment
    public var spacing: CGFloat?

    public init(alignment: HorizontalAlignment = .center, spacing: CGFloat? = nil) {
        self.alignment = alignment
        self.spacing = spacing
    }
}

/// A type-erased stack layout whose children keep their identity when it changes axis.
public struct AnyLayout {
    let horizontal: HStackLayout?
    let vertical: VStackLayout?

    public init(_ layout: HStackLayout) {
        self.horizontal = layout
        self.vertical = nil
    }

    public init(_ layout: VStackLayout) {
        self.horizontal = nil
        self.vertical = layout
    }

    public func callAsFunction(@ViewBuilder _ content: () -> any View) -> some View {
        #if SKIP
        return AnyLayoutStack(horizontal: horizontal, vertical: vertical, content: ComposeBuilder.from(content))
        #else
        return stubView()
        #endif
    }
}

/// VDS: content built from the width proposed to it, in the same layout pass (RNABrowser-gd1n.3.8).
/// A width measured after layout and fed back through state decides one frame late, and each late
/// frame lays the old arrangement out at the new width. Here the content is composed during
/// measurement with the incoming constraint, in points (infinity when unbounded), so the first
/// frame at a new size already has the right arrangement. Use it to choose structure (with an
/// `AnyLayout` inside it to keep children's identity). On Apple the equivalent is a `Layout` that
/// makes the same choice in `sizeThatFits`; SwiftUI has no such view.
public struct ProposedWidthReader: View, Renderable {
    let content: (Double) -> any View

    public init(@ViewBuilder content: @escaping (Double) -> any View) {
        self.content = content
    }

    #if SKIP
    @Composable override func Render(context: ComposeContext) {
        let contentContext = context.content()
        let build = content
        ProposedWidthBox(modifier: context.modifier) { width in
            build(width).Compose(context: contentContext)
        }
    }
    #else
    public var body: some View {
        stubView()
    }
    #endif
}

#if SKIP
/// Renders the current stack around movable children: each child is composed through its own
/// `movableContentOf` slot, so changing between the row and the column moves the children's
/// composition (and their remembered state) instead of disposing it.
struct AnyLayoutStack: View, Renderable {
    let horizontal: HStackLayout?
    let vertical: VStackLayout?
    let content: ComposeBuilder

    @Composable override func Render(context: ComposeContext) {
        let renderables = content.Evaluate(context: context, options: 0).filter { !$0.isSwiftUIEmptyView }
        let children: ComposeBuilder = rememberMovableChildren(renderables, horizontal: horizontal != nil)
        if let horizontal {
            HStack(alignment: horizontal.alignment, spacing: horizontal.spacing) { children }.Compose(context: context)
        } else if let vertical {
            VStack(alignment: vertical.alignment, spacing: vertical.spacing) { children }.Compose(context: context)
        }
    }
}
#endif
#endif
