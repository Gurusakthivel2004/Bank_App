const role = getCookie('role');
console.log("Role: ", role);

document.addEventListener("DOMContentLoaded", async _ => {

	if (role == null) {
		document.querySelector('body').style.display = 'none';
		window.location.href = "error.html";
	}

	if (role != null && role !== 'Customer') {
		window.location.href = "emp-dashboard.html";
	}
	try {
		const response = await fetch('/Bank_Application/api/UserDashboard', {
			method: 'GET',
			headers: {
				'Content-Type': 'application/json',
			},
		});
		const result = await response.json();
		console.log(result);
		console.log(result.message);
		console.log(result.message == 'Invalid token');
		if (result.message != null && (result.message.includes('Session expired') || result.message == 'Invalid Access token') ){
			document.querySelector('body').style.display = 'none';
			deleteAllCookies();
			window.location.href = "error.html";
		} else if (result.message == 'You dont have a account ') {
			document.querySelector('body').style.display = 'none';
			deleteAllCookies();
			sessionStorage.setItem('error', "No Account exists for the user.");
			deleteAllCookies();
			window.location.href = "index.html";
		}

		const userDetails = result.userDetail[0];
		try {
			if (result.account.length == 0) {
				window.location.href = "index.html"
				sessionStorage.setItem('error', "Account doesn't exists.");
				document.querySelector('body').style.display = 'none';
			}

			setValues(userDetails);
			let modifiedAt = userDetails['modifiedAt'];
			if (modifiedAt == null) {
				modifiedAt = userDetails['createdAt'];
			}
			document.getElementById('lastUpdated').innerHTML = "Last Updated: " + getDate(modifiedAt, false);
			// setting accounts dropdown
			const accountsDropdown = document.querySelector('.accountsSelect');
			const accountsDropdown2 = document.querySelector('#fdAccount');
			let overAllBalance = 0, currentAccountIndex = 0, account = result.account[currentAccountIndex];

			setValues(result.account[currentAccountIndex]);
			result.account.forEach((account, index) => {
				overAllBalance += account.balance;
				const option = document.createElement('option');
				option.value = index;
				option.textContent = 'Account : ' + account.accountNumber;

				const option2 = document.createElement('option');
				option2.value = account.accountNumber;
				option2.textContent = account.accountNumber;
				
				accountsDropdown.appendChild(option);
				accountsDropdown2.appendChild(option2);
			});
			// setting overall Balance.
			document.getElementById('overAllBalance').innerHTML = `₹ ${overAllBalance.toLocaleString()}`;
			// dropdown change event listener
			accountsDropdown.addEventListener("change", () => {
				const selectedValue = accountsDropdown.value;
				currentAccountIndex = selectedValue;
				account = result.account[currentAccountIndex]
				setValues(result.account[currentAccountIndex]);
				getTransactionsByAccountNumber(result, account.accountNumber)
				setBranchDetails(result.branch, account.branchId);
			});
			setBranchDetails(result.branch, account.branchId);
			getTransactionsByAccountNumber(result, account.accountNumber);
			setFinanceDetails(result);
			document.querySelector('body').style.display = 'block';
		} catch (dropdownError) {
			console.error('Error populating accounts dropdown:', dropdownError);
		}

	} catch (error) {
		console.error('Error during fetch or processing:', error);
		document.querySelector('body').style.display = 'none';
	}
});

const setValues = object => {
	const keys = Object.keys(object);
	for (let i = 0; i < keys.length; i++) {
		const key = keys[i];
		const htmlclass = document.getElementsByClassName(key);
		for (let j = 0; j < htmlclass.length; j++) {
			if (key === 'balance') {
				htmlclass[j].innerHTML = object[key].toLocaleString()
			} else {
				htmlclass[j].innerHTML = object[key];
			}
		}
	}
};

const setBranchDetails = (branches, branchId) => {
	for (let k = 0; k < branches.length; k++) {
		if (branches[k].id == branchId) {
			const keys = Object.keys(branches[k]);
			for (let i = 0; i < keys.length; i++) {
				const key = keys[i];
				const branchField = document.getElementById('branch' + key);
				if (branchField != null) {
					branchField.innerHTML = branches[k][key];
				}
			}
			break;
		}
	}
}

const setFinanceDetails = result => {
	const months = document.getElementsByClassName('month');
	setAmount(0, months, result);
	for (let i = 0; i < months.length; i++) {
		months[i].addEventListener('click', () => {
			setAmount(i, months, result);
		})
	}
}

const setAmount = (i, months, result) => {
	const month = parseInt(months[i].getAttribute('data-value')) + 1;
	const keys = Object.keys(result[month + '']);
	setDefault();
	for (let j = 0; j < keys.length; j++) {
		const htmlclass = document.getElementById(keys[j]);
		if (htmlclass == null) return;
		htmlclass.textContent = '₹ ' + result[month + ''][keys[j]];
		const credit = parseInt(result[month + '']['Credit']);
		const debit = parseInt(result[month + '']['Debit']);
		let net = (credit > 0 ? credit : 0) - (debit > 0 ? debit : 0), sign = '+ ';
		if (net < 0) {
			sign = ' - ';
		}
		document.getElementById('NetBalance').textContent = sign + ' ₹ ' + Math.abs(net);
	}
}

const setDefault = _ => {
	document.getElementById('Debit').textContent = ' ₹ ' + '0.00';
	document.getElementById('Credit').textContent = ' ₹ ' + '0.00';
	document.getElementById('Deposit').textContent = '₹ ' + '0.00';
	document.getElementById('NetBalance').textContent = ' ₹ ' + '0.00';
}

function getTransactionsByAccountNumber(data, accountNumber) {
	const transactions = data.transactions.filter(transaction => transaction.accountNumber === accountNumber);
	transactions.length = 5;
	addTransactionItems(transactions);
}


// Example usage
const milliseconds = Date.now(); // Current time in milliseconds
const menuButton = document.getElementById("menuDropdown");
const dropdownMenu = document.getElementById("month-dropdown");

const monthNames = ["January", "February", "March", "April", "May",
	"June", "July", "August", "September", "October", "November",
	"December"];

const currentDate = new Date();
const currentMonthIndex = currentDate.getMonth();
menuButton.innerHTML = monthNames[currentMonthIndex] + " &#11167;";

for (let i = 0; i < 3; i++) {
	let monthIndex = (currentMonthIndex - i + 12) % 12;
	const listItem = document.createElement("li");
	const listItemLink = document.createElement("a");
	listItemLink.classList.add("dropdown-item");
	listItemLink.classList.add("month");
	listItemLink.textContent = monthNames[monthIndex];
	listItemLink.setAttribute("data-value", monthIndex);

	listItemLink.addEventListener("click", function() {
		menuButton.innerHTML = this.textContent
			+ " &#11167;";
		console.log('clicked here');
	});
	listItem.appendChild(listItemLink);
	dropdownMenu.appendChild(listItem);
}

function toggleActive(element) {
	const currentActive = document
		.querySelector('.account-item.active');
	if (currentActive) {
		currentActive.classList.remove('active');
		currentActive.style.backgroundColor = 'white';
	}
	element.classList.toggle('active');
	element.style.backgroundColor = element.classList
		.contains('active') ? '#d1e7dd' : 'white';
}

function toggleItem(element) {
	const subitems = element.nextElementSibling;
	while (subitems && subitems.classList.contains('tree-subitem')) {
		subitems.style.display = subitems.style.display === 'block' ? 'none'
			: 'block';
		subitems = subitems.nextElementSibling;
	}
}

function addTransactionItems(transactions) {
	const transactionHistory = document.querySelector('.transaction-details');
	transactionHistory.innerHTML = '';
	if (transactions.length === 0) {
		const message = document.createElement('p');
		message.innerHtml = ''
	}
	transactions.forEach(tx => {
		const transactionItem = document.createElement('div');
		transactionItem.className = 'transaction-item d-flex align-items-center';
		transactionItem.style.backgroundColor = '#f9f9f9';
		transactionItem.style.padding = '10px';
		transactionItem.style.borderRadius = '8px';
		transactionItem.style.borderBottom = '1px solid #ddd;';
		transactionItem.style.fontFamily = 'Roboto';
		transactionItem.innerHTML = `
			<p class="tid"
				style="width: 10%; font-weight: 500;  color: #2b0444;">${tx.id}</p>
			<p class="tfrom" style="width: 20%;font-weight: bold;  color: #2b0444;">${tx.transactionAccountNumber}</p>
			<p class="tamount"
				style="width: 15%; font-weight: bold; color: #4677bd;">₹ ${tx.amount.toLocaleString()}</p>
			<p class="ttimestamp"
				style="width: 20%; font-weight: bold; color: #6c757d;">${getDate(tx.transactionTime, true)}</p>
			<p class="tstatus"
				style="width: 15%; color: #2b0444; font-weight: 500; ">${tx.transactionStatus}</p>
			<p class="ttype" style="width: 10%; font-weight: bold; color: ${tx.transactionType == 'Debit' ? 'red' : 'green'};">${tx.transactionType}</p>
			<p class="tremarks" style="width: 20%; color: #2b0444;">${tx.remarks}</p>			
        `;

		transactionHistory.appendChild(transactionItem);
	});
}
