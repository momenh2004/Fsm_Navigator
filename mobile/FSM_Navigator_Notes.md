# FSM Navigator — Notes de projet

Application Android de navigation indoor pour la **Faculté des Sciences de Monastir (FSM)**.

---

## Fonctionnalités

### Authentification
- Inscription (`/api/auth/register`) avec rôle ETUDIANT par défaut
- Connexion (`/api/auth/login`) avec token JWT stocké localement via `TokenManager`
- Changement de mot de passe (`/api/auth/change-password`)
- Suppression de compte (`DELETE /api/auth/account`)
- Mode **PMR** (Personne à Mobilité Réduite) — badge affiché sur l'accueil si activé

### Accueil (`MainActivity`)
- Message de bienvenue personnalisé (prénom extrait de l'email)
- Barre de recherche rapide → redirige vers `SearchActivity`
- Catégories d'accès rapide : Amphithéâtres, Administration, Bibliothèque, Départements
- Destinations populaires : Amphi A, Salle 301, Bibliothèque Centrale
- Navigation Drawer (menu hamburger)

### Recherche (`SearchActivity`)
- Recherche de salles/bureaux par nom ou bâtiment
- Filtrage par catégorie (passé via Intent `FILTER`)
- Données chargées depuis `/api/blocs` (JSON : blocs → étages → salles)

### Navigation (`NavigationActivity` + `NavigationManager`)
- **Mode en ligne** (WiFi FSM requis) :
  - Scan WiFi toutes les 4 secondes
  - Filtre de Kalman pour lisser les signaux RSSI
  - Algorithme **Weighted k-NN** pour détecter la salle courante
  - Algorithme **A\*** pour calculer le chemin optimal
  - Instructions turn-by-turn + distance restante affichées
- **Mode hors ligne** :
  - Dialogue proposé si WiFi FSM non détecté
  - Calcule directement le chemin A\* depuis l'entrée du bloc
- Détection d'arrivée à destination
- Bouton "Stop" pour revenir à la recherche

### Localisation WiFi (`LocationService` / `WeightedKNN` / `KalmanFilter`)
- Scan des réseaux WiFi visibles → `{BSSID → RSSI}`
- Fingerprints récupérés depuis `/api/fingerprints`
- Distance euclidienne entre scan mesuré et fingerprints stockés
- k-NN avec k=3, vote majoritaire + distance totale pour départager
- Confiance calculée : `max(0, 1.0 - distance/30.0)`
- Pénalité -100 dBm pour tout BSSID absent du scan

### Favoris (`FavoriService`)
- Ajout / suppression de salles et blocs en favoris
- Vérification si un lieu est déjà en favori (`/api/favoris/check`)
- Liste des favoris par email utilisateur

### Profil (`ProfileActivity`)
- Affichage email et rôle
- Historique des destinations visitées (`addToHistory`)
- Accès aux favoris

### Plan & Carte
- Vue personnalisée `FsmMapView` (plan SVG/Canvas du campus)
- Vue `NavigationView` pour afficher le chemin calculé
- `BlocDetailView` / `BlockDetailActivity` pour le détail d'un bloc

### Blocs supportés
| Code   | Nom                          |
|--------|------------------------------|
| `BPAL` | Bloc Palestine (Amphithéâtres A/B/C/D) |
| `B3`   | Bloc 3 (salles 301, etc.)    |
| `BIB`  | Bibliothèque Centrale         |
| `A1-6` | Amphis 1 → 6                 |
| `COUR` | Cour / bâtiment principal     |

---

## Problèmes connus

| # | Problème | Fichier concerné |
|---|----------|-----------------|
| 1 | `WifiManager.startScan()` est déprécié (Android 9+) — le système limite à 4 scans / 2 min en arrière-plan | `LocationService.java`, `NavigationManager.java` |
| 2 | `AsyncTask` déprécié depuis Android 11 — risque de fuite mémoire | `AuthService.java` |
| 3 | Le mapping `nodeId` est fragile : construit par manipulation de chaînes (`blocCode + "_" + etageCode + "_" + num`) — échoue si le nom de salle ne contient pas de chiffres | `NavigationActivity.java:336-367` |
| 4 | Si le nœud cible n'existe pas dans le graphe, `findPath()` retourne `null` sans message d'erreur clair pour l'utilisateur | `NavigationManager.java` |
| 5 | L'appel `navView.setBlocId(targetBlocId)` dans `onCreate` peut planter si `targetBlocId` est null et `navView` n'est pas encore initialisé | `NavigationActivity.java:91` |
| 6 | L'adapter RecyclerView de `NavigationActivity` crée ses vues entièrement par code — difficile à maintenir et non testable | `NavigationActivity.java:247-321` |
| 7 | La formule de confiance (`1.0 - dist/30.0`) est calibrée à la main — valeur 30.0 non justifiée, peut donner une confiance négative | `LocationService.java:197` |
| 8 | Les SSIDs FSM reconnus sont codés en dur — tout nouveau point d'accès non listé sera ignoré | `NavigationManager.java:47-49` |
| 9 | Aucune gestion du token expiré : si le JWT expire, les appels authentifiés échouent silencieusement | `AuthService.java`, `FavoriService.java` |
| 10 | Le mode hors ligne démarre toujours depuis l'entrée du bloc, pas depuis la dernière position connue | `NavigationManager.java:113-162` |

---

## To-do list — Phase de test

### Tests fonctionnels
- [ ] **Auth** : tester login / register / change-password / delete-account avec cas valides et invalides (mauvais mot de passe, email inexistant, serveur injoignable)
- [ ] **PMR** : vérifier que le badge apparaît/disparaît correctement selon l'état du switch
- [ ] **Recherche** : tester la recherche avec filtre de catégorie, chaîne vide, chaîne sans résultat
- [ ] **Favoris** : ajouter, vérifier, supprimer pour type SALLE et type BLOC
- [ ] **Historique** : vérifier que les destinations visitées s'ajoutent bien dans le profil

### Tests de navigation
- [ ] Tester la navigation en ligne dans chaque bloc (BPAL, B3, BIB, A1-6)
- [ ] Tester le mode hors ligne pour chaque bloc — vérifier que l'entrée du bloc est bien trouvée
- [ ] Vérifier que tous les nodeIds générés (`BP_AA`, `B3_RDC_301`, `BIB`, etc.) existent dans le graphe `NavigationGraph`
- [ ] Tester l'arrivée à destination (callback `onArrived`)
- [ ] Tester le bouton "Stop" → retour au mode recherche sans crash
- [ ] Tester le dialogue "WiFi FSM non détecté" → choisir "Voir le chemin" vs "Annuler"

### Tests de localisation WiFi
- [ ] Mesurer la précision du k-NN dans chaque salle instrumentée (fingerprints collectés)
- [ ] Tester avec WiFi désactivé → vérifier retour gracieux
- [ ] Tester avec signal faible → vérifier message "Signal faible"
- [ ] Vérifier que le filtre de Kalman réduit bien les oscillations entre scans

### Tests de robustesse
- [ ] Tester sur Android 9, 10, 12+ (comportement scan WiFi throttlé)
- [ ] Tester avec serveur backend inaccessible (timeout, 500) — tous les écrans doivent rester stables
- [ ] Tester rotation écran pendant navigation active → pas de crash / double navigation
- [ ] Tester retour arrière (`finish()`) depuis `NavigationActivity` → `stopNavigation()` bien appelé

### Corrections prioritaires avant test utilisateur
- [ ] Remplacer `AsyncTask` par `ExecutorService` ou coroutines (Kotlin migration possible)
- [ ] Ajouter un layout XML pour les items du RecyclerView dans `NavigationActivity`
- [ ] Ajouter un fallback si `nodeId` n'existe pas dans le graphe (afficher message + suggérer destination la plus proche)
- [ ] Gérer l'expiration du token JWT (intercepter 401 → rediriger vers Login)

---

## Stack technique

| Couche | Technologie |
|--------|------------|
| Mobile | Android (Java), API min SDK à définir |
| Backend | Spring Boot (`/api/auth`, `/api/blocs`, `/api/fingerprints`, `/api/favoris`) |
| Auth | JWT Bearer Token |
| Localisation | WiFi Fingerprinting + Weighted k-NN + Filtre de Kalman |
| Pathfinding | Algorithme A\* |
| Communication | `HttpURLConnection` (REST JSON) |
