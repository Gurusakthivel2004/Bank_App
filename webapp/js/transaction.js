let newfilterTo = false;
let filterId = -1;
let filterAccount = '';
let filterFromDate = '';
let filterBranch = '';
let filterToDate = '';
let filterType = '', filterOffset = 0;

const transactionsPerPage = 8, role = getCookie('role');
let cachedTransactions = [], transactionsCount = 0, currentPageIndex = 0;
let lastPage = 1000000;
let isrecursed = false;

async function fetchTransactions() {
	try {
		if (!cachedTransactions[currentPageIndex]) {
			const transactionData = {
				offset: filterOffset,
				get: true,
				limit: 8,
			}
			if (filterId) transactionData.customerId = Number(filterId);
			if (filterBranch) transactionData.branchId = Number(filterBranch);
			if (filterAccount && Number.isFinite(Number(filterAccount))) transactionData.accountNumber = Number(filterAccount);
			if (filterType) transactionData.transactionType = filterType;
			if (filterFromDate) transactionData.from = filterFromDate;
			if (filterToDate) transactionData.to = filterToDate;
			const response = await fetch('/Bank_Application/api/Transaction', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
				},
				body: JSON.stringify(transactionData)
			});
			const transactionsMap = await response.json();
			console.log(transactionsMap)
			if (transactionsMap.message != null && (transactionsMap.message.includes('Session expired') || transactionsMap.message == 'Invalid Access token')) {
				document.querySelector('body').style.display = 'none';
				deleteAllCookies();
				window.location.href = "error.html";
			} else if (transactionsMap.message == 'You dont have a account ') {
				document.querySelector('body').style.display = 'none';
				deleteAllCookies();
				sessionStorage.setItem('error', "No Account exists for the user.");
				window.location.href = "index.html";
			}
			console.log(transactionsMap)
			const transactions = transactionsMap["transactions"];
			transactionsCount = filterOffset == 0 ? transactionsMap["count"] : transactionsCount;
			if (response.ok) {
				cachedTransactions[currentPageIndex] = transactions;
				filterOffset += transactionsPerPage;
			} else {
				console.error('Error fetching transactions:', response.message || 'Unknown error');
			}
		}
		renderTransactions(cachedTransactions[currentPageIndex]);
		document.getElementById("prevButton").disabled = currentPageIndex === 0;
		updatePagination();
		updatePagination();
		document.querySelector('body').style.display = 'block';
	} catch (error) {
		console.error('Error during fetch or processing:', error);
		document.querySelector('body').style.display = 'none';
	}
}

async function download() {
	try {
		const transactionData = {
			get: true,
		}
		if (filterId) transactionData.customerId = Number(filterId);
		if (filterBranch) transactionData.branchId = Number(filterBranch);
		if (filterAccount && Number.isFinite(Number(filterAccount))) transactionData.accountNumber = Number(filterAccount);
		if (filterType) transactionData.transactionType = filterType;
		if (filterFromDate) transactionData.from = filterFromDate;
		if (filterToDate) transactionData.to = filterToDate;

		const response = await fetch('/Bank_Application/api/Transaction', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
			},
			body: JSON.stringify(transactionData)
		});

		if (response.ok) {
			const result = await response.json();
			console.log(result);
			const format = document.getElementById('downloadFormat').value;
			if (format === 'csv') {
				downloadCSV(result.transactions);
			} else if (format === 'pdf') {
				downloadPDF(result.transactions);
			}
		} else {
			console.error('Error fetching transactions for download:', response.statusText);
		}
	} catch (error) {
		console.error('Error during transaction download:', error);
	}
}

function downloadCSV(transactions) {
	const csvRows = [];
	const headers = Object.keys(transactions[0]);
	csvRows.push(headers.join(','));

	for (const transaction of transactions) {
		const values = headers.map(header => transaction[header]);
		csvRows.push(values.join(','));
	}

	const csvContent = csvRows.join('\n');
	const blob = new Blob([csvContent], { type: 'text/csv' });
	const url = URL.createObjectURL(blob);

	const link = document.createElement('a');
	link.href = url;
	link.download = 'transactions.csv';
	link.click();

	URL.revokeObjectURL(url);
}

async function downloadPDF(transactions) {
	const { jsPDF } = window.jspdf;

	const doc = new jsPDF();
	doc.setFontSize(14);
	doc.text('Transaction Details', 10, 10);

	const headers = [['Id', 'Account Number', 'Amount', 'Date', 'Status', 'Type', 'Transaction Account number']];
	const data = transactions.map(tx => [tx.id, tx.accountNumber, tx.amount.toLocaleString(), getDate(tx.transactionTime, true), tx.transactionStatus, tx.transactionType, tx.transactionAccountNumber]);

	doc.autoTable({
		head: headers,
		body: data,
	});

	doc.save('transactions.pdf');
}



let totalPages;

function updatePagination() {
	const pageNumbersContainer = document.getElementById("pageNumbers");
	pageNumbersContainer.innerHTML = '';
	console.log(transactionsCount, transactionsPerPage)
	totalPages = Math.ceil(transactionsCount / transactionsPerPage) || 1;
	const currentPage = currentPageIndex + 1;

	document.getElementById('nextButton').disabled = currentPageIndex == totalPages - 1;
	const paginationText = `Page ${currentPage} of ${totalPages}`;
	pageNumbersContainer.innerHTML = `<span>${paginationText}</span>`;
}

function nextPage() {
	console.log("curr, total: ", currentPageIndex, totalPages)
	document.getElementById('nextButton').disabled = currentPageIndex == totalPages - 2;
	if (currentPageIndex < totalPages - 1) {
		currentPageIndex++;
		fetchTransactions();
	}
}

function prevPage() {
	document.getElementById('prevButton').disabled = currentPageIndex == 0;
	if (currentPageIndex > 0) {
		document.getElementById('nextButton').disabled = false;
		currentPageIndex--;
		fetchTransactions();
	}
}

let previousId = '', previousBranch = '', previousFrom = '', previousAccount = '', previousTo = '', previousType = '';

function applyFilters() {
	const idInput = document.getElementById("IdsearchInput").value.trim();
	const branchInput = document.getElementById("branchIdsearchInput").value.trim();
	const accountInput = document.getElementById("AccountsearchInput").value.trim();
	const fromDateInput = document.getElementById("FromDatesearchInput").value.trim();
	const toDateInput = document.getElementById("ToDatesearchInput").value.trim();
	filterType = document.getElementById("typesearchInput").value.trim();
	if (accountInput == '' && branchInput == '') {
		filterId = idInput ? idInput : -1;
	} else {
		filterId = idInput ? idInput : role != "Customer" ? '' : -1;
	}
	filterAccount = accountInput;
	filterFromDate = fromDateInput;
	filterBranch = branchInput;
	filterFromDate = fromDateInput;
	filterToDate = toDateInput;
	if (previousAccount != accountInput || previousBranch != branchInput || previousId != idInput || previousFrom != fromDateInput || previousTo != toDateInput || previousType != filterType) {
		if (!(filterAccount.length > 0 && filterAccount.length < 4) && Number.isFinite(Number(filterAccount))) {
			cachedTransactions = [];
			currentPageIndex = 0;
			filterOffset = 0;
			fetchTransactions();
		}
	}
	if (Number.isFinite(Number(filterAccount))) {
		previousAccount = filterAccount;
	}
	previousId = idInput;
	previousBranch = filterBranch;
	previousFrom = filterFromDate;
	previousTo = filterToDate
	previousType = filterType;
}

document.addEventListener("DOMContentLoaded", () => {
	if (role == null) {
		document.querySelector('body').style.display = 'none';
		window.location.href = "error.html";
	}
	const transactionHistory = document.querySelector(".transaction-data");
	if (!transactionHistory) {
		console.error('Transaction history element not found!');
		return;
	}
	fetchTransactions();
	if (role === 'Customer') {
		document.getElementById('accounts-tree').style.display = 'none';
		document.getElementById('users-tree').style.display = 'none';
		document.getElementById("idfilter").style.display = 'none';
	} else if (role == 'Manager') {
		document.getElementById('branchidfilter').style.display = 'flex';
	}
	document.getElementById("IdsearchInput").addEventListener("input", applyFilters);
	document.getElementById("branchIdsearchInput").addEventListener("input", applyFilters);
	document.getElementById("AccountsearchInput").addEventListener("input", applyFilters);
	document.getElementById("FromDatesearchInput").addEventListener("input", applyFilters);
	document.getElementById("ToDatesearchInput").addEventListener("input", applyFilters);
	document.getElementById("typesearchInput").addEventListener("input", applyFilters);
});

const check = _ => {
	const idInput = document.getElementById("IdsearchInput").value.trim();
	const accountInput = document.getElementById("AccountsearchInput").value.trim();
	const fromDateInput = document.getElementById("FromDatesearchInput").value.trim();
	const toDateInput = document.getElementById("ToDatesearchInput").value.trim();
	return idInput.length == 0 && accountInput.length == 0 && fromDateInput.length == 0 && toDateInput.length == 0
}

function renderTransactions(transactions) {
	const transactionHistory = document.querySelector(".transaction-data");
	transactionHistory.innerHTML = '';
	console.log(transactions);
	if (transactions == null || transactions.length == 0) {
		const transactionDiv = document.createElement("div");
		transactionDiv.className = "transaction-item d-flex align-items-center mb-1";
		transactionDiv.style = "background-color: #ffffff; height: 640px; padding: 10px; border-bottom: 1px solid #ddd; border-radius: 10px;";
		transactionDiv.innerHTML = `
									<img height="500px" style="margin:auto;width: 50%" src="./images/notfound.avif" alt="" />
						          `;
		transactionHistory.appendChild(transactionDiv);
		return;
	}
	transactions.forEach(tx => {
		const transactionDiv = document.createElement("div");
		transactionDiv.className = "transaction-item d-flex align-items-center my-2";
		transactionDiv.style = "background-color: #ffffff; padding: 10px; border-bottom: 1px solid #ddd; border-radius: 10px;";

		transactionDiv.innerHTML = `
            <p class="tid" style="width: 10%; font-weight: bold; color: #2b0444;">${tx.id}</p>
            <p class="tfrom" style="width: 20%; color: #2b0444;">${tx.accountNumber}</p>
            <p class="tamount" style="width: 15%;margin-left: 20px; font-weight: bold; color: #4677bd;">₹ ${tx.amount.toLocaleString()}</p>
            <p class="ttimestamp" style="width: 20%; font-weight: bold; color: #6c757d;">${getDate(tx.transactionTime, true)}</p>
            <p class="tstatus" style="width: 15%; margin-left: 0px; font-weight: bold; color: #2b0444;">${tx.transactionStatus}</p>
            <p class="ttype" style="width: 10%; margin-left: 0px; color: ${tx.transactionType == 'Debit' ? 'red' : 'green'};">${tx.transactionType}</p>
            <p class="tfrom" style="width: 20%; margin-left: 40px; color: #2b0444;">${tx.transactionAccountNumber}</p>
        `;
		transactionHistory.appendChild(transactionDiv);
	});
}