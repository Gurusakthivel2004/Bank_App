let filterCustomer = '';
let filterAccount = '';
let filterBranch = '';
let lastAccount = '';
const accountsPerPage = 8;
let cachedAccounts = [];
let currentPageIndex = 0;
let lastPage = 1000000;
let isrecursed = false;
const role = localStorage.getItem("role");

async function fetchAccounts() {
	try {
		if (!cachedAccounts[currentPageIndex]) {
			let url = 'http://localhost:8080/Bank_Application/api/Account?';
			if (filterCustomer) url += `customerId=${filterCustomer}`;
			if (filterAccount) url += `&accountNumber=${filterAccount}`;
			if (filterBranch) url += `&branchId=${filterBranch}`;
			if (lastAccount) url += `&lastAccount=${lastAccount}`;
			const token = localStorage.getItem("token");
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
		console.log(error);
		if (error.message.includes("session expired")) {
			localStorage.clear();
			window.location.href = 'index.html';
		}
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
	if(role == "Manager") {
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

// Initial load
document.addEventListener("DOMContentLoaded", () => {
	const role = localStorage.getItem("role");
	if (role == null) {
		window.history.back();
	}
	if (role === "Customer") {
		alert("You do not have permission to access this page.");
		window.history.back();
	} else if(role == "Employee") {
		document.getElementById('branchidfilter').style.display = 'none';
		filterBranch = '-1';
	}
	fetchAccounts();
	document.getElementById("customerIdsearchInput").addEventListener("input", applyFilters);
	document.getElementById("branchIdsearchInput").addEventListener("input", applyFilters);
	document.getElementById("accountsearchInput").addEventListener("input", applyFilters);
});

function renderAccounts(accounts) {
	const accountHistory = document.querySelector(".account-data");
	accountHistory.innerHTML = '';
	console.log(accounts.length)
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
		const accountDiv = document.createElement("div");
		accountDiv.className = "account-item d-flex align-items-center my-2";
		accountDiv.style = "background-color: white; padding: 10px; border-bottom: 1px solid #ddd; border-radius: 10px;";
		accountDiv.innerHTML = `
									<p class="userId"
										style="width: 10%; font-weight: bold; color: #2b0444;">${account.userId}</p>
									<p class="branchId"
										style="width: 10%; font-weight: bold; color: #2b0444;">${account.branchId}</p>
									<p class="accountNumber" style="width: 20%; color: #2b0444;">${account.accountNumber}</p>
									<p class="accbalance"
										style="width: 15%; font-weight: bold; color: #28a745;">${account.balance.toLocaleString()}</p>
									<p class="accstatus"
										style="width: 15%; font-weight: bold; color: blue;">${account.status}</p>
									<p class="acctype" style="width: 10%; color: #2b0444;">${account.accountType}</p>
								</div>`;

		accountHistory.appendChild(accountDiv);
	});
}
