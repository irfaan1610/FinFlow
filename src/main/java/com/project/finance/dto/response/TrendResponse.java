package com.project.finance.dto.response;

import java.math.BigDecimal;

public class TrendResponse {

    private int year;
    private int month;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;

    public TrendResponse(int year, int month, BigDecimal totalIncome, BigDecimal totalExpenses) {
        this.year = year;
        this.month = month;
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
    }

    public int getYear() { return year; }
    public int getMonth() { return month; }
    public BigDecimal getTotalIncome() { return totalIncome; }
    public BigDecimal getTotalExpenses() { return totalExpenses; }
}
