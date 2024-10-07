Admin-tools for HAG (Team Helsearbeidsgiver)

Kjøre lokalt: 
Application.kt

Auth er disablet lokalt og i dev-miljøet. (configureSecurity(disabled = Env.isTest()))
(Dev bruker trygdeetaten-tenant, så krever trygdeetaten-bruker med medlemskap i HAG-gruppa for å enables)

Hvis ønskelig, kan man enable sikkerhet lokalt og starte mock-oauth2-server fra docker-compose fil i src/test/resources og deretter hente token og sende med hvert kall, men på MacOS må da hostname i Env: wellKnownUrl og tokenEndpointUrl endres til host.docker.internal (localhost aksepteres ikke av token-support når app starter)