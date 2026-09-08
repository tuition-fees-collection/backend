package com.tuitionnetwork.tuitionnetwork.integration.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class FeeUploadValidatorTest {

    private final FeeUploadValidator validator = new FeeUploadValidator();

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private String futurePeriod() {
        return YearMonth.now().plusMonths(1).format(PERIOD_FORMAT);
    }

    private String pastPeriod() {
        return YearMonth.now().minusMonths(1).format(PERIOD_FORMAT);
    }

    private InputStream csv(String... lines) {
        String content = "national_id,fee_type,amount,currency,collection_period,parent_national_id,parent_full_name\n"
                + String.join("\n", lines);
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private String row(String nationalId, String feeType, String amount, String currency, String period) {
        return nationalId + "," + feeType + "," + amount + "," + currency + "," + period
                + ",19001112223344,Ahmed Mohamed";
    }

    @Test
    void acceptsAWellFormedRow() {
        var outcome = validator.validate(csv(row("29901234567890", "TUITION", "5000", "EGP", futurePeriod())));

        assertThat(outcome.acceptedRows()).hasSize(1);
        assertThat(outcome.errors()).isEmpty();
        assertThat(outcome.acceptedRows().get(0).amount()).isEqualByComparingTo("5000");
        assertThat(outcome.acceptedRows().get(0).parentNationalId()).isEqualTo("19001112223344");
        assertThat(outcome.acceptedRows().get(0).parentFullName()).isEqualTo("Ahmed Mohamed");
    }

    @Test
    void rejectsRowWithMissingField() {
        var outcome = validator.validate(csv(row("29901234567890", "TUITION", "5000", "", futurePeriod())));

        assertThat(outcome.acceptedRows()).isEmpty();
        assertThat(outcome.errors()).hasSize(1);
        assertThat(outcome.errors().get(0).reason()).contains("Missing");
    }

    @Test
    void rejectsRowWithMissingParentNationalId() {
        String period = futurePeriod();
        var outcome = validator.validate(csv(
                "29901234567890,TUITION,5000,EGP," + period + ",,Ahmed Mohamed"));

        assertThat(outcome.acceptedRows()).isEmpty();
        assertThat(outcome.errors()).hasSize(1);
        assertThat(outcome.errors().get(0).reason()).contains("Missing");
    }

    @Test
    void rejectsRowWithMissingParentFullName() {
        String period = futurePeriod();
        var outcome = validator.validate(csv(
                "29901234567890,TUITION,5000,EGP," + period + ",19001112223344,"));

        assertThat(outcome.acceptedRows()).isEmpty();
        assertThat(outcome.errors()).hasSize(1);
        assertThat(outcome.errors().get(0).reason()).contains("Missing");
    }

    @Test
    void rejectsAmountLessThanOrEqualToZero() {
        var outcome = validator.validate(csv(row("29901234567890", "TUITION", "0", "EGP", futurePeriod())));

        assertThat(outcome.errors()).hasSize(1);
        assertThat(outcome.errors().get(0).reason()).contains("greater than zero");
    }

    @Test
    void rejectsUnknownFeeType() {
        var outcome = validator.validate(csv(row("29901234567890", "SWIMMING", "500", "EGP", futurePeriod())));

        assertThat(outcome.errors()).hasSize(1);
        assertThat(outcome.errors().get(0).reason()).contains("Unknown fee type");
    }

    @Test
    void rejectsCollectionPeriodInThePast() {
        var outcome = validator.validate(csv(row("29901234567890", "TUITION", "500", "EGP", pastPeriod())));

        assertThat(outcome.errors()).hasSize(1);
        assertThat(outcome.errors().get(0).reason()).contains("already in the past");
    }

    @Test
    void rejectsDuplicateNationalIdFeeTypeAndPeriodKeepingTheFirstOccurrence() {
        String period = futurePeriod();
        var outcome = validator.validate(csv(
                row("29901234567890", "TUITION", "5000", "EGP", period),
                row("29901234567890", "TUITION", "5000", "EGP", period)));

        assertThat(outcome.acceptedRows()).hasSize(1);
        assertThat(outcome.acceptedRows().get(0).rowNumber()).isEqualTo(1);
        assertThat(outcome.errors()).hasSize(1);
        assertThat(outcome.errors().get(0).rowNumber()).isEqualTo(2);
        assertThat(outcome.errors().get(0).reason()).contains("Duplicate");
    }

    @Test
    void aBadRowDoesNotStopTheRestOfTheFileFromBeingProcessed() {
        String period = futurePeriod();
        var outcome = validator.validate(csv(
                row("29901234567890", "TUITION", "5000", "EGP", period),
                row("30001234567890", "SWIMMING", "500", "EGP", period),
                row("30101234567890", "ACTIVITIES", "750", "EGP", period)));

        assertThat(outcome.totalRows()).isEqualTo(3);
        assertThat(outcome.acceptedRows()).hasSize(2);
        assertThat(outcome.errors()).hasSize(1);
        assertThat(outcome.errors().get(0).rowNumber()).isEqualTo(2);
    }
}
