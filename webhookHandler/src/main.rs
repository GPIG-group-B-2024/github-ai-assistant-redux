mod auth;
mod data;

use crate::auth::{
    ValidationError::{EnvironmentError, SignatureMismatchError},
    validate,
};
use crate::data::WebhookPayload;
use actix_web::{App, HttpRequest, HttpResponse, HttpServer, post, web};
use apache_avro::{Codec, Schema, Writer};
use log::{error, info};
use rdkafka::config::ClientConfig;
use rdkafka::producer::{FutureProducer, FutureRecord, future_producer::OwnedDeliveryResult};
use std::env;
use std::time::Duration;

fn create_producer() -> FutureProducer {
    let cluster_url = match env::var("KAFKA_CLUSTER_URL") {
        Ok(key) => key.to_string(),
        Err(_) => panic!("Could not find kafka cluster URL"),
    };
    info!("Using cluster url of {cluster_url}");
    ClientConfig::new()
        .set("bootstrap.servers", cluster_url)
        .create()
        .expect("Failed to create Kafka producer")
}

async fn send_message(producer: &FutureProducer, payload_bytes: &[u8]) -> OwnedDeliveryResult {
    let webhook_payload_schema = std::fs::read_to_string("../shared/avro/webhook_payload.avsc");
    let schema = Schema::parse_str(&webhook_payload_schema.unwrap()).unwrap();
    let mut writer = Writer::with_codec(&schema, Vec::new(), Codec::Deflate);

    let body_parsed: Result<WebhookPayload, _> = serde_json::from_slice(payload_bytes);
    let payload = body_parsed.unwrap();
    let issue_id = payload.issue.id.to_string();

    writer.append_ser(payload).unwrap();
    writer.flush().unwrap();

    let message_bytes = writer.into_inner().unwrap();

    let message = FutureRecord::to("new-issue")
        .key(&issue_id)
        .payload(&message_bytes);
    producer.send(message, Duration::from_secs(0)).await
}

#[post("/webhooks")]
async fn handle_webhook(
    req: HttpRequest,
    body: web::Bytes,
    producer: web::Data<FutureProducer>,
) -> HttpResponse {
    // Check webhook is of the "issues" type
    match req.headers().get("x-github-event") {
        Some(h) => match h.to_str() {
            Ok("issues") => {}
            _ => return HttpResponse::NotImplemented().finish(),
        },
        None => return HttpResponse::Unauthorized().finish(), // no header -> unauthorized
    };
    info!("Found event type header");
    // Check if the signature header is present
    let signature_header = match req.headers().get("x-hub-signature-256") {
        Some(h) => h,
        None => return HttpResponse::Unauthorized().finish(), // no header -> unauthorized
    };
    info!("Found signature header");
    // Check that the contents of the signature header are valid
    let signature = match signature_header.to_str() {
        Ok(signature) => match signature.strip_prefix("sha256=") {
            Some(s) => s,
            None => return HttpResponse::Unauthorized().finish(),
        },
        Err(e) => return HttpResponse::BadRequest().body(e.to_string()),
    };
    info!("Obtained signature");
    // Check that the request is valid (i.e. encrypted body matches the signature)
    let body_bytes = body.to_vec();
    match validate(&body_bytes, signature.as_bytes()) {
        Ok(_) => {
            info!("Validated payload");
            match send_message(producer.get_ref(), &body).await {
                Ok(_) => HttpResponse::Ok().finish(),
                Err(_) => HttpResponse::InternalServerError().finish(),
            }
        }
        Err(e) => {
            error!("Validation error");
            match e {
                EnvironmentError => HttpResponse::InternalServerError().finish(),
                SignatureMismatchError => HttpResponse::Forbidden().finish(),
            }
        }
    }
}

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    env_logger::init();

    let producer = create_producer();

    HttpServer::new(move || {
        App::new()
            .service(handle_webhook)
            .app_data(web::Data::new(producer.clone()))
    })
    .bind(("0.0.0.0", 8080))?
    .run()
    .await
}
