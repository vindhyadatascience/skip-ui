// Copyright 2023–2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest
#if !SKIP
import CoreGraphics
#endif

#if SKIP
import android.content.ClipData
import android.content.ClipDescription
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
#endif

/// `dropDestination(for:action:isTargeted:)` on Android: a drop's clip data reaches the action
/// as the requested payload type, at the drop location, and a drop with nothing of that type
/// is refused without calling it.
final class DropDestinationTests: SkipUITestCase {
    // SKIP INSERT: @get:org.junit.Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    func testDroppedFilesArriveAsURLs() throws {
        #if !SKIP
        throw XCTSkip("Compose drag and drop is Android-only")
        #else
        let target = DropDestinationTarget(kind: DropPayloadKind.of(URL.self))
        var received: [String] = []
        var receivedAt = CGPoint(x: 0.0, y: 0.0)
        target.deliver = { items, location in
            for item in items {
                received.append((item as! URL).absoluteString)
            }
            receivedAt = location
            return true
        }
        let clip = ClipData(ClipDescription("files", kotlin.arrayOf("text/csv", "application/octet-stream")), ClipData.Item(Uri.parse("content://com.example.files/doc/matrix.csv")))
        clip.addItem(ClipData.Item(Uri.parse("content://com.example.files/doc/project.genefox")))
        XCTAssertTrue(target.drop(clip, at: CGPoint(x: 12.0, y: 34.0)))
        XCTAssertEqual(received, ["content://com.example.files/doc/matrix.csv", "content://com.example.files/doc/project.genefox"])
        XCTAssertEqual(receivedAt.x, 12.0)
        XCTAssertEqual(receivedAt.y, 34.0)
        #endif
    }

    func testDroppedLinkTextArrivesAsAURLAndPlainTextDoesNot() throws {
        #if !SKIP
        throw XCTSkip("Compose drag and drop is Android-only")
        #else
        let link = ClipData.newPlainText("link", " https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE12345 ")
        let urls = DropPayloads.items(from: link, kind: DropPayloadKind.url)
        XCTAssertEqual(urls.count, 1)
        XCTAssertEqual((urls[0] as! URL).host, "www.ncbi.nlm.nih.gov")

        let words = ClipData.newPlainText("words", "not a link")
        XCTAssertEqual(DropPayloads.items(from: words, kind: DropPayloadKind.url).count, 0)
        #endif
    }

    func testDroppedTextArrivesAsStringsAndData() throws {
        #if !SKIP
        throw XCTSkip("Compose drag and drop is Android-only")
        #else
        let clip = ClipData.newPlainText("accession", "GSE12345")
        let strings = DropPayloads.items(from: clip, kind: DropPayloadKind.of(String.self))
        XCTAssertEqual(strings.count, 1)
        XCTAssertEqual(strings[0] as! String, "GSE12345")

        let bytes = DropPayloads.items(from: clip, kind: DropPayloadKind.of(Data.self))
        XCTAssertEqual(bytes.count, 1)
        XCTAssertEqual(String(data: bytes[0] as! Data, encoding: String.Encoding.utf8), "GSE12345")
        #endif
    }

    func testDroppedFileArrivesAsItsBytes() throws {
        #if !SKIP
        throw XCTSkip("Compose drag and drop is Android-only")
        #else
        let file = java.io.File.createTempFile("drop", ".tsv")
        file.writeText("gene\ts1\nTP53\t1.5\n")
        let clip = ClipData.newUri(composeRule.activity.getContentResolver(), "matrix", Uri.fromFile(file))
        let bytes = DropPayloads.items(from: clip, kind: DropPayloadKind.bytes)
        XCTAssertEqual(bytes.count, 1)
        XCTAssertEqual(String(data: bytes[0] as! Data, encoding: String.Encoding.utf8), "gene\ts1\nTP53\t1.5\n")
        file.delete()
        #endif
    }

    func testADropWithNothingOfTheTypeIsRefusedWithoutCallingTheAction() throws {
        #if !SKIP
        throw XCTSkip("Compose drag and drop is Android-only")
        #else
        let target = DropDestinationTarget(kind: DropPayloadKind.url)
        var calls = 0
        target.deliver = { _, _ in
            calls += 1
            return true
        }
        XCTAssertFalse(target.drop(ClipData.newPlainText("words", "not a link"), at: CGPoint(x: 0.0, y: 0.0)))
        XCTAssertFalse(target.drop(nil, at: CGPoint(x: 0.0, y: 0.0)))
        XCTAssertEqual(calls, 0)

        let unsupported = DropDestinationTarget(kind: DropPayloadKind.of(Int.self))
        unsupported.deliver = { _, _ in
            calls += 1
            return true
        }
        XCTAssertFalse(unsupported.drop(ClipData.newPlainText("n", "42"), at: CGPoint(x: 0.0, y: 0.0)))
        XCTAssertEqual(calls, 0)
        #endif
    }

    func testOnlyTextDragsStartAStringDrop() throws {
        #if !SKIP
        throw XCTSkip("Compose drag and drop is Android-only")
        #else
        let text = ClipDescription("t", kotlin.arrayOf("text/plain"))
        let file = ClipDescription("f", kotlin.arrayOf("application/gzip"))
        let acceptsText = DropPayloads.accepts(text, kind: DropPayloadKind.string)
        let acceptsFileAsString = DropPayloads.accepts(file, kind: DropPayloadKind.string)
        let acceptsFileAsURL = DropPayloads.accepts(file, kind: DropPayloadKind.url)
        let acceptsUnsupported = DropPayloads.accepts(text, kind: DropPayloadKind.unsupported)
        XCTAssertTrue(acceptsText)
        XCTAssertFalse(acceptsFileAsString)
        XCTAssertTrue(acceptsFileAsURL)
        XCTAssertFalse(acceptsUnsupported)
        #endif
    }

    func testTheModifierComposes() throws {
        #if !SKIP
        throw XCTSkip("Compose drag and drop is Android-only")
        #else
        composeRule.setContent {
            DropProbe().Compose()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("drop-target").assertIsDisplayed()
        #endif
    }
}

struct DropProbe: View {
    @State var dropped: [URL] = []
    @State var targeted = false

    var body: some View {
        Text(targeted ? "Release to import" : "Drop files here")
            .padding(40.0)
            .accessibilityIdentifier("drop-target")
            .dropDestination(for: URL.self, action: { urls, _ in
                dropped = urls
                return true
            }, isTargeted: { targeted = $0 })
    }
}
