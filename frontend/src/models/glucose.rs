use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum TrendDirection {
    DoubleUp,
    SingleUp,
    FortyFiveUp,
    Flat,
    FortyFiveDown,
    SingleDown,
    DoubleDown,
    None,
}

impl TrendDirection {
    pub fn arrow(&self) -> &'static str {
        match self {
            TrendDirection::DoubleUp => "↑↑",
            TrendDirection::SingleUp => "↑",
            TrendDirection::FortyFiveUp => "↗",
            TrendDirection::Flat => "→",
            TrendDirection::FortyFiveDown => "↘",
            TrendDirection::SingleDown => "↓",
            TrendDirection::DoubleDown => "↓↓",
            TrendDirection::None => "—",
        }
    }

    pub fn css_class(&self) -> &'static str {
        match self {
            TrendDirection::DoubleUp | TrendDirection::SingleUp => "trend-up",
            TrendDirection::DoubleDown | TrendDirection::SingleDown => "trend-down",
            _ => "trend-flat",
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GlucoseReading {
    pub id: Option<i64>,
    pub monitored_subject_id: Option<i64>,
    pub glucose_mmol: f64,
    pub glucose_mgdl: Option<f64>,
    pub trend_direction: Option<TrendDirection>,
    pub reading_time: String,
    pub pushed_to_nightscout: Option<bool>,
}

impl GlucoseReading {
    pub fn glucose_css_class(&self) -> &'static str {
        if self.glucose_mmol < 3.9 {
            "glucose-low"
        } else if self.glucose_mmol > 10.0 {
            "glucose-high"
        } else {
            "glucose-normal"
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SyncResult {
    pub synced_count: i32,
    pub time_range_start: Option<String>,
    pub time_range_end: Option<String>,
}
