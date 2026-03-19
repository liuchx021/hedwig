use crate::api::{delete_empty, get_json, post_json, ApiResult};
use crate::models::vendor::{ConnectLoginRequest, ConnectTokenRequest, VendorConnection};

pub async fn get_connections(token: &str) -> ApiResult<Vec<VendorConnection>> {
    get_json("/api/vendors/connections", Some(token)).await
}

pub async fn connect_token(
    vendor_type: String,
    token_value: String,
    auth_token: &str,
) -> ApiResult<VendorConnection> {
    post_json(
        "/api/vendors/connections/token",
        &ConnectTokenRequest {
            vendor_type,
            access_token: token_value,
        },
        Some(auth_token),
    )
    .await
}

pub async fn connect_login(
    vendor_type: String,
    username: String,
    password: String,
    auth_token: &str,
) -> ApiResult<VendorConnection> {
    post_json(
        "/api/vendors/connections/login",
        &ConnectLoginRequest {
            vendor_type,
            username,
            password,
        },
        Some(auth_token),
    )
    .await
}

pub async fn delete_connection(id: &str, token: &str) -> ApiResult<()> {
    delete_empty(&format!("/api/vendors/connections/{}", id), Some(token)).await
}
