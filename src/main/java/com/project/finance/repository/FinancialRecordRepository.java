package com.project.finance.repository;

import com.project.finance.model.FinancialRecord;
import com.project.finance.model.RecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {

    // Paginated listing of non-deleted records
    Page<FinancialRecord> findAllByDeletedFalse(Pageable pageable);

    // Dynamic filtering: all params are optional (null = ignored)
    @Query("SELECT r FROM FinancialRecord r WHERE r.deleted = false " +
           "AND (:startDate IS NULL OR r.date >= :startDate) " +
           "AND (:endDate IS NULL OR r.date <= :endDate) " +
           "AND (:category IS NULL OR r.category = :category) " +
           "AND (:type IS NULL OR r.type = :type)")
    Page<FinancialRecord> findWithFilters(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("category") String category,
            @Param("type") RecordType type,
            Pageable pageable);

    // Total income sum (non-deleted)
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM FinancialRecord r " +
           "WHERE r.deleted = false AND r.type = 'INCOME'")
    BigDecimal sumTotalIncome();

    // Total expense sum (non-deleted)
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM FinancialRecord r " +
           "WHERE r.deleted = false AND r.type = 'EXPENSE'")
    BigDecimal sumTotalExpenses();

    // Category-wise totals (non-deleted)
    @Query("SELECT r.category, SUM(r.amount) FROM FinancialRecord r " +
           "WHERE r.deleted = false GROUP BY r.category")
    List<Object[]> sumByCategory();

    // Monthly trends for a given year (non-deleted)
    @Query("SELECT YEAR(r.date), MONTH(r.date), r.type, SUM(r.amount) " +
           "FROM FinancialRecord r " +
           "WHERE r.deleted = false AND YEAR(r.date) = :year " +
           "GROUP BY YEAR(r.date), MONTH(r.date), r.type " +
           "ORDER BY MONTH(r.date)")
    List<Object[]> findMonthlyTrends(@Param("year") int year);

    // Top 5 most recent non-deleted records
    List<FinancialRecord> findTop5ByDeletedFalseOrderByDateDesc();
}
