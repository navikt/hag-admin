Admin-tools for HAG (Team Helsearbeidsgiver)

Kjøre lokalt: 
Application.kt

Auth er disablet lokalt og i dev-miljøet. (configureSecurity(disabled = Env.isTest()))
(Dev bruker trygdeetaten-tenant, så krever trygdeetaten-bruker med medlemskap i HAG-gruppa for å enables)

Frontend for `hentEksponerteForespoersler` ligger som et eget npm-prosjekt i
`frontend/`, med React, TypeScript, Vite og NAVs designsystem Aksel.
Siden er tilgjengelig på `/forespoersler/` og fra forsiden til HAG Admin.

`./gradlew build` laster ned Node, installerer låste npm-avhengigheter, bygger
frontenden og pakker den inn i backend-JAR-en. Ktor serverer både frontend og API;
det trengs ingen egen frontend-server eller Node i runtime-imaget.
Byggingen og deploy-workflowene bruker dermed samme kommando som før.

`./gradlew buildFrontend` bygger bare frontenden


Lokal kjøring: `./gradlew run` starter backend
