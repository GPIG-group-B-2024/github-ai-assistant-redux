use hmac::{Hmac, Mac};
use sha2::Sha256;
use std::env;
pub enum ValidationError {
    EnvironmentError,
    SignatureMismatchError,
}

pub fn validate(payload: &[u8], target: &[u8]) -> Result<(), ValidationError> {
    type HmacSha256 = Hmac<Sha256>;
    let github_key = match env::var("GITHUB_KEY") {
        Ok(key) => key.into_bytes(),
        Err(_) => return Err(ValidationError::EnvironmentError),
    };
    let mut mac = HmacSha256::new_from_slice(&github_key).unwrap();
    mac.update(payload);
    match mac.verify_slice(target) {
        Ok(()) => Ok(()),
        Err(_) => Err(ValidationError::SignatureMismatchError),
    }
}
