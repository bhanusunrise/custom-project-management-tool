"use client";

import { signOut } from "next-auth/react";

export function SignOutButton() {
  return (
    <button
      type="button"
      onClick={() => signOut({ callbackUrl: "/login" })}
      className="mt-6 rounded bg-red-600 px-4 py-2 text-white"
    >
      Sign out
    </button>
  );
}