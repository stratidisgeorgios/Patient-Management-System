import { test, expect } from '@playwright/test';

test.describe('Unauthenticated', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('login page renders email, password and sign in button', async ({ page }) => {
    await page.goto('/login');
    await expect(page.locator('input[type="email"]')).toBeVisible();
    await expect(page.locator('input[type="password"]')).toBeVisible();
    await expect(page.locator('button:has-text("Sign In")')).toBeVisible();
  });

  test('visiting /app/patients without a session redirects to /login', async ({ page }) => {
    await page.goto('/app/patients');
    await expect(page).toHaveURL(/\/login/);
  });
});

test('authenticated user sees logout button in header', async ({ page }) => {
  await page.goto('/app/patients');
  await expect(page.locator('button:has-text("Logout")')).toBeVisible();
});
