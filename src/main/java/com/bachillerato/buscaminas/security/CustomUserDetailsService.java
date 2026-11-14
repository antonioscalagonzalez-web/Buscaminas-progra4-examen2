package com.bachillerato.buscaminas.security;

import com.bachillerato.buscaminas.model.entity.Usuario;
import com.bachillerato.buscaminas.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository repositorioUsuarios;

    public CustomUserDetailsService(UsuarioRepository repositorioUsuarios) {
        this.repositorioUsuarios = repositorioUsuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String identificador) throws UsernameNotFoundException {

        Usuario usuario = repositorioUsuarios.findByNombreUsuario(identificador)
                .orElseGet(() -> repositorioUsuarios.findByEmail(identificador)
                        .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas")));

        if (!usuario.getActivo()) {
            throw new UsernameNotFoundException("La cuenta se encuentra inactiva");
        }

        String rolParaSpring = "ROLE_" + usuario.getRol().name();

        return new User(
                usuario.getNombreUsuario(),
                usuario.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority(rolParaSpring))
        );
    }
}