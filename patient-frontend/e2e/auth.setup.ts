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
    await page.fill('input[placeholder="e.g. City Medical Centre"]', 'E2E Test Organisation');
    await page.fill('input[placeholder="admin@example.com"]', email);
    // Start watching before clicking — navigation includes a Cognito session refresh
    // which is a network round-trip and can take 20-30s in CI
    await Promise.all([
      page.waitForURL(/\/app\/patients/, { timeout: 40000 }),
      page.click('button:has-text("Create Organisation")'),
    ]);
  }

  fs.mkdirSync(path.dirname(authFile), { recursive: true });
  await page.context().storageState({ path: authFile });
});
