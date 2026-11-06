import Foundation

/// One showcase as the build copied it into the app (showcases/<folder>/): doc.rc, its mono
/// looks (doc.mono-dark.rc, doc.mono-light.rc) when it has them, the light and dark renders,
/// and meta.json from kobento-mcp's example catalog.
struct Showcase: Identifiable, Hashable {
    let id: String
    let folder: URL
    /// "Marble drop" out of "Marble drop: a physics toy with pegs, gravity and tilt".
    let name: String
    /// The rest of the title, or the whole title.
    let tagline: String
    let summary: String
    /// phone, tablet, square, watch-round, widget-4x1: how the player frames it.
    let formFactor: String
    let looks: [Look]
    let opCount: Int
    let designWidth: Int
    let designHeight: Int
    /// continuous, scheduled or idle.
    let repaint: String?
    /// What it shows off, as the catalog's labels ("Particles", "Drag").
    let capabilities: [String]

    func file(_ name: String) -> URL { folder.appendingPathComponent(name) }

    func bytes(_ look: Look) -> Data { (try? Data(contentsOf: file(look.file))) ?? Data() }
}

/// A way to play a document: the theme it is given, and which .rc.
enum Look: String, CaseIterable, Identifiable {
    case auto = "Auto", light = "Light", dark = "Dark", monoLight = "Mono light", monoDark = "Mono dark"

    var id: String { rawValue }

    var file: String {
        switch self {
        case .monoLight: "doc.mono-light.rc"
        case .monoDark: "doc.mono-dark.rc"
        default: "doc.rc"
        }
    }

    /// Dark in this look, with the system in `systemDark`.
    func dark(systemDark: Bool) -> Bool {
        switch self {
        case .auto: systemDark
        case .light, .monoLight: false
        case .dark, .monoDark: true
        }
    }

    /// The looks a published page has: one document in the system's theme, light or dark.
    static let page: [Look] = [.auto, .light, .dark]
}

enum Showcases {
    /// The showcases in the app, by number (SHW-04, SHW-10, ...).
    static func load() -> [Showcase] {
        guard let root = Bundle.main.resourceURL?.appendingPathComponent("showcases"),
              let folders = try? FileManager.default.contentsOfDirectory(at: root, includingPropertiesForKeys: nil)
        else { return [] }
        let labels = capabilityLabels()
        return folders.filter { $0.lastPathComponent.hasPrefix("SHW-") }.compactMap { folder in
            let files = Set((try? FileManager.default.contentsOfDirectory(atPath: folder.path)) ?? [])
            guard files.contains("doc.rc"), let data = try? Data(contentsOf: folder.appendingPathComponent("meta.json")),
                  let meta = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return nil }
            let derived = meta["derived"] as? [String: Any]
            let header = derived?["header"] as? [String: Any]
            let title = meta["title"] as? String ?? folder.lastPathComponent
            let split = title.range(of: ": ")
            let shortName = (meta["name"] as? String ?? "").replacingOccurrences(of: "-", with: " ")
            let primary = (meta["capabilities"] as? [String: Any])?["primary"] as? [String] ?? []
            return Showcase(
                id: meta["id"] as? String ?? folder.lastPathComponent,
                folder: folder,
                // "Marble drop: a physics toy ..." is a name and a tagline; a title without one is
                // the tagline, under the example's short name.
                name: split.map { String(title[..<$0.lowerBound]) } ?? (shortName.isEmpty ? title : shortName.capitalizedFirst),
                tagline: split.map { String(title[$0.upperBound...]).capitalizedFirst } ?? title,
                summary: meta["summary"] as? String ?? "",
                formFactor: meta["form_factor"] as? String ?? "phone",
                looks: Look.allCases.filter { files.contains($0.file) },
                opCount: derived?["op_count"] as? Int ?? 0,
                designWidth: header?["width"] as? Int ?? 0,
                designHeight: header?["height"] as? Int ?? 0,
                repaint: (derived?["repaint"] as? String).flatMap { $0.isEmpty ? nil : $0 },
                capabilities: primary.map { labels[$0] ?? $0 }
            )
        }.sorted { $0.number < $1.number }
    }

    /// The catalog's capability labels ("fx.particles" → "Particles").
    private static func capabilityLabels() -> [String: String] {
        guard let url = Bundle.main.url(forResource: "examples-vocabulary", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let caps = root["capabilities"] as? [String: [String: Any]] else { return [:] }
        return caps.compactMapValues { $0["label"] as? String }
    }
}

private extension Showcase {
    var number: Int { Int(id.split(separator: "-").last ?? "") ?? .max }
}

extension String {
    var capitalizedFirst: String { prefix(1).uppercased() + dropFirst() }
}

let forms = ["phone": "Phone", "tablet": "Tablet", "square": "Square", "watch-round": "Round watch", "widget-4x1": "4×1 widget"]
let repaints = ["continuous": "Every frame", "scheduled": "On a schedule", "idle": "When something changes"]

func formatBytes(_ n: Int) -> String { n < 1024 ? "\(n) B" : String(format: "%.1f KB", Double(n) / 1024) }
