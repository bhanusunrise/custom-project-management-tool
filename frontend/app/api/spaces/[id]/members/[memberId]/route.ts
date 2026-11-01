import { NextRequest, NextResponse } from "next/server";
import { getCognitoAccessToken } from "@/lib/server/getCognitoAccessToken";

type RouteContext = {
  params: Promise<{
    id: string;
    memberId: string;
  }>;
};

// Validate numeric route parameters.
function parseId(value: string): number | null {
  const id = Number(value);

  return Number.isSafeInteger(id) && id > 0 ? id : null;
}

// Remove an existing member from a Space.
export async function DELETE(
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

  try {
    const response = await fetch(
      `${process.env.SPRING_BOOT_API_URL}/api/spaces/${spaceId}/members/${targetMemberId}`,
      {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        cache: "no-store",
      }
    );

    // Successful DELETE requests return HTTP 204 without a body.
    if (response.status === 204) {
      return new NextResponse(null, { status: 204 });
    }

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
    console.error("Failed to remove Space member:", error);

    return NextResponse.json(
      { message: "Backend service unavailable" },
      { status: 502 }
    );
  }
}