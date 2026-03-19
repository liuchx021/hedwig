pub mod auth;
pub mod glucose;
pub mod nightscout;
pub mod vendors;

use gloo_net::http::Request;
use serde_json::Value;

pub type ApiResult<T> = Result<T, String>;

pub fn is_auth_error(message: &str) -> bool {
    message.starts_with("HTTP 401")
        || message.starts_with("HTTP 403")
        || message.contains("\"status\":401")
        || message.contains("\"status\":403")
        || message.contains("登录状态已失效")
        || message.contains("认证失败")
        || message.contains("Authentication required")
        || message.contains("Access denied")
}

pub async fn get_json<T: serde::de::DeserializeOwned>(
    url: &str,
    token: Option<&str>,
) -> ApiResult<T> {
    let mut req = Request::get(url);
    if let Some(t) = token {
        req = req.header("Authorization", &format!("Bearer {}", t));
    }
    let resp = req
        .send()
        .await
        .map_err(|_| "网络请求失败，请检查网络连接后重试".to_string())?;
    if resp.ok() {
        resp.json::<T>()
            .await
            .map_err(|_| "服务返回数据解析失败，请稍后重试".to_string())
    } else {
        let status = resp.status();
        let body = resp.text().await.unwrap_or_default();
        Err(normalize_api_error(status, &body))
    }
}

pub async fn post_json<B: serde::Serialize, T: serde::de::DeserializeOwned>(
    url: &str,
    body: &B,
    token: Option<&str>,
) -> ApiResult<T> {
    let mut req = Request::post(url).header("Content-Type", "application/json");
    if let Some(t) = token {
        req = req.header("Authorization", &format!("Bearer {}", t));
    }
    let resp = req
        .body(serde_json::to_string(body).map_err(|e| e.to_string())?)
        .map_err(|e| e.to_string())?
        .send()
        .await
        .map_err(|_| "网络请求失败，请检查网络连接后重试".to_string())?;
    if resp.ok() {
        resp.json::<T>()
            .await
            .map_err(|_| "服务返回数据解析失败，请稍后重试".to_string())
    } else {
        let status = resp.status();
        let body_text = resp.text().await.unwrap_or_default();
        Err(normalize_api_error(status, &body_text))
    }
}

pub async fn delete_empty(url: &str, token: Option<&str>) -> ApiResult<()> {
    let mut req = Request::delete(url);
    if let Some(t) = token {
        req = req.header("Authorization", &format!("Bearer {}", t));
    }
    let resp = req
        .send()
        .await
        .map_err(|_| "网络请求失败，请检查网络连接后重试".to_string())?;
    if resp.ok() {
        Ok(())
    } else {
        let status = resp.status();
        let body = resp.text().await.unwrap_or_default();
        Err(normalize_api_error(status, &body))
    }
}

pub async fn post_empty<T: serde::de::DeserializeOwned>(
    url: &str,
    token: Option<&str>,
) -> ApiResult<T> {
    let mut req = Request::post(url);
    if let Some(t) = token {
        req = req.header("Authorization", &format!("Bearer {}", t));
    }
    let resp = req
        .send()
        .await
        .map_err(|_| "网络请求失败，请检查网络连接后重试".to_string())?;
    if resp.ok() {
        resp.json::<T>()
            .await
            .map_err(|_| "服务返回数据解析失败，请稍后重试".to_string())
    } else {
        let status = resp.status();
        let body = resp.text().await.unwrap_or_default();
        Err(normalize_api_error(status, &body))
    }
}

fn normalize_api_error(status: u16, body: &str) -> String {
    let raw = extract_message(body);

    if status == 401 || status == 403 {
        return "登录状态已失效，请重新登录".to_string();
    }
    if status == 404 {
        return map_known_error_message(&raw).unwrap_or_else(|| "请求的资源不存在".to_string());
    }
    if status >= 500 {
        return map_known_error_message(&raw)
            .or_else(|| preserve_backend_message(&raw))
            .unwrap_or_else(|| "服务暂时不可用，请稍后重试".to_string());
    }

    map_known_error_message(&raw)
        .or_else(|| (!raw.is_empty()).then_some(raw))
        .unwrap_or_else(|| format!("请求失败（HTTP {}）", status))
}

fn extract_message(body: &str) -> String {
    serde_json::from_str::<Value>(body)
        .ok()
        .and_then(|v| v.get("message").and_then(|m| m.as_str()).map(String::from))
        .unwrap_or_else(|| body.trim().to_string())
}

fn map_known_error_message(message: &str) -> Option<String> {
    if message.is_empty() {
        return None;
    }

    let lowered = message.to_ascii_lowercase();

    if lowered.contains("authentication required") || lowered.contains("access denied") {
        return Some("登录状态已失效，请重新登录".to_string());
    }
    if lowered.contains("invalid username or password") {
        return Some("用户名或密码错误".to_string());
    }
    if lowered.contains("username already exists") {
        return Some("用户名已存在，请更换后重试".to_string());
    }
    if lowered.contains("connection not found") {
        return Some("未找到设备连接".to_string());
    }
    if lowered.contains("subject not found") {
        return Some("未找到监测对象".to_string());
    }
    if lowered.contains("no readings found") || lowered.contains("no readings after fetch") {
        return Some("未找到血糖读数".to_string());
    }
    if lowered.contains("token is invalid or expired") {
        return Some("访问令牌无效或已过期".to_string());
    }
    if lowered.contains("login failed or returned invalid token") {
        return Some("厂商登录失败或返回的访问令牌无效".to_string());
    }
    if lowered.contains("user not found") {
        return Some("未找到用户".to_string());
    }
    if message.contains("手机号码不能为空") {
        return Some("请输入硅基账号手机号".to_string());
    }
    if message.contains("用户未注册") {
        return Some("硅基账号未注册或不存在".to_string());
    }
    if lowered.contains("an unexpected error occurred") {
        return Some("发生未知错误，请稍后重试".to_string());
    }
    if lowered.contains("ottai login requires wechat mini-program interaction") {
        return Some("欧泰暂不支持账号密码登录，请改用访问令牌连接".to_string());
    }
    if message.contains("请输入硅基账号（手机号）和密码") {
        return Some("请输入硅基账号（手机号）和密码".to_string());
    }
    if lowered.starts_with("http 401") || lowered.starts_with("http 403") {
        return Some("登录状态已失效，请重新登录".to_string());
    }
    if lowered.starts_with("http ") {
        return Some("请求失败，请稍后重试".to_string());
    }

    None
}

fn preserve_backend_message(message: &str) -> Option<String> {
    let trimmed = message.trim();
    if trimmed.is_empty() {
        return None;
    }

    let lowered = trimmed.to_ascii_lowercase();
    if trimmed.starts_with('{')
        || trimmed.starts_with('<')
        || lowered.starts_with("http ")
        || lowered.starts_with("unexpected error")
    {
        return None;
    }

    Some(trimmed.to_string())
}
