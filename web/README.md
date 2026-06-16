# FSM Navigator — Admin Dashboard

## Setup

```bash
cd fsm-navigator-admin
npm install
npm run dev
```

Dashboard disponible sur http://localhost:3000

## Pages

- `/` — Dashboard principal (KPIs + Charts)
- `/coverage` — Couverture WiFi + PMR
- `/analytics` — Analytiques navigation
- `/users` — Gestion utilisateurs
- `/blocs` — Blocs & Salles

## Endpoints backend requis

Tous les endpoints `/api/admin/stats/*` existent déjà dans StatsController.java.

Un seul endpoint manquant à ajouter dans le backend :
GET /api/admin/users — liste des utilisateurs (AdminController)

## Backend CORS

Assurer que Spring Boot autorise localhost:3000 :
```java
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
```
