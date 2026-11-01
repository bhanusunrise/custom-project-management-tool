import type { NextAuthOptions } from "next-auth";
import CognitoProvider from "next-auth/providers/cognito";

export const authOptions: NextAuthOptions = {
  providers: [
    CognitoProvider({
      clientId: process.env.COGNITO_CLIENT_ID!,
      clientSecret: process.env.COGNITO_CLIENT_SECRET!,
      issuer: process.env.COGNITO_ISSUER!,
    }),
  ],

  session: {
    strategy: "jwt",
  },

  pages: {
    signIn: "/login",
  },

  callbacks: {
    async jwt({ token, account }) {
      // Run this block only when Cognito returns a new login.
      if (account) {
        token.accessToken = account.access_token;
        token.accessTokenExpires = account.expires_at
          ? account.expires_at * 1000
          : 0;

        // Synchronize the authenticated user with PostgreSQL.
        if (account.access_token) {
          try {
            const response = await fetch(
              `${process.env.SPRING_BOOT_API_URL}/api/members/me`,
              {
                method: "GET",
                headers: {
                  Authorization: `Bearer ${account.access_token}`,
                },
                cache: "no-store",
              }
            );

            if (!response.ok) {
              console.error(
                "Member synchronization failed:",
                response.status,
                await response.text()
              );
            } else {
              console.log(
                "Member synchronized successfully with PostgreSQL"
              );
            }
          } catch (error) {
            console.error("Member synchronization error:", error);
          }
        }
      }

      return token;
    },

    async session({ session }) {
      // Do not expose the Cognito access token to the browser.
      return session;
    },
  },
};