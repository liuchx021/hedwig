use wasm_bindgen::JsValue;

const SHANGHAI_OFFSET_MS: f64 = 8.0 * 60.0 * 60.0 * 1000.0;

pub fn parse_timestamp_ms(ts: &str) -> Option<f64> {
    if let Ok(ms) = ts.parse::<f64>() {
        return Some(if ms < 1_000_000_000_000.0 {
            ms * 1000.0
        } else {
            ms
        });
    }

    let parsed = js_sys::Date::new(&JsValue::from_str(ts)).get_time();
    if parsed.is_nan() {
        None
    } else {
        Some(parsed)
    }
}

pub fn parse_timestamp_seconds(ts: &str) -> Option<i64> {
    parse_timestamp_ms(ts).map(|ms| (ms / 1000.0) as i64)
}

pub fn format_shanghai_datetime(ts: &str) -> String {
    parse_timestamp_ms(ts)
        .map(format_shanghai_datetime_ms)
        .unwrap_or_else(|| ts.to_string())
}

fn format_shanghai_datetime_ms(epoch_ms: f64) -> String {
    let shifted = js_sys::Date::new(&JsValue::from_f64(epoch_ms + SHANGHAI_OFFSET_MS));
    format!(
        "{:04}-{:02}-{:02} {:02}:{:02}",
        shifted.get_utc_full_year(),
        shifted.get_utc_month() + 1,
        shifted.get_utc_date(),
        shifted.get_utc_hours(),
        shifted.get_utc_minutes()
    )
}

/// Given an ISO-8601 / epoch timestamp representing an expiry time,
/// return a human-readable "X天Y小时" remaining string.
pub fn format_remaining_time(expires_at: &str) -> String {
    let expires_ms = match parse_timestamp_ms(expires_at) {
        Some(ms) => ms,
        None => return "未知".to_string(),
    };
    let now_ms = js_sys::Date::now();
    let remaining_sec = ((expires_ms - now_ms) / 1000.0) as i64;
    if remaining_sec <= 0 {
        return "已过期".to_string();
    }
    let days = remaining_sec / 86400;
    let hours = (remaining_sec % 86400) / 3600;
    if days > 0 {
        format!("{}天{}小时", days, hours)
    } else {
        format!("{}小时", hours)
    }
}
