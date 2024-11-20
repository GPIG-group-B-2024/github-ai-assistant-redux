package uk.ac.york.gpig.teamb.aiassistant.database.conversions

import uk.ac.york.gpig.teamb.aiassistant.enums.MemberType

fun MemberType.toStructurizrString() =
    when (this) {
        MemberType.CONTAINER -> "container"
        MemberType.COMPONENT -> "component"
        MemberType.PERSON -> "person"
        MemberType.SOFTWARE_SYSTEM -> "softwareSystem"
    }
