use crate::api::is_auth_error;
use crate::api::vendors::{delete_connection, get_connections};
use crate::components::alert_banner::AlertBanner;
use crate::components::vendor_card::VendorCard;
use crate::models::vendor::VendorConnection;
use crate::state::auth_state::AuthState;
use leptos::*;
use leptos_router::*;

#[component]
pub fn DashboardPage() -> impl IntoView {
    let auth = use_context::<AuthState>().expect("AuthState context missing");
    let navigate = use_navigate();

    // Redirect to login if not authenticated
    if !auth.is_authenticated() {
        navigate("/login", Default::default());
        return view! { <></> }.into_view();
    }

    let connections: RwSignal<Vec<VendorConnection>> = create_rw_signal(Vec::new());
    let loading = create_rw_signal(true);
    let error = create_rw_signal(Option::<String>::None);

    // Initial load
    let token = auth.get_token().unwrap_or_default();
    {
        let token = token.clone();
        let auth = auth.clone();
        let navigate = navigate.clone();
        spawn_local(async move {
            match get_connections(&token).await {
                Ok(conns) => {
                    connections.set(conns);
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

    let on_delete = {
        let token = token.clone();
        let auth = auth.clone();
        let navigate = navigate.clone();
        Callback::new(move |id: String| {
            let token = token.clone();
            let auth = auth.clone();
            let navigate = navigate.clone();
            spawn_local(async move {
                if let Err(e) = delete_connection(&id, &token).await {
                    if is_auth_error(&e) {
                        auth.logout();
                        navigate("/login", Default::default());
                    } else {
                        web_sys::console::error_1(&wasm_bindgen::JsValue::from_str(&e));
                    }
                } else {
                    // Refresh connections
                    match get_connections(&token).await {
                        Ok(conns) => connections.set(conns),
                        Err(e) => {
                            if is_auth_error(&e) {
                                auth.logout();
                                navigate("/login", Default::default());
                            } else {
                                error.set(Some(e));
                            }
                        }
                    }
                }
            });
        })
    };

    view! {
        <div class="page-container">
            <div class="dashboard-header">
                <div>
                    <h1 class="section-title">"仪表盘"</h1>
                    <p class="section-subtitle">"管理您的血糖监测设备连接"</p>
                </div>
                <A href="/connect">
                    <button class="btn-primary">"+ 添加设备"</button>
                </A>
            </div>

            {move || {
                let conns = connections.get();
                if !conns.is_empty() {
                    view! { <AlertBanner connections=conns /> }.into_view()
                } else {
                    view! { <></> }.into_view()
                }
            }}

            {move || if loading.get() {
                view! {
                    <div class="loading">
                        <div class="spinner"></div>
                        "正在加载..."
                    </div>
                }.into_view()
            } else if let Some(e) = error.get() {
                view! {
                    <div class="alert-banner alert-danger">
                        <span>"加载失败："</span><span>{e}</span>
                    </div>
                }.into_view()
            } else {
                let conns = connections.get();
                if conns.is_empty() {
                    view! {
                        <div class="card" style="text-align:center;padding:48px">
                            <p style="color:#64748b;font-size:16px;margin-bottom:16px">"还没有连接任何设备"</p>
                            <A href="/connect">
                                <button class="btn-primary" style="padding:10px 24px">"添加第一个设备"</button>
                            </A>
                        </div>
                    }.into_view()
                } else {
                    view! {
                        <div class="vendor-grid">
                            {conns.into_iter().map(|conn| {
                                view! {
                                    <VendorCard connection=conn on_delete=on_delete />
                                }
                            }).collect::<Vec<_>>()}
                        </div>
                    }.into_view()
                }
            }}
        </div>
    }
    .into_view()
}
