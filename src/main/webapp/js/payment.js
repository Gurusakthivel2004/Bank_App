let originalValue = '';
let originalActiveItem = null;
const token = localStorage.getItem('token')

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
	let accountNumber = '';
	[...document.querySelectorAll('.accounts')].forEach(input => {
		if (input.value > 0) {
			accountNumber = input.value > 0 ? input.value : transactionAccountNumber;
		}

	})
	const transactionAccountNumber = document.getElementById('transAcc').value;
	const amount = document.getElementById('amount').value;
	const remarks = document.querySelector('textarea[placeholder="Enter remarks"]').value;
	const role = localStorage.getItem("role");
	let transactionType = 'Debit';
	if (role !== "Customer") {
		transactionType = document.getElementById('type').value;
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

	const response = await fetch('http://localhost:8080/Bank_Application/api/Transaction', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`
		},
		body: JSON.stringify(transactionData)
	});

	const result = await response.json();
	console.log(result);
	const successPop = document.getElementById('successPopup');
	if (result.message == 'success') {
		successPop.textContent = "Payment successful!";
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
	}, 3000);
};

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

document.getElementById('type').addEventListener("change", function(event) {
	const role = localStorage.getItem("role");
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


document.addEventListener("DOMContentLoaded", async _ => {
	const role = localStorage.getItem("role");
	if (role === "Customer") {
		document.getElementById('customerAccount').style.display = 'flex';
		document.getElementById('accounts-tree').style.display = 'none';
	} else {
		document.getElementById('paymentmode').style.display = 'flex';
	}
	try {
		const response = await fetch('http://localhost:8080/Bank_Application/api/Account?userId=-1', {
			method: 'GET',
			headers: {
				'Content-Type': 'application/json',
				'Authorization': `Bearer ${token}`
			},
		});
		const result = await response.json(), accountsDropdown = document.querySelector('.accountsSelect');;
		console.log(result);
		result.accounts.forEach((account) => {
			const option = document.createElement('option');
			option.value = account.accountNumber;
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
	}

});