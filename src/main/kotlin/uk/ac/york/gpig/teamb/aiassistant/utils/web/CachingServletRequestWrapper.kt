package uk.ac.york.gpig.teamb.aiassistant.utils.web

import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class CachingServletRequestWrapper(request: HttpServletRequest) : HttpServletRequestWrapper(request) {
    var requestBodyBytes: ByteArray = request.inputStream.use { it.readAllBytes() }

    override fun getInputStream(): ServletInputStream {
        val byteArrayInputStream = ByteArrayInputStream(requestBodyBytes)
        return object : ServletInputStream() {
            override fun isFinished(): Boolean = byteArrayInputStream.available() == 0

            override fun isReady(): Boolean = true

            override fun setReadListener(readListener: ReadListener?) =
                throw NotImplementedError("This class is unsuitable for non-blocking operation")

            override fun read() = byteArrayInputStream.read()
        }
    }

    override fun getReader(): BufferedReader = BufferedReader(InputStreamReader(this.getInputStream(), StandardCharsets.UTF_8))
}
