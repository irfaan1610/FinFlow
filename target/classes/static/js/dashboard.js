if (!localStorage.getItem("token")) {
    window.location.href = "index.html";
}

function logout() {
    localStorage.clear();
    window.location.href = "index.html";
}

async function loadDashboard() {
    const res = await apiFetch("/dashboard/summary");
    const data = await res.json();

    document.getElementById("income").innerText = data.totalIncome;
    document.getElementById("expenses").innerText = data.totalExpenses;
    document.getElementById("balance").innerText = data.netBalance;

    const tbody = document.getElementById("transactions");
    tbody.innerHTML = "";

    data.recentTransactions.forEach(t => {
        tbody.innerHTML += `
            <tr>
                <td>${t.amount}</td>
                <td>${t.type}</td>
                <td>${t.category}</td>
                <td>${t.date}</td>
            </tr>`;
    });

    renderCharts(data);
}

function renderCharts(data) {
    new Chart(document.getElementById("categoryChart"), {
        type: "pie",
        data: {
            labels: Object.keys(data.categoryTotals),
            datasets: [{ data: Object.values(data.categoryTotals) }]
        }
    });

    new Chart(document.getElementById("incomeExpenseChart"), {
        type: "bar",
        data: {
            labels: ["Income", "Expenses"],
            datasets: [{ data: [data.totalIncome, data.totalExpenses] }]
        }
    });
}

loadDashboard();