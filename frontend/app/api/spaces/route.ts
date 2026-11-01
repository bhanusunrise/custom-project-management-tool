
import { NextRequest, NextResponse } from "next/server";
import { getToken } from "next-auth/jwt";

// Retrieve the valid Cognito access token from the server-side session.
async function getAccessToken(request: NextRequest): Promise<string | null> {
  const token = await getToken({
    req: request,
    secret: process.env.NEXTAUTH_SECRET,
  });

  if (
    typeof token?.accessToken !== "string" ||
    typeof token.accessTokenExpires !== "number" ||
    Date.now() >= token.accessTokenExpires
  ) {
    return null;
  }

  return token.accessToken;
}

// Fetch only spaces belonging to the authenticated member.
export async function GET(request: NextRequest) {
  const accessToken = await getAccessToken(request);

  if (!accessToken) {
    return NextResponse.json(
      { message: "Unauthorized or expired session" },
      { status: 401 }
    );
  }

  try {
    const response = await fetch(
      `${process.env.SPRING_BOOT_API_URL}/api/spaces`,
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        cache: "no-store",
      }
    );

    const data = await response.json();

    return NextResponse.json(data, {
      status: response.status,
    });
  } catch (error) {
    console.error("Failed to fetch spaces:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}

// Create a space for the authenticated member.
export async function POST(request: NextRequest) {
  const accessToken = await getAccessToken(request);

  if (!accessToken) {
    return NextResponse.json(
      { message: "Unauthorized or expired session" },
      { status: 401 }
    );
  }

  let body: unknown;

  try {
    body = await request.json();
  } catch {
    return NextResponse.json(
      { message: "Invalid JSON request body" },
      { status: 400 }
    );
  }

  // Accept only fields supported by CreateSpaceRequest.
  if (
    typeof body !== "object" ||
    body === null ||
    Array.isArray(body)
  ) {
    return NextResponse.json(
      { message: "Invalid space data" },
      { status: 400 }
    );
  }

  const input = body as Record<string, unknown>;

  if (
    typeof input.name !== "string" ||
    !input.name.trim() ||
    input.name.length > 150 ||
    (input.description !== undefined &&
      input.description !== null &&
      typeof input.description !== "string")
  ) {
    return NextResponse.json(
      { message: "Invalid space name or description" },
      { status: 400 }
    );
  }

  const payload = {
    name: input.name.trim(),
    description: input.description ?? null,
  };

  try {
    const response = await fetch(
      `${process.env.SPRING_BOOT_API_URL}/api/spaces`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
        cache: "no-store",
      }
    );

    const data = await response.json();

    return NextResponse.json(data, {
      status: response.status,
    });
  } catch (error) {
    console.error("Failed to create space:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}
