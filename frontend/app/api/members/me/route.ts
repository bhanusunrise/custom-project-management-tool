import { NextRequest, NextResponse } from "next/server";
import { getToken } from "next-auth/jwt";

export async function GET(request: NextRequest) {
  // Retrieve the NextAuth JWT from the HTTP-only session cookie.
  const token = await getToken({
    req: request,
    secret: process.env.NEXTAUTH_SECRET,
  });

  if (
    typeof token?.accessToken !== "string" ||
    typeof token.accessTokenExpires !== "number" ||
    Date.now() >= token.accessTokenExpires
  ) {
    return NextResponse.json(
      { message: "Unauthorized or expired session" },
      { status: 401 }
    );
  }

  try {
    // Send the Cognito access token to the Spring Boot backend.
    const response = await fetch(
      `${process.env.SPRING_BOOT_API_URL}/api/members/me`,
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token.accessToken}`,
        },
        cache: "no-store",
      }
    );

    const data = await response.json();

    return NextResponse.json(data, {
      status: response.status,
    });

  } catch (error) {
    console.error("Failed to fetch member:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}