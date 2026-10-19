export async function hentEksponerteForespoersler(fnr: string): Promise<string[]> {
  const response = await fetch("/hentEksponerteForespoersler", {
    method: "POST",
    credentials: "same-origin",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify({ fnr }),
    cache: "no-store",
  });

  if (!response.ok) {
    if (response.status === 401 || response.status === 403) {
      throw new Error("Du mangler tilgang eller innloggingen har utløpt. Logg inn på nytt.");
    }
    if (response.status === 504) {
      throw new Error("Tidsavbrudd ved henting av forespørsler. Prøv igjen senere.");
    }
    throw new Error("Kunne ikke hente forespørsler. Prøv igjen senere.");
  }

  const result: unknown = await response.json();
  if (
    !Array.isArray(result) ||
    !result.every(
      (id): id is string =>
        typeof id === "string" &&
        /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id),
    )
  ) {
    throw new Error("Mottok et ugyldig svar fra tjenesten.");
  }
  return result;
}
