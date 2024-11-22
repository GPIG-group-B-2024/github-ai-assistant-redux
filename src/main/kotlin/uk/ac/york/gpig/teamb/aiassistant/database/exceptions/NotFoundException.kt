package uk.ac.york.gpig.teamb.aiassistant.database.exceptions

import java.util.UUID

sealed class NotFoundException(message: String) : Exception(message) {
    class NotFoundByNameException(name: String, type: String) :
        NotFoundException("""Could not find $type with name "$name"""")

    class NotFoundByIdException(id: UUID, type: String) :
        NotFoundException("""Could not find $type with id "$id"""")
}
