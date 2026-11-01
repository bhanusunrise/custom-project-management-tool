"use client";

import { signIn } from "next-auth/react";

export default function LoginPage() {
  return (
    <main className="flex min-h-screen items-center justify-center">
      <div className="space-y-6 text-center">
        <h1 className="text-3xl font-bold">
          Welcome to ProjectFlow
        </h1>

        <p>Sign in to manage your projects and tasks.</p>

        <button
          type="button"
          onClick={() => signIn("cognito", { callbackUrl: "/dashboard" })}
          className="rounded bg-blue-600 px-6 py-3 text-white"
        >
          Sign in with Cognito
        </button>
      </div>
    </main>
  );
}