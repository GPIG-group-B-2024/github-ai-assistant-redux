#[derive(serde::Deserialize, Debug, serde::Serialize)]
pub struct WebhookPayload {
    pub issue: Issue,
    pub action: Action,
    pub repository: Repository,
}
#[derive(serde::Deserialize, Debug, serde::Serialize)]
pub struct Issue {
    pub id: u64,
    pub number: u32,
    pub body: String,
    pub title: String,
}
#[derive(serde::Deserialize, Debug, serde::Serialize)]
pub struct Repository {
    pub full_name: String,
    pub url: String,
}
#[derive(serde::Deserialize, Debug, serde::Serialize)]
pub enum Action {
    #[serde(rename = "opened")]
    Opened,
    #[serde(rename = "closed")]
    Closed,
}
