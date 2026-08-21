import SwiftUI

public struct GreetingCard: View {
    public var userName: String = "User"
    public var greeting: String = "Good night"

    @Environment(\.colorScheme) private var colorScheme

    public init(userName: String = "User", greeting: String = "Good night") {
        self.userName = userName
        self.greeting = greeting
    }

    public var body: some View {
        HStack(spacing: 14) {
            // Circular Avatar with Mauve Accent
            Circle()
                .fill(Color(red: 0.71, green: 0.36, blue: 0.46)) // #B55D75
                .frame(width: 46, height: 46)
                .overlay(
                    Image(systemName: "person.crop.circle.fill")
                        .font(.system(size: 26))
                        .foregroundColor(.white)
                )

            VStack(alignment: .leading, spacing: 2) {
                Text(userName)
                    .font(.system(size: 17, weight: .bold))
                    .foregroundColor(.primary)

                Text(greeting)
                    .font(.system(size: 13, weight: .regular))
                    .foregroundColor(.secondary)
            }

            Spacer()
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(colorScheme == .dark ? Color(red: 0.11, green: 0.11, blue: 0.12) : Color(red: 0.97, green: 0.98, blue: 0.98))
        )
    }
}
