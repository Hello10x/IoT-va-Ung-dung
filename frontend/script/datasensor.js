// datasensor.js
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
      // ✅ Lưu token vào localStorage
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

// Auth fetch (kèm JWT)
async function authFetch(url, options = {}) {
  const token = localStorage.getItem("jwtToken");
  if (!token) {
    alert("Bạn chưa đăng nhập hoặc token bị mất. Vui lòng đăng nhập.");
    throw new Error("Missing JWT token");
  }

  const headers = {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`,
    ...(options.headers || {}),
  };

  const response = await fetch(url, { ...options, headers });
  if (!response.ok) {
    const text = await response.text().catch(() => "");
    throw new Error(`HTTP ${response.status} ${response.statusText} ${text}`);
  }
  return response.json();
}

// ============================
// Biến toàn cục
let currentPage = 1;
let totalPages = 1;
let pageSize = 10; 
let currentFilter = "All";
let currentValue = "";
let currentSort = { key: "time", direction: "desc" }; 

const API_BASE = "http://localhost:8080/api/datasensors/data";

// DOM refs (the HTML structure you provided)
const tbody = document.querySelector(".table-contents table tbody");
const pageNumbersContainer = document.getElementById("page-numbers");
const prevBtn = document.getElementById("prev-btn");
const nextBtn = document.getElementById("next-btn");
const pageSizeSelect = document.getElementById("page-size");

// ============================
// Load data page từ API và render
async function loadData(page = 1) {
  // show loading row
  if (tbody) tbody.innerHTML = `<tr><td colspan="5">⏳ Đang tải dữ liệu...</td></tr>`;

  try {
    // --- Lấy giá trị filter + search ---
    const filterEl = document.getElementById("filter");
    const searchEl = document.querySelector(".search-box input");
    const filter = filterEl ? filterEl.value : "All";
    const value = searchEl ? searchEl.value.trim() : "";

    // --- Tạo URL động ---
    let url = `${API_BASE}?page=${page}&size=${pageSize}`;

    if (filter && filter !== "All" && value !== "") {
      url += `&filter=${encodeURIComponent(filter)}&value=${encodeURIComponent(value)}`;
    } else if (filter === "All" && value !== "") {
      // nếu chọn All thì tìm theo thời gian
      url += `&filter=All&value=${encodeURIComponent(value)}`;
    }



    let sortBy = currentSort.key;
    let sortDir = currentSort.direction;

    // Thêm vào URL
    url += `&sortBy=${encodeURIComponent(sortBy)}&sortDir=${encodeURIComponent(sortDir)}`;

    // --- Gọi API ---
    const resp = await authFetch(url);

    // --- Cập nhật thông tin trang ---
    currentPage = resp.pageId || page;
    pageSize = resp.pageSize || pageSize;
    totalPages = resp.totalPages || 1;

    // --- Render dữ liệu ---
    renderTable(resp.data || []);
    renderPagination();

  } catch (err) {
    console.error("Load data error:", err);
    if (tbody)
      tbody.innerHTML = `<tr><td colspan="5" style="color:red;">Không thể tải dữ liệu: ${err.message}</td></tr>`;

    // disable pagination
    if (prevBtn) prevBtn.disabled = true;
    if (nextBtn) nextBtn.disabled = true;
    if (pageNumbersContainer) pageNumbersContainer.innerHTML = "";
  }
}

// ============================
// Vẽ bảng (map API data -> table rows)
// mỗi item trong data expected: { sessionId, createdAt, temperature, humidity, lux }
function renderTable(list) {
   // Nếu không có dữ liệu, xóa hết hiển thị
  if (!list || list.length === 0) {
    document.getElementById("temp-avg-value").textContent = "—";
    document.getElementById("temp-max-value").textContent = "—";
    document.getElementById("hum-avg-value").textContent = "—";
    document.getElementById("hum-max-value").textContent = "—";
    document.getElementById("lux-avg-value").textContent = "—";
    document.getElementById("lux-max-value").textContent = "—";
    return;
  }

  // ✅ Tính trung bình và max cho từng loại
  let tempSum = 0, tempMax = -Infinity, tempCount = 0;
  let humSum = 0, humMax = -Infinity, humCount = 0;
  let luxSum = 0, luxMax = -Infinity, luxCount = 0;

  list.forEach(item => {
    const t = parseFloat(item.temperature);
    const h = parseFloat(item.humidity);
    const l = parseFloat(item.lux);

    if (!isNaN(t)) {
      tempSum += t;
      tempMax = Math.max(tempMax, t);
      tempCount++;
    }
    if (!isNaN(h)) {
      humSum += h;
      humMax = Math.max(humMax, h);
      humCount++;
    }
    if (!isNaN(l)) {
      luxSum += l;
      luxMax = Math.max(luxMax, l);
      luxCount++;
    }
  });

  const tempAvg = tempCount ? (tempSum / tempCount).toFixed(1) : "—";
  const humAvg = humCount ? (humSum / humCount).toFixed(1) : "—";
  const luxAvg = luxCount ? (luxSum / luxCount).toFixed(1) : "—";

  // ✅ Cập nhật lên giao diện
  document.getElementById("temp-avg-value").textContent = `${tempAvg}°C`;
  document.getElementById("temp-max-value").textContent = `${tempMax.toFixed(1)}°C`;
  document.getElementById("hum-avg-value").textContent = `${humAvg}%`;
  document.getElementById("hum-max-value").textContent = `${humMax.toFixed(1)}%`;
  document.getElementById("lux-avg-value").textContent = `${luxAvg} lux`;
  document.getElementById("lux-max-value").textContent = `${luxMax.toFixed(1)} lux`;
  

  if (!tbody) return;
  tbody.innerHTML = "";

  if (!Array.isArray(list) || list.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5">Không có dữ liệu</td></tr>`;
    return;
  }

  list.forEach(item => {
  const sessionId = item.sessionId ?? item.id ?? "";

  // Chuẩn hóa giá trị và làm tròn số (1 chữ số sau dấu phẩy)
  const temp = (item.temperature !== null && item.temperature !== undefined)
    ? Number(item.temperature).toFixed(1)
    : "";

  const hum = (item.humidity !== null && item.humidity !== undefined)
    ? Number(item.humidity).toFixed(1)
    : "";

  const lux = (item.lux !== null && item.lux !== undefined)
    ? Number(item.lux).toFixed(1)
    : "";

  const createdAt = item.createdAt || "";

  // Tạo dòng bảng
  const tr = document.createElement("tr");
  tr.innerHTML = `
    <td>${sessionId}</td>
    <td>${temp ? temp : ""}</td>
    <td>${hum ? hum : ""}</td>
    <td>${lux ? lux : ""}</td>
    <td>${createdAt}</td>
  `;
  tbody.appendChild(tr);
});
}

// ============================
// Format time ISO -> local string (giữ timezone)
function formatTime(datetimeStr) {
  if (!datetimeStr) return "";
  return datetimeStr.replace("T", " ").split(".")[0];
}

// ============================
// Phân trang thông minh hiển thị dạng: 1 2 ... 100
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

  const maxVisible = 5; // số nút số trang hiển thị trong khoảng
  let start = Math.max(1, currentPage - Math.floor(maxVisible / 2));
  let end = Math.min(totalPages, start + maxVisible - 1);

  if (end - start < maxVisible - 1) {
    start = Math.max(1, end - maxVisible + 1);
  }

  // helper tạo nút
  function makePageBtn(n, active = false) {
    const btn = document.createElement("button");
    btn.textContent = String(n);
    btn.className = "page-btn";
    if (active) btn.classList.add("active");
    btn.addEventListener("click", () => {
      if (n === currentPage) return;
      loadData(n);
    });
    return btn;
  }

  // nếu có khoảng trước start: show first + dots
  if (start > 1) {
    pageNumbersContainer.appendChild(makePageBtn(1, 1 === currentPage));
    if (start > 2) {
      const dots = document.createElement("span"); dots.textContent = "..."; dots.className = "dots";
      pageNumbersContainer.appendChild(dots);
    }
  }

  // render middle range
  for (let i = start; i <= end; i++) {
    pageNumbersContainer.appendChild(makePageBtn(i, i === currentPage));
  }

  // nếu có khoảng sau end: dots + last
  if (end < totalPages) {
    if (end < totalPages - 1) {
      const dots = document.createElement("span"); dots.textContent = "..."; dots.className = "dots";
      pageNumbersContainer.appendChild(dots);
    }
    pageNumbersContainer.appendChild(makePageBtn(totalPages, totalPages === currentPage));
  }
}

// ============================
// Event listeners cho prev/next và page-size
if (prevBtn) prevBtn.addEventListener("click", () => {
  if (currentPage > 1) loadData(currentPage - 1);
});
if (nextBtn) nextBtn.addEventListener("click", () => {
  if (currentPage < totalPages) loadData(currentPage + 1);
});
if (pageSizeSelect) pageSizeSelect.addEventListener("change", (e) => {
  // page-size select options in your HTML use values like "size10", "size20", "size50"
  const val = e.target.value || "";
  if (val.startsWith("size")) {
    const n = parseInt(val.replace("size", ""), 10);
    if (!isNaN(n) && n > 0) {
      pageSize = n;
    }
  } else {
    const parsed = parseInt(val, 10);
    if (!isNaN(parsed) && parsed > 0) pageSize = parsed;
  }
  // reload page 1 when pageSize changes (the backend will return correct page)
  loadData(1);
});


// ============================
// Bắt sự kiện tìm kiếm và filter
document.addEventListener("DOMContentLoaded", () => {
  const searchInput = document.querySelector(".search-box input");
  const searchBtn = document.querySelector(".search-box i, .search-box button"); // icon hoặc button search
  const filterSelect = document.getElementById("filter");

  // Nhấn Enter trong ô input -> gọi search
  if (searchInput) {
    searchInput.addEventListener("keypress", (e) => {
      if (e.key === "Enter") {
        e.preventDefault();
        handleSearch();
      }
    });
  }

  // Click icon search
  if (searchBtn) {
    searchBtn.addEventListener("click", handleSearch);
  }
});

// ============================
// Hàm xử lý tìm kiếm
function handleSearch() {
  const filterEl = document.getElementById("filter");
  const searchEl = document.querySelector(".search-box input");

  const filter = filterEl ? filterEl.value : "All";
  const value = searchEl ? searchEl.value.trim() : "";

  // Cập nhật biến toàn cục
  currentFilter = filter;
  currentValue = value;
  currentPage = 1; // reset về trang đầu

  // Gọi API
  loadData(currentPage);
}


// ============================
// Init
window.addEventListener("DOMContentLoaded", () => {
  // set initial pageSize from select if present
  if (pageSizeSelect) {
    const v = pageSizeSelect.value;
    if (v && v.startsWith("size")) {
      const n = parseInt(v.replace("size", ""), 10);
      if (!isNaN(n)) pageSize = n;
    }
  }
  loadData(1);
});

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

document.querySelectorAll(".sort-icon").forEach(icon => {
  icon.addEventListener("click", () => {
    const key = icon.dataset.key;

    // Nếu nhấn lại cùng một cột => đảo chiều
    if (currentSort.key === key) {
      currentSort.direction = currentSort.direction === "asc" ? "desc" : "asc";
    } else {
      currentSort.key = key;
      currentSort.direction = "desc"; // Lần đầu nhấn vào cột mới
    }

    updateSortIcons();
    loadData(1);
  });
});