package org.ada.com.application.port.out;

import java.util.List;
import java.util.Optional;
import org.ada.com.domain.model.Game;

public interface GameRepository {
    Game create(Game game);

    boolean update(Game game);

    boolean deactivate(long id);

    Optional<Game> findById(long id);

    List<Game> findAllActive();

    List<Game> filterActive(String titleContains, String genre);
}

