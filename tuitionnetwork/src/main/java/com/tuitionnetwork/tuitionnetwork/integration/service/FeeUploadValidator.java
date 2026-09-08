package com.tuitionnetwork.tuitionnetwork.integration.service;

import com.tuitionnetwork.tuitionnetwork.integration.domain.FeeType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Component
public class FeeUploadValidator {

    private static final DateTimeFormatter COLLECTION_PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    public record ValidationOutcome(
            List<FeeUploadRow> acceptedRows, List<FeeUploadResult.RowError> errors, int totalRows) {
    }

    public ValidationOutcome validate(InputStream csv) {
        List<FeeUploadRow> candidates = new ArrayList<>();
        List<FeeUploadResult.RowError> errors = new ArrayList<>();
        int totalRows = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreHeaderCase(true)
                .build();

        try (CSVParser parser = CSVParser.parse(new InputStreamReader(csv, StandardCharsets.UTF_8), format)) {
            int rowNumber = 0;
            for (CSVRecord record : parser) {
                rowNumber++;
                totalRows++;
                validateRow(rowNumber, record, candidates, errors);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read upload file", e);
        }

        rejectDuplicates(candidates, errors);

        List<FeeUploadRow> accepted = candidates.stream()
                .filter(row -> errors.stream().noneMatch(e -> e.rowNumber() == row.rowNumber()))
                .toList();

        return new ValidationOutcome(accepted, errors, totalRows);
    }

    private void validateRow(int rowNumber, CSVRecord record, List<FeeUploadRow> candidates,
                              List<FeeUploadResult.RowError> errors) {
        String nationalId = safeGet(record, "national_id");
        String feeType = safeGet(record, "fee_type");
        String amountRaw = safeGet(record, "amount");
        String currency = safeGet(record, "currency");
        String collectionPeriod = safeGet(record, "collection_period");
        String parentNationalId = safeGet(record, "parent_national_id");
        String parentFullName = safeGet(record, "parent_full_name");

        if (isBlank(nationalId) || isBlank(feeType) || isBlank(amountRaw) || isBlank(currency) || isBlank(collectionPeriod)
                || isBlank(parentNationalId) || isBlank(parentFullName)) {
            errors.add(new FeeUploadResult.RowError(rowNumber, "Missing one or more required fields"));
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountRaw.trim());
        } catch (NumberFormatException e) {
            errors.add(new FeeUploadResult.RowError(rowNumber, "Invalid amount: " + amountRaw));
            return;
        }
        if (amount.signum() <= 0) {
            errors.add(new FeeUploadResult.RowError(rowNumber, "Amount must be greater than zero"));
            return;
        }

        if (!isKnownFeeType(feeType)) {
            errors.add(new FeeUploadResult.RowError(rowNumber, "Unknown fee type: " + feeType));
            return;
        }

        YearMonth period;
        try {
            period = YearMonth.parse(collectionPeriod.trim(), COLLECTION_PERIOD_FORMAT);
        } catch (DateTimeException e) {
            errors.add(new FeeUploadResult.RowError(rowNumber, "Invalid collection period (expected YYYY-MM): " + collectionPeriod));
            return;
        }
        if (period.isBefore(YearMonth.now())) {
            errors.add(new FeeUploadResult.RowError(rowNumber, "Collection period already in the past: " + collectionPeriod));
            return;
        }

        candidates.add(new FeeUploadRow(rowNumber, nationalId.trim(), feeType.trim().toUpperCase(),
                amount, currency.trim().toUpperCase(), collectionPeriod.trim(),
                parentNationalId.trim(), parentFullName.trim()));
    }

    private void rejectDuplicates(List<FeeUploadRow> candidates, List<FeeUploadResult.RowError> errors) {
        Set<String> seen = new HashSet<>();
        for (FeeUploadRow row : candidates) {
            String key = row.nationalId() + "|" + row.feeType() + "|" + row.collectionPeriod();
            if (!seen.add(key)) {
                errors.add(new FeeUploadResult.RowError(row.rowNumber(),
                        "Duplicate national ID + fee type + collection period in this submission"));
            }
        }
    }

    private boolean isKnownFeeType(String feeType) {
        try {
            FeeType.valueOf(feeType.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String safeGet(CSVRecord record, String column) {
        return record.isMapped(column) ? record.get(column) : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
