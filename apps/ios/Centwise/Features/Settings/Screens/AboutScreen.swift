import SwiftUI

public struct AboutScreen: View {
    public init() {}

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
    }

    private var buildNumber: String {
        Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
    }

    public var body: some View {
        List {
            Section {
                VStack(spacing: CentwiseSpacing.md) {
                        Image("AppLogo")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 84, height: 84)
                            .clipShape(RoundedRectangle(cornerRadius: CentwiseSpacing.radiusMd, style: .continuous))

                        Text("Centwise")
                            .font(CentwiseTypography.title2)
                            .foregroundColor(.primary)

                        Text("Version \(appVersion) (\(buildNumber))")
                            .font(CentwiseTypography.caption1)
                            .foregroundColor(.secondary)

                        Text("Bangladesh-focused expense tracker that turns bank and MFS SMS into insights automatically.")
                            .font(CentwiseTypography.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, CentwiseSpacing.sm)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, CentwiseSpacing.md)
                .listRowBackground(Color.clear)
            }

            Section {
                LabeledContent("Platform", value: "iOS")
                LabeledContent("Made for", value: "Bangladesh 🇧🇩")
                LabeledContent("Data storage", value: "On-device only")
                LabeledContent("Currency", value: "Bangladeshi Taka (৳)")
            }

            Section("Privacy First") {
                Text("Centwise works fully offline. Your SMS messages, transactions, and balances never leave your device unless you create a backup yourself.")
                    .foregroundStyle(.secondary)
            }

            Section {
                Text("© 2026 Centwise")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .listRowBackground(Color.clear)
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("About")
        .navigationBarTitleDisplayMode(.inline)
    }

}
