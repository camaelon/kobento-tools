import XCTest

/// A tap on a document is a click: SHW-15's colour swatch and its Explode button respond.
/// The screenshots are attached to the result for a look.
final class TapTests: XCTestCase {
    func testTapsReachTheDocumentsButtons() {
        let app = XCUIApplication()
        app.launchArguments = ["-showcase", "SHW-15"]
        app.launch()
        sleep(2)
        let window = app.windows.firstMatch
        func tap(_ x: CGFloat, _ y: CGFloat, _ name: String) {
            // iPhone 17 Pro points: the swatches row and the Explode button in portrait.
            window.coordinate(withNormalizedOffset: .zero).withOffset(CGVector(dx: x, dy: y)).tap()
            sleep(2)
            let shot = XCTAttachment(screenshot: app.screenshot())
            shot.name = name
            shot.lifetime = .keepAlways
            add(shot)
        }
        tap(123, 666, "after the sand swatch")
        tap(201, 725, "after Explode view")
    }
}
