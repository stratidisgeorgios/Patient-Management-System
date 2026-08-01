import { test as setup } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

const authFile = 'e2e/.auth/user.json';

setup('authenticate and set up organisation', async ({ page }) => {
  const email = process.env['E2E_TEST_EMAIL']!;
  const password = process.env['E2E_TEST_PASSWORD']!;

  await page.goto('/login');
  await page.fill('input[type="email"]', email);
  await page.fill('input[type="password"]', password);
  await page.click('button:has-text("Sign In")');

  // After login, lands on either patients (org exists) or create-organisation (first run)
  await page.waitForURL(/\/(app\/patients|app\/create-organization)/, { timeout: 20000 });

  if (page.url().includes('create-organization')) {
    // Use pressSequentially to fire keydown/input/keyup per character so Angular's
    // reactive form validators update correctly (page.fill sets DOM value only)
    await page.locator('input[placeholder="e.g. City Medical Centre"]').pressSequentially('E2E Test Organisation');
    await page.locator('input[placeholder="admin@example.com"]').pressSequentially(email);
    await page.waitForTimeout(500); // let Angular change detection mark the form valid

    // Intercept the org creation API response so we know when it succeeds,
    // without relying on refreshSession() completing (it can hang in CI).
    const [response] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/api/organizations') && r.request().method() === 'POST',
        { timeout: 15000 }
      ),
      page.click('button:has-text("Create Organisation")'),
    ]);

    if (!response.ok()) {
      throw new Error(`Organisation creation failed with status ${response.status()}`);
    }

    // Org created — Cognito attribute custom:organizationId is now set on the user.
    // Sign out and back in so Amplify gets a fresh ID token that includes the attribute.
    // This is more reliable than waiting for cognitoService.refreshSession() to navigate.
    await page.click('button:has-text("Logout")');
    await page.waitForURL(/\/login/, { timeout: 10000 });
    // Wait for Angular to finish bootstrapping the login form before interacting.
    // networkidle ensures auth-state checks complete and the DOM stops re-rendering.
    await page.waitForLoadState('networkidle');
    await page.locator('input[type="email"]').fill(email);
    await page.locator('input[type="password"]').fill(password);
    await page.click('button:has-text("Sign In")');
    await page.waitForURL(/\/app\/patients/, { timeout: 30000 });
  }

  fs.mkdirSync(path.dirname(authFile), { recursive: true });
  await page.context().storageState({ path: authFile });
});
