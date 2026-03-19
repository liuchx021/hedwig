use crate::api::{post_empty, ApiResult};
use serde::Deserialize;

#[derive(Debug, Deserialize)]
pub struct SyncResponse {
    pub pushed: Option<u32>,
}

pub async fn sync(token: &str) -> ApiResult<SyncResponse> {
    post_empty("/api/nightscout/sync", Some(token)).await
}
