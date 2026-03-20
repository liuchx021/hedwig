use crate::models::glucose::GlucoseReading;
use crate::utils::time::parse_timestamp_seconds;
use leptos::*;

const CHART_W: f64 = 800.0;
const CHART_H: f64 = 240.0;
const PAD_L: f64 = 48.0;
const PAD_R: f64 = 16.0;
const PAD_T: f64 = 16.0;
const PAD_B: f64 = 36.0;

// Target range: 3.9 - 10.0 mmol/L
const GLUCOSE_LOW: f64 = 3.9;
const GLUCOSE_HIGH: f64 = 10.0;
const GLUCOSE_MIN: f64 = 2.0;
const GLUCOSE_MAX: f64 = 20.0;

#[component]
pub fn GlucoseChart(readings: Vec<GlucoseReading>) -> impl IntoView {
    if readings.is_empty() {
        return view! {
            <div class="chart-container" style="text-align:center;color:#7a8599;padding:40px">
                "暂无数据"
            </div>
        }
        .into_view();
    }

    let inner_w = CHART_W - PAD_L - PAD_R;
    let inner_h = CHART_H - PAD_T - PAD_B;

    // Parse timestamps to get min/max time range
    let timestamps: Vec<i64> = readings
        .iter()
        .filter_map(|r| parse_ts(&r.reading_time))
        .collect();

    let t_min = timestamps.iter().copied().min().unwrap_or(0);
    let t_max = timestamps.iter().copied().max().unwrap_or(1);
    let t_span = (t_max - t_min).max(1) as f64;

    // Map value to y coordinate (inverted: high value = low y)
    let val_to_y = |v: f64| -> f64 {
        let clamped = v.clamp(GLUCOSE_MIN, GLUCOSE_MAX);
        let frac = (clamped - GLUCOSE_MIN) / (GLUCOSE_MAX - GLUCOSE_MIN);
        PAD_T + inner_h * (1.0 - frac)
    };

    // Map timestamp to x coordinate
    let t_to_x = |t: i64| -> f64 {
        let frac = (t - t_min) as f64 / t_span;
        PAD_L + inner_w * frac
    };

    // Build polyline points
    let mut points_with_t: Vec<(i64, f64, f64)> = readings
        .iter()
        .filter_map(|r| {
            parse_ts(&r.reading_time).map(|t| {
                let x = t_to_x(t);
                let y = val_to_y(r.glucose_mmol);
                (t, x, y)
            })
        })
        .collect();
    points_with_t.sort_by_key(|(t, _, _)| *t);

    let polyline_pts: String = points_with_t
        .iter()
        .map(|(_, x, y)| format!("{:.1},{:.1}", x, y))
        .collect::<Vec<_>>()
        .join(" ");

    // Target range band Y coords
    let y_low = val_to_y(GLUCOSE_LOW);
    let y_high = val_to_y(GLUCOSE_HIGH);
    let band_h = y_low - y_high;

    // Y-axis grid lines
    let y_labels: Vec<(f64, f64)> = vec![4.0, 6.0, 8.0, 10.0, 12.0, 14.0, 16.0, 18.0]
        .into_iter()
        .map(|v| (v, val_to_y(v)))
        .collect();

    // Circles for data points
    let circles: Vec<(f64, f64, f64, &'static str)> = points_with_t
        .iter()
        .zip(readings.iter())
        .filter_map(|((_, x, y), r)| {
            let color = if r.glucose_mmol < GLUCOSE_LOW {
                "#e0a84b"
            } else if r.glucose_mmol > GLUCOSE_HIGH {
                "#e05c5c"
            } else {
                "#4ecdc4"
            };
            Some((*x, *y, r.glucose_mmol, color))
        })
        .collect();

    view! {
        <div class="chart-container">
            <svg
                viewBox=format!("0 0 {} {}", CHART_W, CHART_H + PAD_B)
                width="100%"
                style="min-width:600px;max-width:100%"
            >
                // Target range band (green tint)
                <rect
                    x=PAD_L
                    y=y_high
                    width=inner_w
                    height=band_h
                    fill="rgba(78,205,196,0.08)"
                    fill-opacity="1"
                />

                // Grid lines + Y labels
                {y_labels.iter().map(|(val, y)| {
                    view! {
                        <line x1=PAD_L x2=CHART_W-PAD_R y1=*y y2=*y stroke="rgba(255,255,255,0.06)" stroke-width="1"/>
                        <text x=PAD_L-6.0 y=*y+4.0 text-anchor="end" font-size="11" fill="#7a8599">
                            {format!("{:.0}", val)}
                        </text>
                    }
                }).collect::<Vec<_>>()}

                // High/Low threshold lines
                <line
                    x1=PAD_L x2=CHART_W-PAD_R
                    y1=y_high y2=y_high
                    stroke="#e05c5c" stroke-width="1" stroke-dasharray="4,3" opacity="0.6"
                />
                <line
                    x1=PAD_L x2=CHART_W-PAD_R
                    y1=y_low y2=y_low
                    stroke="#e0a84b" stroke-width="1" stroke-dasharray="4,3" opacity="0.6"
                />

                // Data polyline
                <polyline
                    points=polyline_pts
                    fill="none"
                    stroke="#c8a44e"
                    stroke-width="2"
                    stroke-linejoin="round"
                    stroke-linecap="round"
                />

                // Data point circles
                {circles.iter().map(|(x, y, _val, color)| {
                    view! {
                        <circle cx=*x cy=*y r="3.5" fill=*color stroke="#0c1117" stroke-width="1.5"/>
                    }
                }).collect::<Vec<_>>()}

                // Axes
                <line x1=PAD_L x2=PAD_L y1=PAD_T y2=CHART_H-PAD_B stroke="rgba(255,255,255,0.1)" stroke-width="1"/>
                <line x1=PAD_L x2=CHART_W-PAD_R y1=CHART_H-PAD_B y2=CHART_H-PAD_B stroke="rgba(255,255,255,0.1)" stroke-width="1"/>

                // Legend
                <rect x=PAD_L y=CHART_H-4.0 width="12" height="6" fill="rgba(78,205,196,0.15)" stroke="#4ecdc4" stroke-width="0.5"/>
                <text x=PAD_L+16.0 y=CHART_H+2.0 font-size="10" fill="#7a8599">"目标范围 3.9–10.0 mmol/L"</text>
            </svg>
        </div>
    }
    .into_view()
}

fn parse_ts(ts: &str) -> Option<i64> {
    parse_timestamp_seconds(ts)
}
