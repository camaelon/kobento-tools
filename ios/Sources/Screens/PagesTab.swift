import SwiftUI

/// The account's own pages: sign in, then the pages it published with Kobento's MCP tools.
struct PagesTab: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    AccountRow()
                    if model.account.email == nil {
                        Text("Sign in with your Kobento account to play the pages you published with Kobento's MCP tools, and share them to the stream.")
                            .font(.subheadline).foregroundStyle(.secondary)
                    } else if let pages = model.pages {
                        if pages.isEmpty {
                            Text("None published yet. Pages you publish with Kobento's MCP tools appear here.")
                                .font(.subheadline).foregroundStyle(.secondary)
                        }
                        CardGrid {
                            ForEach(pages) { p in
                                NavigationLink(value: OpenPage(page: p, subtitle: "Your page", own: true)) { PageCard(page: p) }
                                    .buttonStyle(.plain)
                                    .accessibilityIdentifier("page-\(p.id)")
                            }
                        }
                    }
                }
                .padding(16)
            }
            .navigationTitle("Your pages")
            .refreshable { if model.account.email != nil { try? await model.reloadPages() } }
            .navigationDestination(for: OpenPage.self) { PagePlayerScreen(open: $0) }
        }
    }
}

/// Sign in, or who is signed in (refresh, sign out); a sign-in error under it.
private struct AccountRow: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(model.account.email.map { "Signed in as \($0)" } ?? "Not signed in")
                    .font(.subheadline).foregroundStyle(.secondary).lineLimit(1).truncationMode(.middle)
                Spacer()
                if model.busy {
                    ProgressView()
                } else if model.account.email == nil {
                    Button("Sign in") { model.signIn() }.buttonStyle(.borderedProminent)
                } else {
                    Button("Refresh") { model.refreshPages() }
                    Button("Sign out") { model.signOut() }
                }
            }
            if let e = model.error { Text(e).font(.caption).foregroundStyle(.red) }
        }
    }
}

private struct PageCard: View {
    let page: PublishedPage

    var body: some View {
        Card {
            RemoteImage(url: page.previewUrl)
        } caption: {
            Text(page.title).font(.subheadline.weight(.semibold)).lineLimit(2)
            Text([String(page.created.prefix(10)), page.rcBytes > 0 ? formatBytes(page.rcBytes) : ""].filter { !$0.isEmpty }.joined(separator: " · "))
                .font(.caption).foregroundStyle(.secondary)
            if let s = page.stream {
                Text("In the stream · score \(s.score)").font(.caption).foregroundStyle(Color.accentColor).lineLimit(1)
            }
        }
    }
}
