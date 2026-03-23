# BoxGames

Terminal-based game catalog and shopping flow using hexagonal architecture.

## Tech stack

- Java 21
- Maven
- H2 (in-memory)
- Lombok
- JUnit 5 + AssertJ

## Features implemented

- Role authorization via terminal input (`1` seller, `2` client)
- Seller: register, edit, and exclude games from catalog
- Client: filter games by title and genre
- Client: add/remove games in cart
- Client: add credits and checkout purchase
- Purchase history
- Wishlist

## Project structure

- `src/main/java/org/ada/com/domain/model`: domain entities (`Game`, `ClientAccount`, `CartItem`, `UserRole`)
- `src/main/java/org/ada/com/application`: use cases and ports
- `src/main/java/org/ada/com/adapters/in/cli`: terminal adapter (`TerminalApp`)
- `src/main/java/org/ada/com/adapters/out/persistence/h2`: H2 repositories and schema bootstrap
- `src/main/java/org/ada/com/config/AppFactory.java`: dependency wiring

## Run

```bash
mvn clean test
mvn exec:java
```

## Notes

- Database is `jdbc:h2:mem:boxgames;DB_CLOSE_DELAY=-1`, recreated on each run.
- `schema.sql` initializes `games`, `clients`, and `cart_items` tables.

