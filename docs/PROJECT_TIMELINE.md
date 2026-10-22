# Pick Pack Ship WMS — Linha do tempo, estado atual e próximos passos

> Documento de contexto para o agente orquestrador. Gerado em 2026-10-22 a partir do histórico git
> e das decisões de design discutidas e registradas no Notion. O Notion é a fonte de verdade da
> especificação; este arquivo é um resumo macro. Em caso de divergência, vale o Notion
> (páginas "Escopo" e "Especificação Técnica" sob "📦 Pick Pack Ship").

## 1. O que é o projeto

Sistema de gestão de pedidos para armazém (WMS): pedido → separação (pick) → conferência (check)
→ expedição (ship). Microsserviços Java 21 (Spring Boot 4.1) desacoplados via Kafka, um PostgreSQL
independente por serviço, API Gateway (Spring Cloud Gateway) atrás de Nginx. Projeto de estudo,
teste de estresse (k6) e portfólio.

Princípios que valem para todo serviço:

- **Multi-tenancy:** `workspaceId` (vem do JWT) em toda entidade e evento. Cada workspace = 1 armazém.
  Leitura de recurso de outro workspace → **404**; escrita/ação sobre recurso de outro workspace → **403**.
- **Outbox pattern** para publicar eventos (mesma transação da escrita). Poller com `SELECT ... FOR UPDATE SKIP LOCKED`.
- **Idempotência decidida por consumer** (ver tabela na Especificação Técnica), não regra única.
- **Schema gerado pelo Hibernate** (`ddl-auto: update`), sem Flyway/Liquibase por enquanto.
- **Testes:** modelo Honeycomb — integração com Testcontainers é a camada principal; source set `integrationTest` por serviço.
- **Erros:** ProblemDetail (RFC 9457).

## 2. Linha do tempo

| Data | Marco |
|------|-------|
| 2026-09-10 | Base do monorepo: gitignore, **auth-service** (signup, login, JWT), **gateway** (validação de JWT), scaffold de order/stock/picking/checking/shipping, docker-compose + Nginx. |
| 2026-09-16 | Gateway deixa de repassar claims como headers e aceita o token via cookie. Auth separa `UserService/UserController`. **order-service**: sellers e pedidos. |
| 2026-09-21 | order-service ligado no docker-compose; correção de rota no gateway; BouncyCastle p/ Argon2; testes de integração do Auth corrigidos; **paginação/filtros** de pedidos e sellers + testes de integração. |
| 2026-09-25 | README do projeto (arquitetura, padrões, decisões). |
| out/2026 (início) | **Sessão de design do Stock Service** (esta conversa): decisões abaixo, registradas no Notion. Nenhum código novo commitado. |
| 2026-10-22 | Decisões do Stock (produto, posição fixa, cancelamento/putaway, partição) fechadas e registradas no Notion. **Fundação do Stock** implementada (não commitada): compila, `./gradlew check` verde. Auth e Order funcionais. |

## 3. Estado atual

### Serviços

| Serviço | Estado |
|---------|--------|
| Auth | Funcional (signup, login, logout, usuários, exclusão de workspace + outbox `workspace.deleted`). Há **mudanças não commitadas** em `User`, `Workspace`, `AuthService`, `UserService` — origem não discutida nesta conversa; revisar o diff antes de commitar. |
| Gateway / Nginx | Funcional. |
| Order | Funcional (pedidos, sellers, cancelamento, paginação/filtros, outbox). Também com **mudanças não commitadas** em domínio, services e testes de integração (`Order`, `Seller`, `OrderService`, `SellerService`, 6 testes) — origem não discutida; revisar. Pendências conhecidas: restrição de status no cancelamento (até `PICKED`), poller da outbox sem chave de partição. |
| Stock | **Fundação pronta, não commitada.** Compila e `./gradlew check` passa (6 testes de integração com Postgres real). Tem: `Product` (uk por seller em SKU e EAN; `weightG`, `Dimensions` em mm, `Packaging` só inner/master, todos opcionais), `StockItem` (posição fixa, `uk_stock_item_position`, FK `ON DELETE RESTRICT`, saldo pode ser zero), `Warehouse`, `SecurityConfig` (JWT HS256 via cookie), `GlobalExceptionHandler` (ProblemDetail) e source set `integrationTest`. **Ainda sem endpoints, sem réplica de seller (FK `products.seller_id`), sem Kafka, outbox, `Receipt` nem `StockReservation`.** `POST /stock/product` depende de `seller.created` no Order. |
| Picking / Checking / Shipping | Apenas scaffold. |

Observação: o `StockServiceApplicationTests` (contextLoads do scaffold) foi removido do Stock porque exigia banco e `jwt.secret`; o contexto agora é carregado pelos testes de integração. O mesmo teste ainda existe no Order.

Diretórios que existem no repo e **não foram explorados** nesta conversa: `contracts/`, `frontend/`, `docs/images/`.

### Decisões de design tomadas (já documentadas no Notion)

**Produto e unicidade**
- `Product` tem `sellerId`, `sku` (código do produto) e `eanCode` (barcode da unidade).
- Unicidade **por seller**: `uk_products_seller_sku (seller_id, sku)` e `uk_products_seller_ean (seller_id, ean_code)`. `workspace_id` fica fora (o `sellerId` já pertence a um workspace).
- Duplicado → **409** (tratar `DataIntegrityViolationException`, não confiar só em checagem prévia). Normalizar SKU/EAN (trim; SKU maiúsculo) e validar EAN (8/12/13/14 dígitos + dígito verificador).
- **Decidido:** `eanCode` é o identificador principal da unidade; `Packaging.unitBarcode` foi removido. `Packaging` só descreve inner e master box, com `unitsPerInnerBox`/`unitsPerMasterBox` opcionais (produto sem caixa só tem unidades soltas). Alterar `unitsPer...` → 409 enquanto houver saldo em caixa ou reserva ativa.
- **Decidido:** peso em gramas (`weightG`) e dimensões em milímetros (`heightMm`, `widthMm`, `depthMm`). O código antigo usava `length`; foi alinhado à spec (`depth`).
- `sellerId` vai em `order.created` e `stock.discrepancy_reported` (resolução por `seller_id, sku, workspace_id`); **não** vai em `order.cancelled` (estorno é pelo `orderId`).

**Seller dentro do Stock (sem acoplamento síncrono)**
- Order publica `seller.created` / `seller.deleted` pela outbox; Stock mantém **réplica local** `sellers` (`ACTIVE | INACTIVE`, `deletedAt`) com FK em `products.seller_id`.
- Criar produto exige seller `ACTIVE` do workspace do JWT; senão 404.
- `seller.deleted` apenas marca `INACTIVE` — **não apaga produtos**. O ADMIN lista inativos via **filtro `status=INACTIVE` na listagem existente** e decide apagar: `DELETE /stock/product/{productId}` e `DELETE /stock/seller/{sellerId}/products`. Apagado o último produto, a réplica do seller é removida.
- Exclusão de produto: **409 se houver saldo ou reserva em qualquer posição**; transação trava a linha do `Product` (`PESSIMISTIC_WRITE`), verifica todas as posições, apaga as zeradas e depois o produto.
- Consistência eventual aceita (poller do Order roda a cada 30 s).

**StockItem = posição física**
- Relação **N:1** com `Product`; **uma posição por produto**: `uk_stock_item_position (workspace_id, location_block, location_aisle, location_column, location_level)` (prefixo `location_` porque `column` é palavra reservada em SQL). FK `product_id` com `ON DELETE RESTRICT`.
- Saldo do produto = soma das posições, em unidades (`masters × unitsPerMasterBox + inners × unitsPerInnerBox + loose`).
- **Posição fixa por produto:** o `StockItem` é cadastrado previamente pela interface e **pode existir com saldo zero**. A quantidade **não é editável**: só muda por recebimento (soma), reserva e estorno/putaway. Nunca é apagado automaticamente; exclusão manual só por ADMIN, com 409 enquanto houver `StockReservation` ativa. Tem `stockedSince` (quando saiu de saldo zero; base do FIFO).
- **Exclusão de produto:** só sem nenhuma posição cadastrada (com ou sem saldo) → senão 409; o usuário apaga as posições antes. O recebimento trava o mesmo `Product` para não criar posição durante a exclusão.

**Reserva**
- Consome `order.created`. Domain service escolhe as posições por **FIFO** (`stockedSince` asc, desempate `stockItemId`); dentro da posição, guloso master → inner → solta. Limitação: FIFO por posição, não por lote.
- Trava posições sempre em ordem de `stockItemId` (evita deadlock); `UPDATE` condicional por posição.
- Nova tabela **`StockReservation`** (`orderId`, `stockItemId`, masters, inners, loose, `status ACTIVE | AWAITING_PUTAWAY`), `uk_stock_reservation_order_item (order_id, stock_item_id)` → idempotência do `order.created` e base do estorno exato.
- **Cancelamento** (permitido em qualquer status antes de `SHIPPED`; `statusAtCancellation` vai no `order.cancelled`): em `CREATED`/`STOCK_RESERVED`/`PICKING` o Stock devolve a cada posição o que tirou e apaga a reserva na hora; de `PICKED` em diante a reserva vira `AWAITING_PUTAWAY` (fora do saldo) e **só um SUPERVISOR confirma o putaway** (retorno à posição de origem), quando o saldo volta e a reserva é apagada. Sem reservas para o `orderId` (cancelamento chegou antes do `order.created`): grava `cancelled_orders` (unique por `orderId`) e o `order.created` posterior não reserva nada.
- **Partição da outbox:** chave `orderId` (e `sellerId` nos eventos de seller). Como a chave sozinha não garante ordem com várias réplicas e `SKIP LOCKED`, o poller não publica uma linha enquanto houver outra mais antiga e não publicada do mesmo agregado. No Order, guarda que ignora `stock.reserved` de pedido `CANCELLED`.
- `stock.reserved` já carrega `location` por item (mesmo SKU pode vir em várias linhas, cada uma com seu `pickingBreakdown`).

**Recebimento de carga (`Receipt`) — dentro do Stock, sem evento**
- `POST /stock/receipt` (ADMIN), `GET /stock/receipt`, `GET /stock/receipt/{id}`. Um seller por carga; `receivedBy` = `operatorId` do JWT; carga **imutável**.
- Idempotência pela **chave de acesso da NF-e de entrada**: `uk_receipts_seller_nfe_key (seller_id, nfe_access_key)`; validar 44 dígitos + DV (módulo 11); retry → 409 com o `receiptId` existente. Inserir o `Receipt` primeiro na transação.
- Posição ocupada por outro produto → 409 e a carga inteira volta atrás. `innerBoxes/masterBoxes > 0` exige o respectivo `unitsPer...` no `Packaging` (senão 400). Trava `Product` em ordem de `productId`.
- Limitação aceita: uma NF-e = um recebimento (sem parcial); recebimento errado só é corrigido por ajuste de estoque futuro.

## 4. Pendências abertas (não decididas ou não feitas)

Decisões de design em aberto (levar ao dono antes de codar; ver também "Pendências em aberto do Stock Service" no Notion):

1. **Ciclo de vida da `StockReservation` e do `reservedQuantity` depois da expedição** (hoje a reserva só é removida no cancelamento).
2. **`stock.discrepancy_reported`:** o que o Stock faz ao consumir, já que a quantidade não é editável.
3. **N:1 posições por produto:** confirmar com o dono que um produto pode ter várias posições (o código já permite).
4. **Saldo > 0 também bloqueia a exclusão manual da posição?**
5. **Par obrigatório entre barcode de caixa e `unitsPer...`**, e colisão de barcodes entre unidade, inner e master.
6. Reserva tudo ou nada (assumido, não contestado) e conteúdo de `seller.created` (só ids ou também `name`).

Trabalho pendente:

7. Propagar `sellerId` em `order.created` e `stock.discrepancy_reported` (hoje particionado por `sku`).
8. **Backfill** dos sellers já existentes no Order (não geraram `seller.created`).
9. **Poller da outbox sem chave de partição** (`kafkaTemplate.send(topic, payload)`) e sem a guarda por agregado.
10. Order: restrição de status no cancelamento (hoje qualquer pedido pode ser cancelado, inclusive duas vezes) e guarda contra `stock.reserved` de pedido `CANCELLED`.
11. Stock: FK `products.seller_id` → `sellers` (depende da réplica); `ADMIN`/`SUPERVISOR` por endpoint via `@PreAuthorize`.
12. Verificação opcional: CNPJ do emitente (posições 7–20 da chave da NF-e) contra o `document` do seller (exigiria `document` no `seller.created`).
13. Fora de escopo por ora: transferência entre posições, ajuste de inventário, recebimento parcial.
14. Revisar/commitar as mudanças pendentes de Auth e Order (e agora a fundação do Stock) antes de commits novos; o diretório `services/stock-service/bin` é artefato de IDE e está no `.gitignore`.

## 5. Próximos passos (ordem sugerida)

Cada item = commits separados por serviço/módulo (regras em `CLAUDE.md`). Rodar `./gradlew check`
(não só `compileJava`) antes de cada commit.

0. **Higiene:** revisar e commitar as mudanças pendentes de Auth e Order; commitar a fundação do Stock (`feat(stock)`, separando domínio, segurança/handler, build/integrationTest). `StockRepository` já corrigido.
1. `feat(order)`: publicar `seller.created` e `seller.deleted` pela outbox (decidir se leva só `{sellerId, workspaceId}` ou também `name`); integration test com Kafka real. Incluir `sellerId` em `order.created`/`order.cancelled`.
2. `fix(order)`: chave de partição na outbox (`orderId`/`sellerId`) e poller que respeita a ordem por agregado.
3. `feat(stock)`: réplica de seller (tabela, consumers idempotentes) + FK em `products.seller_id` + `POST /stock/product` com validação de seller ativo (404) e normalização/validação de SKU/EAN (409 em duplicado) + backfill. (`Product` e as constraints únicas já existem.)
4. `feat(stock)`: endpoints de `StockItem` (cadastro de posição fixa, exclusão manual por ADMIN), `Warehouse`, listagem de sellers com filtro `status=INACTIVE`, exclusão de produto/lote com as regras de 409.
5. `feat(stock)`: `Receipt` (recebimento por NF-e, idempotente).
6. `feat(stock)`: `GET /stock/availability` e consumer de `order.created` (reserva FIFO + `StockReservation`), publicando `stock.reserved` / `stock.unavailable`; consumer de `order.cancelled` (estorno).
7. Order: consumers de `stock.reserved` / `stock.unavailable`; restrição de status no cancelamento (qualquer status antes de `SHIPPED`) e guarda contra `stock.reserved` de pedido `CANCELLED`.
7b. `feat(stock)`: consumer de `order.cancelled` com liberação imediata até `PICKING`, `AWAITING_PUTAWAY` de `PICKED` em diante, endpoint de putaway (SUPERVISOR) e `cancelled_orders`.
8. Em seguida: Picking → Checking → Shipping (ver Escopo no Notion), `workspace.deleted` nos 5 serviços, CI (`.github/workflows/ci.yml`), contract tests e teste de carga k6.

## 6. Regras de trabalho para quem for executar

- **Commits:** Conventional Commits em inglês, `type(scope): descrição`; commits pequenos, um serviço/módulo por commit; **nunca** adicionar `Co-Authored-By`, `Claude-Session` ou qualquer atribuição de IA. Verificar com `git status` que só os arquivos pretendidos estão staged antes de cada commit.
- **Sem `catch (Exception)` silencioso.** Rodar `./gradlew check` regularmente.
- **Documentar no Notion antes de implementar** mudanças de design (preferência do dono do projeto); atualizar Escopo/Especificação quando uma decisão mudar.
- O dono do projeto valida decisões de design conversando antes de codar; não assumir decisões das seções 4/5 sem confirmar.
- Fontes: Notion — "📦 Pick Pack Ship" (id `3c72b6a2af3b813abf68eea2f3c563ca`), subpáginas Escopo (`3c72b6a2af3b81698fbbe579ee8cfb88`) e Especificação Técnica (`3c72b6a2af3b81fb8a36d3e7c5e983d2`).
