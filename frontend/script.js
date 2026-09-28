const API = "http://localhost:8080/api";

const esc = (s) => String(s).replace(/[&<>"']/g, (c) => (
    { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]
));

async function loadAll() {
    const [modules, sessions] = await Promise.all([
        fetch(`${API}/modules`).then((r) => r.json()),
        fetch(`${API}/schedule`).then((r) => r.json()),
    ]);
    renderModules(modules);
    renderSchedule(sessions, modules);
}

function renderModules(modules) {
    const el = document.getElementById("modules");
    if (modules.length === 0) {
        el.innerHTML = '<p class="empty">No modules yet. Add one above.</p>';
        return;
    }
    el.innerHTML = modules.map((m) => `
        <div class="module">
            <div class="module-head">
                <div><strong>${esc(m.name)}</strong> <span>exam ${esc(m.examDate)}</span></div>
                <button onclick="generateTopics(${m.id}, this)">✨ AI topics</button>
            </div>
            ${(m.topics || []).map((t) => `
                <div class="topic">
                    <span>${esc(t.name)}</span>
                    <small>difficulty ${t.difficulty} · ${t.hoursCompleted}/${t.estimatedHoursNeeded}h</small>
                </div>`).join("") || '<p class="empty">No topics yet. Click "AI topics".</p>'}
        </div>`).join("");
}

function renderSchedule(sessions, modules) {
    const el = document.getElementById("schedule");
    if (sessions.length === 0) {
        el.innerHTML = '<p class="empty">No schedule yet. Generate one above.</p>';
        return;
    }

    const moduleOf = {};
    modules.forEach((m) => (m.topics || []).forEach((t) => (moduleOf[t.id] = m.name)));

    const byDay = {};
    sessions.forEach((s) => (byDay[s.date] = byDay[s.date] || []).push(s));

    el.innerHTML = Object.keys(byDay).sort().map((date) => `
        <div class="day">
            <h3>${esc(new Date(date).toDateString())}</h3>
            ${byDay[date].map((s) => `
                <div class="session ${s.completed ? "done" : ""}">
                    <span>${esc(s.topic.name)} <small>(${esc(moduleOf[s.topic.id] || "")})</small> · ${s.allocatedHours}h</span>
                    ${s.completed ? "" : `<button onclick="completeSession(${s.id})">Done</button>`}
                </div>`).join("")}
        </div>`).join("");
}

document.getElementById("module-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    await fetch(`${API}/modules`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            name: document.getElementById("module-name").value,
            examDate: document.getElementById("module-exam").value,
        }),
    });
    e.target.reset();
    loadAll();
});

async function generateTopics(moduleId, btn) {
    btn.disabled = true;
    btn.textContent = "Generating...";
    try {
        await fetch(`${API}/topics/generate`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ moduleId }),
        });
    } finally {
        loadAll();
    }
}

document.getElementById("schedule-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    await fetch(`${API}/schedule/generate`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ dailyAvailableHours: parseFloat(document.getElementById("daily-hours").value) }),
    });
    loadAll();
});

async function completeSession(id) {
    await fetch(`${API}/schedule/${id}/complete`, { method: "PUT" });
    loadAll();
}

loadAll();