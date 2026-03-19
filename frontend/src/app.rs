use leptos::*;
use leptos_router::*;

use crate::components::nav_bar::NavBar;
use crate::pages::dashboard_page::DashboardPage;
use crate::pages::glucose_detail_page::GlucoseDetailPage;
use crate::pages::login_page::LoginPage;
use crate::pages::register_page::RegisterPage;
use crate::pages::settings_page::SettingsPage;
use crate::pages::vendor_connect_page::VendorConnectPage;
use crate::state::auth_state::AuthState;

#[component]
pub fn App() -> impl IntoView {
    // Provide global auth context
    let auth_state = AuthState::new();
    provide_context(auth_state.clone());

    view! {
        <Router>
            <AppShell />
        </Router>
    }
}

/// Inner shell that can use router hooks
#[component]
fn AppShell() -> impl IntoView {
    let location = use_location();

    let show_nav = move || {
        let path = location.pathname.get();
        path != "/login" && path != "/register"
    };

    view! {
        <Show when=show_nav>
            <NavBar />
        </Show>
        <main>
            <Routes>
                <Route path="/" view=DashboardPage />
                <Route path="/login" view=LoginPage />
                <Route path="/register" view=RegisterPage />
                <Route path="/connect" view=VendorConnectPage />
                <Route path="/glucose/:id" view=GlucoseDetailPage />
                <Route path="/settings" view=SettingsPage />
                <Route path="/*any" view=|| view! {
                    <div class="page-container" style="text-align:center;padding-top:60px">
                        <h2 style="color:#64748b">"404 — 页面不存在"</h2>
                        <br/>
                        <A href="/">"返回首页"</A>
                    </div>
                } />
            </Routes>
        </main>
    }
}
