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
import org.ada.com.application.service.CouponService;
import org.ada.com.application.service.SellerCatalogService;
import org.ada.com.domain.model.CartItem;
import org.ada.com.domain.model.ClientAccount;
import org.ada.com.domain.model.Coupon;
import org.ada.com.domain.model.Game;
import org.ada.com.domain.model.Order;
import org.ada.com.domain.model.OrderItem;
import org.ada.com.domain.model.UserRole;
import org.ada.com.domain.model.WishlistItem;

public class TerminalApp {

    private final AuthorizationService authorizationService;
    private final SellerCatalogService sellerCatalogService;
    private final ClientCatalogService clientCatalogService;
    private final ClientWalletService clientWalletService;
    private final CartService cartService;
    private final CouponService couponService;
    private final GameRepository gameRepository;

    public TerminalApp(
            AuthorizationService authorizationService,
            SellerCatalogService sellerCatalogService,
            ClientCatalogService clientCatalogService,
            ClientWalletService clientWalletService,
            CartService cartService,
            CouponService couponService,
            GameRepository gameRepository) {
        this.authorizationService = authorizationService;
        this.sellerCatalogService = sellerCatalogService;
        this.clientCatalogService = clientCatalogService;
        this.clientWalletService = clientWalletService;
        this.cartService = cartService;
        this.couponService = couponService;
        this.gameRepository = gameRepository;
    }

    public void run() {
        seedCatalogIfEmpty();

        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
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
            System.out.println("\n--- Seller Menu ---");
            System.out.println("1 - Register game");
            System.out.println("2 - Edit game");
            System.out.println("3 - Exclude game");
            System.out.println("4 - List catalog");
            System.out.println("5 - Manage coupons");
            System.out.println("0 - Back");
            String option = scanner.nextLine().trim();

            switch (option) {
                case "1" -> registerGame(scanner);
                case "2" -> editGame(scanner);
                case "3" -> excludeGame(scanner);
                case "4" -> printGames(sellerCatalogService.listCatalog());
                case "5" -> runCouponMenu(scanner);
                case "0" -> inSellerMenu = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void runCouponMenu(Scanner scanner) {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n--- Coupon Menu ---");
            System.out.println("1 - Create coupon");
            System.out.println("2 - Edit coupon");
            System.out.println("3 - Delete coupon");
            System.out.println("4 - List coupons");
            System.out.println("0 - Back");
            String option = scanner.nextLine().trim();

            try {
                switch (option) {
                    case "1" -> createCoupon(scanner);
                    case "2" -> editCoupon(scanner);
                    case "3" -> deleteCoupon(scanner);
                    case "4" -> listCoupons();
                    case "0" -> inMenu = false;
                    default -> System.out.println("Invalid option.");
                }
            } catch (IllegalArgumentException | IllegalStateException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private void createCoupon(Scanner scanner) {
        System.out.print("Coupon code: ");
        String code = scanner.nextLine();
        BigDecimal pct = readBigDecimal(scanner, "Discount percentage (e.g. 10 for 10%): ");
        Coupon coupon = couponService.createCoupon(code, pct);
        System.out.println("Coupon created with id " + coupon.getId() + " | code: " + coupon.getCode());
    }

    private void editCoupon(Scanner scanner) {
        long id = readLong(scanner, "Coupon id: ");
        System.out.print("New code: ");
        String code = scanner.nextLine();
        BigDecimal pct = readBigDecimal(scanner, "New discount percentage: ");
        boolean active = readBoolean(scanner, "Active? (y/n): ");
        boolean updated = couponService.editCoupon(id, code, pct, active);
        System.out.println(updated ? "Coupon updated." : "Coupon not found.");
    }

    private void deleteCoupon(Scanner scanner) {
        long id = readLong(scanner, "Coupon id to delete: ");
        boolean deleted = couponService.deleteCoupon(id);
        System.out.println(deleted ? "Coupon deleted." : "Coupon not found.");
    }

    private void listCoupons() {
        List<Coupon> coupons = couponService.listCoupons();
        if (coupons.isEmpty()) {
            System.out.println("No active coupons.");
            return;
        }
        for (Coupon c : coupons) {
            System.out.printf("%d - %s | %s%%%n", c.getId(), c.getCode(), c.getDiscountPct());
        }
    }

    private void runClientMenu(Scanner scanner) {
        long clientId = readLong(scanner, "Client id: ");
        System.out.print("Client name: ");
        String name = scanner.nextLine();
        ClientAccount account = clientWalletService.loadOrCreateClient(clientId, name);

        boolean inClientMenu = true;
        while (inClientMenu) {
            System.out.println("\n--- Client Menu ---");
            System.out.println("Client: " + account.getName() + " | Credits: " + account.getCredits());
            System.out.println("1 - Filter games");
            System.out.println("2 - Add game to cart");
            System.out.println("3 - Remove game from cart");
            System.out.println("4 - View cart");
            System.out.println("5 - Add credits");
            System.out.println("6 - Checkout");
            System.out.println("7 - List All Games");
            System.out.println("8 - Add game to wishlist");
            System.out.println("9 - View wishlist");
            System.out.println("10 - Move wishlist item to cart");
            System.out.println("11 - View order history");
            System.out.println("0 - Back");
            String option = scanner.nextLine().trim();

            try {
                switch (option) {
                    case "1" -> filterGames(scanner);
                    case "2" -> addGameToCart(scanner, clientId);
                    case "3" -> removeGameFromCart(scanner, clientId);
                    case "4" -> viewCart(clientId);
                    case "5" -> {
                        BigDecimal amount = readBigDecimal(scanner, "Amount to add: ");
                        account = clientWalletService.addCredits(clientId, amount);
                        System.out.println("Credits updated. New balance: " + account.getCredits());
                    }
                    case "6" -> {
                        System.out.print("Apply coupon code? (leave blank to skip): ");
                        String couponCode = scanner.nextLine().trim();
                        CheckoutResult result = cartService.checkout(clientId, couponCode.isEmpty() ? null : couponCode);
                        if (result.getDiscount() != null && result.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
                            System.out.printf("Coupon '%s' applied! Discount: %s%n",
                                    result.getCouponCode(), result.getDiscount());
                        }
                        System.out.println(result.getMessage() + " Total: " + result.getTotal()
                                + " | Remaining credits: " + result.getRemainingCredits());
                        account = account.toBuilder().credits(result.getRemainingCredits()).build();
                    }
                    case "7" -> {
                        List<Game> games = clientCatalogService.filterGames(null, null);
                        printGames(games);
                    }
                    case "8" -> addGameToWishlist(scanner, clientId);
                    case "9" -> viewWishlist(clientId);
                    case "10" -> moveWishlistItemToCart(scanner, clientId);
                    case "11" -> viewOrderHistory(clientId);
                    case "0" -> inClientMenu = false;
                    default -> System.out.println("Invalid option.");
                }
            } catch (IllegalArgumentException | IllegalStateException ex) {
                System.out.println(ex.getMessage());
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

    private void addGameToWishlist(Scanner scanner, long clientId) {
        long gameId = readLong(scanner, "Game id: ");
        cartService.addGameToWishlist(clientId, gameId);
        System.out.println("Item added to wishlist.");
    }

    private void moveWishlistItemToCart(Scanner scanner, long clientId) {
        long gameId = readLong(scanner, "Game id: ");
        int quantity = (int) readLong(scanner, "Quantity: ");
        boolean moved = cartService.moveGameFromWishlistToCart(clientId, gameId, quantity);
        System.out.println(moved ? "Item moved from wishlist to cart." : "Item not found in wishlist.");
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

    private void viewWishlist(long clientId) {
        List<WishlistItem> items = cartService.getWishlist(clientId);
        if (items.isEmpty()) {
            System.out.println("Wishlist is empty.");
            return;
        }

        for (WishlistItem item : items) {
            Game game = gameRepository.findById(item.getGameId())
                    .orElseThrow(() -> new IllegalStateException("Game in wishlist no longer exists."));
            System.out.printf("Game %d - %s | %s | %s%n", game.getId(), game.getTitle(), game.getGenre(), game.getPrice());
        }
    }

    private void viewOrderHistory(long clientId) {
        List<Order> orders = cartService.getOrderHistory(clientId);
        if (orders.isEmpty()) {
            System.out.println("No orders found.");
            return;
        }

        for (Order order : orders) {
            System.out.printf("Order %d | created at %s | total %s%n",
                    order.getId(), order.getCreatedAt(), order.getTotal());
            for (OrderItem item : order.getItems()) {
                System.out.printf("  Game %d - %s | qty %d | unit %s | subtotal %s%n",
                        item.getGameId(),
                        item.getGameTitle(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal());
            }
        }
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

    private boolean readBoolean(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim().toLowerCase();
            if (line.equals("y") || line.equals("yes")) return true;
            if (line.equals("n") || line.equals("no"))  return false;
            System.out.println("Please enter y or n.");
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
}
