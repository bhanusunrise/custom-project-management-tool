
import { NextRequest } from "next/server";
import { proxyCommentRequest } from "../_lib/commentProxy";

type RouteContext = {
  params: Promise<{
    id: string;
    projectId: string;
    taskId: string;
    commentId: string;
  }>;
};

/**
 * Delete a comment if the authenticated member has permission.
 */
export async function DELETE(
  request: NextRequest,
  context: RouteContext,
) {
  const { id, projectId, taskId, commentId } =
    await context.params;

  return proxyCommentRequest(
    request,
    `/api/spaces/${encodeURIComponent(id)}/projects/${encodeURIComponent(projectId)}/tasks/${encodeURIComponent(taskId)}/comments/${encodeURIComponent(commentId)}`,
  );
}
