package com.example.efficientia.cadastrobase.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum TipoUsuario {
    motorista,
    manobrista,
    analista,
    pecuarista,
    curraleiro;

    @JsonCreator
    public static TipoUsuario from(String value) {
        if (value == null) {
            return null;
        }
        return TipoUsuario.valueOf(value.trim().toLowerCase(Locale.ROOT));
    }

    @JsonValue
    public String value() {
        return name();
    }
}
