let filterCustomer = '';
let filterAccount = '';
let filterBranch = '';
let filterStatus = '';
let filterType = '';
let lastAccount = '';
const accountsPerPage = 8;
let cachedAccounts = [];
let initialBranchId;
let initialStatus;
let currentPageIndex = 0;
let lastPage = 1000000;
let isrecursed = false;
const role = localStorage.getItem("role");
const token = localStorage.getItem('token');

async function fetchAccounts() {
	try {
		if (!cachedAccounts[currentPageIndex]) {
			let url = 'http://localhost:8080/Bank_Application/api/Account?';
			if (filterCustomer) url += `customerId=${filterCustomer}`;
			if (filterAccount) url += `&accountNumber=${filterAccount}`;
			if (filterBranch) url += `&branchId=${filterBranch}`;
			if (lastAccount) url += `&lastAccount=${lastAccount}`;
			console.log(url);
			const response = await fetch(url, {
				method: 'GET',
				headers: {
					'Content-Type': 'application/json',
					'Authorization': `Bearer ${token}`
				},
			});
			const accounts = await response.json();
			console.log(accounts);
			if (accounts.length == 0 && filterBranch.length == 0) {
				if (!isrecursed) {
					isrecursed = true;
					goToPage(currentPageIndex - 1);
					return;
				}
			}
			lastPage = accounts.length < accountsPerPage ? currentPageIndex : 1000000;
			if (response.ok) {
				cachedAccounts[currentPageIndex] = accounts;
				if (accounts.length > 0) {
					lastAccount = accounts[accounts.length - 1].createdAt;
				}
			}
		}
		if (currentPageIndex == lastPage && lastPage != 1000000 || cachedAccounts[currentPageIndex].length == 0) {
			document.getElementById("nextButton").disabled = true;
		} else {
			document.getElementById("nextButton").disabled = false;
		}
		renderAccounts(cachedAccounts[currentPageIndex]);
		document.getElementById("prevButton").disabled = currentPageIndex === 0;
		updatePagination();
	} catch (error) {
		localStorage.clear();
		window.location.href = 'index.html';
	}
}

function updatePagination() {
	const pageNumbersContainer = document.getElementById("pageNumbers");
	pageNumbersContainer.innerHTML = '';

	const totalPages = cachedAccounts.length;
	const maxVisiblePages = 3;

	let startPage = Math.max(0, currentPageIndex - Math.floor(maxVisiblePages / 2));
	let endPage = Math.min(totalPages, startPage + maxVisiblePages);
	if (endPage === totalPages && totalPages > maxVisiblePages) {
		startPage = Math.max(0, totalPages - maxVisiblePages);
	}

	for (let i = startPage; i < endPage; i++) {
		const pageButton = document.createElement("button");
		pageButton.innerText = (i + 1).toString();
		pageButton.style = i === currentPageIndex
			? "background-color: #4677bd; color: white; font-weight: bold;border: 0"
			: "background-color: #2b0444; color: white; font-weight: 500;border: 0";
		pageButton.className = "py-2 px-3 rounded";
		pageButton.onclick = () => goToPage(i);
		pageNumbersContainer.appendChild(pageButton);
	}
}

function goToPage(pageIndex) {
	currentPageIndex = pageIndex;
	fetchAccounts();
}

function nextPage() {
	console.log(currentPageIndex, lastPage)
	if (currentPageIndex <= lastPage) {
		currentPageIndex++;
		fetchAccounts();
	}
}

function prevPage() {
	if (currentPageIndex > 0) {
		currentPageIndex--;
		fetchAccounts();
	}
}

let previousBranch, previousId, previousAccount, branchIdInput;

function applyFilters() {
	const customerIdInput = document.getElementById("customerIdsearchInput").value.trim();
	const accountInput = document.getElementById("accountsearchInput").value.trim();
	const statusInput = document.getElementById("accountStatussearchInput").value.trim();
	const typeInput = document.getElementById("accountTypesearchInput").value.trim();
	if (role == "Manager") {
		branchIdInput = document.getElementById("branchIdsearchInput").value.trim();
		filterBranch = branchIdInput;
	}
	lastAccount = ''
	filterAccount = accountInput;
	filterStatus = statusInput;
	filterType = typeInput;
	filterCustomer = customerIdInput;
	console.log(filterAccount.length);

	if (filterAccount.length == 0 && filterBranch.length == 0) {
		filterCustomer = customerIdInput ? customerIdInput : '';
		if (previousAccount != accountInput || previousBranch != branchIdInput || previousId != customerIdInput) {
			cachedAccounts = [];
			currentPageIndex = 0;
			lastPage = 1000000;
			isrecursed = false;
			fetchAccounts();
		} else {
			console.log('here');
			renderAccounts(cachedAccounts[currentPageIndex]);
		}
	} else if(filterAccount.length >= 4 || filterBranch.length > 0) {
		cachedAccounts = [];
		currentPageIndex = 0;
		lastPage = 1000000;
		isrecursed = false;
		fetchAccounts();
	}
	previousAccount = accountInput;
	previousBranch = branchIdInput;
	previousId = customerIdInput;
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
	const userId = account.userId;
	const userDetailsResponse = await fetch(`http://localhost:8080/Bank_Application/api/User?userId=${userId}&role=Customer`, {
		method: 'GET',
		headers: {
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`
		},
	});
	const userResult = await userDetailsResponse.json();
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

	const filteredAccounts = accounts.filter(account => {
		const matchesStatus = filterStatus ? account.status === filterStatus : true;
		const matchesType = filterType ? account.accountType === filterType : true;
		return matchesStatus && matchesType;
	});

	if (filteredAccounts.length === 0) {

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

	filteredAccounts.forEach(account => {
		if (account.accountType !== "Operational") {
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
		console.log('entered');
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

