import SwiftUI

public final class ProfileManager: ObservableObject {
    public static let shared = ProfileManager()

    @AppStorage("userName") public var userName: String = "User"
    @AppStorage("userAvatar") public var userAvatar: String = "avatar_1"
    @AppStorage("hasCompletedOnboarding") public var hasCompletedOnboarding: Bool = false
    @AppStorage("shortcutsSetupDismissed") public var shortcutsSetupDismissed: Bool = false

    public static let availableAvatars: [String] = [
        "avatar_1", "avatar_2", "avatar_3", "avatar_4", "avatar_5",
        "avatar_6", "avatar_7", "avatar_8", "avatar_9", "avatar_10"
    ]

    public var greeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 5..<12: return "Good morning"
        case 12..<17: return "Good afternoon"
        case 17..<22: return "Good evening"
        default: return "Good night"
        }
    }

    public func setProfile(name: String, avatar: String) {
        self.userName = name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "User" : name
        self.userAvatar = avatar
    }
}
