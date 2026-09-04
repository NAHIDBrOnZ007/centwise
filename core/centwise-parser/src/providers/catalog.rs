//! Canonical provider identifiers and sender mappings for Bangladeshi Banks, MFS, and Telcos.

pub const PROVIDER_BKASH: &str = "bkash";
pub const PROVIDER_NAGAD: &str = "nagad";
pub const PROVIDER_ROCKET: &str = "rocket";
pub const PROVIDER_UPAY: &str = "upay";
pub const PROVIDER_CELLFIN: &str = "cellfin";
pub const PROVIDER_NEXUSPAY: &str = "nexuspay";

pub const PROVIDER_ROBI: &str = "robi";
pub const PROVIDER_GRAMEENPHONE: &str = "grameenphone";
pub const PROVIDER_BANGLALINK: &str = "banglalink";
pub const PROVIDER_AIRTEL: &str = "airtel";
pub const PROVIDER_TELETALK: &str = "teletalk";

pub const PROVIDER_BANKS_GENERIC: &str = "banks-generic";
pub const PROVIDER_BRAC_BANK: &str = "brac-bank";
pub const PROVIDER_CITY_BANK: &str = "city-bank";
pub const PROVIDER_DBBL: &str = "dbbl";
pub const PROVIDER_EBL: &str = "ebl";
pub const PROVIDER_ISLAMI_BANK: &str = "islami-bank";
pub const PROVIDER_SONALI_BANK: &str = "sonali-bank";
pub const PROVIDER_PUBALI_BANK: &str = "pubali-bank";
pub const PROVIDER_UCB: &str = "ucb";
pub const PROVIDER_PRIME_BANK: &str = "prime-bank";
pub const PROVIDER_AGRANI_BANK: &str = "agrani-bank";
pub const PROVIDER_MUTUAL_TRUST_BANK: &str = "mutual-trust-bank";
pub const PROVIDER_STANDARD_CHARTERED: &str = "standard-chartered";
pub const PROVIDER_BANK_ASIA: &str = "bank-asia";
pub const PROVIDER_DHAKA_BANK: &str = "dhaka-bank";
pub const PROVIDER_TRUST_BANK: &str = "trust-bank";
pub const PROVIDER_JANATA_BANK: &str = "janata-bank";
pub const PROVIDER_RUPALI_BANK: &str = "rupali-bank";

/// Matches a normalized sender string against known bank, MFS, or telco IDs.
pub fn lookup_sender(normalized_sender: &str) -> Option<&'static str> {
    match normalized_sender {
        // MFS
        "bkash" | "bkashbd" | "bkash16247" | "16247" => Some(PROVIDER_BKASH),
        "nagad" | "nagadbd" | "nagad16167" | "16167" => Some(PROVIDER_NAGAD),
        "rocket" | "dbblrocket" | "16216" => Some(PROVIDER_ROCKET),
        "upay" | "upaybd" | "ucash" => Some(PROVIDER_UPAY),
        "cellfin" | "cellfinbd" => Some(PROVIDER_CELLFIN),
        "nexuspay" => Some(PROVIDER_NEXUSPAY),

        // Telcos
        "robi" | "robibd" | "123" => Some(PROVIDER_ROBI),
        "gp" | "grameenphone" | "gpstar" | "121" => Some(PROVIDER_GRAMEENPHONE),
        "bl" | "banglalink" | "212" => Some(PROVIDER_BANGLALINK),
        "airtel" | "airtelbd" => Some(PROVIDER_AIRTEL),
        "teletalk" | "teletalkbd" => Some(PROVIDER_TELETALK),

        // Banks
        "dbbl" | "dutchbanglabank" | "dutchbanglabankplc" => Some(PROVIDER_DBBL),
        "citybank" | "thecitybank" | "citybankplc" | "citytouch" | "amex" => {
            Some(PROVIDER_CITY_BANK)
        }
        "bracbank" | "bracbankplc" | "astallion" => Some(PROVIDER_BRAC_BANK),
        "ebl" | "easternbank" | "easternbankplc" | "eblconnect" => Some(PROVIDER_EBL),
        "ibbl" | "islamibank" | "islamibankbangladesh" => Some(PROVIDER_ISLAMI_BANK),
        "sonalibank" | "sonali" => Some(PROVIDER_SONALI_BANK),
        "pubalibank" | "pubali" => Some(PROVIDER_PUBALI_BANK),
        "ucb" | "ucbbank" => Some(PROVIDER_UCB),
        "primebank" | "primebankplc" => Some(PROVIDER_PRIME_BANK),
        "agranibank" | "agrani" => Some(PROVIDER_AGRANI_BANK),
        "mtb" | "mutualtrustbank" | "mtbbank" => Some(PROVIDER_MUTUAL_TRUST_BANK),
        "scb" | "standardchartered" | "scbbd" => Some(PROVIDER_STANDARD_CHARTERED),
        "bankasia" => Some(PROVIDER_BANK_ASIA),
        "dhakabank" => Some(PROVIDER_DHAKA_BANK),
        "trustbank" => Some(PROVIDER_TRUST_BANK),
        "janatabank" | "janata" => Some(PROVIDER_JANATA_BANK),
        "rupalibank" | "rupali" => Some(PROVIDER_RUPALI_BANK),
        _ => None,
    }
}
