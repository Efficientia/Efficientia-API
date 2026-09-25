package com.example.efficientia.cadastrobase.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum NivelAcessoSistema {
    administrativo,
    auditoria,
    conducao;

    @JsonCreator
    public static NivelAcessoSistema from(String value) {
        if (value == null) {
            return null;
        }
        return NivelAcessoSistema.valueOf(value.trim().toLowerCase(Locale.ROOT));
    }

    @JsonValue
    public String value() {
        return name();
    }
}
