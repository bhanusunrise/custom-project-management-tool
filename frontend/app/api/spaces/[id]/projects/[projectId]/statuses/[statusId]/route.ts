
import { NextRequest } from "next/server";
import { proxyStatusRequest } from "../_lib/statusProxy";

type RouteContext = {
  params: Promise<{
    id: string;
    projectId: string;
    statusId: string;
  }>;
};

export async function PUT(
  request: NextRequest,
  context: RouteContext,
) {
  const { id, projectId, statusId } = await context.params;

  return proxyStatusRequest(
    request,
    `/api/spaces/${encodeURIComponent(id)}/projects/${encodeURIComponent(projectId)}/statuses/${encodeURIComponent(statusId)}`,
  );
}

export async function DELETE(
  request: NextRequest,
  context: RouteContext,
) {
  const { id, projectId, statusId } = await context.params;

  return proxyStatusRequest(
    request,
    `/api/spaces/${encodeURIComponent(id)}/projects/${encodeURIComponent(projectId)}/statuses/${encodeURIComponent(statusId)}`,
  );
}
