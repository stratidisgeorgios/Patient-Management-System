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

    // Navigate includes: POST /api/organizations → cognitoService.refreshSession() → router.navigate
    // refreshSession is a Cognito round-trip and can be slow
    await Promise.all([
      page.waitForURL(/\/app\/patients/, { timeout: 60000 }),
      page.click('button:has-text("Create Organisation")'),
    ]);
  }

  fs.mkdirSync(path.dirname(authFile), { recursive: true });
  await page.context().storageState({ path: authFile });
});
