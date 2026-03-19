use crate::components::token_status_badge::TokenStatusBadge;
use crate::models::vendor::VendorConnection;
use crate::utils::time::{format_remaining_time, format_shanghai_datetime};
use leptos::*;
use leptos_router::*;

#[component]
pub fn VendorCard(connection: VendorConnection, on_delete: Callback<String>) -> impl IntoView {
    let id = connection.id;
    let id_str = id.to_string();
    let id_for_delete = id_str.clone();
    let vendor_name = connection.vendor_type.display_name();
    let vendor_user_label = connection
        .vendor_user_id
        .clone()
        .unwrap_or_else(|| "未知".to_string());
    let primary_subject_name = connection
        .primary_subject_name
        .clone()
        .unwrap_or_else(|| "未发现监测对象".to_string());
    let detail_href = connection
        .primary_subject_id
        .map(|id| format!("/glucose/{}", id));
    let token_status = connection.token_status.clone();

    let last_sync_display = connection
        .last_synced_at
        .as_deref()
        .map(format_shanghai_datetime)
        .unwrap_or_else(|| "从未同步".to_string());

    let sensor_remaining = connection
        .sensor_expires_at
        .as_deref()
        .map(format_remaining_time);

    view! {
        <div class="vendor-card">
            <div class="vendor-card-header">
                <span class="vendor-name">{vendor_name}</span>
                <TokenStatusBadge status=token_status />
            </div>
            <div class="vendor-meta">
                <span>"厂商账号："</span>
                <strong>{vendor_user_label}</strong>
            </div>
            <div class="vendor-meta">
                <span>"监测对象："</span>
                <strong>{primary_subject_name}</strong>
            </div>
            {if let Some(ref remaining) = sensor_remaining {
                let is_expired = remaining == "已过期";
                let style = if is_expired { "color:#ef4444" } else { "color:#059669" };
                view! {
                    <div class="vendor-meta">
                        <span>"传感器剩余有效期："</span>
                        <strong style=style>{remaining.clone()}</strong>
                    </div>
                }.into_view()
            } else {
                view! { <></> }.into_view()
            }}
            <div class="vendor-meta">"最后同步："{last_sync_display}</div>
            <div class="vendor-card-actions">
                <div style="flex:1">
                    {if let Some(href) = detail_href {
                        view! {
                            <A href=href>
                                <button class="btn-primary" style="width:100%">"查看详情"</button>
                            </A>
                        }.into_view()
                    } else {
                        view! {
                            <button class="btn-secondary" style="width:100%" disabled=true>"暂无详情"</button>
                        }.into_view()
                    }}
                </div>
                <button
                    class="btn-danger"
                    on:click=move |_| on_delete.call(id_for_delete.clone())
                >
                    "断开"
                </button>
            </div>
        </div>
    }
}
