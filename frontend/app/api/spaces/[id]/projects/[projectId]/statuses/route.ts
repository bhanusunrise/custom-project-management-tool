
import { NextRequest } from "next/server";
import { proxyStatusRequest } from "./_lib/statusProxy";

type RouteContext = {
  params: Promise<{
    id: string;
    projectId: string;
  }>;
};

export async function GET(
  request: NextRequest,
  context: RouteContext,
) {
  const { id, projectId } = await context.params;

  return proxyStatusRequest(
    request,
    `/api/spaces/${encodeURIComponent(id)}/projects/${encodeURIComponent(projectId)}/statuses`,
  );
}

export async function POST(
  request: NextRequest,
  context: RouteContext,
) {
  const { id, projectId } = await context.params;

  return proxyStatusRequest(
    request,
    `/api/spaces/${encodeURIComponent(id)}/projects/${encodeURIComponent(projectId)}/statuses`,
  );
}
