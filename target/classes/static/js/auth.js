// LOGIN
document.getElementById("loginForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();

    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    const res = await apiFetch("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password })
    });

    if (res.ok) {
        const data = await res.json();

        localStorage.setItem("token", data.token);
        localStorage.setItem("role", data.role);

        window.location.href = "dashboard.html";
    } else {
        alert("Login failed");
    }
});

// REGISTER
document.getElementById("registerForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();

    const data = {
        name: document.getElementById("name").value,
        email: document.getElementById("email").value,
        password: document.getElementById("password").value
    };

    const res = await apiFetch("/auth/register", {
        method: "POST",
        body: JSON.stringify(data)
    });

    if (res.ok) {
        alert("Registered successfully");
        window.location.href = "index.html";
    } else {
        alert("Registration failed");
    }
});