use crate::{NewSmartRule, RuleMatchType, TransactionType};

#[derive(Debug, Clone, Copy)]
pub struct DefaultMerchantCategory {
    pub id: &'static str,
    pub name: &'static str,
    pub keywords: &'static [&'static str],
    pub category_id: &'static str,
}

const DEFAULT_MERCHANT_CATEGORIES: &[DefaultMerchantCategory] = &[
    DefaultMerchantCategory {
        id: "foodpanda",
        name: "Foodpanda",
        keywords: &["foodpanda"],
        category_id: "food",
    },
    DefaultMerchantCategory {
        id: "chaldal",
        name: "Chaldal",
        keywords: &["chaldal"],
        category_id: "food",
    },
    DefaultMerchantCategory {
        id: "shwapno",
        name: "Shwapno",
        keywords: &["shwapno", "swapno"],
        category_id: "food",
    },
    DefaultMerchantCategory {
        id: "daraz",
        name: "Daraz",
        keywords: &["daraz"],
        category_id: "shopping",
    },
    DefaultMerchantCategory {
        id: "aarong",
        name: "Aarong",
        keywords: &["aarong"],
        category_id: "shopping",
    },
    DefaultMerchantCategory {
        id: "pathao",
        name: "Pathao",
        keywords: &["pathao"],
        category_id: "transport",
    },
    DefaultMerchantCategory {
        id: "uber",
        name: "Uber",
        keywords: &["uber"],
        category_id: "transport",
    },
    DefaultMerchantCategory {
        id: "obhai",
        name: "OBHAI",
        keywords: &["obhai"],
        category_id: "transport",
    },
    DefaultMerchantCategory {
        id: "shohoz",
        name: "Shohoz",
        keywords: &["shohoz"],
        category_id: "transport",
    },
    DefaultMerchantCategory {
        id: "metro-rail",
        name: "Metro Rail",
        keywords: &["metro rail"],
        category_id: "transport",
    },
    DefaultMerchantCategory {
        id: "rail-sheba",
        name: "Rail Sheba",
        keywords: &["rail sheba"],
        category_id: "travel",
    },
    DefaultMerchantCategory {
        id: "jatri",
        name: "Jatri",
        keywords: &["jatri"],
        category_id: "travel",
    },
    DefaultMerchantCategory {
        id: "biman",
        name: "Biman",
        keywords: &["biman bangladesh", "biman"],
        category_id: "travel",
    },
    DefaultMerchantCategory {
        id: "us-bangla",
        name: "US-Bangla",
        keywords: &["us-bangla", "us bangla"],
        category_id: "travel",
    },
    DefaultMerchantCategory {
        id: "novoair",
        name: "Novoair",
        keywords: &["novoair"],
        category_id: "travel",
    },
    DefaultMerchantCategory {
        id: "grameenphone",
        name: "Grameenphone",
        keywords: &["grameenphone"],
        category_id: "recharge",
    },
    DefaultMerchantCategory {
        id: "airtel",
        name: "Airtel",
        keywords: &["airtel"],
        category_id: "recharge",
    },
    DefaultMerchantCategory {
        id: "robi",
        name: "Robi",
        keywords: &["robi"],
        category_id: "recharge",
    },
    DefaultMerchantCategory {
        id: "banglalink",
        name: "Banglalink",
        keywords: &["banglalink"],
        category_id: "recharge",
    },
    DefaultMerchantCategory {
        id: "teletalk",
        name: "Teletalk",
        keywords: &["teletalk"],
        category_id: "recharge",
    },
    DefaultMerchantCategory {
        id: "skitto",
        name: "Skitto",
        keywords: &["skitto"],
        category_id: "recharge",
    },
    DefaultMerchantCategory {
        id: "dpdc",
        name: "DPDC",
        keywords: &["dpdc"],
        category_id: "bills",
    },
    DefaultMerchantCategory {
        id: "desco",
        name: "DESCO",
        keywords: &["desco"],
        category_id: "bills",
    },
    DefaultMerchantCategory {
        id: "nesco",
        name: "NESCO",
        keywords: &["nesco"],
        category_id: "bills",
    },
    DefaultMerchantCategory {
        id: "wasa",
        name: "Dhaka WASA",
        keywords: &["wasa"],
        category_id: "bills",
    },
    DefaultMerchantCategory {
        id: "titas",
        name: "Titas Gas",
        keywords: &["titas"],
        category_id: "bills",
    },
    DefaultMerchantCategory {
        id: "palli-bidyut",
        name: "Palli Bidyut",
        keywords: &["palli bidyut", "breb"],
        category_id: "bills",
    },
    DefaultMerchantCategory {
        id: "carnival-internet",
        name: "Carnival Internet",
        keywords: &["carnival internet", "carnival"],
        category_id: "bills",
    },
    DefaultMerchantCategory {
        id: "icc-communication",
        name: "ICC Communication",
        keywords: &["icc communication", "icc"],
        category_id: "bills",
    },
    DefaultMerchantCategory {
        id: "btcl",
        name: "BTCL",
        keywords: &["btcl"],
        category_id: "bills",
    },
    DefaultMerchantCategory {
        id: "link3",
        name: "Link3",
        keywords: &["link3"],
        category_id: "bills",
    },
    DefaultMerchantCategory {
        id: "amberit",
        name: "AmberIT",
        keywords: &["amberit"],
        category_id: "bills",
    },
    DefaultMerchantCategory {
        id: "netflix",
        name: "Netflix",
        keywords: &["netflix"],
        category_id: "entertainment",
    },
    DefaultMerchantCategory {
        id: "spotify",
        name: "Spotify",
        keywords: &["spotify"],
        category_id: "entertainment",
    },
    DefaultMerchantCategory {
        id: "hoichoi",
        name: "Hoichoi",
        keywords: &["hoichoi"],
        category_id: "entertainment",
    },
    DefaultMerchantCategory {
        id: "chorki",
        name: "Chorki",
        keywords: &["chorki"],
        category_id: "entertainment",
    },
    DefaultMerchantCategory {
        id: "lazz-pharma",
        name: "Lazz Pharma",
        keywords: &["lazz pharma"],
        category_id: "health",
    },
    DefaultMerchantCategory {
        id: "10ms",
        name: "10 Minute School",
        keywords: &["10 minute school"],
        category_id: "education",
    },
];

const SEEDED_RULE_IDS: &[&str] = &[
    "foodpanda",
    "chaldal",
    "shwapno",
    "daraz",
    "aarong",
    "pathao",
    "shohoz",
    "metro-rail",
    "grameenphone",
    "robi",
    "banglalink",
    "skitto",
    "dpdc",
    "desco",
    "wasa",
    "lazz-pharma",
    "10ms",
];

pub fn default_merchant_categories() -> &'static [DefaultMerchantCategory] {
    DEFAULT_MERCHANT_CATEGORIES
}

/// Authentic Bangladeshi starter rules for daily bank and MFS transactions.
pub fn default_rules() -> Vec<NewSmartRule> {
    DEFAULT_MERCHANT_CATEGORIES
        .iter()
        .filter(|merchant| SEEDED_RULE_IDS.contains(&merchant.id))
        .map(|merchant| NewSmartRule {
            id: format!("rule-{}", merchant.id),
            name: merchant.name.into(),
            keyword: merchant.keywords[0].into(),
            match_type: RuleMatchType::Contains,
            category_id: merchant.category_id.into(),
            transaction_type: TransactionType::Expense,
            is_enabled: true,
        })
        .collect()
}
