package com.ordermanagement.dto.response;

import com.ordermanagement.domain.enums.PaymentMethod;
import com.ordermanagement.domain.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private UUID id;
    private UUID orderId;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private PaymentStatus status;
    private String transactionReference;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
