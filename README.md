# Inventory Manager

API REST de gestion de stock pour une boutique : catalogue de produits, approvisionnements (achats auprès des fournisseurs) et ventes, avec calcul automatique des marges et de la valeur du stock.

> Auteur : Galand Noah

---

## Fonctionnalités

- **Catalogue** : création, consultation (par id ou paginée) et mise à jour des produits
- **Approvisionnements** : enregistrement d'un achat de marchandises
    - sur un produit existant (le stock est incrémenté et le prix d'achat mis à jour)
    - ou avec création d'un nouveau produit à la volée
- **Ventes** : enregistrement d'une vente, décrémentation automatique du stock et cumul des quantités vendues
- **Calculs comptables** par produit : marge unitaire, marge totale, total des ventes, valeur du stock
- **Historique** des ventes et des approvisionnements, paginé ou filtré par date
- **Cache** des lectures avec invalidation automatique à chaque écriture
- **Documentation interactive** de l'API via Swagger / OpenAPI

---

## Stack technique

| Élément | Technologie |
|---|---|
| Langage | Java |
| Framework | Spring Boot |
| Persistance | Spring Data JPA / Hibernate |
| Validation | Jakarta Validation |
| Cache | Spring Cache (`@Cacheable`, `@CacheEvict`) |
| Documentation API | Swagger / OpenAPI (springdoc) |
| Utilitaires | Lombok, Mappers DTO ↔ entités, SLF4J |

---

## Architecture

Le projet est organisé par module fonctionnel, chacun suivant la même structure en couches.

```
com.galandnoah.inventory_manager
├── product
│   ├── controller      # ProductController
│   ├── service         # ProductService / ProductServiceImpl
│   ├── repository      # ProductRepository
│   ├── entity          # Product
│   ├── dto             # ProductRequest, UpdateProductRequest, ProductResponse
│   └── mapper          # ProductMapper
├── supply
│   ├── controller      # SupplyController
│   ├── service         # SupplyService / SupplyServiceImpl
│   ├── repository      # SupplyRepository
│   ├── entity          # Supply
│   ├── dto             # SupplyRequest, SupplyResponse
│   └── mapper          # SupplyMapper
└── sale
    ├── controller      # SaleController
    ├── service         # SaleService / SaleServiceImpl
    ├── repository      # SaleRepository
    ├── entity          # Sale
    ├── dto             # SaleRequest, SaleResponse
    └── mapper          # SaleMapper
```

### Modèle de données

```
Product (products)              Supply (supplies)             Sale (sales)
─────────────────────           ──────────────────            ──────────────────
id                              id                            id
designation                     product  ───────────┐         product  ───────────┐
sellingPrice                    cost                │         qty                 │
purchasePrice                   qty                 │         amount              │
sellingQty                      madeBy              │         paid                │
stock                           created             │         created             │
threshold                                           │                             │
created                         N Supply ── 1 Product         N Sale ── 1 Product
updated
```

---

## Règles métier

### Produits
- La désignation d'un produit est unique (insensible à la casse).
- À la création : `sellingQty = 0`, dates `created` / `updated` initialisées.
- Calculs exposés dans `ProductResponse` :
    - `margin = sellingPrice - purchasePrice`
    - `totalMargin = margin × sellingQty`
    - `totalSelling = sellingQty × sellingPrice`
    - `value = stock × purchasePrice`

### Approvisionnements
- Il faut fournir **soit** un `productId`, **soit** une `designation`, jamais les deux ni aucun.
- Le prix d'achat unitaire est calculé : `cost / qty` (arrondi).
- Avec `productId` : le stock est incrémenté et le prix d'achat du produit est mis à jour.
- Avec `designation` : un nouveau produit est créé avec le stock initial égal à la quantité achetée.

### Ventes
- La vente est refusée si le stock est insuffisant.
- Si le montant n'est pas fourni, il est calculé : `qty × sellingPrice`, le stock est décrémenté et la quantité vendue du produit est incrémentée.

---

## Endpoints

Préfixe commun : `/api/v1`

### Produits — `/api/v1/products`

| Méthode | Route | Description |
|---|---|---|
| `POST` | `/api/v1/products` | Ajouter un produit au catalogue |
| `GET` | `/api/v1/products/{id}` | Récupérer un produit par son id |
| `GET` | `/api/v1/products?size=&page=` | Lister les produits (pagination) |
| `PUT` | `/api/v1/products/{id}` | Modifier le stock et/ou la désignation |

### Approvisionnements — `/api/v1/supplies`

| Méthode | Route | Description |
|---|---|---|
| `POST` | `/api/v1/supplies` | Enregistrer un approvisionnement |
| `GET` | `/api/v1/supplies?size=&page=` | Lister les approvisionnements |
| `GET` | `/api/v1/supplies/{date}` | Approvisionnements d'une date (`yyyy-MM-dd`) |

### Ventes — `/api/v1/sales`

| Méthode | Route | Description |
|---|---|---|
| `POST` | `/api/v1/sales` | Enregistrer une vente |
| `GET` | `/api/v1/sales?size=&page=` | Lister les ventes (pagination) |
| `GET` | `/api/v1/sales/{date}` | Ventes d'une date (`yyyy-MM-dd`) |

### Exemples

**Créer un produit**

```http
POST /api/v1/products
Content-Type: application/json

{
  "designation": "Savon",
  "sellingPrice": 500,
  "purchasePrice": 350,
  "stock": 20,
  "threshold": 5
}
```

**Approvisionner un produit existant**

```http
POST /api/v1/supplies
Content-Type: application/json

{
  "productId": 1,
  "cost": 7000,
  "qty": 20
}
```

**Approvisionner avec création d'un nouveau produit**

```http
POST /api/v1/supplies
Content-Type: application/json

{
  "designation": "Huile 1L",
  "sellingPrice": 1500,
  "cost": 24000,
  "qty": 24
}
```

**Enregistrer une vente**

```http
POST /api/v1/sales
Content-Type: application/json

{
  "productId": 1,
  "qty": 3,
  "paid": true
}
```

> Les noms de champs exacts dépendent de vos DTO (`ProductRequest`, `SupplyRequest`, `SaleRequest`). Adaptez les exemples si besoin.

---

## Cache

| Cache | Alimenté par | Invalidé par |
|---|---|---|
| `products` | `findProductById` | création / mise à jour produit, approvisionnement, vente |
| `allProducts` | `findAllProduct` | idem |
| `supplies` | `findByDate` (approvisionnements) | création d'un approvisionnement |
| `allSupplies` | `findAll` (approvisionnements) | création d'un approvisionnement |
| `sales` | `findSalesByDate` | création d'une vente |
| `allSales` | `findSales` | création d'une vente |

Toute écriture vide l'intégralité des caches concernés (`allEntries = true`).

---

## Démarrage

### Prérequis

- JDK 21
- Maven 
- Une base de données relationnelle (à configurer dans `application.yaml`)

### Configuration

Exemple de `src/main/resources/application.yaml` (à adapter à votre base) :

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/inventory_manager
spring.datasource.username=votre_utilisateur
spring.datasource.password=votre_mot_de_passe

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

### Lancer l'application

```bash
# Maven
./mvnw spring-boot:run

```

L'API est alors disponible sur `http://localhost:8080`.

### Documentation Swagger

Une fois l'application lancée : `http://localhost:8080/swagger-ui/index.html`

---

