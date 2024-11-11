# Github AI assistant

## What is this?

This project is one of the products developed by this team.
It's core functionality is currently as follows:
* Receive incoming issues on a connected repository 
* Make pull requests to that repository attempting to address the issue

## Getting started
Getting the project running locally requires a few preparation steps:
1. Install the smee client to forward github webhooks to localhost (requires npm): 

    ```npm install --global smee-client```
2. Start a smee session using the **correct** link:

    ```smee -u https://smee.io/r43wJeiroPFW93o --port 8080 --path "/new-issue"```
    This will forward webhooks to `localhost:8080/new-issue` where the app expects them
3. Obtain a private key for the github app and place it into the `resources` folder.
   * You will need owner access to the organisation - message Ivan if you require this.
4. Run the spring boot application
    
    ```./gradlew bootRun```
5. You are now ready to use the app! You can do one of the following:
    * Navigate to the smee session in your browser and re-issue an event you like
    * Go to the `dummy-repo` and create a new issue there

## Contributing

1. The CI workflow will reject incorrectly formatted code. To format the code locally, run the gradle task:
```./gradlew spotlessApply```
2. You will need a private key for the github app - see point 3 in the getting started section
3. Avoid pushing code directly to main - use pull requests and link pull requests to issues (if they exist), see [github docs](https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/linking-a-pull-request-to-an-issue#linking-a-pull-request-to-an-issue-using-a-keyword)
4. Aim to get at least one approval on your PR from the team
5. Update statuses of any related tickets once your PR is merged