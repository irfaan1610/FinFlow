package com.project.finance.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardSummaryResponse {

    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netBalance;
    private Map<String, BigDecimal> categoryTotals;
    private List<RecordResponse> recentTransactions;

    public DashboardSummaryResponse(BigDecimal totalIncome, BigDecimal totalExpenses,
                                    BigDecimal netBalance, Map<String, BigDecimal> categoryTotals,
                                    List<RecordResponse> recentTransactions) {
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
        this.netBalance = netBalance;
        this.categoryTotals = categoryTotals;
        this.recentTransactions = recentTransactions;
    }

    public BigDecimal getTotalIncome() { return totalIncome; }
    public BigDecimal getTotalExpenses() { return totalExpenses; }
    public BigDecimal getNetBalance() { return netBalance; }
    public Map<String, BigDecimal> getCategoryTotals() { return categoryTotals; }
    public List<RecordResponse> getRecentTransactions() { return recentTransactions; }
}
