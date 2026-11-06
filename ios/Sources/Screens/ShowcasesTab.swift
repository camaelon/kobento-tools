import SwiftUI

/// The showcases bundled with the app, as a grid of their renders.
struct ShowcasesTab: View {
    @Environment(AppModel.self) private var model
    @State private var path: [Showcase] = []

    var body: some View {
        NavigationStack(path: $path) {
            ScrollView {
                VStack(alignment: .leading, spacing: 4) {
                    Text("\(model.showcases.count) RemoteCompose documents, played by Kobento's C++ player (Skia on Metal). Tap one to run it.")
                        .font(.subheadline).foregroundStyle(.secondary)
                        .padding(.bottom, 8)
                    CardGrid {
                        ForEach(model.showcases) { s in
                            NavigationLink(value: s) { ShowcaseCard(showcase: s) }.buttonStyle(.plain)
                        }
                    }
                }
                .padding(16)
            }
            .navigationTitle("Kobento showcases")
            .navigationDestination(for: Showcase.self) { PlayerScreen(showcase: $0) }
        }
        .onAppear {
            if let id = Launch.showcase, path.isEmpty, let s = model.showcases.first(where: { $0.id == id }) { path = [s] }
        }
    }
}

/// Cards in columns at least 160 points wide.
struct CardGrid<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 160), spacing: 12)], spacing: 12) { content }
    }
}

/// A card: a 3:4 preview, then a title and a line or two under it.
struct Card<Preview: View, Caption: View>: View {
    @ViewBuilder let preview: Preview
    @ViewBuilder let caption: Caption

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Color(.tertiarySystemFill)
                .aspectRatio(3 / 4, contentMode: .fit)
                .overlay { preview.padding(8) }
                .clipped()
            VStack(alignment: .leading, spacing: 2) { caption }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 12).padding(.vertical, 10)
        }
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

struct ShowcaseCard: View {
    let showcase: Showcase
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        Card {
            if let image = UIImage(contentsOfFile: showcase.file(scheme == .dark ? "render-dark.png" : "render-light.png").path) {
                Image(uiImage: image).resizable().scaledToFit().clipShape(RoundedRectangle(cornerRadius: 6))
            }
        } caption: {
            Text(showcase.name).font(.subheadline.weight(.semibold)).lineLimit(1)
            Text(showcase.tagline).font(.caption).foregroundStyle(.secondary).lineLimit(2, reservesSpace: true)
        }
    }
}
