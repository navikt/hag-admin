Admin-tools for HAG (Team Helsearbeidsgiver)

Kjøre lokalt: 
Application.kt

Auth er disablet lokalt og i dev-miljøet. (configureSecurity(disabled = Env.isTest()))
(Dev bruker trygdeetaten-tenant, så krever trygdeetaten-bruker med medlemskap i HAG-gruppa for å enables)

Frontend for `hentEksponerteForespoersler` ligger som et eget npm-prosjekt i
`frontend/`, med React, TypeScript, Vite og NAVs designsystem Aksel.
Siden er tilgjengelig på `/forespoersler/` og fra forsiden til HAG Admin.
Eksisterende admin-sider er uendret.

`./gradlew build` laster ned Node, installerer låste npm-avhengigheter, bygger
frontenden og pakker den inn i backend-JAR-en. Ktor serverer både frontend og API;
det trengs ingen egen frontend-server eller Node i runtime-imaget.
Byggingen og deploy-workflowene bruker dermed samme kommando som før.

`./gradlew buildFrontend` bygger bare frontenden, og `./gradlew testFrontend`
kjører frontend-testene. `./gradlew check` inkluderer frontend-testene.
Ved direkte oppstart fra IDE må `./gradlew processResources` kjøres etter
frontend-endringer.

Lokal kjøring: `./gradlew run` starter backend uten Kafka, og
`hentEksponerteForespoersler` returnerer mockede UUID-er. Den bruker
`logback-local.xml`, som kun logger til konsoll. For hot reload,
kjør `./gradlew runFrontend` i tillegg og åpne `http://localhost:5173/forespoersler/`.

Oppslag sender `POST /hentEksponerteForespoersler` med JSON-body
`{"fnr":"..."}` til samme origin og bruker eksisterende autentisering.
Fødselsnummer sendes ikke i URL og lagres ikke i nettleserlagring.
