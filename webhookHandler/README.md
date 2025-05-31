# Webhook forwarding service

This service is responsible for the following:

- Receiving incoming github webhooks
- Validating them using the configured secret
- Extracting useful data from the payload
- Forwarding the data into the message queue

This is the first "point of entry" for an incoming user request, with other services being responsible for processing it.