package net.frey.spring6resttemplate.client;

import java.util.UUID;
import net.frey.spring6resttemplate.model.BeerDTO;
import net.frey.spring6resttemplate.model.BeerStyle;
import org.springframework.data.domain.Page;

public interface BeerClient {
    Page<BeerDTO> listBeers();

    Page<BeerDTO> listBeers(String name, BeerStyle style, Boolean showInventory, Integer pageNumber, Integer pageSize);

    Page<BeerDTO> listBeersByName(String name);

    BeerDTO getBeerById(UUID uuid);

    BeerDTO createBeer(BeerDTO beer);

    BeerDTO updateBeer(BeerDTO beerDTO);

    void deleteBeer(UUID beerId);
}
