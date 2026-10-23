import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";

const id = "12345678-1234-1234-1234-123456789abc";
const fnr = "01234567890";
const forespoersel = {
  forespoerselId: id,
  orgnr: "810007842",
  vedtaksperiodeId: "87654321-4321-4321-4321-cba987654321",
  opprettet: "2026-10-01T12:00:00",
  sykmeldingsperioder: [{ fom: "2026-09-01", tom: "2026-09-10" }, { fom: "2026-09-15", tom: "2026-09-30" }],
  erBesvart: true,
  erBegrenset: false,
  forespurtData: { inntekt: { paakrevd: true } },
};

afterEach(() => vi.unstubAllGlobals());

async function submitLookup() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("Fødselsnummer"), fnr);
  await user.click(screen.getByRole("button", { name: "Søk" }));
  return user;
}

function stubFetch(requestMock: (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>) {
  vi.stubGlobal(
    "fetch",
    vi.fn((input: RequestInfo | URL, init?: RequestInit) =>
      input === "/hentBrukernavn"
        ? Promise.resolve(new Response("testbruker"))
        : requestMock(input, init),
    ),
  );
}

describe("Forespørselsoppslag", () => {
  it("sender JSON til det eksisterende endepunktet og viser forespørsler", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify([forespoersel])));
    stubFetch(fetchMock);
    render(<App />);

    await submitLookup();

    expect(await screen.findByRole("heading", { name: `Forespørsel ${id}` })).toBeVisible();
    expect(screen.getByText("testbruker")).toBeVisible();
    expect(screen.getByRole("heading", { name: "Eksponerte forespørsler" })).toBeVisible();
    expect(screen.getByText("Org.nr. 810007842")).toBeVisible();
    expect(screen.getByText("Gjelder sykmeldingsperiode 01.09.2026 – 30.09.2026")).toBeVisible();
    expect(screen.getByText("Opprettet 01.10.2026")).toBeVisible();
    expect(screen.getByText("Besvart")).toBeVisible();
    expect(fetchMock).toHaveBeenCalledExactlyOnceWith("/hentEksponerteForespoersler", {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify({ fnr }),
      cache: "no-store",
    });
  });

  it("viser Nav-logoen med lenke til HAG Admin", () => {
    render(<App />);
    expect(screen.getByRole("img", { name: "Nav" })).toBeVisible();
    expect(screen.getByRole("link", { name: /^Nav\s*HAG Admin$/ })).toHaveAttribute("href", "/");
  });

  it("avviser ugyldig input uten å sende en forespørsel", async () => {
    const fetchMock = vi.fn();
    stubFetch(fetchMock);
    render(<App />);
    const user = userEvent.setup();
    await user.type(screen.getByLabelText("Fødselsnummer"), "123");
    await user.click(screen.getByRole("button", { name: "Søk" }));

    expect(screen.getByText("Skriv inn et fødselsnummer med 11 sifre.")).toBeVisible();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("viser en melding når søket ikke gir treff", async () => {
    stubFetch(vi.fn().mockResolvedValue(new Response("[]")));
    render(<App />);
    await submitLookup();
    expect(await screen.findByText("Ingen treff.")).toBeVisible();
  });

  it("hindrer gjentatte søk mens den venter på svar", async () => {
    let complete: (response: Response) => void = () => { throw new Error("Request not started"); };
    const fetchMock = vi.fn(() => new Promise<Response>((resolve) => { complete = resolve; }));
    stubFetch(fetchMock);
    render(<App />);
    const user = await submitLookup();

    const button = screen.getByRole("button");
    expect(button).toBeDisabled();
    await user.click(button);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    complete(new Response("[]"));
    expect(await screen.findByText("Ingen treff.")).toBeVisible();
  });

  it("fjerner tidligere resultater når personen endres", async () => {
    stubFetch(vi.fn().mockResolvedValue(new Response(JSON.stringify([forespoersel]))));
    render(<App />);
    const user = await submitLookup();
    await screen.findByRole("heading", { name: `Forespørsel ${id}` });
    await user.clear(screen.getByLabelText("Fødselsnummer"));
    expect(screen.queryByRole("heading", { name: `Forespørsel ${id}` })).not.toBeInTheDocument();
  });

  it.each([
    [401, "Du mangler tilgang eller innloggingen har utløpt."],
    [403, "Du mangler tilgang eller innloggingen har utløpt."],

    [500, "Kunne ikke hente forespørsler."],
  ])("håndterer HTTP %s uten å vise detaljer fra backend", async (status, message) => {
    stubFetch(vi.fn().mockResolvedValue(new Response("Sensitive backend details", { status })));
    render(<App />);
    await submitLookup();
    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.queryByText("Sensitive backend details")).not.toBeInTheDocument();
  });

  it("viser hele objektet når et kort utvides", async () => {
    stubFetch(vi.fn().mockResolvedValue(new Response(JSON.stringify([forespoersel]))));
    render(<App />);
    const user = await submitLookup();
    await screen.findByRole("heading", { name: `Forespørsel ${id}` });
    await user.click(screen.getByRole("button", { name: "Vis mer" }));
    expect(await screen.findByText(/"paakrevd": true/)).toBeVisible();
  });

  it("viser «Ikke besvart» for ubesvarte forespørsler", async () => {
    stubFetch(vi.fn().mockResolvedValue(new Response(JSON.stringify([{ ...forespoersel, erBesvart: false }]))));
    render(<App />);
    await submitLookup();
    expect(await screen.findByText("Ikke besvart")).toBeVisible();
  });

  it("viser en kompakt forkast-knapp bare for ubesvarte forespørsler og beholder kortet etter forkasting", async () => {
    const ubesvart = {
      ...forespoersel,
      forespoerselId: "aaaaaaaa-1234-1234-1234-123456789abc",
      erBesvart: false,
    };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify([forespoersel, ubesvart])))
      .mockResolvedValueOnce(new Response(null, { status: 200 }));
    stubFetch(fetchMock);
    render(<App />);

    const user = await submitLookup();
    expect(await screen.findByRole("heading", { name: `Forespørsel ${ubesvart.forespoerselId}` })).toBeVisible();
    expect(screen.getAllByRole("button", { name: `Forkast forespørsel ${ubesvart.forespoerselId}` })).toHaveLength(1);

    await user.click(screen.getByRole("button", { name: `Forkast forespørsel ${ubesvart.forespoerselId}` }));

    expect(fetchMock).toHaveBeenNthCalledWith(2, "/forkastForespoersel", {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify({ forespoerselId: ubesvart.forespoerselId }),
    });
    expect(await screen.findByRole("heading", { name: `Forespørsel ${ubesvart.forespoerselId}` })).toBeVisible();
    expect(screen.getByText("Forkastet")).toBeVisible();
    expect(screen.queryByRole("button", { name: `Forkast forespørsel ${ubesvart.forespoerselId}` })).not.toBeInTheDocument();
  });

  it("beholder en ubesvart forespørsel og viser en feilmelding hvis forkasting mislykkes", async () => {
    const ubesvart = { ...forespoersel, erBesvart: false };
    stubFetch(
      vi.fn()
        .mockResolvedValueOnce(new Response(JSON.stringify([ubesvart])))
        .mockResolvedValueOnce(new Response(null, { status: 500 })),
    );
    render(<App />);

    const user = await submitLookup();
    await user.click(await screen.findByRole("button", { name: `Forkast forespørsel ${id}` }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Kunne ikke forkaste forespørselen.");
    expect(screen.getByRole("heading", { name: `Forespørsel ${id}` })).toBeVisible();
  });

  it("sorterer forespørsler etter opprettelsestidspunkt", async () => {
    const eldre = { ...forespoersel, forespoerselId: "aaaaaaaa-1234-1234-1234-123456789abc", opprettet: "2026-09-01T08:00:00" };
    stubFetch(vi.fn().mockResolvedValue(new Response(JSON.stringify([eldre, forespoersel]))));
    render(<App />);
    const user = await submitLookup();
    const titler = async () => (await screen.findAllByRole("heading", { level: 3 })).map((heading) => heading.textContent);
    expect(await titler()).toEqual([`Forespørsel ${id}`, `Forespørsel ${eldre.forespoerselId}`]);
    await user.selectOptions(screen.getByLabelText("Sortering"), "eldste");
    expect(await titler()).toEqual([`Forespørsel ${eldre.forespoerselId}`, `Forespørsel ${id}`]);
  });

  it("avviser forespørsler uten gyldig forespørsels-ID", async () => {
    stubFetch(vi.fn().mockResolvedValue(new Response(JSON.stringify([{ orgnr: "810007842" }]))));
    render(<App />);
    await submitLookup();
    expect(await screen.findByRole("alert")).toHaveTextContent("Mottok et ugyldig svar fra tjenesten.");
  });

  it("avviser svar med uventet format", async () => {
    stubFetch(vi.fn().mockResolvedValue(new Response('{"unexpected":true}')));
    render(<App />);
    await submitLookup();
    expect(await screen.findByRole("alert")).toHaveTextContent("Mottok et ugyldig svar fra tjenesten.");
  });

  it("håndterer nettverksfeil og lar brukeren prøve igjen", async () => {
    stubFetch(vi.fn().mockRejectedValue(new TypeError("Failed to fetch")));
    render(<App />);
    await submitLookup();
    expect(await screen.findByRole("alert")).toHaveTextContent("Kontroller forbindelsen");
    await waitFor(() => expect(screen.getByRole("button", { name: "Søk" })).toBeEnabled());
  });
});
