function loadCaptcha() {
	fetch("/Bank_Application/api/Captcha", {
		method: "GET",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
	})
		.then(response => response.text())
		.then(text => {
			drawCaptcha(text);
		});
}

function drawCaptcha(captchaText) {
	let canvas = document.getElementById("captchaCanvas");
	let ctx = canvas.getContext("2d");
	ctx.clearRect(0, 0, canvas.width, canvas.height);

	ctx.fillStyle = "#f8f8f8";
	ctx.fillRect(0, 0, canvas.width, canvas.height);

	// Add random lines for noise
	for (let i = 0; i < 5; i++) {
		ctx.strokeStyle = `rgba(0, 0, 0, ${Math.random()})`;
		ctx.beginPath();
		ctx.moveTo(Math.random() * canvas.width, Math.random() * canvas.height);
		ctx.lineTo(Math.random() * canvas.width, Math.random() * canvas.height);
		ctx.stroke();
	}

	ctx.fillStyle = "black";
	ctx.font = "bold 30px Arial";

	// Center the text
	let textWidth = ctx.measureText(captchaText).width;
	let x = (canvas.width - textWidth) / 2;
	let y = (canvas.height / 2) + 10;

	// Slightly distort text
	ctx.save();
	ctx.translate(x, y);
	ctx.rotate((Math.random() * 0.1) - 0.05);
	ctx.fillText(captchaText, 0, 0);
	ctx.restore();

	// Add noise dots
	for (let i = 0; i < 50; i++) {
		ctx.fillStyle = `rgba(0, 0, 0, ${Math.random() * 0.5})`;
		ctx.beginPath();
		ctx.arc(Math.random() * canvas.width, Math.random() * canvas.height, 1.5, 0, 2 * Math.PI);
		ctx.fill();
	}

	// Store CAPTCHA text in data attribute
	canvas.setAttribute("data-captcha", captchaText);
}


function validateCaptcha() {
	let userCaptcha = document.getElementById("captchaInput").value;

	fetch("/Bank_Application/api/Captcha", {
		method: "POST",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: "captcha=" + encodeURIComponent(userCaptcha)
	})
		.then(response => response.text())
		.then(result => {
			if (result == "success") {
				window.location.href = "index.html";
			}
			document.getElementById("captchaResult").innerText = result;
			loadCaptcha();
		});
}

window.onload = loadCaptcha;