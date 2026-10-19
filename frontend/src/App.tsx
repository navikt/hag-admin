import { Alert, BodyShort, Button, Heading, Link, TextField } from "@navikt/ds-react";
import { useState } from "react";
import type { FormEvent } from "react";
import { hentEksponerteForespoersler } from "./api";

export function App() {
  const [fnr, setFnr] = useState("");
  const [inputError, setInputError] = useState<string>();
  const [error, setError] = useState<string>();
  const [result, setResult] = useState<string[]>();
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (loading) return;
    setResult(undefined);
    setError(undefined);
    if (!/^\d{11}$/.test(fnr)) {
      setInputError("Skriv inn et fødselsnummer med 11 sifre.");
      return;
    }
    setInputError(undefined);
    setLoading(true);
    try {
      setResult(await hentEksponerteForespoersler(fnr));
    } catch (cause) {
      setError(
        cause instanceof Error && !(cause instanceof TypeError || cause instanceof SyntaxError)
          ? cause.message
          : "Kunne ikke hente forespørsler. Kontroller forbindelsen og prøv igjen.",
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="lookup">
      <Link href="/">Tilbake til HAG Admin</Link>
      <Heading level="1" size="large">Hent eksponerte forespørsler</Heading>
      <BodyShort>Søk etter eksponerte forespørsler for en person.</BodyShort>
      <form onSubmit={submit} noValidate autoComplete="off">
        <TextField
          label="Fødselsnummer"
          description="11 sifre"
          value={fnr}
          inputMode="numeric"
          autoComplete="off"
          spellCheck={false}
          required
          disabled={loading}
          error={inputError}
          onChange={(event) => {
            setFnr(event.target.value);
            setResult(undefined);
            setError(undefined);
            setInputError(undefined);
          }}
        />
        <Button type="submit" loading={loading} disabled={loading}>
          Hent eksponerte forespørselid-er
        </Button>
      </form>
      <div aria-live="polite" aria-busy={loading}>
        {loading && <BodyShort>Henter eksponerte forespørselid-er …</BodyShort>}
        {error && <Alert variant="error" role="alert">{error}</Alert>}
        {result && (
          <section aria-labelledby="result-heading">
            <Heading level="2" size="medium" id="result-heading">Resultat</Heading>
            {result.length === 0 ? (
              <BodyShort>Ingen eksponerte forespørselid-er funnet.</BodyShort>
            ) : (
              <>
                <BodyShort>Antall eksponerte forespørselid-er: {result.length}</BodyShort>
                <ul>{result.map((id) => <li key={id}><code>{id}</code></li>)}</ul>
              </>
            )}
          </section>
        )}
      </div>
    </main>
  );
}
