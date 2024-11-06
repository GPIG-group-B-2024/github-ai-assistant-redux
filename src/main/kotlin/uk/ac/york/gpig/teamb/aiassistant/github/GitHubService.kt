package uk.ac.york.gpig.teamb.aiassistant.github

import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
object GitHubService {

    @Value("\${target-repo.url}")
    lateinit var repoUrl: String

    @Value("\${target-repo.token}")
    lateinit var token: String

    @Value("\${target-repo.name}")
    lateinit var repoName: String


    fun createPullRequest(baseBranch: String = "main", featureBranch: String, title: String, body: String) {
        val github = GitHubBuilder().withOAuthToken(token).build()
        val repo = github.getRepository(repoName)
        repo.createPullRequest(title, featureBranch, baseBranch, body)
    }
}
fun main(){
    GitHubService.createPullRequest("main", "my-branch", "My pull request", "I have done some work, here it is!")
}