
import { NextRequest, NextResponse } from "next/server";
import { getToken } from "next-auth/jwt";

type RouteContext = {
  params: Promise<{
    id: string;
    projectId: string;
    taskId: string;
    memberId: string;
  }>;
};

// Retrieve the Cognito access token from the server-side session.
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

// Remove an assignee without deleting the task.
export async function DELETE(request: NextRequest, context: RouteContext) {
  const accessToken = await getAccessToken(request);

  if (!accessToken) {
    return NextResponse.json(
      { message: "Unauthorized or expired session" },
      { status: 401 }
    );
  }

  const { id, projectId, taskId, memberId } = await context.params;

  if (![id, projectId, taskId, memberId].every(isValidId)) {
    return NextResponse.json(
      { message: "Invalid Space, Project, Task or Member ID" },
      { status: 400 }
    );
  }

  try {
    const response = await fetch(
      `${process.env.SPRING_BOOT_API_URL}/api/spaces/${id}/projects/${projectId}/tasks/${taskId}/assignees/${memberId}`,
      {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        cache: "no-store",
      }
    );

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
  } catch (error) {
    console.error("Failed to remove task assignee:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}
