import { Suspense } from "react";
import { getServerSession } from "next-auth";
import { redirect } from "next/navigation";

import { authOptions } from "@/lib/auth";
import { SignOutButton } from "./sign-out-button";
import { SpacesTest } from "./spaces-test";

// Handles authentication and renders the dashboard.
async function DashboardContent() {
  const session = await getServerSession(authOptions);

  // Redirect unauthenticated users to the login page.
  if (!session?.user) {
    redirect("/login");
  }

  return (
    <main className="p-8">
      <SpacesTest />
      <SignOutButton />
    </main>
  );
}

// Provides a fallback while session information is being resolved.
export default function DashboardPage() {
  return (
    <Suspense
      fallback={
        <main className="p-8">
          <p>Loading dashboard...</p>
        </main>
      }
    >
      <DashboardContent />
    </Suspense>
  );
}