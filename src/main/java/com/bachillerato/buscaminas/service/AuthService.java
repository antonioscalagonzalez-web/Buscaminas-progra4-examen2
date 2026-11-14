package com.bachillerato.buscaminas.service;

import com.bachillerato.buscaminas.exception.NegocioException;
import com.bachillerato.buscaminas.model.dto.LoginRequest;
import com.bachillerato.buscaminas.model.dto.RegistroRequest;
import com.bachillerato.buscaminas.model.dto.UsuarioResponse;
import com.bachillerato.buscaminas.model.entity.Rol;
import com.bachillerato.buscaminas.model.entity.Usuario;
import com.bachillerato.buscaminas.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        String nombreUsuarioLimpio = request.nombreUsuario().trim();
        String emailLimpio = request.email().trim().toLowerCase();

        if (usuarioRepository.existsByNombreUsuario(nombreUsuarioLimpio)) {
            throw new NegocioException(HttpStatus.CONFLICT, "El nombre de usuario ya está en uso");
        }

        if (usuarioRepository.existsByEmail(emailLimpio)) {
            throw new NegocioException(HttpStatus.CONFLICT, "El correo electrónico ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setNombreUsuario(nombreUsuarioLimpio);
        usuario.setEmail(emailLimpio);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(Rol.JUGADOR);
        usuario.setActivo(true);

        Usuario guardado = usuarioRepository.save(usuario);
        return mapearResponse(guardado);
    }

    public UsuarioResponse login(LoginRequest request,
                                 HttpServletRequest httpRequest,
                                 HttpServletResponse httpResponse) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.identificador().trim(),
                            request.password()
                    )
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            new HttpSessionSecurityContextRepository().saveContext(context, httpRequest, httpResponse);

            Usuario usuario = usuarioRepository.findByNombreUsuario(auth.getName())
                    .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

            return mapearResponse(usuario);
        } catch (AuthenticationException ex) {
            throw new NegocioException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas o cuenta inactiva");
        }
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPerfil(String nombreUsuario) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return mapearResponse(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(this::mapearResponse)
                .toList();
    }

    @Transactional
    public UsuarioResponse cambiarEstadoUsuario(Long usuarioId, boolean activo) {
        Usuario usuario = usuarioRepository.findByIdForUpdate(usuarioId)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        usuario.setActivo(activo);
        return mapearResponse(usuarioRepository.save(usuario));
    }

    private UsuarioResponse mapearResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getNombreUsuario(),
                usuario.getEmail(),
                usuario.getRol().name(),
                usuario.getActivo()
        );
    }
}