package net.frey.spring6resttemplate.client

import com.fasterxml.jackson.databind.ObjectMapper
import net.frey.spring6resttemplate.config.OAuthClientInterceptor
import net.frey.spring6resttemplate.config.RestTemplateBuilderConfig
import net.frey.spring6resttemplate.model.BeerDTO
import net.frey.spring6resttemplate.model.BeerDTOPageImpl
import net.frey.spring6resttemplate.model.BeerStyle
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.util.UriComponentsBuilder
import spock.lang.Specification

import java.time.Instant

import static net.frey.spring6resttemplate.client.BeerClientImpl.COLLECTION_PATH
import static net.frey.spring6resttemplate.client.BeerClientImpl.INSTANCE_PATH
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate
import static org.springframework.test.web.client.response.MockRestResponseCreators.withAccepted
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@RestClientTest
@Import(RestTemplateBuilderConfig)
class BeerClientMockTest extends Specification {
    static final def URL = "http://localhost:8080"
    static final def AUTH_HEADER_VALUE = "Bearer test"

    @Autowired
    ObjectMapper mapper

    @Autowired
    RestTemplateBuilder configuredBuilder

    @SpringBean
    OAuth2AuthorizedClientManager manager = Mock()

    @TestConfiguration
    static class TestConfig {
        @Bean
        InMemoryClientRegistrationRepository clientRegistrationRepository() {
            new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("springauth")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientId("test")
                .tokenUri("test")
                .build())
        }

        @Bean
        InMemoryOAuth2AuthorizedClientService oauth2AUthorizedClientService(ClientRegistrationRepository registrationRepository) {
            new InMemoryOAuth2AuthorizedClientService(registrationRepository)
        }

        @Bean
        OAuthClientInterceptor oauthClientInterceptor(OAuth2AuthorizedClientManager manager, ClientRegistrationRepository repository) {
            new OAuthClientInterceptor(manager, repository)
        }
    }

    @Autowired
    ClientRegistrationRepository registrationRepository

    BeerClient client

    MockRestServiceServer server

    def mockBuilder = Stub(RestTemplateBuilder)

    def beer = getBeerDto()
    def beerJson

    void setup() {
        def clientRegistration = registrationRepository
            .findByRegistrationId("springauth")

        def token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "test", Instant.MIN, Instant.MAX)

        manager.authorize(_) >> new OAuth2AuthorizedClient(clientRegistration, "test", token)

        beerJson = mapper.writeValueAsString(beer)
        def restTemplate = configuredBuilder.build()
        server = MockRestServiceServer.bindTo(restTemplate).build()

        mockBuilder.build() >> restTemplate

        client = new BeerClientImpl(mockBuilder)
        client.init()
    }

    def "test list beers"() {
        given:
        def response = mapper.writeValueAsString(getPage())

        server.expect(method(HttpMethod.GET))
            .andExpect(requestTo("$URL$COLLECTION_PATH"))
            .andExpect(header("Authorization", AUTH_HEADER_VALUE))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON))

        when:
        def dtos = client.listBeers()

        then:
        dtos.content.size() > 0
    }

    def "test get beer by ID"() {
        given:
        mockGetOperation()

        when:
        def result = client.getBeerById(beer.id)

        then:
        result.id == beer.id
    }

    def "create beer"() {
        given:
        def uri = UriComponentsBuilder.fromPath(INSTANCE_PATH).build(beer.id)

        server.expect(method(HttpMethod.POST))
            .andExpect(requestTo("$URL$COLLECTION_PATH"))
            .andExpect(header("Authorization", AUTH_HEADER_VALUE))
            .andRespond(withAccepted().location(uri))

        mockGetOperation()

        when:
        def result = client.createBeer(beer)

        then:
        result.id == beer.id
    }

    def "update beer"() {
        given:
        server.expect(method(HttpMethod.PUT))
            .andExpect(requestToUriTemplate("$URL$INSTANCE_PATH", beer.id))
            .andExpect(header("Authorization", AUTH_HEADER_VALUE))
            .andRespond(withNoContent())

        mockGetOperation()

        when:
        def result = client.updateBeer(beer)

        then:
        result.id == beer.id
    }

    def "delete beer"() {
        given:
        server.expect(method(HttpMethod.DELETE))
            .andExpect(requestToUriTemplate("$URL$INSTANCE_PATH", beer.id))
            .andExpect(header("Authorization", AUTH_HEADER_VALUE))
            .andRespond(withNoContent())

        when:
        client.deleteBeer(beer.id)

        then:
        server.verify()
    }

    def "delete nonexistent beer"() {
        given:
        server.expect(method(HttpMethod.DELETE))
            .andExpect(requestToUriTemplate("$URL$INSTANCE_PATH", beer.id))
            .andExpect(header("Authorization", AUTH_HEADER_VALUE))
            .andRespond(withResourceNotFound())

        when:
        client.deleteBeer(beer.id)

        then:
        thrown(HttpClientErrorException)
    }

    def "list beer with query parameter"() {
        given:
        def response = mapper.writeValueAsString(getPage())
        URI uri = UriComponentsBuilder.fromHttpUrl("$URL$COLLECTION_PATH")
            .queryParam("name", "ale")
            .build()
            .toUri()

        server.expect(method(HttpMethod.GET))
            .andExpect(requestTo(uri))
            .andExpect(header("Authorization", AUTH_HEADER_VALUE))
            .andExpect(queryParam("name", "ale"))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON))

        when:
        def result = client.listBeersByName("ale")

        then:
        result.content.size() > 0
    }

    static def getPage() {
        new BeerDTOPageImpl([getBeerDto()], 1, 25, 1)
    }

    static def getBeerDto() {
        BeerDTO.builder()
            .id(UUID.randomUUID())
            .price(new BigDecimal("10.99"))
            .beerName("Mango Bob's")
            .beerStyle(BeerStyle.IPA)
            .quantityOnHand(500)
            .upc("123456")
            .build()
    }

    def mockGetOperation() {
        server.expect(method(HttpMethod.GET))
            .andExpect(requestToUriTemplate("$URL$INSTANCE_PATH", beer.id))
            .andExpect(header("Authorization", AUTH_HEADER_VALUE))
            .andRespond(withSuccess(beerJson, MediaType.APPLICATION_JSON))
    }
}
