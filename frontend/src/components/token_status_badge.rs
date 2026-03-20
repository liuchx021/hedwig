use crate::models::vendor::TokenStatus;
use leptos::*;

#[component]
pub fn TokenStatusBadge(status: TokenStatus) -> impl IntoView {
    let (label, css_class, dot_color) = match &status {
        TokenStatus::Active => ("正常", "badge badge-active", "#4ecdc4"),
        TokenStatus::ExpiringSoon => ("即将过期", "badge badge-expiring", "#e0a84b"),
        TokenStatus::Expired => ("已过期", "badge badge-expired", "#e05c5c"),
    };

    view! {
        <span class=css_class>
            <span style=format!("width:6px;height:6px;border-radius:50%;background:{};display:inline-block;", dot_color)></span>
            {label}
        </span>
    }
}
