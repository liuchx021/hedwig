mod api;
mod app;
mod components;
mod models;
mod pages;
mod state;
mod utils;

use app::App;
use leptos::*;

fn main() {
    // Set panic hook for better error messages in browser console
    console_error_panic_hook::set_once();

    mount_to_body(|| view! { <App /> })
}
