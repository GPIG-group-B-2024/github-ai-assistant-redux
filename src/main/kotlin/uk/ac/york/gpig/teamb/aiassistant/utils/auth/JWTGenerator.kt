package uk.ac.york.gpig.teamb.aiassistant.utils.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.interfaces.RSAKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Date

class JWTGenerator {
    companion object {
        /**
         * Read a permanent private key from the contents of a .pem file and generate a Java `PrivateKey` object.
         *
         * NOTE: this assumes the key is in the PKCS#8 format.
         * The keys produced by GitHub are in PKCS#1 and are very inconvenient to use.
         * Formats can be converted using the following command:
         *
         * `openssl pkcs8 -topk8 -inform PEM -outform PEM -in <path-to-pkcs#1> -out <path-to-pkcs#8> -nocrypt`
         * */
        internal fun loadPrivateKey(pemFileContents: String): PrivateKey {
            // Remove PEM headers and footers, and any whitespace
            val keyString =
                pemFileContents
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("\\s".toRegex(), "")

            // Decode the Base64 encoded string
            val decodedKey = Base64.getDecoder().decode(keyString)

            // Generate the PrivateKey object
            val keySpec = PKCS8EncodedKeySpec(decodedKey)
            val keyFactory = KeyFactory.getInstance("RSA") // Use "RSA" for RSA keys
            return keyFactory.generatePrivate(keySpec)
        }

        /**
         * Generate a JWT to the spec outlined in GitHub [docs](https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/generating-a-json-web-token-jwt-for-a-github-app)
         * */
        fun generateJWT(pemFileContents: String): String =
            JWT
                .create()
                .withIssuer("Iv23liv9vlXMoLnWCfG0") // Client ID. Note: this is public and can be hardcoded *for now*
                .withIssuedAt(
                    Date.from(Instant.now().minus(60, ChronoUnit.SECONDS)),
                ) // set issue date at 1 minute into the past (see docstring)
                .withExpiresAt(
                    Date.from(Instant.now().plus(10, ChronoUnit.MINUTES)),
                ) // set expiration date at 10 mins into the future (max allowed amount)
                .sign(
                    Algorithm.RSA256(loadPrivateKey(pemFileContents) as RSAKey),
                ) // encrypt using the required algorithm
    }
}
