import { test, expect } from '@playwright/test';

test('analytics page shows all dashboard sections', async ({ page }) => {
  await page.goto('/app/analytics');

  // Stat cards
  await expect(page.locator('.stat-card', { hasText: 'Active Patients' })).toBeVisible({ timeout: 10000 });
  await expect(page.locator('.stat-card', { hasText: 'Average Age' })).toBeVisible();
  await expect(page.locator('.stat-card', { hasText: /Revenue/ })).toBeVisible();

  // Gender distribution
  await expect(page.locator('.gender-bar')).toBeVisible();

  // Monthly registrations bar chart (12 bars — one per month)
  const bars = page.locator('.bar-col');
  await expect(bars).toHaveCount(12);

  // Most used treatments + revenue by category panels
  await expect(page.locator('.analytics-card', { hasText: 'Most Used Treatments' })).toBeVisible();
  await expect(page.locator('.analytics-card', { hasText: 'Revenue by Category' })).toBeVisible();
});
