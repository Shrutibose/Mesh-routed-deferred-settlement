package com.meshrouteddeferredsettlement.upi.service;

import com.meshrouteddeferredsettlement.upi.crypto.HybridCryptoService;
import com.meshrouteddeferredsettlement.upi.crypto.ServerKeyHolder;
import com.meshrouteddeferredsettlement.upi.model.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DemoService {

    private final AccountRepository accounts;
    private final HybridCryptoService crypto;
    private final ServerKeyHolder serverKey;

    @PostConstruct
    public void seedAccounts() {
        if (accounts.count() == 0) {

            accounts.save(Account.builder()
                    .vpa("alice@demo")
                    .name("Alice")
                    .balance(new BigDecimal("5000.00"))
                    .build());

            accounts.save(Account.builder()
                    .vpa("bob@demo")
                    .name("Bob")
                    .balance(new BigDecimal("1000.00"))
                    .build());

            accounts.save(Account.builder()
                    .vpa("carol@demo")
                    .name("Carol")
                    .balance(new BigDecimal("2500.00"))
                    .build());

            accounts.save(Account.builder()
                    .vpa("dave@demo")
                    .name("Dave")
                    .balance(new BigDecimal("500.00"))
                    .build());

            log.info("Seeded 4 demo accounts");
        }
    }

    public MeshPacket createPacket(String senderVpa, String receiverVpa,
                                   BigDecimal amount, String pin, int ttl) throws Exception {

        PaymentInstruction instruction = PaymentInstruction.builder()
                .senderVpa(senderVpa)
                .receiverVpa(receiverVpa)
                .amount(amount)
                .pinHash(sha256Hex(pin))
                .nonce(UUID.randomUUID().toString())
                .signedAt(Instant.now().toEpochMilli())
                .build();

        String ciphertext = crypto.encrypt(instruction, serverKey.getPublicKey());

        MeshPacket packet = new MeshPacket();
        packet.setPacketId(UUID.randomUUID().toString());
        packet.setTtl(ttl);
        packet.setCreatedAt(Instant.now().toEpochMilli());
        packet.setCiphertext(ciphertext);

        return packet;
    }

    private String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes());

        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }

        return hex.toString();
    }
}