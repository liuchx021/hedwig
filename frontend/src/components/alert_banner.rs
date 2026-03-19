use crate::models::vendor::{TokenStatus, VendorConnection};
use leptos::*;

#[component]
pub fn AlertBanner(connections: Vec<VendorConnection>) -> impl IntoView {
    let expired: Vec<_> = connections
        .iter()
        .filter(|c| c.token_status == TokenStatus::Expired)
        .cloned()
        .collect();
    let expiring: Vec<_> = connections
        .iter()
        .filter(|c| c.token_status == TokenStatus::ExpiringSoon)
        .cloned()
        .collect();

    view! {
        <div>
            {if !expired.is_empty() {
                let names: Vec<_> = expired.iter().map(|c| c.vendor_type.display_name()).collect();
                let msg = format!("访问令牌已过期，需要重新连接：{}", names.join("、"));
                view! {
                    <div class="alert-banner alert-danger">
                        <span style="font-size:18px">"⚠"</span>
                        <span>{msg}</span>
                    </div>
                }.into_view()
            } else {
                view! { <></> }.into_view()
            }}
            {if !expiring.is_empty() {
                let names: Vec<_> = expiring.iter().map(|c| c.vendor_type.display_name()).collect();
                let msg = format!("访问令牌即将过期，请尽快更新：{}", names.join("、"));
                view! {
                    <div class="alert-banner alert-warning">
                        <span style="font-size:18px">"⏰"</span>
                        <span>{msg}</span>
                    </div>
                }.into_view()
            } else {
                view! { <></> }.into_view()
            }}
        </div>
    }
}
