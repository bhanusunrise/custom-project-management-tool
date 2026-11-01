
import { NextRequest, NextResponse } from "next/server";
import { getToken } from "next-auth/jwt";

type RouteContext = {
  params: Promise<{ id: string }>;
};

// Retrieve the Cognito access token from the encrypted NextAuth session.
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

// Validate the Space ID before forwarding requests to Spring Boot.
function validateSpaceId(id: string): boolean {
  return /^[1-9]\d*$/.test(id) && Number.isSafeInteger(Number(id));
}

// Forward Spring Boot responses while preserving their HTTP status.
async function forwardResponse(response: Response) {
  if (response.status === 204) {
    return new NextResponse(null, { status: 204 });
  }

  const body = await response.text();

  if (!body) {
    return new NextResponse(null, { status: response.status });
  }

  return new NextResponse(body, {
    status: response.status,
    headers: {
      "Content-Type":
        response.headers.get("content-type") ?? "application/json",
    },
  });
}

// Retrieve a Space if the authenticated user is a member.
export async function GET(request: NextRequest, context: RouteContext) {
  const accessToken = await getAccessToken(request);

  if (!accessToken) {
    return NextResponse.json(
      { message: "Unauthorized or expired session" },
      { status: 401 }
    );
  }

  const { id } = await context.params;

  if (!validateSpaceId(id)) {
    return NextResponse.json(
      { message: "Invalid Space ID" },
      { status: 400 }
    );
  }

  try {
    const response = await fetch(
      `${process.env.SPRING_BOOT_API_URL}/api/spaces/${id}`,
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        cache: "no-store",
      }
    );

    return forwardResponse(response);
  } catch (error) {
    console.error("Failed to retrieve Space:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}

// Update a Space if the authenticated user is its owner.
export async function PUT(request: NextRequest, context: RouteContext) {
  const accessToken = await getAccessToken(request);

  if (!accessToken) {
    return NextResponse.json(
      { message: "Unauthorized or expired session" },
      { status: 401 }
    );
  }

  const { id } = await context.params;

  if (!validateSpaceId(id)) {
    return NextResponse.json(
      { message: "Invalid Space ID" },
      { status: 400 }
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

  if (typeof body !== "object" || body === null || Array.isArray(body)) {
    return NextResponse.json(
      { message: "Invalid Space data" },
      { status: 400 }
    );
  }

  const input = body as Record<string, unknown>;

  if (
    typeof input.name !== "string" ||
    !input.name.trim() ||
    input.name.trim().length > 150 ||
    (input.description !== undefined &&
      input.description !== null &&
      typeof input.description !== "string")
  ) {
    return NextResponse.json(
      { message: "Invalid Space name or description" },
      { status: 400 }
    );
  }

  const payload = {
    name: input.name.trim(),
    description: input.description ?? null,
  };

  try {
    const response = await fetch(
      `${process.env.SPRING_BOOT_API_URL}/api/spaces/${id}`,
      {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
        cache: "no-store",
      }
    );

    return forwardResponse(response);
  } catch (error) {
    console.error("Failed to update Space:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}

// Delete an empty Space if the authenticated user is its owner.
export async function DELETE(request: NextRequest, context: RouteContext) {
  const accessToken = await getAccessToken(request);

  if (!accessToken) {
    return NextResponse.json(
      { message: "Unauthorized or expired session" },
      { status: 401 }
    );
  }

  const { id } = await context.params;

  if (!validateSpaceId(id)) {
    return NextResponse.json(
      { message: "Invalid Space ID" },
      { status: 400 }
    );
  }

  try {
    const response = await fetch(
      `${process.env.SPRING_BOOT_API_URL}/api/spaces/${id}`,
      {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        cache: "no-store",
      }
    );

    return forwardResponse(response);
  } catch (error) {
    console.error("Failed to delete Space:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}
