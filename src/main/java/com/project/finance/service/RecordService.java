package com.project.finance.service;

import com.project.finance.dto.request.RecordRequest;
import com.project.finance.dto.response.RecordResponse;
import com.project.finance.exception.ResourceNotFoundException;
import com.project.finance.model.FinancialRecord;
import com.project.finance.model.RecordType;
import com.project.finance.model.User;
import com.project.finance.repository.FinancialRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class RecordService {

    private final FinancialRecordRepository recordRepository;

    public RecordService(FinancialRecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    public RecordResponse createRecord(RecordRequest request, User createdBy) {
        FinancialRecord record = new FinancialRecord();
        record.setAmount(request.getAmount());
        record.setType(request.getType());
        record.setCategory(request.getCategory());
        record.setDate(request.getDate());
        record.setDescription(request.getDescription());
        record.setCreatedBy(createdBy);
        return RecordResponse.from(recordRepository.save(record));
    }

    public Page<RecordResponse> getRecords(Pageable pageable,
                                           LocalDate startDate,
                                           LocalDate endDate,
                                           String category,
                                           RecordType type) {
        return recordRepository
                .findWithFilters(startDate, endDate, category, type, pageable)
                .map(RecordResponse::from);
    }

    public RecordResponse updateRecord(Long id, RecordRequest request) {
        FinancialRecord record = recordRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with id: " + id));

        record.setAmount(request.getAmount());
        record.setType(request.getType());
        record.setCategory(request.getCategory());
        record.setDate(request.getDate());
        record.setDescription(request.getDescription());

        return RecordResponse.from(recordRepository.save(record));
    }

    public void deleteRecord(Long id) {
        FinancialRecord record = recordRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with id: " + id));

        record.setDeleted(true);
        recordRepository.save(record);
    }
}
