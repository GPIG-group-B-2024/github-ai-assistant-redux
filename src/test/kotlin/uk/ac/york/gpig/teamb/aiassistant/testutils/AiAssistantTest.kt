package uk.ac.york.gpig.teamb.aiassistant.testutils

import com.ninjasquad.springmockk.MockkBean
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Table
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.ac.york.gpig.teamb.aiassistant.utils.web.OAuthSecurityConfig

/**
 * Mark the annotated class as a spring boot test and:
 *
 * 1. Consume configuration values from `application-test.yml`
 * 2. Reset database after every individual test
 * 3. Mock the security config so that we are not constantly pinging auth0
 * */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@ExtendWith(DbCleanUpExtension::class)
annotation class AiAssistantTest

/**
 * Mock the security config for component tests
 * */
@TestConfiguration
class TestConfig {
    @Suppress("UNUSED")
    @MockkBean(relaxed = true)
    private lateinit var oAuthSecurityConfig: OAuthSecurityConfig
}

/**
 * An extension to automatically clear all tables between every test.
 *
 * */
private class DbCleanUpExtension : BeforeAllCallback, AfterEachCallback {
    companion object {
        /**Tables that are read and written to/from by the app (i.e. not created by flyway, etc)*/
        private var appTables: List<Table<Record>>? = null
    }

    /**
     * Obtain a list of all tables that exist in the app's database
     * */
    override fun beforeAll(context: ExtensionContext) {
        if (appTables == null) {
            val ctx = SpringExtension.getApplicationContext(context).getBean(DSLContext::class.java)
            @Suppress("UNCHECKED_CAST")
            appTables =
                (ctx.meta().tables as List<Table<Record>>).filter {
                    !it.name.contains("flyway_schema_history") &&
                        it.schema?.name != "information_schema" &&
                        it.schema?.name != "pg_catalog"
                }
        }
    }

    /**
     * Delete everything from every known table in the database
     * */
    override fun afterEach(context: ExtensionContext) =
        SpringExtension.getApplicationContext(context).getBean(DSLContext::class.java).let { ctx ->
            appTables!!.forEach { ctx.truncate(it).cascade().execute() }
        }
}
