package uk.ac.york.gpig.teamb.aiassistant.testutils.assertions

import strikt.api.Assertion.Builder
import java.time.OffsetDateTime

@Suppress("UNCHECKED_CAST")
fun Builder<OffsetDateTime?>.isAfter(other: OffsetDateTime): Builder<OffsetDateTime> =
    assert("is after $other") {
        when {
            it == null -> fail("Timestamp is null")
            it.isAfter(other) -> pass()
            else -> fail("Expected timestamp $it to be after $other")
        }
    } as Builder<OffsetDateTime>
