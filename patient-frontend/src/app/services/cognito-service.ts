import { Injectable, signal } from "@angular/core";
import { Amplify } from "aws-amplify";
import { signIn, signUp, confirmSignUp, signOut, fetchAuthSession } from "@aws-amplify/auth";

Amplify.configure({
  Auth: {
    Cognito: {
      userPoolId: 'eu-west-1_Sb4mcDano',
      userPoolClientId: '1lnss15djptnbbaanrvgagvt9q',
    }
  }
});

@Injectable({
  providedIn: "root",
})
export class CognitoService {

  authenticated = signal(false);
  hasOrganization = signal(false);

  async init(): Promise<void> {
    try {
      const session = await fetchAuthSession({ forceRefresh: true });
      console.log('init payload:', session.tokens?.idToken?.payload);
      this.authenticated.set(!!session.tokens?.idToken);
      this.hasOrganization.set(!!session.tokens?.idToken?.payload['custom:organizationId']);
    } catch {
      this.authenticated.set(false);
      this.hasOrganization.set(false);
    }
  }

  async signIn(email: string, password: string): Promise<void> {
    try {
      await signIn({ username: email, password });
      const session = await fetchAuthSession();
      this.authenticated.set(true);
      this.hasOrganization.set(!!session.tokens?.idToken?.payload['custom:organizationId']);
    } catch (error) {
      this.authenticated.set(false);
      throw error;
    }
  }

  async signUp(email: string, password: string): Promise<void> {
    await signUp({
      username: email,
      password,
      options: { userAttributes: { email } }
    });
  }

  async confirmSignUp(email: string, code: string): Promise<void> {
    await confirmSignUp({ username: email, confirmationCode: code });
  }

  async signOut(): Promise<void> {
    await signOut();
    this.authenticated.set(false);
    this.hasOrganization.set(false);
  }

  async refreshSession(): Promise<void> {
    const session = await fetchAuthSession({ forceRefresh: true });
    this.hasOrganization.set(!!session.tokens?.idToken?.payload['custom:organizationId']);
  }

  async getValidToken(): Promise<string | undefined> {
    try {
      const session = await fetchAuthSession({ forceRefresh: false });
      return session.tokens?.idToken?.toString();
    } catch {
      this.authenticated.set(false);
      return undefined;
    }
  }
}
