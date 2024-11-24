package uk.ac.york.gpig.teamb.aiassistant.utils.types

import com.google.gson.annotations.SerializedName

/**
 * The event type of an incoming webhook

 * */
enum class EventType {
    ISSUES,

    ISSUE_COMMENT,

    ;

    companion object {
        fun fromString(s: String): EventType =
            when (s) {
                "issue_comment" -> ISSUE_COMMENT
                "issues" -> ISSUES
                else -> throw IllegalArgumentException("Unknown event type $s")
            }
    }
}
