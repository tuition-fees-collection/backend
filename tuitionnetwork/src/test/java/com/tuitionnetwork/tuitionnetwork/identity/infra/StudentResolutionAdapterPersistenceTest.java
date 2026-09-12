package com.tuitionnetwork.tuitionnetwork.identity.infra;

import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import com.tuitionnetwork.tuitionnetwork.identity.service.NationalIdHasher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties =
        "security.national-id.hmac-key-base64=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUY=")
@Transactional
class StudentResolutionAdapterPersistenceTest {

    @Autowired
    private StudentResolutionAdapter adapter;

    @Autowired
    private NationalIdHasher nationalIdHasher;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void resolvesStudentFromPersistedNationalIdHmac() {
        String nationalId = "29805150101023";
        UUID expectedStudentId = UUID.randomUUID();
        String nationalIdHmac = nationalIdHasher.hash(new NationalId(nationalId));
        jdbcClient.sql("""
                        INSERT INTO identity.student (id, national_id_hmac, full_name)
                        VALUES (:id, :nationalIdHmac, :fullName)
                        """)
                .param("id", expectedStudentId)
                .param("nationalIdHmac", nationalIdHmac)
                .param("fullName", "Resolution Test Student")
                .update();

        assertThat(adapter.resolveRequiredStudentId(nationalId)).isEqualTo(expectedStudentId);
    }
}
