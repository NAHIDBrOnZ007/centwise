use crate::Category;

/// Default category seed shared by fresh installs and migrations.
pub fn default_categories() -> Vec<Category> {
    vec![
        Category {
            id: "food".into(),
            name: "Food & Dining".into(),
            icon: "fork.knife".into(),
            color_hex: "#F97316".into(),
        },
        Category {
            id: "transport".into(),
            name: "Transport".into(),
            icon: "car".into(),
            color_hex: "#06B6D4".into(),
        },
        Category {
            id: "shopping".into(),
            name: "Shopping".into(),
            icon: "bag".into(),
            color_hex: "#EC4899".into(),
        },
        Category {
            id: "bills".into(),
            name: "Bills & Utilities".into(),
            icon: "bolt".into(),
            color_hex: "#EAB308".into(),
        },
        Category {
            id: "recharge".into(),
            name: "Mobile Recharge".into(),
            icon: "antenna.radiowaves.left.and.right".into(),
            color_hex: "#8B5CF6".into(),
        },
        Category {
            id: "salary".into(),
            name: "Salary".into(),
            icon: "banknote".into(),
            color_hex: "#10B981".into(),
        },
        Category {
            id: "income".into(),
            name: "Income".into(),
            icon: "arrow.down.circle".into(),
            color_hex: "#22C55E".into(),
        },
        Category {
            id: "refunds".into(),
            name: "Refunds".into(),
            icon: "arrow.uturn.backward.circle".into(),
            color_hex: "#34D399".into(),
        },
        Category {
            id: "cashback".into(),
            name: "Cashback".into(),
            icon: "gift".into(),
            color_hex: "#2DD4BF".into(),
        },
        Category {
            id: "interest-profit".into(),
            name: "Interest & Profit".into(),
            icon: "percent".into(),
            color_hex: "#84CC16".into(),
        },
        Category {
            id: "dividends".into(),
            name: "Dividends".into(),
            icon: "chart.line.uptrend.xyaxis".into(),
            color_hex: "#65A30D".into(),
        },
        Category {
            id: "fees".into(),
            name: "Fees & Charges".into(),
            icon: "receipt".into(),
            color_hex: "#F59E0B".into(),
        },
        Category {
            id: "cash-withdrawal".into(),
            name: "Cash Withdrawal".into(),
            icon: "banknote.fill".into(),
            color_hex: "#A16207".into(),
        },
        Category {
            id: "housing".into(),
            name: "Rent & Housing".into(),
            icon: "house".into(),
            color_hex: "#0EA5E9".into(),
        },
        Category {
            id: "travel".into(),
            name: "Travel".into(),
            icon: "airplane".into(),
            color_hex: "#0284C7".into(),
        },
        Category {
            id: "transfer".into(),
            name: "Transfers".into(),
            icon: "arrow.left.arrow.right".into(),
            color_hex: "#3B82F6".into(),
        },
        Category {
            id: "health".into(),
            name: "Healthcare".into(),
            icon: "cross.case".into(),
            color_hex: "#EF4444".into(),
        },
        Category {
            id: "entertainment".into(),
            name: "Entertainment".into(),
            icon: "play.tv".into(),
            color_hex: "#6366F1".into(),
        },
        Category {
            id: "education".into(),
            name: "Education".into(),
            icon: "book".into(),
            color_hex: "#14B8A6".into(),
        },
        Category {
            id: "other".into(),
            name: "Other".into(),
            icon: "square.grid.2x2".into(),
            color_hex: "#64748B".into(),
        },
    ]
}
