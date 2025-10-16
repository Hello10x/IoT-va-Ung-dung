// ============================
// ĐĂNG NHẬP & LƯU TOKEN
// ============================
async function login(username, password) {
  try {
    const res = await fetch("http://localhost:8080/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ username, password }),
    });

    const data = await res.json();
    if (data.token) {
      localStorage.setItem("jwtToken", data.token);
      alert("✅ Đăng nhập thành công!");
    } else {
      alert("❌ Sai tài khoản hoặc mật khẩu!");
    }
  } catch (err) {
    console.error("Login error:", err);
    alert("⚠️ Không thể kết nối tới server!");
  }
}

// ============================
// HÀM AUTH FETCH (CÓ JWT TOKEN)
// ============================
async function authFetch(url, options = {}) {
  const token = localStorage.getItem("jwtToken");
  if (!token) {
    alert("⚠️ Bạn chưa đăng nhập!");
    throw new Error("Missing JWT token");
  }

  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
    Authorization: `Bearer ${token}`,
  };

  return fetch(url, { ...options, headers });
}

// ============================
// BIẾN TOÀN CỤC PHÂN TRANG
// ============================
let currentPage = 1;
let totalPages = 1;
let pageSize = 10;
let currentDevice = "All";
let currentAction = "All";
let currentTrigger = "All";

let currentValue = "";
let currentSort = { key: "time", direction: "desc" };

const API_BASE = "http://localhost:8080/api/deviceactions/data";

// ============================
// DOM
// ============================
const tbody = document.querySelector(".actions-table tbody");
const pageNumbersContainer = document.querySelector(".pagination-containers .page-numbers");
const prevBtn = document.querySelector("#prev-btn");
const nextBtn = document.querySelector("#next-btn");
const pageSizeSelect = document.querySelector("#page-size");

// ============================
// LOAD DỮ LIỆU ACTIONS
// ============================
async function loadActions(page = 1) {
  if (tbody)
    tbody.innerHTML = `<tr><td colspan="5">⏳ Đang tải dữ liệu...</td></tr>`;

  try {
    // Lấy giá trị từ select và input
    const deviceEl = document.getElementById("device");
    const actionEl = document.getElementById("action");
    const triggerEl = document.getElementById("trigger");
    const searchEl = document.querySelector(".search-box input");

    const device = deviceEl ? deviceEl.value : "All";
    const action = actionEl ? actionEl.value : "All";
    const trigger = triggerEl ? triggerEl.value : "All";
    const value = searchEl ? searchEl.value.trim() : "";

    // Tạo URL với query params
    let url = `${API_BASE}?page=${page}&size=${pageSize}`;
    url += `&device=${encodeURIComponent(device)}`;
    url += `&action=${encodeURIComponent(action)}`;
    url += `&trigger=${encodeURIComponent(trigger)}`;
    url += `&value=${encodeURIComponent(value)}`;

    // Nếu có sắp xếp thì thêm vào
    if (currentSort && currentSort.key) {
      url += `&sortBy=${encodeURIComponent(currentSort.key)}&sortDir=${encodeURIComponent(currentSort.direction)}`;
    }

    const resp = await authFetch(url);
    const data = await resp.json();

    currentPage = data.pageId || page;
    totalPages = data.totalPages || 1;
    pageSize = data.pageSize || pageSize;

    renderTable(data.data || []);
    renderPagination();
  } catch (err) {
    console.error("Load actions error:", err);
    if (tbody)
      tbody.innerHTML = `<tr><td colspan="5" style="color:red;">Không thể tải dữ liệu: ${err.message}</td></tr>`;
  }
}


// ============================
// HIỂN THỊ BẢNG ACTIONS
// ============================
function renderTable(list) {
  if (!tbody) return;
  tbody.innerHTML = "";

  if (!Array.isArray(list) || list.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5">Không có dữ liệu</td></tr>`;
    return;
  }

  list.forEach(item => {
    const id = item.id ?? "";
    const deviceName = item.deviceName ?? item.device?.name ?? "";
    const action = item.action ? "ON" : "OFF";
    const trigger = item.source ?? item.trigger ?? "";
    const time = item.time ;

    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${id}</td>
      <td>
        <div class="device-cell">
          ${deviceIcon(deviceName)}
          <span>${deviceName}</span>
        </div>
      </td>
      <td><span class="action-badge ${action === "ON" ? "action-on" : "action-off"}">${action}</span></td>
      <td><span class="trigger-badge trigger-${trigger.toLowerCase()}">${trigger}</span></td>
      <td>${time}</td>
    `;
    tbody.appendChild(tr);
  });
}

// ============================
// ICON TÙY THEO TÊN THIẾT BỊ
// ============================
function deviceIcon(name) {
  if (!name) return "";
  const n = name.toLowerCase();
  if (n.includes("fan")) return `<i class="fa-solid fa-fan device-icon-small"></i>`;
  if (n.includes("led")) return `<i class="fa-solid fa-lightbulb device-icon-small"></i>`;
  if (n.includes("ac") || n.includes("air")) return `<i class="fa-solid fa-snowflake device-icon-small"></i>`;
  return `<i class="fa-solid fa-microchip device-icon-small"></i>`;
}

// ============================
// FORMAT THỜI GIAN
// ============================
function formatTime(datetimeStr) {
  if (!datetimeStr) return "";
  return datetimeStr.replace("T", " ").split(".")[0];
}

// ============================
// PHÂN TRANG
// ============================
function renderPagination() {
  if (!pageNumbersContainer) return;
  pageNumbersContainer.innerHTML = "";

  if (totalPages <= 1) {
    prevBtn.disabled = true;
    nextBtn.disabled = true;
    return;
  }

  prevBtn.disabled = currentPage === 1;
  nextBtn.disabled = currentPage === totalPages;

  const maxVisible = 5;
  let start = Math.max(1, currentPage - Math.floor(maxVisible / 2));
  let end = Math.min(totalPages, start + maxVisible - 1);

  if (end - start < maxVisible - 1) start = Math.max(1, end - maxVisible + 1);

  function makeBtn(n, active = false) {
    const btn = document.createElement("button");
    btn.textContent = n;
    btn.className = "page-btn";
    if (active) btn.classList.add("active");
    btn.addEventListener("click", () => loadActions(n));
    return btn;
  }

  if (start > 1) {
    pageNumbersContainer.appendChild(makeBtn(1, 1 === currentPage));
    if (start > 2) {
      const dots = document.createElement("span");
      dots.textContent = "...";
      dots.className = "dots";
      pageNumbersContainer.appendChild(dots);
    }
  }

  for (let i = start; i <= end; i++) {
    pageNumbersContainer.appendChild(makeBtn(i, i === currentPage));
  }

  if (end < totalPages) {
    if (end < totalPages - 1) {
      const dots = document.createElement("span");
      dots.textContent = "...";
      dots.className = "dots";
      pageNumbersContainer.appendChild(dots);
    }
    pageNumbersContainer.appendChild(makeBtn(totalPages, totalPages === currentPage));
  }
}

// ============================
// FILTER + SEARCH
// ============================
function handleSearch() {
  const filterEl = document.getElementById("filter");
  const searchEl = document.querySelector(".search-box input");
  currentFilter = filterEl ? filterEl.value : "All";
  currentValue = searchEl ? searchEl.value.trim() : "";
  currentPage = 1;
  loadActions(currentPage);
}

// ============================
// SORT ICON
// ============================
function updateSortIcons() {
  document.querySelectorAll(".sort-icon").forEach(icon => {
    const key = icon.dataset.key;
    if (key === currentSort.key) {
      icon.textContent = currentSort.direction === "asc" ? "↑" : "↓";
    } else {
      icon.textContent = "↕";
    }
  });
}

// ============================
// TOGGLE DEVICE (BẬT/TẮT)
// ============================
const fanToggle = document.getElementById("fanToggle");
const fanBtn = document.getElementById("fanBtn");
const lightToggle = document.getElementById("lightToggle");
const lightBtn = document.getElementById("lightBtn");
const airToggle = document.getElementById("airToggle");
const airBtn = document.getElementById("airBtn");

const deviceMap = { fan: "led1", light: "led2", air: "led3" };

function updateStatus(toggle, btn) {
  if (toggle.checked) {
    btn.textContent = "ON";
    btn.style.backgroundColor = "#D9D9D9";
    btn.style.color = "black";
  } else {
    btn.textContent = "OFF";
    btn.style.backgroundColor = "#101010";
    btn.style.color = "white";
  }
  updateDeviceIconEffect(toggle);
}

function showError(btn, toggle, prevChecked) {
  btn.innerHTML = "Error!";
  btn.style.backgroundColor = "#ff4c4c";
  btn.style.color = "white";
  setTimeout(() => {
    alert("❌ Mất kết nối tới thiết bị hoặc MQTT.\nNhấn OK để quay lại trạng thái cũ.");
    toggle.checked = prevChecked;
    updateStatus(toggle, btn);
  }, 100);
}

async function toggleDevice(key, toggle, btn) {
  const state = toggle.checked;
  const prevChecked = !state;
  showLoading(btn);

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort("Timeout 10s"), 10000);

  try {
    const res = await authFetch(`http://localhost:8080/api/device/action/${key}?state=${state}`, {
      method: "POST",
      signal: controller.signal,
    });
    clearTimeout(timeoutId);
    const text = await res.text();
    if (text.includes("✅")) {
      updateStatus(toggle, btn);
    } else {
      showError(btn, toggle, prevChecked);
    }
    if (text.includes("✅")) {
        updateStatus(toggle, btn);

        // ✅ Reload lại bảng action
        if (typeof loadActions === "function") {
            await loadActions(currentPage); // true = trigger highlight
        }   
    }
  } catch (err) {
    clearTimeout(timeoutId);
    console.error("Toggle error:", err);
    showError(btn, toggle, prevChecked);
  }
}

function showLoading(btn) {
  btn.innerHTML = `<div class="spinner" style="
    border: 3px solid #ccc;
    border-top: 3px solid #333;
    border-radius: 50%;
    width: 16px;
    height: 16px;
    animation: spin 1s linear infinite;
    display: inline-block;"></div>`;
  btn.style.backgroundColor = "#ccc";
  btn.style.color = "transparent";
}

const style = document.createElement("style");
style.textContent = `@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); }}`;
document.head.appendChild(style);

if (fanToggle) fanToggle.addEventListener("change", () => toggleDevice(deviceMap.fan, fanToggle, fanBtn));
if (lightToggle) lightToggle.addEventListener("change", () => toggleDevice(deviceMap.light, lightToggle, lightBtn));
if (airToggle) airToggle.addEventListener("change", () => toggleDevice(deviceMap.air, airToggle, airBtn));

// ============================
// LOAD TRẠNG THÁI BAN ĐẦU
// ============================
async function loadDeviceStatus() {
  try {
    const res = await authFetch("http://localhost:8080/api/device/status");
    const data = await res.json();
    data.forEach(d => {
      switch (d.name) {
        case "led1":
          fanToggle.checked = d.status;
          updateStatus(fanToggle, fanBtn);
          break;
        case "led2":
          lightToggle.checked = d.status;
          updateStatus(lightToggle, lightBtn);
          break;
        case "led3":
          airToggle.checked = d.status;
          updateStatus(airToggle, airBtn);
          break;
      }
    });
    updateDeviceIconEffect(fanToggle);
    updateDeviceIconEffect(lightToggle);
    updateDeviceIconEffect(airToggle);

  } catch (err) {
    console.error("Lỗi khi load trạng thái:", err);
  }
}

// ============================
// INIT
// ============================
document.addEventListener("DOMContentLoaded", () => {
  // Search & Sort
  document.querySelectorAll(".sort-icon").forEach(icon => {
    icon.addEventListener("click", () => {
      const key = icon.dataset.key;
      if (currentSort.key === key) {
        currentSort.direction = currentSort.direction === "asc" ? "desc" : "asc";
      } else {
        currentSort.key = key;
        currentSort.direction = "desc";
      }
      updateSortIcons();
      loadActions(1);
    });
  });

  const searchBtn = document.querySelector(".search-box button, .search-box i");
  const searchInput = document.querySelector(".search-box input");
  if (searchInput) searchInput.addEventListener("keypress", e => e.key === "Enter" && handleSearch());
  if (searchBtn) searchBtn.addEventListener("click", handleSearch);

  if (pageSizeSelect)
    pageSizeSelect.addEventListener("change", e => {
      const val = e.target.value;
      if (val.startsWith("size")) {
        const n = parseInt(val.replace("size", ""), 10);
        if (!isNaN(n)) pageSize = n;
      }
      loadActions(1);
    });

  const filterSelects = document.querySelectorAll("#device, #action, #trigger");
  filterSelects.forEach(sel => {
    sel.addEventListener("change", () => loadActions(1));
  });

  loadDeviceStatus();
  loadActions(1);
});

function updateDeviceIconEffect(toggle) {
  // tìm phần control-header gần nhất
  const header = toggle.closest(".control-header") || toggle.parentElement.parentElement;
  if (!header) return;

  const icon = header.querySelector(".control-icon i");
  if (!icon) return;

  // reset class cũ
  icon.classList.remove("fan-rotate", "light-glow", "ac-rotate");

  const id = toggle.id.toLowerCase();

  // ✅ bật thì thêm hiệu ứng
  if (toggle.checked) {
    if (id.includes("fan")) {
      icon.classList.add("fan-rotate");
    } else if (id.includes("light")) {
      icon.classList.add("light-glow");
    } else if (id.includes("air") || id.includes("ac")) {
      icon.classList.add("ac-rotate");
    }
  }
}

