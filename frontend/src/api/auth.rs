use crate::api::{post_json, ApiResult};
use crate::models::user::{AuthResponse, LoginRequest, RegisterRequest};

pub async fn login(username: String, password: String) -> ApiResult<AuthResponse> {
    post_json(
        "/api/auth/login",
        &LoginRequest { username, password },
        None,
    )
    .await
}

pub async fn register(username: String, password: String) -> ApiResult<AuthResponse> {
    post_json(
        "/api/auth/register",
        &RegisterRequest { username, password },
        None,
    )
    .await
}
