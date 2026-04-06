package com.project.finance.service;

import com.project.finance.dto.request.RecordRequest;
import com.project.finance.dto.response.DashboardSummaryResponse;
import com.project.finance.dto.response.TrendResponse;
import com.project.finance.model.RecordType;
import com.project.finance.model.Role;
import com.project.finance.model.User;
import com.project.finance.model.UserStatus;
import com.project.finance.repository.FinancialRecordRepository;
import com.project.finance.repository.UserRepository;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for DashboardService.
 *
 * Feature: finance-dashboard
 * Property 10: Dashboard summary invariant
 * Property 11: Monthly trends correctness
 * Validates: Requirements 6.1–6.4
 */
@SpringBootTest
@JqwikSpringSupport
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DashboardServicePropertyTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private RecordService recordService;

    @Autowired
    private FinancialRecordRepository recordRepository;

    @Autowired
    private UserRepository userRepository;

    // -------------------------------------------------------------------------
    // Property 10: Dashboard summary invariant
    // For any set of non-deleted financial records, the dashboard summary SHALL
    // satisfy:
    //   - totalIncome  = sum of all INCOME record amounts
    //   - totalExpenses = sum of all EXPENSE record amounts
    //   - netBalance   = totalIncome - totalExpenses
    //   - each category total = sum of amounts for records in that category
    //   - recentTransactions contains exactly min(5, total records) records
    //     ordered by date descending
    // Validates: Requirements 6.1, 6.2, 6.4
    // -------------------------------------------------------------------------

    @Property(tries = 100)
    @Label("Property 10: Dashboard summary invariant")
    void dashboardSummaryInvariant(@ForAll("recordScenarios") List<RecordRequest> requests) {
        // Feature: finance-dashboard, Property 10: Dashboard summary invariant
        // Validates: Requirements 6.1, 6.2, 6.4

        User admin = createAndSaveUser();
        try {
            for (RecordRequest req : requests) {
                recordService.createRecord(req, admin);
            }

            DashboardSummaryResponse summary = dashboardService.getSummary();

            // Compute expected values from the requests directly
            BigDecimal expectedIncome = requests.stream()
                    .filter(r -> r.getType() == RecordType.INCOME)
                    .map(RecordRequest::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal expectedExpenses = requests.stream()
                    .filter(r -> r.getType() == RecordType.EXPENSE)
                    .map(RecordRequest::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal expectedNet = expectedIncome.subtract(expectedExpenses);

            // totalIncome and totalExpenses must match (using compareTo for BigDecimal scale)
            assertThat(summary.getTotalIncome().compareTo(expectedIncome)).isZero();
            assertThat(summary.getTotalExpenses().compareTo(expectedExpenses)).isZero();

            // netBalance = totalIncome - totalExpenses
            assertThat(summary.getNetBalance().compareTo(expectedNet)).isZero();

            // Category totals: each category sum must match
            Map<String, BigDecimal> categoryTotals = summary.getCategoryTotals();
            Map<String, BigDecimal> expectedCategoryTotals = new java.util.HashMap<>();
            for (RecordRequest req : requests) {
                expectedCategoryTotals.merge(req.getCategory(), req.getAmount(), BigDecimal::add);
            }
            for (Map.Entry<String, BigDecimal> entry : expectedCategoryTotals.entrySet()) {
                assertThat(categoryTotals).containsKey(entry.getKey());
                assertThat(categoryTotals.get(entry.getKey()).compareTo(entry.getValue())).isZero();
            }

            // recentTransactions: at most 5, ordered by date descending
            List<?> recent = summary.getRecentTransactions();
            int expectedCount = Math.min(5, requests.size());
            assertThat(recent).hasSizeLessThanOrEqualTo(5);
            assertThat(recent.size()).isEqualTo(expectedCount);

            // Verify descending date order
            if (recent.size() > 1) {
                var responses = summary.getRecentTransactions();
                for (int i = 0; i < responses.size() - 1; i++) {
                    assertThat(responses.get(i).getDate())
                            .isAfterOrEqualTo(responses.get(i + 1).getDate());
                }
            }

        } finally {
            recordRepository.deleteAll(recordRepository.findAll().stream()
                    .filter(r -> r.getCreatedBy().getId().equals(admin.getId()))
                    .toList());
            userRepository.delete(admin);
        }
    }

    // -------------------------------------------------------------------------
    // Property 11: Monthly trends correctness
    // For any set of non-deleted financial records in the current year, each
    // month entry in getTrends() SHALL have totalIncome equal to the sum of
    // INCOME amounts for that month and totalExpenses equal to the sum of
    // EXPENSE amounts for that month.
    // Validates: Requirements 6.3
    // -------------------------------------------------------------------------

    @Property(tries = 100)
    @Label("Property 11: Monthly trends correctness")
    void monthlyTrendsCorrectness(@ForAll("currentYearRecordScenarios") List<RecordRequest> requests) {
        // Feature: finance-dashboard, Property 11: Monthly trends correctness
        // Validates: Requirements 6.3

        User admin = createAndSaveUser();
        try {
            for (RecordRequest req : requests) {
                recordService.createRecord(req, admin);
            }

            List<TrendResponse> trends = dashboardService.getTrends();

            // Build expected per-month totals from the seeded requests
            Map<Integer, BigDecimal> expectedIncomeByMonth = new java.util.HashMap<>();
            Map<Integer, BigDecimal> expectedExpenseByMonth = new java.util.HashMap<>();

            for (RecordRequest req : requests) {
                int month = req.getDate().getMonthValue();
                if (req.getType() == RecordType.INCOME) {
                    expectedIncomeByMonth.merge(month, req.getAmount(), BigDecimal::add);
                } else {
                    expectedExpenseByMonth.merge(month, req.getAmount(), BigDecimal::add);
                }
            }

            // Every trend entry must match the expected sums for that month
            for (TrendResponse trend : trends) {
                int month = trend.getMonth();
                BigDecimal expectedIncome = expectedIncomeByMonth.getOrDefault(month, BigDecimal.ZERO);
                BigDecimal expectedExpense = expectedExpenseByMonth.getOrDefault(month, BigDecimal.ZERO);

                assertThat(trend.getTotalIncome().compareTo(expectedIncome)).isZero();
                assertThat(trend.getTotalExpenses().compareTo(expectedExpense)).isZero();
            }

            // Every month that has records must appear in the trends
            java.util.Set<Integer> monthsWithRecords = new java.util.HashSet<>();
            monthsWithRecords.addAll(expectedIncomeByMonth.keySet());
            monthsWithRecords.addAll(expectedExpenseByMonth.keySet());

            java.util.Set<Integer> trendMonths = new java.util.HashSet<>();
            for (TrendResponse t : trends) {
                trendMonths.add(t.getMonth());
            }

            assertThat(trendMonths).containsAll(monthsWithRecords);

        } finally {
            recordRepository.deleteAll(recordRepository.findAll().stream()
                    .filter(r -> r.getCreatedBy().getId().equals(admin.getId()))
                    .toList());
            userRepository.delete(admin);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User createAndSaveUser() {
        User u = new User();
        u.setName("Admin");
        u.setEmail("admin-" + UUID.randomUUID() + "@test.com");
        u.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        u.setRole(Role.ADMIN);
        u.setStatus(UserStatus.ACTIVE);
        return userRepository.save(u);
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    /**
     * Generates 1–5 records with dates spread across any year (for Property 10).
     * Uses a fixed small set of dates to keep the test deterministic enough.
     */
    @Provide
    Arbitrary<List<RecordRequest>> recordScenarios() {
        return recordRequest(false).list().ofMinSize(1).ofMaxSize(5);
    }

    /**
     * Generates 1–5 records all dated within the current year (for Property 11).
     */
    @Provide
    Arbitrary<List<RecordRequest>> currentYearRecordScenarios() {
        return recordRequest(true).list().ofMinSize(1).ofMaxSize(5);
    }

    private Arbitrary<RecordRequest> recordRequest(boolean currentYearOnly) {
        Arbitrary<BigDecimal> amounts = Arbitraries.bigDecimals()
                .between(BigDecimal.ONE, new BigDecimal("99999"))
                .ofScale(2);
        Arbitrary<RecordType> types = Arbitraries.of(RecordType.class);
        Arbitrary<String> categories = Arbitraries.of("Food", "Transport", "Salary", "Utilities", "Entertainment");

        int currentYear = LocalDate.now().getYear();
        Arbitrary<LocalDate> dates = currentYearOnly
                ? Arbitraries.of(
                        LocalDate.of(currentYear, 1, 15),
                        LocalDate.of(currentYear, 3, 10),
                        LocalDate.of(currentYear, 6, 20),
                        LocalDate.of(currentYear, 9, 5),
                        LocalDate.of(currentYear, 12, 1))
                : Arbitraries.of(
                        LocalDate.of(2024, 2, 10),
                        LocalDate.of(2025, 5, 15),
                        LocalDate.of(currentYear, 4, 1),
                        LocalDate.of(currentYear, 8, 22),
                        LocalDate.of(currentYear, 11, 30));

        return Combinators.combine(amounts, types, categories, dates)
                .as((amount, type, category, date) -> {
                    RecordRequest r = new RecordRequest();
                    r.setAmount(amount);
                    r.setType(type);
                    r.setCategory(category);
                    r.setDate(date);
                    r.setDescription(null);
                    return r;
                });
    }
}
