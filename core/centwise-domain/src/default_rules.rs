use crate::{NewSmartRule, RuleMatchType, TransactionType};

#[derive(Debug, Clone, Copy)]
pub struct DefaultMerchantCategory {
    pub id: &'static str,
    pub name: &'static str,
    pub keywords: &'static [&'static str],
    pub category_id: &'static str,
}

const DEFAULT_MERCHANT_CATEGORIES: &[DefaultMerchantCategory] = &[
    // Food & Dining / Groceries
    DefaultMerchantCategory {
        id: "foodpanda",
        name: "Foodpanda",
        keywords: &["foodpanda"],
        category_id: "food",
    },
    DefaultMerchantCategory {
        id: "foodi",
        name: "Foodi",
        keywords: &["foodi"],
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
        id: "agora",
        name: "Agora Superstore",
        keywords: &["agora superstore", "agora"],
        category_id: "food",
    },
    DefaultMerchantCategory {
        id: "meena-bazar",
        name: "Meena Bazar",
        keywords: &["meena bazar", "meenabazar"],
        category_id: "food",
    },
    DefaultMerchantCategory {
        id: "unimart",
        name: "Unimart",
        keywords: &["unimart"],
        category_id: "food",
    },
    DefaultMerchantCategory {
        id: "prince-bazar",
        name: "Prince Bazar",
        keywords: &["prince bazar"],
        category_id: "food",
    },
    DefaultMerchantCategory {
        id: "kacchi-bhai",
        name: "Kacchi Bhai",
        keywords: &["kacchi bhai", "kacchibhai"],
        category_id: "food",
    },
    DefaultMerchantCategory {
        id: "sultans-dine",
        name: "Sultan's Dine",
        keywords: &["sultan's dine", "sultans dine"],
        category_id: "food",
    },
    DefaultMerchantCategory {
        id: "kfc",
        name: "KFC",
        keywords: &["kfc"],
        category_id: "food",
    },
    DefaultMerchantCategory {
        id: "pizza-hut",
        name: "Pizza Hut",
        keywords: &["pizza hut", "pizzahut"],
        category_id: "food",
    },
    DefaultMerchantCategory {
        id: "dominos",
        name: "Domino's Pizza",
        keywords: &["domino's", "dominos"],
        category_id: "food",
    },
    DefaultMerchantCategory {
        id: "burger-king",
        name: "Burger King",
        keywords: &["burger king"],
        category_id: "food",
    },
    DefaultMerchantCategory {
        id: "chillox",
        name: "Chillox",
        keywords: &["chillox"],
        category_id: "food",
    },
    DefaultMerchantCategory {
        id: "takeout",
        name: "Takeout",
        keywords: &["takeout"],
        category_id: "food",
    },
    DefaultMerchantCategory {
        id: "madchef",
        name: "Madchef",
        keywords: &["madchef"],
        category_id: "food",
    },
    DefaultMerchantCategory {
        id: "secret-recipe",
        name: "Secret Recipe",
        keywords: &["secret recipe"],
        category_id: "food",
    },
    // Shopping / Fashion & Footwear / Tech / Books
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
        id: "bata",
        name: "Bata",
        keywords: &["bata"],
        category_id: "shopping",
    },
    DefaultMerchantCategory {
        id: "apex",
        name: "Apex",
        keywords: &["apex footwear", "apex"],
        category_id: "shopping",
    },
    DefaultMerchantCategory {
        id: "lotto",
        name: "Lotto",
        keywords: &["lotto"],
        category_id: "shopping",
    },
    DefaultMerchantCategory {
        id: "sailor",
        name: "Sailor",
        keywords: &["sailor"],
        category_id: "shopping",
    },
    DefaultMerchantCategory {
        id: "yellow",
        name: "Yellow",
        keywords: &["beximco yellow", "yellow"],
        category_id: "shopping",
    },
    DefaultMerchantCategory {
        id: "richman-lubnan",
        name: "Richman & Lubnan",
        keywords: &["richman", "lubnan"],
        category_id: "shopping",
    },
    DefaultMerchantCategory {
        id: "cats-eye",
        name: "Cats Eye",
        keywords: &["cats eye", "cat's eye"],
        category_id: "shopping",
    },
    DefaultMerchantCategory {
        id: "artisan",
        name: "Artisan Outfitters",
        keywords: &["artisan"],
        category_id: "shopping",
    },
    DefaultMerchantCategory {
        id: "le-reve",
        name: "Le Reve",
        keywords: &["le reve", "lereve"],
        category_id: "shopping",
    },
    DefaultMerchantCategory {
        id: "infinity",
        name: "Infinity Mega Mall",
        keywords: &["infinity mega mall", "infinity"],
        category_id: "shopping",
    },
    DefaultMerchantCategory {
        id: "sara-lifestyle",
        name: "Sara Lifestyle",
        keywords: &["sara lifestyle"],
        category_id: "shopping",
    },
    DefaultMerchantCategory {
        id: "star-tech",
        name: "Star Tech",
        keywords: &["star tech", "startech"],
        category_id: "shopping",
    },
    DefaultMerchantCategory {
        id: "ryans",
        name: "Ryans Computers",
        keywords: &["ryans", "ryans computers"],
        category_id: "shopping",
    },
    DefaultMerchantCategory {
        id: "pickaboo",
        name: "Pickaboo",
        keywords: &["pickaboo"],
        category_id: "shopping",
    },
    DefaultMerchantCategory {
        id: "rokomari",
        name: "Rokomari",
        keywords: &["rokomari"],
        category_id: "shopping",
    },
    DefaultMerchantCategory {
        id: "shajgoj",
        name: "Shajgoj",
        keywords: &["shajgoj"],
        category_id: "shopping",
    },
    DefaultMerchantCategory {
        id: "sslcommerz",
        name: "SSLCOMMERZ",
        keywords: &["sslcommerz", "software shop limited", "software shop ltd"],
        category_id: "shopping",
    },
    // Transport & Travel
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
        keywords: &["shohoz", "shohoj"],
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
        id: "green-line",
        name: "Green Line Paribahan",
        keywords: &["green line", "greenline"],
        category_id: "travel",
    },
    DefaultMerchantCategory {
        id: "hanif",
        name: "Hanif Enterprise",
        keywords: &["hanif enterprise", "hanif"],
        category_id: "travel",
    },
    DefaultMerchantCategory {
        id: "shyamoli",
        name: "Shyamoli Paribahan",
        keywords: &["shyamoli paribahan", "shyamoli"],
        category_id: "travel",
    },
    DefaultMerchantCategory {
        id: "shohagh",
        name: "Shohagh Paribahan",
        keywords: &["shohagh paribahan", "shohagh"],
        category_id: "travel",
    },
    DefaultMerchantCategory {
        id: "ena",
        name: "Ena Transport",
        keywords: &["ena transport", "ena"],
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
    // Mobile Recharge
    DefaultMerchantCategory {
        id: "mygp",
        name: "MyGP",
        keywords: &["my gp", "mygp"],
        category_id: "recharge",
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
    // Bills & Utilities
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
    // Entertainment & Streaming
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
        id: "toffee",
        name: "Toffee",
        keywords: &["toffee"],
        category_id: "entertainment",
    },
    // Healthcare & Pharmacy
    DefaultMerchantCategory {
        id: "arogga",
        name: "Arogga",
        keywords: &["arogga", "arroga", "arrogo"],
        category_id: "health",
    },
    DefaultMerchantCategory {
        id: "osudpotro",
        name: "OsudPotro",
        keywords: &["osudpotro", "osud potro"],
        category_id: "health",
    },
    DefaultMerchantCategory {
        id: "lazz-pharma",
        name: "Lazz Pharma",
        keywords: &["lazz pharma"],
        category_id: "health",
    },
    DefaultMerchantCategory {
        id: "praava",
        name: "Praava Health",
        keywords: &["praava health", "praava"],
        category_id: "health",
    },
    DefaultMerchantCategory {
        id: "square-hospital",
        name: "Square Hospital",
        keywords: &["square hospital"],
        category_id: "health",
    },
    DefaultMerchantCategory {
        id: "evercare",
        name: "Evercare Hospital",
        keywords: &["evercare hospital", "evercare"],
        category_id: "health",
    },
    DefaultMerchantCategory {
        id: "united-hospital",
        name: "United Hospital",
        keywords: &["united hospital"],
        category_id: "health",
    },
    DefaultMerchantCategory {
        id: "labaid",
        name: "Labaid",
        keywords: &["labaid diagnostic", "labaid hospital", "labaid"],
        category_id: "health",
    },
    DefaultMerchantCategory {
        id: "ibn-sina",
        name: "Ibn Sina",
        keywords: &["ibn sina diagnostic", "ibn sina hospital", "ibn sina"],
        category_id: "health",
    },
    DefaultMerchantCategory {
        id: "popular-diagnostic",
        name: "Popular Diagnostic",
        keywords: &["popular diagnostic", "popular hospital"],
        category_id: "health",
    },
    // Education (EdTech & Universities & Coaching)
    DefaultMerchantCategory {
        id: "10ms",
        name: "10 Minute School",
        keywords: &["10 minute school", "10ms"],
        category_id: "education",
    },
    DefaultMerchantCategory {
        id: "shikho",
        name: "Shikho",
        keywords: &["shikho"],
        category_id: "education",
    },
    DefaultMerchantCategory {
        id: "interactive-cares",
        name: "Interactive Cares",
        keywords: &["interactive cares"],
        category_id: "education",
    },
    DefaultMerchantCategory {
        id: "bohubrihi",
        name: "Bohubrihi",
        keywords: &["bohubrihi"],
        category_id: "education",
    },
    DefaultMerchantCategory {
        id: "ostad",
        name: "Ostad",
        keywords: &["ostad"],
        category_id: "education",
    },
    DefaultMerchantCategory {
        id: "udvash",
        name: "Udvash Academic",
        keywords: &["udvash"],
        category_id: "education",
    },
    DefaultMerchantCategory {
        id: "unmesh",
        name: "Unmesh Medical",
        keywords: &["unmesh"],
        category_id: "education",
    },
    DefaultMerchantCategory {
        id: "mentors",
        name: "Mentors' Education",
        keywords: &["mentors'", "mentors"],
        category_id: "education",
    },
    DefaultMerchantCategory {
        id: "saifurs",
        name: "Saifur's",
        keywords: &["saifurs", "saifur's"],
        category_id: "education",
    },
    DefaultMerchantCategory {
        id: "british-council",
        name: "British Council",
        keywords: &["british council"],
        category_id: "education",
    },
    DefaultMerchantCategory {
        id: "bracu",
        name: "BRAC University",
        keywords: &["brac university", "bracu"],
        category_id: "education",
    },
    DefaultMerchantCategory {
        id: "nsu",
        name: "North South University",
        keywords: &["north south university", "nsu"],
        category_id: "education",
    },
    DefaultMerchantCategory {
        id: "aiub",
        name: "AIUB",
        keywords: &["aiub"],
        category_id: "education",
    },
    DefaultMerchantCategory {
        id: "iub",
        name: "Independent University Bangladesh",
        keywords: &["independent university", "iub"],
        category_id: "education",
    },
    DefaultMerchantCategory {
        id: "uiu",
        name: "United International University",
        keywords: &["united international university", "uiu"],
        category_id: "education",
    },
];

pub fn default_merchant_categories() -> &'static [DefaultMerchantCategory] {
    DEFAULT_MERCHANT_CATEGORIES
}

/// Authentic Bangladeshi starter rules for daily bank and MFS transactions.
pub fn default_rules() -> Vec<NewSmartRule> {
    DEFAULT_MERCHANT_CATEGORIES
        .iter()
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
