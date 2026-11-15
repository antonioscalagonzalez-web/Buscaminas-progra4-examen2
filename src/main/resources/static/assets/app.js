const estado = {
    usuario: null,
    partida: null,
    timerId: null,
    segundosTranscurridos: 0
};

document.addEventListener("DOMContentLoaded", () => {
    registrarEventos();
    verificarSesion();
});

function registrarEventos() {
    document.getElementById("nav-juego").addEventListener("click", () => mostrarVista("vista-juego"));
    document.getElementById("nav-ranking").addEventListener("click", () => {
        mostrarVista("vista-ranking");
        cargarRanking();
    });
    document.getElementById("nav-historial").addEventListener("click", () => {
        mostrarVista("vista-historial");
        cargarHistorial();
    });
    document.getElementById("nav-admin").addEventListener("click", () => {
        mostrarVista("vista-admin");
        cargarUsuariosAdmin();
    });

    document.getElementById("btn-abrir-auth").addEventListener("click", () => mostrarVista("vista-auth"));
    document.getElementById("btn-cta-auth").addEventListener("click", () => mostrarVista("vista-auth"));
    document.getElementById("btn-logout").addEventListener("click", cerrarSesion);

    document.getElementById("tab-login").addEventListener("click", () => alternarTabAuth("login"));
    document.getElementById("tab-registro").addEventListener("click", () => alternarTabAuth("registro"));

    document.getElementById("form-login").addEventListener("submit", manejarLogin);
    document.getElementById("form-registro").addEventListener("submit", manejarRegistro);

    document.getElementById("btn-nueva-partida").addEventListener("click", iniciarNuevaPartida);
    document.getElementById("btn-abandonar").addEventListener("click", abandonarPartida);
    document.getElementById("filtro-ranking-nivel").addEventListener("change", cargarRanking);
}

async function apiFetch(url, opciones = {}) {
    const config = {
        headers: { "Content-Type": "application/json" },
        credentials: "same-origin",
        ...opciones
    };
    const respuesta = await fetch(url, config);
    const texto = await respuesta.text();
    const datos = texto ? JSON.parse(texto) : null;

    if (!respuesta.ok) {
        const mensaje = datos?.mensaje || datos?.error || "Error en la solicitud";
        throw new Error(mensaje);
    }
    return datos;
}

function mostrarAlerta(mensaje, tipo = "error") {
    const el = document.getElementById("alerta");
    el.textContent = mensaje;
    el.className = `alerta ${tipo}`;
    setTimeout(() => el.classList.add("hidden"), 4500);
}

function mostrarVista(idVista) {
    document.querySelectorAll(".vista, #vista-auth").forEach(v => v.classList.add("hidden"));
    document.getElementById(idVista).classList.remove("hidden");

    document.querySelectorAll(".nav-btn").forEach(b => b.classList.remove("active"));
    const mapaNav = {
        "vista-juego": "nav-juego",
        "vista-ranking": "nav-ranking",
        "vista-historial": "nav-historial",
        "vista-admin": "nav-admin"
    };
    if (mapaNav[idVista]) {
        document.getElementById(mapaNav[idVista]).classList.add("active");
    }
}

function alternarTabAuth(tab) {
    const esLogin = tab === "login";
    document.getElementById("tab-login").classList.toggle("active", esLogin);
    document.getElementById("tab-registro").classList.toggle("active", !esLogin);
    document.getElementById("form-login").classList.toggle("hidden", !esLogin);
    document.getElementById("form-registro").classList.toggle("hidden", esLogin);
}

async function verificarSesion() {
    try {
        const perfil = await apiFetch("/api/auth/me");
        establecerUsuario(perfil);
        await buscarPartidaActiva();
    } catch {
        establecerUsuario(null);
    }
}

function establecerUsuario(usuario) {
    estado.usuario = usuario;
    const autenticado = Boolean(usuario);

    document.getElementById("btn-abrir-auth").classList.toggle("hidden", autenticado);
    document.getElementById("btn-logout").classList.toggle("hidden", !autenticado);
    document.getElementById("user-badge").classList.toggle("hidden", !autenticado);
    document.getElementById("nav-historial").classList.toggle("hidden", !autenticado);
    document.getElementById("nav-admin").classList.toggle("hidden", !autenticado || usuario.rol !== "ADMIN");

    document.getElementById("invitado-banner").classList.toggle("hidden", autenticado);
    document.getElementById("panel-juego").classList.toggle("hidden", !autenticado);

    if (autenticado) {
        document.getElementById("user-badge").textContent = `@${usuario.nombreUsuario}`;
        mostrarVista("vista-juego");
    } else {
        detenerCronometro();
        estado.partida = null;
        document.getElementById("tablero").innerHTML = "";
    }
}

async function manejarLogin(e) {
    e.preventDefault();
    try {
        const payload = {
            identificador: document.getElementById("login-identificador").value,
            password: document.getElementById("login-password").value
        };
        const perfil = await apiFetch("/api/auth/login", {
            method: "POST",
            body: JSON.stringify(payload)
        });
        e.target.reset();
        establecerUsuario(perfil);
        await buscarPartidaActiva();
    } catch (err) {
        mostrarAlerta(err.message, "error");
    }
}

async function manejarRegistro(e) {
    e.preventDefault();
    try {
        const payload = {
            nombre: document.getElementById("reg-nombre").value,
            nombreUsuario: document.getElementById("reg-usuario").value,
            email: document.getElementById("reg-email").value,
            password: document.getElementById("reg-password").value
        };
        await apiFetch("/api/auth/registro", {
            method: "POST",
            body: JSON.stringify(payload)
        });
        e.target.reset();
        alternarTabAuth("login");
        mostrarAlerta("Cuenta creada con éxito. Ya puedes iniciar sesión.", "ok");
    } catch (err) {
        mostrarAlerta(err.message, "error");
    }
}

async function cerrarSesion() {
    try {
        await apiFetch("/api/auth/logout", { method: "POST" });
    } finally {
        establecerUsuario(null);
        mostrarVista("vista-juego");
    }
}

async function buscarPartidaActiva() {
    try {
        const partida = await apiFetch("/api/partidas/activa");
        actualizarPartida(partida, true);
    } catch {
        detenerCronometro();
    }
}

async function iniciarNuevaPartida() {
    try {
        const nivel = document.getElementById("select-nivel").value;
        const partida = await apiFetch("/api/partidas", {
            method: "POST",
            body: JSON.stringify({ nivel })
        });
        estado.segundosTranscurridos = 0;
        actualizarPartida(partida, true);
    } catch (err) {
        mostrarAlerta(err.message, "error");
    }
}

async function revelarCelda(fila, columna) {
    if (!estado.partida || estado.partida.estado !== "EN_CURSO") return;
    try {
        const partida = await apiFetch(`/api/partidas/${estado.partida.id}/revelar`, {
            method: "POST",
            body: JSON.stringify({ fila, columna })
        });
        actualizarPartida(partida, false);
    } catch (err) {
        mostrarAlerta(err.message, "error");
    }
}

async function alternarBandera(e, fila, columna) {
    e.preventDefault();
    if (!estado.partida || estado.partida.estado !== "EN_CURSO") return;
    try {
        const partida = await apiFetch(`/api/partidas/${estado.partida.id}/bandera`, {
            method: "POST",
            body: JSON.stringify({ fila, columna })
        });
        actualizarPartida(partida, false);
    } catch (err) {
        mostrarAlerta(err.message, "error");
    }
}

async function abandonarPartida() {
    if (!estado.partida || estado.partida.estado !== "EN_CURSO") return;
    try {
        const partida = await apiFetch(`/api/partidas/${estado.partida.id}/abandonar`, {
            method: "POST"
        });
        actualizarPartida(partida, false);
    } catch (err) {
        mostrarAlerta(err.message, "error");
    }
}

function actualizarPartida(partida, reiniciarTimer) {
    estado.partida = partida;
    document.getElementById("hud-banderas").textContent = partida.banderasRestantes;
    document.getElementById("hud-movimientos").textContent = partida.movimientos;

    const enCurso = partida.estado === "EN_CURSO";
    document.getElementById("btn-abandonar").classList.toggle("hidden", !enCurso);
    document.getElementById("btn-nueva-partida").disabled = enCurso;

    const bannerResultado = document.getElementById("resultado-partida");
    if (enCurso) {
        bannerResultado.classList.add("hidden");
        if (reiniciarTimer) {
            iniciarCronometro();
        }
    } else {
        detenerCronometro();
        const segundosFinales = Math.max(1, Math.round((partida.tiempoMs || 0) / 1000));
        document.getElementById("hud-tiempo").textContent = formatearSegundos(segundosFinales);
        bannerResultado.classList.remove("hidden", "victoria", "derrota");

        const tieneEstrellas = Boolean(partida.estrellas);
        if (partida.estado === "TERMINADA" && tieneEstrellas) {
            const estrellas = "⭐".repeat(partida.estrellas);
            bannerResultado.textContent = `¡Victoria! Tiempo: ${formatearSegundos(segundosFinales)} | Calificación: ${estrellas}`;
            bannerResultado.classList.add("victoria");
        } else if (partida.estado === "ABANDONADA") {
            bannerResultado.textContent = "Partida abandonada.";
            bannerResultado.classList.add("derrota");
        } else {
            bannerResultado.textContent = "¡Pisaste una mina! Fin de la partida.";
            bannerResultado.classList.add("derrota");
        }
    }

    renderizarTablero(partida);
}

function renderizarTablero(partida) {
    const contenedor = document.getElementById("tablero");
    contenedor.innerHTML = "";
    contenedor.style.gridTemplateColumns = `repeat(${partida.columnas}, 32px)`;

    const matriz = partida.tableroVisible || [];
    matriz.forEach((filaDatos, r) => {
        filaDatos.forEach((valor, c) => {
            const celda = document.createElement("div");
            celda.className = "cell";

            if (valor === "H") {
                celda.addEventListener("click", () => revelarCelda(r, c));
                celda.addEventListener("contextmenu", e => alternarBandera(e, r, c));
            } else if (valor === "F") {
                celda.textContent = "🚩";
                celda.addEventListener("contextmenu", e => alternarBandera(e, r, c));
            } else if (valor === "M") {
                celda.classList.add("open", "mine");
                celda.textContent = "💣";
            } else {
                celda.classList.add("open");
                if (valor !== "0") {
                    celda.textContent = valor;
                    celda.dataset.val = valor;
                }
            }

            contenedor.appendChild(celda);
        });
    });
}

function iniciarCronometro() {
    detenerCronometro();
    document.getElementById("hud-tiempo").textContent = formatearSegundos(estado.segundosTranscurridos);
    estado.timerId = setInterval(() => {
        estado.segundosTranscurridos++;
        document.getElementById("hud-tiempo").textContent = formatearSegundos(estado.segundosTranscurridos);
    }, 1000);
}

function detenerCronometro() {
    if (estado.timerId) {
        clearInterval(estado.timerId);
        estado.timerId = null;
    }
}

function formatearSegundos(totalSegundos) {
    const mins = String(Math.floor(totalSegundos / 60)).padStart(2, "0");
    const secs = String(totalSegundos % 60).padStart(2, "0");
    return `${mins}:${secs}`;
}

async function cargarRanking() {
    const tbody = document.getElementById("tbody-ranking");
    tbody.innerHTML = "";
    try {
        const nivel = document.getElementById("filtro-ranking-nivel").value;
        const query = nivel ? `?nivel=${encodeURIComponent(nivel)}` : "";
        const datos = await apiFetch(`/api/ranking${query}`);

        if (!datos.length) {
            tbody.innerHTML = `<tr><td colspan="6">Aún no hay registros en el ranking.</td></tr>`;
            return;
        }

        datos.forEach((item, idx) => {
            const tr = document.createElement("tr");
            const tiempoSeg = item.mejorTiempoMs ? `${(item.mejorTiempoMs / 1000).toFixed(2)} s` : "-";
            tr.innerHTML = `
                <td>${idx + 1}</td>
                <td>@${item.nombreUsuario}</td>
                <td>${item.nivel}</td>
                <td>${tiempoSeg}</td>
                <td>${item.partidasTerminadas}</td>
                <td>${Number(item.promedioMovimientos).toFixed(1)}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        mostrarAlerta(err.message, "error");
    }
}

async function cargarHistorial() {
    const tbody = document.getElementById("tbody-historial");
    tbody.innerHTML = "";
    try {
        const partidas = await apiFetch("/api/partidas/historial");
        if (!partidas.length) {
            tbody.innerHTML = `<tr><td colspan="6">Todavía no has jugado ninguna partida.</td></tr>`;
            return;
        }

        partidas.forEach(p => {
            const tr = document.createElement("tr");
            const tiempo = p.tiempoMs ? `${(p.tiempoMs / 1000).toFixed(1)} s` : "-";
            const estrellas = p.estrellas ? "⭐".repeat(p.estrellas) : "-";
            tr.innerHTML = `
                <td>#${p.id}</td>
                <td>${p.nivel}</td>
                <td>${p.estado}</td>
                <td>${p.movimientos}</td>
                <td>${tiempo}</td>
                <td>${estrellas}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        mostrarAlerta(err.message, "error");
    }
}

async function cargarUsuariosAdmin() {
    const tbody = document.getElementById("tbody-admin");
    tbody.innerHTML = "";
    try {
        const usuarios = await apiFetch("/api/admin/usuarios");
        usuarios.forEach(u => {
            const tr = document.createElement("tr");
            const textoBtn = u.activo ? "Desactivar" : "Activar";
            const claseBtn = u.activo ? "btn-danger" : "btn-primary";
            tr.innerHTML = `
                <td>${u.id}</td>
                <td>${u.nombre}</td>
                <td>@${u.nombreUsuario}</td>
                <td>${u.email}</td>
                <td>${u.rol}</td>
                <td>${u.activo ? "Activo" : "Inactivo"}</td>
                <td>
                    <button class="btn ${claseBtn}" data-id="${u.id}" data-activo="${!u.activo}">
                        ${textoBtn}
                    </button>
                </td>
            `;
            tr.querySelector("button").addEventListener("click", async e => {
                const id = e.target.dataset.id;
                const nuevoEstado = e.target.dataset.activo === "true";
                await cambiarEstadoUsuario(id, nuevoEstado);
            });
            tbody.appendChild(tr);
        });
    } catch (err) {
        mostrarAlerta(err.message, "error");
    }
}

async function cambiarEstadoUsuario(id, activo) {
    try {
        await apiFetch(`/api/admin/usuarios/${id}/estado`, {
            method: "PATCH",
            body: JSON.stringify({ activo })
        });
        mostrarAlerta("Estado del usuario actualizado.", "ok");
        cargarUsuariosAdmin();
    } catch (err) {
        mostrarAlerta(err.message, "error");
    }
}