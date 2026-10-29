import { describe, expect, it, vi } from "vitest";
import { redirect } from "next/navigation";
import Home from "./page";

vi.mock("next/navigation", () => ({
  redirect: vi.fn((destination: string) => {
    throw new Error(`redirect:${destination}`);
  }),
}));

describe("rota raiz", () => {
  it("redireciona para o login institucional", () => {
    expect(() => Home()).toThrow("redirect:/login");
    expect(redirect).toHaveBeenCalledWith("/login");
  });
});
