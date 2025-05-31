FROM rust:latest AS builder
COPY . .
RUN cargo build -r

FROM debian:stable-slim AS runner
COPY --from=builder ./target/release/test-webhook-receiver ./app
ENTRYPOINT ["./app"]