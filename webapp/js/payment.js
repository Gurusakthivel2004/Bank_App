let originalValue = '';
let originalActiveItem = null;
let validAccounts = [];
const role = getCookie('role');

const handleSubmit = async (event) => {
	event.preventDefault();
	const selectElement = document.querySelector('.accountsSelect');
	const branchId = selectElement.options[selectElement.selectedIndex].id;
	const isOtherBankChecked = document.querySelector('#otherBankCheckbox').checked;

	let bankName = "Horizon";
	let transactionIfsc = "";

	if (isOtherBankChecked) {
		bankName = document.querySelector('#bank').value;
		transactionIfsc = document.querySelector('#ifsc').value;
	}

	const transactionAccountNumber = document.getElementById('transAcc').value;
	const amount = document.getElementById('amount').value;
	const remarks = document.querySelector('textarea[placeholder="Enter remarks"]').value;

	let transactionType = 'Debit';
	if (role !== "Customer") {
		transactionType = document.getElementById('type').value;
	}

	let accountNumber = '';
	if (role != 'Customer') {
		accountNumber = document.getElementById('account').value;
		if (transactionType == 'Debit') {
			accountNumber = document.getElementById('account').value;
		} else {
			accountNumber = document.getElementById('customerAccounts').value;
		}
	} else {
		accountNumber = document.getElementById('customerAccounts').value;
	}
	const transactionData = {
		accountNumber: accountNumber,
		transactionAccountNumber: transactionAccountNumber,
		amount: amount,
		branchId: branchId,
		remarks: remarks,
		bankName: bankName,
		transactionIfsc: transactionIfsc,
		transactionType: transactionType
	}
	console.log(transactionData);

	if (accountNumber == transactionAccountNumber) {
		displayInvalidAccount("Cannot make a transaction to same account.");
		return;
	} if (validAccounts.includes(accountNumber) == false && transactionType == 'Debit' && role != 'Customer') {
		console.log('1');
		displayInvalidAccount("Enter valid account number.");
		return;
	} if (validAccounts.includes(transactionAccountNumber) == false && role != "Customer") {
		console.log('1');
		displayInvalidAccount("Enter valid account number.");
		return;
	} if (bankName == "Horizon" && !validAccounts.includes(transactionAccountNumber) && role != "Customer") {
		console.log('2');
		displayInvalidAccount("Enter valid account number.");
		return;
	} else {
		console.log(transactionData);

		const response = await fetch('http://localhost:8080/Bank_Application/api/Transaction', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
			},
			body: JSON.stringify(transactionData)
		});

		const result = await response.json();
		console.log(result);
		if (result.message == 'success') {
			const successPop = document.getElementById('successModal');
			document.getElementById('successMessage').innerHTML = "Payment successful!";
			successPop.style.display = 'flex';
		} else {
			const errorMessage = document.getElementById('errorMessage');
			errorMessage.style.display = 'block';
			errorMessage.innerHTML = result.message;
		}

	}
};
function paymentcloseModal() {
	const modal = document.getElementById('successModal');
	modal.style.animation = 'fadeOut 0.5s ease-out';
	setTimeout(() => {
		modal.style.display = 'none';
	}, 500);
	window.location.reload();
}
const toggleBankDropdown = () => {
	const checkbox = document.getElementById('otherBankCheckbox');
	document.querySelectorAll(".bankDropdownContainer").forEach(a => {
		a.style.display = checkbox.checked === true ? 'flex' : 'none';
	});
	const bankField = document.getElementById('bank');
	const ifscField = document.getElementById('ifsc');

	if (checkbox.checked) {
		bankField.required = true;
		ifscField.required = true;
		bankField.disabled = false;
		ifscField.disabled = false;
	} else {
		bankField.required = false;
		ifscField.required = false;
		bankField.disabled = true;
		ifscField.disabled = true;
	}
}

const displayInvalidAccount = msg => {
	const errorMessage = document.getElementById('errorMessage');
	errorMessage.style.display = 'block';
	errorMessage.innerHTML = msg;
}

document.getElementById('type').addEventListener("change", function(event) {
	console.log(this.value)
	if (role !== "Customer") {
		document.getElementById('paymentmode').style.display = 'flex';
		if (this.value !== 'Debit') {
			document.getElementById('customerAccount').style.display = 'flex';
			document.getElementById('employeeAccount').style.display = 'none';
			document.querySelector('#otherBank').style.display = 'none';
		} else {
			document.querySelector('#otherBank').style.display = 'flex';
			document.getElementById('customerAccount').style.display = 'none';
			document.getElementById('employeeAccount').style.display = 'flex';
		}
	} else {
		document.getElementById('customerAccount').style.display = 'flex';
	}
})

document.addEventListener("DOMContentLoaded", () => {
	if (role == null) {
		window.location.href = "error.html";
	}
	const accountInput = document.getElementById("account");
	const accountInput2 = document.getElementById("transAcc");
	const otherBankCheckbox = document.querySelector('#otherBankCheckbox');

	handleInputDropdown(accountInput);
	if (!otherBankCheckbox.checked) {
		handleInputDropdown(accountInput2);
	}

	otherBankCheckbox.addEventListener('change', (e) => {
		if (e.target.checked) {
			console.log(accountInput2.nextElementSibling);
			accountInput2.nextElementSibling.style.display = "none";
		} else {
			handleInputDropdown(accountInput2);
		}
	});

});

const handleInputDropdown = accountInput => {
	const suggestionsBox = document.createElement("div");
	suggestionsBox.className = "dropdown-menu position-absolute bg-white";
	suggestionsBox.style.display = "none";
	suggestionsBox.style.border = "1px solid #ccc";
	suggestionsBox.style.maxHeight = "150px";
	suggestionsBox.style.overflowY = "auto";

	accountInput.parentNode.appendChild(suggestionsBox);

	let timeoutId = null;
	accountInput.addEventListener("input", () => {
		const otherBankCheckbox = document.querySelector('#otherBankCheckbox');
		if (otherBankCheckbox.checked) {
			return;
		}
		console.log(accountInput.value);
		const inputValue = accountInput.value.trim();

		clearTimeout(timeoutId);

		if (inputValue.length >= 4 && role != "Customer") {
			timeoutId = setTimeout(async () => {
				try {
					const accountData = {
						get: true,
						status: "Active",
						accountNumber: Number(inputValue)
					}
					const response = await fetch(
						`http://localhost:8080/Bank_Application/api/Account`,
						{
							method: 'POST',
							headers: {
								'Content-Type': 'application/json',
							},
							body: JSON.stringify(accountData)
						}
					);
					const result = await response.json();
					if (result.message == 'Invalid token' || result.message == 'Invalid Access token') {
						document.querySelector('body').style.display = 'none';
						window.location.href = "error.html";
					} else if (result.message == 'You dont have a account ') {
						document.querySelector('body').style.display = 'none';
						deleteAllCookies();
						sessionStorage.setItem('error', "No Account exists for the user.");
						window.location.href = "index.html";
					}
					console.log(result);
					// Clear previous suggestions
					suggestionsBox.innerHTML = "";
					if (result.accounts && result.accounts.length > 0) {
						result.accounts.forEach(account => {
							const option = document.createElement("div");
							option.className = "dropdown-item";
							option.style = `
								    cursor: pointer;
								    padding: 10px;
								    transition: background-color 0.3s ease;
								`;

							const accountContainer = document.createElement("div");

							// Create a div for the account number
							const accountNumber = document.createElement("div");
							accountNumber.textContent = 'Account number: ' + account.accountNumber;
							accountNumber.style = `
								    font-weight: bold;
								    font-size: 14px;
								    color: #2c3e50;
								`;

							// Create a div for the account balance
							const accountBalance = document.createElement("div");
							accountBalance.textContent = 'Balance: ₹' + account.balance; // Using the rupee symbol directly
							accountBalance.style = `
								    font-size: 12px;
								    color: grey;
								    margin-top: 5px;
								`;
							validAccounts.push(account.accountNumber + '');
							accountContainer.appendChild(accountNumber);
							accountContainer.appendChild(accountBalance);

							option.appendChild(accountContainer);

							option.dataset.accountId = account.id;

							option.addEventListener("click", () => {
								accountInput.value = account.accountNumber;
								suggestionsBox.style.display = "none";
							});

							option.addEventListener("mouseenter", () => {
								option.style.backgroundColor = "#f1f1f1";
							});
							option.addEventListener("mouseleave", () => {
								option.style.backgroundColor = "transparent";
							});

							suggestionsBox.appendChild(option);

						});

						suggestionsBox.style.display = "block";
					} else {
						suggestionsBox.style.display = "none";
					}
				} catch (error) {
					console.error("Error fetching suggestions:", error);
					suggestionsBox.style.display = "none";
				}
			}, 300);
		} else {
			suggestionsBox.style.display = "none";
		}
	})

	document.addEventListener("click", (event) => {
		if (!accountInput.contains(event.target) && !suggestionsBox.contains(event.target)) {
			suggestionsBox.style.display = "none";
		}
	})
}

document.addEventListener("DOMContentLoaded", async _ => {
	if (role === "Customer") {
		document.getElementById('customerAccount').style.display = 'flex';
		document.getElementById('accounts-tree').style.display = 'none';
		document.getElementById('users-tree').style.display = 'none';
	} else {
		document.getElementById('paymentmode').style.display = 'flex';
	}
	try {
		const response = await fetch(`http://localhost:8080/Bank_Application/api/Account?userId=-1`, {
			method: 'GET',
			headers: {
				'Content-Type': 'application/json',
			},
		});
		const result = await response.json(), accountsDropdown = document.querySelector('.accountsSelect');;
		console.log(result);
		if (result.message == 'Invalid token' || result.message == 'Invalid Access token') {
			deleteAllCookies();
			window.location.href = "error.html";
		} else if (result.message == 'You dont have a account ') {
			document.querySelector('body').style.display = 'none';
			deleteAllCookies();
			sessionStorage.setItem('error', "No Account exists for the user.");
			window.location.href = "index.html";
		}
		result.accounts.forEach((account) => {
			const option = document.createElement('option');
			option.value = account.accountNumber;
			validAccounts.push(account.accountNumber + '');
			option.id = account.branchId;
			option.textContent = account.accountNumber;
			accountsDropdown.appendChild(option);
		});
		// dropdown change event listener
		accountsDropdown.addEventListener("change", () => {
			const selectedValue = accountsDropdown.value;
			currentAccountIndex = selectedValue;
		});
	} catch (error) {
		console.log(error);
		document.querySelector('body').style.display = 'none';
	}

});