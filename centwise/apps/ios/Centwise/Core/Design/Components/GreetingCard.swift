import SwiftUI

public struct GreetingCard: View {
    @ObservedObject private var profileManager = ProfileManager.shared
    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme
    @State private var showEditProfile = false
    @State private var tempName = ""
    @State private var tempAvatar = ""

    public var userName: String?
    public var greeting: String?

    public init(userName: String? = nil, greeting: String? = nil) {
        self.userName = userName
        self.greeting = greeting
    }

    public var body: some View {
        Button(action: {
            tempName = profileManager.userName
            tempAvatar = profileManager.userAvatar
            showEditProfile = true
        }) {
            HStack(spacing: 14) {
                // Circular Avatar
                ZStack {
                    Circle()
                        .fill(themeManager.accentColor.opacity(0.18))
                        .frame(width: 48, height: 48)

                    Image(profileManager.userAvatar)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 38, height: 38)
                        .clipShape(Circle())
                }
                .overlay(
                    Circle()
                        .stroke(themeManager.accentColor.opacity(0.4), lineWidth: 1.5)
                )

                VStack(alignment: .leading, spacing: 2) {
                    Text(userName ?? profileManager.userName)
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(.primary)

                    Text(greeting ?? profileManager.greeting)
                        .font(.system(size: 13, weight: .regular))
                        .foregroundColor(.secondary)
                }

                Spacer()

                Image(systemName: "pencil.circle")
                    .font(.system(size: 20))
                    .foregroundColor(.secondary.opacity(0.6))
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(colorScheme == .dark ? Color(red: 0.11, green: 0.11, blue: 0.12) : Color(red: 0.97, green: 0.98, blue: 0.98))
            )
        }
        .buttonStyle(.plain)
        .sheet(isPresented: $showEditProfile) {
            NavigationStack {
                AvatarPickerView(
                    selectedAvatar: $tempAvatar,
                    userName: $tempName,
                    onSave: {
                        profileManager.setProfile(name: tempName, avatar: tempAvatar)
                    }
                )
                .navigationTitle("Edit Profile")
                .navigationBarTitleDisplayMode(.inline)
            }
        }
    }
}
