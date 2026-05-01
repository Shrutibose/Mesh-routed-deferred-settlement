package com.meshrouteddeferredsettlement.upi.model;

import lombok.*;
import java.math.BigDecimal;

/**
 * The actual payment instruction. After the server decrypts MeshPacket.ciphertext,
 * it gets one of these.
 * Critical fields for security:
 *   - nonce: unique per payment (prevents duplicate/replay)
 *   - signedAt: helps reject old/replayed requests
 *   - pinHash: simulated UPI PIN verification
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInstruction {

    private String senderVpa;
    private String receiverVpa;
    private BigDecimal amount;
    private String pinHash;
    private String nonce;     // UUID, unique per payment
    private Long signedAt;    // epoch millis
}