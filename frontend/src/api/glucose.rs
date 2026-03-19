use crate::api::{get_json, post_empty, ApiResult};
use crate::models::glucose::{GlucoseReading, SyncResult};

pub async fn get_history(
    subject_id: &str,
    _hours_back: u32,
    token: &str,
) -> ApiResult<Vec<GlucoseReading>> {
    let url = format!("/api/glucose/subjects/{}/readings", subject_id);
    get_json(&url, Some(token)).await
}

pub async fn sync_history(subject_id: &str, token: &str) -> ApiResult<SyncResult> {
    let url = format!("/api/glucose/subjects/{}/sync", subject_id);
    post_empty(&url, Some(token)).await
}
