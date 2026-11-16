Admin-tools for HAG (Team Helsearbeidsgiver)

Kjøre lokalt: 
Application.kt

Auth er disablet lokalt og i dev-miljøet. (configureSecurity(disabled = Env.isTest()))
(Dev bruker trygdeetaten-tenant, så krever trygdeetaten-bruker og egen AD-gruppe for å virke)

Rollen som gir tilgang i produksjon er: "Sykepenger - forkast forespørsler om inntektsmelding"
https://myaccount.microsoft.com/groups/dda4c4ed-1820-4b25-920f-eb2f54e19d8b

Vi brukte tidligere Helse arbeidsgiver Nais-gruppa, men som en midlertidig løsning endrer vi til denne gruppa slik at noen utvalgte personer i NKS kan få tilgang.
Som en del av denne midlertidige løsningen, er en del interne funksjoner fjernet inntil videre - mest for å unngå forvirring for NKS. 

Frontend for `hentEksponerteForespoersler` ligger som et eget npm-prosjekt i
`frontend/`, med React, TypeScript, Vite og NAVs designsystem Aksel.
Siden er tilgjengelig på `/forespoersler/` og fra forsiden til HAG Admin.

`./gradlew build` laster ned Node, installerer låste npm-avhengigheter, bygger
frontenden og pakker den inn i backend-JAR-en. Ktor serverer både frontend og API;
det trengs ingen egen frontend-server eller Node i runtime-imaget.
Byggingen og deploy-workflowene bruker dermed samme kommando som før.

`./gradlew buildFrontend` bygger bare frontenden


Lokal kjøring: `./gradlew run` starter backend
