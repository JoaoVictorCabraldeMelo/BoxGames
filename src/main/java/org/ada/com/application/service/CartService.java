package org.ada.com.application.service;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ada.com.application.port.out.CartRepository;
import org.ada.com.application.port.out.ClientRepository;
import org.ada.com.application.port.out.GameRepository;
import org.ada.com.application.port.out.WishlistRepository;
import org.ada.com.domain.model.CartItem;
import org.ada.com.domain.model.ClientAccount;
import org.ada.com.domain.model.Game;
import org.ada.com.domain.model.WishlistItem;

@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final GameRepository gameRepository;
    private final ClientRepository clientRepository;
    private final WishlistRepository wishlistRepository;

    public void addGame(long clientId, long gameId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        Game game = gameRepository.findById(gameId)
                .filter(Game::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Game not found in active catalog."));

        if (game.getPrice().signum() <= 0) {
            throw new IllegalArgumentException("Invalid game price.");
        }

        cartRepository.addItem(clientId, gameId, quantity);
    }

    public boolean removeGame(long clientId, long gameId) {
        return cartRepository.removeItem(clientId, gameId);
    }

    public List<CartItem> getCart(long clientId) {
        return cartRepository.findByClientId(clientId);
    }

    public void addGameToWishlist(long clientId, long gameId) {
        ensureClientExists(clientId);
        gameRepository.findById(gameId)
                .filter(Game::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Game not found in active catalog."));
        wishlistRepository.addItem(clientId, gameId);
    }

    public boolean removeGameFromWishlist(long clientId, long gameId) {
        return wishlistRepository.removeItem(clientId, gameId);
    }

    public List<WishlistItem> getWishlist(long clientId) {
        return wishlistRepository.findByClientId(clientId);
    }

    public boolean moveGameFromWishlistToCart(long clientId, long gameId, int quantity) {
        ensureClientExists(clientId);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        boolean removed = wishlistRepository.removeItem(clientId, gameId);
        if (!removed) {
            return false;
        }

        addGame(clientId, gameId, quantity);
        return true;
    }

    public CheckoutResult checkout(long clientId) {
        ClientAccount client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client account not found."));

        List<CartItem> items = cartRepository.findByClientId(clientId);
        if (items.isEmpty()) {
            return CheckoutResult.builder()
                    .success(false)
                    .total(BigDecimal.ZERO)
                    .remainingCredits(client.getCredits())
                    .message("Cart is empty.")
                    .build();
        }

        BigDecimal total = items.stream()
                .map(this::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (client.getCredits().compareTo(total) < 0) {
            return CheckoutResult.builder()
                    .success(false)
                    .total(total)
                    .remainingCredits(client.getCredits())
                    .message("Insufficient credits.")
                    .build();
        }

        BigDecimal newCredits = client.getCredits().subtract(total);
        clientRepository.updateCredits(clientId, newCredits);
        cartRepository.clearByClientId(clientId);

        return CheckoutResult.builder()
                .success(true)
                .total(total)
                .remainingCredits(newCredits)
                .message("Purchase completed.")
                .build();
    }

    private BigDecimal subtotal(CartItem item) {
        Game game = gameRepository.findById(item.getGameId())
                .orElseThrow(() -> new IllegalStateException("Game in cart no longer exists."));
        return game.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    private void ensureClientExists(long clientId) {
        clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client account not found."));
    }
}

