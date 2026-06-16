# Architecture MVC — FSM Navigation

## Critère discriminant

| Couche | Règle |
|---|---|
| **Modèle** | Contient les données et la logique métier pure. Aucune importation Android UI. |
| **Vue** | Hérite de `View`, surcharge `onDraw()`. Affiche uniquement, délègue les décisions via des listeners. |
| **Contrôleur** | Reçoit les événements, interroge les modèles, met à jour les vues. |

---

## Modèles

### `NavigationNode.java` — `com.fsm.navigator.model`
Structure de données brute : `id`, `nom`, `x`, `y`, `etage`, `type`, liste de voisins (`Edge`).
La méthode `distanceTo()` est du calcul mathématique pur (`Math.sqrt`).
N'importe aucune classe Android. C'est la définition même d'un modèle.

### `PointInteret.java` — `com.fsm.navigator.model`
Uniquement des champs privés (`id`, `nom`, `categorie`, `batiment`...) avec getters/setters.
Commenté *"Alimenté par le backend via GET /api/blocs"*.
Aucune logique d'affichage : il représente une donnée transportée entre le serveur et l'app.

### `NavigationGraph.java` — `com.fsm.navigator.model`
Construit le graphe multi-bâtiments et exécute l'algorithme A*.
Il ne sait pas comment les résultats seront affichés : il retourne un objet `NavPath` que les autres couches consomment.
La logique appartient au domaine (navigation), pas à l'UI.

### `KalmanFilter.java` — `com.fsm.navigator.location`
Algorithme de filtrage de signal WiFi. Manipule des `float[]` et des mesures brutes.
Aucune dépendance Android UI. Modélise la réalité physique du signal.

### `WeightedKNN.java` — `com.fsm.navigator.location`
Algorithme k-NN pondéré pour la localisation par empreintes WiFi.
Calcule une position (`LocationResult`) à partir de mesures RSSI.
Logique mathématique pure, sans interaction avec l'interface.

### `StabilityFilter.java` — `com.fsm.navigator.location`
Filtre temporel qui lisse les estimations de position pour éviter le jitter.
Opère sur des coordonnées `float` — aucune dépendance UI.

### `ImuHelper.java` — `com.fsm.navigator.location`
Dead reckoning via accéléromètre/gyroscope.
Modélise le déplacement physique de l'utilisateur quand le WiFi est insuffisant.

---

## Vues

### `NavigationView.java` — `com.fsm.navigator.view`
Étend `View`. La méthode centrale est `onDraw(Canvas canvas)` qui dessine les murs,
couloirs, salles et le chemin A* avec `canvas.drawRect()`, `canvas.drawPath()`, etc.
Elle reçoit les données via `setNavigationData(...)` et appelle `invalidate()`.
Elle ne décide rien : elle représente ce qu'on lui donne, et ignore d'où ça vient.

### `FsmMapView.java` — `com.fsm.navigator.view`
Étend `View`. Charge une image bitmap satellite (`campus_visible`) et dans `onDraw()`
dessine les marqueurs de blocs avec zoom/pan via `MotionEvent`.
Quand l'utilisateur clique sur un bloc, elle délègue immédiatement :
```java
if (clickListener != null) clickListener.onBlocClick(closest);
```
Elle gère l'interaction visuelle, mais ne prend aucune décision métier.

### `SearchResultAdapter.java` — `com.fsm.navigator.adapter`
Hérite de `RecyclerView.Adapter`. Prend des objets `PointInteret` (modèle) et les
affiche dans des items XML. Pont entre données et présentation en liste.

### Fichiers XML — `res/layout/`
Vues déclaratives pures (`activity_*.xml`, `item_*.xml`, `dialog_*.xml`).
Définissent la hiérarchie de widgets, marges, couleurs, tailles. Aucune logique.

---

## Contrôleurs

### `NavigationActivity.java` — `com.fsm.navigator.controller`
Étend `AppCompatActivity`. Dans `onCreate()`, lit l'`Intent` (`TARGET_NODE_ID`),
instancie le `NavigationManager`, branche le `NavigationView`, écoute les boutons.
Fait le lien : destination saisie → interroge `NavigationGraph` → envoie le résultat
à `navView.setNavigationData(...)`.

### `MapActivity.java` — `com.fsm.navigator.controller`
Reçoit les clics de `FsmMapView` via `OnBlocClickListener`.
Décide quoi faire : ouvrir `BlockDetailActivity`, lancer une navigation, afficher un Toast.
Coordonne modèle et vue sans faire ni l'un ni l'autre.

### `MainActivity.java` — `com.fsm.navigator.controller`
Gère l'écran d'accueil : tiroir de navigation, barre de recherche rapide, destinations
populaires. Reçoit les interactions utilisateur et dispatch vers les autres Activities.

### `BaseDrawerActivity.java` — `com.fsm.navigator.controller`
Classe de base partagée par toutes les Activities avec tiroir.
Centralise la logique commune : ouverture du drawer, activation du mode PMR,
gestion des items de menu. Les sous-classes héritent de ce comportement contrôleur.

### `SearchActivity.java` — `com.fsm.navigator.controller`
Reçoit la saisie utilisateur, appelle le backend pour récupérer les POI,
alimente le `SearchResultAdapter` (vue) avec les résultats filtrés.

### `LoginActivity.java` / `RegisterActivity.java` — `com.fsm.navigator.controller`
Récupèrent les champs du formulaire, appellent `AuthService` (modèle/service),
stockent le token via `TokenManager`, puis redirigent vers `MainActivity`.

### `NavigationManager.java` — `com.fsm.navigator.navigation`
Contrôleur de service : orchestre le pipeline
WiFi scan → `KalmanFilter` → `WeightedKNN` → `NavigationGraph.findPath()` → callback.
Tient un `Handler` pour planifier les scans périodiques (`SCAN_INTERVAL = 4000ms`).
Coordonne plusieurs modèles pour produire un résultat utilisable par l'Activity.

### `LocationManager.java` — `com.fsm.navigator.location`
Orchestre la chaîne de localisation : instancie `KalmanFilter`, `StabilityFilter`,
`ImuHelper` et les appelle en séquence selon le pipeline documenté.
Décide quelle stratégie utiliser (WiFi ou fallback IMU) — logique de coordination,
ni logique de domaine ni affichage.

### `AuthService.java` / `FavoriService.java` — `com.fsm.navigator.auth` / `service`
Contrôleurs d'infrastructure : gèrent les appels HTTP vers le backend.
Répondent à des intentions utilisateur (login, ajout de favoris) en agissant
sur les données distantes.

### `TokenManager.java` — `com.fsm.navigator.auth`
Gère le stockage et la lecture du JWT dans `SharedPreferences`.
Contrôle l'état de session (connecté / déconnecté) pour les autres contrôleurs.

### `PmrManager.java` / `TtsManager.java` — `com.fsm.navigator.auth`
Contrôleurs d'accessibilité : `PmrManager` active/désactive les profils PMR
(malvoyant, moteur, auditif) ; `TtsManager` déclenche la synthèse vocale.
Ils réagissent à des états applicatifs et agissent sur les services système Android.

### `AppConfig.java` — package racine
Configuration centralisée (URL serveur, détection émulateur/device).
Utilisé par les contrôleurs pour pointer vers le bon backend.

---

## Flux de données résumé

```
Utilisateur (interaction)
        │
        ▼
  Contrôleur (Activity / Manager)
  ├── interroge ──► Modèle (NavigationGraph, LocationManager...)
  │                   └── retourne données (NavPath, LocationResult...)
  └── met à jour ──► Vue (NavigationView, FsmMapView, Adapter)
                        └── appelle invalidate() → onDraw()
```
