import { NextRequest, NextResponse } from "next/server";
import { getCognitoAccessToken } from "@/lib/server/getCognitoAccessToken";

type RouteContext = {
  params: Promise<{
    id: string;
    memberId: string;
  }>;
};

function parseId(value: string): number | null {
  const id = Number(value);

  return Number.isSafeInteger(id) && id > 0 ? id : null;
}

// Update the role assigned to a Space member.
export async function PUT(
  request: NextRequest,
  context: RouteContext
) {
  const { id, memberId } = await context.params;

  const spaceId = parseId(id);
  const targetMemberId = parseId(memberId);

  if (!spaceId || !targetMemberId) {
    return NextResponse.json(
      { message: "Invalid Space ID or Member ID" },
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
    !("role" in body) ||
    (body.role !== "SPACE_OWNER" &&
      body.role !== "SPACE_MEMBER")
  ) {
    return NextResponse.json(
      { message: "Role must be SPACE_OWNER or SPACE_MEMBER" },
      { status: 400 }
    );
  }

  try {
    const response = await fetch(
      `${process.env.SPRING_BOOT_API_URL}/api/spaces/${spaceId}/members/${targetMemberId}/role`,
      {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          role: body.role,
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
    console.error("Failed to update Space member role:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}