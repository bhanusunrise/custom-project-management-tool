
import { NextRequest, NextResponse } from "next/server";
import { getToken } from "next-auth/jwt";

// Read the Cognito access token from the server-side NextAuth JWT.
export async function getAccessToken(
  request: NextRequest
): Promise<string | null> {
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

// Accept only positive numeric route IDs.
export function isValidId(id: string): boolean {
  return /^[1-9]\d*$/.test(id);
}

export function errorResponse(message: string, status: number) {
  return NextResponse.json({ message }, { status });
}

// Preserve Spring Boot's HTTP status and JSON response.
export async function forwardResponse(response: Response) {
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
    return errorResponse("Unexpected backend response", 502);
  }
}

// Forward authenticated requests from Next.js to Spring Boot.
export async function proxyProjectMemberRequest(
  request: NextRequest,
  path: string,
  method: "GET" | "POST" | "PUT" | "DELETE",
  payload?: unknown
) {
  const accessToken = await getAccessToken(request);

  if (!accessToken) {
    return errorResponse("Unauthorized or expired session", 401);
  }

  const baseUrl = process.env.SPRING_BOOT_API_URL;

  if (!baseUrl) {
    console.error("SPRING_BOOT_API_URL is not configured");
    return errorResponse("Backend service unavailable", 502);
  }

  try {
    const response = await fetch(`${baseUrl}${path}`, {
      method,
      headers: {
        Authorization: `Bearer ${accessToken}`,
        ...(payload !== undefined
          ? { "Content-Type": "application/json" }
          : {}),
      },
      ...(payload !== undefined
        ? { body: JSON.stringify(payload) }
        : {}),
      cache: "no-store",
    });

    return forwardResponse(response);
  } catch (error) {
    console.error("Project member API request failed:", error);
    return errorResponse("Backend service unavailable", 502);
  }
}
