package org.ada.com.application.service;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ada.com.application.port.out.GameRepository;
import org.ada.com.domain.model.Game;

@RequiredArgsConstructor
public class SellerCatalogService {

    private final GameRepository gameRepository;

    public Game registerGame(String title, String genre, BigDecimal price) {
        validateGame(title, genre, price);
        return gameRepository.create(Game.builder()
                .title(title.trim())
                .genre(genre.trim())
                .price(price)
                .active(true)
                .build());
    }

    public boolean editGame(long id, String title, String genre, BigDecimal price) {
        validateGame(title, genre, price);
        return gameRepository.update(Game.builder()
                .id(id)
                .title(title.trim())
                .genre(genre.trim())
                .price(price)
                .active(true)
                .build());
    }

    public boolean excludeGame(long id) {
        return gameRepository.deactivate(id);
    }

    public List<Game> listCatalog() {
        return gameRepository.findAllActive();
    }

    private void validateGame(String title, String genre, BigDecimal price) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required.");
        }
        if (genre == null || genre.isBlank()) {
            throw new IllegalArgumentException("Genre is required.");
        }
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero.");
        }
    }
}

