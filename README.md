# 🧭 FSM Navigator

> Application mobile de navigation indoor basée sur le WiFi Fingerprinting pour la Faculté des Sciences de Monastir (FSM).

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Java](https://img.shields.io/badge/Language-Java-007396?logo=java&logoColor=white)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot%204.0-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org)
[![React](https://img.shields.io/badge/Dashboard-React%20%2B%20Vite-61DAFB?logo=react&logoColor=black)](https://react.dev)
[![License](https://img.shields.io/badge/License-Academic-lightgrey)](./LICENSE)

---

## 📌 Présentation

FSM Navigator est une application mobile Android permettant à tout usager du campus de la FSM (étudiant, enseignant, visiteur) de :

- **Se localiser en temps réel** grâce aux signaux WiFi (WiFi Fingerprinting)
- **Calculer l'itinéraire optimal** vers n'importe quelle salle du campus via l'algorithme A*
- **Naviguer hors ligne** avec un chemin statique depuis l'entrée du bloc
- **Accéder à un profil personnel** avec historique de navigation et favoris

Le système inclut également un **dashboard d'administration** (React) pour gérer les données du campus et visualiser les statistiques d'utilisation.

---

## 🏛️ Contexte

**Projet de Fin d'Études (PFE)** — Licence Génie Logiciel et Systèmes d'Information  
Faculté des Sciences de Monastir — Université de Monastir — Année 2025/2026

| Rôle | Nom |
|------|-----|
| Auteur | Hellara Mohamed Momen |

---

## ✨ Fonctionnalités

### Application mobile (Android)
| Acteur | Fonctionnalités |
|--------|----------------|
| **Visiteur** | Consulter la carte du campus, rechercher une salle, navigation hors ligne |
| **Utilisateur** | Localisation WiFi temps réel, historique, favoris, gestion du profil |
| **Administrateur** | Tableau de bord, gestion des blocs/salles/empreintes WiFi, gestion des utilisateurs |

### Dashboard web (React)
- KPIs en temps réel (utilisateurs, salles, empreintes WiFi, navigations)
- Couverture WiFi par bloc
- Évolution de l'activité sur 7 jours
- Classement des salles les plus fréquentées
- Gestion des empreintes WiFi

---

## 🗺️ Campus couvert

| Statistique | Valeur |
|-------------|--------|
| Blocs | 12 |
| Salles et POIs | 92 salles / 69 POIs |
| Empreintes WiFi collectées | 460 |
| Profils d'accessibilité PMR/TTS | 4 |

---

## 🔬 Algorithmes

### WiFi Fingerprinting — Localisation (WkNN + Filtre de Kalman)

La localisation s'effectue en **4 secondes** par cycle :

1. **Scan WiFi** — collecte des signaux BSSID/RSSI environnants
2. **Filtre de Kalman** — lissage des RSSI bruités (seuil signal faible : −85 dBm, seuil stabilité : 1,5)
3. **WkNN (k=3)** — comparaison aux 460 empreintes de référence par distance euclidienne sur les BSSID communs, pondération inverse des distances
4. **Position estimée** — coordonnées (x, y, étage) par interpolation pondérée

### Pathfinding — A* sur graphe NavNode/NavEdge

- Graphe de navigation avec nœuds typés : `SALLE`, `COULOIR`, `ESCALIER`, `ENTREE`, `CARREFOUR`
- Heuristique euclidienne
- Filtrage des `ESCALIER` pour les trajets intra-étage
- Profils PMR : exclusion des nœuds `accessiblePmr = false`

---

## 🏗️ Architecture

```
┌─────────────────────────────┐     HTTPS/REST (JWT)     ┌──────────────────────────┐
│   Smartphone Android        │◄────────────────────────►│   Serveur Backend        │
│   FSM Navigator App (Java)  │                           │   Spring Boot 4.0        │
│   Pattern MVC               │                           │   API REST               │
│   Room/SQLite (offline)     │                           │   JWT + OTP Auth         │
└─────────────────────────────┘                           │   PostgreSQL             │
                                                          └──────────────────────────┘
┌─────────────────────────────┐     HTTPS/REST (JWT)              ▲
│   Navigateur Web Admin      │◄──────────────────────────────────┘
│   React + Vite              │
│   TailwindCSS + Recharts    │
└─────────────────────────────┘
```

**Modèle de données clés :**
- `NavNode` / `NavEdge` — graphe de navigation (décorrélé de la BD via `blocId: String`)
- `WifiFingerprint` — empreintes liées aux `PointLocalisation`
- `NavigationHistory` / `Favori` — liés à l'utilisateur par email (persistance après suppression de compte)
- Héritage `User` → `Admin` / `Membre` en stratégie `SINGLE_TABLE`

---

## 🛠️ Stack technique

| Catégorie | Technologies |
|-----------|-------------|
| Application mobile | Android Studio, Java, Room/SQLite |
| Backend | Spring Boot 4.0, JPA/Hibernate, PostgreSQL 15 |
| Dashboard web | React, Vite, TailwindCSS, Recharts, Axios |
| Sécurité | JWT, OTP par email (double authentification admin) |
| Modélisation | PlantUML, UML (Cycle en V) |
| Tests | JUnit (unitaires), Postman (intégration) |
| Versioning | Git |

---

## 🧪 Tests

### Tests unitaires (JUnit) — 66 tests, 100% de réussite

| Suite | Tests | Résultat |
|-------|-------|----------|
| `KalmanFilterTest` | 17 | ✅ PASS |
| `WeightedKNNTest` | 14 | ✅ PASS |
| `NavigationGraphTest` | 35 | ✅ PASS |
| **Total** | **66** | **✅ 100%** |

### Tests d'intégration (Postman) — 3 tests, 100% de réussite

| Endpoint | Résultat |
|----------|----------|
| `POST /api/auth/login` | ✅ 200 OK |
| `GET /api/salles?query=301` | ✅ 200 OK |
| `GET /api/admin/stats/overview` | ✅ 200 OK |

### Tests de validation — 6 scénarios fonctionnels validés

Navigation temps réel, navigation hors ligne, localisation WkNN+Kalman, authentification JWT, gestion des favoris, accessibilité PMR + TTS.

---

## 🚀 Installation et démarrage

### Prérequis

- Java 17+
- Node.js 18+
- PostgreSQL 15+
- Android Studio (pour l'app mobile)

### Backend (Spring Boot)

```bash
# Cloner le dépôt
git clone https://github.com/<your-username>/fsm-navigator.git
cd fsm-navigator/backend

# Configurer la base de données dans application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/fsm_navigator
spring.datasource.username=<your_user>
spring.datasource.password=<your_password>

# Lancer le backend
./mvnw spring-boot:run
```

### Dashboard Admin (React)

```bash
cd fsm-navigator/dashboard
npm install
npm run dev
# Accessible sur http://localhost:5173
```

### Application Android

Ouvrir le dossier `android/` dans Android Studio, synchroniser Gradle, puis exécuter sur un appareil physique connecté au WiFi FSM.

> ⚠️ La localisation WiFi nécessite d'être physiquement sur le campus de la FSM et connecté au réseau WiFi de l'établissement.

---

## 📡 API REST — Endpoints principaux

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/api/auth/login` | Authentification (JWT) |
| `POST` | `/api/auth/register` | Inscription membre |
| `POST` | `/api/auth/verify-otp` | Vérification OTP admin |
| `GET` | `/api/salles?query={nom}` | Recherche de salles |
| `GET` | `/api/fingerprints/{poiId}` | Empreintes WiFi d'un POI |
| `GET` | `/api/admin/stats/overview` | KPIs du dashboard |
| `GET` | `/api/admin/stats/activity` | Activité des 7 derniers jours |
| `POST` | `/api/admin/fingerprints` | Ajout d'une empreinte WiFi |

Tous les endpoints `/api/admin/*` sont protégés par JWT.

---

## 📂 Structure du projet

```
fsm-navigator/
├── android/          # Application mobile Android (Java, MVC)
│   ├── app/src/main/java/
│   │   ├── algorithms/   # KalmanFilter, WeightedKNN, NavigationGraph (A*)
│   │   ├── model/        # NavNode, NavEdge, WifiFingerprint, ...
│   │   ├── view/         # Activities, Fragments
│   │   └── controller/   # NavigationManager, WiFiScanReceiver, ...
│   └── app/src/test/     # 66 tests unitaires JUnit
├── backend/          # API REST Spring Boot 4.0
│   └── src/main/java/
│       ├── controller/   # REST Controllers
│       ├── service/      # Business logic
│       ├── repository/   # JPA Repositories
│       └── model/        # Entités JPA
├── dashboard/        # Admin dashboard React + Vite
│   └── src/
│       ├── components/   # Recharts, TailwindCSS
│       └── api/          # Axios (api.js)
└── docs/             # Rapport PFE, diagrammes UML
```

---

## 🔮 Perspectives d'évolution

- Intégration des établissements voisins (ENIM, IPEIM)
- Extension EduPage pour navigation directe depuis les emplois du temps
- Collecte automatique d'empreintes WiFi via crowdsourcing
- Support des notifications push pour les changements de salle

---

## 📄 Méthodologie

Ce projet a été développé selon la méthodologie du **Cycle en V**, garantissant une traçabilité complète entre les exigences, la conception UML et les phases de tests (unitaires → intégration → validation → recette).

---

## 👤 Auteur

**Hellara Mohamed Momen**  
Licence GLSI — Faculté des Sciences de Monastir  
📧 hellaramomen8@gmail.com

---

*FSM Navigator — Faculté des Sciences de Monastir, 2025/2026*
