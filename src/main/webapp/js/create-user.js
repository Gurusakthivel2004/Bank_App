let currentCreate = 'Customer';
const token = localStorage.getItem('token');
window.onload = async _ => {
	const role = localStorage.getItem("role");
	if (role === "Customer" || role == null) {
		alert("You do not have permission to access this page.");
		window.history.back();
	}
	toggleDisplay();
};

document.querySelectorAll('.accNumberInput').forEach(item => {
	item.addEventListener('click', function(event) {
		const parentDiv = this.closest('.detail-item');
		const dropdownMenu = parentDiv.querySelector(".dropdown-menu");
		if (dropdownMenu != null) {
			dropdownMenu.style.display = dropdownMenu.style.display == "block" ? "none" : "block";
		}
	})
})
// Update dropdown selection and store the new value
document.querySelectorAll('.dropdown-item').forEach(item => {
	item.addEventListener('click', function(event) {
		const parentDiv = this.closest('.detail-item');
		const selectedText = this.innerHTML;
		const dropdownTextElement = parentDiv.querySelector('.accNumberInput');
		const dropdownMenu = parentDiv.querySelector(".dropdown-menu");
		dropdownMenu.style.display = "none"
		dropdownTextElement.value = selectedText;
		parentDiv.querySelectorAll('.dropdown-item').forEach(item => item.classList.remove('active'));
		this.classList.add('active');
	});
});

const toggleDisplay = _ => {
	const role = localStorage.getItem('role');
	if (role == 'Employee') {
		document.getElementById('branchOption').style.display = 'none';
		document.getElementById('managerOption').style.display = 'none';
		document.getElementById('employeeOption').style.display = 'none';
	}
	const createRole = document.getElementById('profileSelect').value;
	currentCreate = createRole;
	if (createRole === 'Customer') {
		document.getElementById('rightSection').style.maxWidth = '';
		document.getElementById('branchIdDiv').style.display = 'none';
		document.getElementById('branchId').required = false;
		document.getElementById('userDetails').style.display = 'block';
		document.getElementById('accountSection').style.display = 'none';
		document.getElementById('addressDiv').style.display = 'flex';
		document.getElementById('customerDetails').style.display = 'block';
		[...document.getElementsByClassName('detail-label')].forEach(item => {
			item.style.marginLeft = '10%';
		});

		document.querySelectorAll('#customerDetails input, #userDetails textarea').forEach(input => {
			input.required = true;
		});
	} else if (createRole == 'Account') {
		document.getElementById('rightSection').style.maxWidth = '50%';
		document.getElementById('accountSection').style.display = 'block';
		document.getElementById('branchSection').style.display = 'none';
		document.getElementById('userDetails').style.display = 'none';
		document.getElementById('customerDetails').style.display = 'none';
		document.querySelectorAll('#customerDetails input, #userDetails input, #userDetails textarea').forEach(input => {
			input.required = false;
		});
	} else if (createRole == 'Branch') {
		document.getElementById('rightSection').style.maxWidth = '50%';
		document.getElementById('accountSection').style.display = 'none';
		document.getElementById('branchSection').style.display = 'block';
		document.getElementById('userDetails').style.display = 'none';
		document.getElementById('customerDetails').style.display = 'none';
		document.querySelectorAll('#customerDetails input, #accountSection input, #userDetails input, #userDetails textarea').forEach(input => {
			input.required = false;
		});
	} else {
		document.getElementById('rightSection').style.maxWidth = '50%';
		document.getElementById('branchIdDiv').style.display = 'flex';
		document.getElementById('branchSection').style.display = 'none';
		document.getElementById('branchId').required = true;
		document.getElementById('accountSection').style.display = 'none';
		document.getElementById('addressDiv').style.display = 'none';
		document.getElementById('userDetails').style.display = 'block';
		document.getElementById('customerDetails').style.display = 'none';
		[...document.getElementsByClassName('detail-label')].forEach(item => {
			item.style.marginLeft = '15%';
		});

		document.querySelectorAll('#customerDetails input, #userDetails textarea').forEach(input => {
			input.required = false;
		});
	}
};

document.getElementById('profileSelect').addEventListener('change', toggleDisplay);

const successDisplay = (successPop) => {
	successPop.textContent = 'Created successfully!';
	successPop.style.backgroundColor = 'green';
	successPop.style.display = 'block';
	document.querySelectorAll('.accNumberInput').forEach(item => {
		item.value = '';
	})
}

const errorDisplay = (successPop, message) => {
	successPop.textContent = message;
	successPop.style.backgroundColor = 'red';
	successPop.style.color = 'white';
	successPop.style.display = 'block';
}

const saveAccount = async formDataObject => {
	console.log(formDataObject);
	try {
		const response = await fetch('http://localhost:8080/Bank_Application/api/Account', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				'Authorization': `Bearer ${token}`
			},
			body: JSON.stringify(formDataObject)
		});
		const result = await response.json();
		return result
	} catch (error) {
		console.error('Error during fetch or processing:' + error);
	}
}

const saveBranch = async formDataObject => {
	let modifiedObject = {};
	Object.keys(formDataObject).forEach(key => {
		const newKey = key.startsWith('branch-') ? key.replace('branch-', '') : key;
		modifiedObject[newKey] = formDataObject[key];
	});
	console.log(modifiedObject);
	try {
		const response = await fetch('http://localhost:8080/Bank_Application/api/Branch', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json'
			},
			body: JSON.stringify(modifiedObject)
		});
		const result = await response.json();
		return result
	} catch (error) {
		console.error('Error during fetch or processing:' + error);
	}
}

const saveUser = async formDataObject => {
	console.log(formDataObject);
	formDataObject['role'] = document.getElementById('profileSelect').value;
	formDataObject['password'] = 'default';
	formDataObject['status'] = 'Active';
	console.log(formDataObject);
	try {
		const response = await fetch('http://localhost:8080/Bank_Application/api/User', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				'Authorization': `Bearer ${token}`
			},
			body: JSON.stringify(formDataObject)
		});
		const result = await response.json();
		return result;
	} catch (error) {
		console.error('Error during fetch or processing:' + error);
	}
}

async function toggleSaveAll(event) {
	event.preventDefault();
	const form = document.querySelector('#userForm');
	const inputs = form.querySelectorAll('input, select, textarea');
	const formDataObject = {};
	inputs.forEach(input => {
		if (input.value.length > 0) {
			formDataObject[input.id] = input.value;
		}
	});
	delete formDataObject.profileSelect;
	let result;
	if (currentCreate == 'Account') {
		result = await saveAccount(formDataObject);
	} else if (currentCreate == 'Branch') {
		result = await saveBranch(formDataObject);
	} else {
		result = await saveUser(formDataObject);
	}
	console.log(result);
	const successPop = document.getElementById('successPopup');
	if (result.message == 'success') {
		successDisplay(successPop);
	} else {
		errorDisplay(successPop, result.message);
	}
	setTimeout(() => {
		successPop.style.display = 'none';
	}, 3000);
}