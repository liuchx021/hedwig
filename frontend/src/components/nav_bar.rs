use crate::state::auth_state::AuthState;
use leptos::*;
use leptos_router::*;

#[component]
pub fn NavBar() -> impl IntoView {
    let auth = use_context::<AuthState>().expect("AuthState context missing");
    let auth_for_logout = auth.clone();
    let auth_for_show = auth.clone();
    let auth_for_name = auth.clone();

    let on_logout = move |_| {
        auth_for_logout.logout();
        use_navigate()("/login", Default::default());
    };

    view! {
        <nav class="navbar">
            <div class="navbar-brand">
                <div class="navbar-brand-icon">{"\u{1F989}"}</div>
                <span class="navbar-brand-text">"Hedwig"</span>
                <span class="navbar-brand-sub">"CGM Gateway"</span>
            </div>
            <div class="navbar-links">
                <A href="/" class="nav-link" active_class="active">"仪表盘"</A>
                <A href="/connect" class="nav-link" active_class="active">"连接设备"</A>
                <A href="/settings" class="nav-link" active_class="active">"设置"</A>
                <Show when=move || auth_for_show.is_authenticated()>
                    <span style="margin-left:8px;color:#7a8599;font-size:13px">
                        {move || auth_for_name.username.get().unwrap_or_default()}
                    </span>
                    <button
                        class="btn-secondary"
                        style="margin-left:8px;padding:5px 12px;font-size:13px"
                        on:click=on_logout.clone()
                    >
                        "退出"
                    </button>
                </Show>
            </div>
        </nav>
    }
}
