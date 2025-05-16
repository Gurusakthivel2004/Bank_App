const createNavButton = document.getElementById("create-user-item");
const logButton = document.getElementById("log-item");
const empChat = document.getElementById('employeeChatbutton');
const cusChat = document.getElementById('customerChatbutton')
const reqButton = document.getElementById('requestbutton');
const bellButton = document.getElementById('bellbutton');

const getCookie = name => {
	const cookies = document.cookie.split('; ');
	for (let cookie of cookies) {
		const [key, value] = cookie.split('=');
		if (key === name) {
			return decodeURIComponent(value);
		}
	}
	return null;
}

function moveToNext(input, event) {
	let next = input.nextElementSibling;
	let prev = input.previousElementSibling;

	if (event.inputType === 'deleteContentBackward' && prev) {
		prev.focus();
	} else if (next && input.value) {
		next.focus();
	}
}

const userRole = getCookie('role');
const branchId = getCookie('branchId');

if (userRole == "Customer") {
	let ele = document.getElementsByClassName('left-section');
	for (let index = 0; index < ele.length; index++) {
		ele[index].style.marginBottom = '350px';
	}
	if (createNavButton != null) {
		createNavButton.style.display = "none";
	}
	if (logButton != null) {
		logButton.style.display = "none";
	}
	if (empChat != null) document.getElementById('employeeChatbutton').style.display = 'none';
	if (bellButton != null) document.getElementById('bellbutton').style.display = 'none';
} else if (userRole == "Employee") {
	if (logButton != null) {
		logButton.style.display = "none";
	}
	if (cusChat != null) document.getElementById('customerChatbutton').style.display = 'none';
	if (reqButton != null) document.getElementById('requestbutton').style.display = 'none';
} else if (userRole == "Manager") {
	if (cusChat != null) document.getElementById('customerChatbutton').style.display = 'none';
	if (reqButton != null) document.getElementById('requestbutton').style.display = 'none';
}

if (document.getElementById("requestType") != null) {
	document.getElementById("requestType").addEventListener("change", function(event) {
		const selectedAction = event.target.value;

		document.getElementById("create-transaction").style.display = "none";
		document.getElementById("accountNumberdiv").style.display = "none";
		document.getElementById("update-profile").style.display = "none";

		if (selectedAction === "createTransaction") {
			document.getElementById("accountNumberdiv").style.display = "flex";
			document.getElementById("create-transaction").style.display = "block";
			document.getElementById("transactionAccount").style.display = "flex";
			document.getElementById("update-profile").style.display = "none";
		} else if (selectedAction === "deactivateAccount") {
			document.getElementById("accountNumberdiv").style.display = "flex";
			document.getElementById("create-transaction").style.display = "none";
			document.getElementById("update-profile").style.display = "none";
		} else if (selectedAction === "updateProfile") {
			document.getElementById("accountNumberdiv").style.display = "none";
			document.getElementById("create-transaction").style.display = "none";
			document.getElementById("update-profile").style.display = "block";
		} else if (selectedAction === "applyLoan") {
			document.getElementById("accountNumberdiv").style.display = "flex";
			document.getElementById("create-transaction").style.display = "flex";
			document.getElementById("transactionAccount").style.display = "none";
			document.getElementById("update-profile").style.display = "none";
		}
	});
}

let offset = 0;
const limit = 8;
let hasMore = true;
document.addEventListener("DOMContentLoaded", () => {
	if (userRole != "Customer") {
		fetchNotifications();
		document.getElementById('notificationDropdown').style.display = 'none';
	}
})

async function fetchNotifications(openDropdown = false, loadMore = false) {
	if (!hasMore) return;
	const response = await fetch(`/Bank_Application/api/Message?branchId=${branchId}&limit=${limit}&offset=${offset}&status=pending`, {
		method: 'GET',
		headers: {
			'Content-Type': 'application/json',
		}
	});

	const result = await response.json();
	console.log(result);
	if (result.messages == null || result.messages.length == 0) return;
	if (result.message != null && (result.message.includes('Session expired') || result.message == 'Invalid Access token')) {
		document.querySelector('body').style.display = 'none';
		deleteAllCookies();
		window.location.href = "error.html";
	}
	else if (result.message == 'You dont have a account ') {
		document.querySelector('body').style.display = 'none';
		deleteAllCookies();
		sessionStorage.setItem('error', "No Account exists for the user.");
		window.location.href = "index.html";
	}
	const pendingMessages = result.messages.filter(msg => msg.messageStatus === "Pending");
	const notificationList = document.getElementById('notificationList');
	const notificationBadge = document.getElementById('notificationBadge');
	const loadMoreBtn = document.getElementById('loadMoreBtn');

	hasMore = pendingMessages.length - 1 >= limit;
	if (pendingMessages.length > 0) {
		notificationBadge.textContent = result.messages.length > limit ? '8+' : result.messages.length;
		notificationBadge.style.display = 'grid';

		pendingMessages.forEach(({ id, senderId, messageType, messageContent, createdAt }) => {
			const notificationItem = document.createElement('div');
			notificationItem.classList.add('notification-item');
			notificationItem.innerHTML = `
			<div class="notif-content" onclick="showModal('${senderId}', '${messageType}', '${messageContent}', ${createdAt})">
								<strong>${messageType}</strong>
								<p>${messageContent}</p>
								<small>${timeAgo(new Date(createdAt))}</small>
							</div>
							<button class="mark-read-btn" style="border:0;border-radius:200%;padding:0;margin:0;background:transparent;" onclick="event.stopPropagation(); markAsRead('${id}', this)">
								<img src="./images/success.webp" width=30px height=30px />
							</button>
							<button class="mark-read-btn" style="border:0;border-radius:200%;padding:0;margin:0;margin-left:5px;background:transparent;" onclick="event.stopPropagation(); cancelRequest('${id}', this)">
															<img src="./images/cancel.svg" width=30px height=30px />
														</button>
			`;
			notificationList.appendChild(notificationItem);
		});

		offset += limit;
	}
	loadMoreBtn.style.display = hasMore ? 'block' : 'none';

	if (openDropdown) {
		toggleDropdown();
	}
}

async function markAsRead(messageId, btn) {
	const token = getCookie('token');

	const response = await fetch(`/Bank_Application/api/Message`, {
		method: 'PUT',
		headers: {
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`
		},
		body: JSON.stringify({ messageId: messageId, messageStatus: 'Completed' })
	});
	if (response.ok) {
		if (response.message != null && response.message.includes('Session expired') || response.message == 'Invalid Access token') {
			document.querySelector('body').style.display = 'none';
			deleteAllCookies();
			window.location.href = "error.html";
		}
		if (response.message == 'You dont have a account ') {
			document.querySelector('body').style.display = 'none';
			window.location.href = "error.html";
		}
		const successPop = document.getElementById('successModal');
		document.getElementById('successMessage').innerHTML = "Completed successully";
		successPop.style.display = 'flex';
		btn.parentElement.remove();
		toggleDropdown();
		fetchNotifications();
	}
}

async function cancelRequest(messageId, btn) {
	const token = getCookie('token');

	const response = await fetch(`/Bank_Application/api/Message`, {
		method: 'PUT',
		headers: {
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`
		},
		body: JSON.stringify({ messageId: messageId, messageStatus: 'Cancelled' })
	});
	if (response.message != null && (response.message.includes('Session expired') || response.message == 'Invalid Access token')) {
		document.querySelector('body').style.display = 'none';
		deleteAllCookies();
		window.location.href = "error.html";
	}
	if (response.ok) {
		const successPop = document.getElementById('successModal');
		document.getElementById('successMessage').innerHTML = "Cancelled successully";
		successPop.style.display = 'flex';
		btn.parentElement.remove();
		toggleDropdown();
		fetchNotifications();
	}
}

function showModal(senderId, messageType, messageContent, createdAt) {
	const modal = document.getElementById('messageModal');
	document.getElementById('modalMessageType').textContent = messageType;
	document.getElementById('modalMessageContent').innerHTML = messageContent.replace(/\n/g, '<br>');
	document.getElementById('modalMessagesender').textContent = 'sender Id: ' + senderId;
	document.getElementById('modalCreatedAt').textContent = getDate(createdAt, true);

	modal.style.display = 'flex';
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


function closeModal() {
	document.getElementById('messageModal').style.display = 'none';
}

function toggleDropdown() {
	const notificationDropdown = document.getElementById('notificationDropdown');
	const isOpen = notificationDropdown.style.display === 'block';
	if (!isOpen) {
		notificationDropdown.style.display = 'block';
	} else {
		notificationDropdown.style.display = 'none';
	}
}

function timeAgo(date) {
	const seconds = Math.floor((new Date() - date) / 1000);
	const intervals = { year: 31536000, month: 2592000, week: 604800, day: 86400, hour: 3600, minute: 60 };
	for (const [key, value] of Object.entries(intervals)) {
		const count = Math.floor(seconds / value);
		if (count > 0) return `${count} ${key}${count > 1 ? 's' : ''} ago`;
	}
	return 'Just now';
}

const bell = document.getElementById('notificationBell');
const load = document.getElementById('loadMoreBtn')
if (bell != null) {
	bell.addEventListener('click', toggleDropdown);
}
if (load != null) {
	load.addEventListener('click', () => fetchNotifications(false, true));
}
function sendRequest() {
	const selectedAction = document.getElementById("requestType").value;

	const accountNumber = document.getElementById("requestaccount")?.value?.trim();
	const transactionAccount = document.getElementById("transactionAccount")?.value?.trim();
	const amount = document.getElementById("requestamount")?.value?.trim();

	const requestMessage = document.getElementById("requestMessage");

	let requestData = {};

	requestMessage.style.display = "none";
	requestMessage.textContent = "";

	if (selectedAction === "createTransaction") {
		if (!accountNumber || !transactionAccount || !amount) {
			requestMessage.style.display = "block";
			requestMessage.textContent = "Please fill in all fields for Create Transaction.";
			return;
		}
		requestData = {
			senderId: getCookie("id"),
			messageType: "TransactionRequest",
			messageContent: "Account number: " + accountNumber + "/n transaction account number: " + transactionAccount + " /n amount: " + Number(amount)
		};
	} else if (selectedAction === "deactivateAccount") {
		if (!accountNumber) {
			requestMessage.style.display = "block";
			requestMessage.textContent = "Please provide the Account Number for Deactivate Account.";
			return;
		}
		requestData = {
			senderId: getCookie("id"),
			messageType: "AccountRequest",
			messageContent: "Account number: " + accountNumber
		};
	} else if (selectedAction === "updateProfile") {
		let profileData = "";
		const profileInputs = document.querySelectorAll("#update-profile input, #update-profile select, #update-profile textarea");

		profileInputs.forEach((input) => {
			const key = input.id;
			const value = input.value.trim();
			if (value) {
				if (profileData.size > 0) profileData += ", ";
				profileData += (key + "=" + value);
			}
		});
		if (profileData.size == 0) {
			requestMessage.style.display = "block";
			requestMessage.textContent = `Please fill in necessary fields to update.`;
			return;
		}

		if (requestMessage.style.display === "block") return;

		requestData = {
			senderId: getCookie("id"),
			messageType: "UserRequest",
			messageContent: profileData
		};
	} else if (selectedAction === "applyLoan") {
		if (!accountNumber || !amount) {
			requestMessage.style.display = "block";
			requestMessage.textContent = "Please fill in all fields for appl.";
			return;
		}
		requestData = {
			senderId: getCookie("id"),
			messageType: "ApplyLoan",
			messageContent: "Account number: " + accountNumber + ", Amount: " + amount
		};
	} else {
		requestMessage.style.display = "block";
		requestMessage.textContent = "Invalid action selected.";
		return;
	}

	console.log("Request Data:", requestData);
	sendRequestToServer(requestData);
}

const sendRequestToServer = async data => {

	const response = await fetch('/Bank_Application/api/Message', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(data)
	});
	const result = await response.json();
	if (result.message.includes('Session expired') || result.message == 'Invalid Access token') {
		document.querySelector('body').style.display = 'none';
		deleteAllCookies();
		window.location.href = "error.html";
	}
	if (result.message == 'success') {
		toggleModal('newRequestModal');
		document.getElementById('successMessage').innerHTML = "Request sent!";
		document.getElementById('successModal').style.display = 'flex';
	} else {
		const requestMessage = document.getElementById("requestMessage");
		requestMessage.style.display = "block";
		requestMessage.textContent = result.message;
	}
}

function deleteAllCookies() {
	document.cookie.split(";").forEach((cookie) => {
		document.cookie = cookie.split("=")[0] +
			"=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/Bank_Application;";
	});
}

const logout = async _ => {
	try {
		const token = getCookie('token');
		const response = await fetch('/Bank_Application/api/Logout', {
			method: 'DELETE',
			headers: {
				'Content-Type': 'application/json',
				'Authorization': `Bearer ${token}`
			},
		});
		const result = await response.json();
		if (result) {
			deleteAllCookies();
			window.location.href = 'index.html';
		}
	} catch (error) {
		console.error('Error during fetch or processing:', error);
	}
}
function closeModal() {
	const modal = document.getElementById('successModal');
	modal.style.animation = 'fadeOut 0.5s ease-out';
	setTimeout(() => {
		modal.style.display = 'none';
	}, 500);
}
const toggleProfilePanel = _ => {
	const panel = document.getElementById('profilePanel');
	panel.classList.toggle('open');
}

document.querySelectorAll('.createOption').forEach(item => {
	item.addEventListener('click', _ => {
		sessionStorage.setItem("createUserRole", item.innerHTML);
		window.location.href = "create-user.html";
	})
})

const toggleSubmenu = () => {
	const submenu = document.getElementById('userOptions');
	submenu.style.display = submenu.style.display === 'none' ? 'block' : 'none';
}

document.getElementById('profileButton').addEventListener('click',
	toggleProfilePanel);

function sendMessage() {
	const messageInput = document.getElementById("messageInput");
	const chatMessages = document.getElementById("chatMessages");
	const newMessage = messageInput.value.trim();
	if (newMessage !== "") {
		const messageDiv = document.createElement("div");
		messageDiv.className = "message";
		messageDiv.style.display = "flex";
		messageDiv.style.justifyContent = "flex-end";
		messageDiv.innerHTML = `<div class="message-bubble" style="background-color: #f1f1f1; color: #333; padding: 8px 15px; border-radius: 20px 20px 20px 0; max-width: 75%; margin-right: 10px;">
	                                ${newMessage}
	                            </div>`;
		chatMessages.appendChild(messageDiv);
		messageInput.value = ""; // Clear the input after sending
		chatMessages.scrollTop = chatMessages.scrollHeight; // Auto scroll to the latest message
	}
}

const toggleModal = modalId => {
	const modal = document.getElementById(modalId);
	if (modalId == "chatBoxModel") {
		if (modal.style.display == 'none') {
			startChat();
		}
	}
	modal.style.display = modal.style.display === 'none'
		|| modal.style.display === '' ? 'flex' : 'none';
	if (modalId == "accountDetailsModal") {
		document.getElementById('accountbranchId').style.border = "0";
		document.getElementById('branchSave').style.display = "none";
		document.getElementById('statusSave').style.display = "none";

		if (modal.style.display === "flex") {
			if (role == "Manager") {
				document.getElementById('branchEdit').style.display = "block";
			}
			document.getElementById('statusEdit').style.display = "block";
		}
		document.getElementById('accountbranchId').disabled = true;
		document.getElementById('dropdown').style.display = "none";
		document.getElementById('dropdownStatus').style.display = "none";
	}
	if (modalId == "newUserModal") {
		console.log('hi');
		document.getElementById('saveButton').style.display = 'none';
		document.getElementById('editButton').style.display = 'block';
		document.getElementById('newUserButton').style.display = 'none';
		document.getElementById('dropdown').style.display = 'none';
		document.getElementById('errorMessage').style.display = "none";
		document.querySelectorAll("#newUserModal input").forEach(input => {
			console.log(input.id == "subOrgName");
			if (!input.id == "subOrgName") {
				input.disabled = false;
			}
		})
	}
	if (modalId == "branchDetailsModal") {
		setBranchDetails(branchDetails, account.branchId);
		const inputElements = document.querySelectorAll("#branchDetailsModal input");
		const addressInput = document.getElementById('branchaddressInput');
		inputElements.forEach(input => {
			input.disabled = true;
			input.style.border = '0px solid #ccc';
		})
		addressInput.disabled = true;
		addressInput.style.border = '0px solid #ccc';
		document.getElementById('saveBranchButton').style.display = 'none';
		document.getElementById('branchIfscdiv').style.display = 'flex';
	}

}

const createOrg = _ => {
	const name = document.getElementById("orgName").value.trim();
	const orgType = document.getElementById("orgType").value.trim();
	const employees = document.getElementById("orgEmployees").value.trim();
	const phone = document.getElementById("orgPhone").value.trim();
	const salaryBand = document.getElementById("orgSalaryBand").value.trim();
	const parentOrg = document.getElementById("orgParentName").value.trim();

	const errorMessageEl = document.getElementById("orgErrorMessage");
	errorMessageEl.style.display = "none";
	errorMessageEl.innerText = "";

	if (parentOrg.length > 0 && validOrgNames.includes(parentOrg)) {
		if (!name || name.length < 4) {
			showError("Please enter a valid name.");
			return;
		}
		if (!salaryBand || isNaN(salaryBand) || parseInt(salaryBand) <= 0) {
			showError("Please enter a valid number of salary.");
			return;
		}

		const orgData = {
			name: name,
			salaryBand: salaryBand,
			parentOrgName: parentOrg
		};
		console.log("Sub Org Data:", orgData);
		submitOrgData(orgData, "SubOrg");
		return;
	}

	if (!name || name.length < 4) {
		showError("Please enter a valid name.");
		return;
	}
	if (!orgType || orgType === "- None -") {
		showError("Please select an org type.");
		return;
	}
	if (!employees || isNaN(employees) || parseInt(employees) <= 0) {
		showError("Please enter a valid number of employees.");
		return;
	}
	if (!phone || !/^\d{6,15}$/.test(phone)) {
		showError("Please enter a valid phone number (6 to 15 digits).");
		return;
	}

	const orgData = {
		name: name,
		orgType: orgType,
		employees: parseInt(employees),
		phone: phone,
	};
	console.log("Main Org Data :", orgData);
	submitOrgData(orgData, "Org");

	function showError(msg) {
		errorMessageEl.innerText = msg;
		errorMessageEl.style.display = "block";
	}
}

const submitOrgData = async (orgData, controller) => {
	console.log("Sending data to server:", orgData);
	const userDetailsResponse = await fetch('/Bank_Application/api/' + controller, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json',
		},
		body: JSON.stringify(orgData)
	});
	const result = await userDetailsResponse.json();
	console.log(result);
	if (result.message != null && (result.message.includes('Session expired') || result.message == 'Invalid Access token')) {
		document.querySelector('body').style.display = 'none';
		deleteAllCookies();
		window.location.href = "error.html";
	}
	else if (result.message == 'success') {
		successDisplayWithMessage("Org created successfully.");
	} else {
		const errorMessageElement = document.getElementById('orgErrorMessage');
		errorMessageElement.style.display = "block";
		errorMessageElement.innerHTML = result.message;
	}
}

const successDisplayWithMessage = (msg) => {
	toggleModal('orgModal')
	const successPop = document.getElementById('successModal');
	document.getElementById('successMessage').innerHTML = msg;
	successPop.style.display = 'flex';
}

let validOrgNames = [], validSubOrgNames = [];

const fetchOrgNameDropdownOptions = (query, id, flag) => {
	const dropdown = document.getElementById('orgDropdown');
	dropdown.innerHTML = '';

	if (!query.trim()) {
		dropdown.style.display = 'none';
		return;
	}

	fetch(`/Bank_Application/api/Org?name=${query}&notExact=true`, {
		method: 'GET',
		headers: {
			'Content-Type': 'application/json',
		},
	})
		.then(response => response.json())
		.then(data => {
			console.log(data);
			if (data.message != null && (data.message.includes('Session expired') || data.message == 'Invalid Access token')) {
				document.querySelector('body').style.display = 'none';
				deleteAllCookies();
				window.location.href = "error.html";
			}

			if (!data || data.length === 0) {
				dropdown.innerHTML = '<div class="dropdown-option" style="padding: 10px; color: grey;">Org not found</div>';
				dropdown.style.display = 'block';
				return;
			}

			validOrgNames = [];
			data.forEach(org => {
				const option = document.createElement('div');
				option.className = 'dropdown-option';
				option.style = `
	                    padding: 10px;
	                    cursor: pointer;
	                    border-bottom: 1px solid #ddd;
	                `;
				option.innerHTML = `
	                    <div style="font-weight: bold; color: #2c3e50;">
	                        ID: ${org.id}
	                    </div>
	                    <div style="font-size: 12px; color: grey;">
	                        Name: ${org.name}
	                    </div>
	                `;
				validOrgNames.push(org.name);

				option.onclick = () => {
					const input = document.getElementById(id);
					input.value = org.name;
					dropdown.style.display = 'none';
					if (flag) {
						fetchSubOrdData(org.id);
					}
				};
				dropdown.appendChild(option);
			});

			dropdown.style.display = 'block';
		})
		.catch(err => {
			console.error('Error fetching dropdown options:', err);
			dropdown.innerHTML = '<div class="dropdown-option" style="padding: 10px; color: red;">No Org found.</div>';
			dropdown.style.display = 'block';
		});
}

const fetchSubOrdData = orgId => {
	const select = document.getElementById("subOrgName");

	select.innerHTML = '<option value="">Select Org Name</option>';

	fetch(`/Bank_Application/api/SubOrg?orgId=${orgId}`, {
		method: 'GET',
		headers: {
			'Content-Type': 'application/json',
		},
	})
		.then(response => response.json())
		.then(data => {
			console.log(data);
			if (data.message != null && (data.message.includes('Session expired') || data.message == 'Invalid Access token')) {
				document.querySelector('body').style.display = 'none';
				deleteAllCookies();
				window.location.href = "error.html";
			}

			if (!data || data.length === 0) {
				const option = document.createElement("option");
				option.value = "";
				option.textContent = "Org not found";
				option.disabled = true;
				select.appendChild(option);
				return;
			}

			data.forEach(org => {
				const option = document.createElement("option");
				option.value = org.name;
				option.textContent = `${org.name}`;
				select.appendChild(option);
				validOrgNames.push(org.name);
			});

			select.disabled = false;
		});
};


const togglePasswordVisibility = (inputId, button) => {
	const passwordInput = document.getElementById(inputId);
	const type = passwordInput.getAttribute('type') === 'password' ? 'text'
		: 'password';
	passwordInput.setAttribute('type', type);

	const img = button.querySelector('img');
	img.src = type === 'password' ? './images/eye.svg'
		: './images/eyeclose.svg';
	img.alt = type === 'password' ? 'Show password'
		: 'Hide password';
}

const submitPasswordChange = async _ => {
	const currentPassword = document.getElementById('currentPassword').value;
	const newPassword = document.getElementById('newPassword').value;
	const confirmPassword = document.getElementById('confirmPassword').value;
	if (newPassword != confirmPassword) {
		document.getElementById('passwordmessage').style.display = 'block';
		document.getElementById('passwordmessage').textContent = 'Passwords do not match. Try again.';
		return;
	}
	if (newPassword == currentPassword) {
		document.getElementById('passwordmessage').style.display = 'block';
		document.getElementById('passwordmessage').textContent = 'Please enter a new password.';
		return;
	}
	const passwordData = {
		currentPassword: currentPassword,
		newPassword: newPassword
	};
	console.log(passwordData)
	try {
		const token = getCookie('token')
		const response = await fetch('/Bank_Application/api/User', {
			method: 'PUT',
			headers: {
				'Content-Type': 'application/json',
				'Authorization': `Bearer ${token}`
			},
			body: JSON.stringify(passwordData)
		});
		const result = await response.json();
		console.log(result)
		const passwordMessage = document.getElementById('passwordmessage');
		if (result.message != null && (result.message.includes('Session expired') || result.message == 'Invalid Access token')) {
			document.querySelector('body').style.display = 'none';
			deleteAllCookies();
			window.location.href = "error.html";
		}
		if (result.message == "success") {
			sessionStorage.setItem('passwordChangeSuccess', 'true');
			deleteAllCookies();
			window.location.href = 'index.html';
		} else {
			passwordMessage.style.display = 'block'
			passwordMessage.textContent = result.message;
		}
	} catch (error) {
		passwordMessage.textContent = 'An error occured. Try again.';
		return;
	}
}

let cookies = document.cookie.split('; ');
for (let i = 0; i < cookies.length; i++) {
	let [key, value] = cookies[i].split('=');
	const profileElement = document.getElementById('profile-' + key);
	if (profileElement != null) {
		profileElement.textContent = decodeURIComponent(value);
	}
}