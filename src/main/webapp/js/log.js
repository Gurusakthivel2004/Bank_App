let filterId = '';
let filterAccount = '';
let filterStatus = '';
let filterType = '';
const logsPerPage = 8;
let cachedLogs = [];
let logsCount;
let currentPageIndex = 0;
let filterOffset = 0;
const role = localStorage.getItem("role");
const token = localStorage.getItem('token');

async function fetchLogs() {
	try {
		if (!cachedLogs[currentPageIndex]) {
			const logsData = {
				offset: filterOffset,
				get: true,
				limit: logsPerPage,
			}
			if (filterId && filterId != '-1') logsData.userId = Number(filterId);
			if (filterAccount && Number.isFinite(Number(filterAccount))) logsData.accountNumber = Number(filterAccount);
			if (filterType) logsData.accountType = filterType;
			if (filterStatus) logsData.status = filterStatus;

			console.log(logsData);
			const token = localStorage.getItem('token')
			const response = await fetch('http://localhost:8080/Bank_Application/api/Logs', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					'Authorization': `Bearer ${token}`
				},
				body: JSON.stringify(logsData)
			});

			const logsResult = await response.json();
			const logs = logsResult["logs"];
			console.log(logsResult);

			logsCount = filterOffset == 0 ? logsResult["count"] : logsCount;
			if (response.ok) {
				cachedLogs[currentPageIndex] = logs;
				filterOffset += logsPerPage;
			} else {
				console.error('Error fetching logs:', response.message || 'Unknown error');
				renderlogs([]);
			}
		}
		renderlogs(cachedLogs[currentPageIndex]);
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
	console.log(logsCount, logsPerPage)
	totalPages = Math.ceil(logsCount / logsPerPage) || 1;
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
		fetchLogs();
	}
}

function prevPage() {
	document.getElementById('prevButton').disabled = currentPageIndex == 0;
	if (currentPageIndex > 0) {
		document.getElementById('nextButton').disabled = false;
		currentPageIndex--;
		fetchLogs();
	}
}

let previousId = '', previousAccount = '', branchIdInput = '', previousType = '', previousStatus = '';

function applyFilters() {
	const idInput = document.getElementById("customerIdsearchInput").value.trim();
	const accountInput = document.getElementById("logsearchInput").value.trim();
	const statusInput = document.getElementById("logstatussearchInput").value.trim();
	const typeInput = document.getElementById("accountTypesearchInput").value.trim();
	if (accountInput == '') {
		filterId = idInput ? idInput : -1;
	} else {
		filterId = idInput ? idInput : role != "Customer" ? 0 : -1;
	}
	filterAccount = accountInput;
	filterStatus = statusInput;
	filterType = typeInput;
	console.log(previousId, idInput);

	if (previousAccount != accountInput || previousId != idInput || previousStatus != statusInput || previousType != typeInput) {
		console.log('here1');
		if (!(filterAccount.length > 0 && filterAccount.length < 4) && Number.isFinite(Number(filterAccount))) {
			console.log('here');
			cachedLogs = [];
			currentPageIndex = 0;
			filterOffset = 0;
			fetchLogs();
		}
	}
	if (Number.isFinite(Number(filterAccount))) {
		previousAccount = filterAccount;
	}
	previousAccount = accountInput;
	previousId = idInput;
	previousType = typeInput;
	previousStatus = statusInput;
}

document.addEventListener("DOMContentLoaded", () => {
	console.log(role);
	if (role == null) {
		window.history.back();
	} else if (role === "Customer" || role == "Employee") {
		alert("You do not have permission to access this page.");
		window.history.back();
	}
	fetchLogs();
	document.getElementById("customerIdsearchInput").addEventListener("input", applyFilters);
	document.getElementById("logsearchInput").addEventListener("input", applyFilters);
	document.getElementById("logstatussearchInput").addEventListener("input", applyFilters);
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


function renderlogs(logs) {
	const accountHistory = document.querySelector(".account-data");
	accountHistory.innerHTML = '';
	if (logs == null || logs.length === 0) {
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
	for (let i = 0; i < logs.length; i++) {
		let account = logs[i].instance;
		let branch = logs[i].joinedFields;
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
								style="width: 10%; font-weight: bold; color: #4677bd;">${account.balance.toLocaleString()}</p>
								<p class="branchName" style="width: 5%; font-weight: bold; color: #6c757d;">${branch.name}</p>
								
								<p class="acctype" style="width: 15%; font-weight: bold; color: #2b0444;">${account.accountType}</p>
								<p class="accstatus"
								style="width: 15%; font-weight: bold; color: ${account.status === 'Active' ? '#28a745' : 'red'};">${account.status}</p>
							<p class="accountcreatedat" style="width: 15%; font-weight: bold; color: #6c757d;">${getDate(account.createdAt, false)}</p>
						</div>`;

		accountHistory.appendChild(accountDiv);
	}
}