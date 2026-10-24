// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

/// Strict geometry probes run against native SwiftUI and the real Compose renderer.
/// Black/white pixels avoid font and theme rasterization differences.
final class GridWidthReferenceTests: XCSnapshotTestCase {
    func testFixedColumnCountAndCenterAlignment() throws {
        try assertColumns(0, expected: [GridPixelRun(start: 76, width: 40), GridPixelRun(start: 124, width: 40)])
    }
    func testFixedAndFlexibleColumnsShareTheRemainingWidth() throws {
        try assertColumns(1, expected: [GridPixelRun(start: 0, width: 40), GridPixelRun(start: 48, width: 92), GridPixelRun(start: 148, width: 92)])
    }
    func testFlexibleMaximumLeavesCenteredExtraSpace() throws {
        try assertColumns(2, expected: [GridPixelRun(start: 66, width: 50), GridPixelRun(start: 124, width: 50)])
    }
    func testAdaptiveColumnsFitTheirMinimumAndSpacing() throws {
        try assertColumns(3, expected: [GridPixelRun(start: 0, width: 75), GridPixelRun(start: 83, width: 74), GridPixelRun(start: 165, width: 75)])
    }
    func testAdaptiveMaximumLeavesCenteredExtraSpace() throws {
        try assertColumns(4, expected: [GridPixelRun(start: 20, width: 80)])
    }
    func testEachColumnsSpacingIsAppliedAfterThatColumn() throws {
        try assertColumns(5, expected: [GridPixelRun(start: 0, width: 20), GridPixelRun(start: 24, width: 100), GridPixelRun(start: 140, width: 100)])
    }
    func testCellAlignmentBelongsToEachColumn() throws {
        try assertColumns(6, expected: [GridPixelRun(start: 76, width: 10), GridPixelRun(start: 154, width: 10)])
    }

    func testDefaultColumnAlignmentCentersAShorterCellWithinItsRow() throws {
        try assertVerticalAlignment(bottom: false, firstBlackRow: 15)
    }
    func testEachColumnsBottomAlignmentUsesTheEntireRowHeight() throws {
        try assertVerticalAlignment(bottom: true, firstBlackRow: 30)
    }

    private func assertVerticalAlignment(bottom: Bool, firstBlackRow: Int) throws {
        let snapshot = try render(compact: 1, view: GridVerticalAlignmentReferenceProbe(bottom: bottom))
        let rows = snapshot.pixmap.split(separator: "\n")
        XCTAssertEqual(rows.count, 40)
        guard rows.count == 40 else { return }
        for y in 0..<rows.count {
            let pixels = rows[y].split(separator: " ")
            XCTAssertEqual(pixels.count, 88)
            guard pixels.count == 88 else { return }
            for x in 0..<pixels.count {
                let black = x >= 48 || (x < 40 && y >= firstBlackRow && y < firstBlackRow + 10)
                XCTAssertEqual(String(pixels[x]), black ? "0" : "F", "Column alignment at (\(x), \(y)), bottom=\(bottom)")
            }
        }
    }

    private func assertColumns(_ caseIndex: Int, expected: [GridPixelRun]) throws {
        let snapshot = try render(compact: 1, view: GridWidthReferenceProbe(caseIndex: caseIndex))
        let first = snapshot.pixmap.split(separator: "\n").first ?? ""
        let pixels = first.split(separator: " ")
        XCTAssertEqual(pixels.count, caseIndex == 4 ? 120 : 240)
        var actual: [GridPixelRun] = []
        var start = -1
        for index in 0..<pixels.count {
            if pixels[index] == "0" {
                if start < 0 { start = index }
            } else if start >= 0 {
                actual.append(GridPixelRun(start: start, width: index - start))
                start = -1
            }
        }
        if start >= 0 { actual.append(GridPixelRun(start: start, width: pixels.count - start)) }
        XCTAssertEqual(actual.count, expected.count, "case \(caseIndex): \(snapshot.pixmap)")
        guard actual.count == expected.count else { return }
        for index in 0..<expected.count {
            // A fractional point may straddle a pixel boundary; retain a one-pixel rounding bound.
            XCTAssertEqual(Double(actual[index].start), Double(expected[index].start), accuracy: 1.0, "case \(caseIndex), column \(index)")
            XCTAssertEqual(Double(actual[index].width), Double(expected[index].width), accuracy: 1.0, "case \(caseIndex), column \(index)")
        }
    }
}

private struct GridPixelRun {
    let start: Int
    let width: Int
}

struct GridWidthReferenceProbe: View {
    let caseIndex: Int

    private var columns: [GridItem] {
        switch caseIndex {
        case 0, 6:
            return [GridItem(.fixed(40.0), spacing: 8.0, alignment: .leading),
                    GridItem(.fixed(40.0), spacing: 8.0, alignment: .trailing)]
        case 1:
            return [GridItem(.fixed(40.0), spacing: 8.0),
                    GridItem(.flexible(minimum: 20.0, maximum: 200.0), spacing: 8.0),
                    GridItem(.flexible(minimum: 20.0, maximum: 200.0), spacing: 8.0)]
        case 2:
            return [GridItem(.flexible(minimum: 20.0, maximum: 50.0), spacing: 8.0),
                    GridItem(.flexible(minimum: 20.0, maximum: 50.0), spacing: 8.0)]
        case 3:
            return [GridItem(.adaptive(minimum: 60.0, maximum: 100.0), spacing: 8.0)]
        case 4:
            return [GridItem(.adaptive(minimum: 60.0, maximum: 80.0), spacing: 8.0)]
        default:
            return [GridItem(.fixed(20.0), spacing: 4.0),
                    GridItem(.flexible(), spacing: 16.0), GridItem(.flexible(), spacing: 8.0)]
        }
    }

    var body: some View {
        LazyVGrid(columns: columns, alignment: .center, spacing: 0.0) {
            ForEach(0..<(caseIndex == 4 ? 1 : (caseIndex == 1 || caseIndex == 3 || caseIndex == 5 ? 3 : 2)), id: \.self) { _ in
                if caseIndex == 6 {
                    Color.black.frame(width: 10.0, height: 20.0)
                } else {
                    Color.black.frame(height: 20.0)
                }
            }
        }
        .frame(width: caseIndex == 4 ? 120.0 : 240.0, height: 20.0, alignment: .top)
        .background(Color.white)
    }
}

struct GridVerticalAlignmentReferenceProbe: View {
    let bottom: Bool
    var body: some View {
        LazyVGrid(columns: [GridItem(.fixed(40.0), spacing: 8.0, alignment: bottom ? .bottom : .center),
                            GridItem(.fixed(40.0), spacing: 0.0, alignment: .top)], spacing: 0.0) {
            Color.black.frame(height: 10.0)
            Color.black.frame(height: 40.0)
        }
        .frame(width: 88.0, height: 40.0, alignment: .top)
        .background(Color.white)
    }
}
