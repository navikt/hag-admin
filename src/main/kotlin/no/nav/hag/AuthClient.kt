package no.nav.hag

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import no.nav.helsearbeidsgiver.utils.json.jsonConfig

class AuthClient {
    private val tokenEndpoint = "NAIS_TOKEN_ENDPOINT".let(System::getenv) ?: "http://localhost/mock-token"
    private val tokenIntrospectionEndpoint =
        "NAIS_TOKEN_INTROSPECTION_ENDPOINT".let(System::getenv) ?: "http://localhost/mock-introspection"

    private val httpClient =
        HttpClient(Apache5) {
            expectSuccess = true

            install(ContentNegotiation) {
                json(jsonConfig)
            }
        }

    fun tokenGetter(target: String): () -> String =
        {
            runBlocking {
                httpClient
                    .submitForm(
                        url = tokenEndpoint,
                        formParameters =
                            parameters {
                                append(Params.IDENTITY_PROVIDER, Params.Values.ENTRA_ID)
                                append(Params.TARGET, target)
                            },
                    ).body<TokenResponse>()
                    .accessToken
            }
        }

    suspend fun introspect(accessToken: String): Boolean =
        httpClient
            .submitForm(
                url = tokenIntrospectionEndpoint,
                formParameters =
                    parameters {
                        append(Params.IDENTITY_PROVIDER, Params.Values.ENTRA_ID)
                        append(Params.TOKEN, accessToken)
                    },
            ).body<TokenIntrospectionResponse>()
            .active
}

@Serializable
@OptIn(ExperimentalSerializationApi::class)
internal data class TokenResponse(
    @JsonNames("access_token")
    val accessToken: String,
)

@Serializable
internal data class TokenIntrospectionResponse(
    val active: Boolean,
)

private object Params {
    const val IDENTITY_PROVIDER = "identity_provider"
    const val TARGET = "target"
    const val TOKEN = "token"

    object Values {
        // Identity provider value
        const val ENTRA_ID = "azuread"
    }
}
