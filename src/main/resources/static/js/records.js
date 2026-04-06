// AUTH CHECK
if (!localStorage.getItem("token")) {
    window.location.href = "index.html";
}

const role = localStorage.getItem("role");

// HIDE ADD BUTTON IF NOT ADMIN
if (role !== "ADMIN") {
    document.getElementById("addBtn").style.display = "none";
    document.getElementById("actionHeader").style.display = "none";
}

// LOGOUT
function logout() {
    localStorage.clear();
    window.location.href = "index.html";
}

// TOGGLE FORM
document.getElementById("addBtn")?.addEventListener("click", () => {
    const form = document.getElementById("formCard");
    form.style.display = form.style.display === "none" ? "block" : "none";
});

// LOAD RECORDS
async function loadRecords() {
    const res = await apiFetch("/records");

    if (!res.ok) {
        alert("Failed to load records");
        return;
    }

    const data = await res.json();

    const tbody = document.getElementById("recordsTable");
    tbody.innerHTML = "";

    data.content.forEach(r => {
        let actions = "";

        if (role === "ADMIN") {
            actions = `<button class="btn btn-danger btn-sm" onclick="deleteRecord(${r.id})">Delete</button>`;
        }

        const row = `
            <tr>
                <td>${r.amount}</td>
                <td>${r.type}</td>
                <td>${r.category}</td>
                <td>${r.date}</td>
                <td>${actions}</td>
            </tr>
        `;

        tbody.innerHTML += row;
    });
}

// ADD RECORD
document.getElementById("recordForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();

    const record = {
        amount: document.getElementById("amount").value,
        type: document.getElementById("type").value,
        category: document.getElementById("category").value,
        date: document.getElementById("date").value,
        description: document.getElementById("description").value
    };

    const res = await apiFetch("/records", {
        method: "POST",
        body: JSON.stringify(record)
    });

    if (res.ok) {
        alert("Record added");
        loadRecords();
    } else {
        alert("Failed to add record");
    }
});

// DELETE RECORD
async function deleteRecord(id) {
    if (!confirm("Delete this record?")) return;

    const res = await apiFetch(`/records/${id}`, {
        method: "DELETE"
    });

    if (res.ok) {
        loadRecords();
    } else {
        alert("Delete failed");
    }
}

loadRecords();