
import { NextRequest, NextResponse } from "next/server";
import { getToken } from "next-auth/jwt";

type RouteContext = {
  params: Promise<{
    id: string;
    projectId: string;
    taskId: string;
  }>;
};

// Retrieve the Cognito access token from the NextAuth session.
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

// Forward the Spring Boot response to the browser.
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

// List all members assigned to a task.
export async function GET(request: NextRequest, context: RouteContext) {
  const accessToken = await getAccessToken(request);

  if (!accessToken) {
    return NextResponse.json(
      { message: "Unauthorized or expired session" },
      { status: 401 }
    );
  }

  const { id, projectId, taskId } = await context.params;

  if (![id, projectId, taskId].every(isValidId)) {
    return NextResponse.json(
      { message: "Invalid Space, Project or Task ID" },
      { status: 400 }
    );
  }

  try {
    const response = await fetch(
      `${process.env.SPRING_BOOT_API_URL}/api/spaces/${id}/projects/${projectId}/tasks/${taskId}/assignees`,
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
    console.error("Failed to fetch task assignees:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}

// Assign an existing Project member to a task.
export async function POST(request: NextRequest, context: RouteContext) {
  const accessToken = await getAccessToken(request);

  if (!accessToken) {
    return NextResponse.json(
      { message: "Unauthorized or expired session" },
      { status: 401 }
    );
  }

  const { id, projectId, taskId } = await context.params;

  if (![id, projectId, taskId].every(isValidId)) {
    return NextResponse.json(
      { message: "Invalid Space, Project or Task ID" },
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
      { message: "Invalid assignee data" },
      { status: 400 }
    );
  }

  const input = body as Record<string, unknown>;

  if (
    typeof input.memberId !== "number" ||
    !Number.isSafeInteger(input.memberId) ||
    input.memberId <= 0
  ) {
    return NextResponse.json(
      { message: "Invalid Member ID" },
      { status: 400 }
    );
  }

  try {
    const response = await fetch(
      `${process.env.SPRING_BOOT_API_URL}/api/spaces/${id}/projects/${projectId}/tasks/${taskId}/assignees`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          memberId: input.memberId,
        }),
        cache: "no-store",
      }
    );

    return forwardResponse(response);
  } catch (error) {
    console.error("Failed to assign task member:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}
