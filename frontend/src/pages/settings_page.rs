use crate::api::is_auth_error;
use crate::api::nightscout::sync;
use crate::state::auth_state::AuthState;
use leptos::*;
use leptos_router::*;

#[component]
pub fn SettingsPage() -> impl IntoView {
    let auth = use_context::<AuthState>().expect("AuthState context missing");
    let navigate = use_navigate();

    if !auth.is_authenticated() {
        navigate("/login", Default::default());
        return view! { <></> }.into_view();
    }

    let syncing = create_rw_signal(false);
    let error = create_rw_signal(Option::<String>::None);
    let success_msg = create_rw_signal(Option::<String>::None);

    let on_sync = {
        let token = auth.get_token().unwrap_or_default();
        let auth = auth.clone();
        let navigate = navigate.clone();
        move |_: web_sys::MouseEvent| {
            syncing.set(true);
            success_msg.set(None);
            error.set(None);
            let token = token.clone();
            let auth = auth.clone();
            let navigate = navigate.clone();
            spawn_local(async move {
                match sync(&token).await {
                    Ok(resp) => {
                        let msg = format!("同步成功，共上传 {} 条数据", resp.pushed.unwrap_or(0));
                        success_msg.set(Some(msg));
                        syncing.set(false);
                    }
                    Err(e) => {
                        if is_auth_error(&e) {
                            auth.logout();
                            navigate("/login", Default::default());
                        } else {
                            error.set(Some(e));
                        }
                        syncing.set(false);
                    }
                }
            });
        }
    };

    view! {
        <div class="page-container" style="max-width:600px">
            <h1 class="section-title">"系统设置"</h1>
            <p class="section-subtitle">"Nightscout 同步管理"</p>

            <div class="card settings-section">
                <h2>"手动同步到 Nightscout"</h2>
                <p style="color:#64748b;font-size:13px;margin-bottom:16px">
                    "手动触发将待推送的血糖数据同步至 Nightscout（需在后端配置 Nightscout 地址和 API 密钥）"
                </p>

                {move || error.get().map(|e| view! { <p class="error-text">{e}</p> })}
                {move || success_msg.get().map(|s| view! { <p class="success-text">{s}</p> })}

                <button
                    class="btn-success"
                    style="padding:9px 20px;white-space:nowrap"
                    on:click=on_sync
                    disabled=move || syncing.get()
                >
                    {move || if syncing.get() { "同步中..." } else { "立即同步" }}
                </button>
            </div>
        </div>
    }
    .into_view()
}
