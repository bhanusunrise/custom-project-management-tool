
import { NextRequest, NextResponse } from "next/server";
import { getToken } from "next-auth/jwt";

type RouteContext = {
  params: Promise<{ id: string; projectId: string }>;
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

// List tasks belonging to the selected Project.
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
      `${process.env.SPRING_BOOT_API_URL}/api/spaces/${id}/projects/${projectId}/tasks`,
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
    console.error("Failed to fetch tasks:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}

// Create a new task.
export async function POST(request: NextRequest, context: RouteContext) {
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
      { message: "Invalid task data" },
      { status: 400 }
    );
  }

  const input = body as Record<string, unknown>;

  if (
    typeof input.name !== "string" ||
    !input.name.trim() ||
    input.name.trim().length > 200 ||
    !Number.isSafeInteger(input.statusId) ||
    (input.statusId as number) <= 0 ||
    (input.description != null &&
      typeof input.description !== "string") ||
    (input.priority != null &&
      !["LOW", "MEDIUM", "HIGH", "URGENT"].includes(
        input.priority as string
      ))
  ) {
    return NextResponse.json(
      { message: "Invalid task information" },
      { status: 400 }
    );
  }

  const validDate = (value: unknown) =>
    value == null ||
    (typeof value === "string" &&
      /^\d{4}-\d{2}-\d{2}$/.test(value));

  if (!validDate(input.startDate) || !validDate(input.dueDate)) {
    return NextResponse.json(
      { message: "Invalid task date format" },
      { status: 400 }
    );
  }

  const payload = {
    name: input.name.trim(),
    description: input.description ?? null,
    statusId: input.statusId,
    startDate: input.startDate ?? null,
    dueDate: input.dueDate ?? null,
    priority: input.priority ?? null,
  };

  try {
    const response = await fetch(
      `${process.env.SPRING_BOOT_API_URL}/api/spaces/${id}/projects/${projectId}/tasks`,
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
    console.error("Failed to create task:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}
