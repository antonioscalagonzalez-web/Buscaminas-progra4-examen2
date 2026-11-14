package com.bachillerato.buscaminas.model.dto;

public record UsuarioResponse(
        Long id,
        String nombre,
        String nombreUsuario,
        String email,
        String rol,
        Boolean activo
) {}