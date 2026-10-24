// Copyright 2023–2026 Skip
// SPDX-License-Identifier: MPL-2.0
#if !SKIP_BRIDGE
import Foundation

/// Bounds, a local rectangle, or a unit point to which a popover is attached.
public enum PopoverAttachmentAnchor {
    case rect(Anchor<CGRect>.Source)
    case point(UnitPoint)

    /// The source frame is in pixels; explicit source rectangles are in points.
    /// Unit points follow logical leading/trailing coordinates, as alignment does.
    func resolved(in bounds: CGRect, isRightToLeft: Bool, scale: Double) -> CGRect {
        switch self {
        case .rect(let source):
            guard let rect = source.rectangle else { return bounds }
            return CGRect(x: bounds.minX + rect.minX * scale,
                          y: bounds.minY + rect.minY * scale,
                          width: (rect.maxX - rect.minX) * scale,
                          height: (rect.maxY - rect.minY) * scale)
        case .point(let point):
            let x = isRightToLeft ? 1.0 - point.x : point.x
            return CGRect(x: bounds.minX + bounds.width * x,
                          y: bounds.minY + bounds.height * point.y,
                          width: 0.0, height: 0.0)
        }
    }
}
#endif

/*
import struct CoreGraphics.CGRect

/// An attachment anchor for a popover.
@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
public enum PopoverAttachmentAnchor {

    /// The anchor point for the popover relative to the source's frame.
    case rect(Anchor<CGRect>.Source)

    /// The anchor point for the popover expressed as a unit point  that
    /// describes possible alignments relative to a SkipUI view.
    case point(UnitPoint)
}
*/
