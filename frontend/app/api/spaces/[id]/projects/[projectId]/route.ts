
import { NextRequest, NextResponse } from "next/server";
import { getToken } from "next-auth/jwt";

type RouteContext = {
  params: Promise<{
    id: string;
    projectId: string;
  }>;
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

function isValidId(id: string): boolean {
  return /^[1-9]\d*$/.test(id);
}

async function forwardResponse(response: Response) {
  if (response.status === 204) {
    return new NextResponse(null, { status: 204 });
  }

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

// Retrieve a specific project.
export async function GET(request: NextRequest, context: RouteContext) {
  const accessToken = await getAccessToken(request);

  if (!accessToken) {
    return NextResponse.json(
      { message: "Unauthorized or expired session" },
      { status: 401 }
    );
  }

  const { id, projectId } = await context.params;

  if (!isValidId(id) || !isValidId(projectId)) {
    return NextResponse.json(
      { message: "Invalid Space or Project ID" },
      { status: 400 }
    );
  }

  try {
    const response = await fetch(
      `${process.env.SPRING_BOOT_API_URL}/api/spaces/${id}/projects/${projectId}`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        cache: "no-store",
      }
    );

    return forwardResponse(response);
  } catch (error) {
    console.error("Failed to retrieve project:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}

// Update project information.
export async function PUT(request: NextRequest, context: RouteContext) {
  const accessToken = await getAccessToken(request);

  if (!accessToken) {
    return NextResponse.json(
      { message: "Unauthorized or expired session" },
      { status: 401 }
    );
  }

  const { id, projectId } = await context.params;

  if (!isValidId(id) || !isValidId(projectId)) {
    return NextResponse.json(
      { message: "Invalid Space or Project ID" },
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

  const allowedStatuses = [
    "PLANNED",
    "ACTIVE",
    "ON_HOLD",
    "COMPLETED",
    "CANCELLED",
  ];

  const validDate = (value: unknown) =>
    value == null ||
    (typeof value === "string" &&
      /^\d{4}-\d{2}-\d{2}$/.test(value));

  if (
    typeof input.name !== "string" ||
    !input.name.trim() ||
    input.name.trim().length > 150 ||
    (input.description != null &&
      typeof input.description !== "string") ||
    (input.status != null &&
      (typeof input.status !== "string" ||
        !allowedStatuses.includes(input.status))) ||
    !validDate(input.startDate) ||
    !validDate(input.endDate)
  ) {
    return NextResponse.json(
      { message: "Invalid project update data" },
      { status: 400 }
    );
  }

  const payload = {
    name: input.name.trim(),
    description: input.description ?? null,
    status: input.status ?? null,
    startDate: input.startDate ?? null,
    endDate: input.endDate ?? null,
  };

  try {
    const response = await fetch(
      `${process.env.SPRING_BOOT_API_URL}/api/spaces/${id}/projects/${projectId}`,
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
    console.error("Failed to update project:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}

// Delete a project.
export async function DELETE(request: NextRequest, context: RouteContext) {
  const accessToken = await getAccessToken(request);

  if (!accessToken) {
    return NextResponse.json(
      { message: "Unauthorized or expired session" },
      { status: 401 }
    );
  }

  const { id, projectId } = await context.params;

  if (!isValidId(id) || !isValidId(projectId)) {
    return NextResponse.json(
      { message: "Invalid Space or Project ID" },
      { status: 400 }
    );
  }

  try {
    const response = await fetch(
      `${process.env.SPRING_BOOT_API_URL}/api/spaces/${id}/projects/${projectId}`,
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
    console.error("Failed to delete project:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}
