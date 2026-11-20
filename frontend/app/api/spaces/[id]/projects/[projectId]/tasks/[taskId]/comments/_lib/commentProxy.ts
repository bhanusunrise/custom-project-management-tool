
import { getToken } from "next-auth/jwt";
import { NextRequest, NextResponse } from "next/server";

/**
 * Forward authenticated comment requests to Spring Boot.
 */
export async function proxyCommentRequest(
  request: NextRequest,
  backendPath: string,
): Promise<NextResponse> {
  const token = await getToken({
    req: request,
    secret: process.env.NEXTAUTH_SECRET,
  });

  // Reject requests without a valid Cognito access token.
  if (
    typeof token?.accessToken !== "string" ||
    typeof token.accessTokenExpires !== "number" ||
    Date.now() >= token.accessTokenExpires
  ) {
    return NextResponse.json(
      { error: "Unauthorized" },
      { status: 401 },
    );
  }

  const backendUrl = process.env.SPRING_BOOT_API_URL;

  if (!backendUrl) {
    return NextResponse.json(
      { error: "Backend URL is not configured" },
      { status: 500 },
    );
  }

  try {
    const headers = new Headers({
      Authorization: `Bearer ${token.accessToken}`,
    });

    const hasBody = ["POST", "PUT", "PATCH"].includes(request.method);

    if (hasBody) {
      headers.set("Content-Type", "application/json");
    }

    // Send the request to Spring Boot through Docker networking.
    const response = await fetch(
      `${backendUrl.replace(/\/$/, "")}${backendPath}`,
      {
        method: request.method,
        headers,
        body: hasBody ? await request.text() : undefined,
        cache: "no-store",
      },
    );

    // HTTP 204 responses must not contain a body.
    if (response.status === 204) {
      return new NextResponse(null, { status: 204 });
    }

    const responseBody = await response.text();

    return new NextResponse(responseBody, {
      status: response.status,
      headers: {
        "Content-Type":
          response.headers.get("content-type") ??
          "application/json",
      },
    });
  } catch (error) {
    console.error("Comment API proxy error:", error);

    return NextResponse.json(
      { error: "Unable to communicate with backend" },
      { status: 502 },
    );
  }
}
