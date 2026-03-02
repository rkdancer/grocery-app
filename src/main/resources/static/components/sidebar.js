/* ===== SIDEBAR COMPONENT (GLOBAL) ===== */
(function () {
  function logout() {
    localStorage.clear();
    location.href = "login.html";
  }

  const me = JSON.parse(localStorage.getItem("user") || "null");
  if (!me) {
    logout();
    return;
  }

  const sidebar = document.getElementById("sidebarMenu");
  if (!sidebar) return;

  let html = "";

  const role = String(me.role || "").toUpperCase();

  // OWNER dashboard
  if (role === "OWNER" || role === "ADMIN") {
    html += `
      <li>
        <a href="owner_dashboard.html" class="sidebar-link">
          <i class="fas fa-chart-line"></i> แดชบอร์ด
        </a>
      </li>
    `;
  }

  // Product pages (STAFF/OWNER/ADMIN)
  if (role === "OWNER" || role === "STAFF" || role === "ADMIN") {
    html += `
      <li>
        <a href="categories.html" class="sidebar-link">
          <i class="fas fa-box"></i> สินค้า
        </a>
      </li>
      <li>
        <a href="pos.html" class="sidebar-link">
          <i class="fas fa-cash-register"></i> ขายสินค้า
        </a>
      </li>
    `;
  }

  // Admin users
  if (role === "ADMIN") {
    html += `
      <li>
        <a href="admin_users.html" class="sidebar-link">
          <i class="fas fa-users"></i> จัดการผู้ใช้
        </a>
      </li>
    `;
  }

  // Logout
  html += `
    <li>
      <button class="sidebar-link logout-btn" onclick="logout()">
        <i class="fas fa-right-from-bracket"></i> Logout
      </button>
    </li>
  `;

  sidebar.innerHTML = html;

  // Active menu highlight
  const path = location.pathname.split("/").pop();
  sidebar.querySelectorAll("a").forEach((a) => {
    if (a.getAttribute("href") === path) a.classList.add("active");
  });

  window.logout = logout;
})();