package com.bachillerato.buscaminas.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class Pbkdf2PasswordEncoder implements PasswordEncoder {

    private static final int ITERACIONES = 65536;
    private static final int LONGITUD_LLAVE = 256;
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";

    @Override
    public String encode(CharSequence rawPassword) {
        try {
            byte[] salAleatoria = new byte[16];
            SecureRandom.getInstanceStrong().nextBytes(salAleatoria);

            byte[] hashGenerado = calcularHash(rawPassword.toString().toCharArray(), salAleatoria);

            String salBase64 = Base64.getEncoder().encodeToString(salAleatoria);
            String hashBase64 = Base64.getEncoder().encodeToString(hashGenerado);

            return salBase64 + ":" + hashBase64;
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar la contraseña", e);
        }
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null || !encodedPassword.contains(":")) {
            return false;
        }
        try {
            String[] partes = encodedPassword.split(":");
            byte[] salAlmacenada = Base64.getDecoder().decode(partes[0]);
            byte[] hashAlmacenado = Base64.getDecoder().decode(partes[1]);

            byte[] hashCalculado = calcularHash(rawPassword.toString().toCharArray(), salAlmacenada);


            return MessageDigest.isEqual(hashAlmacenado, hashCalculado);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] calcularHash(char[] password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec especificaciones = new PBEKeySpec(password, salt, ITERACIONES, LONGITUD_LLAVE);
        SecretKeyFactory fabrica = SecretKeyFactory.getInstance(ALGORITMO);
        return fabrica.generateSecret(especificaciones).getEncoded();
    }
}