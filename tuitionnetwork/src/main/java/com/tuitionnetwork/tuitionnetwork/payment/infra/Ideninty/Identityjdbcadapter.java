package com.tuitionnetwork.tuitionnetwork.payment.infra.Ideninty;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.IdentityPort;
@Component
public class Identityjdbcadapter implements IdentityPort {

    private static final String LINK_EXISTS = """
            SELECT EXISTS (
                SELECT 1 FROM identity.guardian_student
                WHERE guardian_id = ? AND student_id = ?
            )
            """;

    private final JdbcTemplate jdbc;

    public Identityjdbcadapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    @Override
    @Transactional(readOnly = true)
    public boolean canAccessStudent(UUID guardianId, UUID studentId) {
        if (guardianId == null || studentId == null) {
            return false;
        }
        return Boolean.TRUE.equals(
                jdbc.queryForObject(LINK_EXISTS, Boolean.class, guardianId, studentId));
    }
}


