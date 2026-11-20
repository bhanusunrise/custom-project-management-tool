
import { NextRequest, NextResponse } from "next/server";
import {
  getAccessToken,
  isValidId,
  errorResponse,
  proxyProjectMemberRequest,
} from "../../_lib/projectMemberProxy";

type RouteContext = {
  params: Promise<{ id: string; projectId: string }>;
};

// Retrieve all members assigned to the Project.
export async function GET(request: NextRequest, context: RouteContext) {
  const { id, projectId } = await context.params;

  if (!isValidId(id) || !isValidId(projectId)) {
    return errorResponse("Invalid Space or Project ID", 400);
  }

  return proxyProjectMemberRequest(
    request,
    `/api/spaces/${id}/projects/${projectId}/members`,
    "GET"
  );
}

// Add an existing Space member to the Project.
export async function POST(request: NextRequest, context: RouteContext) {
  const accessToken = await getAccessToken(request);

  if (!accessToken) {
    return errorResponse("Unauthorized or expired session", 401);
  }

  const { id, projectId } = await context.params;

  if (!isValidId(id) || !isValidId(projectId)) {
    return errorResponse("Invalid Space or Project ID", 400);
  }

  let body: unknown;

  try {
    body = await request.json();
  } catch {
    return errorResponse("Invalid JSON request body", 400);
  }

  if (
    typeof body !== "object" ||
    body === null ||
    Array.isArray(body)
  ) {
    return errorResponse("Invalid member data", 400);
  }

  const input = body as Record<string, unknown>;

  if (
    typeof input.email !== "string" ||
    !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(input.email.trim())
  ) {
    return errorResponse("Invalid member email", 400);
  }

  return proxyProjectMemberRequest(
    request,
    `/api/spaces/${id}/projects/${projectId}/members`,
    "POST",
    { email: input.email.trim() }
  );
}
