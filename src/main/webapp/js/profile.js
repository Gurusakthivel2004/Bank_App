const userRole = localStorage.getItem("role");
if (userRole == "Customer") {
	const createButton = document.getElementById("create-user-item");
	if (createButton != null) {
		createButton.style.display = "none";
	}
}

const logout = async _ => {
	try {
		const token = localStorage.getItem('token');
		const response = await fetch('http://localhost:8080/Bank_Application/api/Logout', {
			method: 'DELETE',
			headers: {
				'Content-Type': 'application/json',
				'Authorization': `Bearer ${token}`
			},
		});
		const result = await response.json();
		if (result) {
			localStorage.clear();
			window.location.href = 'index.html';
		}
	} catch (error) {
		console.error('Error during fetch or processing:', error);
	}
}

const toggleProfilePanel = _ => {
	const panel = document.getElementById('profilePanel');
	panel.classList.toggle('open');
}

document.querySelectorAll('.createOption').forEach(item => {
	item.addEventListener('click', _ => {
		localStorage.setItem("createUserRole", item.innerHTML);
		window.location.href = "create-user.html";
	})
})

const toggleSubmenu = () => {
	const submenu = document.getElementById('userOptions');
	submenu.style.display = submenu.style.display === 'none' ? 'block' : 'none';
}

document.getElementById('profileButton').addEventListener('click',
	toggleProfilePanel);

const toggleModal = modalId => {
	const modal = document.getElementById(modalId);
	modal.style.display = modal.style.display === 'none'
		|| modal.style.display === '' ? 'flex' : 'none';
	if (modalId == "accountDetailsModal") {
		document.getElementById('accountbranchId').style.border = "0";
		document.getElementById('branchSave').style.display = "none";
		if (role == "Manager") {
			if(modal.style.display === "flex") {
				document.getElementById('branchEdit').style.display = "block";
			}
		}
		document.getElementById('accountbranchId').disabled = true;
	}
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
	console.log(passwordData)
	try {
		const token = localStorage.getItem('token')
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

let keys = Object.keys(localStorage), i = keys.length;
while (i--) {
	const profileElement = document.getElementById('profile-' + keys[i]);
	if (profileElement != null) {
		profileElement.textContent = localStorage.getItem(keys[i]);
	}
}

