use gloo_storage::{LocalStorage, Storage};
use leptos::*;

const TOKEN_KEY: &str = "hedwig_token";
const USERNAME_KEY: &str = "hedwig_username";

#[derive(Clone, Debug)]
pub struct AuthState {
    pub token: RwSignal<Option<String>>,
    pub username: RwSignal<Option<String>>,
}

impl AuthState {
    pub fn new() -> Self {
        // Restore from localStorage on init
        let token: Option<String> = LocalStorage::get(TOKEN_KEY).ok();
        let username: Option<String> = LocalStorage::get(USERNAME_KEY).ok();
        Self {
            token: create_rw_signal(token),
            username: create_rw_signal(username),
        }
    }

    pub fn is_authenticated(&self) -> bool {
        self.token.get().is_some()
    }

    pub fn login(&self, token: String, username: String) {
        let _ = LocalStorage::set(TOKEN_KEY, &token);
        let _ = LocalStorage::set(USERNAME_KEY, &username);
        self.token.set(Some(token));
        self.username.set(Some(username));
    }

    pub fn logout(&self) {
        LocalStorage::delete(TOKEN_KEY);
        LocalStorage::delete(USERNAME_KEY);
        self.token.set(None);
        self.username.set(None);
    }

    pub fn get_token(&self) -> Option<String> {
        self.token.get()
    }
}
