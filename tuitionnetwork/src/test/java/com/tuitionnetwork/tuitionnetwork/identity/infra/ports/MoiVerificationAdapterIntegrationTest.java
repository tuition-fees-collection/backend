package com.tuitionnetwork.tuitionnetwork.identity.infra.ports;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("integration")
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
class MoiVerificationAdapterIntegrationTest {

    @Autowired
    private MoiVerificationAdapter adapter;

    @Test
    void activeNationalIdReturnsHolderAndMatchingName() {
        var result = adapter.verify(new MoiVerificationPort.VerificationCommand(
                "29805150101023", "Mona Samir Abdelrahman", "EPP_ENROLMENT"));

        assertThat(result.valid()).isTrue();
        assertThat(result.recordStatus()).isEqualTo("ACTIVE");
        assertThat(result.holder()).isNotNull();
        assertThat(result.holder().fullNameEnglish()).isEqualTo("Mona Samir Abdelrahman");
        assertThat(result.nameMatch()).isNotNull();
        assertThat(result.nameMatch().matched()).isTrue();
        assertThat(result.verificationId()).isNotBlank();
    }

    @Test
    void verificationCanBeFetchedByItsIdentifier() {
        var created = adapter.verify(new MoiVerificationPort.VerificationCommand(
                "30103222103442", "Nour Khaled Fahmy", "ACCOUNT_OPENING"));

        var fetched = adapter.findByVerificationId(created.verificationId());

        assertThat(fetched.verificationId()).isEqualTo(created.verificationId());
        assertThat(fetched.nationalId()).isEqualTo("30103222103442");
        assertThat(fetched.recordStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void notFoundNationalIdIsAValidBusinessResponse() {
        var result = adapter.verify(new MoiVerificationPort.VerificationCommand(
                "29805150100003", "Unknown Person", "ACCOUNT_OPENING"));

        assertThat(result.valid()).isFalse();
        assertThat(result.recordStatus()).isEqualTo("NOT_FOUND");
        assertThat(result.reasons()).isNotEmpty();
    }

    @Test
    void deceasedNationalIdIsAValidBusinessResponse() {
        var result = adapter.verify(new MoiVerificationPort.VerificationCommand(
                "29805150188883", "Unknown Person", "ACCOUNT_OPENING"));

        assertThat(result.valid()).isFalse();
        assertThat(result.recordStatus()).isEqualTo("DECEASED");
    }

    @Test
    void blockedNationalIdIsAValidBusinessResponse() {
        var result = adapter.verify(new MoiVerificationPort.VerificationCommand(
                "29805150199993", "Unknown Person", "ACCOUNT_OPENING"));

        assertThat(result.valid()).isFalse();
        assertThat(result.recordStatus()).isEqualTo("BLOCKED");
    }

    @Test
    void malformedNationalIdIsRejectedBeforeCallingTheMock() {
        assertThatThrownBy(() -> new MoiVerificationPort.VerificationCommand(
                "123", "Invalid", "ACCOUNT_OPENING"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("14 digits");
    }

    @Test
    void semanticallyInvalidNationalIdIsMappedToStableMoiException() {
        assertThatThrownBy(() -> adapter.verify(new MoiVerificationPort.VerificationCommand(
                "11111111111111", "Invalid", "ACCOUNT_OPENING")))
                .isInstanceOf(MoiVerificationException.class)
                .satisfies(error -> {
                    MoiVerificationException exception = (MoiVerificationException) error;
                    assertThat(exception.code()).isEqualTo("INVALID_NATIONAL_ID");
                    assertThat(exception.details()).containsEntry("national_id", "11111111111111");
                });
    }

    @Test
    void unknownVerificationIdIsMappedToStableMoiException() {
        assertThatThrownBy(() -> adapter.findByVerificationId("ver_missing"))
                .isInstanceOf(MoiVerificationException.class)
                .satisfies(error -> assertThat(((MoiVerificationException) error).code()).isNotBlank());
    }
}
