
import { NextRequest } from "next/server";
import {
  isValidId,
  errorResponse,
  proxyProjectMemberRequest,
} from "../../../../_lib/projectMemberProxy";

type RouteContext = {
  params: Promise<{
    id: string;
    projectId: string;
    memberId: string;
  }>;
};

// Change a member's Project role.
export async function PUT(request: NextRequest, context: RouteContext) {
  const { id, projectId, memberId } = await context.params;

  if (
    !isValidId(id) ||
    !isValidId(projectId) ||
    !isValidId(memberId)
  ) {
    return errorResponse("Invalid Space, Project or Member ID", 400);
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
    return errorResponse("Invalid role data", 400);
  }

  const input = body as Record<string, unknown>;

  if (
    input.role !== "PROJECT_MANAGER" &&
    input.role !== "PROJECT_MEMBER"
  ) {
    return errorResponse("Invalid Project role", 400);
  }

  return proxyProjectMemberRequest(
    request,
    `/api/spaces/${id}/projects/${projectId}/members/${memberId}/role`,
    "PUT",
    { role: input.role }
  );
}
