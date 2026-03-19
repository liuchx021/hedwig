use crate::api::glucose::{get_history, sync_history};
use crate::api::is_auth_error;
use crate::components::glucose_chart::GlucoseChart;
use crate::components::glucose_table::GlucoseTable;
use crate::models::glucose::GlucoseReading;
use crate::state::auth_state::AuthState;
use crate::utils::time::parse_timestamp_ms;
use leptos::*;
use leptos_router::*;

#[component]
pub fn GlucoseDetailPage() -> impl IntoView {
    let auth = use_context::<AuthState>().expect("AuthState context missing");
    let navigate = use_navigate();
    let params = use_params_map();

    if !auth.is_authenticated() {
        navigate("/login", Default::default());
        return view! { <></> }.into_view();
    }

    let subject_id = move || params.get().get("id").cloned().unwrap_or_default();

    let hours_back = create_rw_signal(24u32);
    let readings: RwSignal<Vec<GlucoseReading>> = create_rw_signal(Vec::new());
    let loading = create_rw_signal(true);
    let error = create_rw_signal(Option::<String>::None);
    let syncing = create_rw_signal(false);
    let sync_msg = create_rw_signal(Option::<String>::None);

    let auth_token = auth.get_token().unwrap_or_default();

    // Reactive fetch: re-runs when hours_back changes
    let do_fetch = {
        let token = auth_token.clone();
        let auth = auth.clone();
        let navigate = navigate.clone();
        move || {
            let sid = subject_id();
            let token = token.clone();
            let auth = auth.clone();
            let navigate = navigate.clone();
            let h = hours_back.get();
            loading.set(true);
            error.set(None);
            spawn_local(async move {
                match get_history(&sid, h, &token).await {
                    Ok(data) => {
                        let now_ms = js_sys::Date::now();
                        let cutoff_ms = now_ms - (h as f64 * 60.0 * 60.0 * 1000.0);
                        let filtered = data
                            .into_iter()
                            .filter(|reading| {
                                parse_timestamp_ms(&reading.reading_time)
                                    .map(|timestamp| timestamp >= cutoff_ms)
                                    .unwrap_or(false)
                            })
                            .collect::<Vec<_>>();
                        readings.set(filtered);
                        loading.set(false);
                    }
                    Err(e) => {
                        if is_auth_error(&e) {
                            auth.logout();
                            navigate("/login", Default::default());
                        } else {
                            error.set(Some(e));
                        }
                        loading.set(false);
                    }
                }
            });
        }
    };

    // Initial fetch
    do_fetch();

    let on_hours_change = {
        let do_fetch = do_fetch.clone();
        move |ev: web_sys::Event| {
            let val: u32 = event_target_value(&ev).parse().unwrap_or(24);
            hours_back.set(val);
            do_fetch();
        }
    };

    let on_refresh = {
        let do_fetch = do_fetch.clone();
        move |_: web_sys::MouseEvent| do_fetch()
    };

    let on_sync = {
        let do_fetch = do_fetch.clone();
        let token = auth_token.clone();
        let auth = auth.clone();
        let navigate = navigate.clone();
        move |_: web_sys::MouseEvent| {
            let sid = subject_id();
            let token = token.clone();
            let auth = auth.clone();
            let navigate = navigate.clone();
            let do_fetch = do_fetch.clone();
            syncing.set(true);
            sync_msg.set(None);
            error.set(None);
            spawn_local(async move {
                match sync_history(&sid, &token).await {
                    Ok(result) => {
                        sync_msg.set(Some(format!(
                            "同步成功：已同步 {} 条数据",
                            result.synced_count
                        )));
                        syncing.set(false);
                        do_fetch();
                    }
                    Err(e) => {
                        if is_auth_error(&e) {
                            auth.logout();
                            navigate("/login", Default::default());
                        } else {
                            error.set(Some(format!("同步失败：{}", e)));
                        }
                        syncing.set(false);
                    }
                }
            });
        }
    };

    view! {
        <div class="page-container">
            <div style="margin-bottom:20px">
                <A href="/"><span style="color:#64748b;font-size:14px">"← 返回仪表盘"</span></A>
            </div>

            <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:20px;flex-wrap:wrap;gap:12px">
                <div>
                    <h1 class="section-title">
                        "血糖详情 — "
                        {move || subject_id()}
                    </h1>
                    <p class="section-subtitle">"查看历史血糖值变化趋势"</p>
                </div>
                <div style="display:flex;align-items:center;gap:12px">
                    <label style="margin:0;white-space:nowrap">"时间范围："</label>
                    <select
                        style="width:auto"
                        prop:value=move || hours_back.get().to_string()
                        on:change=on_hours_change
                    >
                        <option value="6">"最近6小时"</option>
                        <option value="12">"最近12小时"</option>
                        <option value="24" selected>"最近24小时"</option>
                        <option value="48">"最近48小时"</option>
                        <option value="72">"最近72小时"</option>
                    </select>
                    <button class="btn-secondary" on:click=on_refresh>"刷新"</button>
                    <button
                        class="btn-secondary"
                        on:click=on_sync
                        disabled=move || syncing.get()
                    >
                        {move || if syncing.get() { "同步中..." } else { "同步最近血糖数据" }}
                    </button>
                </div>
            </div>

            {move || sync_msg.get().map(|msg| view! {
                <div class="alert-banner alert-success">{msg}</div>
            })}

            {move || if loading.get() {
                view! {
                    <div class="loading">
                        <div class="spinner"></div>
                        "正在加载血糖数据..."
                    </div>
                }.into_view()
            } else if let Some(e) = error.get() {
                view! {
                    <div class="alert-banner alert-danger">
                        "加载失败："{e}
                    </div>
                }.into_view()
            } else {
                let data = readings.get();
                let count = data.len();
                let avg = if count > 0 {
                    let sum: f64 = data.iter().map(|r| r.glucose_mmol).sum();
                    Some(sum / count as f64)
                } else {
                    None
                };
                let latest_val = data
                    .iter()
                    .max_by(|a, b| a.reading_time.cmp(&b.reading_time))
                    .map(|r| (r.glucose_mmol, r.glucose_css_class()));

                view! {
                    <div style="display:flex;flex-direction:column;gap:20px">
                        // Summary stats
                        <div style="display:flex;gap:16px;flex-wrap:wrap">
                            <div class="card" style="flex:1;min-width:120px;text-align:center">
                                <div style="font-size:11px;color:#64748b;margin-bottom:4px">"读数条数"</div>
                                <div style="font-size:24px;font-weight:700;color:#2563eb">{count}</div>
                            </div>
                            {avg.map(|a| view! {
                                <div class="card" style="flex:1;min-width:120px;text-align:center">
                                    <div style="font-size:11px;color:#64748b;margin-bottom:4px">"平均血糖值"</div>
                                    <div style="font-size:24px;font-weight:700;color:#16a34a">
                                        {format!("{:.1}", a)}
                                        <span style="font-size:13px;font-weight:400;color:#64748b">" mmol/L"</span>
                                    </div>
                                </div>
                            })}
                            {latest_val.map(|(val, css)| view! {
                                <div class="card" style="flex:1;min-width:120px;text-align:center">
                                    <div style="font-size:11px;color:#64748b;margin-bottom:4px">"最新血糖值"</div>
                                    <div style="font-size:24px;font-weight:700" class=css>
                                        {format!("{:.1}", val)}
                                        <span style="font-size:13px;font-weight:400;color:#64748b">" mmol/L"</span>
                                    </div>
                                </div>
                            })}
                        </div>

                        // Chart
                        <GlucoseChart readings=data.clone() />

                        // Table
                        <div class="card" style="padding:0;overflow:hidden">
                            <div style="padding:16px 16px 12px;border-bottom:1px solid #e2e8f0">
                                <strong>"数据明细"</strong>
                                <span style="color:#64748b;font-size:13px;margin-left:8px">
                                    {format!("共 {} 条记录", count)}
                                </span>
                            </div>
                            <GlucoseTable readings=data />
                        </div>
                    </div>
                }.into_view()
            }}
        </div>
    }
    .into_view()
}
