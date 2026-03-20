use crate::api::auth::login;
use crate::state::auth_state::AuthState;
use leptos::*;
use leptos_router::*;

#[component]
pub fn LoginPage() -> impl IntoView {
    let auth = use_context::<AuthState>().expect("AuthState context missing");
    let navigate = use_navigate();

    let username = create_rw_signal(String::new());
    let password = create_rw_signal(String::new());
    let error = create_rw_signal(Option::<String>::None);
    let loading = create_rw_signal(false);

    let on_submit = move |ev: web_sys::SubmitEvent| {
        ev.prevent_default();
        let u = username.get();
        let p = password.get();
        if u.is_empty() || p.is_empty() {
            error.set(Some("请输入用户名和密码".to_string()));
            return;
        }
        error.set(None);
        loading.set(true);
        let auth = auth.clone();
        let navigate = navigate.clone();
        spawn_local(async move {
            match login(u, p).await {
                Ok(resp) => {
                    auth.login(resp.token, resp.username);
                    navigate("/", Default::default());
                }
                Err(e) => {
                    error.set(Some(e));
                    loading.set(false);
                }
            }
        });
    };

    view! {
        <div class="auth-page">
            <div class="auth-card">
                <div class="auth-brand">
                    <div class="auth-brand-icon">{"\u{1F989}"}</div>
                    <div class="auth-brand-name">"Hedwig"</div>
                    <div class="auth-brand-desc">"CGM \u{00b7} 血糖数据网关"</div>
                </div>
                <h1>"欢迎回来"</h1>
                <p class="subtitle">"登录 Hedwig 管理系统"</p>

                <form on:submit=on_submit>
                    <div class="form-group">
                        <label for="username">"用户名"</label>
                        <input
                            id="username"
                            type="text"
                            placeholder="请输入用户名"
                            prop:value=move || username.get()
                            on:input=move |ev| username.set(event_target_value(&ev))
                            autocomplete="username"
                        />
                    </div>
                    <div class="form-group">
                        <label for="password">"密码"</label>
                        <input
                            id="password"
                            type="password"
                            placeholder="请输入密码"
                            prop:value=move || password.get()
                            on:input=move |ev| password.set(event_target_value(&ev))
                            autocomplete="current-password"
                        />
                    </div>

                    {move || error.get().map(|e| view! {
                        <p class="error-text">{e}</p>
                    })}

                    <button
                        type="submit"
                        class="btn-primary"
                        style="width:100%;margin-top:8px;padding:10px"
                        disabled=move || loading.get()
                    >
                        {move || if loading.get() { "登录中..." } else { "登 录" }}
                    </button>
                </form>

                <p class="auth-footer">
                    "没有账号？"
                    <A href="/register">"立即注册"</A>
                </p>
            </div>
        </div>
    }
}
