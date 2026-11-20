
import { getToken } from "next-auth/jwt";
import { NextRequest, NextResponse } from "next/server";

export async function proxyStatusRequest(
  request: NextRequest,
  backendPath: string,
): Promise<NextResponse> {
  const token = await getToken({
    req: request,
    secret: process.env.NEXTAUTH_SECRET,
  });

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

    const response = await fetch(
      `${backendUrl.replace(/\/$/, "")}${backendPath}`,
      {
        method: request.method,
        headers,
        body: hasBody ? await request.text() : undefined,
        cache: "no-store",
      },
    );

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
    console.error("Kanban status proxy error:", error);

    return NextResponse.json(
      { error: "Unable to communicate with backend" },
      { status: 502 },
    );
  }
}
