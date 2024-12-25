let filterCustomer = '';
let filterAccount = '';
let filterBranch = '';
let lastAccount = '';
const accountsPerPage = 8;
let cachedAccounts = [];
let initialBranchId;
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
			? "background-color: #4677bd; color: white; font-weight: bold; margin: 0 5px;"
			: "background-color: #2b0444; color: white; font-weight: 500; margin: 0 5px;";
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

function applyFilters() {
	const customerIdInput = document.getElementById("customerIdsearchInput").value.trim();
	const accountInput = document.getElementById("accountsearchInput").value.trim();
	if (role == "Manager") {
		const branchIdInput = document.getElementById("branchIdsearchInput").value.trim();
		filterBranch = branchIdInput;
	}
	lastAccount = ''
	filterAccount = accountInput;
	cachedAccounts = [];
	currentPageIndex = 0;
	filterCustomer = customerIdInput;
	if (filterAccount.length == 0 && filterBranch.length == 0) {
		filterCustomer = customerIdInput ? customerIdInput : '-1';
		cachedAccounts = [];
		currentPageIndex = 0;
		lastPage = 1000000;
		isrecursed = false;
	}
	fetchAccounts();
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
	document.getElementById('accountUserStatus').innerText = userResult.status;
	toggleModal('accountDetailsModal');
}

function renderAccounts(accounts) {
	const accountHistory = document.querySelector(".account-data");
	accountHistory.innerHTML = '';
	if (accounts.length == 0) {
		const accountDiv = document.createElement("div");
		accountDiv.className = "account-item d-flex align-items-center mb-1";
		accountDiv.style = "background-color: #ffffff; padding: 10px; border-bottom: 1px solid #ddd; border-radius: 10px;";
		accountDiv.innerHTML = `
			           <p class="text-center py-5" style="width: 100%;font-family:'Roboto'; font-size: 23px; font-weight: bold; color: #007BFF; margin-left: 40%;">No accounts found!</p>
			        `;
		accountHistory.appendChild(accountDiv);
		return;
	}
	accounts.forEach(account => {
		if (account.accountType !== "Operational") {
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
					style="width: 15%; font-weight: bold; color: ${account.accountType === 'Active' ? 'blue' : 'red'};">${account.status}</p>
				<p class="accountcreatedat" style="width: 15%; color: #2b0444;">${getDate(account.createdAt, false)}</p>
			</div>`;

			accountHistory.appendChild(accountDiv);
		}
	});
}

let validBranchIds = [];
const dropdown = document.getElementById('dropdown');

function fetchDropdownOptions(query) {
	dropdown.innerHTML = '';
	if (!query) {
		dropdown.innerHTML = '<div class="dropdown-option" style="padding: 10px; color: grey;">Enter a branch ID</div>';
		dropdown.style.display = 'block';
		return;
	}

	fetch(`http://localhost:8080/Bank_Application/api/Branch?branchId=${query}`, {
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
	console.log(validBranchIds)
	console.log(inputValue)
	console.log(dropdown.style.display)
	console.log(validBranchIds.includes(inputValue))
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
	console.log(accountUpdateData);

	const response = await fetch('http://localhost:8080/Bank_Application/api/Account', {
		method: 'PUT',
		headers: {
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`
		},
		body: JSON.stringify(accountUpdateData)
	});
	const result = await response.json();
	console.log(result);
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

