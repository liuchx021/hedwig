use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum VendorType {
    Ottai,
    Sisensing,
}

impl VendorType {
    pub fn display_name(&self) -> &'static str {
        match self {
            VendorType::Ottai => "欧泰（Ottai）",
            VendorType::Sisensing => "硅基轻享（SiSensing）",
        }
    }

    pub fn login_instructions(&self) -> &'static str {
        match self {
            VendorType::Ottai => "欧泰暂不支持账号密码直连，需要先在微信小程序中抓取 access token",
            VendorType::Sisensing => {
                "输入硅基轻享注册手机号和密码即可直连，系统会自动换取并校验访问令牌"
            }
        }
    }

    pub fn token_instructions(&self) -> &'static str {
        match self {
            VendorType::Ottai => "从微信小程序抓包获取访问令牌（access token），仅粘贴令牌值，不要包含 Bearer 前缀或 Authorization 请求头",
            VendorType::Sisensing => "如果你已经从硅基轻享 App 获取到 access token，可直接粘贴令牌值，不要包含 Bearer 前缀",
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum TokenStatus {
    Active,
    ExpiringSoon,
    Expired,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct VendorConnection {
    pub id: i64,
    pub vendor_type: VendorType,
    pub vendor_user_id: Option<String>,
    pub primary_subject_id: Option<i64>,
    pub primary_subject_name: Option<String>,
    pub token_status: TokenStatus,
    pub token_expires_at: Option<String>,
    pub last_synced_at: Option<String>,
    pub sensor_expires_at: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectTokenRequest {
    pub vendor_type: String,
    pub access_token: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectLoginRequest {
    pub vendor_type: String,
    pub username: String,
    pub password: String,
}
