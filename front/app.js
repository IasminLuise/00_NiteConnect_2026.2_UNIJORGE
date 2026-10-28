const $ = (s, root = document) => root.querySelector(s);
const $$ = (s, root = document) => [...root.querySelectorAll(s)];
const storeKey = "niteConnectDemoV1";
const initialData = {
  projects: [
    {
      id: 1,
      name: "NITE Smart Campus",
      description: "Soluções inteligentes para o ambiente universitário.",
      scope: "IoT, automação e monitoramento de ambientes",
      manager: "Iasmin Santos",
      status: "Ativo",
      start: "2026-08-10",
      end: "2026-12-15",
      members: 8,
      progress: 72,
    },
    {
      id: 2,
      name: "Robótica Educacional",
      description: "Desenvolvimento de protótipos robóticos educacionais.",
      scope: "Robótica, eletrônica e programação",
      manager: "Breno Cerqueira",
      status: "Ativo",
      start: "2026-08-15",
      end: "2026-12-10",
      members: 6,
      progress: 48,
    },
    {
      id: 3,
      name: "NITE Connect",
      description: "Plataforma de gestão das atividades do núcleo.",
      scope: "Sistemas web, dados e UX",
      manager: "Iasmin Santos",
      status: "Em Revisão",
      start: "2026-09-01",
      end: "2026-12-18",
      members: 5,
      progress: 64,
    },
    {
      id: 4,
      name: "Laboratório Maker",
      description: "Organização do acervo e documentação técnica.",
      scope: "Hardware, documentação e impressão 3D",
      manager: "João Victor Souza",
      status: "Proposto",
      start: "2026-10-01",
      end: "2026-12-12",
      members: 4,
      progress: 15,
    },
  ],
  activities: [
    {
      id: 1,
      title: "Finalizar documentação da API",
      project: "NITE Connect",
      assignee: "Iasmin Santos",
      due: "2026-10-29",
      status: "Em Andamento",
      priority: "Alta",
      description: "Documentar os endpoints e os fluxos de autenticação.",
    },
    {
      id: 2,
      title: "Validar sensores de presença",
      project: "NITE Smart Campus",
      assignee: "Breno Cerqueira",
      due: "2026-10-25",
      status: "Atrasada",
      priority: "Alta",
      description: "Testar os sensores e registrar resultados.",
    },
    {
      id: 3,
      title: "Preparar apresentação de progresso",
      project: "Robótica Educacional",
      assignee: "João Victor Souza",
      due: "2026-10-30",
      status: "Planejada",
      priority: "Média",
      description: "Organizar slides e demonstração do protótipo.",
    },
    {
      id: 4,
      title: "Revisar relatório técnico",
      project: "NITE Smart Campus",
      assignee: "Monique Amorim",
      due: "2026-11-02",
      status: "Em Revisão",
      priority: "Média",
      description: "Revisar relatório de testes do projeto.",
    },
    {
      id: 5,
      title: "Montagem do protótipo v2",
      project: "Robótica Educacional",
      assignee: "Nicholas Fróes",
      due: "2026-10-20",
      status: "Concluída",
      priority: "Alta",
      description: "Montar e testar a segunda versão do protótipo.",
    },
  ],
  members: [
    {
      id: 1,
      name: "Iasmin Santos",
      email: "iasmin@unijorge.edu.br",
      course: "Ciência da Computação",
      role: "Integrante",
      tech: "Programação",
      status: "Ativo",
      attendance: 88,
      plan: "Aprovado",
      p1: 8.5,
      p2: 8,
      p3: 9,
    },
    {
      id: 2,
      name: "Breno Cerqueira",
      email: "breno@unijorge.edu.br",
      course: "Ciência da Computação",
      role: "Gestor do Projeto",
      tech: "Sistemas Embarcados",
      status: "Ativo",
      attendance: 92,
      plan: "Aprovado",
      p1: 9,
      p2: 8.5,
      p3: 8.5,
    },
    {
      id: 3,
      name: "João Victor Souza",
      email: "joao@unijorge.edu.br",
      course: "Engenharia da Computação",
      role: "Integrante",
      tech: "Robótica",
      status: "Ativo",
      attendance: 76,
      p1: 7.5,
      p2: 8,
      p3: 7.8,
      plan: "Aprovado",
    },
    {
      id: 4,
      name: "Monique Amorim",
      email: "monique@unijorge.edu.br",
      course: "Ciência da Computação",
      role: "Integrante",
      tech: "Documentação Técnica",
      status: "Ativo",
      attendance: 68,
      p1: 8,
      p2: 0,
      p3: 8.5,
      plan: "Pendente",
    },
    {
      id: 5,
      name: "Nicholas Fróes",
      email: "nicholas@unijorge.edu.br",
      course: "Sistemas de Informação",
      role: "Integrante",
      tech: "Hardware",
      status: "Ativo",
      attendance: 84,
      p1: 8,
      p2: 8.5,
      p3: 8,
      plan: "Aprovado",
    },
    {
      id: 6,
      name: "Santhiago Santana",
      email: "santhiago@unijorge.edu.br",
      course: "Ciência da Computação",
      role: "Gestor Geral",
      tech: "Pesquisa",
      status: "Ativo",
      attendance: 95,
      p1: 9,
      p2: 9,
      p3: 9,
      plan: "Aprovado",
    },
  ],
  diary: [
    {
      id: 1,
      member: "Iasmin Santos",
      activity: "Finalizar documentação da API",
      project: "NITE Connect",
      date: "2026-10-26",
      description:
        "Estruturei os endpoints de projetos e atividades, revisei os parâmetros obrigatórios e atualizei os exemplos de requisição.",
      evidence: "https://github.com/nite-connect/documentacao",
      status: "Enviado",
    },
    {
      id: 2,
      member: "Breno Cerqueira",
      activity: "Validar sensores de presença",
      project: "NITE Smart Campus",
      date: "2026-10-24",
      description:
        "Realizei testes iniciais com três sensores e registrei as diferenças de leitura em ambiente aberto e fechado.",
      evidence: "https://github.com/nite-connect/sensores",
      status: "Enviado",
    },
  ],
  attendance: [
    {
      id: 1,
      date: "2026-10-22",
      session: "Reunião geral presencial",
      member: "Iasmin Santos",
      status: "Presente",
      hours: 2.5,
    },
    {
      id: 2,
      date: "2026-10-22",
      session: "Reunião geral presencial",
      member: "Breno Cerqueira",
      status: "Presente",
      hours: 2.5,
    },
    {
      id: 3,
      date: "2026-10-22",
      session: "Reunião geral presencial",
      member: "João Victor Souza",
      status: "Presente",
      hours: 2.5,
    },
    {
      id: 4,
      date: "2026-10-22",
      session: "Reunião geral presencial",
      member: "Monique Amorim",
      status: "Falta",
      hours: 0,
    },
    {
      id: 5,
      date: "2026-10-22",
      session: "Reunião geral presencial",
      member: "Nicholas Fróes",
      status: "Presente",
      hours: 2.5,
    },
    {
      id: 6,
      date: "2026-10-22",
      session: "Reunião geral presencial",
      member: "Santhiago Santana",
      status: "Presente",
      hours: 2.5,
    },
  ],
  notifications: [
    {
      id: 1,
      title: "Diário de Bordo pendente",
      body: "Não se esqueça de registrar o progresso das suas atividades com evidências digitais.",
      date: "Hoje, 09:15",
      read: false,
      type: "warning",
    },
    {
      id: 2,
      title: "Reunião geral do NITE",
      body: "Lembrete: reunião de apresentação de progresso na quinta-feira, às 17h.",
      date: "Hoje, 08:00",
      read: false,
      type: "info",
    },
    {
      id: 3,
      title: "Plano de atividades aguardando aprovação",
      body: "Seu plano individual está aguardando análise da coordenação.",
      date: "Ontem, 15:40",
      read: false,
      type: "warning",
    },
    {
      id: 4,
      title: "Atividade atualizada",
      body: "A atividade “Revisar relatório técnico” foi movida para Em Revisão.",
      date: "24 out, 16:20",
      read: true,
      type: "info",
    },
  ],
  audit: [
    {
      id: 1,
      author: "Santhiago Santana",
      action: "Atualização de status",
      target: "Robótica Educacional",
      date: "27/10/2026 09:22",
      reason: "Revisão semanal do projeto.",
    },
    {
      id: 2,
      author: "Iasmin Santos",
      action: "Envio de Diário de Bordo",
      target: "NITE Connect / API",
      date: "26/10/2026 18:05",
      reason: "Registro de execução da atividade.",
    },
  ],
  plans: [
    {
      id: 1,
      member: "Iasmin Santos",
      objective:
        "Desenvolver competências em sistemas web, arquitetura de software e gestão de projetos.",
      schedule: "Entregas quinzenais, documentação e revisão com o gestor.",
      status: "Aprovado",
    },
    {
      id: 2,
      member: "Monique Amorim",
      objective: "Aprimorar documentação técnica e processos de validação.",
      schedule: "Revisões semanais e produção de relatórios.",
      status: "Pendente",
    },
  ],
  sessionPin: "482619",
  sessionOpen: false,
  nextId: 10,
};
let data;
try {
  data =
    JSON.parse(localStorage.getItem(storeKey)) || structuredClone(initialData);
} catch {
  data = structuredClone(initialData);
}
let currentRole = "Integrante",
  currentName = "Iasmin Santos",
  currentEmail = "iasmin@unijorge.edu.br",
  currentPage = "dashboard",
  attendanceDraft = {},
  activitySearch = "",
  projectSearch = "",
  memberSearch = "";
const roleNames = {
  Integrante: "Iasmin Santos",
  "Gestor do Projeto": "Breno Cerqueira",
  "Gestor Geral": "Santhiago Santana",
  Coordenador: "Iasmin Santos",
};
const roleEmails = {
  Integrante: "iasmin@unijorge.edu.br",
  "Gestor do Projeto": "breno@unijorge.edu.br",
  "Gestor Geral": "santhiago@unijorge.edu.br",
  Coordenador: "iasmin@unijorge.edu.br",
};
function persist() {
  localStorage.setItem(storeKey, JSON.stringify(data));
}
function escapeHtml(v = "") {
  return String(v).replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[
        c
      ])
  );
}
function initials(name = "") {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((n) => n[0])
    .join("")
    .toUpperCase();
}
function fmtDate(date) {
  if (!date) return "—";
  const d = new Date(date + "T12:00:00");
  return d
    .toLocaleDateString("pt-BR", { day: "2-digit", month: "short" })
    .replace(".", "");
}
function fmtDateLong(date) {
  if (!date) return "—";
  return new Date(date + "T12:00:00").toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "long",
    year: "numeric",
  });
}
function statusPill(status) {
  let c = "gray";
  if (
    [
      "Ativo",
      "Concluída",
      "Presente",
      "Aprovado",
      "Enviado",
      "Homologada",
      "Convalidada",
    ].includes(status)
  )
    c = "green";
  if (
    ["Em Andamento", "Em Revisão", "Solicitada", "Em acompanhamento"].includes(
      status
    )
  )
    c = "blue";
  if (["Planejada", "Proposto", "Pendente", "Abonado"].includes(status))
    c = "yellow";
  if (
    ["Atrasada", "Falta", "Cancelado", "Anulada", "Reprovado"].includes(status)
  )
    c = "red";
  return `<span class="pill ${c}">${escapeHtml(status)}</span>`;
}
function toast(title, message, type = "success") {
  const el = document.createElement("div");
  el.className = `toast ${type}`;
  el.innerHTML = `<span>${
    type === "error" ? "⚠" : "✓"
  }</span><div><b>${escapeHtml(title)}</b><small>${escapeHtml(
    message
  )}</small></div>`;
  $("#toastRoot").append(el);
  setTimeout(() => el.remove(), 3700);
}
function logAudit(
  action,
  target,
  reason = "Ação registrada pela interface de demonstração."
) {
  data.audit.unshift({
    id: Date.now(),
    author: currentName,
    action,
    target,
    date: new Date().toLocaleString("pt-BR"),
    reason,
  });
  persist();
}
function canManage() {
  return ["Gestor do Projeto", "Gestor Geral", "Coordenador"].includes(
    currentRole
  );
}
function canCoordinate() {
  return currentRole === "Coordenador";
}
function canSeeAudit() {
  return ["Coordenador", "Gestor Geral"].includes(currentRole);
}
function canManageAttendance() {
  return ["Gestor Geral", "Coordenador"].includes(currentRole);
}
function allowedPage(page) {
  if (page === "audit" && !canSeeAudit()) return false;
  if (
    page === "evaluations" &&
    !["Coordenador", "Gestor Geral", "Integrante"].includes(currentRole)
  )
    return true;
  return true;
}
function setProfile() {
  const auditLink = $('.nav-link[data-page="audit"]');
  if (auditLink) auditLink.style.display = canSeeAudit() ? "" : "none";
  $("#topName").textContent = currentName;
  $("#topRole").textContent = currentRole;
  $("#topAvatar").textContent = initials(currentName);
  $("#topAvatar").className =
    "avatar " +
    (currentRole === "Coordenador"
      ? "avatar-purple"
      : currentRole === "Gestor Geral"
      ? "avatar-green"
      : currentRole === "Gestor do Projeto"
      ? "avatar-orange"
      : "avatar-blue");
  $("#notificationBadge").textContent = data.notifications.filter(
    (n) => !n.read
  ).length;
  $("#notificationBadge").style.display = data.notifications.some(
    (n) => !n.read
  )
    ? ""
    : "none";
}
function showApp() {
  $("#loginScreen").classList.add("hidden");
  $("#appShell").classList.remove("hidden");
  setProfile();
  renderPage();
}
$("#loginButton").addEventListener("click", () => {
  const email = $("#loginEmail").value.trim();
  if (!email || !email.includes("@")) {
    toast(
      "E-mail inválido",
      "Informe um e-mail institucional válido.",
      "error"
    );
    return;
  }
  currentRole = $("#demoRole").value;
  currentName = roleNames[currentRole];
  currentEmail = email;
  logAudit(
    "Acesso ao sistema",
    currentRole,
    "Entrada em modo de demonstração."
  );
  showApp();
  toast("Acesso realizado", `Perfil ativo: ${currentRole}.`);
});
$("#logoutButton").addEventListener("click", () => {
  $("#appShell").classList.add("hidden");
  $("#loginScreen").classList.remove("hidden");
  $("#sidebar").classList.remove("open");
  toast("Sessão encerrada", "Você saiu do ambiente de demonstração.");
});
$("#mobileMenu").addEventListener("click", () =>
  $("#sidebar").classList.toggle("open")
);
$("#mainNav").addEventListener("click", (e) => {
  const b = e.target.closest("[data-page]");
  if (!b) return;
  navigate(b.dataset.page);
});
$("#topNotifications").addEventListener("click", () =>
  navigate("notifications")
);
$("#profileButton").addEventListener("click", () => {
  openModal(
    "Trocar perfil de demonstração",
    "Selecione o perfil que deseja visualizar.",
    `<div class="field"><label>Perfil de acesso</label><select id="switchRole" class="form-control">${Object.keys(
      roleNames
    )
      .map(
        (r) => `<option ${r === currentRole ? "selected" : ""}>${r}</option>`
      )
      .join(
        ""
      )}</select></div><p class="muted" style="font-size:10px;margin-top:12px">A troca de perfil serve apenas para demonstrar as visões de acesso definidas no documento de requisitos.</p>`,
    [
      { label: "Cancelar", class: "btn-ghost", action: closeModal },
      {
        label: "Aplicar perfil",
        class: "btn-primary",
        action: () => {
          currentRole = $("#switchRole").value;
          currentName = roleNames[currentRole];
          currentEmail = roleEmails[currentRole];
          logAudit("Alteração de perfil de demonstração", currentRole);
          closeModal();
          setProfile();
          renderPage();
          toast(
            "Perfil atualizado",
            `Agora você está visualizando como ${currentRole}.`
          );
        },
      },
    ]
  );
});
$("#helpButton").addEventListener("click", () =>
  openModal(
    "Central de ajuda",
    "Orientações rápidas para usar o protótipo.",
    `<div class="timeline"><div class="timeline-item"><b>1. Navegue pelos módulos</b><p>Use o menu lateral para acessar projetos, atividades, frequência e avaliações.</p></div><div class="timeline-item"><b>2. Cadastre e edite informações</b><p>Os formulários validam os campos e salvam os dados no armazenamento local do navegador.</p></div><div class="timeline-item"><b>3. Teste os perfis</b><p>Clique no perfil do topo para alternar entre Integrante, Gestor do Projeto, Gestor Geral e Coordenador.</p></div></div><p class="muted" style="font-size:10px">Este é um protótipo frontend. SSO Microsoft, banco relacional, e-mail automático e validações no servidor precisam de integração backend.</p>`,
    [{ label: "Fechar", class: "btn-primary", action: closeModal }]
  )
);
function navigate(page) {
  if (page === "audit" && !canSeeAudit()) {
    toast(
      "Acesso restrito",
      "A auditoria é exclusiva para Gestor Geral e Coordenador.",
      "error"
    );
    return;
  }
  currentPage = page;
  $("#sidebar").classList.remove("open");
  $$(".nav-link").forEach((b) =>
    b.classList.toggle("active", b.dataset.page === page)
  );
  $("#breadcrumbCurrent").textContent =
    {
      dashboard: "Visão geral",
      projects: "Projetos",
      activities: "Atividades",
      diary: "Diário de Bordo",
      attendance: "Frequência",
      members: "Integrantes",
      evaluations: "Avaliações e AV3",
      reports: "Relatórios",
      notifications: "Notificações",
      audit: "Auditoria",
    }[page] || "Visão geral";
  renderPage();
}
function pageHeading(title, subtitle, actions = "") {
  return `<div class="page-heading"><div><h1>${title}</h1><p>${subtitle}</p></div><div class="heading-actions">${actions}</div></div>`;
}
function statCard(label, value, icon, foot, cls = "") {
  return `<div class="stat-card"><div class="stat-top"><span>${label}</span><span class="stat-icon">${icon}</span></div><div class="stat-value ${cls}">${value}</div><div class="stat-foot">${foot}</div></div>`;
}
function panelHead(title, sub = "", right = "") {
  return `<div class="panel-head"><div><h3>${title}</h3>${
    sub ? `<p>${sub}</p>` : ""
  }</div>${right}</div>`;
}
function progress(label, val, color = "") {
  return `<div class="progress-row"><div class="progress-info"><b>${label}</b><span>${val}%</span></div><div class="progress-track"><div class="progress-fill ${color}" style="width:${Math.min(
    100,
    Math.max(0, val)
  )}%"></div></div></div>`;
}
function projectByName(name) {
  return data.projects.find((p) => p.name === name);
}
function weighted(m) {
  return (
    Number(m.p1 || 0) * 0.3 +
    Number(m.p2 || 0) * 0.2 +
    Number(m.p3 || 0) * 0.5
  ).toFixed(1);
}
function renderPage() {
  setProfile();
  const root = $("#pageContent");
  const pages = {
    dashboard: renderDashboard,
    projects: renderProjects,
    activities: renderActivities,
    diary: renderDiary,
    attendance: renderAttendance,
    members: renderMembers,
    evaluations: renderEvaluations,
    reports: renderReports,
    notifications: renderNotifications,
    audit: renderAudit,
  };
  root.innerHTML = (pages[currentPage] || renderDashboard)();
  bindPageEvents();
}
function renderDashboard() {
  const totalActivities = data.activities.length,
    completed = data.activities.filter((a) => a.status === "Concluída").length,
    overdue = data.activities.filter(
      (a) =>
        a.status === "Atrasada" ||
        (a.status !== "Concluída" && a.due < "2026-10-27")
    ).length;
  const attendance =
    currentRole === "Integrante"
      ? data.members.find((m) => m.name === currentName)?.attendance || 88
      : Math.round(
          data.members.reduce((s, m) => s + m.attendance, 0) /
            Math.max(1, data.members.length)
        );
  const activeProjects = data.projects.filter(
    (p) => p.status === "Ativo"
  ).length;
  const myTasks = data.activities.filter((a) => a.assignee === currentName);
  const shownTasks =
    currentRole === "Integrante" && myTasks.length
      ? myTasks
      : data.activities.slice(0, 4);
  return `${pageHeading(
    `Olá, ${escapeHtml(currentName.split(" ")[0])} 👋`,
    "Acompanhe os principais indicadores e as atividades do NITE neste semestre.",
    `<button class="btn btn-secondary" data-action="quick-diary">＋ Registrar progresso</button><button class="btn btn-primary" data-action="new-activity">＋ Nova atividade</button>`
  )}<div class="stats-grid">${statCard(
    "Projetos ativos",
    activeProjects,
    "▦",
    `<span class="positive">${data.projects.length}</span> projetos cadastrados`
  )}${statCard(
    "Atividades registradas",
    totalActivities,
    "☷",
    `<span class="positive">${completed}</span> concluídas`
  )}${statCard(
    "Frequência acumulada",
    `${attendance}%`,
    "◷",
    attendance < 75
      ? '<span class="negative">Abaixo do mínimo de 75%</span>'
      : '<span class="positive">Dentro do limite regulamentar</span>'
  )}${statCard(
    "Atividades em atraso",
    overdue,
    "◴",
    overdue
      ? '<span class="negative">Precisam de atenção</span>'
      : "Sem atrasos identificados"
  )}</div><div class="dashboard-grid"><section class="panel">${panelHead(
    "Visão geral das atividades",
    "Acompanhamento do andamento das entregas do núcleo.",
    `<span class="chip">Semestre 2026.2</span>`
  )}<div class="chart-area"><svg viewBox="0 0 650 205" role="img" aria-label="Gráfico de evolução das atividades"><defs><linearGradient id="areaBlue" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#4285f4" stop-opacity=".28"/><stop offset="100%" stop-color="#4285f4" stop-opacity="0"/></linearGradient></defs>${[
    30, 70, 110, 150, 190,
  ]
    .map(
      (y) =>
        `<line x1="36" y1="${y}" x2="635" y2="${y}" stroke="#27384f" stroke-dasharray="3 5"/>`
    )
    .join(
      ""
    )}<path d="M36 166 C90 150 105 145 135 138 S220 126 235 111 S330 105 335 87 S430 91 435 68 S530 61 535 46 S600 40 635 30 L635 190 L36 190Z" fill="url(#areaBlue)"/><path d="M36 166 C90 150 105 145 135 138 S220 126 235 111 S330 105 335 87 S430 91 435 68 S530 61 535 46 S600 40 635 30" fill="none" stroke="#4e98ff" stroke-width="3" stroke-linecap="round"/><path d="M36 176 C90 171 105 159 135 165 S220 150 235 145 S330 138 335 128 S430 130 435 115 S530 113 535 97 S600 93 635 84" fill="none" stroke="#48c5df" stroke-width="2.5" stroke-linecap="round" stroke-dasharray="5 5"/>${[
    36, 135, 235, 335, 435, 535, 635,
  ]
    .map(
      (x, i) =>
        `<circle cx="${x}" cy="${
          [166, 138, 111, 87, 68, 46, 30][i]
        }" r="3.5" fill="#4e98ff" stroke="#152238" stroke-width="2"/>`
    )
    .join(
      ""
    )}</svg><div class="chart-labels"><span>Set</span><span>Sem 2</span><span>Sem 3</span><span>Sem 4</span><span>Sem 5</span><span>Sem 6</span><span>Atual</span></div></div><div class="legend"><span>Entregas concluídas</span><span class="legend-cyan">Entregas planejadas</span></div></section><section class="panel">${panelHead(
    "Distribuição dos projetos",
    "Progresso médio por projeto"
  )}<div style="margin-top:20px">${data.projects
    .slice(0, 4)
    .map((p) =>
      progress(
        escapeHtml(p.name),
        p.progress,
        p.progress < 40 ? "yellow" : p.progress > 65 ? "green" : ""
      )
    )
    .join(
      ""
    )}</div><button class="text-link" data-navigate="projects">Ver todos os projetos →</button></section></div><div class="dashboard-grid"><section class="panel">${panelHead(
    "Atividades em destaque",
    "Prazos e tarefas que merecem atenção",
    `<button class="text-link" data-navigate="activities">Ver todas →</button>`
  )}<div class="task-list">${
    shownTasks
      .slice(0, 4)
      .map(
        (a) =>
          `<div class="task-item"><div class="task-symbol">☷</div><div class="task-copy"><b>${escapeHtml(
            a.title
          )}</b><small>${escapeHtml(a.project)} · ${escapeHtml(
            a.assignee
          )}</small></div><div class="task-meta">${statusPill(
            a.status
          )}<small>Prazo ${fmtDate(a.due)}</small></div></div>`
      )
      .join("") ||
    '<div class="empty-state">Nenhuma atividade atribuída a este perfil.</div>'
  }</div></section><section class="panel">${panelHead(
    "Atenção necessária",
    "Alertas automáticos do sistema"
  )}<div class="activity-list">${
    overdue
      ? `<div class="activity-item"><div class="task-symbol" style="color:#ff9ba7;background:#482832">!</div><div class="task-copy"><b>${overdue} atividade(s) atrasada(s)</b><small>Verifique prazos e atualize o status das entregas.</small></div></div>`
      : ""
  }${
    data.members
      .filter((m) => m.attendance < 75)
      .map(
        (m) =>
          `<div class="activity-item"><div class="task-symbol" style="color:#f6cc70;background:#47391e">◷</div><div class="task-copy"><b>Frequência em risco: ${escapeHtml(
            m.name
          )}</b><small>Assiduidade acumulada de ${
            m.attendance
          }% (mínimo 75%).</small></div></div>`
      )
      .join("") ||
    '<div class="activity-item"><div class="task-symbol" style="color:#5dd8a8;background:#173b35">✓</div><div class="task-copy"><b>Nenhum alerta crítico de frequência</b><small>Continue acompanhando os indicadores do semestre.</small></div></div>'
  }<div class="activity-item"><div class="task-symbol">♧</div><div class="task-copy"><b>Reunião geral do NITE</b><small>Quinta-feira, às 17h · apresentação de progresso</small></div></div></div></section></div>`;
}
function renderProjects() {
  const filtered = data.projects.filter((p) =>
    (p.name + " " + p.manager + " " + p.status)
      .toLowerCase()
      .includes(projectSearch.toLowerCase())
  );
  return `${pageHeading(
    "Projetos",
    "Gerencie os projetos de extensão e inovação, seus responsáveis e andamento.",
    `${
      canManage()
        ? '<button class="btn btn-primary" data-action="new-project">＋ Cadastrar projeto</button>'
        : ""
    }`
  )}<div class="stats-grid">${statCard(
    "Total de projetos",
    data.projects.length,
    "▦",
    "Projetos cadastrados no semestre"
  )}${statCard(
    "Projetos ativos",
    data.projects.filter((p) => p.status === "Ativo").length,
    "↗",
    "Em desenvolvimento"
  )}${statCard(
    "Em revisão",
    data.projects.filter((p) => p.status === "Em Revisão").length,
    "◷",
    "Aguardando validação"
  )}${statCard(
    "Precisam de apoio",
    data.projects.filter((p) => projectRisk(p)).length,
    "⚑",
    "Com pendências de prazo"
  )}</div><section class="panel table-panel"><div class="table-toolbar"><div class="toolbar-left"><div class="searchbox"><input id="projectSearch" placeholder="Buscar projeto ou responsável..." value="${escapeHtml(
    projectSearch
  )}" /></div><select id="projectStatusFilter" class="filter-select"><option value="">Todos os status</option>${[
    "Proposto",
    "Ativo",
    "Em Revisão",
    "Concluído",
    "Cancelado",
  ]
    .map((s) => `<option>${s}</option>`)
    .join(
      ""
    )}</select></div><div class="toolbar-right"><span class="muted" style="font-size:10px">${
    filtered.length
  } projeto(s)</span></div></div><div class="table-wrap"><table class="data-table"><thead><tr><th>PROJETO</th><th>GESTOR RESPONSÁVEL</th><th>PERÍODO</th><th>PROGRESSO</th><th>STATUS</th><th>AÇÕES</th></tr></thead><tbody id="projectsRows">${projectRows(
    filtered
  )}</tbody></table></div></section>`;
}
function projectRisk(p) {
  const tasks = data.activities.filter((a) => a.project === p.name);
  return (
    tasks.length > 0 &&
    tasks.filter((a) => a.status === "Atrasada").length / tasks.length > 0.3
  );
}
function projectRows(items) {
  return (
    items
      .map(
        (p) =>
          `<tr><td><div class="table-name"><span class="mini-avatar">${initials(
            p.name
          )}</span><span><b>${escapeHtml(p.name)}</b><small>${escapeHtml(
            p.description
          )}</small></span></div></td><td>${escapeHtml(
            p.manager
          )}</td><td>${fmtDate(p.start)} – ${fmtDate(
            p.end
          )}</td><td style="min-width:120px"><div class="progress-info"><span>${
            p.progress
          }%</span></div><div class="progress-track"><div class="progress-fill ${
            p.progress > 65 ? "green" : ""
          }" style="width:${p.progress}%"></div></div></td><td>${statusPill(
            p.status
          )} ${
            projectRisk(p)
              ? '<span class="pill red">Precisa de apoio</span>'
              : ""
          }</td><td><div class="row-actions"><button class="small-action" data-action="view-project" data-id="${
            p.id
          }">Ver</button>${
            canManage()
              ? `<button class="small-action" data-action="edit-project" data-id="${p.id}">Editar</button>`
              : ""
          }</div></td></tr>`
      )
      .join("") ||
    '<tr><td colspan="6"><div class="empty-state">Nenhum projeto encontrado.</div></td></tr>'
  );
}
function renderActivities() {
  const items = data.activities.filter((a) =>
    (a.title + " " + a.project + " " + a.assignee)
      .toLowerCase()
      .includes(activitySearch.toLowerCase())
  );
  return `${pageHeading(
    "Atividades",
    "Cadastre tarefas, acompanhe responsáveis e identifique entregas atrasadas.",
    `${
      canManage()
        ? '<button class="btn btn-primary" data-action="new-activity">＋ Nova atividade</button>'
        : ""
    }`
  )}<div class="stats-grid">${statCard(
    "Total de atividades",
    data.activities.length,
    "☷",
    "Atividades registradas"
  )}${statCard(
    "Concluídas",
    data.activities.filter((a) => a.status === "Concluída").length,
    "✓",
    "Entregas finalizadas"
  )}${statCard(
    "Em andamento",
    data.activities.filter((a) => a.status === "Em Andamento").length,
    "↗",
    "Em execução"
  )}${statCard(
    "Atrasadas",
    data.activities.filter(
      (a) =>
        a.status === "Atrasada" ||
        (a.status !== "Concluída" && a.due < "2026-10-27")
    ).length,
    "◴",
    "Requerem acompanhamento"
  )}</div><section class="panel table-panel"><div class="table-toolbar"><div class="toolbar-left"><div class="searchbox"><input id="activitySearch" placeholder="Buscar atividade, projeto ou integrante..." value="${escapeHtml(
    activitySearch
  )}" /></div><select id="activityStatusFilter" class="filter-select"><option value="">Todos os status</option>${[
    "Planejada",
    "Em Andamento",
    "Em Revisão",
    "Concluída",
    "Atrasada",
  ]
    .map((s) => `<option>${s}</option>`)
    .join(
      ""
    )}</select></div></div><div class="table-wrap"><table class="data-table"><thead><tr><th>ATIVIDADE</th><th>PROJETO</th><th>RESPONSÁVEL</th><th>PRAZO</th><th>PRIORIDADE</th><th>STATUS</th><th>AÇÕES</th></tr></thead><tbody id="activitiesRows">${activityRows(
    items
  )}</tbody></table></div></section>`;
}
function activityRows(items) {
  return (
    items
      .map(
        (a) =>
          `<tr><td><div class="table-name"><span class="mini-avatar">☷</span><span><b>${escapeHtml(
            a.title
          )}</b><small>${escapeHtml(
            a.description
          )}</small></span></div></td><td>${escapeHtml(
            a.project
          )}</td><td>${escapeHtml(a.assignee)}</td><td>${fmtDate(a.due)}${
            a.due < "2026-10-27" && a.status !== "Concluída"
              ? '<br><small style="color:#ff9ba7">Prazo vencido</small>'
              : ""
          }</td><td>${
            a.priority === "Alta"
              ? '<span class="pill red">Alta</span>'
              : a.priority === "Média"
              ? '<span class="pill yellow">Média</span>'
              : '<span class="pill gray">Baixa</span>'
          }</td><td>${statusPill(
            a.status
          )}</td><td><div class="row-actions"><button class="small-action" data-action="view-activity" data-id="${
            a.id
          }">Ver</button>${
            canManage() || a.assignee === currentName
              ? `<button class="small-action" data-action="edit-activity" data-id="${a.id}">Editar</button>`
              : ""
          }</div></td></tr>`
      )
      .join("") ||
    '<tr><td colspan="7"><div class="empty-state">Nenhuma atividade encontrada.</div></td></tr>'
  );
}
function renderDiary() {
  const own =
    currentRole === "Integrante"
      ? data.diary.filter((d) => d.member === currentName)
      : data.diary;
  return `${pageHeading(
    "Diário de Bordo",
    "Registre o progresso diário das atividades e vincule evidências técnicas reais.",
    `<button class="btn btn-primary" data-action="quick-diary">＋ Novo registro</button>`
  )}${
    data.members.find((m) => m.name === currentName)?.plan !== "Aprovado" &&
    currentRole === "Integrante"
      ? `<div class="notice-card unread"><div class="notice-icon">!</div><div class="notice-content"><b>Plano individual pendente</b><p>O envio do Diário de Bordo fica bloqueado até que o Plano de Atividades Individual esteja aprovado pela coordenação.</p><button class="text-link" data-navigate="evaluations">Consultar plano →</button></div></div>`
      : ""
  }<div class="dashboard-grid"><section class="panel">${panelHead(
    "Registros recentes",
    "Cada registro deve conter uma evidência digital verificável."
  )}${
    own
      .map(
        (d) =>
          `<div class="task-item"><div class="task-symbol">▤</div><div class="task-copy"><b>${escapeHtml(
            d.activity
          )}</b><small>${escapeHtml(d.member)} · ${fmtDateLong(
            d.date
          )}</small><p style="font-size:10px;color:#91a5bd;margin:7px 0">${escapeHtml(
            d.description
          )}</p><a href="${escapeHtml(
            d.evidence
          )}" target="_blank" rel="noopener" style="font-size:10px;color:#78adff">↗ Abrir evidência técnica</a></div><div class="task-meta">${statusPill(
            d.status
          )}</div></div>`
      )
      .join("") ||
    '<div class="empty-state"><strong>Nenhum registro encontrado</strong>Registre o progresso da atividade para manter o histórico atualizado.</div>'
  }</section><section class="panel">${panelHead(
    "Orientações para registro",
    "Regras de envio do Diário de Bordo"
  )}<div class="timeline"><div class="timeline-item"><b>Selecione uma atividade cadastrada</b><p>O diário sempre precisa estar vinculado a uma atividade previamente registrada.</p></div><div class="timeline-item"><b>Descreva o que foi realizado</b><p>Informe etapas executadas, resultados obtidos e próximos passos.</p></div><div class="timeline-item"><b>Adicione evidência digital</b><p>Inclua link para commit, repositório, relatório ou outra evidência técnica real.</p></div><div class="timeline-item"><b>Envie para acompanhamento</b><p>O registro integra o histórico individual e o acompanhamento do projeto.</p></div></div><button class="btn btn-secondary btn-wide" data-action="quick-diary">Registrar progresso</button></section></div>`;
}
function renderAttendance() {
  const members = data.members.filter((m) => m.status === "Ativo");
  const avg = Math.round(
    members.reduce((s, m) => s + m.attendance, 0) / Math.max(1, members.length)
  );
  const todayRecords = data.attendance.filter((a) => a.date === "2026-10-22");
  return `${pageHeading(
    "Frequência e assiduidade",
    "Acompanhe a presença, registre chamadas e gerencie justificativas.",
    `${
      canManageAttendance()
        ? '<button class="btn btn-secondary" data-action="open-session">' +
          (data.sessionOpen ? "Sessão aberta" : "＋ Abrir sessão") +
          "</button>"
        : ""
    }${
      canManageAttendance()
        ? '<button class="btn btn-primary" data-action="save-attendance">Salvar chamada</button>'
        : ""
    }`
  )}<div class="stats-grid">${statCard(
    "Assiduidade média",
    `${avg}%`,
    "◷",
    avg < 75
      ? '<span class="negative">Abaixo do mínimo regulamentar</span>'
      : '<span class="positive">Meta mínima: 75%</span>'
  )}${statCard(
    "Integrantes ativos",
    members.length,
    "♙",
    "Participantes do semestre"
  )}${statCard(
    "Presentes na chamada",
    todayRecords.filter((a) => a.status === "Presente").length,
    "✓",
    "Última sessão registrada"
  )}${statCard(
    "Justificativas pendentes",
    2,
    "▤",
    "Aguardando análise"
  )}</div><div class="attendance-grid"><section class="panel">${panelHead(
    "Chamada digital · 22/10/2026",
    "Reunião geral presencial · 16h às 18h30",
    `<span class="chip">${
      todayRecords.filter((a) => a.status === "Presente").length
    }/${members.length} presentes</span>`
  )}${members
    .map((m) => {
      const rec = todayRecords.find((a) => a.member === m.name);
      const status = attendanceDraft[m.name] || rec?.status || "Presente";
      return `<div class="attendance-row"><span class="mini-avatar">${initials(
        m.name
      )}</span><div class="member-info"><b>${escapeHtml(
        m.name
      )}</b><small>${escapeHtml(
        m.course
      )}</small></div><div class="attendance-controls"><button class="${
        status === "Presente" ? "selected present" : ""
      }" data-action="mark-attendance" data-name="${escapeHtml(
        m.name
      )}" data-status="Presente">Presente</button><button class="${
        status === "Falta" ? "selected absent" : ""
      }" data-action="mark-attendance" data-name="${escapeHtml(
        m.name
      )}" data-status="Falta">Falta</button><button class="${
        status === "Abonado" ? "selected justified" : ""
      }" data-action="mark-attendance" data-name="${escapeHtml(
        m.name
      )}" data-status="Abonado">Abonado</button></div></div>`;
    })
    .join("")}${
    canManageAttendance()
      ? '<p class="muted" style="font-size:9px;margin-top:12px">As alterações ficam como rascunho até clicar em “Salvar chamada”.</p>'
      : '<p class="muted" style="font-size:9px;margin-top:12px">A chamada em lote é gerenciada pelo Gestor Geral ou Coordenador.</p>'
  }</section><section class="panel">${panelHead(
    "Minha assiduidade",
    "Resumo da frequência acumulada"
  )}<div class="ring-wrap"><div class="ring" style="--ring:${
    data.members.find((m) => m.name === currentName)?.attendance || 88
  }%"><div class="ring-inner"><b>${
    data.members.find((m) => m.name === currentName)?.attendance || 88
  }%</b><small>presença</small></div></div><div><div class="metric-big">${
    data.members.find((m) => m.name === currentName)?.attendance || 88
  }%</div><div class="metric-caption">Frequência acumulada</div><p style="font-size:10px;color:#8fa3bc">Mínimo exigido: 75%</p>${
    (data.members.find((m) => m.name === currentName)?.attendance || 88) < 75
      ? statusPill("Atrasada")
      : statusPill("Ativo")
  }</div></div><div class="progress-row"><div class="progress-info"><b>Meta de assiduidade</b><span>75%</span></div><div class="progress-track"><div class="progress-fill green" style="width:75%"></div></div></div><hr style="border:0;border-top:1px solid #26364c;margin:18px 0"><h3 style="font-size:11px">Self check-in por PIN</h3><p class="muted" style="font-size:10px">Insira o código disponibilizado pelo Gestor Geral durante a sessão presencial.</p><div class="field"><label for="checkinPin">PIN da sessão</label><input id="checkinPin" class="form-control" placeholder="Ex.: 482619" maxlength="6"></div><button class="btn btn-primary btn-wide" data-action="checkin">Confirmar presença</button><p class="muted" style="font-size:9px">${
    data.sessionOpen
      ? "Sessão aberta para check-in."
      : "A sessão precisa ser aberta pela gestão para aceitar o PIN."
  }</p><hr style="border:0;border-top:1px solid #26364c;margin:18px 0"><button class="btn btn-secondary btn-wide" data-action="justify-absence">Enviar justificativa de ausência</button></section></div>`;
}
function renderMembers() {
  const items = data.members.filter((m) =>
    (m.name + " " + m.email + " " + m.tech)
      .toLowerCase()
      .includes(memberSearch.toLowerCase())
  );
  return `${pageHeading(
    "Integrantes",
    "Consulte os participantes, seus papéis técnicos e indicadores de acompanhamento.",
    `${
      canManage()
        ? '<button class="btn btn-primary" data-action="new-member">＋ Cadastrar integrante</button>'
        : ""
    }`
  )}<div class="stats-grid">${statCard(
    "Integrantes ativos",
    data.members.filter((m) => m.status === "Ativo").length,
    "♙",
    "No semestre atual"
  )}${statCard(
    "Frequência em risco",
    data.members.filter((m) => m.attendance < 75).length,
    "◷",
    "Abaixo de 75%"
  )}${statCard(
    "Planos pendentes",
    data.members.filter((m) => m.plan !== "Aprovado").length,
    "▤",
    "Aguardando aprovação"
  )}${statCard(
    "Média geral",
    `${(
      data.members.reduce((s, m) => s + Number(weighted(m)), 0) /
      Math.max(1, data.members.length)
    ).toFixed(1)}`,
    "☆",
    "Média ponderada dos pilares"
  )}</div><section class="panel table-panel"><div class="table-toolbar"><div class="toolbar-left"><div class="searchbox"><input id="memberSearch" placeholder="Buscar integrante, e-mail ou papel técnico..." value="${escapeHtml(
    memberSearch
  )}" /></div><select class="filter-select" id="memberStatusFilter"><option value="">Todos os status</option><option>Ativo</option><option>Pendente</option><option>Inativo</option></select></div><div class="toolbar-right"><span class="muted" style="font-size:10px">${
    items.length
  } integrante(s)</span></div></div><div class="table-wrap"><table class="data-table"><thead><tr><th>INTEGRANTE</th><th>CURSO</th><th>PAPEL TÉCNICO</th><th>FREQUÊNCIA</th><th>PLANO INDIVIDUAL</th><th>NOTA FINAL</th><th>AÇÕES</th></tr></thead><tbody id="membersRows">${memberRows(
    items
  )}</tbody></table></div></section>`;
}
function memberRows(items) {
  return (
    items
      .map(
        (m) =>
          `<tr><td><div class="table-name"><span class="mini-avatar">${initials(
            m.name
          )}</span><span><b>${escapeHtml(m.name)}</b><small>${escapeHtml(
            m.email
          )}</small></span></div></td><td>${escapeHtml(
            m.course
          )}</td><td>${escapeHtml(m.tech)}</td><td><b style="color:${
            m.attendance < 75 ? "#ff9ba7" : "#dbe7f6"
          }">${
            m.attendance
          }%</b><div class="progress-track" style="margin-top:5px;width:70px"><div class="progress-fill ${
            m.attendance < 75 ? "yellow" : "green"
          }" style="width:${m.attendance}%"></div></div></td><td>${statusPill(
            m.plan || "Pendente"
          )}</td><td><b>${weighted(
            m
          )}</b></td><td><button class="small-action" data-action="view-member" data-id="${
            m.id
          }">Histórico</button>${
            canManage()
              ? ` <button class="small-action" data-action="edit-member" data-id="${m.id}">Editar</button>`
              : ""
          }</td></tr>`
      )
      .join("") ||
    '<tr><td colspan="7"><div class="empty-state">Nenhum integrante encontrado.</div></td></tr>'
  );
}
function renderEvaluations() {
  const members =
    currentRole === "Integrante"
      ? data.members.filter((m) => m.name === currentName)
      : data.members;
  const mine =
    data.members.find((m) => m.name === currentName) || data.members[0];
  return `${pageHeading(
    "Avaliações e AV3",
    "Acompanhe os três pilares avaliativos e a consolidação da nota final.",
    `${
      canCoordinate()
        ? '<button class="btn btn-secondary" data-action="plan-review">Revisar planos</button><button class="btn btn-primary" data-action="new-score">＋ Lançar avaliação</button>'
        : ""
    }`
  )}<section class="panel" style="margin-bottom:16px">${panelHead(
    "Composição da nota final",
    "Média ponderada conforme o regulamento do NITE 2026.2"
  )}<div class="pillar-grid"><div class="pillar-card"><small>PILAR 1 · DIÁRIO DE BORDO</small><strong>30%</strong><span>Documentação individual e evidências digitais</span></div><div class="pillar-card"><small>PILAR 2 · APRESENTAÇÕES</small><strong>20%</strong><span>Reuniões gerais e progresso semanal</span></div><div class="pillar-card"><small>PILAR 3 · ENTREGAS REAIS</small><strong>50%</strong><span>Resultados concretos produzidos nos projetos</span></div></div><div class="score-preview"><div><small>FÓRMULA APLICADA</small><b style="display:block;margin-top:5px">Nota final = (P1 × 0,30) + (P2 × 0,20) + (P3 × 0,50)</b></div><div><small>EXEMPLO · ${escapeHtml(
    mine.name
  )}</small><strong>${weighted(
    mine
  )} <span style="font-size:10px;color:#91a5bd">/ 10</span></strong></div></div><p style="font-size:9px;color:#8da1ba;margin:13px 0 0">Regra de negócio: nota zero em qualquer pilar configura risco de desligamento sumário conforme o regulamento descrito no documento.</p></section><section class="panel table-panel"><div class="table-toolbar"><div class="toolbar-left"><b style="font-size:11px">Notas individuais</b><span class="muted" style="font-size:10px">${
    members.length
  } integrante(s)</span></div>${
    canCoordinate()
      ? '<button class="btn btn-primary" data-action="new-score">Lançar notas</button>'
      : ""
  }</div><div class="table-wrap"><table class="data-table"><thead><tr><th>INTEGRANTE</th><th>P1 · 30%</th><th>P2 · 20%</th><th>P3 · 50%</th><th>NOTA FINAL</th><th>STATUS</th><th>AV3</th></tr></thead><tbody>${members
    .map(
      (m) =>
        `<tr><td><div class="table-name"><span class="mini-avatar">${initials(
          m.name
        )}</span><span><b>${escapeHtml(m.name)}</b><small>${escapeHtml(
          m.course
        )}</small></span></div></td><td>${Number(m.p1 || 0).toFixed(
          1
        )}</td><td>${Number(m.p2 || 0).toFixed(1)}</td><td>${Number(
          m.p3 || 0
        ).toFixed(1)}</td><td><b>${weighted(m)}</b></td><td>${
          Number(m.p1) === 0 || Number(m.p2) === 0 || Number(m.p3) === 0
            ? '<span class="pill red">Nota zero</span>'
            : Number(weighted(m)) >= 7
            ? '<span class="pill green">Regular</span>'
            : '<span class="pill yellow">Acompanhar</span>'
        }</td><td><button class="small-action" data-action="av3" data-id="${
          m.id
        }">Consolidar</button></td></tr>`
    )
    .join(
      ""
    )}</tbody></table></div></section><section class="panel" style="margin-top:16px">${panelHead(
    "Plano de Atividades Individual",
    "O plano precisa estar aprovado para liberar os registros do Diário de Bordo.",
    canCoordinate()
      ? '<button class="text-link" data-action="plan-review">Analisar solicitações →</button>'
      : ""
  )}<div class="task-list">${
    data.plans
      .filter((p) => currentRole !== "Integrante" || p.member === currentName)
      .map(
        (p) =>
          `<div class="task-item"><div class="task-symbol">▤</div><div class="task-copy"><b>${escapeHtml(
            p.member
          )}</b><small>${escapeHtml(
            p.objective
          )}</small><small style="margin-top:5px">Cronograma: ${escapeHtml(
            p.schedule
          )}</small></div><div class="task-meta">${statusPill(p.status)}${
            canCoordinate() && p.status !== "Aprovado"
              ? `<button class="small-action" data-action="approve-plan" data-id="${p.id}" style="margin-top:6px">Aprovar</button>`
              : ""
          }</div></div>`
      )
      .join("") ||
    '<div class="empty-state">Nenhum plano cadastrado para este perfil.</div>'
  }${
    currentRole === "Integrante"
      ? '<button class="btn btn-secondary btn-wide" data-action="new-plan">＋ Cadastrar/atualizar meu plano</button>'
      : ""
  }</div></section><section class="panel" style="margin-top:16px">${panelHead(
    "Substituição da AV3",
    "Fluxo de solicitação e convalidação acadêmica"
  )}<div class="timeline"><div class="timeline-item"><b>1. Indicação das disciplinas</b><p>O integrante de Computação indica até duas disciplinas regulares elegíveis.</p></div><div class="timeline-item"><b>2. Acompanhamento e cálculo</b><p>A nota final é calculada com base nos três pilares de avaliação.</p></div><div class="timeline-item"><b>3. Homologação pela coordenação</b><p>A coordenação revisa e homologa o formulário de convalidação. A emissão oficial em PDF requer integração de backend.</p></div></div><button class="btn btn-secondary" data-action="request-av3">Solicitar / consultar substituição AV3</button></section>`;
}
function renderReports() {
  return `${pageHeading(
    "Relatórios e dossiês",
    "Gere visões operacionais de projetos, frequência, atividades e desempenho.",
    `<button class="btn btn-secondary" data-action="export-csv">⇩ Exportar CSV</button><button class="btn btn-primary" data-action="print-report">Imprimir / salvar PDF</button>`
  )}<div class="stats-grid">${statCard(
    "Projetos",
    data.projects.length,
    "▦",
    "Incluídos no resumo"
  )}${statCard(
    "Atividades",
    data.activities.length,
    "☷",
    "Todas as situações"
  )}${statCard(
    "Integrantes",
    data.members.length,
    "♙",
    "Cadastro do semestre"
  )}${statCard(
    "Registros de presença",
    data.attendance.length,
    "◷",
    "Histórico registrado"
  )}</div><div class="dashboard-grid"><section class="panel">${panelHead(
    "Filtros do relatório",
    "Escolha o tipo de informação que deseja consultar"
  )}<div class="form-grid"><div class="field"><label>Tipo de relatório</label><select id="reportType" class="form-control"><option value="overview">Resumo gerencial</option><option value="projects">Projetos</option><option value="activities">Atividades</option><option value="members">Dossiê de integrantes</option><option value="attendance">Frequência</option><option value="scores">Avaliações</option></select></div><div class="field"><label>Projeto</label><select id="reportProject" class="form-control"><option value="">Todos os projetos</option>${data.projects
    .map((p) => `<option>${escapeHtml(p.name)}</option>`)
    .join(
      ""
    )}</select></div><div class="field"><label>Período inicial</label><input id="reportStart" type="date" value="2026-08-01" /></div><div class="field"><label>Período final</label><input id="reportEnd" type="date" value="2026-12-18" /></div></div><button class="btn btn-primary btn-wide" data-action="generate-report">Gerar relatório</button></section><section class="panel">${panelHead(
    "Resumo de indicadores",
    "Dados atuais registrados no protótipo"
  )}<div class="activity-list"><div class="activity-item"><div class="task-symbol">▦</div><div class="task-copy"><b>Projetos em andamento</b><small>${
    data.projects.filter((p) => p.status === "Ativo").length
  } projetos ativos no semestre</small></div></div><div class="activity-item"><div class="task-symbol">☷</div><div class="task-copy"><b>Conclusão de atividades</b><small>${Math.round(
    (data.activities.filter((a) => a.status === "Concluída").length /
      Math.max(1, data.activities.length)) *
      100
  )}% das atividades concluídas</small></div></div><div class="activity-item"><div class="task-symbol">◷</div><div class="task-copy"><b>Frequência média</b><small>${Math.round(
    data.members.reduce((s, m) => s + m.attendance, 0) /
      Math.max(1, data.members.length)
  )}% de assiduidade acumulada</small></div></div><div class="activity-item"><div class="task-symbol">☆</div><div class="task-copy"><b>Nota ponderada média</b><small>${(
    data.members.reduce((s, m) => s + Number(weighted(m)), 0) /
    Math.max(1, data.members.length)
  ).toFixed(
    1
  )} de 10 pontos</small></div></div></div></section></div><section id="reportResult" class="panel" style="margin-top:16px">${panelHead(
    "Prévia do relatório",
    "Selecione os filtros e clique em “Gerar relatório”."
  )}<div class="empty-state"><strong>Relatório ainda não gerado</strong>Os dados serão apresentados aqui e poderão ser exportados em CSV ou impressos em PDF.</div></section>`;
}
function renderNotifications() {
  return `${pageHeading(
    "Notificações",
    "Acompanhe lembretes, pendências e alertas gerados pelo sistema.",
    `<button class="btn btn-secondary" data-action="mark-all-read">Marcar todas como lidas</button>`
  )}<div class="dashboard-grid"><section class="panel">${panelHead(
    "Central de notificações",
    `${data.notifications.filter((n) => !n.read).length} não lida(s)`
  )}${data.notifications
    .map(
      (n) =>
        `<div class="notice-card ${
          !n.read ? "unread" : ""
        }"><div class="notice-icon">${
          n.type === "warning" ? "!" : "♧"
        }</div><div class="notice-content"><b>${escapeHtml(
          n.title
        )}</b><p>${escapeHtml(n.body)}</p><small>${escapeHtml(n.date)} · ${
          n.read ? "Lida" : "Não lida"
        }</small></div><div class="notice-actions">${
          !n.read
            ? `<button class="small-action" data-action="read-notification" data-id="${n.id}">Marcar lida</button>`
            : ""
        }</div></div>`
    )
    .join("")}</section><section class="panel">${panelHead(
    "Gatilhos automáticos",
    "Eventos previstos nos requisitos"
  )}<div class="timeline"><div class="timeline-item"><b>Apresentação geral do NITE</b><p>Lembrete às quintas-feiras pela manhã sobre a reunião das 17h.</p></div><div class="timeline-item"><b>Diário de Bordo pendente</b><p>Aviso quando o integrante não registra o diário após as sessões de trabalho.</p></div><div class="timeline-item"><b>Alerta crítico de desligamento</b><p>Notificação à coordenação e ao integrante em caso de quatro ausências consecutivas injustificadas ou risco de reprovação.</p></div></div><p class="muted" style="font-size:9px">No protótipo, as notificações são simuladas localmente. Disparos por e-mail e agendamentos exigem serviço de backend.</p></section></div>`;
}
function renderAudit() {
  return `${pageHeading(
    "Diário de auditoria",
    "Rastreabilidade de ações administrativas, alterações e justificativas.",
    `<button class="btn btn-secondary" data-action="export-audit">⇩ Exportar CSV</button>`
  )}<div class="notice-card unread"><div class="notice-icon">◈</div><div class="notice-content"><b>Registro de auditoria restrito</b><p>Conforme o RF-003, a consulta é exclusiva para os perfis Gestor Geral e Coordenador. Registros devem preservar autor, data/hora, estado anterior, novo estado e justificativa.</p></div></div><section class="panel table-panel"><div class="table-toolbar"><div class="toolbar-left"><b style="font-size:11px">Histórico de ações</b><span class="muted" style="font-size:10px">${
    data.audit.length
  } registro(s)</span></div><div class="toolbar-right"><input id="auditSearch" class="filter-select" placeholder="Buscar ação ou autor..." /></div></div><div class="table-wrap"><table class="data-table"><thead><tr><th>DATA/HORA</th><th>RESPONSÁVEL</th><th>AÇÃO</th><th>ALVO</th><th>JUSTIFICATIVA / DETALHE</th></tr></thead><tbody id="auditRows">${auditRows(
    data.audit
  )}</tbody></table></div></section>`;
}
function auditRows(items) {
  return (
    items
      .map(
        (a) =>
          `<tr><td>${escapeHtml(a.date)}</td><td>${escapeHtml(
            a.author
          )}</td><td>${escapeHtml(a.action)}</td><td>${escapeHtml(
            a.target
          )}</td><td>${escapeHtml(a.reason)}</td></tr>`
      )
      .join("") ||
    '<tr><td colspan="5"><div class="empty-state">Nenhum registro encontrado.</div></td></tr>'
  );
}
function openModal(title, subtitle, body, buttons = []) {
  const root = $("#modalRoot");
  root.innerHTML = `<div class="modal-backdrop" id="modalBackdrop"><div class="modal" role="dialog" aria-modal="true" aria-label="${escapeHtml(
    title
  )}"><div class="modal-header"><div><h2>${escapeHtml(
    title
  )}</h2><p>${escapeHtml(
    subtitle
  )}</p></div><button class="modal-close" id="modalClose" aria-label="Fechar">×</button></div><div class="modal-body">${body}</div><div class="modal-footer">${buttons
    .map(
      (b, i) =>
        `<button class="btn ${
          b.class || "btn-secondary"
        }" data-modal-btn="${i}">${escapeHtml(b.label)}</button>`
    )
    .join("")}</div></div></div>`;
  $("#modalClose").onclick = closeModal;
  $("#modalBackdrop").addEventListener("click", (e) => {
    if (e.target.id === "modalBackdrop") closeModal();
  });
  $$("[data-modal-btn]", root).forEach((el) =>
    el.addEventListener("click", () =>
      buttons[Number(el.dataset.modalBtn)].action()
    )
  );
}
function closeModal() {
  $("#modalRoot").innerHTML = "";
}
function field(
  name,
  label,
  value = "",
  type = "text",
  required = true,
  extra = ""
) {
  return `<div class="field"><label for="f_${name}">${label}${
    required ? " *" : ""
  }</label><input id="f_${name}" name="${name}" type="${type}" value="${escapeHtml(
    value
  )}" ${required ? "required" : ""} ${extra}/></div>`;
}
function selectField(name, label, options, value = "", required = true) {
  return `<div class="field"><label for="f_${name}">${label}${
    required ? " *" : ""
  }</label><select id="f_${name}" name="${name}" ${
    required ? "required" : ""
  }>${options
    .map(
      (o) =>
        `<option value="${escapeHtml(o)}" ${
          o === value ? "selected" : ""
        }>${escapeHtml(o)}</option>`
    )
    .join("")}</select></div>`;
}
function textAreaField(name, label, value = "", required = true) {
  return `<div class="field full"><label for="f_${name}">${label}${
    required ? " *" : ""
  }</label><textarea id="f_${name}" name="${name}" ${
    required ? "required" : ""
  }>${escapeHtml(value)}</textarea></div>`;
}
function getVal(name) {
  return $(`#f_${name}`)?.value?.trim() || "";
}
function formButtons(onSave, label = "Salvar") {
  return [
    { label: "Cancelar", class: "btn-ghost", action: closeModal },
    { label, class: "btn-primary", action: onSave },
  ];
}
function newProject(p = null) {
  if (!canManage()) {
    toast(
      "Acesso restrito",
      "Seu perfil não pode cadastrar projetos.",
      "error"
    );
    return;
  }
  openModal(
    p ? "Editar projeto" : "Cadastrar projeto",
    "Informe os dados principais do projeto.",
    `<form id="entityForm" class="form-grid">${field(
      "name",
      "Nome do projeto",
      p?.name || ""
    )}${selectField(
      "manager",
      "Gestor responsável",
      data.members.map((m) => m.name),
      p?.manager || currentName
    )}${field(
      "start",
      "Data de início",
      p?.start || "2026-10-27",
      "date"
    )}${field(
      "end",
      "Data de término",
      p?.end || "2026-12-18",
      "date"
    )}${selectField(
      "status",
      "Situação",
      ["Proposto", "Ativo", "Em Revisão", "Concluído", "Cancelado"],
      p?.status || "Proposto"
    )}${field(
      "progress",
      "Progresso (%)",
      p?.progress ?? 0,
      "number",
      true,
      'min="0" max="100"'
    )}${textAreaField(
      "description",
      "Descrição funcional",
      p?.description || ""
    )}${textAreaField("scope", "Escopo do projeto", p?.scope || "")}</form>`,
    formButtons(() => {
      const name = getVal("name"),
        manager = getVal("manager"),
        start = getVal("start"),
        end = getVal("end"),
        progressN = Number(getVal("progress"));
      if (
        !name ||
        !manager ||
        !start ||
        !end ||
        end < start ||
        progressN < 0 ||
        progressN > 100
      ) {
        toast(
          "Revise os campos",
          "Preencha os campos obrigatórios e informe um período e progresso válidos.",
          "error"
        );
        return;
      }
      const obj = {
        id: p?.id || Date.now(),
        name,
        manager,
        start,
        end,
        status: getVal("status"),
        progress: progressN,
        description: getVal("description"),
        scope: getVal("scope"),
        members: p?.members || 1,
      };
      if (p) {
        data.projects = data.projects.map((x) => (x.id === p.id ? obj : x));
        logAudit("Edição de projeto", name, "Dados do projeto atualizados.");
      } else {
        data.projects.unshift(obj);
        logAudit(
          "Cadastro de projeto",
          name,
          "Projeto cadastrado com gestor responsável."
        );
      }
      persist();
      closeModal();
      renderPage();
      toast("Projeto salvo", `“${name}” foi salvo com sucesso.`);
    })
  );
}
function newActivity(a = null) {
  if (!canManage()) {
    toast(
      "Acesso restrito",
      "Apenas gestores e coordenação podem cadastrar atividades.",
      "error"
    );
    return;
  }
  const names = data.members.map((m) => m.name);
  const projects = data.projects.map((p) => p.name);
  openModal(
    a ? "Editar atividade" : "Cadastrar atividade",
    "Vincule a atividade a um projeto e defina o responsável e prazo.",
    `<form id="entityForm" class="form-grid">${field(
      "title",
      "Título da atividade",
      a?.title || ""
    )}${selectField(
      "project",
      "Projeto",
      projects,
      a?.project || projects[0] || ""
    )}${selectField(
      "assignee",
      "Integrante responsável",
      names,
      a?.assignee || currentName
    )}${field(
      "due",
      "Prazo limite",
      a?.due || "2026-11-15",
      "date"
    )}${selectField(
      "status",
      "Situação",
      ["Planejada", "Em Andamento", "Em Revisão", "Concluída", "Atrasada"],
      a?.status || "Planejada"
    )}${selectField(
      "priority",
      "Prioridade",
      ["Baixa", "Média", "Alta"],
      a?.priority || "Média"
    )}${textAreaField(
      "description",
      "Escopo / descrição",
      a?.description || ""
    )}</form><p class="muted" style="font-size:9px;margin:10px 0 0">Regra RF-007: o prazo não pode ser anterior à data atual nem ultrapassar o término do semestre vigente.</p>`,
    formButtons(() => {
      const title = getVal("title"),
        project = getVal("project"),
        assignee = getVal("assignee"),
        due = getVal("due");
      if (
        !title ||
        !project ||
        !assignee ||
        !due ||
        due < "2026-10-27" ||
        due > "2026-12-18"
      ) {
        toast(
          "Não foi possível salvar",
          "Informe os campos obrigatórios e um prazo entre 27/10/2026 e 18/12/2026.",
          "error"
        );
        return;
      }
      const obj = {
        id: a?.id || Date.now(),
        title,
        project,
        assignee,
        due,
        status: getVal("status"),
        priority: getVal("priority"),
        description: getVal("description"),
      };
      if (a) {
        data.activities = data.activities.map((x) => (x.id === a.id ? obj : x));
        logAudit("Edição de atividade", title, "Atividade atualizada.");
      } else {
        data.activities.unshift(obj);
        logAudit(
          "Cadastro de atividade",
          title,
          "Atividade vinculada a um projeto e integrante."
        );
      }
      persist();
      closeModal();
      renderPage();
      toast("Atividade salva", `“${title}” foi registrada.`);
    })
  );
}
function newDiary() {
  const member = data.members.find((m) => m.name === currentName);
  if (currentRole === "Integrante" && member?.plan !== "Aprovado") {
    toast(
      "Plano individual pendente",
      "O Diário de Bordo só pode ser enviado após aprovação do plano individual.",
      "error"
    );
    return;
  }
  const tasks = data.activities.filter(
    (a) => currentRole !== "Integrante" || a.assignee === currentName
  );
  if (!tasks.length) {
    toast(
      "Nenhuma atividade disponível",
      "Cadastre ou solicite uma atividade antes de registrar execução.",
      "error"
    );
    return;
  }
  openModal(
    "Novo registro de Diário de Bordo",
    "Descreva o progresso e inclua uma evidência digital obrigatória.",
    `<form id="entityForm" class="form-grid">${selectField(
      "activity",
      "Atividade vinculada",
      tasks.map((a) => a.title),
      tasks[0].title
    )}${field("date", "Data do registro", "2026-10-27", "date")}${textAreaField(
      "description",
      "Descrição do progresso",
      ""
    )}<div class="field full"><label for="f_evidence">Link de evidência técnica *</label><input id="f_evidence" type="url" required placeholder="https://github.com/..."/><small>Informe um link para commit, repositório, relatório ou evidência técnica verificável.</small></div></form>`,
    formButtons(() => {
      const activity = getVal("activity"),
        date = getVal("date"),
        description = getVal("description"),
        evidence = $("#f_evidence").value.trim();
      if (
        !activity ||
        !date ||
        !description ||
        !evidence ||
        !/^https?:\/\//i.test(evidence)
      ) {
        toast(
          "Campos obrigatórios",
          "Informe a atividade, descrição e um link de evidência válido.",
          "error"
        );
        return;
      }
      const a = tasks.find((x) => x.title === activity);
      data.diary.unshift({
        id: Date.now(),
        member: currentName,
        activity,
        project: a.project,
        date,
        description,
        evidence,
        status: "Enviado",
      });
      logAudit(
        "Envio de Diário de Bordo",
        activity,
        "Registro enviado com evidência digital."
      );
      persist();
      closeModal();
      renderPage();
      toast(
        "Diário enviado",
        "Seu progresso foi registrado com evidência digital."
      );
    })
  );
}
function newMember(m = null) {
  if (!canManage()) {
    toast(
      "Acesso restrito",
      "Seu perfil não pode cadastrar integrantes.",
      "error"
    );
    return;
  }
  openModal(
    m ? "Editar integrante" : "Cadastrar integrante",
    "Cadastre dados de identificação e perfil técnico.",
    `<form id="entityForm" class="form-grid">${field(
      "name",
      "Nome completo",
      m?.name || ""
    )}${field("email", "E-mail institucional", m?.email || "", "email")}${field(
      "course",
      "Curso",
      m?.course || ""
    )}${selectField(
      "role",
      "Perfil de acesso",
      ["Integrante", "Gestor do Projeto", "Gestor Geral", "Coordenador"],
      m?.role || "Integrante"
    )}${selectField(
      "tech",
      "Papel técnico",
      [
        "Programação",
        "Sistemas Embarcados",
        "Eletrônica",
        "Hardware",
        "Modelagem Mecânica/3D",
        "Robótica",
        "Mídias Sociais",
        "DataCenter",
        "Automação",
        "Pesquisa",
        "Documentação Técnica",
        "Gestor do Projeto",
      ],
      m?.tech || "Programação"
    )}${selectField(
      "status",
      "Situação",
      ["Ativo", "Pendente", "Inativo"],
      m?.status || "Ativo"
    )}</form>`,
    formButtons(() => {
      const name = getVal("name"),
        email = getVal("email"),
        course = getVal("course");
      if (!name || !email.includes("@") || !course) {
        toast(
          "Revise os campos",
          "Nome, e-mail válido e curso são obrigatórios.",
          "error"
        );
        return;
      }
      const obj = {
        ...(m || {}),
        id: m?.id || Date.now(),
        name,
        email,
        course,
        role: getVal("role"),
        tech: getVal("tech"),
        status: getVal("status"),
        attendance: m?.attendance ?? 100,
        p1: m?.p1 ?? 0,
        p2: m?.p2 ?? 0,
        p3: m?.p3 ?? 0,
        plan: m?.plan || "Pendente",
      };
      if (m) data.members = data.members.map((x) => (x.id === m.id ? obj : x));
      else data.members.push(obj);
      logAudit(m ? "Edição de integrante" : "Cadastro de integrante", name);
      persist();
      closeModal();
      renderPage();
      toast("Integrante salvo", `${name} foi salvo com sucesso.`);
    })
  );
}
function viewProject(p) {
  const tasks = data.activities.filter((a) => a.project === p.name);
  openModal(
    p.name,
    "Detalhes do projeto e atividades relacionadas.",
    `<div class="stats-grid" style="grid-template-columns:repeat(2,minmax(0,1fr))">${statCard(
      "Progresso",
      `${p.progress}%`,
      "↗",
      "Andamento registrado"
    )}${statCard(
      "Integrantes",
      p.members,
      "♙",
      "Equipe vinculada"
    )}</div><p style="font-size:11px;color:#a9bad0">${escapeHtml(
      p.description
    )}</p><p style="font-size:10px;color:#8296af">Escopo: ${escapeHtml(
      p.scope || "Não informado"
    )}</p><p style="font-size:10px;color:#8296af">Responsável: ${escapeHtml(
      p.manager
    )} · Período: ${fmtDateLong(p.start)} até ${fmtDateLong(
      p.end
    )}</p><h3 style="font-size:11px;margin-top:20px">Atividades do projeto</h3>${
      tasks
        .map(
          (a) =>
            `<div class="task-item"><div class="task-copy"><b>${escapeHtml(
              a.title
            )}</b><small>${escapeHtml(a.assignee)} · Prazo ${fmtDate(
              a.due
            )}</small></div>${statusPill(a.status)}</div>`
        )
        .join("") || '<p class="muted">Nenhuma atividade vinculada.</p>'
    }`,
    [
      { label: "Fechar", class: "btn-ghost", action: closeModal },
      ...(canManage()
        ? [
            {
              label: "Editar projeto",
              class: "btn-primary",
              action: () => {
                closeModal();
                newProject(p);
              },
            },
          ]
        : []),
    ]
  );
}
function viewActivity(a) {
  openModal(
    a.title,
    "Detalhes da atividade.",
    `<div class="form-grid"><div class="field"><label>Projeto</label><div>${escapeHtml(
      a.project
    )}</div></div><div class="field"><label>Responsável</label><div>${escapeHtml(
      a.assignee
    )}</div></div><div class="field"><label>Prazo</label><div>${fmtDateLong(
      a.due
    )}</div></div><div class="field"><label>Situação</label><div>${statusPill(
      a.status
    )}</div></div><div class="field full"><label>Descrição</label><p style="margin:0;color:#a9bad0">${escapeHtml(
      a.description
    )}</p></div></div><h3 style="font-size:11px;margin-top:20px">Diário de Bordo vinculado</h3>${
      data.diary
        .filter((d) => d.activity === a.title)
        .map(
          (d) =>
            `<div class="task-item"><div class="task-copy"><b>${escapeHtml(
              d.member
            )} · ${fmtDate(d.date)}</b><small>${escapeHtml(
              d.description
            )}</small><a href="${escapeHtml(
              d.evidence
            )}" target="_blank" rel="noopener" style="color:#78adff;font-size:10px">Abrir evidência ↗</a></div></div>`
        )
        .join("") ||
      '<p class="muted" style="font-size:10px">Nenhum Diário de Bordo vinculado até o momento.</p>'
    }`,
    [
      { label: "Fechar", class: "btn-ghost", action: closeModal },
      ...(canManage() || a.assignee === currentName
        ? [
            {
              label: "Editar atividade",
              class: "btn-primary",
              action: () => {
                closeModal();
                newActivity(a);
              },
            },
          ]
        : []),
    ]
  );
}
function viewMember(m) {
  const attendance = data.attendance.filter((a) => a.member === m.name),
    diaries = data.diary.filter((d) => d.member === m.name);
  openModal(
    `Histórico · ${m.name}`,
    "Dossiê individual de acompanhamento.",
    `<div class="stats-grid" style="grid-template-columns:repeat(2,minmax(0,1fr))">${statCard(
      "Frequência",
      `${m.attendance}%`,
      "◷",
      "Assiduidade acumulada"
    )}${statCard(
      "Nota ponderada",
      weighted(m),
      "☆",
      "Resultado atual"
    )}</div><div class="timeline"><div class="timeline-item"><b>Cadastro do integrante</b><p>${escapeHtml(
      m.course
    )} · ${escapeHtml(m.tech)}</p><small>Perfil: ${escapeHtml(
      m.role
    )} · Situação: ${escapeHtml(
      m.status
    )}</small></div><div class="timeline-item"><b>Plano individual</b><p>Status: ${escapeHtml(
      m.plan || "Pendente"
    )}</p></div><div class="timeline-item"><b>Registros de Diário de Bordo (${
      diaries.length
    })</b><p>${
      diaries.map((d) => `${fmtDate(d.date)} — ${d.activity}`).join("<br>") ||
      "Sem registros de diário."
    }</p></div><div class="timeline-item"><b>Registros de frequência (${
      attendance.length
    })</b><p>${
      attendance.map((a) => `${fmtDate(a.date)} — ${a.status}`).join("<br>") ||
      "Consulte as chamadas para o histórico completo."
    }</p></div><div class="timeline-item"><b>Notas dos pilares</b><p>P1: ${
      m.p1
    } · P2: ${m.p2} · P3: ${m.p3}</p></div></div>`,
    [{ label: "Fechar", class: "btn-ghost", action: closeModal }]
  );
}
function newScore() {
  if (!canCoordinate()) {
    toast(
      "Acesso restrito",
      "Somente a Coordenação pode lançar ou alterar notas.",
      "error"
    );
    return;
  }
  openModal(
    "Lançar avaliação",
    "Registre notas de 0 a 10 para os três pilares.",
    `<form id="entityForm" class="form-grid">${selectField(
      "member",
      "Integrante",
      data.members.map((m) => m.name),
      data.members[0]?.name || ""
    )}${field(
      "p1",
      "Pilar 1 · Diário de Bordo (30%)",
      data.members[0]?.p1 ?? 0,
      "number",
      true,
      'min="0" max="10" step="0.1"'
    )}${field(
      "p2",
      "Pilar 2 · Apresentações (20%)",
      data.members[0]?.p2 ?? 0,
      "number",
      true,
      'min="0" max="10" step="0.1"'
    )}${field(
      "p3",
      "Pilar 3 · Entregas reais (50%)",
      data.members[0]?.p3 ?? 0,
      "number",
      true,
      'min="0" max="10" step="0.1"'
    )}</form><p class="muted" style="font-size:10px">Nota final calculada automaticamente: (P1 × 0,30) + (P2 × 0,20) + (P3 × 0,50).</p>`,
    formButtons(() => {
      const m = data.members.find((x) => x.name === getVal("member")),
        p1 = Number(getVal("p1")),
        p2 = Number(getVal("p2")),
        p3 = Number(getVal("p3"));
      if (
        !m ||
        [p1, p2, p3].some((n) => !Number.isFinite(n) || n < 0 || n > 10)
      ) {
        toast(
          "Notas inválidas",
          "Informe notas entre 0 e 10 em todos os pilares.",
          "error"
        );
        return;
      }
      if (!m.plan || m.plan !== "Aprovado") {
        toast(
          "Plano não aprovado",
          "O plano individual deve estar aprovado antes do lançamento de notas.",
          "error"
        );
        return;
      }
      const before = `P1=${m.p1}, P2=${m.p2}, P3=${m.p3}`;
      m.p1 = p1;
      m.p2 = p2;
      m.p3 = p3;
      logAudit(
        "Alteração de notas",
        m.name,
        `Estado anterior: ${before}. Novo estado: P1=${p1}, P2=${p2}, P3=${p3}.`
      );
      persist();
      closeModal();
      renderPage();
      toast(
        "Avaliação registrada",
        `Nota final calculada: ${weighted(m)} / 10.`
      );
    })
  );
}
function reviewPlans() {
  if (!canCoordinate()) {
    toast(
      "Acesso restrito",
      "Somente a Coordenação pode aprovar planos individuais.",
      "error"
    );
    return;
  }
  openModal(
    "Análise de planos individuais",
    "A aprovação libera o envio do Diário de Bordo e lançamento de notas.",
    `${
      data.plans
        .map(
          (p) =>
            `<div class="notice-card"><div class="notice-icon">▤</div><div class="notice-content"><b>${escapeHtml(
              p.member
            )}</b><p>${escapeHtml(p.objective)}</p><small>${escapeHtml(
              p.schedule
            )}</small></div><div class="notice-actions">${statusPill(
              p.status
            )}${
              p.status !== "Aprovado"
                ? `<button class="small-action" data-approve-plan="${p.id}">Aprovar</button>`
                : ""
            }</div></div>`
        )
        .join("") || '<p class="muted">Nenhum plano cadastrado.</p>'
    }`,
    [{ label: "Fechar", class: "btn-primary", action: closeModal }]
  );
  $$("[data-approve-plan]").forEach((b) =>
    b.addEventListener("click", () =>
      approvePlan(Number(b.dataset.approvePlan))
    )
  );
}
function approvePlan(id) {
  if (!canCoordinate()) return;
  const p = data.plans.find((x) => x.id === id);
  if (!p) return;
  p.status = "Aprovado";
  const m = data.members.find((x) => x.name === p.member);
  if (m) m.plan = "Aprovado";
  logAudit(
    "Aprovação de plano individual",
    p.member,
    "Plano individual aprovado pela coordenação."
  );
  persist();
  closeModal();
  renderPage();
  toast("Plano aprovado", `O plano de ${p.member} foi aprovado.`);
}
function newPlan() {
  const m = data.members.find((x) => x.name === currentName);
  if (!m) {
    toast(
      "Integrante não encontrado",
      "Confira o perfil selecionado.",
      "error"
    );
    return;
  }
  const p = data.plans.find((x) => x.member === currentName);
  openModal(
    p ? "Atualizar plano individual" : "Cadastrar plano individual",
    "Informe objetivos técnicos e cronograma inicial.",
    `<form id="entityForm" class="form-grid">${textAreaField(
      "objective",
      "Objetivos técnicos",
      p?.objective || ""
    )}${textAreaField(
      "schedule",
      "Cronograma e entregas",
      p?.schedule || ""
    )}</form><p class="muted" style="font-size:9px">Após a atualização, o plano ficará pendente de aprovação da coordenação.</p>`,
    formButtons(() => {
      const objective = getVal("objective"),
        schedule = getVal("schedule");
      if (!objective || !schedule) {
        toast(
          "Campos obrigatórios",
          "Preencha objetivos técnicos e cronograma.",
          "error"
        );
        return;
      }
      if (p) {
        p.objective = objective;
        p.schedule = schedule;
        p.status = "Pendente";
      } else
        data.plans.push({
          id: Date.now(),
          member: currentName,
          objective,
          schedule,
          status: "Pendente",
        });
      m.plan = "Pendente";
      logAudit(
        "Envio de plano individual",
        currentName,
        "Plano enviado para aprovação da coordenação."
      );
      persist();
      closeModal();
      renderPage();
      toast(
        "Plano enviado",
        "O plano foi encaminhado para análise da coordenação."
      );
    })
  );
}
function requestAV3(m) {
  if (!m) m = data.members.find((x) => x.name === currentName);
  if (!m) {
    toast(
      "Integrante não encontrado",
      "Selecione um integrante válido.",
      "error"
    );
    return;
  }
  openModal(
    "Substituição da AV3",
    "Registre disciplinas elegíveis e consulte a nota consolidada.",
    `<div class="notice-card"><div class="notice-icon">i</div><div class="notice-content"><b>Regra de elegibilidade</b><p>A substituição da AV3 é obrigatória para estudantes dos cursos de Computação, conforme o documento de requisitos. O protótipo não realiza homologação oficial.</p></div></div><div class="form-grid">${field(
      "av3Member",
      "Integrante",
      m.name
    )}${field(
      "av3Score",
      "Nota consolidada",
      weighted(m),
      "number",
      true,
      "readonly"
    )}${field("discipline1", "Disciplina 1", "")}${field(
      "discipline2",
      "Disciplina 2",
      "",
      "text",
      false
    )}</div><p class="muted" style="font-size:10px">Estado atual do processo: Solicitada. A homologação e geração do PDF assinado dependem da integração institucional.</p>`,
    formButtons(() => {
      const d1 = getVal("discipline1"),
        d2 = getVal("discipline2");
      if (!d1) {
        toast(
          "Disciplina obrigatória",
          "Informe ao menos uma disciplina elegível.",
          "error"
        );
        return;
      }
      if (Number(m.p1) === 0 || Number(m.p2) === 0 || Number(m.p3) === 0) {
        toast(
          "Atenção: nota zero",
          "O documento prevê desligamento sumário em caso de nota zero em qualquer pilar.",
          "error"
        );
        return;
      }
      logAudit(
        "Solicitação de substituição AV3",
        m.name,
        `Disciplinas indicadas: ${d1}${
          d2 ? ` e ${d2}` : ""
        }. Nota calculada ${weighted(m)}.`
      );
      persist();
      closeModal();
      toast(
        "Solicitação registrada",
        "A indicação foi registrada no protótipo. A homologação oficial é feita pela coordenação."
      );
    }, "Registrar solicitação")
  );
}
function justifyAbsence() {
  openModal(
    "Justificativa de ausência",
    "Envie a justificativa para análise da gestão e coordenação.",
    `<form id="entityForm" class="form-grid">${field(
      "absenceDate",
      "Data da ausência",
      "2026-10-22",
      "date"
    )}${selectField(
      "absenceType",
      "Ocorrência",
      ["Falta", "Saída antecipada"],
      "Falta"
    )}${textAreaField(
      "absenceReason",
      "Justificativa",
      ""
    )}<div class="field full"><label for="absenceFile">Atestado / documento (PDF ou imagem, até 5 MB)</label><input id="absenceFile" type="file" accept="application/pdf,image/*"/><small>O arquivo é validado no navegador para fins de demonstração. O envio seguro ao servidor ainda precisa ser implementado.</small></div></form>`,
    formButtons(() => {
      const date = getVal("absenceDate"),
        reason = getVal("absenceReason"),
        file = $("#absenceFile").files[0];
      if (!date || !reason) {
        toast(
          "Campos obrigatórios",
          "Informe a data e a justificativa.",
          "error"
        );
        return;
      }
      if (
        file &&
        (file.size > 5 * 1024 * 1024 || !/pdf|image\//.test(file.type))
      ) {
        toast(
          "Arquivo inválido",
          "Anexe um PDF ou imagem de até 5 MB.",
          "error"
        );
        return;
      }
      data.notifications.unshift({
        id: Date.now(),
        title: "Justificativa enviada para análise",
        body: `${currentName}: ${reason}`,
        date: "Agora",
        read: false,
        type: "info",
      });
      logAudit(
        "Submissão de justificativa",
        currentName,
        `Ocorrência: ${getVal("absenceType")} em ${date}.`
      );
      persist();
      closeModal();
      renderPage();
      toast(
        "Justificativa enviada",
        "A gestão poderá analisar a solicitação. A falta só será abonada após aprovação."
      );
    }),
    "Enviar justificativa"
  );
}
function openSession() {
  if (!canManageAttendance()) {
    toast(
      "Acesso restrito",
      "Somente Gestor Geral e Coordenador podem abrir a sessão.",
      "error"
    );
    return;
  }
  data.sessionOpen = !data.sessionOpen;
  persist();
  logAudit(
    data.sessionOpen
      ? "Abertura de sessão de presença"
      : "Encerramento de sessão de presença",
    "Sessão presencial",
    `PIN de demonstração: ${data.sessionPin}`
  );
  renderPage();
  toast(
    data.sessionOpen ? "Sessão aberta" : "Sessão encerrada",
    data.sessionOpen
      ? `PIN ativo: ${data.sessionPin}`
      : "O check-in por PIN está desativado."
  );
}
function saveAttendance() {
  if (!canManageAttendance()) {
    toast(
      "Acesso restrito",
      "Seu perfil não pode salvar a chamada em lote.",
      "error"
    );
    return;
  }
  Object.entries(attendanceDraft).forEach(([name, status]) => {
    const rec = data.attendance.find(
      (a) => a.date === "2026-10-22" && a.member === name
    );
    if (rec) {
      rec.status = status;
      rec.hours = status === "Presente" ? 2.5 : status === "Abonado" ? 2.5 : 0;
    } else
      data.attendance.push({
        id: Date.now() + Math.random(),
        date: "2026-10-22",
        session: "Reunião geral presencial",
        member: name,
        status,
        hours: status === "Presente" || status === "Abonado" ? 2.5 : 0,
      });
    const m = data.members.find((x) => x.name === name);
    if (m) {
      const records = data.attendance.filter((a) => a.member === name);
      m.attendance = Math.round(
        (records.filter(
          (a) => a.status === "Presente" || a.status === "Abonado"
        ).length /
          Math.max(1, records.length)) *
          100
      );
    }
  });
  logAudit(
    "Salvamento de chamada em lote",
    "Reunião geral 22/10/2026",
    `${Object.keys(attendanceDraft).length} registros alterados.`
  );
  attendanceDraft = {};
  persist();
  renderPage();
  toast("Chamada salva", "Os registros de presença foram atualizados.");
}
function checkin() {
  const pin = $("#checkinPin")?.value.trim();
  if (!data.sessionOpen) {
    toast(
      "Sessão não disponível",
      "A gestão ainda não abriu uma sessão para check-in.",
      "error"
    );
    return;
  }
  if (pin !== data.sessionPin) {
    toast(
      "PIN inválido",
      "Confira o código disponibilizado pelo Gestor Geral.",
      "error"
    );
    return;
  }
  if (
    data.attendance.some(
      (a) =>
        a.date === "2026-10-27" &&
        a.member === currentName &&
        a.session === "Self check-in"
    )
  ) {
    toast(
      "Check-in já realizado",
      "Você já registrou presença nesta sessão.",
      "error"
    );
    return;
  }
  data.attendance.push({
    id: Date.now(),
    date: "2026-10-27",
    session: "Self check-in",
    member: currentName,
    status: "Presente",
    hours: 2.5,
  });
  logAudit(
    "Self check-in confirmado",
    currentName,
    "Check-in registrado por PIN de sessão."
  );
  persist();
  renderPage();
  toast("Presença confirmada", "Seu check-in foi registrado com sucesso.");
}
function markAllRead() {
  data.notifications.forEach((n) => (n.read = true));
  persist();
  renderPage();
  toast(
    "Notificações atualizadas",
    "Todas as notificações foram marcadas como lidas."
  );
}
function readNotification(id) {
  const n = data.notifications.find((x) => x.id === id);
  if (n) n.read = true;
  persist();
  renderPage();
  toast("Notificação atualizada", "A notificação foi marcada como lida.");
}
function csvEscape(v) {
  return '"' + String(v ?? "").replace(/"/g, '""') + '"';
}
function downloadCSV(filename, rows) {
  const csv =
    "\ufeff" + rows.map((row) => row.map(csvEscape).join(";")).join("\r\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
  toast("Exportação iniciada", `${filename} foi preparado para download.`);
}
function exportCSV() {
  const type = $("#reportType")?.value || "overview",
    project = $("#reportProject")?.value || "";
  let rows = [];
  if (type === "projects")
    rows = [
      [
        "Projeto",
        "Descrição",
        "Gestor",
        "Início",
        "Término",
        "Status",
        "Progresso",
      ],
      ...data.projects
        .filter((p) => !project || p.name === project)
        .map((p) => [
          p.name,
          p.description,
          p.manager,
          p.start,
          p.end,
          p.status,
          p.progress,
        ]),
    ];
  else if (type === "activities")
    rows = [
      ["Atividade", "Projeto", "Responsável", "Prazo", "Prioridade", "Status"],
      ...data.activities
        .filter((a) => !project || a.project === project)
        .map((a) => [
          a.title,
          a.project,
          a.assignee,
          a.due,
          a.priority,
          a.status,
        ]),
    ];
  else if (type === "members")
    rows = [
      [
        "Integrante",
        "E-mail",
        "Curso",
        "Papel técnico",
        "Frequência",
        "P1",
        "P2",
        "P3",
        "Nota final",
      ],
      ...data.members.map((m) => [
        m.name,
        m.email,
        m.course,
        m.tech,
        m.attendance,
        m.p1,
        m.p2,
        m.p3,
        weighted(m),
      ]),
    ];
  else if (type === "attendance")
    rows = [
      ["Data", "Sessão", "Integrante", "Status", "Horas"],
      ...data.attendance.map((a) => [
        a.date,
        a.session,
        a.member,
        a.status,
        a.hours,
      ]),
    ];
  else if (type === "scores")
    rows = [
      ["Integrante", "P1", "P2", "P3", "Nota final"],
      ...data.members.map((m) => [m.name, m.p1, m.p2, m.p3, weighted(m)]),
    ];
  else
    rows = [
      ["Indicador", "Valor"],
      ["Projetos", data.projects.length],
      ["Atividades", data.activities.length],
      ["Integrantes", data.members.length],
      [
        "Frequência média",
        Math.round(
          data.members.reduce((s, m) => s + m.attendance, 0) /
            data.members.length
        ) + "%",
      ],
      [
        "Média ponderada",
        (
          data.members.reduce((s, m) => s + Number(weighted(m)), 0) /
          data.members.length
        ).toFixed(1),
      ],
    ];
  downloadCSV(`nite-connect-${type}.csv`, rows);
}
function generateReport() {
  const type = $("#reportType").value,
    project = $("#reportProject").value,
    start = $("#reportStart").value,
    end = $("#reportEnd").value;
  let title = "Resumo gerencial",
    rows = [];
  if (type === "projects") {
    title = "Relatório de projetos";
    rows = data.projects
      .filter(
        (p) =>
          (!project || p.name === project) &&
          (!start || p.start >= start) &&
          (!end || p.end <= end)
      )
      .map(
        (p) =>
          `<tr><td>${escapeHtml(p.name)}</td><td>${escapeHtml(
            p.manager
          )}</td><td>${statusPill(p.status)}</td><td>${p.progress}%</td></tr>`
      );
    rows = `<table class="data-table"><thead><tr><th>Projeto</th><th>Gestor</th><th>Status</th><th>Progresso</th></tr></thead><tbody>${rows.join(
      ""
    )}</tbody></table>`;
  } else if (type === "activities") {
    title = "Relatório de atividades";
    rows = data.activities
      .filter(
        (a) =>
          (!project || a.project === project) &&
          (!start || a.due >= start) &&
          (!end || a.due <= end)
      )
      .map(
        (a) =>
          `<tr><td>${escapeHtml(a.title)}</td><td>${escapeHtml(
            a.project
          )}</td><td>${escapeHtml(a.assignee)}</td><td>${fmtDate(
            a.due
          )}</td><td>${statusPill(a.status)}</td></tr>`
      );
    rows = `<table class="data-table"><thead><tr><th>Atividade</th><th>Projeto</th><th>Responsável</th><th>Prazo</th><th>Status</th></tr></thead><tbody>${rows.join(
      ""
    )}</tbody></table>`;
  } else if (type === "members" || type === "scores") {
    title =
      type === "scores" ? "Relatório de avaliações" : "Dossiê de integrantes";
    rows = `<table class="data-table"><thead><tr><th>Integrante</th><th>Frequência</th><th>P1</th><th>P2</th><th>P3</th><th>Nota final</th></tr></thead><tbody>${data.members
      .map(
        (m) =>
          `<tr><td>${escapeHtml(m.name)}</td><td>${m.attendance}%</td><td>${
            m.p1
          }</td><td>${m.p2}</td><td>${m.p3}</td><td>${weighted(m)}</td></tr>`
      )
      .join("")}</tbody></table>`;
  } else if (type === "attendance") {
    title = "Relatório de frequência";
    rows = `<table class="data-table"><thead><tr><th>Data</th><th>Integrante</th><th>Sessão</th><th>Status</th><th>Horas</th></tr></thead><tbody>${data.attendance
      .filter((a) => (!start || a.date >= start) && (!end || a.date <= end))
      .map(
        (a) =>
          `<tr><td>${fmtDate(a.date)}</td><td>${escapeHtml(
            a.member
          )}</td><td>${escapeHtml(a.session)}</td><td>${statusPill(
            a.status
          )}</td><td>${a.hours}</td></tr>`
      )
      .join("")}</tbody></table>`;
  } else {
    rows = `<div class="stats-grid">${statCard(
      "Projetos",
      data.projects.length,
      "▦",
      "Total cadastrado"
    )}${statCard(
      "Atividades",
      data.activities.length,
      "☷",
      "Total cadastrado"
    )}${statCard(
      "Integrantes",
      data.members.length,
      "♙",
      "Total cadastrado"
    )}${statCard(
      "Média de frequência",
      Math.round(
        data.members.reduce((s, m) => s + m.attendance, 0) / data.members.length
      ) + "%",
      "◷",
      "Assiduidade média"
    )}</div>`;
  }
  $("#reportResult").innerHTML = `${panelHead(
    title,
    `Período: ${start || "início"} até ${end || "atual"}${
      project ? " · Projeto: " + escapeHtml(project) : ""
    }`
  )}${
    rows ||
    '<div class="empty-state">Nenhum registro corresponde aos filtros selecionados.</div>'
  }`;
  toast(
    "Relatório gerado",
    "A prévia foi atualizada com os filtros selecionados."
  );
}
function printReport() {
  window.print();
}
function bindPageEvents() {
  const root = $("#pageContent");
  root.onclick = (e) => {
    const nav = e.target.closest("[data-navigate]");
    if (nav) {
      navigate(nav.dataset.navigate);
      return;
    }
    const b = e.target.closest("[data-action]");
    if (!b) return;
    const action = b.dataset.action,
      id = Number(b.dataset.id);
    switch (action) {
      case "new-project":
        newProject();
        break;
      case "edit-project":
        newProject(data.projects.find((p) => p.id === id));
        break;
      case "view-project":
        viewProject(data.projects.find((p) => p.id === id));
        break;
      case "new-activity":
        newActivity();
        break;
      case "edit-activity":
        newActivity(data.activities.find((a) => a.id === id));
        break;
      case "view-activity":
        viewActivity(data.activities.find((a) => a.id === id));
        break;
      case "quick-diary":
        newDiary();
        break;
      case "new-member":
        newMember();
        break;
      case "edit-member":
        newMember(data.members.find((m) => m.id === id));
        break;
      case "view-member":
        viewMember(data.members.find((m) => m.id === id));
        break;
      case "new-score":
        newScore();
        break;
      case "plan-review":
        reviewPlans();
        break;
      case "approve-plan":
        approvePlan(id);
        break;
      case "new-plan":
        newPlan();
        break;
      case "av3":
        requestAV3(data.members.find((m) => m.id === id));
        break;
      case "request-av3":
        requestAV3();
        break;
      case "justify-absence":
        justifyAbsence();
        break;
      case "open-session":
        openSession();
        break;
      case "save-attendance":
        saveAttendance();
        break;
      case "mark-attendance":
        attendanceDraft[b.dataset.name] = b.dataset.status;
        renderPage();
        break;
      case "checkin":
        checkin();
        break;
      case "mark-all-read":
        markAllRead();
        break;
      case "read-notification":
        readNotification(id);
        break;
      case "export-csv":
        exportCSV();
        break;
      case "print-report":
        printReport();
        break;
      case "generate-report":
        generateReport();
        break;
      case "export-audit":
        downloadCSV("nite-connect-auditoria.csv", [
          ["Data/hora", "Responsável", "Ação", "Alvo", "Justificativa"],
          ...data.audit.map((a) => [
            a.date,
            a.author,
            a.action,
            a.target,
            a.reason,
          ]),
        ]);
        break;
    }
  };
  const ps = $("#projectSearch");
  if (ps)
    ps.addEventListener("input", () => {
      projectSearch = ps.value;
      const status = $("#projectStatusFilter")?.value || "";
      const list = data.projects.filter(
        (p) =>
          (p.name + " " + p.manager + " " + p.status)
            .toLowerCase()
            .includes(projectSearch.toLowerCase()) &&
          (!status || p.status === status)
      );
      $("#projectsRows").innerHTML = projectRows(list);
    });
  const pfilter = $("#projectStatusFilter");
  if (pfilter)
    pfilter.addEventListener("change", () => {
      const q = $("#projectSearch").value.toLowerCase();
      $("#projectsRows").innerHTML = projectRows(
        data.projects.filter(
          (p) =>
            (p.name + " " + p.manager + " " + p.status)
              .toLowerCase()
              .includes(q) &&
            (!pfilter.value || p.status === pfilter.value)
        )
      );
    });
  const as = $("#activitySearch");
  if (as)
    as.addEventListener("input", () => {
      activitySearch = as.value;
      const status = $("#activityStatusFilter")?.value || "";
      $("#activitiesRows").innerHTML = activityRows(
        data.activities.filter(
          (a) =>
            (a.title + " " + a.project + " " + a.assignee)
              .toLowerCase()
              .includes(activitySearch.toLowerCase()) &&
            (!status || a.status === status)
        )
      );
    });
  const af = $("#activityStatusFilter");
  if (af)
    af.addEventListener("change", () => {
      const q = $("#activitySearch").value.toLowerCase();
      $("#activitiesRows").innerHTML = activityRows(
        data.activities.filter(
          (a) =>
            (a.title + " " + a.project + " " + a.assignee)
              .toLowerCase()
              .includes(q) &&
            (!af.value || a.status === af.value)
        )
      );
    });
  const ms = $("#memberSearch");
  if (ms)
    ms.addEventListener("input", () => {
      memberSearch = ms.value;
      const st = $("#memberStatusFilter")?.value || "";
      $("#membersRows").innerHTML = memberRows(
        data.members.filter(
          (m) =>
            (m.name + " " + m.email + " " + m.tech)
              .toLowerCase()
              .includes(memberSearch.toLowerCase()) &&
            (!st || m.status === st)
        )
      );
    });
  const mf = $("#memberStatusFilter");
  if (mf)
    mf.addEventListener("change", () => {
      const q = $("#memberSearch").value.toLowerCase();
      $("#membersRows").innerHTML = memberRows(
        data.members.filter(
          (m) =>
            (m.name + " " + m.email + " " + m.tech).toLowerCase().includes(q) &&
            (!mf.value || m.status === mf.value)
        )
      );
    });
  const au = $("#auditSearch");
  if (au)
    au.addEventListener("input", () => {
      $("#auditRows").innerHTML = auditRows(
        data.audit.filter((a) =>
          (a.author + " " + a.action + " " + a.target)
            .toLowerCase()
            .includes(au.value.toLowerCase())
        )
      );
    });
}
// Start on the institutional sign-in screen. No remote authentication is performed in this prototype.
