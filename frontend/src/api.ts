export type Forespoersel = { forespoerselId: string } & Record<string, unknown>;

const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function erForespoersel(value: unknown): value is Forespoersel {
  if (typeof value !== "object" || value === null || Array.isArray(value)) return false;
  const id = (value as Record<string, unknown>).forespoerselId;
  return typeof id === "string" && uuid.test(id);
}

export async function hentEksponerteForespoersler(fnr: string): Promise<Forespoersel[]> {
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
  if (!Array.isArray(result) || !result.every(erForespoersel)) {
    throw new Error("Mottok et ugyldig svar fra tjenesten.");
  }
  return result;
}
