# Frontend

Angular 22 client for Mugloar Mission Control.

```bash
npm ci
npm start      # http://localhost:4200, proxies /api to the backend
npm test       # Vitest, one run
npm run build  # production build
```

`GameStore` is the UI state boundary. Components render state and dispatch intent; `GameApiService` is the only HTTP client for game operations. Keep recommendation policy on the backend so guided manual play and automation cannot drift apart.
