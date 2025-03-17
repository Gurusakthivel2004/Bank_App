let filterId = '', filterAccount = '', filterBranch = '', filterStatus = '', filterType = '';
const usersPerPage = 8;
let cachedAccounts = [], usersCount, currentPageIndex = 0; filterOffset = 0;
const role = getCookie('role');
const token = getCookie('token');

async function fetchUsers() {
	try {
		if (!cachedAccounts[currentPageIndex]) {
			const userData = {
				offset: filterOffset,
				get: true,
				limit: usersPerPage,
			}
			if (filterId && filterId != '-1') userData.userId = Number(filterId);
			if (filterBranch && filterBranch != '-1') userData.branchId = Number(filterBranch);
			if (filterType) userData.role = filterType;
			if (filterStatus) userData.status = filterStatus;

			console.log(userData);
			const token = getCookie('token')
			const response = await fetch('http://localhost:8080/Bank_Application/api/User', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					'Authorization': `Bearer ${token}`
				},
				body: JSON.stringify(userData)
			});

			const usersResult = await response.json();
			if (usersResult.message == 'Invalid token' || usersResult.message == 'Invalid Access token') {
				deleteAllCookies();
				window.location.href = "error.html";
			} else if (usersResult.message == 'You dont have a account ') {
				document.querySelector('body').style.display = 'none';
				deleteAllCookies();
				sessionStorage.setItem('error', "No Account exists for the user.");
				window.location.href = "index.html";
			}
			const users = usersResult['users'];
			console.log(usersResult);

			usersCount = filterOffset == 0 ? usersResult["count"] : usersCount;
			if (response.ok) {
				cachedAccounts[currentPageIndex] = users;
				filterOffset += usersPerPage;
			} else {
				console.error('Error fetching accounts:', response.message || 'Unknown error');
				document.querySelector('body').style.display = 'none';
			}
		}
		renderUsers(cachedAccounts[currentPageIndex]);
		document.getElementById("prevButton").disabled = currentPageIndex === 0;
		updatePagination();
		document.querySelector('body').style.display = 'block';
	} catch (error) {
		console.log(error);
		document.querySelector('body').style.display = 'none';
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
	branchIdInput = document.getElementById("branchIdsearchInput").value.trim();
	filterBranch = branchIdInput;
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
	console.log("Role: ", role);
	if (role == null) {
		document.querySelector('body').style.display = 'none';
		window.location.href = "error.html";
	} else if (role === "Customer") {
		document.querySelector('body').style.display = 'none';
		window.location.href = "error.html";
	}
	fetchUsers();
	document.getElementById("customerIdsearchInput").addEventListener("input", applyFilters);
	document.getElementById("branchIdsearchInput").addEventListener("input", applyFilters);
	document.getElementById("userStatussearchInput").addEventListener("input", applyFilters);
	document.getElementById("userTypesearchInput").addEventListener("input", applyFilters);
});

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
							<p class="username" style="margin-left: -23px; color: #2b0444; font-weight: bold;">${user.fullname}</p>
							<p class="useremail"
									style="margin-left: -35px;font-weight: 600; color: #6c757d;">${user.email}</p>		
							<p class="userphone"
								style="margin-left: 25px; font-weight: bold; color: #4677bd;">${user.phone}</p>
								<p class="userusername" style="margin-left: 25px; font-weight: bold; color: #2b0444;">${user.username}</p>
								<p class="userstatus"
								style="margin-left: 0px; font-weight: bold; color: ${user.status === 'Active' ? '#28a745' : 'red'};">${user.status}</p>
							<p class="usercreatedAt" style="margin-left: 0px; font-weight: bold; color: #6c757d;">${getDate(user.createdAt, false)}</p>
						</div>`;

		userHistory.appendChild(userDiv);
	}
}

let originalValues = {};
let updatedValues = {};
let userFields = {};

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
	const userResult = result['users'][0];
	if (user.role != "Customer") {
		document.getElementById('customerDetails').style.display = 'none';
		document.getElementById('addressDiv').style.display = 'none';
		document.getElementById('roleDiv').style.display = 'flex';
		document.getElementById('branchIdDiv').style.display = 'flex';
	} else {
		document.getElementById('customerDetails').style.display = 'block';
		document.getElementById('addressDiv').style.display = 'flex';
		document.getElementById('roleDiv').style.display = 'none';
		document.getElementById('branchIdDiv').style.display = 'none';
	}
	toggleModal('newUserModal');
	if (!userResult) return;
	userFields['userId'] = userResult.id + '';
	userFields['role'] = userResult.role;
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

const createButton = () => {
	const buttonContainer = document.getElementById("dynamicButtons");

	buttonContainer.innerHTML = "";

	const selectDropdown = document.createElement("select");

	selectDropdown.id = "newUserRole";
	selectDropdown.style.padding = "8px";
	selectDropdown.style.fontSize = "16px";
	selectDropdown.style.color = "#2b0444";
	selectDropdown.style.background = "#ffffff";
	selectDropdown.style.border = "1px solid #ddd";
	selectDropdown.style.borderRadius = "6px";
	selectDropdown.style.boxShadow = "0 2px 4px rgba(0, 0, 0, 0.2)";
	selectDropdown.style.cursor = "pointer";

	const options = [
		{ value: "Customer", text: "Customer" },
		{ value: "Staff", text: "Staff" }
	];

	options.forEach(option => {
		const opt = document.createElement("option");
		opt.value = option.value;
		opt.textContent = option.text;
		selectDropdown.appendChild(opt);
	});

	selectDropdown.addEventListener("click", (event) => {
		const selectedValue = event.target.value;
		if (selectedValue) {
			console.log(`Selected: ${selectedValue}`);
			handleCreateSelection(selectedValue);
		}
	});

	buttonContainer.appendChild(selectDropdown);

	buttonContainer.style.display = "none";

	const imgElement = document.querySelector("img.createButton");

	imgElement.addEventListener("mouseover", () => {
		buttonContainer.style.display = "block";
		buttonContainer.style.position = "absolute";

		const imgRect = imgElement.getBoundingClientRect();
		buttonContainer.style.left = `${imgRect.right}px`;
		buttonContainer.style.top = `${imgRect.top}px`;
		buttonContainer.style.zIndex = "100";
		buttonContainer.style.borderRadius = "6px";
		buttonContainer.style.padding = "8px";
	});

	imgElement.addEventListener("mouseout", (event) => {
		const relatedTarget = event.relatedTarget;
		if (!buttonContainer.contains(relatedTarget)) {
			buttonContainer.style.display = "none";
		}
	});

	buttonContainer.addEventListener("mouseleave", () => {
		buttonContainer.style.display = "none";
	});
};

createButton();

function handleCreateSelection(selectedValue) {
	console.log(selectedValue);
	if (selectedValue === "Customer") {
		document.getElementById('customerDetails').style.display = 'block';
		document.getElementById('addressDiv').style.display = 'flex';
		document.getElementById('roleDiv').style.display = 'none';
		document.getElementById('address').disabled = false;
		document.getElementById('branchIdDiv').style.display = 'none';
	} else if (selectedValue === "Staff") {
		document.getElementById('customerDetails').style.display = 'none';
		document.getElementById('addressDiv').style.display = 'none';
		document.getElementById('roleDiv').style.display = 'flex';
		document.getElementById('branchIdDiv').style.display = 'flex';
	}
	toggleModal('newUserModal');
	document.getElementById('editButton').style.display = 'none';
	document.getElementById('saveButton').style.display = 'none';
	document.getElementById('newUserButton').style.display = 'flex';

	document.getElementById('address').value = '';
	document.getElementById('address').style.border = "1px solid black";
	document.querySelectorAll("input").forEach(input => {
		input.disabled = false;
		input.style.border = "1px solid black";
		input.value = "";
	})
}

const newUser = _ => {
	const userRole = document.getElementById('newUserRole').value, inputData = { role: userRole };
	console.log(userRole);
	let inputElements = document.querySelectorAll("#userDetails input, #customerDetails input, #userDetails textarea, #customerDetails textarea"), errorCount = 0;
	if (userRole == "Staff") {
		inputElements = document.querySelectorAll("#userDetails input");
	}
	console.log("valid: ", validBranchIds);
	inputElements.forEach(input => {
		const key = input.id;
		if ((key == "role" || key == "branchId") && userRole == "Customer") return;
		const value = input.value.trim();

		const formattedKey = key
			.replace(/([A-Z])/g, ' $1')
			.replace(/^./, str => str.toUpperCase());


		const errorMessageElement = document.getElementById('errorMessage');
		if (input.hasAttribute("required") && !value) {
			errorMessageElement.style.display = "block";
			errorMessageElement.innerHTML = `${formattedKey} is empty. Please provide valid details.`;
			console.error(`The field ${formattedKey} is required but is empty.`);
			errorCount++;
			return;
		} else if (key == "branchId" && !validBranchIds.includes(parseInt(value))) {
			errorMessageElement.style.display = "block";
			errorMessageElement.innerHTML = `Please provide valid branch Id.`;
			console.error(`Please provide valid branch Id.`);
			errorCount++;
			return;
		}
		inputData[key] = value;
	});

	if (errorCount == 0) {
		console.log("Collected Input Data:", inputData);
		createNewUser(inputData);
	}
}

const createNewUser = async data => {
	console.log("Sending data to server:", data);
	const userDetailsResponse = await fetch('http://localhost:8080/Bank_Application/api/User', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json',
		},
		body: JSON.stringify(data)
	});
	const result = await userDetailsResponse.json();
	console.log(result);
	if (result.message == 'success') {
		successDisplayWithMsg("User created successfully.");
	} else {
		const errorMessageElement = document.getElementById('errorMessage');
		errorMessageElement.style.display = "block";
		errorMessageElement.innerHTML = result.message;
	}
};

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

function fetchDropdownOptions(query) {
	const dropdown = document.getElementById('dropdown');
	dropdown.innerHTML = '';

	if (!query.trim()) {
		dropdown.innerHTML = '<div class="dropdown-option" style="padding: 10px; color: grey;">Enter a branch ID</div>';
		dropdown.style.display = 'block';
		return;
	}

	fetch(`http://localhost:8080/Bank_Application/api/Branch?branchId=${query}&notExact=true`, {
		method: 'GET',
		headers: {
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`,
		},
	})
		.then(response => response.json())
		.then(data => {
			console.log(data);

			if (!data || data.length === 0) {
				dropdown.innerHTML = '<div class="dropdown-option" style="padding: 10px; color: grey;">Branch ID not found</div>';
				dropdown.style.display = 'block';
				return;
			}

			validBranchIds = [];
			data.forEach(branch => {
				const option = document.createElement('div');
				option.className = 'dropdown-option';
				option.style = `
                    padding: 10px;
                    cursor: pointer;
                    border-bottom: 1px solid #ddd;
                `;
				option.innerHTML = `
                    <div style="font-weight: bold; color: #2c3e50;">
                        ID: ${branch.id}
                    </div>
                    <div style="font-size: 12px; color: grey;">
                        Name: ${branch.name} <br>
                        IFSC: ${branch.ifscCode} <br>
                        Address: ${branch.address}
                    </div>
                `;
				validBranchIds.push(branch.id);

				option.onclick = () => {
					const input = document.getElementById('branchId');
					input.value = branch.id;
					dropdown.style.display = 'none';
				};
				dropdown.appendChild(option);
			});

			dropdown.style.display = 'block';
		})
		.catch(err => {
			console.error('Error fetching dropdown options:', err);
			dropdown.innerHTML = '<div class="dropdown-option" style="padding: 10px; color: red;">Error fetching data</div>';
			dropdown.style.display = 'block';
		});
}

document.addEventListener('click', (event) => {
	const dropdown = document.getElementById('dropdown');
	const input = document.getElementById('branchId');
	if (!dropdown || !input) {
		return;
	}
	if (!dropdown.contains(event.target) &&
		!input.contains(event.target)) {
		console.log('Clicked outside');
		dropdown.style.display = 'none';
	}
});


const inputFieldsIds = ["email", "phone", "address", "maritalStatus", "status", "role"];
let validBranchIds = [];

const toggleEditUser = () => {
	inputFieldsIds.forEach(id => {
		const inputField = document.getElementById(id);
		inputField.disabled = false;
		inputField.style.border = "1px solid";
		originalValues[id] = inputField.value;
	})
	document.getElementById('saveButton').style.display = 'block';
	document.getElementById('editButton').style.display = 'none';
	validBranchIds.push(parseInt(document.getElementById('branchId').value));
}
const toggleSaveAll = () => {
	let isValid = true;
	let borderSet = 0;
	inputFieldsIds.forEach(id => {
		const inputField = document.getElementById(id);
		const newValue = inputField.value.trim();
		if (!newValue) {
			if (!(role == 'Customer') && (id == 'address' || id == 'maritalStatus')) return;
			console.log(id, newValue);
			isValid = false;
			inputField.style.border = "1px solid red";
			borderSet++;
		}
		else if (id === "email" && newValue && !validateEmail(newValue)) {
			console.log('here2');
			isValid = false;
			inputField.style.border = '1px solid red';
			borderSet++;
		}
		else if (id === "phone" && newValue && !validatePhone(newValue)) {
			console.log('here3');
			isValid = false;
			inputField.style.border = '1px solid red';
			borderSet++;
		}
		else if (id === "branchId" && !validBranchIds.includes(parseInt(newValue))) {
			console.log('here4');
			isValid = false;
			inputField.style.border = '1px solid red';
			borderSet++;
			return;
		}
		else if (newValue && newValue !== originalValues[id]) {
			updatedValues[id] = newValue;
			inputField.style.border = "0";
		}
	});
	console.log(isValid, Object.keys(updatedValues).length, borderSet);
	if (!isValid || borderSet > 0 || Object.keys(updatedValues).length == 0) return;
	document.getElementById('saveButton').style.display = 'none';
	document.getElementById('editButton').style.display = 'block';
	inputFieldsIds.forEach(id => {
		const inputField = document.getElementById(id);
		inputField.disabled = true;
		inputField.style.border = "0px";
	});
	userFields.updatedValues = updatedValues;
	userFields.userId = userFields['userId'];
	updatedValues = {};
	sendToServer(userFields);
};

const validateEmail = (email) => {
	const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
	return emailRegex.test(email.trim());
};



const validatePhone = (phone) => {
	const phoneRegex = /^\d{10}$/;
	return phoneRegex.test(phone);
};

const successDisplayWithMsg = (msg) => {
	toggleModal('newUserModal')
	const successPop = document.getElementById('successModal');
	document.getElementById('successMessage').innerHTML = msg;
	successPop.style.display = 'flex';
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
		successDisplayWithMsg('Updated successfully!');
	} else {
		errorDisplay(successPop, result.message);
	}
	setTimeout(() => {
		successPop.style.display = 'none';
	}, 3000);
};