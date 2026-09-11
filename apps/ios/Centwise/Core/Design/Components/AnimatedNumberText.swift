import SwiftUI

public struct AnimatedNumberText: View {
    private let value: Double
    private let format: (Double) -> String
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var displayedValue: Double

    public init(value: Double, format: @escaping (Double) -> String) {
        self.value = value
        self.format = format
        self._displayedValue = State(initialValue: value)
    }

    public var body: some View {
        InterpolatingNumberText(value: displayedValue, format: format)
            .accessibilityLabel(format(value))
            .onAppear {
                if displayedValue != value {
                    update(to: value)
                }
            }
            .onChange(of: value) { update(to: $0) }
    }

    private func update(to newValue: Double) {
        withAnimation(reduceMotion ? nil : .easeOut(duration: 0.45)) {
            displayedValue = newValue
        }
    }
}

private struct InterpolatingNumberText: View, Animatable {
    var value: Double
    let format: (Double) -> String

    var animatableData: Double {
        get { value }
        set { value = newValue }
    }

    var body: some View {
        Text(format(value))
    }
}
