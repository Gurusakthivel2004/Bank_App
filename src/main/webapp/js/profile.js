const userRole = sessionStorage.getItem("role");
if (userRole == "Customer") {
	const createButton = document.getElementById("create-user-item");
	if (createButton != null) {
		createButton.style.display = "none";
	}
}

const logout = async _ => {
	try {
		const token = sessionStorage.getItem('token');
		const response = await fetch('http://localhost:8080/Bank_Application/api/Logout', {
			method: 'DELETE',
			headers: {
				'Content-Type': 'application/json',
				'Authorization': `Bearer ${token}`
			},
		});
		const result = await response.json();
		if (result) {
			sessionStorage.clear();
			window.location.href = 'index.html';
		}
	} catch (error) {
		console.error('Error during fetch or processing:', error);
	}
}
function closeModal() {
	const modal = document.getElementById('successModal');
	modal.style.animation = 'fadeOut 0.5s ease-out';
	setTimeout(() => {
		modal.style.display = 'none';
	}, 500);
}
const toggleProfilePanel = _ => {
	const panel = document.getElementById('profilePanel');
	panel.classList.toggle('open');
}

document.querySelectorAll('.createOption').forEach(item => {
	item.addEventListener('click', _ => {
		sessionStorage.setItem("createUserRole", item.innerHTML);
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
		document.getElementById('statusSave').style.display = "none";

		if (modal.style.display === "flex") {
			if (role == "Manager") {
				document.getElementById('branchEdit').style.display = "block";
			}
			document.getElementById('statusEdit').style.display = "block";
		}
		document.getElementById('accountbranchId').disabled = true;
		document.getElementById('dropdown').style.display = "none";
		document.getElementById('dropdownStatus').style.display = "none";
	}
	if (modalId == "newUserModal") {
		console.log('hi');
		document.getElementById('saveButton').style.display = 'none';
		document.getElementById('editButton').style.display = 'block';
		document.getElementById('newUserButton').style.display = 'none';
		document.getElementById('dropdown').style.display = 'none';
		document.getElementById('errorMessage').style.display = "none";
		document.querySelectorAll("input").forEach(input => {
			input.disabled = false;
			input.style.border = "0px solid ";
		})
	}
	if (modalId == "branchDetailsModal") {
		setBranchDetails(branchDetails, account.branchId);
		const inputElements = document.querySelectorAll("#branchDetailsModal input");
		const addressInput = document.getElementById('branchaddressInput');
		inputElements.forEach(input => {
			input.disabled = true;
			input.style.border = '0px solid #ccc';
		})
		addressInput.disabled = true;
		addressInput.style.border = '0px solid #ccc';
		document.getElementById('saveBranchButton').style.display = 'none';
		document.getElementById('branchIfscdiv').style.display = 'flex';
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
		document.getElementById('passwordmessage').style.display = 'block';
		document.getElementById('passwordmessage').textContent = 'Passwords do not match. Try again.';
		return;
	}
	if (newPassword == currentPassword) {
		document.getElementById('passwordmessage').style.display = 'block';
		document.getElementById('passwordmessage').textContent = 'Please enter a new password.';
		return;
	}
	const passwordData = {
		currentPassword: currentPassword,
		newPassword: newPassword
	};
	console.log(passwordData)
	try {
		const token = sessionStorage.getItem('token')
		const response = await fetch('http://localhost:8080/Bank_Application/api/User', {
			method: 'PUT',
			headers: {
				'Content-Type': 'application/json',
				'Authorization': `Bearer ${token}`
			},
			body: JSON.stringify(passwordData)
		});
		const result = await response.json();
		console.log(result)
		const passwordMessage = document.getElementById('passwordmessage');
		if (result.message == "success") {
			sessionStorage.setItem('passwordChangeSuccess', 'true');
			window.location.href = 'index.html';
		} else {
			passwordMessage.style.display = 'block'
			passwordMessage.textContent = result.message;
		}
	} catch (error) {
		passwordMessage.textContent = 'An error occured. Try again.';
		return;
	}
}

let keys = Object.keys(sessionStorage), i = keys.length;
while (i--) {
	const profileElement = document.getElementById('profile-' + keys[i]);
	if (profileElement != null) {
		profileElement.textContent = sessionStorage.getItem(keys[i]);
	}
}

