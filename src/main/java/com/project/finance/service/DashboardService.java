package com.project.finance.service;

import com.project.finance.dto.response.DashboardSummaryResponse;
import com.project.finance.dto.response.RecordResponse;
import com.project.finance.dto.response.TrendResponse;
import com.project.finance.model.RecordType;
import com.project.finance.repository.FinancialRecordRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final FinancialRecordRepository recordRepository;

    public DashboardService(FinancialRecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    public DashboardSummaryResponse getSummary() {
        BigDecimal totalIncome = recordRepository.sumTotalIncome();
        BigDecimal totalExpenses = recordRepository.sumTotalExpenses();
        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        // Build category totals map from Object[] rows: [category, sum]
        Map<String, BigDecimal> categoryTotals = new HashMap<>();
        for (Object[] row : recordRepository.sumByCategory()) {
            categoryTotals.put((String) row[0], (BigDecimal) row[1]);
        }

        // Top 5 recent transactions
        List<RecordResponse> recentTransactions = recordRepository
                .findTop5ByDeletedFalseOrderByDateDesc()
                .stream()
                .map(RecordResponse::from)
                .collect(Collectors.toList());

        return new DashboardSummaryResponse(totalIncome, totalExpenses, netBalance,
                categoryTotals, recentTransactions);
    }

    public List<TrendResponse> getTrends() {
        int currentYear = LocalDate.now().getYear();
        List<Object[]> rows = recordRepository.findMonthlyTrends(currentYear);

        // Aggregate rows into a map keyed by (year, month)
        // Each row: [year(int), month(int), type(RecordType), sum(BigDecimal)]
        Map<String, BigDecimal[]> monthMap = new HashMap<>();

        for (Object[] row : rows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            RecordType type = RecordType.valueOf(row[2].toString());
            BigDecimal amount = (BigDecimal) row[3];

            String key = year + "-" + month;
            monthMap.computeIfAbsent(key, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});

            if (type == RecordType.INCOME) {
                monthMap.get(key)[0] = monthMap.get(key)[0].add(amount);
            } else {
                monthMap.get(key)[1] = monthMap.get(key)[1].add(amount);
            }
        }

        List<TrendResponse> trends = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> entry : monthMap.entrySet()) {
            String[] parts = entry.getKey().split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            trends.add(new TrendResponse(year, month, entry.getValue()[0], entry.getValue()[1]));
        }

        // Sort by month ascending
        trends.sort((a, b) -> Integer.compare(a.getMonth(), b.getMonth()));
        return trends;
    }
}
