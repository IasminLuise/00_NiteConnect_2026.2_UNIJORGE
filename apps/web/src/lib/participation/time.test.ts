import { describe, expect, it } from "vitest";
import { addDays, localDateTimeToInstant, startOfWeek } from "./time";

describe("participation date and time helpers", () => {
  it("moves between Monday-based weeks using calendar dates", () => {
    expect(startOfWeek("2027-03-03")).toBe("2027-03-01");
    expect(addDays("2027-03-01", 6)).toBe("2027-03-07");
  });

  it("converts local time in the API zone to an instant", () => {
    expect(localDateTimeToInstant("2027-03-01T09:30", "America/Bahia")).toBe("2027-03-01T12:30:00.000Z");
  });

  it("rejects local times that do not exist in the requested timezone", () => {
    expect(localDateTimeToInstant("2027-03-01T09:30", "Unknown/Zone")).toBeNull();
  });
});
