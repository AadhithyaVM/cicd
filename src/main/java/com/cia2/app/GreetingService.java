package com.cia2.app;

/** Page and text logic, kept separate from HTTP code so it is easy to unit test. */
public class GreetingService {

    /** Title shown at the top of the application. Change this text to demo the pipeline (Deliverable 4). */
    public static final String HEADLINE = "TaskFlow - plan your day (version 1)";

    public String greet(String name) {
        if (name == null || name.isBlank()) {
            return "Hello, World!";
        }
        return "Hello, " + name.trim() + "!";
    }

    /** A small single-page task manager. Tasks are kept in the user's browser (localStorage). */
    public String renderHomePage(String version, String podName) {
        String page = """
            <!DOCTYPE html>
            <html lang="en" data-theme="light">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>TaskFlow</title>
              <style>
                :root { --bg:#f1f5f9; --card:#ffffff; --text:#0f172a; --muted:#64748b; --line:#e2e8f0; --accent:#4f46e5; --accent-soft:#eef2ff; }
                html[data-theme="dark"] { --bg:#0b1120; --card:#131c31; --text:#e5e7eb; --muted:#94a3b8; --line:#243049; --accent:#818cf8; --accent-soft:#1c2547; }
                * { box-sizing:border-box; }
                body { margin:0; background:var(--bg); color:var(--text); font-family:"Segoe UI",Arial,sans-serif; }
                .wrap { max-width:760px; margin:0 auto; padding:32px 16px 48px; }
                header { display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:22px; }
                h1 { font-size:clamp(22px,4vw,30px); margin:0; }
                .tag { color:var(--muted); font-size:14px; margin-top:4px; }
                .icon-btn { border:1px solid var(--line); background:var(--card); color:var(--text); border-radius:10px; padding:8px 12px; cursor:pointer; font-size:15px; }
                .panel { background:var(--card); border:1px solid var(--line); border-radius:16px; padding:18px; box-shadow:0 6px 24px rgba(15,23,42,.06); }
                .stats { display:grid; grid-template-columns:repeat(3,1fr); gap:12px; margin-bottom:16px; }
                .stat { background:var(--accent-soft); border-radius:12px; padding:12px 14px; }
                .stat b { display:block; font-size:24px; color:var(--accent); }
                .stat span { font-size:12px; color:var(--muted); text-transform:uppercase; letter-spacing:.08em; }
                .bar { height:8px; background:var(--line); border-radius:99px; overflow:hidden; margin-bottom:18px; }
                .bar i { display:block; height:100%; width:0; background:var(--accent); transition:width .3s; }
                form { display:flex; flex-wrap:wrap; gap:8px; margin-bottom:16px; }
                input[type=text], input[type=date], select { padding:11px 12px; border:1px solid var(--line); border-radius:10px; background:var(--bg); color:var(--text); font-size:15px; }
                input[type=text] { flex:1 1 220px; }
                .primary { background:var(--accent); color:#fff; border:0; border-radius:10px; padding:11px 18px; font-size:15px; font-weight:600; cursor:pointer; }
                .tabs { display:flex; gap:6px; flex-wrap:wrap; margin-bottom:10px; align-items:center; }
                .tab { border:1px solid var(--line); background:transparent; color:var(--muted); border-radius:99px; padding:6px 14px; cursor:pointer; font-size:14px; }
                .tab.on { background:var(--accent); border-color:var(--accent); color:#fff; }
                .grow { flex:1; }
                ul { list-style:none; margin:0; padding:0; }
                li { display:flex; align-items:center; gap:12px; padding:12px 6px; border-bottom:1px solid var(--line); }
                li:last-child { border-bottom:0; }
                li input[type=checkbox] { width:20px; height:20px; accent-color:var(--accent); cursor:pointer; }
                .txt { flex:1; min-width:0; }
                .txt div { word-break:break-word; }
                .done .txt div { text-decoration:line-through; color:var(--muted); }
                .meta { font-size:12px; color:var(--muted); margin-top:3px; }
                .late { color:#dc2626; font-weight:600; }
                .pri { font-size:12px; font-weight:700; padding:3px 9px; border-radius:99px; }
                .pri.High { background:#fee2e2; color:#b91c1c; }
                .pri.Medium { background:#fef3c7; color:#b45309; }
                .pri.Low { background:#dcfce7; color:#15803d; }
                .del { border:0; background:transparent; color:var(--muted); cursor:pointer; font-size:18px; }
                .del:hover { color:#dc2626; }
                .empty { text-align:center; color:var(--muted); padding:30px 0; }
                footer { margin-top:20px; display:flex; justify-content:space-between; flex-wrap:wrap; gap:8px; color:var(--muted); font-size:13px; }
                .link { background:none; border:0; color:var(--accent); cursor:pointer; font-size:13px; padding:0; }
              </style>
            </head>
            <body>
              <div class="wrap">
                <header>
                  <div>
                    <h1>@@HEADLINE@@</h1>
                    <div class="tag">Simple task manager &middot; your tasks are saved in this browser</div>
                  </div>
                  <button class="icon-btn" id="theme" title="Switch light / dark">&#9790;</button>
                </header>

                <div class="panel">
                  <div class="stats">
                    <div class="stat"><b id="sTotal">0</b><span>Total</span></div>
                    <div class="stat"><b id="sActive">0</b><span>To do</span></div>
                    <div class="stat"><b id="sDone">0</b><span>Done</span></div>
                  </div>
                  <div class="bar"><i id="progress"></i></div>

                  <form id="form">
                    <input type="text" id="text" placeholder="What needs to be done?" maxlength="120" autocomplete="off" required>
                    <select id="priority">
                      <option>Low</option><option selected>Medium</option><option>High</option>
                    </select>
                    <input type="date" id="due">
                    <button class="primary" type="submit">Add task</button>
                  </form>

                  <div class="tabs">
                    <button class="tab on" data-f="all">All</button>
                    <button class="tab" data-f="active">To do</button>
                    <button class="tab" data-f="done">Done</button>
                    <span class="grow"></span>
                    <input type="text" id="search" placeholder="Search..." style="flex:0 1 150px;padding:7px 10px;font-size:14px">
                  </div>

                  <ul id="list"></ul>
                </div>

                <footer>
                  <span>Build <b>#@@VERSION@@</b> &middot; pod <b>@@POD@@</b> &middot; <a href="/health" style="color:var(--accent)">health</a></span>
                  <button class="link" id="clear">Clear completed</button>
                </footer>
              </div>

              <script>
              (function () {
                var KEY = 'taskflow.tasks.v1';
                var tasks = [];
                var filter = 'all';
                try { tasks = JSON.parse(localStorage.getItem(KEY) || '[]'); } catch (e) { tasks = []; }
                if (!Array.isArray(tasks)) { tasks = []; }

                function save() { try { localStorage.setItem(KEY, JSON.stringify(tasks)); } catch (e) {} }
                function uid() { return Date.now().toString(36) + Math.random().toString(36).slice(2, 6); }
                function el(id) { return document.getElementById(id); }

                function render() {
                  var q = el('search').value.trim().toLowerCase();
                  var today = new Date().toISOString().slice(0, 10);
                  var list = el('list');
                  list.innerHTML = '';
                  var shown = tasks.filter(function (t) {
                    if (filter === 'active' && t.done) { return false; }
                    if (filter === 'done' && !t.done) { return false; }
                    return !q || t.text.toLowerCase().indexOf(q) !== -1;
                  });
                  shown.forEach(function (t) {
                    var li = document.createElement('li');
                    if (t.done) { li.className = 'done'; }

                    var cb = document.createElement('input');
                    cb.type = 'checkbox'; cb.checked = t.done;
                    cb.onchange = function () { t.done = cb.checked; save(); render(); };

                    var box = document.createElement('div'); box.className = 'txt';
                    var title = document.createElement('div'); title.textContent = t.text;
                    var meta = document.createElement('div'); meta.className = 'meta';
                    if (t.due) {
                      var late = !t.done && t.due < today;
                      var s = document.createElement('span');
                      s.textContent = 'Due ' + t.due + (late ? ' (overdue)' : '');
                      if (late) { s.className = 'late'; }
                      meta.appendChild(s);
                    } else { meta.textContent = 'No due date'; }
                    box.appendChild(title); box.appendChild(meta);

                    var pri = document.createElement('span');
                    pri.className = 'pri ' + t.priority; pri.textContent = t.priority;

                    var del = document.createElement('button');
                    del.className = 'del'; del.title = 'Delete'; del.innerHTML = '&times;';
                    del.onclick = function () { tasks = tasks.filter(function (x) { return x.id !== t.id; }); save(); render(); };

                    li.appendChild(cb); li.appendChild(box); li.appendChild(pri); li.appendChild(del);
                    list.appendChild(li);
                  });
                  if (!shown.length) {
                    var e = document.createElement('div');
                    e.className = 'empty';
                    e.textContent = tasks.length ? 'No tasks match this view.' : 'No tasks yet. Add your first one above.';
                    list.appendChild(e);
                  }
                  var done = tasks.filter(function (t) { return t.done; }).length;
                  el('sTotal').textContent = tasks.length;
                  el('sDone').textContent = done;
                  el('sActive').textContent = tasks.length - done;
                  el('progress').style.width = (tasks.length ? Math.round(done * 100 / tasks.length) : 0) + '%';
                }

                el('form').onsubmit = function (ev) {
                  ev.preventDefault();
                  var text = el('text').value.trim();
                  if (!text) { return; }
                  tasks.unshift({ id: uid(), text: text, priority: el('priority').value, due: el('due').value, done: false });
                  el('text').value = ''; el('due').value = '';
                  save(); render();
                };
                Array.prototype.forEach.call(document.querySelectorAll('.tab'), function (b) {
                  b.onclick = function () {
                    filter = b.getAttribute('data-f');
                    Array.prototype.forEach.call(document.querySelectorAll('.tab'), function (x) { x.classList.toggle('on', x === b); });
                    render();
                  };
                });
                el('search').oninput = render;
                el('clear').onclick = function () { tasks = tasks.filter(function (t) { return !t.done; }); save(); render(); };
                el('theme').onclick = function () {
                  var d = document.documentElement;
                  var next = d.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
                  d.setAttribute('data-theme', next);
                  try { localStorage.setItem('taskflow.theme', next); } catch (e) {}
                };
                try { var th = localStorage.getItem('taskflow.theme'); if (th) { document.documentElement.setAttribute('data-theme', th); } } catch (e) {}
                render();
              })();
              </script>
            </body>
            </html>
            """;
        return page
                .replace("@@HEADLINE@@", escape(HEADLINE))
                .replace("@@VERSION@@", escape(version))
                .replace("@@POD@@", escape(podName));
    }

    public String healthJson(String version) {
        return "{\"status\":\"UP\",\"version\":\"" + version + "\"}";
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}