import SwiftUI

public enum ToastStyle {
    case success
    case error
    case info

    public var icon: String {
        switch self {
        case .success: return "checkmark.circle.fill"
        case .error: return "exclamationmark.triangle.fill"
        case .info: return "info.circle.fill"
        }
    }

    public var iconColor: Color {
        switch self {
        case .success: return CentwiseColors.incomeGreen
        case .error: return CentwiseColors.expenseRed
        case .info: return .blue
        }
    }
}

public struct ToastItem: Identifiable, Equatable {
    public let id = UUID()
    public let message: String
    public let style: ToastStyle

    public init(_ message: String, style: ToastStyle = .success) {
        self.message = message
        self.style = style
    }
}

public struct CentwiseToastBanner: View {
    public let item: ToastItem

    public var body: some View {
        HStack(spacing: 10) {
            Image(systemName: item.style.icon)
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(item.style.iconColor)

            Text(item.message)
                .font(CentwiseTypography.subheadline)
                .foregroundColor(.white)
                .lineLimit(2)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 11)
        .background(
            Capsule()
                .fill(Color(white: 0.15))
                .shadow(color: .black.opacity(0.35), radius: 12, y: 6)
        )
    }
}

public struct ToastModifier: ViewModifier {
    @Binding var toast: ToastItem?

    public func body(content: Content) -> some View {
        content
            .overlay(alignment: .bottom) {
                if let currentToast = toast {
                    CentwiseToastBanner(item: currentToast)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                        .padding(.bottom, 24)
                        .zIndex(999)
                }
            }
            .animation(.spring(response: 0.35, dampingFraction: 0.75), value: toast)
            .onChange(of: toast) { newToast in
                if newToast != nil {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                        withAnimation {
                            if toast == newToast {
                                toast = nil
                            }
                        }
                    }
                }
            }
    }
}

public extension View {
    func toast(item: Binding<ToastItem?>) -> some View {
        modifier(ToastModifier(toast: item))
    }
}
