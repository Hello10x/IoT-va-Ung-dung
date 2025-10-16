// ==========================
// ĐĂNG NHẬP & LƯU TOKEN
// ==========================
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

// CHẠY LẦN ĐẦU ĐỂ LẤY TOKEN — chỉ cần gọi 1 lần (VD: login("hello10x37", "vangiap123"))
// login("hello10x37", "vangiap123");

// ==========================
// HÀM TIỆN ÍCH: GỌI API CÓ TOKEN
// ==========================
async function authFetch(url, options = {}) {
  const token = localStorage.getItem("jwtToken");
  if (!token) {
    alert("⚠️ Bạn chưa đăng nhập!");
    throw new Error("Missing JWT token");
  }

  const headers = {
    ...options.headers,
    Authorization: `Bearer ${token}`,
  };

  return fetch(url, { ...options, headers });
}

// ==========================
// KHAI BÁO DOM ELEMENT
// ==========================
const fanToggle = document.getElementById("fanToggle");
const fanBtn = document.getElementById("fanBtn");

const lightToggle = document.getElementById("lightToggle");
const lightBtn = document.getElementById("lightBtn");

const airToggle = document.getElementById("airToggle");
const airBtn = document.getElementById("airBtn");

const deviceMap = {
  fan: "led1",
  light: "led2",
  air: "led3",
};

// ==========================
// HÀM CẬP NHẬT NÚT TRẠNG THÁI
// ==========================
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

// ==========================
// LOADING SPINNER
// ==========================
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
style.textContent = `
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}`;
document.head.appendChild(style);

// ==========================
// HIỂN THỊ LỖI
// ==========================
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

// ==========================
// GỬI YÊU CẦU BẬT/TẮT (CÓ TOKEN)
// ==========================
async function toggleDevice(key, toggle, btn) {
  const state = toggle.checked;
  const prevChecked = !state;
  showLoading(btn);

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort("Timeout 10s"), 10000);

  try {
    const res = await authFetch(
      `http://localhost:8080/api/device/action/${key}?state=${state}`,
      { method: "POST", signal: controller.signal }
    );

    clearTimeout(timeoutId);

    if (!res.ok) throw new Error(`Request failed: ${res.status}`);

    const text = await res.text();
    console.log("Response:", text);

    if (text.includes("✅")) {
      updateStatus(toggle, btn);
    } else {
      showError(btn, toggle, prevChecked);
    }
  } catch (err) {
    clearTimeout(timeoutId);
    console.error("Toggle error:", err);
    showError(btn, toggle, prevChecked);
  }
}

// ==========================
// GÁN SỰ KIỆN CHO NÚT
// ==========================
fanToggle.addEventListener("change", () => {
  toggleDevice(deviceMap.fan, fanToggle, fanBtn);
});
lightToggle.addEventListener("change", () => {
  toggleDevice(deviceMap.light, lightToggle, lightBtn);
});
airToggle.addEventListener("change", () => {
  toggleDevice(deviceMap.air, airToggle, airBtn);
});

// ==========================
// LOAD TRẠNG THÁI BAN ĐẦU (CÓ TOKEN)
// ==========================
async function loadDeviceStatus() {
  try {
    const res = await authFetch("http://localhost:8080/api/device/status");
    const data = await res.json();

    data.forEach(device => {
      const { name, status } = device;
      if (name === "led1") {
        fanToggle.checked = status;
        updateStatus(fanToggle, fanBtn);
      } else if (name === "led2") {
        lightToggle.checked = status;
        updateStatus(lightToggle, lightBtn);
      } else if (name === "led3") {
        airToggle.checked = status;
        updateStatus(airToggle, airBtn);
      }
    });
    updateDeviceIconEffect(fanToggle);
    updateDeviceIconEffect(lightToggle);
    updateDeviceIconEffect(airToggle);
  } catch (err) {
    console.error("Lỗi khi load trạng thái:", err);
  }
}

// ==========================
// CHART + DỮ LIỆU SENSOR (CÓ TOKEN)
// ==========================
const ctx = document.getElementById('lineChart').getContext('2d');
let chartData = {
  labels: [],
  datasets: [
    { label: 'Temperature (°C)', data: [], borderColor: 'red', yAxisID: 'yLeft' },
    { label: 'Humidity (%)', data: [], borderColor: 'blue', yAxisID: 'yLeft' },
    { label: 'Lux', data: [], borderColor: 'yellow', yAxisID: 'yRight' }
  ]
};

let chart = new Chart(ctx, {
  type: 'line',
  data: chartData,
  options: {
    responsive: true,
    interaction: { mode: 'index', intersect: false },
    scales: {
      x: {
        title: { display: true, text: 'Time (hh:mm:ss)' },
        ticks: {
          callback: function(value) {
            const raw = this.getLabelForValue(value);
            const date = new Date(raw);
            return date.toLocaleTimeString('en-GB', { hour12: false });
          }
        }
      },
      yLeft: { type: 'linear', position: 'left' },
      yRight: { type: 'linear', position: 'right', grid: { drawOnChartArea: false } }
    }
  }
});

// Hàm nội suy màu
function interpolateColor(color1, color2, factor) {
  const c1 = parseInt(color1.slice(1), 16);
  const c2 = parseInt(color2.slice(1), 16);
  const r1 = (c1 >> 16) & 0xff, g1 = (c1 >> 8) & 0xff, b1 = c1 & 0xff;
  const r2 = (c2 >> 16) & 0xff, g2 = (c2 >> 8) & 0xff, b2 = c2 & 0xff;
  const r = Math.round(r1 + factor * (r2 - r1));
  const g = Math.round(g1 + factor * (g2 - g1));
  const b = Math.round(b1 + factor * (b2 - b1));
  return `rgb(${r},${g},${b})`;
}

const tempEl = document.getElementById("temp-value");
const humEl = document.getElementById("hum-value");
const lightEl = document.getElementById("light-value");

function updateMetrics(session) {
  const tempFactor = Math.min(Math.max((session.temperature - 20) / 20, 0), 1);
  tempEl.textContent = session.temperature + "°C";
  tempEl.style.color = interpolateColor("#FFA500", "#FF0000", tempFactor);

  const humFactor = Math.min(Math.max(session.humidity / 100, 0), 1);
  humEl.textContent = session.humidity + "%";
  humEl.style.color = interpolateColor("#ADD8E6", "#0000FF", humFactor);

  const lightFactor = Math.min(Math.max(session.lux / 100, 0), 1);
  lightEl.textContent = session.lux + " lux";
  lightEl.style.color = interpolateColor("#AAAAAA", "#FFD700", lightFactor);
}

// Load 100 sessions ban đầu
async function loadInitialData() {
  try {
    const res = await authFetch("http://localhost:8080/api/datasensors/last-100-session");
    const sessions = await res.json();

    chartData.labels = sessions.map(s => s.createdAt);
    chartData.datasets[0].data = sessions.map(s => s.temperature);
    chartData.datasets[1].data = sessions.map(s => s.humidity);
    chartData.datasets[2].data = sessions.map(s => s.lux);

    updateMetrics(sessions[sessions.length - 1]);
    chart.update();
  } catch (err) {
    console.error("Error loading initial data:", err);
  }
}

// Lấy session mới nhất mỗi 2 giây
async function fetchLatestData() {
  try {
    const res = await authFetch("http://localhost:8080/api/datasensors/latest-session");
    const session = await res.json();

    chartData.labels.push(session.createdAt);
    chartData.datasets[0].data.push(session.temperature);
    chartData.datasets[1].data.push(session.humidity);
    chartData.datasets[2].data.push(session.lux);

    if (chartData.labels.length > 100) {
      chartData.labels.shift();
      chartData.datasets.forEach(ds => ds.data.shift());
    }

    updateMetrics(session);
    chart.update();
  } catch (err) {
    console.error("Error fetching latest session:", err);
  }
}

// --- MAIN ---
document.addEventListener("DOMContentLoaded", async () => {
  await loadDeviceStatus();
  await loadInitialData();
  setInterval(fetchLatestData, 2000);
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

