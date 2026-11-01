import { NextRequest, NextResponse } from "next/server";
import { getCognitoAccessToken } from "@/lib/server/getCognitoAccessToken";

type RouteContext = {
  params: Promise<{ id: string }>;
};

// Validate that the Space ID is a positive integer.
function parseSpaceId(value: string): number | null {
  const id = Number(value);

  return Number.isSafeInteger(id) && id > 0 ? id : null;
}

// Retrieve all members belonging to a Space.
export async function GET(
  request: NextRequest,
  context: RouteContext
) {
  const { id } = await context.params;
  const spaceId = parseSpaceId(id);

  if (!spaceId) {
    return NextResponse.json(
      { message: "Invalid Space ID" },
      { status: 400 }
    );
  }

  const accessToken = await getCognitoAccessToken(request);

  if (!accessToken) {
    return NextResponse.json(
      { message: "Unauthorized or expired session" },
      { status: 401 }
    );
  }

  try {
    const response = await fetch(
      `${process.env.SPRING_BOOT_API_URL}/api/spaces/${spaceId}/members`,
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        cache: "no-store",
      }
    );

    const responseText = await response.text();

    return new NextResponse(responseText || null, {
      status: response.status,
      headers: responseText
        ? {
            "Content-Type":
              response.headers.get("content-type") ??
              "application/json",
          }
        : undefined,
    });
  } catch (error) {
    console.error("Failed to fetch Space members:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}

// Add an existing registered member to a Space.
export async function POST(
  request: NextRequest,
  context: RouteContext
) {
  const { id } = await context.params;
  const spaceId = parseSpaceId(id);

  if (!spaceId) {
    return NextResponse.json(
      { message: "Invalid Space ID" },
      { status: 400 }
    );
  }

  const accessToken = await getCognitoAccessToken(request);

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
      { message: "Invalid JSON body" },
      { status: 400 }
    );
  }

  if (
    typeof body !== "object" ||
    body === null ||
    Array.isArray(body) ||
    !("email" in body) ||
    typeof body.email !== "string" ||
    !body.email.trim()
  ) {
    return NextResponse.json(
      { message: "A valid email is required" },
      { status: 400 }
    );
  }

  try {
    const response = await fetch(
      `${process.env.SPRING_BOOT_API_URL}/api/spaces/${spaceId}/members`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email: body.email.trim(),
        }),
        cache: "no-store",
      }
    );

    const responseText = await response.text();

    return new NextResponse(responseText || null, {
      status: response.status,
      headers: responseText
        ? {
            "Content-Type":
              response.headers.get("content-type") ??
              "application/json",
          }
        : undefined,
    });
  } catch (error) {
    console.error("Failed to add Space member:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}