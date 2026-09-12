package com.tuitionnetwork.tuitionnetwork.integration.service;

import com.tuitionnetwork.tuitionnetwork.integration.domain.FeeType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class FeeUploadValidator {

    public static final int MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
    public static final int MAX_ROW_COUNT = 10_000;

    private static final DateTimeFormatter COLLECTION_PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final Set<String> ALLOWED_CURRENCIES = Set.of("EGP");
    private static final List<String> REQUIRED_HEADERS = List.of(
            "national_id",
            "fee_type",
            "amount",
            "currency",
            "collection_period",
            "parent_national_id",
            "parent_full_name");

    public record ValidationOutcome(
            List<FeeUploadRow> acceptedRows,
            List<FeeUploadResult.RowError> errors,
            int totalRows,
            String fileChecksum) {
    }

    private record UploadPayload(byte[] content, String checksum) {
    }

    public ValidationOutcome validate(InputStream csv) {
        UploadPayload payload = readAndFingerprint(csv);
        List<FeeUploadRow> candidates = new ArrayList<>();
        List<FeeUploadResult.RowError> errors = new ArrayList<>();
        int totalRows = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreHeaderCase(true)
                .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW)
                .build();

        String content = new String(payload.content(), StandardCharsets.UTF_8);
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }

        try (CSVParser parser = CSVParser.parse(content, format)) {
            validateHeaders(parser.getHeaderNames());
            for (CSVRecord record : parser) {
                totalRows++;
                if (totalRows > MAX_ROW_COUNT) {
                    throw new UploadValidationException(
                            "ROW_LIMIT_EXCEEDED",
                            "Upload contains more than " + MAX_ROW_COUNT + " data rows");
                }
                validateRow(totalRows, record, candidates, errors);
            }
        } catch (UploadValidationException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new UploadValidationException("INVALID_HEADERS", "CSV contains invalid or duplicate headers", e);
        } catch (IOException e) {
            throw new UploadValidationException("INVALID_CSV", "Could not parse upload file", e);
        }

        if (totalRows == 0) {
            throw new UploadValidationException("NO_DATA_ROWS", "Upload must contain at least one data row");
        }

        rejectDuplicates(candidates, errors);

        List<FeeUploadRow> accepted = candidates.stream()
                .filter(row -> errors.stream().noneMatch(e -> e.rowNumber() == row.rowNumber()))
                .toList();

        return new ValidationOutcome(accepted, List.copyOf(errors), totalRows, payload.checksum());
    }

    private UploadPayload readAndFingerprint(InputStream csv) {
        if (csv == null) {
            throw new UploadValidationException("EMPTY_FILE", "Upload file is required");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int totalBytes = 0;
            int bytesRead;

            while ((bytesRead = csv.read(buffer)) != -1) {
                totalBytes += bytesRead;
                if (totalBytes > MAX_FILE_SIZE_BYTES) {
                    throw new UploadValidationException(
                            "FILE_TOO_LARGE",
                            "Upload file exceeds the " + MAX_FILE_SIZE_BYTES + " byte limit");
                }
                digest.update(buffer, 0, bytesRead);
                content.write(buffer, 0, bytesRead);
            }

            return new UploadPayload(content.toByteArray(), HexFormat.of().formatHex(digest.digest()));
        } catch (IOException e) {
            throw new UploadValidationException("FILE_READ_ERROR", "Could not read upload file", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private void validateHeaders(List<String> actualHeaders) {
        List<String> normalized = actualHeaders.stream()
                .map(header -> header.trim().toLowerCase(Locale.ROOT))
                .toList();
        Set<String> actual = new LinkedHashSet<>(normalized);
        Set<String> required = new LinkedHashSet<>(REQUIRED_HEADERS);

        Set<String> missing = new LinkedHashSet<>(required);
        missing.removeAll(actual);
        Set<String> unexpected = new LinkedHashSet<>(actual);
        unexpected.removeAll(required);

        if (normalized.size() != REQUIRED_HEADERS.size() || !missing.isEmpty() || !unexpected.isEmpty()) {
            throw new UploadValidationException(
                    "INVALID_HEADERS",
                    "CSV headers must be exactly " + REQUIRED_HEADERS
                            + "; missing=" + missing + ", unexpected=" + unexpected);
        }
    }

    private void validateRow(int rowNumber, CSVRecord record, List<FeeUploadRow> candidates,
                             List<FeeUploadResult.RowError> errors) {
        String nationalId = record.get("national_id");
        String feeType = record.get("fee_type");
        String amountRaw = record.get("amount");
        String currency = record.get("currency");
        String collectionPeriod = record.get("collection_period");
        String parentNationalId = record.get("parent_national_id");
        String parentFullName = record.get("parent_full_name");
        int errorsBeforeRow = errors.size();

        require(rowNumber, "national_id", nationalId, errors);
        require(rowNumber, "fee_type", feeType, errors);
        require(rowNumber, "amount", amountRaw, errors);
        require(rowNumber, "currency", currency, errors);
        require(rowNumber, "collection_period", collectionPeriod, errors);
        require(rowNumber, "parent_national_id", parentNationalId, errors);
        require(rowNumber, "parent_full_name", parentFullName, errors);

        if (!isBlank(nationalId) && !isNationalId(nationalId)) {
            errors.add(new FeeUploadResult.RowError(
                    rowNumber, "Invalid student national ID: expected exactly 14 digits"));
        }
        if (!isBlank(parentNationalId) && !isNationalId(parentNationalId)) {
            errors.add(new FeeUploadResult.RowError(
                    rowNumber, "Invalid parent national ID: expected exactly 14 digits"));
        }

        BigDecimal amount = null;
        if (!isBlank(amountRaw)) {
            try {
                amount = new BigDecimal(amountRaw.trim());
                if (amount.signum() <= 0) {
                    errors.add(new FeeUploadResult.RowError(rowNumber, "Amount must be greater than zero"));
                }
            } catch (NumberFormatException e) {
                errors.add(new FeeUploadResult.RowError(rowNumber, "Invalid amount: " + amountRaw));
            }
        }

        if (!isBlank(feeType) && !isKnownFeeType(feeType)) {
            errors.add(new FeeUploadResult.RowError(rowNumber, "Unknown fee type: " + feeType));
        }

        if (!isBlank(currency) && !ALLOWED_CURRENCIES.contains(currency.trim().toUpperCase(Locale.ROOT))) {
            errors.add(new FeeUploadResult.RowError(
                    rowNumber, "Unsupported currency: " + currency + "; allowed=" + ALLOWED_CURRENCIES));
        }

        if (!isBlank(collectionPeriod)) {
            try {
                YearMonth period = YearMonth.parse(collectionPeriod.trim(), COLLECTION_PERIOD_FORMAT);
                if (period.isBefore(YearMonth.now())) {
                    errors.add(new FeeUploadResult.RowError(
                            rowNumber, "Collection period already in the past: " + collectionPeriod));
                }
            } catch (DateTimeException e) {
                errors.add(new FeeUploadResult.RowError(
                        rowNumber, "Invalid collection period (expected YYYY-MM): " + collectionPeriod));
            }
        }

        if (errors.size() == errorsBeforeRow) {
            candidates.add(new FeeUploadRow(
                    rowNumber,
                    nationalId.trim(),
                    feeType.trim().toUpperCase(Locale.ROOT),
                    amount,
                    currency.trim().toUpperCase(Locale.ROOT),
                    collectionPeriod.trim(),
                    parentNationalId.trim(),
                    parentFullName.trim()));
        }
    }

    private void require(int rowNumber, String field, String value, List<FeeUploadResult.RowError> errors) {
        if (isBlank(value)) {
            errors.add(new FeeUploadResult.RowError(rowNumber, "Missing required field: " + field));
        }
    }

    private void rejectDuplicates(List<FeeUploadRow> candidates, List<FeeUploadResult.RowError> errors) {
        Set<String> seen = new HashSet<>();
        for (FeeUploadRow row : candidates) {
            String key = row.nationalId() + "|" + row.feeType() + "|" + row.collectionPeriod();
            if (!seen.add(key)) {
                errors.add(new FeeUploadResult.RowError(
                        row.rowNumber(),
                        "Duplicate national ID + fee type + collection period in this submission"));
            }
        }
    }

    private boolean isNationalId(String value) {
        return value.trim().matches("\\d{14}");
    }

    private boolean isKnownFeeType(String feeType) {
        try {
            FeeType.valueOf(feeType.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
