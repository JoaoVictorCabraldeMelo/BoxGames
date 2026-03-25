package org.ada.com.application.service;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ada.com.application.port.out.GameRepository;
import org.ada.com.domain.model.Game;

@RequiredArgsConstructor
public class ClientCatalogService {

    private final GameRepository gameRepository;

    public List<Game> filterGames(String titleContains, String genre, BigDecimal minPrice, BigDecimal maxPrice) {
        return gameRepository.filterActive(titleContains, genre, minPrice, maxPrice);
    }
}

