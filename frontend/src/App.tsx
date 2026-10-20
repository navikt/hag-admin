import { Alert, BodyShort, Button, Heading, Link, Table, TextField } from "@navikt/ds-react";
import { useState } from "react";
import type { FormEvent } from "react";
import { hentEksponerteForespoersler } from "./api";
import type { Forespoersel } from "./api";

function visVerdi(value: unknown): string {
  if (value === undefined || value === null) return "–";
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}

function visDato(value: unknown): string {
  const dato = typeof value === "string" ? /^(\d{4})-(\d{2})-(\d{2})/.exec(value) : null;
  return dato ? `${dato[3]}.${dato[2]}.${dato[1]}` : visVerdi(value);
}

export function App() {
  const [fnr, setFnr] = useState("");
  const [inputError, setInputError] = useState<string>();
  const [error, setError] = useState<string>();
  const [result, setResult] = useState<Forespoersel[]>();
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
                <BodyShort>Antall eksponerte forespørsel: {result.length}</BodyShort>
                <Table size="small">
                  <Table.Header>
                    <Table.Row>
                      <Table.HeaderCell />
                      <Table.HeaderCell scope="col">Forespørsel-ID</Table.HeaderCell>
                      <Table.HeaderCell scope="col">Orgnr</Table.HeaderCell>
                      <Table.HeaderCell scope="col">Vedtaksperiode-ID</Table.HeaderCell>
                      <Table.HeaderCell scope="col">Opprettet</Table.HeaderCell>
                    </Table.Row>
                  </Table.Header>
                  <Table.Body>
                    {result.map((forespoersel) => (
                      <Table.ExpandableRow
                        key={forespoersel.forespoerselId}
                        content={<pre className="json">{JSON.stringify(forespoersel, null, 2)}</pre>}
                      >
                        <Table.DataCell><code>{forespoersel.forespoerselId}</code></Table.DataCell>
                        <Table.DataCell>{visVerdi(forespoersel.orgnr)}</Table.DataCell>
                        <Table.DataCell><code>{visVerdi(forespoersel.vedtaksperiodeId)}</code></Table.DataCell>
                        <Table.DataCell>{visDato(forespoersel.opprettet)}</Table.DataCell>
                      </Table.ExpandableRow>
                    ))}
                  </Table.Body>
                </Table>
              </>
            )}
          </section>
        )}
      </div>
    </main>
  );
}
