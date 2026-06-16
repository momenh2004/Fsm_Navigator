# FSM Navigator — Architecture du Projet

> Application de navigation intérieure pour la Faculté des Sciences de Monastir.
> Projet de Fin d'Études — Architecture complète : Application Android + Backend Spring Boot + Dashboard React.

---

## Table des matières

1. [Vue d'ensemble](#1-vue-densemble)
2. [Application Android](#2-application-android)
3. [Backend Spring Boot](#3-backend-spring-boot)
4. [Dashboard React Admin](#4-dashboard-react-admin)
5. [Flux de données](#5-flux-de-données)
6. [Algorithmes clés](#6-algorithmes-clés)
7. [Sécurité](#7-sécurité)
8. [Déploiement](#8-déploiement)

---

## 1. Vue d'ensemble

FSM Navigator est un système de navigation intérieure et extérieure composé de trois parties indépendantes qui communiquent via une API REST.

```
┌─────────────────────┐        HTTPS / REST        ┌──────────────────────┐
│   Application       │ ◄────────────────────────► │   Backend            │
│   Android (Java)    │                             │   Spring Boot        │
│                     │                             │   PostgreSQL         │
│  - Navigation WiFi  │                             │                      │
│  - Carte campus     │                             │  - Auth JWT + OTP    │
│  - Mode PMR / TTS   │                             │  - Gestion données   │
└─────────────────────┘                             │  - Stats + graphe    │
                                                    └──────────┬───────────┘
┌─────────────────────┐        HTTPS / REST                   │
│   Dashboard Admin   │ ◄─────────────────────────────────────┘
│   React (Vite)      │
│                     │
│  - Gestion blocs    │
│  - Gestion users    │
│  - Stats WiFi       │
└─────────────────────┘
```

### Stack technique

| Composant | Technologie | Version |
|-----------|-------------|---------|
| Application mobile | Java + Android SDK | API 24–36 |
| Backend | Spring Boot | 4.0.0 |
| Base de données | PostgreSQL | — |
| Auth | JWT (JJWT) | 0.11.5 |
| Dashboard admin | React + Vite | 18.x |
| Graphiques | Recharts | 2.x |
| HTTP client (Android) | HttpURLConnection | natif |
| HTTP client (React) | Axios | 1.x |

---

## 2. Application Android

**Répertoire :** `app/src/main/java/com/fsm/navigator`

### 2.1 Structure des packages

```
com.fsm.navigator
├── AppConfig.java              ← URL backend (ngrok / émulateur)
├── auth/                       ← Authentification & accessibilité
│   ├── TokenManager.java       ← JWT dans SharedPreferences
│   ├── AuthService.java        ← Appels HTTP auth (login, register...)
│   ├── TtsManager.java         ← Synthèse vocale (accessibilité)
│   ├── PmrManager.java         ← Profil mobilité réduite
│   └── PmrDialogHelper.java    ← Boîte de dialogue PMR
├── controller/                 ← Activities + algorithmes
│   ├── SplashActivity.java
│   ├── LoginActivity.java
│   ├── RegisterActivity.java
│   ├── MainActivity.java
│   ├── SearchActivity.java
│   ├── MapActivity.java
│   ├── NavigationActivity.java
│   ├── BlockDetailActivity.java
│   ├── ChangePasswordActivity.java
│   ├── ProfileActivity.java
│   ├── BaseDrawerActivity.java ← Drawer navigation commun
│   ├── AStar.java              ← Algorithme A*
│   ├── KalmanFilter.java       ← Filtre Kalman RSSI
│   └── WeightedKNN.java        ← k-NN pondéré localisation
├── location/                   ← Localisation WiFi + IMU
│   ├── LocationManager.java    ← Orchestrateur de localisation
│   ├── LocationService.java    ← k-NN simplifié
│   ├── StabilityFilter.java    ← Filtre d'hystérésis
│   └── ImuHelper.java          ← Accéléromètre / gyroscope
├── model/                      ← Modèles de données
│   ├── NavigationGraph.java    ← Graphe de navigation + NavPath
│   ├── NavigationNode.java     ← Nœud du graphe
│   ├── PointInteret.java       ← Point d'intérêt (UI)
│   └── ...
├── navigation/
│   └── NavigationManager.java  ← Orchestrateur navigation temps réel
├── service/
│   ├── FavoriService.java      ← Favoris (API)
│   └── HistoryService.java     ← Historique (API)
└── view/
    ├── NavigationView.java     ← Canvas navigation intérieure
    ├── FsmMapView.java         ← Canvas carte campus extérieure
    └── BlocDetailView.java     ← Canvas plan du bâtiment
```

### 2.2 Authentification

```
LoginActivity
  │
  ├─► AuthService.login(email, password)
  │     └─► POST /api/auth/login
  │           ├─ Succès Membre  → { token, email, role }
  │           │    └─► TokenManager.saveToken(token)
  │           │    └─► TokenManager.saveEmail / saveRole
  │           └─ Admin → { requiresOtp: true }
  │                └─► [OTP envoyé par email]
  │
  └─► (si Admin) AuthService.verifyOtp(email, otp)
        └─► POST /api/auth/verify-otp → { token }
              └─► TokenManager.saveToken(token)
```

**TokenManager** stocke dans `SharedPreferences ("fsm_auth_prefs")` :
- `jwt_token` — token JWT
- `user_email` — email de l'utilisateur
- `user_role` — rôle (ETUDIANT / ADMIN)

### 2.3 Navigation — Pipeline complet

```
NavigationActivity
  │
  ├─ isConnectedToFsmWifi() ?
  │     ├─ OUI → startNavigation()          (mode en ligne)
  │     └─ NON → showOfflineDialog()
  │                  ├─ "Voir le chemin" → startOfflineNavigation()
  │                  └─ "Annuler"
  │
  ├─ Mode EN LIGNE (NavigationManager.startNavigation)
  │     └─ Toutes les 4 secondes :
  │           1. scanWifi()            → Map<BSSID, RSSI>
  │           2. KalmanFilter          → RSSI lissés
  │           3. GET /api/fingerprints → empreintes stockées
  │           4. WeightedKNN.locate()  → position courante (salle, x, y)
  │           5. StabilityFilter       → confirmation changement de salle
  │           6. graph.findPath()      → AStar → NavPath
  │           7. onPositionUpdated(currentNode, path) → callback UI
  │
  └─ Mode HORS LIGNE (NavigationManager.startOfflineNavigation)
        1. buildFallback()             → graphe embarqué
        2. findEntree(blocId)          → nœud de départ = entrée du bloc
        3. graph.findPath(entrée → cible) → NavPath
        4. onPositionUpdated(entrée, path)
```

### 2.4 Localisation WiFi — Algorithme

La localisation se fait en 4 étapes enchaînées :

```
Scan WiFi brut
    │
    ▼
KalmanFilter (par BSSID)
  ─ Lisse les fluctuations RSSI
  ─ Paramètres : Q=0.008, R=0.1
    │
    ▼
WeightedKNN (k=3)
  ─ Distance euclidienne dans l'espace RSSI
  ─ Pénalité -100 dBm pour BSSID manquant
  ─ Pondération 1/d²
  ─ Interpolation (x,y) pondérée
    │
    ▼
StabilityFilter
  ─ Score cumulatif par salle
  ─ Changement confirmé après N détections consécutives
    │
    ▼
Position confirmée → NavigationManager
```

### 2.5 Modes de navigation extérieure

Quand la destination est dans un autre bâtiment, l'app affiche d'abord un **trajet extérieur** via BFS sur le graphe campus :

```
Nœuds campus : PCOUR, COUR, A1-6, BIB, BM, BP1, BP2, BC1, BC2, B1, B2, B3, B4, ADM, INF, STH, D1, D2, BC
```

L'interface extérieure (`layoutOutdoor`) affiche :
- Carte campus avec chemin surligné (`FsmMapView`)
- Liste d'étapes textuelles
- Bouton "Je suis arrivé" → phase 2 : navigation intérieure

### 2.6 Activités et navigation entre écrans

```
SplashActivity
    │
    ▼
LoginActivity ──────────────► RegisterActivity
    │
    ▼ (token sauvegardé)
MainActivity ──────────────── Navigation drawer
    ├── SearchActivity             (recherche salle)
    ├── MapActivity                (carte campus)
    ├── NavigationActivity         (navigation)
    ├── BlockDetailActivity        (plan du bâtiment)
    └── ProfileActivity
          ├── ChangePasswordActivity
          └── Favoris / Historique
```

### 2.7 Vues personnalisées (Canvas)

| Vue | Rôle | Blocs couverts |
|-----|------|----------------|
| `NavigationView` | Chemin + position en temps réel | B3, B1 (Palestine), A1-6, BM |
| `BlocDetailView` | Plan statique du bâtiment | B3, B1, A1-6, BM |
| `FsmMapView` | Carte campus extérieure | Tout le campus |

Les blocs sans plan (`BP1`, `BP2`, `B2`, `B4`, `BC`, `BC1`, `BC2`, `BIB`, `INF`) affichent "Plan à venir".

### 2.8 Configuration

**`AppConfig.java`**
```java
BASE_URL = isEmulator()
    ? "http://10.0.2.2:8080"           // émulateur Android Studio
    : "https://scientist-flaring-fondness.ngrok-free.dev";  // appareil physique
FSM_WIFI_SSID = "Wifi-FSM";
```

**`AndroidManifest.xml`** — Permissions déclarées :
- `INTERNET`
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`
- `ACCESS_WIFI_STATE` / `CHANGE_WIFI_STATE`

---

## 3. Backend Spring Boot

**Répertoire :** `src/main/java/com/fsm/navigator/backend`

### 3.1 Structure des packages

```
com.fsm.navigator.backend
├── BackendApplication.java
├── DataInitializer.java          ← Seed données au démarrage
├── security/
│   ├── SecurityConfig.java       ← Config Spring Security + filtre JWT
│   └── JwtUtil.java              ← Génération / validation JWT
├── controller/
│   ├── AuthController.java       ← /api/auth/**
│   ├── AdminController.java      ← /api/admin/**
│   ├── NavigationController.java ← /api/blocs, /api/fingerprints...
│   ├── StatsController.java      ← /api/admin/stats/**
│   ├── FavoriController.java     ← /api/favoris/**
│   ├── HistoryController.java    ← /api/history/**
│   ├── WifiFingerprintController.java
│   └── GraphController.java      ← /api/graph/**
├── model/
│   ├── User.java (abstract)
│   ├── Admin.java
│   ├── Membre.java
│   ├── Bloc.java
│   ├── Etage.java
│   ├── Salle.java
│   ├── PointLocalisation.java
│   ├── WifiFingerprint.java
│   ├── Favori.java
│   ├── NavigationHistory.java
│   ├── NavNode.java
│   └── NavEdge.java
├── repository/                   ← Interfaces JPA
└── service/
    ├── OtpService.java           ← Génération / validation OTP
    └── EmailService.java         ← Envoi email (Gmail SMTP)
```

### 3.2 Modèle de données

```
User (héritage SINGLE_TABLE)
├── Admin    (dtype = 'Admin')
└── Membre   (dtype = 'Membre')

Bloc
└── Etage (1..*)
    └── Salle (1..*)

PointLocalisation (POI)
├── → Salle      (type SALLE)
├── → Etage      (type COULOIR, ESCALIER)
└── → Bloc       (type ENTREE, SORTIE, INTERSECTION)

WifiFingerprint
└── → PointLocalisation

Favori
├── → Membre
└── → Salle

NavigationHistory
├── → Membre
└── → Salle

NavNode ──────── NavEdge ──────── NavNode
(source)                          (destination)
```

**Énumérations :**
- `CategorieSalle` : SALLE_ETUDE, BUREAU
- `TypePOI` : SALLE, COULOIR, ESCALIER, ENTREE, SORTIE, INTERSECTION

### 3.3 Endpoints API

#### Authentification — `/api/auth/**` (public)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/auth/login` | Login → JWT ou OTP si Admin |
| POST | `/api/auth/verify-otp` | Valide OTP admin → JWT |
| POST | `/api/auth/register` | Inscription Membre |
| POST | `/api/auth/change-password` | Changement mot de passe |
| DELETE | `/api/auth/account` | Suppression compte |

#### Navigation — public

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/blocs` | Tous les blocs avec étages et salles |
| GET | `/api/fingerprints` | Toutes les empreintes WiFi |
| GET | `/api/salles` | Toutes les salles |
| GET | `/api/poi` | Tous les points de localisation |
| GET | `/api/poi/bloc/{id}` | POI d'un bloc |
| GET | `/api/graph/nodes` | Nœuds du graphe |
| GET | `/api/graph/edges` | Arêtes du graphe |

#### Admin — `/api/admin/**` (ROLE_ADMIN requis)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET/POST/PUT/DELETE | `/api/admin/users/**` | Gestion utilisateurs |
| GET/POST/PUT/DELETE | `/api/admin/blocs/**` | Gestion bâtiments |
| GET/POST/PUT/DELETE | `/api/admin/etages/**` | Gestion étages |
| GET/POST/PUT/DELETE | `/api/admin/salles/**` | Gestion salles |
| GET/POST/PUT/DELETE | `/api/admin/poi/**` | Gestion POI |
| GET/POST/PUT/DELETE | `/api/admin/fingerprints/**` | Gestion empreintes WiFi |
| GET | `/api/admin/blocs/{id}/export` | Export JSON d'un bloc |
| POST | `/api/admin/blocs/import` | Import JSON d'un bloc |
| GET | `/api/admin/stats/overview` | KPIs généraux |
| GET | `/api/admin/stats/wifi-coverage` | Couverture WiFi par bloc |
| GET | `/api/admin/stats/top-navigated` | Destinations les plus naviguées |
| GET | `/api/admin/stats/activity` | Activité par jour |

> **Exceptions publiques :** `/api/admin/request-otp` et `/api/admin/verify-otp`

### 3.4 Sécurité

```
Requête HTTP
    │
    ▼
JwtFilter (OncePerRequestFilter — classe interne SecurityConfig)
  ─ Lit header "Authorization: Bearer <token>"
  ─ JwtUtil.isTokenValid(token)
  ─ Extrait email + role
  ─ Crée UsernamePasswordAuthenticationToken(email, null, [ROLE_<role>])
  ─ SecurityContextHolder.setAuthentication(...)
    │
    ▼
SecurityConfig.filterChain
  ─ /api/admin/request-otp  → permitAll
  ─ /api/admin/verify-otp   → permitAll
  ─ /api/admin/**           → hasRole("ADMIN")
  ─ /api/auth/**            → permitAll
  ─ reste                   → permitAll
```

**JwtUtil :**
- Algorithme : HS256
- Expiration : 24h (`jwt.expiration=86400000`)
- Claims : `sub` (email), `role`

**OTP Admin :**
1. `POST /api/admin/request-otp` → génère code 6 chiffres, stocké en mémoire (`HashMap`), envoyé par Gmail
2. `POST /api/admin/verify-otp` → valide code, retourne JWT réel avec `role=ADMIN`

### 3.5 Initialisation des données

`DataInitializer` s'exécute au démarrage et peuple la base si vide :

**Compte admin :**
- Email : `hellaramomen8@gmail.com`
- Mot de passe : `admin1234` (haché BCrypt)

**Blocs créés :**

| Code | Nom | Empreintes WiFi |
|------|-----|----------------|
| B3 | Bloc 3 (Informatique) | 16 (salles 301–316) |
| B1 | Bloc 1 (Palestine) | 17 (Amphi A–D, 101–117) |
| B2 | Bloc 2 (Préparatoire) | 14 (salles 201–218) |
| B4 | Bloc 4 | 0 |
| BM | Bloc Mathématique | 9 |
| BP1/BP2 | Blocs Physique | 0 |
| BC1/BC2 | Blocs Chimie | 0 |
| BIB | Bibliothèque | 2 |
| COUR | Cour Rouge | 3 |
| A1-6 | Amphithéâtres 1→6 | 6 |
| D1, D2, THESE, INF, ECOUTE, C | Divers | quelques-uns |

**Graphe de navigation :** 71+ nœuds + 100+ arêtes (NavNode / NavEdge).

---

## 4. Dashboard React Admin

**Répertoire :** `src/`

### 4.1 Structure

```
src/
├── main.jsx                    ← Point d'entrée (BrowserRouter)
├── App.jsx                     ← Router + routes protégées
├── context/
│   ├── AuthContext.jsx         ← Session admin (sessionStorage)
│   └── ThemeContext.jsx        ← Mode clair / sombre
├── services/
│   └── api.js                  ← Instance Axios + intercepteurs
├── pages/
│   ├── Login.jsx               ← Formulaire connexion
│   ├── OtpVerify.jsx           ← Saisie code OTP
│   ├── Dashboard.jsx           ← KPIs + graphiques
│   ├── Coverage.jsx            ← Analyse couverture WiFi
│   ├── Users.jsx               ← Gestion utilisateurs
│   └── Blocs.jsx               ← Gestion bâtiments / salles / fingerprints
├── components/
│   ├── layout/
│   │   ├── Layout.jsx          ← Wrapper avec sidebar
│   │   ├── Sidebar.jsx         ← Menu latéral
│   │   └── CampusMap.jsx       ← Carte campus interactive
│   ├── cards/
│   │   └── KpiCard.jsx         ← Carte métrique
│   └── ui/
│       ├── Spinner.jsx
│       └── Toast.jsx
├── hooks/
│   └── useFetch.js             ← Hook API avec état loading/error
└── config/
    └── coverage.js             ← Mapping couleurs RSSI
```

### 4.2 Routing

```
/login          → Login.jsx         (public)
/otp            → OtpVerify.jsx     (requiert otpEmail en state)
/               → Dashboard.jsx     (protégé)
/coverage       → Coverage.jsx      (protégé)
/users          → Users.jsx         (protégé)
/blocs          → Blocs.jsx         (protégé)
*               → redirect /
```

### 4.3 Flux d'authentification admin

```
Login.jsx
  │ authAPI.login(email, password)
  │ POST /api/auth/login
  ▼
  ├─ { requiresOtp: true }  → navigate('/otp')
  └─ { token }              → _saveSession() → navigate('/')

OtpVerify.jsx
  │ authAPI.verifyOtp(otpEmail, code)
  │ POST /api/auth/verify-otp
  ▼
  { token } → _saveSession() → navigate('/')

_saveSession :
  sessionStorage.setItem('fsm_token', token)
  sessionStorage.setItem('fsm_admin', JSON.stringify(adminObj))
```

> Utilise `sessionStorage` : la session expire à la fermeture de l'onglet.

### 4.4 Intercepteurs Axios

```javascript
// Requête : ajoute le token JWT
api.interceptors.request → Authorization: Bearer <fsm_token>

// Réponse : redirige si 401
api.interceptors.response → 401 → sessionStorage.clear() → /login
```

### 4.5 Pages

| Page | Données affichées | API appelée |
|------|------------------|-------------|
| Dashboard | KPIs, RSSI, top destinations, activité, top users | `statsAPI.*` |
| Coverage | Heatmap couverture WiFi par bloc | `statsAPI.wifiCoverage()`, `statsAPI.uncovered()` |
| Users | Liste membres, création, suppression | `usersAPI.*` |
| Blocs | Arbre blocs/étages/salles, CRUD, fingerprints, POI, import/export | `blocsAPI.*` |

---

## 5. Flux de données

### 5.1 Navigation en ligne

```
Utilisateur choisit une destination
    │
    ▼
NavigationActivity.doNavigateTo(poi)
    ├── HistoryService.logNavigation()  →  POST /api/history/log
    └── NavigationManager.startNavigation(targetNodeId)
            │
            └── [Toutes les 4 s]
                    │
                    ├── WifiManager.startScan()
                    ├── KalmanFilter.filter(scanResults)
                    ├── GET /api/fingerprints          (si pas en cache)
                    ├── WeightedKNN.locate(filtered)   → LocationResult
                    ├── StabilityFilter.update(result) → confirmedNode
                    ├── NavigationGraph.findPath(confirmed, target)
                    └── callback.onPositionUpdated(node, path)
                                │
                                ▼
                         NavigationActivity
                           └── navView.setNavigationData(graph, path, current, dest)
                                   └── canvas.invalidate() → redessine
```

### 5.2 Administration des données

```
Admin → Dashboard React
    │
    ├── GET /api/admin/stats/overview     → KPIs
    ├── GET /api/admin/stats/wifi-coverage → Couverture WiFi
    ├── GET /api/admin/users              → Liste utilisateurs
    ├── POST /api/admin/blocs             → Créer bâtiment
    ├── POST /api/admin/fingerprints      → Ajouter empreinte WiFi
    └── GET /api/admin/blocs/{id}/export  → Export JSON
```

---

## 6. Algorithmes clés

### 6.1 A* (AStar.java)

- **Heuristique :** distance euclidienne entre nœuds
- **Coût :** poids de l'arête (distance réelle en mètres)
- **Optimisation escaliers :** ignore les escaliers si départ et arrivée sont au même étage
- **Sortie :** `NavPath` avec liste de nœuds + instructions en français

### 6.2 Weighted k-NN (WeightedKNN.java)

```
k = 3

Pour chaque empreinte stockée :
  distance = √( Σ (RSSI_mesuré(AP) - RSSI_stocké(AP))² )
  pénalité AP manquant : -100 dBm

Pondération : w_i = 1 / (distance_i² + ε)

Position interpolée :
  x = Σ(w_i × x_i) / Σw_i
  y = Σ(w_i × y_i) / Σw_i

Confiance : 1 / (1 + distance_min)
```

### 6.3 Filtre de Kalman (KalmanFilter.java)

Appliqué par BSSID pour lisser les fluctuations RSSI :

```
Prédiction :   P_pred = P + Q          (Q = 0.008)
Gain Kalman :  K = P_pred / (P_pred + R) (R = 0.1)
Correction :   x̂ = x̂ + K × (mesure - x̂)
               P = (1 - K) × P_pred
```

### 6.4 Filtre de stabilité (StabilityFilter.java)

Évite les changements de salle intempestifs dus aux fluctuations WiFi :
- Score cumulatif par salle à chaque scan
- Changement de salle confirmé uniquement après N scores consécutifs au-dessus du seuil

---

## 7. Sécurité

### 7.1 Backend

| Mécanisme | Implémentation |
|-----------|----------------|
| Hashage mots de passe | BCrypt |
| Tokens | JWT HS256, expiration 24h |
| Auth admin | JWT + OTP email (double facteur) |
| Contrôle d'accès | `hasRole("ADMIN")` sur `/api/admin/**` |
| Filtre JWT | `JwtFilter extends OncePerRequestFilter` |
| CORS | Origines whitelist (localhost:3000, localhost:5173) |

### 7.2 Application Android

| Mécanisme | Implémentation |
|-----------|----------------|
| Stockage token | `SharedPreferences` (MODE_PRIVATE) |
| Transport | HTTPS via ngrok (production) |
| Permissions | Déclarées explicitement dans Manifest |
| Vérification WiFi | `isConnectedToFsmWifi()` avant navigation |

### 7.3 Dashboard React

| Mécanisme | Implémentation |
|-----------|----------------|
| Stockage session | `sessionStorage` (expire à fermeture onglet) |
| Routes protégées | `ProtectedRoute` vérifie `AuthContext.admin` |
| Auto-déconnexion | Intercepteur Axios → 401 → redirect `/login` |

---

## 8. Déploiement

### 8.1 Backend

```
Prérequis : PostgreSQL, Java 21, Maven
Base : fsm_navigator
Port : 8080

Variables d'environnement recommandées :
  DB_PASSWORD, JWT_SECRET, GMAIL_PASSWORD, ADMIN_EMAIL
```

### 8.2 Application Android

```
minSdk  : 24 (Android 7.0)
targetSdk: 36 (Android 15)
Backend URL : AppConfig.java → NGROK_URL (à remplacer par URL prod)
Build release : signé avec keystore
```

### 8.3 Dashboard React

```
Build : npm run build  (Vite → dist/)
Proxy : configurer VITE_API_URL vers le backend
Deploy : fichiers statiques (Nginx, Vercel, etc.)
```

### 8.4 Tunnel de développement (ngrok)

Pour le développement mobile sans déploiement serveur :

```bash
ngrok http --url=scientist-flaring-fondness.ngrok-free.dev 8080
```

L'URL est fixe (domaine statique ngrok) — pas de rebuild APK nécessaire.

---

*Document généré à partir du code source — FSM Navigator PFE 2025-2026*
