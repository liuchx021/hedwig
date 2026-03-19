use crate::api::is_auth_error;
use crate::api::vendors::{connect_login, connect_token};
use crate::models::vendor::VendorType;
use crate::state::auth_state::AuthState;
use leptos::*;
use leptos_router::*;

const VENDOR_OTTAI: &str = "OTTAI";
const VENDOR_SISENSING: &str = "SISENSING";
const AUTH_MODE_TOKEN: &str = "token";
const AUTH_MODE_LOGIN: &str = "login";

fn mode_instructions(vendor: &str, auth_mode: &str) -> &'static str {
    match (vendor, auth_mode) {
        (VENDOR_SISENSING, AUTH_MODE_LOGIN) => VendorType::Sisensing.login_instructions(),
        (VENDOR_OTTAI, _) => VendorType::Ottai.token_instructions(),
        _ => VendorType::Sisensing.token_instructions(),
    }
}

fn mode_title(vendor: &str, auth_mode: &str) -> &'static str {
    match (vendor, auth_mode) {
        (VENDOR_SISENSING, AUTH_MODE_LOGIN) => "硅基账号密码直连",
        (VENDOR_OTTAI, _) => "欧泰 access token 接入",
        _ => "硅基 access token 接入",
    }
}

fn mode_summary(vendor: &str, auth_mode: &str) -> &'static str {
    match (vendor, auth_mode) {
        (VENDOR_SISENSING, AUTH_MODE_LOGIN) => {
            "推荐方式。输入硅基轻享注册手机号和密码，系统会自动完成登录、校验 token 与同步监测对象。"
        }
        (VENDOR_OTTAI, _) => {
            "欧泰暂不支持账号密码直连，需要从微信小程序抓取 access token 后粘贴到这里。"
        }
        _ => {
            "适合已经获取硅基 access token 的场景。系统会先校验令牌，再自动初始化连接信息。"
        }
    }
}

#[component]
pub fn VendorConnectPage() -> impl IntoView {
    let auth = use_context::<AuthState>().expect("AuthState context missing");
    let navigate = use_navigate();

    if !auth.is_authenticated() {
        navigate("/login", Default::default());
        return view! { <></> }.into_view();
    }

    let vendor_type = create_rw_signal(VENDOR_OTTAI.to_string());
    let auth_mode = create_rw_signal(AUTH_MODE_TOKEN.to_string());
    let token_value = create_rw_signal(String::new());
    let vendor_username = create_rw_signal(String::new());
    let vendor_password = create_rw_signal(String::new());
    let loading = create_rw_signal(false);
    let error = create_rw_signal(Option::<String>::None);
    let success = create_rw_signal(Option::<String>::None);

    let submit_label = move || {
        let using_password_login =
            vendor_type.get() == VENDOR_SISENSING && auth_mode.get() == AUTH_MODE_LOGIN;
        if loading.get() {
            if using_password_login {
                "登录并连接中..."
            } else {
                "验证并连接中..."
            }
        } else if using_password_login {
            "登录并连接"
        } else {
            "验证并连接"
        }
    };

    let on_submit = {
        let auth_token = auth.get_token().unwrap_or_default();
        let navigate = navigate.clone();
        let auth = auth.clone();
        move |ev: web_sys::SubmitEvent| {
            ev.prevent_default();
            let vt = vendor_type.get();
            let mode = auth_mode.get();
            let tv = token_value.get();
            let vu = vendor_username.get();
            let vp = vendor_password.get();

            if vt == VENDOR_SISENSING && mode == AUTH_MODE_LOGIN {
                if vu.trim().is_empty() || vp.trim().is_empty() {
                    error.set(Some("请输入硅基账号（手机号）和密码".to_string()));
                    return;
                }
            } else if tv.trim().is_empty() {
                error.set(Some("请输入访问令牌".to_string()));
                return;
            }

            error.set(None);
            success.set(None);
            loading.set(true);
            let auth_token = auth_token.clone();
            let navigate = navigate.clone();
            let auth = auth.clone();
            spawn_local(async move {
                let result = if vt == VENDOR_SISENSING && mode == AUTH_MODE_LOGIN {
                    connect_login(vt, vu, vp, &auth_token).await
                } else {
                    connect_token(vt, tv, &auth_token).await
                };

                match result {
                    Ok(_) => {
                        success.set(Some("连接成功，正在返回仪表盘……".to_string()));
                        loading.set(false);
                        let navigate = navigate.clone();
                        spawn_local(async move {
                            gloo_timers::future::TimeoutFuture::new(1200).await;
                            navigate("/", Default::default());
                        });
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

    view! {
        <div class="page-container connect-shell">
            <section class="connect-hero">
                <div>
                    <span class="connect-eyebrow">"Device Onboarding"</span>
                    <h1 class="section-title">"连接血糖监测设备"</h1>
                    <p class="section-subtitle">
                        "按厂商选择最合适的鉴权方式，系统会自动完成令牌校验、监测对象同步与后续拉取准备。"
                    </p>
                </div>
                <div class="connect-stat-grid">
                    <div class="connect-stat">
                        <span>"支持厂商"</span>
                        <strong>"2"</strong>
                        <small>"欧泰 / 硅基"</small>
                    </div>
                    <div class="connect-stat">
                        <span>"硅基方式"</span>
                        <strong>"双模式"</strong>
                        <small>"账号密码 / token"</small>
                    </div>
                    <div class="connect-stat">
                        <span>"连接后动作"</span>
                        <strong>"自动同步"</strong>
                        <small>"验证并发现监测对象"</small>
                    </div>
                </div>
            </section>

            <div class="connect-layout">
                <section class="card connect-card">
                    <A href="/" class="connect-back-link">"← 返回仪表盘"</A>

                    <div class="provider-switch">
                        <button
                            type="button"
                            class=move || {
                                if vendor_type.get() == VENDOR_OTTAI {
                                    "provider-option is-active"
                                } else {
                                    "provider-option"
                                }
                            }
                            on:click=move |_| {
                                vendor_type.set(VENDOR_OTTAI.to_string());
                                auth_mode.set(AUTH_MODE_TOKEN.to_string());
                                error.set(None);
                                success.set(None);
                            }
                        >
                            <strong>"欧泰（Ottai）"</strong>
                            <span>"从微信小程序抓包获取 access token"</span>
                        </button>

                        <button
                            type="button"
                            class=move || {
                                if vendor_type.get() == VENDOR_SISENSING {
                                    "provider-option is-active"
                                } else {
                                    "provider-option"
                                }
                            }
                            on:click=move |_| {
                                vendor_type.set(VENDOR_SISENSING.to_string());
                                auth_mode.set(AUTH_MODE_LOGIN.to_string());
                                error.set(None);
                                success.set(None);
                            }
                        >
                            <strong>"硅基轻享（SiSensing）"</strong>
                            <span>"支持账号密码直连，也支持手动粘贴 token"</span>
                        </button>
                    </div>

                    {move || {
                        if vendor_type.get() == VENDOR_SISENSING {
                            view! {
                                <div class="form-group">
                                    <label>"连接方式"</label>
                                    <div class="auth-toggle">
                                        <button
                                            type="button"
                                            class=move || {
                                                if auth_mode.get() == AUTH_MODE_LOGIN {
                                                    "auth-chip is-active"
                                                } else {
                                                    "auth-chip"
                                                }
                                            }
                                            on:click=move |_| auth_mode.set(AUTH_MODE_LOGIN.to_string())
                                        >
                                            "账号密码登录"
                                        </button>
                                        <button
                                            type="button"
                                            class=move || {
                                                if auth_mode.get() == AUTH_MODE_TOKEN {
                                                    "auth-chip is-active"
                                                } else {
                                                    "auth-chip"
                                                }
                                            }
                                            on:click=move |_| auth_mode.set(AUTH_MODE_TOKEN.to_string())
                                        >
                                            "访问令牌"
                                        </button>
                                    </div>
                                    <p class="field-hint">
                                        "硅基推荐直接使用账号密码登录；如果你已经拿到 access token，也可以切换到令牌模式。"
                                    </p>
                                </div>
                            }
                            .into_view()
                        } else {
                            view! { <></> }.into_view()
                        }
                    }}

                    <div class="connect-intro">
                        <span class="connect-intro-title">
                            {move || mode_title(&vendor_type.get(), &auth_mode.get())}
                        </span>
                        <p class="connect-intro-copy">
                            {move || mode_summary(&vendor_type.get(), &auth_mode.get())}
                        </p>
                        <p class="field-hint">
                            {move || mode_instructions(&vendor_type.get(), &auth_mode.get())}
                        </p>
                    </div>

                    <form on:submit=on_submit>
                        {move || {
                            if vendor_type.get() == VENDOR_SISENSING
                                && auth_mode.get() == AUTH_MODE_LOGIN
                            {
                                view! {
                                    <>
                                        <div class="form-group">
                                            <label for="vendor-username">"账号（手机号）"</label>
                                            <input
                                                id="vendor-username"
                                                type="text"
                                                placeholder="请输入硅基轻享注册手机号"
                                                prop:value=move || vendor_username.get()
                                                on:input=move |ev| vendor_username.set(event_target_value(&ev))
                                                autocomplete="username"
                                            />
                                        </div>
                                        <div class="form-group">
                                            <label for="vendor-password">"密码"</label>
                                            <input
                                                id="vendor-password"
                                                type="password"
                                                placeholder="请输入硅基轻享登录密码"
                                                prop:value=move || vendor_password.get()
                                                on:input=move |ev| vendor_password.set(event_target_value(&ev))
                                                autocomplete="current-password"
                                            />
                                            <p class="field-hint">
                                                "登录成功后系统会自动换取并保存厂商访问令牌，你不需要手动抓包。"
                                            </p>
                                        </div>
                                    </>
                                }
                                .into_view()
                            } else {
                                view! {
                                    <div class="form-group">
                                        <label for="vendor-token">"访问令牌"</label>
                                        <textarea
                                            id="vendor-token"
                                            class="token-input"
                                            rows="6"
                                            placeholder="在此粘贴 access token……"
                                            prop:value=move || token_value.get()
                                            on:input=move |ev| token_value.set(event_target_value(&ev))
                                        ></textarea>
                                    </div>
                                }
                                .into_view()
                            }
                        }}

                        {move || error.get().map(|e| view! {
                            <p class="error-text">{e}</p>
                        })}
                        {move || success.get().map(|s| view! {
                            <p class="success-text">{s}</p>
                        })}

                        <div class="connect-actions">
                            <button
                                type="submit"
                                class="btn-primary"
                                disabled=move || loading.get()
                            >
                                {submit_label}
                            </button>
                            <A href="/" class="connect-secondary-link">
                                <button type="button" class="btn-secondary">
                                    "暂不连接"
                                </button>
                            </A>
                        </div>
                    </form>
                </section>

                <aside class="connect-side">
                    <div class="card connect-panel">
                        <h2>"接入规则"</h2>
                        <ul class="connect-note-list">
                            <li>
                                <span class="connect-note-kicker">"硅基轻享"</span>
                                <strong>"优先使用账号密码登录"</strong>
                                <p>"现在可以直接输入硅基注册手机号与密码，系统会自动换取 token 并完成设备绑定。"</p>
                            </li>
                            <li>
                                <span class="connect-note-kicker">"欧泰"</span>
                                <strong>"仅支持 access token"</strong>
                                <p>"欧泰仍然需要从微信小程序抓包拿 token，暂不支持账号密码直连。"</p>
                            </li>
                            <li>
                                <span class="connect-note-kicker">"连接完成后"</span>
                                <strong>"自动发现监测对象"</strong>
                                <p>"系统会验证身份、同步亲友列表，并把首个可用监测对象绑定到仪表盘。"</p>
                            </li>
                        </ul>
                    </div>

                    <div class="card connect-panel">
                        <h2>"当前选择"</h2>
                        <div class="connect-current">
                            <strong>{move || {
                                if vendor_type.get() == VENDOR_SISENSING {
                                    "硅基轻享（SiSensing）"
                                } else {
                                    "欧泰（Ottai）"
                                }
                            }}</strong>
                            <p>{move || mode_summary(&vendor_type.get(), &auth_mode.get())}</p>
                        </div>
                    </div>
                </aside>
            </div>
        </div>
    }
    .into_view()
}
