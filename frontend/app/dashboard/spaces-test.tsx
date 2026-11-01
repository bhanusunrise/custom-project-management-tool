"use client";

import { useState } from "react";

export function SpacesTest() {
  const [result, setResult] = useState<string>("");
  const [loading, setLoading] = useState(false);

  async function fetchSpaces() {
    setLoading(true);

    try {
      const response = await fetch("/api/spaces");

      const data = await response.json();

      setResult(
        JSON.stringify(
          {
            status: response.status,
            data,
          },
          null,
          2
        )
      );
    } catch {
      setResult("Failed to connect to the API.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="mt-8">
      <h2 className="text-xl font-semibold">
        Spring Boot API Test
      </h2>

      <button
        onClick={fetchSpaces}
        disabled={loading}
        className="mt-4 rounded bg-blue-600 px-4 py-2 text-white"
      >
        {loading ? "Loading..." : "Fetch Spaces"}
      </button>

      <pre className="mt-4 overflow-auto rounded bg-gray-100 p-4 text-sm text-black">
        {result}
      </pre>
    </section>
  );
}