
import { NextRequest, NextResponse } from "next/server";
import { getToken } from "next-auth/jwt";

type RouteContext = {
  params: Promise<{ id: string }>;
};

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

// Validate the Space ID before contacting Spring Boot.
function isValidId(id: string): boolean {
  return /^[1-9]\d*$/.test(id);
}

// Forward Spring Boot responses without assuming every response is JSON.
async function forwardResponse(response: Response) {
  const text = await response.text();

  if (!text) {
    return new NextResponse(null, { status: response.status });
  }

  try {
    return NextResponse.json(JSON.parse(text), {
      status: response.status,
    });
  } catch {
    return NextResponse.json(
      { message: "Unexpected backend response" },
      { status: 502 }
    );
  }
}

// Retrieve all projects belonging to a Space.
export async function GET(request: NextRequest, context: RouteContext) {
  const accessToken = await getAccessToken(request);

  if (!accessToken) {
    return NextResponse.json(
      { message: "Unauthorized or expired session" },
      { status: 401 }
    );
  }

  const { id } = await context.params;

  if (!isValidId(id)) {
    return NextResponse.json(
      { message: "Invalid Space ID" },
      { status: 400 }
    );
  }

  try {
    const response = await fetch(
      `${process.env.SPRING_BOOT_API_URL}/api/spaces/${id}/projects`,
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
    console.error("Failed to fetch projects:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}

// Create a new project inside the selected Space.
export async function POST(request: NextRequest, context: RouteContext) {
  const accessToken = await getAccessToken(request);

  if (!accessToken) {
    return NextResponse.json(
      { message: "Unauthorized or expired session" },
      { status: 401 }
    );
  }

  const { id } = await context.params;

  if (!isValidId(id)) {
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

  if (
    typeof body !== "object" ||
    body === null ||
    Array.isArray(body)
  ) {
    return NextResponse.json(
      { message: "Invalid project data" },
      { status: 400 }
    );
  }

  const input = body as Record<string, unknown>;

  if (
    typeof input.name !== "string" ||
    !input.name.trim() ||
    input.name.trim().length > 150 ||
    (input.description != null &&
      typeof input.description !== "string")
  ) {
    return NextResponse.json(
      { message: "Invalid project name or description" },
      { status: 400 }
    );
  }

  // Dates must use the format YYYY-MM-DD or be null.
  const validDate = (value: unknown) =>
    value == null ||
    (typeof value === "string" &&
      /^\d{4}-\d{2}-\d{2}$/.test(value));

  if (!validDate(input.startDate) || !validDate(input.endDate)) {
    return NextResponse.json(
      { message: "Invalid project date format" },
      { status: 400 }
    );
  }

  const payload = {
    name: input.name.trim(),
    description: input.description ?? null,
    startDate: input.startDate ?? null,
    endDate: input.endDate ?? null,
  };

  try {
    const response = await fetch(
      `${process.env.SPRING_BOOT_API_URL}/api/spaces/${id}/projects`,
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

    return forwardResponse(response);
  } catch (error) {
    console.error("Failed to create project:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}
