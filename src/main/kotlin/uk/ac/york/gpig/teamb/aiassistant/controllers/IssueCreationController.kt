package uk.ac.york.gpig.teamb.aiassistant.controllers

import com.google.gson.Gson
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.ac.york.gpig.teamb.aiassistant.git.GitService
import uk.ac.york.gpig.teamb.aiassistant.github.GitHubService
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload

@RestController
class IssueCreationController(
    val gitService: GitService,
    val githubService: GitHubService,
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/new-issue")
    fun receiveNewIssue(@RequestBody body: String) {
        val issueContents = Gson().fromJson(body, WebhookPayload::class.java)
        logger.info("Received a new issue!")
    }
}
