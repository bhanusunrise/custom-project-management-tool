
import { NextRequest } from "next/server";
import { proxyCommentRequest } from "./_lib/commentProxy";

type RouteContext = {
  params: Promise<{
    id: string;
    projectId: string;
    taskId: string;
  }>;
};

/**
 * Retrieve all comments belonging to a task.
 */
export async function GET(
  request: NextRequest,
  context: RouteContext,
) {
  const { id, projectId, taskId } = await context.params;

  return proxyCommentRequest(
    request,
    `/api/spaces/${encodeURIComponent(id)}/projects/${encodeURIComponent(projectId)}/tasks/${encodeURIComponent(taskId)}/comments`,
  );
}

/**
 * Create a comment for a task.
 */
export async function POST(
  request: NextRequest,
  context: RouteContext,
) {
  const { id, projectId, taskId } = await context.params;

  return proxyCommentRequest(
    request,
    `/api/spaces/${encodeURIComponent(id)}/projects/${encodeURIComponent(projectId)}/tasks/${encodeURIComponent(taskId)}/comments`,
  );
}
