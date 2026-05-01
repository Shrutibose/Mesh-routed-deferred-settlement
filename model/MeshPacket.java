package com.meshrouteddeferredsettlement.upi.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.*;

/**
 * The over-the-wire format. This is what hops from phone to phone via Bluetooth.
 *
 * The intermediate phones can read the OUTER fields (packetId, ttl, createdAt)
 * because they need them for routing and dedup. They CANNOT read `ciphertext` —
 * that's encrypted with the server's public key.
 *
 * NOTE on outer-field tampering:
 *   A malicious intermediate could change `packetId` or `createdAt`. That's why
 *   we use the ciphertext's hash (not packetId) as the idempotency key on the
 *   server. The ciphertext is authenticated by hybrid encryption, so any
 *   tampering inside the encrypted blob is detected on decryption.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class
MeshPacket {

    @NotBlank
    private String packetId; // UUID for dedup between devices

    @Min(0)
    private int ttl; // hops remaining

    @NotNull
    private Long createdAt; // epoch millis

    @NotBlank
    private String ciphertext; // encrypted payload
}