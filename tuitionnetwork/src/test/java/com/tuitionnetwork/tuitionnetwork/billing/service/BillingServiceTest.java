package com.tuitionnetwork.tuitionnetwork.billing.service;

import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

class BillingServiceTest {

    @Mock
    private FeeService feeService;

    private BillingService billingService;
    private UUID feeId;
    private UUID studentId;
    private UUID institutionId;
    private Fee fee;

    @BeforeEach
    void settUp() {
        billingService = new BillingService(feeService);

        feeId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        institutionId = UUID.randomUUID();

        fee = new Fee(
            studentId,
            institutionId,
            FeeType.TUITION,
            "2026-2027",
            "EGP",
            new BigDecimal("10000.00"),
            null
        );
    }

    @Test
    /* get payable fee should return fee from fee service */
    void testGetPayableFee_Success() {

        when(feeService.getPayableFee(feeId))
            .thenReturn(fee);

        Fee result = billingService.getPayableFee(feeId);

        assertNotNull(result);
        assertEquals(fee, result);

        verify(feeService).getPayableFee(feeId);
    }

    @Test
    /* get outstanding amount should return amount from fee service */
    void testGetOutstandingAmount_Success() {

        BigDecimal outstandingAmount =
            new BigDecimal("7000.00");

        when(feeService.getOutstandingAmount(feeId))
            .thenReturn(outstandingAmount);

        BigDecimal result =
            billingService.getOutstandingAmount(feeId);

        assertEquals(outstandingAmount, result);

        verify(feeService).getOutstandingAmount(feeId);
    }

    @Test
    /* settle should delegate fee settlement to fee service */
    void testSettle_Success() {

        BigDecimal paymentAmount =
            new BigDecimal("3000.00");

        when(feeService.settle(feeId, paymentAmount))
            .thenReturn(fee);

        Fee result =
            billingService.settle(
                feeId,
                paymentAmount
            );

        assertNotNull(result);
        assertEquals(fee, result);

        verify(feeService).settle(
            feeId,
            paymentAmount
        );
    }

    @Test
    /* find all fees by ids should return fees from fee service */
    void testFindAllById_Success() {

        UUID secondFeeId = UUID.randomUUID();

        Fee secondFee = new Fee(
            UUID.randomUUID(),
            institutionId,
            FeeType.ACTIVITIES,
            "2026-2027",
            "EGP",
            new BigDecimal("2000.00"),
            null
        );

        List<UUID> feeIds =
            List.of(feeId, secondFeeId);

        List<Fee> fees =
            List.of(fee, secondFee);

        when(feeService.findAllById(feeIds))
            .thenReturn(fees);

        List<Fee> result =
            billingService.findAllById(feeIds);

        assertEquals(2, result.size());
        assertEquals(fees, result);

        verify(feeService).findAllById(feeIds);
    }
}