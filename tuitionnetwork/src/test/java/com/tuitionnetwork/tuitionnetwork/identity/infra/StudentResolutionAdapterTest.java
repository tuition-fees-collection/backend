package com.tuitionnetwork.tuitionnetwork.identity.infra;

import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.StudentIdentityLookupRepository;
import com.tuitionnetwork.tuitionnetwork.identity.service.NationalIdHasher;
import com.tuitionnetwork.tuitionnetwork.identity.service.StudentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentResolutionAdapterTest {

    @Mock
    private NationalIdHasher nationalIdHasher;

    @Mock
    private StudentIdentityLookupRepository studentIdentityLookupRepository;

    private StudentResolutionAdapter adapter;

    @BeforeEach
    void setUp() {
        var beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("nationalIdHasher", nationalIdHasher);
        adapter = new StudentResolutionAdapter(
                beanFactory.getBeanProvider(NationalIdHasher.class),
                studentIdentityLookupRepository);
    }

    @Test
    void hashesNationalIdAndReturnsMatchingStudentId() {
        String nationalId = "29805150101023";
        String nationalIdHmac = "a".repeat(64);
        UUID studentId = UUID.randomUUID();
        when(nationalIdHasher.hash(new NationalId(nationalId))).thenReturn(nationalIdHmac);
        when(studentIdentityLookupRepository.findStudentIdByNationalIdHmac(nationalIdHmac))
                .thenReturn(Optional.of(studentId));

        assertThat(adapter.isAvailable()).isTrue();
        assertThat(adapter.resolveRequiredStudentId(nationalId)).isEqualTo(studentId);

        verify(nationalIdHasher).hash(new NationalId(nationalId));
        verify(studentIdentityLookupRepository).findStudentIdByNationalIdHmac(nationalIdHmac);
    }

    @Test
    void rejectsMalformedNationalIdBeforeHashingOrQuerying() {
        assertThatThrownBy(() -> adapter.resolveRequiredStudentId("123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("14 digits");

        verifyNoInteractions(nationalIdHasher, studentIdentityLookupRepository);
    }

    @Test
    void missingStudentReturnsStableExceptionWithoutExposingNationalId() {
        String nationalId = "29805150101023";
        String nationalIdHmac = "b".repeat(64);
        when(nationalIdHasher.hash(new NationalId(nationalId))).thenReturn(nationalIdHmac);
        when(studentIdentityLookupRepository.findStudentIdByNationalIdHmac(nationalIdHmac))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.resolveRequiredStudentId(nationalId))
                .isInstanceOf(StudentNotFoundException.class)
                .hasMessageNotContaining(nationalId);
    }

    @Test
    void reportsUnavailableWhenIdentityHasherHasNotBeenConfigured() {
        var emptyBeanFactory = new StaticListableBeanFactory();
        var unconfiguredAdapter = new StudentResolutionAdapter(
                emptyBeanFactory.getBeanProvider(NationalIdHasher.class),
                studentIdentityLookupRepository);

        assertThat(unconfiguredAdapter.isAvailable()).isFalse();
        assertThatThrownBy(() -> unconfiguredAdapter.resolveRequiredStudentId("29805150101023"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
        verifyNoInteractions(studentIdentityLookupRepository);
    }
}
