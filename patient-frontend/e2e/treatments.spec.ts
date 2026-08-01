import { test, expect } from '@playwright/test';

test('treatment and category CRUD: create, search, view, edit, delete', async ({ page }) => {
  const ts = Date.now();
  const categoryName = `E2E Category ${ts}`;
  const treatmentName = `E2E Treatment ${ts}`;
  const updatedName = `${treatmentName} Updated`;

  await page.goto('/app/treatments');
  await expect(page.locator('input[placeholder="Search treatments..."]')).toBeVisible();

  // --- Create category ---
  // "Categories" opens the list modal; clicking "+ Add Category" inside it calls closeAllModals()
  // then opens the create modal — so only one modal is ever visible at a time
  await page.click('button:has-text("Categories")');
  await expect(page.locator('.modal-card')).toBeVisible();
  await page.locator('.modal-card button:has-text("+ Add Category")').click();

  const createCatModal = page.locator('.modal-card');
  await expect(createCatModal).toBeVisible();
  await createCatModal.locator('input[placeholder="Category name"]').fill(categoryName);
  await createCatModal.locator('input[placeholder="Category description"]').fill('E2E test category');
  await createCatModal.locator('button:has-text("Create Category")').click();
  await expect(createCatModal).toBeHidden({ timeout: 8000 });

  // --- Create treatment ---
  await page.click('button:has-text("+ Add Treatment")');
  const createModal = page.locator('.modal-card');
  await expect(createModal).toBeVisible();
  await createModal.locator('input[placeholder="Treatment name"]').fill(treatmentName);
  await createModal.locator('select').selectOption({ label: categoryName });
  await createModal.locator('input[type="number"]').fill('150');
  await createModal.locator('button:has-text("Create Treatment")').click();
  await expect(createModal).toBeHidden({ timeout: 8000 });

  // --- Search ---
  await page.fill('input[placeholder="Search treatments..."]', treatmentName);
  const row = page.locator('tr.treatment-row', { hasText: treatmentName });
  await expect(row).toBeVisible({ timeout: 12000 });

  // --- View profile ---
  await row.locator('button.action-view').click();
  await expect(page).toHaveURL(/\/app\/treatments\/\d+/, { timeout: 10000 });
  await expect(page.locator('.profile-field-value', { hasText: treatmentName })).toBeVisible();
  await expect(page.locator('.profile-field-value', { hasText: categoryName })).toBeVisible();
  await expect(page.locator('.profile-field-value', { hasText: '$150' })).toBeVisible();

  // --- Edit ---
  await page.click('button:has-text("Edit")');
  const editModal = page.locator('.modal-card');
  await expect(editModal).toBeVisible();
  await editModal.locator('input[placeholder="Treatment name"]').fill(updatedName);
  await editModal.locator('button:has-text("Save Changes")').click();
  await expect(editModal).toBeHidden({ timeout: 8000 });
  await expect(page.locator('.profile-field-value', { hasText: updatedName })).toBeVisible();

  // --- Delete treatment ---
  await page.click('button:has-text("Delete")');
  await expect(page.locator('.confirm-card')).toBeVisible();
  await page.locator('.confirm-actions button:has-text("Confirm")').click();
  await expect(page).toHaveURL(/\/app\/treatments$/, { timeout: 10000 });

  // --- Delete category ---
  await page.click('button:has-text("Categories")');
  const catModal = page.locator('.modal-card');
  await expect(catModal).toBeVisible();
  const catRow = catModal.locator('tr.category-row', { hasText: categoryName });
  await expect(catRow).toBeVisible({ timeout: 5000 });
  await catRow.locator('button.action-delete').click();
  await expect(page.locator('.confirm-card')).toBeVisible();
  await page.locator('.confirm-actions button:has-text("Confirm")').click();
  await expect(catRow).toBeHidden({ timeout: 8000 });
});
