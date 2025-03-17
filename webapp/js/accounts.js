let filterId = '';
let filterAccount = '';
let filterBranch = '';
let filterStatus = '';
let filterType = '';
const accountsPerPage = 8;
let cachedAccounts = [];
let accountsCount;
let currentPageIndex = 0;
let filterOffset = 0;
const role = getCookie("role");

async function fetchAccounts() {
	try {
		if (!cachedAccounts[currentPageIndex]) {
			const accountData = {
				offset: filterOffset,
				get: true,
				limit: accountsPerPage,
			}
			if (filterId && filterId != '-1') accountData.userId = Number(filterId);
			if (filterBranch && filterBranch != '-1') accountData.branchId = Number(filterBranch);
			if (filterAccount && Number.isFinite(Number(filterAccount))) accountData.accountNumber = Number(filterAccount);
			if (filterType) accountData.accountType = filterType;
			if (filterStatus) accountData.status = filterStatus;

			console.log(accountData);
			const response = await fetch('http://localhost:8080/Bank_Application/api/Account', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify(accountData)
			});
			const accountsResult = await response.json();
			if (accountsResult.message == 'Invalid token' || accountsResult.message == 'Invalid Access token') {
				document.querySelector('body').style.display = 'none';
				deleteAllCookies();
				window.location.href = "error.html";
			} else if (accountsResult.message == 'You dont have a account ') {
				document.querySelector('body').style.display = 'none'; 
				deleteAllCookies();
				sessionStorage.setItem('error', "No Account exists for the user.");
				window.location.href = "index.html";
			}

			const accounts = accountsResult["accounts"];
			console.log(accountsResult);

			accountsCount = filterOffset == 0 ? accountsResult["count"] : accountsCount;
			if (response.ok) {
				cachedAccounts[currentPageIndex] = accounts;
				filterOffset += accountsPerPage;
			} else {
				console.error('Error fetching accounts:', response.message || 'Unknown error');
				document.querySelector('body').style.display = 'none';
			}
		}
		renderAccounts(cachedAccounts[currentPageIndex]);
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
	console.log(accountsCount, accountsPerPage)
	totalPages = Math.ceil(accountsCount / accountsPerPage) || 1; // Calculate total pages
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
		fetchAccounts();
	}
}

function prevPage() {
	document.getElementById('prevButton').disabled = currentPageIndex == 0;
	if (currentPageIndex > 0) {
		document.getElementById('nextButton').disabled = false;
		currentPageIndex--;
		fetchAccounts();
	}
}

let previousBranch = '', previousId = '', previousAccount = '', branchIdInput = '', previousType = '', previousStatus = '';

function applyFilters() {
	const idInput = document.getElementById("customerIdsearchInput").value.trim();
	const accountInput = document.getElementById("accountsearchInput").value.trim();
	const statusInput = document.getElementById("accountStatussearchInput").value.trim();
	const typeInput = document.getElementById("accountTypesearchInput").value.trim();
	if (role == "Manager") {
		branchIdInput = document.getElementById("branchIdsearchInput").value.trim();
		filterBranch = branchIdInput;
	} if (accountInput == '') {
		filterId = idInput ? idInput : -1;
	} else {
		filterId = idInput ? idInput : role != "Customer" ? 0 : -1;
	}
	filterAccount = accountInput;
	filterStatus = statusInput;
	filterType = typeInput;
	console.log(previousId, idInput);

	if (previousAccount != accountInput || previousBranch != branchIdInput || previousId != idInput || previousStatus != statusInput || previousType != typeInput) {
		console.log('here1');
		if (!(filterAccount.length > 0 && filterAccount.length < 4) && Number.isFinite(Number(filterAccount))) {
			console.log('here');
			cachedAccounts = [];
			currentPageIndex = 0;
			filterOffset = 0;
			fetchAccounts();
		}
	}
	if (Number.isFinite(Number(filterAccount))) {
		previousAccount = filterAccount;
	}
	previousAccount = accountInput;
	previousBranch = branchIdInput;
	previousId = idInput;
	previousType = typeInput;
	previousStatus = statusInput;
}

document.addEventListener("DOMContentLoaded", () => {
	console.log(role);
	if (role == null || role === "Customer") {
		document.querySelector('body').style.display = 'none';
		window.location.href = "error.html";
	} else if (role == "Employee") {
		document.getElementById('branchidfilter').style.display = 'none';
		filterBranch = '-1';
	}
	fetchAccounts();
	document.getElementById("customerIdsearchInput").addEventListener("input", applyFilters);
	document.getElementById("branchIdsearchInput").addEventListener("input", applyFilters);
	document.getElementById("accountsearchInput").addEventListener("input", applyFilters);
	document.getElementById("accountStatussearchInput").addEventListener("input", applyFilters);
	document.getElementById("accountTypesearchInput").addEventListener("input", applyFilters);
});

const updateBranch = async _ => {
	const branchId = document.getElementById('accountbranchId');
	branchId.disabled = false;
	branchId.style.border = "1px solid";
	document.getElementById('branchSave').style.display = "block";
	document.getElementById('branchEdit').style.display = "none";
	initialBranchId = parseInt(document.getElementById('accountbranchId').value);
}

const accountClick = async account => {
	const userRole = account.accountType == "Operational" ? "Employee" : "Customer";
	const userDetailsResponse = await fetch(`http://localhost:8080/Bank_Application/api/User?userId=${account.userId}&role=${userRole}`, {
		method: 'GET',
		headers: {
			'Content-Type': 'application/json'
		},
	});
	const result = await userDetailsResponse.json();
	console.log(result);
	const userResult = result['users'];
	document.getElementById('accountbranchId').value = account.branchId;
	document.getElementById('accountfullname').innerText = userResult[0].fullname;
	document.getElementById('accountphone').innerText = userResult[0].phone;
	document.getElementById('accountNumber2').innerText = account.accountNumber;
	document.getElementById('accountStatus').value = account.status
	toggleModal('accountDetailsModal');
	initialStatus = accountStatusInput.value;
}

function renderAccounts(accounts) {
	const accountHistory = document.querySelector(".account-data");
	accountHistory.innerHTML = '';
	console.log(accounts);
	var listed = 0;
	if (accounts == null || accounts.length === 0) {
		document.getElementById('buttons').style.display = "none";
		const accountDiv = document.createElement("div");
		accountDiv.className = "account-item d-flex row mb-1";
		accountDiv.style = "background-color: #ffffff; height: 650px; padding: 10px; border-bottom: 1px solid #ddd; border-radius: 10px;";
		accountDiv.innerHTML = `
						<img height="500px" style="margin:auto;width: 50%" src="./images/notfound.avif" alt="" />
			          `;
		accountHistory.appendChild(accountDiv);
		return;
	}
	for (let i = 0; i < accounts.length; i++) {
		let account = accounts[i].instance;
		let branch = accounts[i].joinedFields;
		if (role == "Employee" && account.accountType == "Operational") continue;
		document.getElementById('buttons').style.display = "flex";
		const accountDiv = document.createElement("div");
		listed++;
		accountDiv.onclick = () => accountClick(account);
		accountDiv.className = "account-item d-flex align-items-center my-2";
		accountDiv.style = "background-color: white; padding: 10px; border-bottom: 1px solid #ddd; border-radius: 10px;cursor: pointer;";
		accountDiv.innerHTML = `
							<p class="userId"
								style="width: 5%; margin-left:10px; font-weight: bold; color: #2b0444;">${account.userId}</p>
							<p class="accountNumber" style="width: 15%; color: #2b0444;">${account.accountNumber}</p>
							<p class="branchId"
									style="width: 5%; font-weight: bold; color: #2b0444;">${account.branchId}</p>		
							<p class="accbalance"
								style="width: 10%; font-weight: bold; color: #4677bd;">${account.balance.toLocaleString()}</p>
								<p class="branchName" style="width: 5%; font-weight: bold; color: #6c757d;">${branch.name}</p>
								
								<p class="acctype" style="width: 15%; font-weight: bold; color: #2b0444;">${account.accountType}</p>
								<p class="accstatus"
								style="width: 15%; font-weight: bold; color: ${account.status === 'Active' ? '#28a745' : 'red'};">${account.status}</p>
							<p class="accountcreatedat" style="width: 15%; font-weight: bold; color: #6c757d;">${getDate(account.createdAt, false)}</p>
						</div>`;

		accountHistory.appendChild(accountDiv);
	}
	if (listed == 0) {
		document.getElementById('buttons').style.display = "none";
		const accountDiv = document.createElement("div");
		accountDiv.className = "account-item d-flex row mb-1";
		accountDiv.style = "background-color: #ffffff; height: 650px; padding: 10px; border-bottom: 1px solid #ddd; border-radius: 10px;";
		accountDiv.innerHTML = `
							<img height="500px" style="margin:auto;width: 50%" src="./images/notfound.avif" alt="" />
				          `;
		accountHistory.appendChild(accountDiv);
		return;
	}
}


const accountStatusInput = document.getElementById("accountStatus");
const dropdownStatus = document.getElementById("dropdownStatus");
const options = ["Active", "Inactive", "Suspended"];

function populateDropdown() {
	initialStatus = accountStatusInput.value;
	dropdownStatus.innerHTML = "";
	options.forEach(option => {
		const optionElement = document.createElement("div");
		optionElement.textContent = option;
		optionElement.style = "padding: 5px; cursor: pointer; color: #2c3e50;";
		optionElement.onclick = () => selectOption(option);
		optionElement.onmouseover = () => {
			optionElement.style.backgroundColor = "#f0f0f0";
		};
		optionElement.onmouseout = () => {
			optionElement.style.backgroundColor = "white";
		};
		dropdownStatus.appendChild(optionElement);
	});
}

function toggleDropdown() {
	if (accountStatusInput.disabled) return;
	dropdownStatus.style.display = dropdownStatus.style.display === "none" ? "block" : "none";
}

function selectOption(option) {
	accountStatusInput.value = option;
	dropdownStatus.style.display = "none";
}

function updateStatus() {
	accountStatusInput.disabled = false;
	document.getElementById("statusEdit").style.display = "none";
	document.getElementById("statusSave").style.display = "block";
	toggleDropdown();
}

async function saveStatus() {
	accountStatusInput.disabled = true;
	document.getElementById("statusEdit").style.display = "block";
	document.getElementById("statusSave").style.display = "none";
	dropdownStatus.style.display = "none";
	const accountUpdateData = {
		accountNumber: document.getElementById('accountNumber2').innerText,
		status: accountStatusInput.value
	};
	if (accountStatusInput && accountStatusInput.value !== initialStatus) {
		updateAccount(accountUpdateData);
	}
}

const updateAccount = async accountUpdateData => {
	const response = await fetch('http://localhost:8080/Bank_Application/api/Account', {
		method: 'PUT',
		headers: {
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(accountUpdateData)
	});
	const result = await response.json();
	console.log(result);
	const successPop = document.getElementById('successModal');
	if (result.message == 'success') {
		toggleModal('accountDetailsModal')
		document.getElementById('successMessage').innerHTML = "Account updated successfully!";
		successPop.style.display = 'flex';
	} else {
		const errorUpdate = document.getElementById('errorUpdate');
		errorUpdate.style.display = 'block';
		errorUpdate.innerHTML = result.message;
	}
}

let validUserIds = [];

async function saveAccount() {
	const userIdInput = document.getElementById('newUserId');
	const balanceInput = document.getElementById('newbalance');
	const accountTypeSelect = document.getElementById('newAccountType');
	const message = document.getElementById('accountMessage');
	message.style.display = 'none';

	if (!userIdInput.value.trim()) {
		message.textContent = "User ID is required.";
		message.style.display = 'block';
		userIdInput.focus();
		return;
	}

	if (!validUserIds.includes(Number(userIdInput.value.trim()))) {
		message.textContent = "Invalid User ID. Please select a valid ID.";
		message.style.display = 'block';
		userIdInput.focus();
		return;
	}

	if (!balanceInput.value.trim() || isNaN(balanceInput.value) || parseFloat(balanceInput.value) < 0) {
		message.textContent = "Balance must be a valid positive number.";
		message.style.display = 'block';
		balanceInput.focus();
		return;
	}

	if (!accountTypeSelect.value) {
		message.textContent = "Please select an account type.";
		message.style.display = 'block';
		accountTypeSelect.focus();
		return;
	}

	const accountDetails = {
		userId: userIdInput.value.trim(),
		balance: parseFloat(balanceInput.value),
		accountType: accountTypeSelect.value
	};

	console.log("Account Details Saved:", accountDetails);

	const response = await fetch('http://localhost:8080/Bank_Application/api/Account', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`
		},
		body: JSON.stringify(accountDetails)
	});
	const result = await response.json();
	console.log(result);
	if (result.message == 'success') {
		const successPop = document.getElementById('successModal');
		document.getElementById('successMessage').innerHTML = "Account created successfully!";
		toggleModal('newAccountModal');
		successPop.style.display = 'flex';
		userIdInput.value = '';
		balanceInput.value = '0.0';
		accountTypeSelect.value = '';
	} else {
		const errorMessage = document.getElementById('accountMessage');
		errorMessage.style.display = 'block';
		errorMessage.innerHTML = result.message;
	}
}

function fetchUserIdDetails(query) {
	const userDropdown = document.getElementById('userIddropdown');
	userDropdown.innerHTML = '';
	const accountTypeDropdown = document.getElementById('newAccountType');
	const defaultAccountOptions = `
		<option value="" selected>Select Type</option>
		<option value="Current">Current</option>
		<option value="Savings">Savings</option>
	`;

	// Reset account type dropdown to default
	accountTypeDropdown.innerHTML = defaultAccountOptions;
	accountTypeDropdown.disabled = true;
	userDropdown.style.display = 'block';
	if (!query) {
		userDropdown.innerHTML = '<div class="dropdown-option" style="padding: 10px; color: grey;">Enter a user ID</div>';
		return;
	}
	fetch(`http://localhost:8080/Bank_Application/api/User?userId=${query}&notExact=true`, {
		method: 'GET',
		headers: {
			'Content-Type': 'application/json'
		},
	})
		.then(response => response.json())
		.then(data => {
			console.log(data);
			if (!data || data.length === 0) {
				userDropdown.innerHTML = '<div class="dropdown-option" style="padding: 10px; color: grey;">User ID not found</div>';
				userDropdown.style.display = 'block';
				return;
			}
			validUserIds = [];
			data["users"].forEach((user, index) => {
				if (role == "Employee" && user.role == "Manager") return;
				if (sessionStorage.getItem('email') == user.email) return;
				const option = document.createElement('div');
				option.className = 'dropdown-option py-2 px-3';
				option.style = `
					cursor: pointer;
					${validUserIds.length > 0 ? 'border-top: 1px solid #cdc2c2;' : ''}
				`;
				option.innerHTML = `
					<div style="font-weight: bold; color: #2c3e50;">
						ID: ${user.id}
					</div>
					<div style="font-size: 12px; color: grey;">
						NAME: ${user.fullname} <br>
					</div>
				`;
				validUserIds.push(user.id);
				option.onclick = () => {
					document.getElementById('newUserId').value = user.id;
					userDropdown.style.display = 'none';

					// Update account type options based on role
					if (user.role !== "Customer") {
						accountTypeDropdown.innerHTML = `
							<option value="Operational" selected>Operational</option>
						`;
						accountTypeDropdown.disabled = true;
					} else {
						accountTypeDropdown.innerHTML = `
							<option value="" selected>Select Type</option>
							<option value="Current">Current</option>
							<option value="Savings">Savings</option>
						`;
						accountTypeDropdown.disabled = false;
					}
				};
				userDropdown.appendChild(option);
			});
			if (validUserIds.length == 0) {
				userDropdown.innerHTML = '<div class="dropdown-option" style="padding: 10px; color: grey;">User ID not found</div>';
				userDropdown.style.display = 'block';
			}
		})
		.catch(err => {
			console.error('Error fetching user details:', err);
			userDropdown.innerHTML = '<div class="dropdown-option" style="padding: 10px; color: red;">Error fetching data</div>';
			userDropdown.style.display = 'block';
		});
}

populateDropdown();
let validBranchIds = [];
const dropdown = document.getElementById('dropdown');

function fetchDropdownOptions(query) {
	dropdown.innerHTML = '';
	if (!query) {
		dropdown.innerHTML = '<div class="dropdown-option" style="padding: 10px; color: grey;">Enter a branch ID</div>';
		dropdown.style.display = 'block';
		return;
	}

	fetch(`http://localhost:8080/Bank_Application/api/Branch?branchId=${query}&notExact=true`, {
		method: 'GET',
		headers: {
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`
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
				`;
				option.innerHTML = `
					<div style="font-weight: bold; color: #2c3e50;">
						ID: ${branch.id}
					</div>
					<div style="font-size: 12px; color: grey;">
						NAME: ${branch.name} <br>
						IFSC: ${branch.ifscCode} <br>
						Address: ${branch.address}
					</div>
				`;
				validBranchIds.push(branch.id);
				option.onclick = () => {
					document.getElementById('accountbranchId').value = branch.id;
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

async function saveBranch() {
	const input = document.getElementById('accountbranchId');
	const inputValue = parseInt(input.value);
	if (inputValue && inputValue === initialBranchId) {
		dropdown.innerHTML = '<div class="dropdown-option" style="padding: 10px; color: grey;">No changes made</div>';
		dropdown.style.display = 'block';
		return;
	}
	if (!inputValue || !validBranchIds.includes(inputValue)) {
		console.log("Invalid branch selected");
		dropdown.innerHTML = '<div class="dropdown-option" style="padding: 10px; color: grey;">Enter a valid branch ID</div>';
		dropdown.style.display = 'block';
		console.log(dropdown.style.display)
		return;
	}
	const accountUpdateData = {
		accountNumber: document.getElementById('accountNumber2').innerText,
		branchId: input.value
	};
	updateAccount(accountUpdateData);
}

document.addEventListener('click', (event) => {
	const dropdown = document.getElementById('dropdown');
	const input = document.getElementById('accountbranchId');
	const saveButton = document.getElementById('saveButton');
	if (!dropdown || !input || !saveButton) {
		return;
	}
	if (!dropdown.contains(event.target) &&
		!input.contains(event.target) &&
		event.target !== saveButton) {
		console.log('Clicked outside');
		dropdown.style.display = 'none';
	}
});

