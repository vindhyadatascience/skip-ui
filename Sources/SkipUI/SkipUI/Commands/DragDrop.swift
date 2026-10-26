// Copyright 2025–2026 Skip
// SPDX-License-Identifier: MPL-2.0
#if !SKIP_BRIDGE
import Foundation
#if canImport(CoreGraphics)
import CoreGraphics
#endif
#if SKIP
import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
#endif

public struct DragConfiguration {
    public struct OperationsWithinApp {
        public init(allowMove: Bool = false) {
        }
    }

    public struct OperationsOutsideApp {
        public init(allowCopy: Bool = true) {
        }
    }

    public var operationsWithinApp: DragConfiguration.OperationsWithinApp
    public var operationsOutsideApp: DragConfiguration.OperationsOutsideApp

    public init(operationsWithinApp: DragConfiguration.OperationsWithinApp = OperationsWithinApp(), operationsOutsideApp: DragConfiguration.OperationsOutsideApp = OperationsOutsideApp()) {
        self.operationsWithinApp = operationsWithinApp
        self.operationsOutsideApp = operationsOutsideApp
    }

    public init(allowMove: Bool) {
        self.operationsWithinApp = OperationsWithinApp(allowMove: allowMove)
        self.operationsOutsideApp = OperationsOutsideApp()
    }
}

public struct DragSession : Identifiable {
    public enum Phase : Hashable {
        case initial
        case active
        case ending(DropOperation)
        case ended(DropOperation)
        case dataTransferCompleted
    }

    public struct ID : Hashable {
        @available(*, unavailable)
        public func matches(_ dragSession: Any /* any UIDragSession */) -> Bool {
            fatalError()
        }
    }

    public let id: DragSession.ID
    public let phase: DragSession.Phase
    public let draggedItemIndex: Int

    @available(*, unavailable)
    public func draggedItemIDs(for type: Any.Type) -> [Any] {
        fatalError()
    }
}

public struct DropConfiguration {
    public let operation: DropOperation

    public init(operation: DropOperation) {
        self.operation = operation
    }
}

public protocol DropDelegate {
    func validateDrop(info: DropInfo) -> Bool
    func performDrop(info: DropInfo) -> Bool
    func dropEntered(info: DropInfo)
    func dropUpdated(info: DropInfo) -> DropProposal?
    func dropExited(info: DropInfo)
}

extension DropDelegate {
    public func validateDrop(info: DropInfo) -> Bool {
        return true
    }

    public func dropEntered(info: DropInfo) {
    }

    public func dropUpdated(info: DropInfo) -> DropProposal? {
        return nil
    }

    public func dropExited(info: DropInfo) {
    }
}

public struct DropInfo {
    public let location: CGPoint

    @available(*, unavailable)
    public func hasItemsConforming(to contentTypes: [Any /* UTType */]) -> Bool {
        fatalError()
    }

    @available(*, unavailable)
    public func itemProviders(for contentTypes: [Any /* UTType */]) -> [Any /* NSItemProvider */] {
        fatalError()
    }
}

//extension DropInfo {
//    @available(*, unavailable)
//    public func hasItemsConforming(to types: [String]) -> Bool {
//        fatalError()
//    }
//
//    @available(*, unavailable)
//    public func itemProviders(for types: [String]) -> [Any /* NSItemProvider */] {
//        fatalError()
//    }
//}

public enum DropOperation: Int, Hashable, Sendable {
    case cancel = 1
    case forbidden = 2
    case copy = 4
    case move = 8
    case delete = 16

    public struct Set : OptionSet, Hashable {
        public let rawValue: Int

        public init(rawValue: Int) {
            self.rawValue = rawValue
        }

        public static let cancel = DropOperation.Set(rawValue: DropOperation.cancel.rawValue)
        public static let copy = DropOperation.Set(rawValue: DropOperation.copy.rawValue)
        public static let move = DropOperation.Set(rawValue: DropOperation.move.rawValue)
        public static let forbidden = DropOperation.Set(rawValue: DropOperation.forbidden.rawValue)
        public static let delete = DropOperation.Set(rawValue: DropOperation.delete.rawValue)
    }
}

public struct DropProposal {
    public let operation: DropOperation
    public let operationOutsideApplication: DropOperation?

    public init(operation: DropOperation) {
        self.operation = operation
        self.operationOutsideApplication = nil
    }

    public init(withinApplication: DropOperation, outsideApplication: DropOperation) {
        self.operation = withinApplication
        self.operationOutsideApplication = outsideApplication
    }
}

public struct DropSession : Identifiable {
    public struct LocalSession {
        @available(*, unavailable)
        public func draggedItemIDs(for type: Any.Type) -> [Any] {
            fatalError()
        }
    }

    public struct ID : Hashable {
        @available(*, unavailable)
        public func matches(_ dropSession: Any /* any UIDropSession */) -> Bool {
            fatalError()
        }
    }

    public enum Phase : Hashable {
        case entering
        case active
        case exiting
        case ended(DropOperation)
        case dataTransferCompleted
    }

    public let id: DropSession.ID
    public let phase: DropSession.Phase
    public let localSession: DropSession.LocalSession?

    public var itemsCount: Int
    public var suggestedOperations: DropOperation.Set
    public var size: CGSize
    public var location: CGPoint
}

/// The bound on `dropDestination`'s payload type. It gives the Kotlin signature the
/// `T: Any` bound a `KClass<T>` parameter needs, as `GeometryChangeType` does.
public typealias DropPayloadType = Any

extension View {
    /// Accepts items dropped on this view, as SwiftUI's `dropDestination(for:action:isTargeted:)`
    /// does: on Android, a drag from another app or the desktop (a file from ChromeOS Files, a
    /// link or text from a browser) is delivered to `action` as the requested payload type.
    ///
    /// Supported payload types are `URL` (each dropped file's `content://` URL, readable with
    /// `Data(contentsOf:)` for as long as the activity lives, or a dropped link), `String` (each
    /// item's text) and `Data` (each item's bytes, read when it is dropped). Any other type
    /// accepts nothing. `location` is in this view's coordinate space, in points. `isTargeted`
    /// reports a drag entering and leaving the view.
    public func dropDestination<T: DropPayloadType>(for payloadType: T.Type, action: @escaping (_ items: [T], _ location: CGPoint) -> Bool, isTargeted: @escaping (Bool) -> Void = { _ in }) -> some View {
        #if SKIP
        let kind = DropPayloadKind.of(payloadType)
        return ModifiedContent(content: self, modifier: RenderModifier { context in
            let target = remember { DropDestinationTarget(kind: kind) }
            target.kind = kind
            target.deliver = { items, location in
                var typed: [T] = []
                for item in items {
                    typed.append(item as! T)
                }
                return action(typed, location)
            }
            target.targeted = isTargeted
            target.activity = LocalContext.current as? Activity
            target.density = LocalDensity.current.density
            return context.modifier
                .onGloballyPositioned { target.origin = $0.positionInRoot() }
                .dragAndDropTarget(shouldStartDragAndDrop: { event in
                    return DropPayloads.accepts(event.toAndroidDragEvent().clipDescription, kind: target.kind)
                }, target: target)
        })
        #else
        return self
        #endif
    }

    // SwiftUI 26's `dropDestination(for:isEnabled:action:)` (items and a DropSession) is not
    // declared here. Skip emits an unavailable declaration as a deprecated-error Kotlin
    // function, and Kotlin resolved an ordinary two-argument `dropDestination(for:action:)`
    // call to it rather than to the implemented overload above.

    @available(*, unavailable)
    public func onDragSessionUpdated(_ onUpdate: @escaping (DragSession) -> Void) -> some View {
        return self
    }

    @available(*, unavailable)
    public func dropConfiguration(_ configuration: @escaping (DropSession) -> DropConfiguration) -> some View {
        return self
    }

    @available(*, unavailable)
    public func onDropSessionUpdated(_ onUpdate: @escaping (DropSession) -> Void) -> some View {
        return self
    }

    @available(*, unavailable)
    public func dragConfiguration(_ configuration: DragConfiguration) -> some View {
        return self
    }

    @available(*, unavailable)
    public func draggable(for type: Any.Type? = nil, id: AnyHashable, _ payload: @escaping (Any) -> Any?) -> some View {
        return self
    }

    @available(*, unavailable)
    public func dragContainer(for itemType: Any.Type? = nil, in namespace: Namespace.ID? = nil, selection: /* @autoclosure @escaping () -> */ [Any], _ payload: @escaping ([Any]) -> any Collection) -> some View {
        return self
    }

    @available(*, unavailable)
    public func dragContainer(for itemType: Any.Type? = nil, id: @escaping (Any) -> Any, in namespace: Namespace.ID? = nil, selection: /* @autoclosure @escaping () -> */ [Any], _ payload: @escaping ([Any]) -> any Collection) -> some View {
        return self
    }

    @available(*, unavailable)
    public func dragContainer(for itemType: Any.Type? = nil, in namespace: Namespace.ID? = nil, _ payload: @escaping (_ draggedItemID: Any) -> any Collection) -> some View {
        return self
    }

    @available(*, unavailable)
    public func dragContainer(for itemType: Any.Type? = nil, id: @escaping (Any) -> Any, in namespace: Namespace.ID? = nil, _ payload: @escaping (_ draggedItemID: Any) -> any Collection) -> some View {
        return self
    }

    @available(*, unavailable)
    public func dragContainer(for itemType: Any.Type? = nil, in namespace: Namespace.ID? = nil, selection: /* @autoclosure @escaping () -> */ Any?, _ payload: @escaping (_ draggedItemID: Any) -> Any?) -> some View {
        return self
    }

    @available(*, unavailable)
    public func dragContainer(for itemType: Any.Type? = nil, id: @escaping (Any) -> Any, in namespace: Namespace.ID? = nil, selection: /* @autoclosure @escaping () -> */ Any?, _ payload: @escaping (Any) -> Any?) -> some View {
        return self
    }
}

/// The payload types `dropDestination(for:action:isTargeted:)` can deliver.
enum DropPayloadKind {
    case url
    case string
    case bytes
    case unsupported

    static func of(_ type: Any.Type) -> DropPayloadKind {
        if type == URL.self {
            return .url
        } else if type == String.self {
            return .string
        } else if type == Data.self {
            return .bytes
        } else {
            return .unsupported
        }
    }
}

#if SKIP
/// Turns a drop's clip data into payload values.
struct DropPayloads {
    /// Whether a drag described by `description` can deliver anything of `kind`. Files and
    /// links can always become a `URL` or `Data`; a `String` needs a text item.
    static func accepts(_ description: ClipDescription?, kind: DropPayloadKind) -> Bool {
        switch kind {
        case .url, .bytes:
            return description != nil
        case .string:
            guard let description else { return false }
            for i in 0..<description.getMimeTypeCount() {
                let mime = description.getMimeType(i)
                if mime.hasPrefix("text/") {
                    return true
                }
            }
            return false
        case .unsupported:
            return false
        }
    }

    /// The values `clipData` carries as `kind`, in drop order. Items that cannot become `kind`
    /// are left out.
    static func items(from clipData: ClipData?, kind: DropPayloadKind) -> [Any] {
        var items: [Any] = []
        guard let clipData else { return items }
        for i in 0..<clipData.getItemCount() {
            let item = clipData.getItemAt(i)
            let uri = item.getUri()
            let text = item.getText()?.toString()
            switch kind {
            case .url:
                if let uri {
                    if let url = URL(string: uri.toString()) {
                        items.append(url)
                    }
                } else if let text, let url = URL(string: text.trim()), url.scheme != nil {
                    items.append(url)
                }
            case .string:
                if let text {
                    items.append(text)
                } else if let uri {
                    items.append(uri.toString())
                }
            case .bytes:
                if let uri {
                    if let url = URL(string: uri.toString()), let data = try? Data(contentsOf: url) {
                        items.append(data)
                    }
                } else if let text, let data = text.data(using: String.Encoding.utf8) {
                    items.append(data)
                }
            case .unsupported:
                break
            }
        }
        return items
    }
}

/// The Compose drop target behind `dropDestination(for:action:isTargeted:)`.
final class DropDestinationTarget: DragAndDropTarget {
    var kind: DropPayloadKind
    var deliver: ([Any], CGPoint) -> Bool = { _, _ in false }
    var targeted: (Bool) -> Void = { _ in }
    var activity: Activity? = nil
    /// The view's top-left in the root, in pixels.
    var origin: Offset = Offset.Zero
    var density: Float = Float(1.0)

    init(kind: DropPayloadKind) {
        self.kind = kind
    }

    override func onDrop(event: DragAndDropEvent) -> Bool {
        let dragEvent = event.toAndroidDragEvent()
        // A file dragged in from another app arrives as a content:// URI that this app may
        // read only once the activity holds the drag's permissions. They last as long as the
        // activity, so a URL handed to the action stays readable after the drop returns.
        if let activity {
            do {
                activity.requestDragAndDropPermissions(dragEvent)
            } catch {
            }
        }
        targeted(false)
        let x = Double(dragEvent.getX() - origin.x) / Double(density)
        let y = Double(dragEvent.getY() - origin.y) / Double(density)
        return drop(dragEvent.getClipData(), at: CGPoint(x: x, y: y))
    }

    /// Delivers `clipData` to the action. Returns false, without calling it, when nothing in
    /// the drop is of the requested kind.
    func drop(_ clipData: ClipData?, at location: CGPoint) -> Bool {
        let items = DropPayloads.items(from: clipData, kind: kind)
        if items.isEmpty {
            return false
        }
        return deliver(items, location)
    }

    override func onEntered(event: DragAndDropEvent) {
        targeted(true)
    }

    override func onExited(event: DragAndDropEvent) {
        targeted(false)
    }

    override func onEnded(event: DragAndDropEvent) {
        targeted(false)
    }
}
#endif
#endif
