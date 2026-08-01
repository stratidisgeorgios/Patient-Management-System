import { test, expect } from '@playwright/test';

// Creates a patient + category + treatment, then exercises the full billing flow:
// add a charge to the patient, verify it appears, remove it, then clean everything up.
test('billing: charge a patient for a treatment then remove it', async ({ page }) => {
  const ts = Date.now();
  const patientName = `E2E Billing Patient ${ts}`;
  const categoryName = `E2E Billing Cat ${ts}`;
  const treatmentName = `E2E Billing Treatment ${ts}`;
  const treatmentPrice = '200';

  // --- Setup: create category ---
  await page.goto('/app/treatments');
  await page.click('button:has-text("Categories")');
  await page.locator('.modal-card button:has-text("+ Add Category")').click();
  const catModal = page.locator('.modal-card');
  await catModal.locator('input[placeholder="Category name"]').fill(categoryName);
  await catModal.locator('input[placeholder="Category description"]').fill('Billing E2E');
  await catModal.locator('button:has-text("Create Category")').click();
  await expect(catModal).toBeHidden({ timeout: 8000 });

  // --- Setup: create treatment ---
  await page.click('button:has-text("+ Add Treatment")');
  const treatModal = page.locator('.modal-card');
  await expect(treatModal).toBeVisible();
  await treatModal.locator('input[placeholder="Treatment name"]').fill(treatmentName);
  await treatModal.locator('select').selectOption({ label: categoryName });
  await treatModal.locator('input[type="number"]').fill(treatmentPrice);
  await treatModal.locator('button:has-text("Create Treatment")').click();
  await expect(treatModal).toBeHidden({ timeout: 8000 });

  // --- Setup: create patient ---
  await page.goto('/app/patients');
  await page.click('button:has-text("+ Add Patient")');
  const patModal = page.locator('.modal-card');
  await expect(patModal).toBeVisible();
  await patModal.locator('input[placeholder="Full name"]').fill(patientName);
  await patModal.locator('input[type="email"]').fill(`e2ebilling${ts}@test.com`);
  await patModal.locator('select').selectOption('FEMALE');
  await patModal.locator('input[placeholder="Street address"]').fill('99 Billing Lane');
  const dateInputs = patModal.locator('input[type="date"]');
  await dateInputs.nth(0).fill('1985-03-20');
  await dateInputs.nth(1).fill('2024-06-01');
  await patModal.locator('button:has-text("Create Patient")').click();
  await expect(patModal).toBeHidden({ timeout: 8000 });

  // Navigate to patient profile
  await page.fill('input[placeholder="Search patients..."]', patientName);
  const patRow = page.locator('tr.patient-row', { hasText: patientName });
  await expect(patRow).toBeVisible({ timeout: 12000 });
  await patRow.locator('button.action-view').click();
  await expect(page).toHaveURL(/\/app\/patients\/\d+/, { timeout: 10000 });

  // Wait for billing section to load (async after patient info)
  const billingSection = page.locator('.billing-header');
  await expect(billingSection).toBeVisible({ timeout: 10000 });

  // --- Add charge ---
  const chargeSearch = page.locator('input[placeholder="Search treatments to add a charge..."]');
  await chargeSearch.fill(treatmentName.substring(0, 10)); // partial match triggers search
  const dropdown = page.locator('.treatment-dropdown');
  await expect(dropdown).toBeVisible({ timeout: 10000 });
  await dropdown.locator('.treatment-option', { hasText: treatmentName }).click();

  // Verify charge row appears with correct treatment name and price
  const chargeRow = page.locator('tr.charge-row', { hasText: treatmentName });
  await expect(chargeRow).toBeVisible({ timeout: 10000 });
  await expect(chargeRow.locator('td', { hasText: '$200' })).toBeVisible();

  // Verify balance updated
  await expect(page.locator('.billing-balance-value')).toContainText('$200');

  // --- Remove charge ---
  await chargeRow.locator('button.action-remove').click();
  await expect(chargeRow).toBeHidden({ timeout: 8000 });
  await expect(page.locator('.no-charges')).toBeVisible();

  // --- Cleanup: delete patient ---
  await page.click('button:has-text("Delete")');
  await expect(page.locator('.confirm-card')).toBeVisible();
  await page.locator('.confirm-actions button:has-text("Confirm")').click();
  await expect(page).toHaveURL(/\/app\/patients$/, { timeout: 10000 });

  // --- Cleanup: delete treatment ---
  await page.goto('/app/treatments');
  await page.fill('input[placeholder="Search treatments..."]', treatmentName);
  const treatRow = page.locator('tr.treatment-row', { hasText: treatmentName });
  await expect(treatRow).toBeVisible({ timeout: 12000 });
  await treatRow.locator('button.action-view').click();
  await expect(page).toHaveURL(/\/app\/treatments\/\d+/, { timeout: 10000 });
  await page.click('button:has-text("Delete")');
  await page.locator('.confirm-actions button:has-text("Confirm")').click();
  await expect(page).toHaveURL(/\/app\/treatments$/, { timeout: 10000 });

  // --- Cleanup: delete category ---
  await page.click('button:has-text("Categories")');
  const cleanCatModal = page.locator('.modal-card');
  await expect(cleanCatModal).toBeVisible();
  const cleanCatRow = cleanCatModal.locator('tr.category-row', { hasText: categoryName });
  await expect(cleanCatRow).toBeVisible({ timeout: 5000 });
  await cleanCatRow.locator('button.action-delete').click();
  await page.locator('.confirm-actions button:has-text("Confirm")').click();
  await expect(cleanCatRow).toBeHidden({ timeout: 8000 });
});
