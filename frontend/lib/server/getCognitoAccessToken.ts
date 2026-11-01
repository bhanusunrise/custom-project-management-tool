import { getToken } from "next-auth/jwt";
import type { NextRequest } from "next/server";

export async function getCognitoAccessToken(request: NextRequest) {
  const token = await getToken({
    req: request,
    secret: process.env.NEXTAUTH_SECRET,
  });

  // Reject missing or expired Cognito access tokens.
  if (
    typeof token?.accessToken !== "string" ||
    typeof token.accessTokenExpires !== "number" ||
    Date.now() >= token.accessTokenExpires
  ) {
    return null;
  }

  return token.accessToken;
}