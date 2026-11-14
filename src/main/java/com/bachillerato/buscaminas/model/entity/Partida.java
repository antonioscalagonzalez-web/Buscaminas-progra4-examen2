package com.bachillerato.buscaminas.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "partida")
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 20)
    private String nivel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('EN_CURSO','TERMINADA','ABANDONADA')")
    private EstadoPartida estado = EstadoPartida.EN_CURSO;

    @Column(name = "tiempo_ms")
    private Integer tiempoMs;

    @Column(nullable = false)
    private Short movimientos = 0;

    @Column(nullable = false)
    private Byte estrellas = 0;

    @Column(nullable = false, columnDefinition = "json")
    private String tablero;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    public Partida() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }

    public EstadoPartida getEstado() { return estado; }
    public void setEstado(EstadoPartida estado) { this.estado = estado; }

    public Integer getTiempoMs() { return tiempoMs; }
    public void setTiempoMs(Integer tiempoMs) { this.tiempoMs = tiempoMs; }

    public Short getMovimientos() { return movimientos; }
    public void setMovimientos(Short movimientos) { this.movimientos = movimientos; }

    public Byte getEstrellas() { return estrellas; }
    public void setEstrellas(Byte estrellas) { this.estrellas = estrellas; }

    public String getTablero() { return tablero; }
    public void setTablero(String tablero) { this.tablero = tablero; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }
}