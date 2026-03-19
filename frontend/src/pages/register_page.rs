use crate::api::auth::register;
use crate::state::auth_state::AuthState;
use leptos::*;
use leptos_router::*;

#[component]
pub fn RegisterPage() -> impl IntoView {
    let auth = use_context::<AuthState>().expect("AuthState context missing");
    let navigate = use_navigate();

    let username = create_rw_signal(String::new());
    let password = create_rw_signal(String::new());
    let confirm_password = create_rw_signal(String::new());
    let error = create_rw_signal(Option::<String>::None);
    let loading = create_rw_signal(false);

    let on_submit = move |ev: web_sys::SubmitEvent| {
        ev.prevent_default();
        let u = username.get();
        let p = password.get();
        let cp = confirm_password.get();
        if u.is_empty() || p.is_empty() {
            error.set(Some("请填写所有字段".to_string()));
            return;
        }
        if p != cp {
            error.set(Some("两次密码输入不一致".to_string()));
            return;
        }
        if p.len() < 6 {
            error.set(Some("密码至少需要6个字符".to_string()));
            return;
        }
        error.set(None);
        loading.set(true);
        let auth = auth.clone();
        let navigate = navigate.clone();
        spawn_local(async move {
            match register(u, p).await {
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
                <h1>"创建账号"</h1>
                <p class="subtitle">"注册血糖网关管理系统"</p>

                <form on:submit=on_submit>
                    <div class="form-group">
                        <label for="username">"用户名"</label>
                        <input
                            id="username"
                            type="text"
                            placeholder="请设置用户名"
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
                            placeholder="至少6个字符"
                            prop:value=move || password.get()
                            on:input=move |ev| password.set(event_target_value(&ev))
                            autocomplete="new-password"
                        />
                    </div>
                    <div class="form-group">
                        <label for="confirm-password">"确认密码"</label>
                        <input
                            id="confirm-password"
                            type="password"
                            placeholder="再次输入密码"
                            prop:value=move || confirm_password.get()
                            on:input=move |ev| confirm_password.set(event_target_value(&ev))
                            autocomplete="new-password"
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
                        {move || if loading.get() { "注册中..." } else { "注 册" }}
                    </button>
                </form>

                <p class="auth-footer">
                    "已有账号？"
                    <A href="/login">"立即登录"</A>
                </p>
            </div>
        </div>
    }
}
