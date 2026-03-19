use crate::models::glucose::GlucoseReading;
use crate::utils::time::format_shanghai_datetime;
use leptos::*;

#[component]
pub fn GlucoseTable(readings: Vec<GlucoseReading>) -> impl IntoView {
    if readings.is_empty() {
        return view! {
            <div style="text-align:center;color:#94a3b8;padding:24px">"暂无血糖数据"</div>
        }
        .into_view();
    }

    // Show newest first
    let mut sorted = readings.clone();
    sorted.sort_by(|a, b| b.reading_time.cmp(&a.reading_time));

    view! {
        <div style="overflow-x:auto">
            <table class="glucose-table">
                <thead>
                    <tr>
                        <th>"时间"</th>
                        <th>"血糖值（mmol/L）"</th>
                        <th>"变化趋势"</th>
                    </tr>
                </thead>
                <tbody>
                    {sorted.iter().map(|r| {
                        let css = r.glucose_css_class();
                        let time_display = format_shanghai_datetime(&r.reading_time);
                        let trend_str = r.trend_direction.as_ref().map(|t| t.arrow()).unwrap_or("—");
                        let trend_css = r.trend_direction.as_ref().map(|t| t.css_class()).unwrap_or("trend-flat");
                        view! {
                            <tr>
                                <td style="white-space:nowrap">{time_display}</td>
                                <td class=css>{format!("{:.1}", r.glucose_mmol)}</td>
                                <td class=trend_css style="font-size:16px">{trend_str}</td>
                            </tr>
                        }
                    }).collect::<Vec<_>>()}
                </tbody>
            </table>
        </div>
    }
    .into_view()
}
