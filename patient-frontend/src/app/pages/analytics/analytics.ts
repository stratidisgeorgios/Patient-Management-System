import { Component, OnInit, signal } from "@angular/core";
import { AnalyticsService } from "../../services/analytics-service";
import { forkJoin } from "rxjs";

interface MonthData { month: string; count: number; }

@Component({
  selector: "app-analytics",
  standalone: true,
  imports: [],
  templateUrl: "./analytics.html",
  styleUrl: "./analytics.css",
})
export class Analytics implements OnInit {
  isLoading = signal(true);
  error = signal<string | null>(null);

  activePatients = signal(0);
  averageAge = signal(0);
  annualRevenue = signal('0');
  malePct = signal(0);
  femalePct = signal(0);
  monthlyRegistrations = signal<MonthData[]>([]);
  mostUsedTreatments = signal<{ name: string; count: number }[]>([]);
  revenuePerCategory = signal<{ category: string; amount: number }[]>([]);

  readonly currentYear = new Date().getFullYear();

  private readonly MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
                             'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

  constructor(private analyticsService: AnalyticsService) {}

  ngOnInit() {
    forkJoin({
      activePatients: this.analyticsService.getActivePatients(),
      averageAge: this.analyticsService.getAverageAge(),
      genderDistribution: this.analyticsService.getGenderDistribution(),
      monthly: this.analyticsService.getPatientRegistrationsPerMonth(this.currentYear),
      annualRevenue: this.analyticsService.getAnnualRevenue(this.currentYear),
      mostUsedTreatments: this.analyticsService.getMostUsedTreatments(),
      revenuePerCategory: this.analyticsService.getRevenuePerCategory(),
    }).subscribe({
      next: (data) => {
        this.activePatients.set(data.activePatients ?? 0);
        this.averageAge.set(Math.round(data.averageAge ?? 0));
        this.annualRevenue.set(data.annualRevenue ?? '0');

        const male = data.genderDistribution?.['MALE'] ?? 0;
        const female = data.genderDistribution?.['FEMALE'] ?? 0;
        this.malePct.set(Math.round(male));
        this.femalePct.set(Math.round(female));

        const months = this.MONTHS.map(m => ({ month: m, count: 0 }));
        for (const row of (data.monthly ?? [])) {
          const idx = Number(row[0]) - 1;
          if (idx >= 0 && idx < 12) months[idx].count = Number(row[1]);
        }
        this.monthlyRegistrations.set(months);

        this.mostUsedTreatments.set(
          (data.mostUsedTreatments ?? []).slice(0, 5)
            .map((row: any[]) => ({ name: String(row[0]), count: Number(row[1]) }))
        );

        this.revenuePerCategory.set(
          (data.revenuePerCategory ?? [])
            .map((row: any[]) => ({ category: String(row[0]), amount: Number(row[1]) }))
            .sort((a, b) => b.amount - a.amount)
            .slice(0, 5)
        );

        this.isLoading.set(false);
      },
      error: () => {
        this.error.set('Failed to load analytics data. Please try again.');
        this.isLoading.set(false);
      }
    });
  }

  get maxMonthCount(): number {
    return Math.max(1, ...this.monthlyRegistrations().map(m => m.count));
  }

  formatRevenue(raw: string): string {
    return this.formatAmount(parseFloat(raw) || 0);
  }

  formatAmount(n: number): string {
    if (n >= 1_000_000) return `€${(n / 1_000_000).toFixed(1)}M`;
    if (n >= 1_000) return `€${(n / 1_000).toFixed(1)}K`;
    return `€${n.toFixed(0)}`;
  }

  formatCount(n: number): string {
    if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
    if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
    return String(n);
  }
}
