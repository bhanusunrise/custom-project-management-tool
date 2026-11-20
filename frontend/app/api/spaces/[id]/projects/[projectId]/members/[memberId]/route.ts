
import { NextRequest } from "next/server";
import {
  isValidId,
  errorResponse,
  proxyProjectMemberRequest,
} from "../../../_lib/projectMemberProxy";

type RouteContext = {
  params: Promise<{
    id: string;
    projectId: string;
    memberId: string;
  }>;
};

// Remove an existing Project member.
export async function DELETE(request: NextRequest, context: RouteContext) {
  const { id, projectId, memberId } = await context.params;

  if (
    !isValidId(id) ||
    !isValidId(projectId) ||
    !isValidId(memberId)
  ) {
    return errorResponse("Invalid Space, Project or Member ID", 400);
  }

  return proxyProjectMemberRequest(
    request,
    `/api/spaces/${id}/projects/${projectId}/members/${memberId}`,
    "DELETE"
  );
}
