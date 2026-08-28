import SwiftUI

public struct AvatarPickerView: View {
    @Binding public var selectedAvatar: String
    @Binding public var userName: String
    public var onSave: (() -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    private let columns = [
        GridItem(.adaptive(minimum: 64, maximum: 80), spacing: 14)
    ]

    public init(
        selectedAvatar: Binding<String>,
        userName: Binding<String>,
        onSave: (() -> Void)? = nil
    ) {
        self._selectedAvatar = selectedAvatar
        self._userName = userName
        self.onSave = onSave
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: CentwiseSpacing.lg) {
            // Live Selected Avatar Preview
            VStack(spacing: CentwiseSpacing.sm) {
                ZStack {
                    Circle()
                        .fill(themeManager.accentColor.opacity(0.15))
                        .frame(width: 90, height: 90)

                    Image(selectedAvatar)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 70, height: 70)
                        .clipShape(Circle())
                }
                .overlay(
                    Circle()
                        .stroke(themeManager.accentColor, lineWidth: 3)
                        .frame(width: 90, height: 90)
                )

                TextField("Enter your name", text: $userName)
                    .font(CentwiseTypography.headline)
                    .multilineTextAlignment(.center)
                    .textFieldStyle(.plain)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(
                        RoundedRectangle(cornerRadius: 10)
                            .fill(colorScheme == .dark ? Color.white.opacity(0.08) : Color.black.opacity(0.05))
                    )
                    .frame(maxWidth: 220)
            }
            .padding(.top, CentwiseSpacing.sm)

            Text("Choose Your Avatar")
                .font(CentwiseTypography.subheadline)
                .foregroundColor(.secondary)

            // 10 Avatar Grid
            LazyVGrid(columns: columns, spacing: 14) {
                ForEach(ProfileManager.availableAvatars, id: \.self) { avatar in
                    let isSelected = selectedAvatar == avatar
                    Button(action: {
                        themeManager.triggerHapticFeedback(.light)
                        withAnimation(.spring(response: 0.25, dampingFraction: 0.7)) {
                            selectedAvatar = avatar
                        }
                    }) {
                        ZStack {
                            Circle()
                                .fill(colorScheme == .dark ? Color.white.opacity(0.06) : Color.black.opacity(0.04))
                                .frame(width: 64, height: 64)

                            Image(avatar)
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 50, height: 50)
                                .clipShape(Circle())

                            if isSelected {
                                Circle()
                                    .stroke(themeManager.accentColor, lineWidth: 3)
                                    .frame(width: 64, height: 64)

                                Image(systemName: "checkmark.circle.fill")
                                    .font(.system(size: 18))
                                    .foregroundColor(themeManager.accentColor)
                                    .background(Circle().fill(Color.white).padding(2))
                                    .offset(x: 22, y: -22)
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, CentwiseSpacing.md)

                .padding(.bottom, CentwiseSpacing.lg)
            }
        }
        .padding(.top, CentwiseSpacing.lg)
        .background(CentwiseColors.background(for: colorScheme, isAmoled: themeManager.isAmoledActive).ignoresSafeArea())
        .navigationTitle("Edit Profile")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") { dismiss() }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Done") {
                    onSave?()
                    dismiss()
                }
                .disabled(userName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
        }
    }
}
