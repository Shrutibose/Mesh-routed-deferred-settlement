package com.meshrouteddeferredsettlement.upi.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meshrouteddeferredsettlement.upi.model.PaymentInstruction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class HybridCryptoService {

    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_KEY_BITS = 256;
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int RSA_KEY_SIZE_BYTES = 256;

    private final SecureRandom rng = new SecureRandom();
    private final ObjectMapper json = new ObjectMapper();

    private final ServerKeyHolder serverKey;

    public String encrypt(PaymentInstruction instruction, PublicKey serverPublicKey) throws Exception {

        byte[] plaintext = json.writeValueAsBytes(instruction);

        // 1. AES key
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(AES_KEY_BITS);
        SecretKey aesKey = kg.generateKey();

        // 2. AES-GCM encrypt
        byte[] iv = new byte[GCM_IV_BYTES];
        rng.nextBytes(iv);

        Cipher aes = Cipher.getInstance(AES_TRANSFORMATION);
        aes.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] aesCiphertext = aes.doFinal(plaintext);

        // 3. RSA encrypt AES key
        Cipher rsa = Cipher.getInstance(RSA_TRANSFORMATION);
        OAEPParameterSpec oaep = new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
        );

        rsa.init(Cipher.ENCRYPT_MODE, serverPublicKey, oaep);
        byte[] encryptedAesKey = rsa.doFinal(aesKey.getEncoded());

        // 4. Pack everything
        ByteBuffer buffer = ByteBuffer.allocate(
                encryptedAesKey.length + iv.length + aesCiphertext.length
        );

        buffer.put(encryptedAesKey);
        buffer.put(iv);
        buffer.put(aesCiphertext);

        return Base64.getEncoder().encodeToString(buffer.array());
    }

    public PaymentInstruction decrypt(String base64Ciphertext) throws Exception {

        byte[] all = Base64.getDecoder().decode(base64Ciphertext);

        if (all.length < RSA_KEY_SIZE_BYTES + GCM_IV_BYTES) {
            throw new IllegalArgumentException("Ciphertext too short");
        }

        byte[] encryptedAesKey = new byte[RSA_KEY_SIZE_BYTES];
        byte[] iv = new byte[GCM_IV_BYTES];
        byte[] aesCiphertext = new byte[all.length - RSA_KEY_SIZE_BYTES - GCM_IV_BYTES];

        ByteBuffer buffer = ByteBuffer.wrap(all);
        buffer.get(encryptedAesKey);
        buffer.get(iv);
        buffer.get(aesCiphertext);

        // 1. RSA decrypt AES key
        Cipher rsa = Cipher.getInstance(RSA_TRANSFORMATION);
        OAEPParameterSpec oaep = new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
        );

        rsa.init(Cipher.DECRYPT_MODE, serverKey.getPrivateKey(), oaep);
        byte[] aesKeyBytes = rsa.doFinal(encryptedAesKey);

        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

        // 2. AES decrypt
        Cipher aes = Cipher.getInstance(AES_TRANSFORMATION);
        aes.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] plaintext = aes.doFinal(aesCiphertext);

        return json.readValue(plaintext, PaymentInstruction.class);
    }

    public String hashCiphertext(String base64Ciphertext) throws Exception {

        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha256.digest(base64Ciphertext.getBytes());

        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }

        return hex.toString();
    }
}