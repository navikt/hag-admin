import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";

const id = "12345678-1234-1234-1234-123456789abc";
const fnr = "01234567890";

afterEach(() => vi.unstubAllGlobals());

async function submitLookup() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("Fødselsnummer"), fnr);
  await user.click(screen.getByRole("button", { name: "Hent forespørsler" }));
  return user;
}

describe("Forespørselsoppslag", () => {
  it("sends JSON to the existing endpoint and displays UUIDs", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify([id])));
    vi.stubGlobal("fetch", fetchMock);
    render(<App />);

    await submitLookup();

    expect(await screen.findByText(id)).toBeVisible();
    expect(fetchMock).toHaveBeenCalledExactlyOnceWith("/hentEksponerteForespoersler", {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify({ fnr }),
      cache: "no-store",
    });
  });

  it("rejects invalid input without sending a request", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    render(<App />);
    const user = userEvent.setup();
    await user.type(screen.getByLabelText("Fødselsnummer"), "123");
    await user.click(screen.getByRole("button", { name: "Hent forespørsler" }));

    expect(screen.getByText("Skriv inn et fødselsnummer med 11 sifre.")).toBeVisible();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("shows empty results", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("[]")));
    render(<App />);
    await submitLookup();
    expect(await screen.findByText("Ingen eksponerte forespørsler funnet.")).toBeVisible();
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
    expect(await screen.findByText("Ingen eksponerte forespørsler funnet.")).toBeVisible();
  });

  it("clears previous results when the person changes", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify([id]))));
    render(<App />);
    const user = await submitLookup();
    await screen.findByText(id);
    await user.clear(screen.getByLabelText("Fødselsnummer"));
    expect(screen.queryByText(id)).not.toBeInTheDocument();
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
    await waitFor(() => expect(screen.getByRole("button", { name: "Hent forespørsler" })).toBeEnabled());
  });
});
