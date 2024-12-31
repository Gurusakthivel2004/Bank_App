async function login(event) {
	event.preventDefault();

	const username = document.getElementById('user-name').value;
	const password = document.getElementById('password').value;

	const loginData = {
		username: username,
		password: password,
	};

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
		if (result.message == "success") {
			localStorage.setItem('token', result.token); 
			localStorage.setItem('fullname', result.fullname);
			localStorage.setItem('email', result.email);
			localStorage.setItem('phone', result.phone);
			localStorage.setItem('status', result.status);
			localStorage.setItem('role', result.role);
			if(result.branchId !== null) {
				localStorage.setItem('branchId', result.branchId);
			} 

			if (password === 'default') {
				toggleModal('passwordChangeModal');
			} else {
				if (result.role === 'Customer') {
					window.location.href = 'dashboard.html';
				} else {
					window.location.href = 'emp-dashboard.html';
				}
			}
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
		const token = localStorage.getItem('token');
		const response = await fetch('http://localhost:8080/Bank_Application/api/Profile', {
			method: 'PUT',
			headers: {
				'Content-Type': 'application/json',
				'Authorization': `Bearer ${token}`
			},
			body: JSON.stringify(passwordData)
		});
		const result = await response.json();
		if (result.success) {
			localStorage.setItem('passwordChangeSuccess', 'true');
			window.location.href = 'index.html';
		} else {
			const passwordMessage = document.getElementById('passwordmessage');
			passwordMessage.textContent = result.error;
			passwordMessage.style.backgroundColor = 'red';
			passwordMessage.style.color = 'white';
		}
	} catch (error) {
		document.getElementById('passwordmessage').textContent = 'An error occured. Try again.';
		return;
	}
}

window.onload = () => {
	const successMessage = localStorage.getItem('passwordChangeSuccess');
	const errorMessage = localStorage.getItem('error');
	if (errorMessage) {
		document.getElementById('successPopup').style.display = 'block';
		document.getElementById('successPopup').style.backgroundColor = 'red';
		document.getElementById('successPopup').style.color = 'white';
		document.getElementById('successPopup').textContent = errorMessage;
		localStorage.removeItem('error');
		setTimeout(() => {
			document.getElementById('successPopup').style.display = 'none';
		}, 3000);
	}
	if (successMessage) {
		document.getElementById('successPopup').style.display = 'block';
		localStorage.removeItem('passwordChangeSuccess');
		setTimeout(() => {
			document.getElementById('successPopup').style.display = 'none';
		}, 3000);
	}
};