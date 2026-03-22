package org.ada.com.adapters.in.cli;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;
import org.ada.com.application.port.out.GameRepository;
import org.ada.com.application.service.AuthorizationService;
import org.ada.com.application.service.CartService;
import org.ada.com.application.service.CheckoutResult;
import org.ada.com.application.service.ClientCatalogService;
import org.ada.com.application.service.ClientWalletService;
import org.ada.com.application.service.SellerCatalogService;
import org.ada.com.domain.model.CartItem;
import org.ada.com.domain.model.ClientAccount;
import org.ada.com.domain.model.Game;
import org.ada.com.domain.model.UserRole;

public class TerminalApp {

    private final AuthorizationService authorizationService;
    private final SellerCatalogService sellerCatalogService;
    private final ClientCatalogService clientCatalogService;
    private final ClientWalletService clientWalletService;
    private final CartService cartService;
    private final GameRepository gameRepository;

    public TerminalApp(
            AuthorizationService authorizationService,
            SellerCatalogService sellerCatalogService,
            ClientCatalogService clientCatalogService,
            ClientWalletService clientWalletService,
            CartService cartService,
            GameRepository gameRepository) {
        this.authorizationService = authorizationService;
        this.sellerCatalogService = sellerCatalogService;
        this.clientCatalogService = clientCatalogService;
        this.clientWalletService = clientWalletService;
        this.cartService = cartService;
        this.gameRepository = gameRepository;
    }

    public void run() {
        seedCatalogIfEmpty();

        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                clearScreen();
                printMainMenu();
                String option = scanner.nextLine().trim();
                if ("0".equals(option)) {
                    running = false;
                    continue;
                }

                try {
                    UserRole role = authorizationService.authorizeByMenuOption(option);
                    if (role == UserRole.SELLER) {
                        runSellerMenu(scanner);
                    } else {
                        runClientMenu(scanner);
                    }
                } catch (IllegalArgumentException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
    }

    private void runSellerMenu(Scanner scanner) {
        boolean inSellerMenu = true;
        while (inSellerMenu) {
            clearScreen();
            printNewlines(3);
            System.out.println("\n--- Seller Menu ---");
            System.out.println("1 - Register game");
            System.out.println("2 - Edit game");
            System.out.println("3 - Exclude game");
            System.out.println("4 - List catalog");
            System.out.println("0 - Back");
            String option = scanner.nextLine().trim();

            switch (option) {
                case "1" -> {
                    clearScreen();
                    registerGame(scanner);
                    pauseBeforeReturn(scanner);
                }
                case "2" -> {
                    clearScreen();
                    editGame(scanner);
                    pauseBeforeReturn(scanner);
                }
                case "3" -> {
                    clearScreen();
                    excludeGame(scanner);
                    pauseBeforeReturn(scanner);
                }
                case "4" -> {
                    clearScreen();
                    printGames(sellerCatalogService.listCatalog());
                    pauseBeforeReturn(scanner);
                }
                case "0" -> inSellerMenu = false;
                default -> {
                    System.out.println("Invalid option.");
                    pauseBeforeReturn(scanner);
                }
            }
        }
    }

    private void runClientMenu(Scanner scanner) {
        clearScreen();
        long clientId = readLong(scanner, "Client id: ");
        System.out.print("Client name: ");
        String name = scanner.nextLine();
        ClientAccount account = clientWalletService.loadOrCreateClient(clientId, name);

        boolean inClientMenu = true;
        while (inClientMenu) {
            clearScreen();
            printNewlines(3);
            System.out.println("\n--- Client Menu ---");
            System.out.println("Client: " + account.getName() + " | Credits: " + account.getCredits());
            System.out.println("1 - Filter games");
            System.out.println("2 - Add game to cart");
            System.out.println("3 - Remove game from cart");
            System.out.println("4 - View cart");
            System.out.println("5 - Add credits");
            System.out.println("6 - Checkout");
            System.out.println("0 - Back");
            String option = scanner.nextLine().trim();

            try {
                switch (option) {
                    case "1" -> {
                        clearScreen();
                        filterGames(scanner);
                        pauseBeforeReturn(scanner);
                    }
                    case "2" -> {
                        clearScreen();
                        addGameToCart(scanner, clientId);
                        pauseBeforeReturn(scanner);
                    }
                    case "3" -> {
                        clearScreen();
                        removeGameFromCart(scanner, clientId);
                        pauseBeforeReturn(scanner);
                    }
                    case "4" -> {
                        clearScreen();
                        viewCart(clientId);
                        pauseBeforeReturn(scanner);
                    }
                    case "5" -> {
                        clearScreen();
                        BigDecimal amount = readBigDecimal(scanner, "Amount to add: ");
                        account = clientWalletService.addCredits(clientId, amount);
                        System.out.println("Credits updated. New balance: " + account.getCredits());
                        pauseBeforeReturn(scanner);
                    }
                    case "6" -> {
                        clearScreen();
                        CheckoutResult result = cartService.checkout(clientId);
                        System.out.println(result.getMessage() + " Total: " + result.getTotal());
                        account = account.toBuilder().credits(result.getRemainingCredits()).build();
                        pauseBeforeReturn(scanner);
                    }
                    case "0" -> inClientMenu = false;
                    default -> {
                        System.out.println("Invalid option.");
                        pauseBeforeReturn(scanner);
                    }
                }
            } catch (IllegalArgumentException | IllegalStateException ex) {
                System.out.println(ex.getMessage());
                pauseBeforeReturn(scanner);
            }
        }
    }

    private void registerGame(Scanner scanner) {
        System.out.print("Title: ");
        String title = scanner.nextLine();
        System.out.print("Genre: ");
        String genre = scanner.nextLine();
        BigDecimal price = readBigDecimal(scanner, "Price: ");

        Game game = sellerCatalogService.registerGame(title, genre, price);
        System.out.println("Registered game with id " + game.getId());
    }

    private void editGame(Scanner scanner) {
        long id = readLong(scanner, "Game id: ");
        System.out.print("New title: ");
        String title = scanner.nextLine();
        System.out.print("New genre: ");
        String genre = scanner.nextLine();
        BigDecimal price = readBigDecimal(scanner, "New price: ");

        boolean updated = sellerCatalogService.editGame(id, title, genre, price);
        System.out.println(updated ? "Game updated." : "Game not found.");
    }

    private void excludeGame(Scanner scanner) {
        long id = readLong(scanner, "Game id to exclude: ");
        boolean excluded = sellerCatalogService.excludeGame(id);
        System.out.println(excluded ? "Game excluded from catalog." : "Game not found.");
    }

    private void filterGames(Scanner scanner) {
        System.out.print("Filter by title (optional): ");
        String title = scanner.nextLine();
        System.out.print("Filter by genre (optional): ");
        String genre = scanner.nextLine();

        List<Game> games = clientCatalogService.filterGames(title, genre);
        printGames(games);
    }

    private void addGameToCart(Scanner scanner, long clientId) {
        long gameId = readLong(scanner, "Game id: ");
        int quantity = (int) readLong(scanner, "Quantity: ");
        cartService.addGame(clientId, gameId, quantity);
        System.out.println("Item added to cart.");
    }

    private void removeGameFromCart(Scanner scanner, long clientId) {
        long gameId = readLong(scanner, "Game id: ");
        boolean removed = cartService.removeGame(clientId, gameId);
        System.out.println(removed ? "Item removed from cart." : "Item not found in cart.");
    }

    private void viewCart(long clientId) {
        List<CartItem> items = cartService.getCart(clientId);
        if (items.isEmpty()) {
            System.out.println("Cart is empty.");
            return;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : items) {
            Game game = gameRepository.findById(item.getGameId())
                    .orElseThrow(() -> new IllegalStateException("Game in cart no longer exists."));
            BigDecimal subtotal = game.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(subtotal);
            System.out.printf("Game %d - %s | qty %d | subtotal %s%n",
                    game.getId(), game.getTitle(), item.getQuantity(), subtotal);
        }
        System.out.println("Cart total: " + total);
    }

    private void printGames(List<Game> games) {
        if (games.isEmpty()) {
            System.out.println("No games found.");
            return;
        }

        for (Game game : games) {
            System.out.printf("%d - %s | %s | %s%n", game.getId(), game.getTitle(), game.getGenre(), game.getPrice());
        }
    }

    private void printMainMenu() {
        clearScreen();
        printNewlines(3);
        System.out.println("\n=== BoxGames ===");
        System.out.println("1 - Seller");
        System.out.println("2 - Client");
        System.out.println("0 - Exit");
    }

    private long readLong(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            try {
                return Long.parseLong(line.trim());
            } catch (NumberFormatException ex) {
                System.out.println("Invalid number.");
            }
        }
    }

    private BigDecimal readBigDecimal(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            try {
                return new BigDecimal(line.trim());
            } catch (NumberFormatException ex) {
                System.out.println("Invalid decimal number.");
            }
        }
    }

    private void seedCatalogIfEmpty() {
        if (!sellerCatalogService.listCatalog().isEmpty()) {
            return;
        }

        sellerCatalogService.registerGame("Cyber Odyssey", "RPG", new BigDecimal("49.90"));
        sellerCatalogService.registerGame("Rocket Arena", "Action", new BigDecimal("29.90"));
        sellerCatalogService.registerGame("Farm Architect", "Simulation", new BigDecimal("19.90"));
    }

    private void clearScreen() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // Fallback: print empty lines
            for (int i = 0; i < 10; i++) {
                System.out.println();
            }
        }
    }

    private void pauseBeforeReturn(Scanner scanner) {
        printNewlines(3);
        System.out.print("Press Enter to continue...");
        scanner.nextLine();
    }

    private void printNewlines(int lines) {
        for (int i = 0; i < lines; i++) {
            System.out.println();
        }
    }
}

