const getCookie = name => {
	const cookies = document.cookie.split('; ');
	for (let cookie of cookies) {
		const [key, value] = cookie.split('=');
		if (key === name) {
			return decodeURIComponent(value);
		}
	}
	return null;
}

const role = getCookie('role');
if (role != null) {
	window.history.back();
}

async function login(event) {
	event.preventDefault();

	const username = document.getElementById('user-name').value;
	const password = document.getElementById('password').value;

	const iv = CryptoJS.lib.WordArray.random(16);
	const secretKey = "770A8A65DA156D24EE2A093277530142";

	const key = CryptoJS.enc.Utf8.parse(secretKey);

	const encryptedPassword = CryptoJS.AES.encrypt(password, key, {
		iv: iv,
		mode: CryptoJS.mode.CBC,
		padding: CryptoJS.pad.Pkcs7
	});

	console.log("Encrypted Ciphertext (Hex):", encryptedPassword.ciphertext.toString(CryptoJS.enc.Hex));

	const encryptedBase64 = encryptedPassword.ciphertext.toString(CryptoJS.enc.Base64);
	console.log("Encrypted Ciphertext (Base64):", encryptedBase64);

	const ivBase64 = CryptoJS.enc.Base64.stringify(iv);

	console.log("Key:", secretKey);
	console.log("IV (Base64):", ivBase64);
	console.log("Encrypted Data (Base64):", encryptedBase64);

	console.log("Encrypted Ciphertext (Base64):", encryptedPassword.ciphertext.toString(CryptoJS.enc.Base64));
	console.log("IV (Base64):", iv.toString(CryptoJS.enc.Base64));

	const loginData = {
		username: username,
		password: encryptedBase64,
		iv: ivBase64
	};

	console.log(loginData);

	try {
		const response = await fetch('http://localhost:8080/Bank_Application/api/Login', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json'
			},
			body: JSON.stringify(loginData)
		});

		const result = await response.json();
		console.log(result);

		if (result.message === "success") {
			await new Promise(resolve => setTimeout(resolve, 50));

			if (password === 'default') {
				toggleModal('passwordChangeModal');
			} else {
				const role = getCookie('role');
				console.log('role: ', role);
				if (role === 'Customer') {
					window.location.href = 'dashboard.html';
				} else {
					window.location.href = 'emp-dashboard.html';
				}
			}
		} else if (result.message == 'You dont have a account ') {
			deleteAllCookies();	
			sessionStorage.setItem('error', "No Account exists for the user.");
			window.location.href = "index.html";
		} else {
			document.getElementById('errormessage').textContent = result.message || 'Login failed.';
		}
	} catch (error) {
		console.error('Error during login:', error);
	}
}

const toggleModal = modalId => {
	const modal = document.getElementById(modalId);
	modal.style.display = modal.style.display === 'none'
		|| modal.style.display === '' ? 'flex' : 'none';
}

const oauthsignin = () => {
	window.location.href = 'http://localhost:8080/Bank_Application/api/oauth?provider=google';
};


const togglePasswordVisibility = (inputId, button) => {
	const passwordInput = document.getElementById(inputId);
	const type = passwordInput.getAttribute('type') === 'password' ? 'text'
		: 'password';
	passwordInput.setAttribute('type', type);

	const img = button.querySelector('img');
	img.src = type === 'password' ? './images/eye.svg'
		: './images/eyeclose.svg';
	img.alt = type === 'password' ? 'Show password'
		: 'Hide password';
}
function deleteAllCookies() {
	document.cookie.split(";").forEach((cookie) => {
		document.cookie = cookie.split("=")[0] +
			"=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/Bank_Application;";
	});
}

const submitPasswordChange = async _ => {
	const currentPassword = document.getElementById('currentPassword').value;
	const newPassword = document.getElementById('newPassword').value;
	const confirmPassword = document.getElementById('confirmPassword').value;
	if (newPassword != confirmPassword) {
		document.getElementById('passwordmessage').textContent = 'Passwords do not match. Try again.';
		return;
	}
	if (newPassword == currentPassword) {
		document.getElementById('passwordmessage').textContent = 'Please enter a new password.';
		return;
	}
	const passwordData = {
		currentPassword: currentPassword,
		newPassword: newPassword
	};
	try {
		const response = await fetch('http://localhost:8080/Bank_Application/api/User', {
			method: 'PUT',
			headers: {
				'Content-Type': 'application/json',
			},
			body: JSON.stringify(passwordData)
		});
		const result = await response.json();
		console.log(result);
		if (result.message == 'success') {
			sessionStorage.setItem('passwordChangeSuccess', 'true');
			deleteAllCookies();
			window.location.href = 'index.html';
		} else if (result.message == 'You dont have a account') {
			deleteAllCookies();
			sessionStorage.setItem('error', "No Account exists for the user.");
			window.location.href = "index.html";
		}
		else {
			const passwordMessage = document.getElementById('passwordmessage');
			passwordMessage.textContent = result.error;
			passwordMessage.style.backgroundColor = 'red';
			passwordMessage.style.color = 'white';
		}
	} catch (error) {
		console.log(error);
		document.getElementById('passwordmessage').textContent = 'An error occured. Try again.';
		return;
	}
}

window.onload = () => {
	const successMessage = sessionStorage.getItem('passwordChangeSuccess');
	const errorMessage = sessionStorage.getItem('error');
	if (errorMessage) {
		document.getElementById('successPopup').style.display = 'block';
		document.getElementById('successPopup').style.backgroundColor = 'red';
		document.getElementById('successPopup').style.color = 'white';
		document.getElementById('successPopup').textContent = errorMessage;
		sessionStorage.removeItem('error');
		setTimeout(() => {
			document.getElementById('successPopup').style.display = 'none';
		}, 3000);
	}
	if (successMessage) {
		document.getElementById('successPopup').style.display = 'block';
		sessionStorage.removeItem('passwordChangeSuccess');
		setTimeout(() => {
			document.getElementById('successPopup').style.display = 'none';
		}, 3000);
	}
};