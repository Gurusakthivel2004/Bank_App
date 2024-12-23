let newfilterTo = false;
let role = localStorage.getItem("role");
// Filter values
let filterId = -1;
let filterAccount = '';
let filterFromDate = '';
let filterToDate = '';

const transactionsPerPage = 8;
let cachedTransactions = [];
let currentPageIndex = 0;
let lastPage = 1000000;
let isrecursed = false;

async function fetchTransactions() {
	try {
		if (!cachedTransactions[currentPageIndex]) {
			let url = `http://localhost:8080/Bank_Application/api/Transaction?id=${filterId}`;
			if (filterAccount) url += `&accountNumber=${filterAccount}`;
			if (filterFromDate) url += `&from=${filterFromDate}`;
			if (filterToDate) url += `&to=${filterToDate}`;
			const token = localStorage.getItem('token')
			const response = await fetch(url, {
				method: 'GET',
				headers: {
					'Content-Type': 'application/json',
					'Authorization': `Bearer ${token}`
				},
			});

			const transactions = await response.json();
			console.log(transactions)
			newfilterTo = transactions.length > 0;
			console.log("curr" + currentPageIndex);
			isrecursed = transactions.length == 0;
			if (transactions.length == 0 && filterFromDate.length == 0 && !newfilterTo) {
				if (!isrecursed) {
					isrecursed = true;
					document.getElementById("nextButton").disabled = true;
					goToPage(currentPageIndex - 1);
					return;
				}
			}
			lastPage = transactions.length < transactionsPerPage ? currentPageIndex : 1000000;
			if (response.ok) {
				cachedTransactions[currentPageIndex] = transactions;
				if (transactions.length > 0) {
					filterToDate = transactions[transactions.length - 1].transactionTime;
				}
			} else {
				console.error('Error fetching transactions:', transactions.message || 'Unknown error');
			}
		}
		if (currentPageIndex == lastPage && lastPage != 1000000 || cachedTransactions[currentPageIndex].length == 0) {
			document.getElementById("nextButton").disabled = true;
		} else {
			document.getElementById("nextButton").disabled = false;
		}
		renderTransactions(cachedTransactions[currentPageIndex]);
		document.getElementById("prevButton").disabled = currentPageIndex === 0;
		updatePagination();
	} catch (error) {
		console.error('Error during fetch or processing:', error);
	}
}

function updatePagination() {
	const pageNumbersContainer = document.getElementById("pageNumbers");
	pageNumbersContainer.innerHTML = '';

	const totalPages = cachedTransactions.length;
	const maxVisiblePages = 4;

	let startPage = Math.max(0, currentPageIndex - Math.floor(maxVisiblePages / 2));
	let endPage = Math.min(totalPages, startPage + maxVisiblePages);
	if (endPage === totalPages && totalPages > maxVisiblePages) {
		startPage = Math.max(0, totalPages - maxVisiblePages);
	}

	for (let i = startPage; i < endPage; i++) {
		const pageButton = document.createElement("button");
		pageButton.innerText = (i + 1).toString();
		pageButton.style = i === currentPageIndex
			? "background-color: #4677bd; border: 1px solid #4677bd; color: white; font-weight: bold; margin: 0 5px;" // Highlight for current page
			: "background-color: #2b0444; color: white; font-weight: 500; margin: 0 5px;";
		pageButton.className = "py-2 px-3 rounded";
		pageButton.onclick = () => goToPage(i);
		pageNumbersContainer.appendChild(pageButton);
	}
}

function goToPage(pageIndex) {
	currentPageIndex = pageIndex;
	fetchTransactions();
}

function nextPage() {
	if (currentPageIndex <= lastPage) {
		currentPageIndex++;
		fetchTransactions();
	}
}

function prevPage() {
	if (currentPageIndex > 0) {
		currentPageIndex--;
		fetchTransactions();
	}
}

function applyFilters() {
	const idInput = document.getElementById("IdsearchInput").value.trim();
	const accountInput = document.getElementById("AccountsearchInput").value.trim();
	const fromDateInput = document.getElementById("FromDatesearchInput").value.trim();
	const toDateInput = document.getElementById("ToDatesearchInput").value.trim();
	if (accountInput.length == 0) {
		filterId = idInput ? idInput : -1;
	} else {
		filterId = idInput ? idInput : role != "Customer" ? 0 : -1;
	}
	filterAccount = accountInput;
	filterFromDate = fromDateInput;
	filterToDate = toDateInput;
	newfilterTo = filterToDate.length > 0;
	cachedTransactions = [];
	currentPageIndex = 0;
	isrecursed = false;
	if (idInput.length == 0 && filterAccount.length == 0 && filterFromDate.length == 0 && filterToDate.length == 0) {
		newfilterTo = false;
		cachedTransactions = [];
		currentPageIndex = 0;
		lastPage = 1000000;
		filterId = -1;
	}
	fetchTransactions();
}

// Initial load
document.addEventListener("DOMContentLoaded", () => {
	const transactionHistory = document.querySelector(".transaction-data");
	if (!transactionHistory) {
		console.error('Transaction history element not found!');
		return;
	}
	fetchTransactions();
	if (role === 'Customer') {
		document.getElementById('accounts-tree').style.display = 'none';
		document.getElementById("idfilter").style.display = 'none';
	}
	document.getElementById("IdsearchInput").addEventListener("input", applyFilters);
	document.getElementById("AccountsearchInput").addEventListener("input", applyFilters);
	document.getElementById("FromDatesearchInput").addEventListener("input", applyFilters);
	document.getElementById("ToDatesearchInput").addEventListener("input", applyFilters);
});

const check = _ => {
	const idInput = document.getElementById("IdsearchInput").value.trim();
	const accountInput = document.getElementById("AccountsearchInput").value.trim();
	const fromDateInput = document.getElementById("FromDatesearchInput").value.trim();
	const toDateInput = document.getElementById("ToDatesearchInput").value.trim();
	return idInput.length == 0 && accountInput.length == 0 && fromDateInput.length == 0 && toDateInput.length == 0
}

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

function renderTransactions(transactions) {
	const transactionHistory = document.querySelector(".transaction-data");
	transactionHistory.innerHTML = '';
	if (transactions.length == 0) {
		const transactionDiv = document.createElement("div");
		transactionDiv.className = "transaction-item d-flex align-items-center mb-1";
		transactionDiv.style = "background-color: #ffffff; padding: 10px; border-bottom: 1px solid #ddd; border-radius: 10px;";

		transactionDiv.innerHTML = `
		           <a href="payment.html" class="text-center py-5" style="width: 100%; font-size: 23px; color: #555; text-decoration: none; margin-left: 1px;">No results found! Click here to make a transaction.</a>
		        `;

		transactionHistory.appendChild(transactionDiv);
		return;
	}
	transactions.forEach(tx => {
		const transactionDiv = document.createElement("div");
		transactionDiv.className = "transaction-item d-flex align-items-center mb-1";
		transactionDiv.style = "background-color: #ffffff; padding: 10px; border-bottom: 1px solid #ddd; border-radius: 10px;";

		transactionDiv.innerHTML = `
            <p class="tid" style="width: 10%; font-weight: bold; color: #2b0444;">${tx.id}</p>
            <p class="tfrom" style="width: 20%; color: #2b0444;">${tx.accountNumber}</p>
            <p class="tamount" style="width: 15%;margin-left: 20px; font-weight: bold; color: #4677bd;">₹ ${tx.amount.toLocaleString()}</p>
            <p class="ttimestamp" style="width: 20%; font-weight: bold; color: #6c757d;">${getDate(tx.transactionTime, true)}</p>
            <p class="tstatus" style="width: 15%; margin-left: 0px; font-weight: bold; color: #2b0444;">${tx.status}</p>
            <p class="ttype" style="width: 10%; margin-left: 0px; color: ${tx.transactionType == 'Debit' ? 'red' : 'green'};">${tx.transactionType}</p>
            <p class="tfrom" style="width: 20%; margin-left: 40px; color: #2b0444;">${tx.transactionAccountNumber}</p>
        `;

		transactionHistory.appendChild(transactionDiv);
	});
}
