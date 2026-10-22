import {BodyShort, Detail, Heading, Label, Link, LocalAlert, ReadMore, Search, Select, Tag} from "@navikt/ds-react";
import {useState} from "react";
import type {FormEvent} from "react";
import {hentEksponerteForespoersler} from "./api";
import type {Forespoersel} from "./api";
import navLogo from "./NavLogo.svg";

function visVerdi(value: unknown): string {
    if (value === undefined || value === null) return "–";
    if (typeof value === "object") return JSON.stringify(value);
    return String(value);
}

function visDato(value: unknown): string {
    const dato = typeof value === "string" ? /^(\d{4})-(\d{2})-(\d{2})/.exec(value) : null;
    return dato ? `${dato[3]}.${dato[2]}.${dato[1]}` : visVerdi(value);
}

function visPeriode(value: unknown): string | undefined {
    if (!Array.isArray(value) || value.length === 0) return undefined;
    const fom = (value[0] as { fom?: unknown }).fom;
    const tom = (value[value.length - 1] as { tom?: unknown }).tom;
    return `${visDato(fom)} – ${visDato(tom)}`;
}

function ForespoerselKort({forespoersel}: { forespoersel: Forespoersel }) {
    const periode = visPeriode(forespoersel.sykmeldingsperioder);
    return (
        <article className="card" aria-labelledby={`fsp-${forespoersel.forespoerselId}`}>
            <div className="card__top">
                <Detail uppercase>Org.nr. {visVerdi(forespoersel.orgnr)}</Detail>
                {typeof forespoersel.erBesvart === "boolean" && (
                    <Tag
                        variant="outline"
                        size="small"
                        data-color={forespoersel.erBesvart ? "success" : "warning"}
                    >
                        {forespoersel.erBesvart ? "Besvart" : "Ikke besvart"}
                    </Tag>
                )}
            </div>
            <Heading level="3" size="small" id={`fsp-${forespoersel.forespoerselId}`} className="card__title">
                Forespørsel {forespoersel.forespoerselId}
            </Heading>
            {periode && <BodyShort>Gjelder sykmeldingsperiode {periode}</BodyShort>}
            <div>
                <Label as="p" size="small">Vedtaksperiode-ID</Label>
                <BodyShort size="small"><code>{visVerdi(forespoersel.vedtaksperiodeId)}</code></BodyShort>
            </div>
            <Detail>Opprettet {visDato(forespoersel.opprettet)}</Detail>
            <ReadMore header="Vis hele objektet" size="small">
                <pre className="json">{JSON.stringify(forespoersel, null, 2)}</pre>
            </ReadMore>

        </article>
    );
}

export function App() {
    const [fnr, setFnr] = useState("");
    const [inputError, setInputError] = useState<string>();
    const [error, setError] = useState<string>();
    const [result, setResult] = useState<Forespoersel[]>();
    const [loading, setLoading] = useState(false);
    const [sortering, setSortering] = useState<"nyeste" | "eldste">("nyeste");
    const sortert = result && [...result].sort((a, b) => {
        const forskjell = String(b.opprettet ?? "").localeCompare(String(a.opprettet ?? ""));
        return sortering === "nyeste" ? forskjell : -forskjell;
    });

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
        <div className="page">
            <div className="topbar">
                <div className="topbar__inner">
                    <a href="/" className="topbar__home">
                        <img src={navLogo} alt="Nav" className="topbar__logo"/>
                        <span>HAG Admin</span>
                    </a>
                </div>
            </div>
            <header className="page-header">
                <div className="page-header__inner">
                    <nav aria-label="Brødsmulesti" className="breadcrumbs">
                        <Link href="/">HAG Admin</Link>
                        <span aria-hidden="true">›</span>
                        <span aria-current="page">Eksponerte forespørsler</span>
                    </nav>
                    <Heading level="1" size="xlarge">Eksponerte forespørsler</Heading>
                </div>
            </header>
            <div className="page-body">
                <aside className="sidebar">
                    <form role="search" onSubmit={submit} noValidate autoComplete="off">
                        <Search
                            label="Fødselsnummer"
                            hideLabel={false}
                            description="11 sifre"
                            variant="primary"
                            clearButton={false}
                            value={fnr}
                            inputMode="numeric"
                            autoComplete="off"
                            spellCheck={false}
                            disabled={loading}
                            error={inputError}
                            onChange={(value) => {
                                setFnr(value);
                                setResult(undefined);
                                setError(undefined);
                                setInputError(undefined);
                            }}
                        >
                            <Search.Button loading={loading}/>
                        </Search>
                    </form>
                </aside>
                <main className="content" aria-live="polite" aria-busy={loading}>
                    {loading && <BodyShort>Henter eksponerte forespørsler …</BodyShort>}
                    {error && (
                        <LocalAlert status="error">
                            <LocalAlert.Header>
                                <LocalAlert.Title as="h2">Noe gikk galt</LocalAlert.Title>
                            </LocalAlert.Header>
                            <LocalAlert.Content>{error}</LocalAlert.Content>
                        </LocalAlert>
                    )}
                    {!loading && !error && !result && (
                        <BodyShort>Søk med fødselsnummer for å se eksponerte forespørsler for en person.</BodyShort>
                    )}
                    {sortert && (
                        <section aria-labelledby="result-heading" className="result">
                            {sortert.length === 0 ? (
                                <Heading level="2" size="xsmall" id="result-heading">Ingen treff.</Heading>
                            ) : (
                                <>
                                    <div className="result__toolbar">
                                        <Heading level="2" size="xsmall" id="result-heading">
                                            {sortert.length} {sortert.length === 1 ? "forespørsel" : "forespørsler"} sortert
                                            på
                                        </Heading>
                                        <Select
                                            label="Sortering"
                                            hideLabel
                                            size="small"
                                            value={sortering}
                                            onChange={(event) => setSortering(event.target.value as "nyeste" | "eldste")}
                                        >
                                            <option value="nyeste">Nyeste først</option>
                                            <option value="eldste">Eldste først</option>
                                        </Select>
                                    </div>
                                    {sortert.map((forespoersel) => (
                                        <ForespoerselKort key={forespoersel.forespoerselId}
                                                          forespoersel={forespoersel}/>
                                    ))}
                                </>
                            )}
                        </section>
                    )}
                </main>
            </div>
        </div>
    );
}
