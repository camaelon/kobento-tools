import SwiftUI

/// A published page to play: the account's own (it can share it), or a stream item.
struct OpenPage: Hashable {
    let page: PublishedPage
    let subtitle: String
    let own: Bool
}

/// The public stream: search, newest or top, a tag; tap an item to play it, vote on it signed in.
struct StreamTab: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        @Bindable var model = model
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    Text("Documents people made with Kobento and chose to share."
                         + (model.account.email == nil ? " Sign in to vote." : ""))
                        .font(.subheadline).foregroundStyle(.secondary)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            Chip(label: "Newest", selected: model.sort == "new") { model.sort = "new" }
                            Chip(label: "Top", selected: model.sort == "top") { model.sort = "top" }
                            Divider().frame(height: 20).padding(.horizontal, 4)
                            Chip(label: "All", selected: model.tag == nil) { model.tag = nil }
                            // The tag filtering stays shown even when it is not among the most used.
                            let shown = model.tag.map { t in model.streamTags.contains(t) ? model.streamTags : [t] + model.streamTags } ?? model.streamTags
                            ForEach(shown, id: \.self) { t in
                                Chip(label: "#\(t)", selected: model.tag == t) { model.tag = model.tag == t ? nil : t }
                            }
                        }
                    }
                    if let e = model.streamError {
                        HStack {
                            Text(e).font(.caption).foregroundStyle(.red)
                            Spacer()
                            Button("Try again") { Task { await model.loadStream(force: true) } }.font(.caption)
                        }
                    }
                    if let items = model.streamItems {
                        if items.isEmpty {
                            Text(model.query.isEmpty && model.tag == nil ? "Nothing in the stream yet." : "Nothing matches.")
                                .font(.subheadline).foregroundStyle(.secondary).padding(.top, 8)
                        }
                        CardGrid {
                            ForEach(items) { item in
                                StreamCard(item: item) { model.vote(item, $0) }
                            }
                        }
                    } else if model.streamLoading {
                        ProgressView().frame(maxWidth: .infinity).padding(.top, 40)
                    }
                }
                .padding(16)
            }
            .navigationTitle("Stream")
            .searchable(text: $model.query, prompt: "Search titles, people, tags")
            .refreshable { await model.loadStream(force: true) }
            .toolbar { if model.streamLoading && model.streamItems != nil { ProgressView() } }
            // Loads for the filters as they are; typing waits a moment before it asks.
            .task(id: "\(model.sort)|\(model.tag ?? "")|\(model.query)|\(model.account.email ?? "")") {
                if !model.query.isEmpty { try? await Task.sleep(for: .milliseconds(300)) }
                guard !Task.isCancelled else { return }
                await model.loadStream()
            }
            .navigationDestination(for: OpenPage.self) { PagePlayerScreen(open: $0) }
        }
    }
}

struct StreamCard: View {
    let item: StreamItem
    let onVote: (Int) -> Void

    var body: some View {
        VStack(spacing: 0) {
            NavigationLink(value: OpenPage(page: item.page, subtitle: "by \(item.author)", own: false)) {
                Card {
                    RemoteImage(url: item.page.previewUrl)
                        .overlay(alignment: .topLeading) {
                            if item.showcase {
                                Text("Showcase").font(.caption2.weight(.semibold))
                                    .padding(.horizontal, 6).padding(.vertical, 2)
                                    .background(Color.accentColor.opacity(0.2), in: RoundedRectangle(cornerRadius: 6))
                            }
                        }
                } caption: {
                    Text(item.title).font(.subheadline.weight(.semibold)).lineLimit(2, reservesSpace: true)
                    Text("by \(item.author)").font(.caption).foregroundStyle(.secondary).lineLimit(1)
                    if !item.tags.isEmpty {
                        Text(item.tags.map { "#\($0)" }.joined(separator: "  ")).font(.caption).foregroundStyle(Color.accentColor).lineLimit(1)
                    }
                }
            }
            .buttonStyle(.plain)
            HStack(spacing: 4) {
                VoteButton(systemImage: "arrowtriangle.up.fill", label: "Vote up", on: item.myVote == 1, color: .green) { onVote(1) }
                Text("\(item.score)").font(.subheadline.weight(.bold)).monospacedDigit()
                VoteButton(systemImage: "arrowtriangle.down.fill", label: "Vote down", on: item.myVote == -1, color: .red) { onVote(-1) }
                Text("+\(item.ups) / −\(item.downs)").font(.caption2).foregroundStyle(.secondary).lineLimit(1)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 6).padding(.bottom, 4)
            .background(Color(.secondarySystemBackground))
        }
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

private struct VoteButton: View {
    let systemImage: String
    let label: String
    let on: Bool
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage).font(.body).foregroundStyle(on ? color : Color.secondary)
                .frame(width: 36, height: 36)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(on ? "\(label) (tap again to take it back)" : label)
    }
}

/// A published page's preview, downloaded (nothing while it loads or when there is none).
struct RemoteImage: View {
    let url: URL?

    var body: some View {
        AsyncImage(url: url) { phase in
            if let image = phase.image { image.resizable().scaledToFit().clipShape(RoundedRectangle(cornerRadius: 6)) }
        }
    }
}
