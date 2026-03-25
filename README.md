# BoxGames

Catálogo e fluxo de compras baseados em terminal utilizando arquitetura hexagonal.

## Tech stack

- Java 21
- Maven
- H2 (in-memory)
- Lombok
- JUnit 5 + AssertJ

## Funcionalidades implementadas

- Autorização por função via entrada no terminal (1 vendedor, 2 cliente)
- Vendedor: registrar, editar e excluir jogos do catálogo
- Cliente: filtrar jogos por título e gênero
- Cliente: adicionar/remover jogos do carrinho
- Cliente: adicionar créditos e finalizar a compra
- Histórico de compras
- Lista de desejos (wishlist)

## Estrutura do projeto

- `src/main/java/org/ada/com/domain/model`: entidades de domínio (`Game`, `ClientAccount`, `CartItem`, `UserRole`)
- `src/main/java/org/ada/com/application`: casos de uso e portas
- `src/main/java/org/ada/com/adapters/in/cli`: adaptador de terminal (`TerminalApp`)
- `src/main/java/org/ada/com/adapters/out/persistence/h2`:h2: repositórios H2 e bootstrap do schema
- `src/main/java/org/ada/com/config/AppFactory.java`: configuração de injeção de dependências

## Executar

```bash
mvn clean test
mvn exec:java
```

## Notes

- O banco de dados é `jdbc:h2:mem:boxgames;DB_CLOSE_DELAY=-1`, recriado a cada execução.
- `schema.sql` inicializa as tabelas `games`, `clients`, e `cart_items`.

