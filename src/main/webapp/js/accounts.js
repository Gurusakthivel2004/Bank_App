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
const role = localStorage.getItem("role");
const token = localStorage.getItem('token');

async function fetchAccounts() {
	try {
		if (!cachedAccounts[currentPageIndex]) {
			if (role == "Employee") {
				filterBranch = localStorage.getItem('branchId');
			}
			console.log(filterId);
			let url = `http://localhost:8080/Bank_Application/api/Account?offset=${filterOffset}&limit=${accountsPerPage}`;
			if (filterId) url += `&userId=${filterId}`;
			if (filterAccount) url += `&accountNumber=${filterAccount}`;
			if (filterBranch) url += `&branchId=${filterBranch}`;
			if (filterType) url += `&accountType=${filterType}`;
			if (filterStatus) url += `&status=${filterStatus}`;
			console.log(url);
			const response = await fetch(url, {
				method: 'GET',
				headers: {
					'Content-Type': 'application/json',
					'Authorization': `Bearer ${token}`
				},
			});
			console.log(response);
			const accountsResult = await response.json();
			const accounts = accountsResult["accounts"];
			console.log(accountsResult);

			accountsCount = filterOffset == 0 ? accountsResult["count"] : accountsCount;
			if (response.ok) {
				cachedAccounts[currentPageIndex] = accounts;
				filterOffset += accountsPerPage;
			} else {
				console.error('Error fetching accounts:', response.message || 'Unknown error');
				renderAccounts([]);
			}
		}
		renderAccounts(cachedAccounts[currentPageIndex]);
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
	if (role == null) {
		window.history.back();
	} else if (role === "Customer") {
		alert("You do not have permission to access this page.");
		window.history.back();
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

const updateBranch = async _ => {
	const branchId = document.getElementById('accountbranchId');
	branchId.disabled = false;
	branchId.style.border = "1px solid";
	document.getElementById('branchSave').style.display = "block";
	document.getElementById('branchEdit').style.display = "none";
	initialBranchId = parseInt(document.getElementById('accountbranchId').value);
}

const accountClick = async account => {
	const userDetailsResponse = await fetch(`http://localhost:8080/Bank_Application/api/User?accountNumber=${account.accountNumber}`, {
		method: 'GET',
		headers: {
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`
		},
	});
	const userResult = await userDetailsResponse.json();
	console.log(userResult);
	document.getElementById('accountbranchId').value = account.branchId;
	document.getElementById('accountfullname').innerText = userResult.fullname;
	document.getElementById('accountphone').innerText = userResult.phone;
	document.getElementById('accountNumber2').innerText = account.accountNumber;
	document.getElementById('accountStatus').value = account.status
	toggleModal('accountDetailsModal');
	initialStatus = accountStatusInput.value;
}

function renderAccounts(accounts) {
	const accountHistory = document.querySelector(".account-data");
	accountHistory.innerHTML = '';
	console.log(accounts);
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

	accounts.forEach(account => {
		if (role != "Customer" && (account.accountType != "Operational" && role == "Employee")) {
			document.getElementById('buttons').style.display = "flex";
			const accountDiv = document.createElement("div");
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
					style="width: 10%; font-weight: bold; color: #28a745;">${account.balance.toLocaleString()}</p>
					<p class="branchName" style="width: 5%; font-weight: bold; color: #2b0444;">Madurai</p>

					<p class="acctype" style="width: 15%; color: #2b0444;">${account.accountType}</p>
					<p class="accstatus"
					style="width: 15%; font-weight: bold; color: ${account.status === 'Active' ? 'blue' : 'red'};">${account.status}</p>
				<p class="accountcreatedat" style="width: 15%; color: #2b0444;">${getDate(account.createdAt, false)}</p>
			</div>`;

			accountHistory.appendChild(accountDiv);
		}
	});
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
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`
		},
		body: JSON.stringify(accountUpdateData)
	});
	const result = await response.json();
	const successPop = document.getElementById('successPopup');
	if (result.message == 'success') {
		successPop.textContent = "Account updated!";
		successPop.style.backgroundColor = '#4CAF50';
		successPop.style.color = 'white';
		successPop.style.display = 'block';
	} else {
		successPop.textContent = result.message;
		successPop.style.backgroundColor = 'red';
		successPop.style.color = 'white';
		successPop.style.display = 'block';
	}
	setTimeout(() => {
		successPop.style.display = 'none';
		location.reload();
	}, 3000);
}

let validUserIds = []; // Declare this globally or in the appropriate scope

function saveAccount() {
	const userIdInput = document.getElementById('newUserId');
	const balanceInput = document.getElementById('newbalance');
	const accountTypeSelect = document.getElementById('newAccountType');
	const message = document.getElementById('accountMessage');
	message.style.display = 'none';

	console.log(validUserIds)
	console.log(userIdInput.value.trim());
	console.log(validUserIds.includes(userIdInput.value.trim()));
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

	// Check if the balance is valid
	if (!balanceInput.value.trim() || isNaN(balanceInput.value) || parseFloat(balanceInput.value) < 0) {
		message.textContent = "Balance must be a valid positive number.";
		message.style.display = 'block';
		balanceInput.focus();
		return;
	}

	// Check if account type is selected
	if (!accountTypeSelect.value) {
		message.textContent = "Please select an account type.";
		message.style.display = 'block';
		accountTypeSelect.focus();
		return;
	}

	// Prepare account details
	const accountDetails = {
		userId: userIdInput.value.trim(),
		balance: parseFloat(balanceInput.value),
		accountType: accountTypeSelect.value
	};

	console.log("Account Details Saved:", accountDetails);

	// Show success popup
	const successPopup = document.getElementById('successPopup');
	successPopup.textContent = "Account details saved successfully!";
	successPopup.style.display = 'block';

	setTimeout(() => {
		successPopup.style.display = 'none';
	}, 3000);

	// Reset form fields
	userIdInput.value = '';
	balanceInput.value = '0.0';
	accountTypeSelect.value = '';
	toggleModal('newAccountModal');
}



function fetchUserIdDetails(query) {
	const userDropdown = document.getElementById('dropdown');
	userDropdown.innerHTML = '';
	if (!query) {
		userDropdown.innerHTML = '<div class="dropdown-option" style="padding: 10px; color: grey;">Enter a user ID</div>';
		userDropdown.style.display = 'block';
		return;
	}

	fetch(`http://localhost:8080/Bank_Application/api/User?userId=${query}&notExact=true`, {
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
				userDropdown.innerHTML = '<div class="dropdown-option" style="padding: 10px; color: grey;">User ID not found</div>';
				userDropdown.style.display = 'block';
				return;
			}
			data.forEach((user, index) => {
				const option = document.createElement('div');
				option.className = 'dropdown-option py-2 px-3';
				option.style = `
				       cursor: pointer;
				       ${index === data.length - 1 ? '' : 'border-bottom: 1px solid #cdc2c2;'}
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
				};
				userDropdown.appendChild(option);
			});
			userDropdown.style.display = 'block';
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
	} else {
		console.log('Clicked inside or save button');
	}
});

