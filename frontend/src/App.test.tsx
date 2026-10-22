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

describe("Forespørselsoppslag", () => {
  it("sends JSON to the existing endpoint and displays forespørsler", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify([forespoersel])));
    vi.stubGlobal("fetch", fetchMock);
    render(<App />);

    await submitLookup();

    expect(await screen.findByRole("heading", { name: `Forespørsel ${id}` })).toBeVisible();
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

  it("shows the Nav logo linking to HAG Admin", () => {
    render(<App />);
    expect(screen.getByRole("img", { name: "Nav" })).toBeVisible();
    expect(screen.getByRole("link", { name: /^Nav\s*HAG Admin$/ })).toHaveAttribute("href", "/");
  });

  it("rejects invalid input without sending a request", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    render(<App />);
    const user = userEvent.setup();
    await user.type(screen.getByLabelText("Fødselsnummer"), "123");
    await user.click(screen.getByRole("button", { name: "Søk" }));

    expect(screen.getByText("Skriv inn et fødselsnummer med 11 sifre.")).toBeVisible();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("shows empty results", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("[]")));
    render(<App />);
    await submitLookup();
    expect(await screen.findByText("Ingen treff.")).toBeVisible();
  });

  it("disables duplicate submissions while awaiting the response", async () => {
    let complete: (response: Response) => void = () => { throw new Error("Request not started"); };
    const fetchMock = vi.fn(() => new Promise<Response>((resolve) => { complete = resolve; }));
    vi.stubGlobal("fetch", fetchMock);
    render(<App />);
    const user = await submitLookup();

    const button = screen.getByRole("button");
    expect(button).toBeDisabled();
    await user.click(button);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    complete(new Response("[]"));
    expect(await screen.findByText("Ingen treff.")).toBeVisible();
  });

  it("clears previous results when the person changes", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify([forespoersel]))));
    render(<App />);
    const user = await submitLookup();
    await screen.findByRole("heading", { name: `Forespørsel ${id}` });
    await user.clear(screen.getByLabelText("Fødselsnummer"));
    expect(screen.queryByRole("heading", { name: `Forespørsel ${id}` })).not.toBeInTheDocument();
  });

  it.each([
    [401, "Du mangler tilgang eller innloggingen har utløpt."],
    [403, "Du mangler tilgang eller innloggingen har utløpt."],
    [504, "Tidsavbrudd ved henting av forespørsler."],
    [500, "Kunne ikke hente forespørsler."],
  ])("handles HTTP %s without displaying backend details", async (status, message) => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("Sensitive backend details", { status })));
    render(<App />);
    await submitLookup();
    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.queryByText("Sensitive backend details")).not.toBeInTheDocument();
  });

  it("shows the full object when a row is expanded", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify([forespoersel]))));
    render(<App />);
    const user = await submitLookup();
    await screen.findByRole("heading", { name: `Forespørsel ${id}` });
    await user.click(screen.getByRole("button", { name: "Vis hele objektet" }));
    expect(await screen.findByText(/"paakrevd": true/)).toBeVisible();
  });

  it("shows ikke besvart for unanswered forespørsler", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify([{ ...forespoersel, erBesvart: false }]))));
    render(<App />);
    await submitLookup();
    expect(await screen.findByText("Ikke besvart")).toBeVisible();
  });

  it("sorts forespørsler by opprettet", async () => {
    const eldre = { ...forespoersel, forespoerselId: "aaaaaaaa-1234-1234-1234-123456789abc", opprettet: "2026-09-01T08:00:00" };
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify([eldre, forespoersel]))));
    render(<App />);
    const user = await submitLookup();
    const titler = async () => (await screen.findAllByRole("heading", { level: 3 })).map((heading) => heading.textContent);
    expect(await titler()).toEqual([`Forespørsel ${id}`, `Forespørsel ${eldre.forespoerselId}`]);
    await user.selectOptions(screen.getByLabelText("Sortering"), "eldste");
    expect(await titler()).toEqual([`Forespørsel ${eldre.forespoerselId}`, `Forespørsel ${id}`]);
  });

  it("rejects forespørsler without a valid forespoerselId", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify([{ orgnr: "810007842" }]))));
    render(<App />);
    await submitLookup();
    expect(await screen.findByRole("alert")).toHaveTextContent("Mottok et ugyldig svar fra tjenesten.");
  });

  it("rejects an unexpected response shape", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response('{"unexpected":true}')));
    render(<App />);
    await submitLookup();
    expect(await screen.findByRole("alert")).toHaveTextContent("Mottok et ugyldig svar fra tjenesten.");
  });

  it("handles a network failure and allows retry", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("Failed to fetch")));
    render(<App />);
    await submitLookup();
    expect(await screen.findByRole("alert")).toHaveTextContent("Kontroller forbindelsen");
    await waitFor(() => expect(screen.getByRole("button", { name: "Søk" })).toBeEnabled());
  });
});
