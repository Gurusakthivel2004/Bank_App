let filterId = '';
let filterlog = '';
let filterStatus = '';
let filterType = '';
let filterToDate = '';
let filterFromDate = '';
const logsPerPage = 8;
let cachedLogs = [];
let logsCount;
let currentPageIndex = 0;
let filterOffset = 0;
const role = getCookie("role");
const token = getCookie('token');

async function fetchLogs() {
	try {
		if (!cachedLogs[currentPageIndex]) {
			const logsData = {
				offset: filterOffset,
				get: true,
				limit: logsPerPage,
			}
			if (filterId) logsData.userId = Number(filterId);
			if (filterlog && Number.isFinite(Number(filterlog))) logsData.accountNumber = Number(filterlog);
			if (filterType) logsData.logType = filterType;
			if (filterFromDate) logsData.from = filterFromDate;
			if (filterToDate) logsData.to = filterToDate;

			console.log(logsData);
			const token = getCookie('token')
			const response = await fetch('http://localhost:8080/Bank_Application/api/Log', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					'Authorization': `Bearer ${token}`
				},
				body: JSON.stringify(logsData)
			});
			const logsResult = await response.json();
			if (logsResult.message == 'Invalid token' || logsResult.message == 'Invalid Access token') {
				document.querySelector('body').style.display = 'none';
				deleteAllCookies();
				window.location.href = "error.html";
			} else if (logsResult.message == 'You dont have a account ') {
				document.querySelector('body').style.display = 'none';
				deleteAllCookies();
				sessionStorage.setItem('error', "No Account exists for the user.");
				window.location.href = "index.html";
			}

			const logs = logsResult["logs"];
			console.log(logsResult);

			logsCount = filterOffset == 0 ? logsResult["count"] : logsCount;
			if (response.ok) {
				cachedLogs[currentPageIndex] = logs;
				filterOffset += logsPerPage;
			} else {
				console.error('Error fetching logs:', response.message || 'Unknown error');
				document.querySelector('body').style.display = 'none';
			}
		}
		renderlogs(cachedLogs[currentPageIndex]);
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

let previousId = '', previouslog = '', branchIdInput = '', previousType = '', previousStatus = '', previousFrom = '', previousTo = '';

function applyFilters() {
	const idInput = document.getElementById("customerIdsearchInput").value.trim();
	const logInput = document.getElementById("accountsearchInput").value.trim();
	const typeInput = document.getElementById("logTypesearchInput").value.trim();
	const fromDateInput = document.getElementById("FromDatesearchInput").value.trim();
	const toDateInput = document.getElementById("ToDatesearchInput").value.trim();

	filterId = idInput;
	filterlog = logInput;
	filterType = typeInput;
	filterFromDate = fromDateInput;
	filterToDate = toDateInput;
	console.log(previousId, idInput);

	if (previouslog != logInput || previousId != idInput || previousType != typeInput || previousFrom != fromDateInput || previousTo != toDateInput) {
		if (!(filterlog.length > 0 && filterlog.length < 4) && Number.isFinite(Number(filterlog))) {
			cachedLogs = [];
			currentPageIndex = 0;
			filterOffset = 0;
			fetchLogs();
		}
	}
	if (Number.isFinite(Number(filterlog))) {
		previouslog = filterlog;
	}
	previouslog = logInput;
	previousId = idInput;
	previousType = typeInput;
	previousFrom = filterFromDate;
	previousTo = filterToDate
}

document.addEventListener("DOMContentLoaded", () => {
	console.log(role);
	if (role == null || role === "Customer" || role == "Employee") {
		document.querySelector('body').style.display = 'none';
		window.location.href = "error.html";
	}
	fetchLogs();
	document.getElementById("customerIdsearchInput").addEventListener("input", applyFilters);
	document.getElementById("accountsearchInput").addEventListener("input", applyFilters);
	document.getElementById("logTypesearchInput").addEventListener("input", applyFilters);
	document.getElementById("FromDatesearchInput").addEventListener("input", applyFilters);
	document.getElementById("ToDatesearchInput").addEventListener("input", applyFilters);
});

function renderlogs(logs) {
	const logHistory = document.querySelector(".log-data");
	logHistory.innerHTML = '';
	if (logs == null || logs.length === 0) {
		document.getElementById('buttons').style.display = "none";
		const logDiv = document.createElement("div");
		logDiv.className = "account-item d-flex row mb-1";
		logDiv.style = "background-color: #ffffff; height: 650px; padding: 10px; border-bottom: 1px solid #ddd; border-radius: 10px;";
		logDiv.innerHTML = `
						<img height="500px" style="margin:auto;width: 50%" src="./images/notfound.avif" alt="" />
			          `;
		logHistory.appendChild(logDiv);
		return;
	}
	for (let i = 0; i < logs.length; i++) {
		let log = logs[i];
		document.getElementById('buttons').style.display = "flex";
		const logDiv = document.createElement("div");
		logDiv.className = "account-item d-flex align-items-center my-2";
		logDiv.style = "background-color: white; padding: 10px; border-bottom: 1px solid #ddd; border-radius: 10px;";

		// Determine log type color
		let logTypeColor = '';
		switch (log.logType) {
			case 'Insert':
				logTypeColor = '#28a745'; // Green
				break;
			case 'Update':
				logTypeColor = '#4677bd'; // Blue 
				break;
			case 'Delete':
				logTypeColor = '#dc3545'; // Red
				break;
			case 'Login':
				logTypeColor = '#28a745'; // Green
				break;
			case 'Logout':
				logTypeColor = '#dc3545'; // Red
				break;
			default:
				logTypeColor = '#6c757d'; // Default Gray
		}

		logDiv.innerHTML = `
	        <p class="userId" style="font-weight: bold; color: #2b0444;">${log.userId ?? "-"}</p>
	        <p class="tableName" style="font-weight: bold; color: #2b0444;">${log.tableName ?? "-"}</p>
	        <p class="rowId" style="font-weight: bold; color: #2b0444;">${log.rowId ?? "-"}</p>
	        <p class="logtype" style="font-weight: bold; color: ${logTypeColor};">${log.logType ?? "-"}</p>
	        <p class="accountNumber" style="color: #2b0444; padding-left: 20px;">${log.userAccountNumber ?? "-"}</p>
			<p class="logBy" style="margin-left: 10px; font-weight: bold; color: #6c757d;">${log.performedBy ?? "-"}</p>
			<div class="logMessage-wrapper" style="width: 24%; position: relative;">
	            <p class="logMessage" 
	               style="font-weight: bold; color: #2b0444; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; cursor: pointer;">
	               ${log.logMessage ?? "-"}
	            </p>
	            <div class="tooltip" style="visibility: hidden; opacity: 0; color: #fff; position: absolute; top: 100%; left: 0; background-color: #343a40; border-radius: 5px; padding: 10px; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); z-index: 10; white-space: normal; max-width: 300px; transition: opacity 0.3s, visibility 0.3s;">
	               ${log.logMessage ?? "-"}
	            </div>
	        </div>
			 <p class="logcreatedat" style="width: 15%;margin-left: 50px; font-weight: bold; color: #6c757d;">${getDate(log.timestamp, false) ?? "-"}</p>`;

		const logMessage = logDiv.querySelector('.logMessage');
		const tooltip = logDiv.querySelector('.tooltip');

		// Tooltip hover effect
		logMessage.addEventListener('mouseenter', () => {
			tooltip.style.visibility = 'visible';
			tooltip.style.opacity = '1';
		});

		logMessage.addEventListener('mouseleave', () => {
			tooltip.style.visibility = 'hidden';
			tooltip.style.opacity = '0';
		});

		logHistory.appendChild(logDiv);
	}

}