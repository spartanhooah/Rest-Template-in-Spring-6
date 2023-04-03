package net.frey.spring6resttemplate.client

import com.fasterxml.jackson.databind.ObjectMapper
import net.frey.spring6resttemplate.config.RestTemplateBuilderConfig
import net.frey.spring6resttemplate.model.BeerDTO
import net.frey.spring6resttemplate.model.BeerDTOPageImpl
import net.frey.spring6resttemplate.model.BeerStyle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.util.UriComponentsBuilder
import spock.lang.Specification

import static net.frey.spring6resttemplate.client.BeerClientImpl.COLLECTION_PATH
import static net.frey.spring6resttemplate.client.BeerClientImpl.INSTANCE_PATH
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

    @Autowired
    ObjectMapper mapper

    @Autowired
    RestTemplateBuilder configuredBuilder

    BeerClient client

    MockRestServiceServer server

    def mockBuilder = Stub(RestTemplateBuilder)

    def beer = getBeerDto()
    def beerJson

    void setup() {
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
            .andRespond(withSuccess(beerJson, MediaType.APPLICATION_JSON))
    }
}
