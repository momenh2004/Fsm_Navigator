# FSM Navigator — Class Diagram

## Entities Overview

| Entity | Table | Description |
|---|---|---|
| `Bloc` | `blocs` | A building block of the FSM campus |
| `Etage` | `etages` | A floor within a bloc |
| `Salle` | `salles` | A room on a floor |
| `PointLocalisation` | `poi` | A WiFi localization point (POI) |
| `Fingerprint` | `fingerprints` | A WiFi fingerprint linked to a POI |
| `User` | `users` | An authenticated user |
| `Favori` | `favoris` | A user's saved favourite (room or bloc) |
| `NavigationHistory` | `navigation_history` | A record of a navigation or room view event |

---

## Bloc

**Table:** `blocs`

| Field | Java Type | Column | Constraints | Default |
|---|---|---|---|---|
| `id` | `Long` | `id` | PK, AUTO_INCREMENT | — |
| `code` | `String` | `code` | NOT NULL, UNIQUE | — |
| `nom` | `String` | `nom` | NOT NULL | — |
| `description` | `String` | `description` | nullable | — |
| `accessiblePmr` | `boolean` | `accessible_pmr` | — | `false` |
| `etages` | `List<Etage>` | *(mapped by bloc)* | OneToMany, CASCADE ALL | — |

**Constructors:**
- `Bloc(code, nom, accessiblePmr)`
- `Bloc(code, nom, description, accessiblePmr)`

**Enum:** none

---

## Etage

**Table:** `etages`

| Field | Java Type | Column | Constraints | Default |
|---|---|---|---|---|
| `id` | `Long` | `id` | PK, AUTO_INCREMENT | — |
| `numero` | `int` | `numero` | NOT NULL | — |
| `label` | `String` | `label` | NOT NULL | — |
| `accessiblePmr` | `boolean` | `accessible_pmr` | — | `false` |
| `bloc` | `Bloc` | `bloc_id` | ManyToOne → Bloc, NOT NULL, @JsonIgnore | — |
| `salles` | `List<Salle>` | *(mapped by etage)* | OneToMany, CASCADE ALL | — |

**Notes:**
- `numero = 0` means RDC (ground floor)
- `accessiblePmr = false` means stairs-only access

**Constructors:**
- `Etage(numero, label, accessiblePmr, bloc)`

---

## Salle

**Table:** `salles`

| Field | Java Type | Column | Constraints | Default |
|---|---|---|---|---|
| `id` | `Long` | `id` | PK, AUTO_INCREMENT | — |
| `nom` | `String` | `nom` | NOT NULL | — |
| `categorie` | `String` | `categorie` | NOT NULL | — |
| `ordreDepuisEntree` | `int` | `ordre_depuis_entree` | — | — |
| `entreeReference` | `String` | `entree_reference` | nullable | — |
| `accessiblePmr` | `boolean` | `accessible_pmr` | — | `true` |
| `estSalleEtude` | `boolean` | `est_salle_etude` | — | `true` |
| `etage` | `Etage` | `etage_id` | ManyToOne → Etage, NOT NULL, @JsonIgnore | — |

**Notes:**
- `estSalleEtude = true` → lecture hall / lab / amphitheatre
- `estSalleEtude = false` → office or other

**Constructors:**
- `Salle(nom, categorie, ordreDepuisEntree, entreeReference, accessiblePmr, etage)`
- `Salle(nom, categorie, ordreDepuisEntree, entreeReference, direction, accessiblePmr, etage)`

---

## PointLocalisation

**Table:** `poi`

| Field | Java Type | Column | Constraints | Default |
|---|---|---|---|---|
| `id` | `Long` | `id` | PK, AUTO_INCREMENT | — |
| `nom` | `String` | `nom` | NOT NULL | — |
| `type` | `Type` (enum) | `type` | NOT NULL, EnumType.STRING | — |
| `x` | `float` | `x` | NOT NULL | `0f` |
| `y` | `float` | `y` | NOT NULL | `0f` |
| `accessiblePmr` | `boolean` | `accessible_pmr` | — | `false` |
| `salle` | `Salle` | `salle_id` | ManyToOne → Salle, nullable, @JsonIgnore | — |
| `etage` | `Etage` | `etage_id` | ManyToOne → Etage, nullable, @JsonIgnore | — |
| `bloc` | `Bloc` | `bloc_id` | ManyToOne → Bloc, nullable, @JsonIgnore | — |
| `fingerprints` | `List<Fingerprint>` | *(mapped by poi)* | OneToMany, CASCADE ALL | — |

**Enum `Type`:**

| Value | Required relation |
|---|---|
| `SALLE` | `salle_id` |
| `COULOIR` | `etage_id` |
| `ESCALIER` | `etage_id` |
| `ENTREE` | `bloc_id` |
| `SORTIE` | `bloc_id` |
| `RAMPE` | `bloc_id` |

**Constructors:**
- `PointLocalisation(nom, x, y, accessiblePmr, salle)` — for SALLE
- `PointLocalisation(nom, type, x, y, accessiblePmr, etage)` — for COULOIR / ESCALIER
- `PointLocalisation(nom, type, x, y, accessiblePmr, bloc)` — for ENTREE / SORTIE / RAMPE

**Derived getters (for frontend):**
- `getSalleId()`, `getEtageId()`, `getBlocId()`, `getBlocCode()`, `getEtageNumero()`

---

## Fingerprint

**Table:** `fingerprints`

| Field | Java Type | Column | Constraints | Default |
|---|---|---|---|---|
| `id` | `Long` | `id` | PK, AUTO_INCREMENT | — |
| `bssid` | `String` | `bssid` | NOT NULL | — |
| `ssid` | `String` | `ssid` | nullable | — |
| `rssiMoyen` | `double` | `rssi_moyen` | — | — |
| `poi` | `PointLocalisation` | `poi_id` | ManyToOne → PointLocalisation, NOT NULL, @JsonIgnore | — |

**Constructors:**
- `Fingerprint(bssid, ssid, rssiMoyen, poi)`

**Derived getters (for frontend):**
- `getPoiNom()`, `getPoiId()`, `getPoiType()`, `getSalleId()`, `getBlocCode()`

---

## User

**Table:** `users`

| Field | Java Type | Column | Constraints | Default |
|---|---|---|---|---|
| `id` | `Long` | `id` | PK, AUTO_INCREMENT | — |
| `email` | `String` | `email` | NOT NULL, UNIQUE | — |
| `password` | `String` | `password` | NOT NULL (BCrypt hashed) | — |
| `role` | `Role` (enum) | `role` | NOT NULL, EnumType.STRING | `ETUDIANT` |

**Enum `Role`:**

| Value | Description |
|---|---|
| `ETUDIANT` | Student user |
| `VISITEUR` | Visitor user |
| `ADMIN` | Administrator |

**Constructors:**
- `User(email, password, role)`

---

## Favori

**Table:** `favoris`

| Field | Java Type | Column | Constraints | Default |
|---|---|---|---|---|
| `id` | `Long` | `id` | PK, AUTO_INCREMENT | — |
| `userEmail` | `String` | `user_email` | NOT NULL | — |
| `type` | `TypeFavori` (enum) | `type` | NOT NULL, EnumType.STRING | — |
| `salle` | `Salle` | `salle_id` | ManyToOne → Salle, nullable | — |
| `bloc` | `Bloc` | `bloc_id` | ManyToOne → Bloc, nullable | — |
| `nom` | `String` | `nom` | NOT NULL | — |
| `createdAt` | `LocalDateTime` | `created_at` | NOT NULL, set via @PrePersist | — |

**Enum `TypeFavori`:**

| Value | Active relation |
|---|---|
| `SALLE` | `salle_id` |
| `BLOC` | `bloc_id` |

**Lifecycle:** `@PrePersist` sets `createdAt = LocalDateTime.now()`

---

## NavigationHistory

**Table:** `navigation_history`

| Field | Java Type | Column | Constraints | Default |
|---|---|---|---|---|
| `id` | `Long` | `id` | PK, AUTO_INCREMENT | — |
| `salleId` | `Long` | `salle_id` | — | — |
| `salleNom` | `String` | `salle_nom` | — | — |
| `blocCode` | `String` | `bloc_code` | — | — |
| `type` | `Type` (enum) | `type` | NOT NULL, EnumType.STRING | — |
| `userEmail` | `String` | `user_email` | — | — |
| `createdAt` | `LocalDateTime` | `created_at` | — | `LocalDateTime.now()` |

**Enum `Type`:**

| Value | Description |
|---|---|
| `NAVIGATION` | User launched a navigation to this room |
| `VIEW` | User viewed the room in search results |

**Notes:**
- No FK constraint on `salleId` / `blocCode` — stored as raw values for history resilience (room may be deleted later)

**Constructors:**
- `NavigationHistory(salleId, salleNom, blocCode, type, userEmail)`

---

## Relationships

```
Bloc           1 ──────────────────────< *  Etage
Etage          1 ──────────────────────< *  Salle
Salle          1 ──────────────────────< *  PointLocalisation  (type = SALLE)
Etage          1 ──────────────────────< *  PointLocalisation  (type = COULOIR, ESCALIER)
Bloc           1 ──────────────────────< *  PointLocalisation  (type = ENTREE, SORTIE, RAMPE)
PointLocalisation  1 ──────────────────< *  Fingerprint

Salle          1 ──────────────────────< *  Favori  (TypeFavori = SALLE)
Bloc           1 ──────────────────────< *  Favori  (TypeFavori = BLOC)

User           (linked by email string to)  Favori.userEmail
User           (linked by email string to)  NavigationHistory.userEmail
```

### Cascade Summary

| Parent | Child | Cascade |
|---|---|---|
| Bloc | Etage | ALL |
| Etage | Salle | ALL |
| PointLocalisation | Fingerprint | ALL |

### @JsonIgnore Summary

Relations marked `@JsonIgnore` to prevent infinite recursion during serialization:

| Entity | Ignored field |
|---|---|
| Etage | `bloc` |
| Salle | `etage` |
| PointLocalisation | `salle`, `etage`, `bloc` |
| Fingerprint | `poi` |
