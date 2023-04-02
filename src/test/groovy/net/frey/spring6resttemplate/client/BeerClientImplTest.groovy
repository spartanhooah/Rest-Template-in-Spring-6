package net.frey.spring6resttemplate.client

import net.frey.spring6resttemplate.model.BeerDTO
import net.frey.spring6resttemplate.model.BeerStyle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.client.HttpClientErrorException
import spock.lang.Specification

@SpringBootTest
class BeerClientImplTest extends Specification {
    @Autowired
    BeerClientImpl client

    def "list beers"() {
        expect:
        client.listBeersByName().totalPages == 97
    }

    def "list beers with name 'ale'"() {
        expect:
        client.listBeersByName("ale").totalPages == 26
    }

    def "get beer by ID"() {
        given:
        def beer = client.listBeers()[0]

        expect:
        client.getBeerById(beer.id)
    }

    def "create beer"() {
        given: "A new beer"
        def newBeer = BeerDTO.builder()
            .price(new BigDecimal("10.99"))
            .beerName("Mango Bob's")
            .beerStyle(BeerStyle.IPA)
            .quantityOnHand(500)
            .upc("12345")
            .build()

        when:
        def savedBeer = client.createBeer(newBeer)

        then:
        savedBeer
    }

    def "update beer"() {
        given: "A new beer"
        def newBeer = BeerDTO.builder()
            .price(new BigDecimal("10.99"))
            .beerName("Mango Bob's 2")
            .beerStyle(BeerStyle.IPA)
            .quantityOnHand(500)
            .upc("12345")
            .build()

        and: "the beer is saved"
        def savedBeer = client.createBeer(newBeer)

        final def newName = "Mango Bob's 3"
        savedBeer.beerName = newName

        when: "the beer's name is updated"
        def updatedBeer = client.updateBeer(savedBeer)

        then:
        updatedBeer.beerName == newName
    }

    def "delete beer"() {
        given: "A new beer"
        def newBeer = BeerDTO.builder()
            .price(new BigDecimal("10.99"))
            .beerName("Mango Bob's 2")
            .beerStyle(BeerStyle.IPA)
            .quantityOnHand(500)
            .upc("12345")
            .build()

        and: "the beer is saved"
        def savedBeer = client.createBeer(newBeer)

        and: "the new beer is deleted"
        client.deleteBeer(savedBeer.id)

        when:
        client.getBeerById(savedBeer.id)

        then:
        thrown(HttpClientErrorException)
    }
}
