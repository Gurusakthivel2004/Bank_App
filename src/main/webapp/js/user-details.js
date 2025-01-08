let filterId = '', filterAccount = '', filterBranch = '', filterStatus = '', filterType = '';
const usersPerPage = 8;
let cachedAccounts = [], usersCount, currentPageIndex = 0; filterOffset = 0;
const role = localStorage.getItem("role");
const token = localStorage.getItem('token');

async function fetchUsers() {
	try {
		if (!cachedAccounts[currentPageIndex]) {
			const userData = {
				offset: filterOffset,
				get: true,
				limit: usersPerPage,
			}
			if (role === "Employee") {
				filterBranch = localStorage.getItem("branchId");
			}
			if (filterId && filterId != '-1') userData.userId = Number(filterId);
			if (filterBranch && filterBranch != '-1') userData.branchId = Number(filterBranch);
			if (filterType) userData.role = filterType;
			if (filterStatus) userData.status = filterStatus;

			console.log(userData);
			const token = localStorage.getItem('token')
			const response = await fetch('http://localhost:8080/Bank_Application/api/User', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					'Authorization': `Bearer ${token}`
				},
				body: JSON.stringify(userData)
			});

			const usersResult = await response.json();
			const users = usersResult["users"];
			console.log(usersResult);

			usersCount = filterOffset == 0 ? usersResult["count"] : usersCount;
			if (response.ok) {
				cachedAccounts[currentPageIndex] = users;
				filterOffset += usersPerPage;
			} else {
				console.error('Error fetching accounts:', response.message || 'Unknown error');
				renderAccounts([]);
			}
		}
		renderUsers(cachedAccounts[currentPageIndex]);
		document.getElementById("prevButton").disabled = currentPageIndex === 0;
		updatePagination();
	} catch (error) {
		console.log(error);
	}
}

let totalPages;

function updatePagination() {
	const pageNumbersContainer = document.getElementById("pageNumbers");
	pageNumbersContainer.innerHTML = '';
	console.log(usersCount, usersPerPage)
	totalPages = Math.ceil(usersCount / usersPerPage) || 1;
	const currentPage = currentPageIndex + 1;

	console.log("curr, total: ", currentPageIndex, totalPages)
	document.getElementById('nextButton').disabled = currentPageIndex == totalPages - 1;
	const paginationText = `Page ${currentPage} of ${totalPages}`;
	pageNumbersContainer.innerHTML = `<span>${paginationText}</span>`;
}

function nextPage() {
	document.getElementById('nextButton').disabled = currentPageIndex == totalPages - 2;
	if (currentPageIndex < totalPages - 1) {
		currentPageIndex++;
		fetchUsers();
	}
}

function prevPage() {
	document.getElementById('prevButton').disabled = currentPageIndex == 0;
	if (currentPageIndex > 0) {
		document.getElementById('nextButton').disabled = false;
		currentPageIndex--;
		fetchUsers();
	}
}

let previousBranch = '', previousId = '', branchIdInput = '', previousType = '', previousStatus = '';

function applyFilters() {
	const idInput = document.getElementById("customerIdsearchInput").value.trim();
	const statusInput = document.getElementById("userStatussearchInput").value.trim();
	const typeInput = document.getElementById("userTypesearchInput").value.trim();
	if (role == "Manager") {
		branchIdInput = document.getElementById("branchIdsearchInput").value.trim();
		filterBranch = branchIdInput;
	}
	filterId = idInput ? idInput : role != "Customer" ? 0 : -1;
	filterStatus = statusInput;
	filterType = typeInput;
	console.log(previousId, idInput);

	if (previousBranch != branchIdInput || previousId != idInput || previousStatus != statusInput || previousType != typeInput) {
		cachedAccounts = [];
		currentPageIndex = 0;
		filterOffset = 0;
		fetchUsers();
	}
	previousBranch = branchIdInput;
	previousId = idInput;
	previousType = typeInput;
	previousStatus = statusInput;
}

document.addEventListener("DOMContentLoaded", () => {
	console.log(role);
	if (role == null) {
		window.history.back();
	} else if (role === "Customer") {
		alert("You do not have permission to access this page.");
		window.history.back();
	} else if (role == "Employee") {
		document.getElementById('branchidfilter').style.display = 'none';
		filterBranch = '-1';
	}
	fetchUsers();
	document.getElementById("customerIdsearchInput").addEventListener("input", applyFilters);
	document.getElementById("branchIdsearchInput").addEventListener("input", applyFilters);
	document.getElementById("userStatussearchInput").addEventListener("input", applyFilters);
	document.getElementById("userTypesearchInput").addEventListener("input", applyFilters);
});

const getDate = (millis, time) => {
	const date = new Date(millis);
	const day = String(date.getDate()).padStart(2, '0');
	const month = String(date.getMonth() + 1).padStart(2, '0');
	const year = date.getFullYear();
	if (!time) {
		return `${day}/${month}/${year}`;
	}
	const hours = String(date.getHours()).padStart(2, '0');
	const minutes = String(date.getMinutes()).padStart(2, '0');
	return `${day}/${month}/${year}, ${hours}:${minutes}`;
}

function renderUsers(users) {
	const userHistory = document.querySelector(".user-data");
	userHistory.innerHTML = '';
	if (users == null || users.length === 0) {
		document.getElementById('buttons').style.display = "none";
		const userDiv = document.createElement("div");
		userDiv.className = "account-item d-flex row mb-1";
		userDiv.style = "background-color: #ffffff; height: 650px; padding: 10px; border-bottom: 1px solid #ddd; border-radius: 10px;";
		userDiv.innerHTML = `
						<img height="500px" style="margin:auto;width: 50%" src="./images/notfound.avif" alt="" />
			          `;
		userHistory.appendChild(userDiv);
		return;
	}

	for (let i = 0; i < users.length; i++) {
		let user = users[i];
		document.getElementById('buttons').style.display = "flex";
		const userDiv = document.createElement("div");
		userDiv.onclick = () => userClick(user);
		userDiv.className = "account-item d-flex align-items-center my-2";
		userDiv.style = "background-color: white; padding: 10px; border-bottom: 1px solid #ddd; border-radius: 10px;cursor: pointer;";
		userDiv.innerHTML = `
							<p class="userid"
								style="margin-left:10px; font-weight: bold; color: #2b0444;">${user.id}</p>
							<p class="username" style="margin-left: -23px; color: #2b0444;">${user.fullname}</p>
							<p class="useremail"
									style="margin-left: -35px;font-weight: 600; color: #2b0444;">${user.email}</p>		
							<p class="userphone"
								style="margin-left: 25px; font-weight: bold; color: #28a745;">${user.phone}</p>
								<p class="userusername" style="margin-left: 25px; font-weight: bold; color: #2b0444;">${user.username}</p>
								<p class="userstatus"
								style="margin-left: 0px; font-weight: bold; color: ${user.status === 'Active' ? 'blue' : 'red'};">${user.status}</p>
							<p class="usercreatedAt" style="margin-left: 0px; color: #2b0444;">${getDate(user.createdAt, false)}</p>
						</div>`;

		userHistory.appendChild(userDiv);
	}
}

let originalValues = {};
let updatedValues = {};

const userClick = async user => {
	const userDetailsResponse = await fetch(`http://localhost:8080/Bank_Application/api/User?userId=${user.id}&role=${user.role}`, {
		method: 'GET',
		headers: {
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`
		},
	});
	const result = await userDetailsResponse.json();
	console.log(result);
	const resultKey = user.role == "Customer" ? "customerDetail" : "staff";
	const userResult = result[resultKey][0];
	if (user.role != "Customer") {
		document.getElementById('customerDetails').style.display = 'none';
		document.getElementById('addressDiv').style.display = 'none';
		document.getElementById('roleDiv').style.display = 'flex';
	} else {
		document.getElementById('customerDetails').style.display = 'block';
		document.getElementById('addressDiv').style.display = 'flex';
		document.getElementById('roleDiv').style.display = 'none';
	}
	toggleModal('newUserModal');
	if (!userResult) return;
	updatedValues['userId'] = userResult.id;
	updatedValues['role'] = userResult.role;
	Object.keys(userResult).forEach(key => {
		const inputField = document.getElementById(key);
		if (inputField) {
			inputField.value = userResult[key];
			inputField.style.border = "0px";
			inputField.style.backgroundColor = 'transparent';
			inputField.disabled = true;
		}
	});
}

const createButton = _ => {
	toggleModal('newUserModal');
	const modalDiv = document.getElementById('newUserModal');
	const inputFields = modalDiv.querySelectorAll('input');
	inputFields.forEach(input => {
		input.disabled = false;
		input.value = "";
		input.style.border = "1px solid";
	});
	document.getElementById('customerAddress').value = "";
	document.getElementById('customerAddress').disabled = false;
	document.getElementById('customerAddress').style.border = "1px solid";
}

document.querySelectorAll('.accNumberInput').forEach(item => {
	item.addEventListener('click', function(event) {
		const parentDiv = this.closest('.detail-item');
		const dropdownMenu = parentDiv.querySelector(".dropdown-menu");
		if (dropdownMenu != null) {
			dropdownMenu.style.display = dropdownMenu.style.display == "block" ? "none" : "block";
		}
	})
})

document.querySelectorAll('.dropdown-item').forEach(item => {
	item.addEventListener('click', function(event) {
		const parentDiv = this.closest('.detail-item');
		const selectedText = this.innerHTML;
		const dropdownTextElement = parentDiv.querySelector('.accNumberInput');
		const dropdownMenu = parentDiv.querySelector(".dropdown-menu");
		dropdownMenu.style.display = "none"
		dropdownTextElement.value = selectedText;
		parentDiv.querySelectorAll('.dropdown-item').forEach(item => item.classList.remove('active'));
		this.classList.add('active');
	});
});
const inputFieldsIds = ["email", "phone", "address", "maritalStatus", "status", "role"];
const toggleEditUser = () => {

	inputFieldsIds.forEach(id => {
		const inputField = document.getElementById(id);
		inputField.disabled = false;
		inputField.style.border = "1px solid";
		originalValues[id] = inputField.value;
	})
	document.getElementById('saveButton').style.display = 'block';
	document.getElementById('editButton').style.display = 'none';
}
const toggleSaveAll = () => {
	let isValid = true;
	let borderSet = 0;
	inputFieldsIds.forEach(id => {
		const inputField = document.getElementById(id);
		const newValue = inputField.value.trim();
		if (!newValue) {
			isValid = false;
			inputField.style.border = "1px solid red";
			borderSet++;
		}
		if (id === "customerEmail" && newValue && !validateEmail(newValue)) {
			isValid = false;
			inputField.style.border = '1px solid red';
			borderSet++;
		}
		else if (id === "customerPhone" && newValue && !validatePhone(newValue)) {
			isValid = false;
			inputField.style.border = '1px solid red';
			borderSet++;
		}
		else if (newValue && newValue !== originalValues[id]) {
			updatedValues[id] = newValue;
			inputField.style.border = "0";
		} else {
			inputField.style.border = "0";
			isValid = false;
		}
	});
	if (!isValid && borderSet > 0) return;
	document.getElementById('saveButton').style.display = 'none';
	document.getElementById('editButton').style.display = 'block';
	inputFieldsIds.forEach(id => {
		const inputField = document.getElementById(id);
		inputField.disabled = true;
		inputField.style.border = "0px";
	});
	sendToServer(updatedValues);
	toggleModal('newUserModal');
};

const validateEmail = (email) => {
	const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
	return emailRegex.test(email);
};

const validatePhone = (phone) => {
	const phoneRegex = /^\d{10}$/;
	return phoneRegex.test(phone);
};

const successDisplay = (successPop) => {
	successPop.textContent = 'Updated successfully!';
	successPop.style.backgroundColor = 'green';
	successPop.style.display = 'block';
}

const errorDisplay = (successPop, message) => {
	successPop.textContent = message;
	successPop.style.backgroundColor = 'red';
	successPop.style.color = 'white';
	successPop.style.display = 'block';
}

const sendToServer = async data => {
	console.log("Sending data to server:", data);
	const userDetailsResponse = await fetch('http://localhost:8080/Bank_Application/api/User', {
		method: 'PUT',
		headers: {
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`
		},
		body: JSON.stringify(data)
	});
	const result = await userDetailsResponse.json();
	const successPop = document.getElementById('successPopup');
	if (result.message == 'success') {
		successDisplay(successPop);
	} else {
		errorDisplay(successPop, result.message);
	}
	setTimeout(() => {
		successPop.style.display = 'none';
	}, 3000);
};

document.getElementById('saveButton').addEventListener('click', toggleSaveAll);

