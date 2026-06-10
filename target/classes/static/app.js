const state = {
  token: localStorage.getItem("equipoFutbolToken") || "",
  user: JSON.parse(localStorage.getItem("equipoFutbolUser") || "null"),
};

const sessionStatus = document.querySelector("#sessionStatus");
const messagePanel = document.querySelector("#messagePanel");
const topPlayers = document.querySelector("#topPlayers");

function apiBase() {
  const path = window.location.pathname;
  const contextPath = path.includes("/api/v1") ? "/api/v1" : "";
  return `${window.location.origin}${contextPath}`;
}

function updateSession() {
  sessionStatus.textContent = state.user
    ? `Sesion: ${state.user.name || state.user.username} (${state.user.role || "rol"})`
    : "Sin sesion activa";
}

function setSession(response) {
  state.token = response.jwt;
  state.user = { name: response.name, role: response.role };
  localStorage.setItem("equipoFutbolToken", state.token);
  localStorage.setItem("equipoFutbolUser", JSON.stringify(state.user));
  updateSession();
}

function clearSession() {
  state.token = "";
  state.user = null;
  localStorage.removeItem("equipoFutbolToken");
  localStorage.removeItem("equipoFutbolUser");
  updateSession();
  showMessage("Sesion cerrada", "success");
}

function showMessage(message, type = "success") {
  const toast = document.createElement("div");
  toast.className = `toast ${type}`;
  toast.textContent = message;
  messagePanel.appendChild(toast);
  window.setTimeout(() => toast.remove(), 4200);
}

async function request(path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };

  if (state.token && !headers.Authorization) {
    headers.Authorization = `Bearer ${state.token}`;
  }

  const response = await fetch(`${apiBase()}${path}`, {
    ...options,
    headers,
  });

  const contentType = response.headers.get("content-type") || "";
  const body = contentType.includes("application/json") ? await response.json() : await response.text();

  if (!response.ok) {
    const message = typeof body === "string" ? body : body.message || body.error || "Error en la solicitud";
    throw new Error(message);
  }

  return body;
}

function formValues(form) {
  return Object.fromEntries(new FormData(form).entries());
}

function numberValue(value) {
  return value === "" ? null : Number(value);
}

function resetForm(form) {
  form.reset();
  const firstInput = form.querySelector("input, select");
  if (firstInput) firstInput.focus();
}

document.querySelector("#loginForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const data = formValues(form);

  try {
    const response = await request("/auth/login", {
      method: "POST",
      body: JSON.stringify(data),
    });
    setSession(response);
    resetForm(form);
    showMessage("Inicio de sesion correcto", "success");
  } catch (error) {
    showMessage(error.message, "error");
  }
});

document.querySelector("#registerForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const data = formValues(form);
  data.rol = numberValue(data.rol);

  try {
    const response = await request("/auth/register", {
      method: "POST",
      body: JSON.stringify(data),
    });
    resetForm(form);
    showMessage(response.message || "Usuario registrado", "success");
  } catch (error) {
    showMessage(error.message, "error");
  }
});

document.querySelector("#playerForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const data = formValues(form);
  data.numeroCamiseta = numberValue(data.numeroCamiseta);
  data.rol = 1;
  data.puntajeTotal = 0;

  try {
    const response = await request("/user", {
      method: "POST",
      body: JSON.stringify(data),
    });
    resetForm(form);
    showMessage(response.message || "Jugador creado", "success");
  } catch (error) {
    showMessage(error.message, "error");
  }
});

document.querySelector("#trainingForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const data = formValues(form);
  data.numeroEntrenamiento = numberValue(data.numeroEntrenamiento);
  data.pasesEfectivos = numberValue(data.pasesEfectivos);
  data.potenciaTiro = numberValue(data.potenciaTiro);
  data.velocidadJugador = numberValue(data.velocidadJugador);
  data.users = String(data.users);

  try {
    const response = await request("/resultados", {
      method: "POST",
      body: JSON.stringify(data),
    });
    resetForm(form);
    showMessage(response.message || "Resultado registrado", "success");
  } catch (error) {
    showMessage(error.message, "error");
  }
});

document.querySelector("#loadTopButton").addEventListener("click", loadTopPlayers);
document.querySelector("#logoutButton").addEventListener("click", clearSession);

async function loadTopPlayers() {
  topPlayers.className = "players-grid empty-state";
  topPlayers.textContent = "Consultando equipo titular...";

  try {
    const players = await request("/user/top5");
    renderPlayers(players);
  } catch (error) {
    topPlayers.textContent = "No se pudo consultar el top 5.";
    showMessage(error.message, "error");
  }
}

function renderPlayers(players) {
  if (!Array.isArray(players) || players.length === 0) {
    topPlayers.className = "players-grid empty-state";
    topPlayers.textContent = "No hay suficiente informacion para mostrar titulares.";
    return;
  }

  topPlayers.className = "players-grid";
  topPlayers.innerHTML = players.map((player, index) => playerCard(player, index + 1)).join("");
}

function playerCard(player, rank) {
  const entrenamientos = Array.isArray(player.entrenamientos) ? player.entrenamientos : [];
  const trainingHtml = entrenamientos.length
    ? entrenamientos.map((item) => `
        <li>
          E${item.numeroEntrenamiento}: ${formatScore(item.puntajeEntrenamiento)} pts
        </li>
      `).join("")
    : "<li>Sin entrenamientos registrados</li>";

  return `
    <article class="player-card" data-rank="#${rank}">
      <h3>${escapeHtml(player.nombre || "Jugador")}</h3>
      <div class="player-meta">
        <span>${escapeHtml(player.posicion || "Sin posicion")}</span>
        <span>Camiseta #${player.numeroCamiseta ?? "-"}</span>
        <span class="score-pill">${formatScore(player.puntajeTotal)} pts</span>
      </div>
      <ul class="training-list">${trainingHtml}</ul>
    </article>
  `;
}

function formatScore(value) {
  const number = Number(value || 0);
  return number.toFixed(2);
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

updateSession();
