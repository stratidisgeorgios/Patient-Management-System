import { test, expect } from '@playwright/test';

test('patient CRUD: create, search, view, edit, delete', async ({ page }) => {
  const ts = Date.now();
  const name = `E2E Patient ${ts}`;
  const email = `e2epatient${ts}@test.com`;
  const updatedName = `${name} Updated`;

  await page.goto('/app/patients');
  await expect(page.locator('input[placeholder="Search patients..."]')).toBeVisible();
  await expect(page.locator('button:has-text("+ Add Patient")')).toBeVisible();

  // --- Create ---
  await page.click('button:has-text("+ Add Patient")');
  const modal = page.locator('.modal-card');
  await expect(modal).toBeVisible();

  await modal.locator('input[placeholder="Full name"]').fill(name);
  await modal.locator('input[type="email"]').fill(email);
  await modal.locator('select').selectOption('MALE');
  await modal.locator('input[placeholder="Street address"]').fill('1 Test Avenue');
  const dateInputs = modal.locator('input[type="date"]');
  await dateInputs.nth(0).fill('1990-06-15');  // dateOfBirth
  await dateInputs.nth(1).fill('2024-01-01');  // registeredDate

  await modal.locator('button:has-text("Create Patient")').click();
  await expect(modal).toBeHidden({ timeout: 8000 });

  // --- Search ---
  // Retry search until the item appears in the search index (Elasticsearch has indexing delay)
  const row = page.locator('tr.patient-row', { hasText: name });
  await expect(async () => {
    await page.fill('input[placeholder="Search patients..."]', '');
    await page.fill('input[placeholder="Search patients..."]', name);
    await expect(row).toBeVisible({ timeout: 5000 });
  }).toPass({ timeout: 30000 });

  // --- View profile ---
  await row.locator('button.action-view').click();
  await expect(page).toHaveURL(/\/app\/patients\/\d+/, { timeout: 10000 });
  await expect(page.locator('.profile-field-value', { hasText: name })).toBeVisible();
  await expect(page.locator('.profile-field-value', { hasText: email })).toBeVisible();

  // --- Edit ---
  await page.click('button:has-text("Edit")');
  const editModal = page.locator('.modal-card');
  await expect(editModal).toBeVisible();
  await editModal.locator('input[placeholder="Full name"]').fill(updatedName);
  await editModal.locator('button:has-text("Save Changes")').click();
  await expect(editModal).toBeHidden({ timeout: 8000 });
  await expect(page.locator('.profile-field-value', { hasText: updatedName })).toBeVisible();

  // --- Delete ---
  await page.click('button:has-text("Delete")');
  await expect(page.locator('.confirm-card')).toBeVisible();
  await page.locator('.confirm-actions button:has-text("Confirm")').click();
  await expect(page).toHaveURL(/\/app\/patients$/, { timeout: 10000 });
  await expect(page.locator('input[placeholder="Search patients..."]')).toBeVisible();
});
