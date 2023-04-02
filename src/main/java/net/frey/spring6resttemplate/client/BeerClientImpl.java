package net.frey.spring6resttemplate.client;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.frey.spring6resttemplate.model.BeerDTO;
import net.frey.spring6resttemplate.model.BeerDTOPageImpl;
import net.frey.spring6resttemplate.model.BeerStyle;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class BeerClientImpl implements BeerClient {
    private static final String COLLECTION_PATH = "/api/v1/beers";
    private static final String INSTANCE_PATH = "/api/v1/beers/{id}";

    private final RestTemplateBuilder builder;

    private UriComponentsBuilder uriComponentsBuilder;
    private RestTemplate template;

    @PostConstruct
    public void init() {
        uriComponentsBuilder = UriComponentsBuilder.fromPath(COLLECTION_PATH);
        template = builder.build();
    }

    @Override
    public Page<BeerDTO> listBeers(
            String name, BeerStyle style, Boolean showInventory, Integer pageNumber, Integer pageSize) {
        if (StringUtils.isNotBlank(name)) {
            uriComponentsBuilder.queryParam("name", name);
        }

        if (style != null) {
            uriComponentsBuilder.queryParam("style", style);
        }

        if (showInventory != null) {
            uriComponentsBuilder.queryParam("showInventory", showInventory);
        }

        if (pageNumber != null) {
            uriComponentsBuilder.queryParam("pageNumber", pageNumber);
        }

        if (pageSize != null) {
            uriComponentsBuilder.queryParam("pageSize", pageSize);
        }

        ResponseEntity<BeerDTOPageImpl> response =
                template.getForEntity(uriComponentsBuilder.toUriString(), BeerDTOPageImpl.class);

        return response.getBody();
    }

    @Override
    public Page<BeerDTO> listBeers() {
        return listBeers(null, null, null, null, null);
    }

    @Override
    public Page<BeerDTO> listBeersByName(String name) {
        return listBeers(name, null, null, null, null);
    }

    @Override
    public BeerDTO getBeerById(UUID id) {
        return template.getForObject(INSTANCE_PATH, BeerDTO.class, id);
    }

    @Override
    public BeerDTO createBeer(BeerDTO beer) {
        URI uri = template.postForLocation(COLLECTION_PATH, beer);

        return template.getForObject(uri.getPath(), BeerDTO.class);
    }

    @Override
    public BeerDTO updateBeer(BeerDTO beer) {
        UUID beerId = beer.getId();
        template.put(INSTANCE_PATH, beer, beerId);

        return getBeerById(beerId);
    }

    @Override
    public void deleteBeer(UUID beerId) {
        template.delete(INSTANCE_PATH, beerId);
    }
}
