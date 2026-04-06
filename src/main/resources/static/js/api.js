const API_BASE = "https://finflow-vpm9.onrender.com/api";

async function apiFetch(endpoint, options = {}) {
    const token = localStorage.getItem("token");

    const res = await fetch(API_BASE + endpoint, {
        headers: {
            "Content-Type": "application/json",
            ...(token && { Authorization: "Bearer " + token })
        },
        ...options
    });

    return res;
}