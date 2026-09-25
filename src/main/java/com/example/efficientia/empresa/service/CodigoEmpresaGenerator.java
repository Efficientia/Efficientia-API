package com.example.efficientia.empresa.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;

@Component
public class CodigoEmpresaGenerator {

    private static final String LETRAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final SecureRandom random = new SecureRandom();

    public String gerarCodigo(String nomeEmpresa) {
        String prefixo = extrairPrefixo(nomeEmpresa);
        int numero = random.nextInt(100000);
        String sufixo = String.format("%05d", numero);
        return prefixo + sufixo;
    }

    private String extrairPrefixo(String nomeEmpresa) {
        if (nomeEmpresa == null) {
            return gerarLetrasAleatorias(3);
        }

        String normalizado = Normalizer.normalize(nomeEmpresa, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String apenasLetras = normalizado.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);

        if (apenasLetras.length() >= 3) {
            return apenasLetras.substring(0, 3);
        }

        StringBuilder sb = new StringBuilder(apenasLetras);
        while (sb.length() < 3) {
            sb.append(LETRAS.charAt(random.nextInt(LETRAS.length())));
        }
        return sb.toString();
    }

    private String gerarLetrasAleatorias(int quantidade) {
        StringBuilder sb = new StringBuilder(quantidade);
        for (int i = 0; i < quantidade; i++) {
            sb.append(LETRAS.charAt(random.nextInt(LETRAS.length())));
        }
        return sb.toString();
    }
}
