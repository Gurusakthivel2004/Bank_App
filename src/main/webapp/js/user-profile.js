let originalValues = {};
let updatedFields = [];
const token = localStorage.getItem('token')

// user details fetch
window.onload = async _ => {
	let role = localStorage.getItem("role");
	console.log(role)
	if (role === 'Customer') {
		document.getElementById('accounts-tree').style.display = 'none';
	} else {
		toggleDisplay();
	}
	try {
		const response = await fetch('http://localhost:8080/Bank_Application/api/User', {
			method: 'GET',
			headers: {
				'Content-Type': 'application/json',
				'Authorization': `Bearer ${token}`
			},
		});
		const result = await response.json();
		setValues(result[0]);
		console.log(result)
	} catch (error) {
		console.error('Error during fetch or processing:' + error);
		window.location = "index.html";
	}
};

const setValues = object => {
	const keys = Object.keys(object);
	for (let i = 0; i < keys.length; i++) {
		const key = keys[i];
		const htmlclass = document.getElementById(key);
		if (htmlclass != null) {
			console.log()
			if (htmlclass.localName == 'textarea') {
				htmlclass.innerHTML = object[key];
			} else {
				htmlclass.value = object[key];
			}
		}
	}
};

const toggleDisplay = _ => {
	document.getElementById('rightSection').style.maxWidth = '50%';
	document.getElementById('addressDiv').style.display = 'none';
	document.getElementById('statusDiv').style.border = "0px";
	document.getElementById('customerDetails').style.display = 'none';
	[...document.getElementsByClassName('detail-label')].forEach(item => {
		item.style.marginLeft = '15%';
	});

	document.querySelectorAll('#customerDetails input, #userDetails textarea').forEach(input => {
		input.required = false;
	});
};


function toggleEditOne(button) {
	const parentDiv = button.closest(".detail-item");
	const inputField = parentDiv.querySelector(".accNumberInput");
	const editButton = parentDiv.querySelector(".editOneButton");
	const saveButton = document.querySelector(".saveAllButton");
	const cancelButton = parentDiv.querySelector(".cancelButton");
	const dropdownMenu = parentDiv.querySelector(".dropdown-menu");
	const fieldId = parentDiv.getAttribute("data-id");
	document.querySelector(".saveAllButton").style.display = "block";
	document.querySelector(".editAllButton").style.display = "none";

	if (dropdownMenu !== null) {
		dropdownMenu.style.display = "block";
	}

	originalValues[fieldId] = inputField.value;

	inputField.disabled = false;
	editButton.style.display = "none";
	saveButton.style.display = "block";
	cancelButton.style.display = "block";
	inputField.style.border = "1px solid #2b0444";
	inputField.style.cursor = "pointer";

	inputField.addEventListener("input", function() {
		handleInputChange(inputField, parentDiv);
	});
}

function handleInputChange(inputField, parentDiv) {
	const fieldId = parentDiv.getAttribute("data-id");

	const existingFieldIndex = updatedFields.findIndex(field => field.id === fieldId);
	if (inputField.value !== originalValues[fieldId]) {
		if (existingFieldIndex === -1) {
			updatedFields.push({ id: fieldId, name: inputField.id, value: inputField.value });
		} else {
			updatedFields[existingFieldIndex].value = inputField.value;
		}
	} else {
		if (existingFieldIndex !== -1) {
			updatedFields.splice(existingFieldIndex, 1);
		}
	}
}
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
		const fieldId = parentDiv.getAttribute("data-id");

		dropdownTextElement.value = selectedText;
		dropdownMenu.style.display = "none"

		const existingFieldIndex = updatedFields.findIndex(field => field.id === fieldId);
		if (existingFieldIndex === -1) {
			updatedFields.push({ id: fieldId, name: dropdownTextElement.id, value: dropdownTextElement.value });
		} else {
			updatedFields[existingFieldIndex].value = selectedText;
		}

		parentDiv.querySelectorAll('.dropdown-item').forEach(item => item.classList.remove('active'));
		this.classList.add('active');
	});
});

function toggleCancel(button) {
	const parentDiv = button.closest('.detail-item');
	const editButton = parentDiv.querySelector(".editOneButton");
	const inputField = parentDiv.querySelector('.accNumberInput');
	const dropdownMenu = parentDiv.querySelector(".dropdown-menu");
	const fieldId = parentDiv.getAttribute("data-id");

	inputField.value = originalValues[fieldId] || '';

	editButton.style.display = "block";
	inputField.disabled = true;
	inputField.style.cursor = "default";
	button.style.display = "none";
	inputField.style.border = "1px solid white";

	if (dropdownMenu !== null) {
		parentDiv.querySelectorAll('.dropdown-item').forEach(item => item.classList.remove('active'));
		const originalActiveItem = dropdownMenu.querySelector(`.dropdown-item[data-value='${originalValues[fieldId]}']`);
		if (originalActiveItem) {
			originalActiveItem.classList.add('active');
		}
		dropdownMenu.style.display = "none";
	}

	// Remove the field from updatedFields array if it exists
	updatedFields = updatedFields.filter(field => field.id !== fieldId);

	const anyCancelVisible = Array.from(document.querySelectorAll(".cancelButton")).some(btn => btn.style.display !== "none");
	if (!anyCancelVisible) {
		document.querySelector(".saveAllButton").style.display = "none";
		document.querySelector(".editAllButton").style.display = "block";
	}
}

function toggleEditAll() {
	document.querySelectorAll(".accNumberInput").forEach(input => {
		input.disabled = false;
		input.style.border = "1px solid #2b0444";
		input.style.cursor = "pointer";

		const parentDiv = input.closest('.detail-item');
		const fieldId = parentDiv.getAttribute("data-id");
		originalValues[fieldId] = input.value;

		input.addEventListener("input", function() {
			handleInputChange(input, parentDiv);
		});
	});

	document.querySelectorAll(".editOneButton").forEach(button => {
		button.style.display = "none";
	});

	document.querySelector(".saveAllButton").style.display = "block";
	document.querySelector(".editAllButton").style.display = "none";
	document.querySelector(".cancelAllButton").style.display = "block";
}

async function toggleSaveAll() {
	if (updatedFields.length > 0) {
		console.log("Saving updated fields:", updatedFields);
		try {
			const response = await fetch('http://localhost:8080/Bank_Application/api/User', {
				method: 'PUT',
				headers: {
					'Content-Type': 'application/json',
					'Authorization': `Bearer ${token}`
				},
				body: JSON.stringify(updatedFields)
			});
			const result = await response.json();
			const successPop = document.getElementById('successPopup');
			if (result.message == 'success') {
				successPop.textContent = "profile updated successfully!";
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
		} catch (error) {
			console.error('Error during fetch or processing:' + error);
		}
		updatedFields = [];
	} else {
		console.log("No changes to save.");
	}

	document.querySelectorAll(".accNumberInput").forEach(input => {
		input.disabled = true;
		input.style.border = "1px solid white";
		input.style.cursor = "default";
	});

	document.querySelectorAll(".editOneButton").forEach(button => {
		button.style.display = "block";
	});

	document.querySelectorAll(".cancelButton").forEach(button => {
		button.style.display = "none";
	});

	document.querySelector(".saveAllButton").style.display = "none";
	document.querySelector(".cancelAllButton").style.display = "none";
	document.querySelector(".editAllButton").style.display = "block";
}

function toggleCancelAll() {
	document.querySelector(".cancelAllButton").style.display = "none";
	location.reload();
}