package com.project.finance.dto.response;

import com.project.finance.model.FinancialRecord;
import com.project.finance.model.RecordType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RecordResponse {

    private Long id;
    private BigDecimal amount;
    private RecordType type;
    private String category;
    private LocalDate date;
    private String description;
    private Long createdById;
    private LocalDateTime createdAt;

    public static RecordResponse from(FinancialRecord record) {
        RecordResponse r = new RecordResponse();
        r.id = record.getId();
        r.amount = record.getAmount();
        r.type = record.getType();
        r.category = record.getCategory();
        r.date = record.getDate();
        r.description = record.getDescription();
        r.createdById = record.getCreatedBy().getId();
        r.createdAt = record.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public RecordType getType() { return type; }
    public String getCategory() { return category; }
    public LocalDate getDate() { return date; }
    public String getDescription() { return description; }
    public Long getCreatedById() { return createdById; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
