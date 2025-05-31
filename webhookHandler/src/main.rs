mod auth;

use crate::auth::validate;
use crate::auth::ValidationError::{EnvironmentError, SignatureMismatchError};
use actix_web::{post, web, App, HttpRequest, HttpResponse, HttpServer};
use log::info;
use rdkafka::config::ClientConfig;
use rdkafka::producer::{FutureProducer, FutureRecord};
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

#[post("/webhooks")]
async fn handle_webhook(
    req: HttpRequest,
    body: web::Bytes,
    producer: web::Data<FutureProducer>,
) -> HttpResponse {
    // Check if the signature header is present
    let header = match req.headers().get("x-hub-signature-256") {
        Some(h) => h,
        None => return HttpResponse::Unauthorized().finish(), // no header -> unauthorized
    };
    // Check that the contents of the signature header are valid
    let signature = match header.to_str() {
        Ok(signature) => match signature.strip_prefix("sha256=") {
            Some(s) => s,
            None => return HttpResponse::Unauthorized().finish(),
        },
        Err(e) => return HttpResponse::BadRequest().body(e.to_string()),
    };
    // Check that the request is valid (i.e. encrypted body matches the signature)
    let body_bytes = body.to_vec();
    match validate(&body_bytes, signature.as_bytes()) {
        Ok(_) => {
            let message = FutureRecord::to("my-topic").key("0").payload(&body_bytes);
            match producer.send(message, Duration::from_secs(0)).await {
                Ok(_) => HttpResponse::Ok().finish(),
                Err(_) => HttpResponse::InternalServerError().finish(),
            }
        }
        Err(e) => match e {
            EnvironmentError => HttpResponse::InternalServerError().finish(),
            SignatureMismatchError => HttpResponse::Forbidden().finish(),
        },
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
