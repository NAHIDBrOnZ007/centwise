import Foundation

public final class DateFormatterHelper {
    public static let shared = DateFormatterHelper()

    private let relativeDateFormatter: RelativeDateTimeFormatter = {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .full
        return formatter
    }()

    private let dayMonthFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "d MMM"
        return formatter
    }()

    private let fullDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "d MMM yyyy, h:mm a"
        return formatter
    }()

    private let timeOnlyFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "h:mm a"
        return formatter
    }()

    public func formatRelativeOrDate(_ date: Date) -> String {
        let calendar = Calendar.current
        if calendar.isDateInToday(date) {
            return "Today, " + timeOnlyFormatter.string(from: date)
        } else if calendar.isDateInYesterday(date) {
            return "Yesterday, " + timeOnlyFormatter.string(from: date)
        } else {
            return fullDateFormatter.string(from: date)
        }
    }

    public func sectionHeaderTitle(for date: Date) -> String {
        let calendar = Calendar.current
        if calendar.isDateInToday(date) {
            return "Today"
        } else if calendar.isDateInYesterday(date) {
            return "Yesterday"
        } else {
            return dayMonthFormatter.string(from: date)
        }
    }

    public func formatFullDate(_ date: Date) -> String {
        fullDateFormatter.string(from: date)
    }
}
