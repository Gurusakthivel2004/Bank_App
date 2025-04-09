const submitFd = async _ => {
	const accountNumber = document.getElementById('fdAccount').value;
	const amount = document.getElementById('fdAmount').value;
	const duration = document.getElementById('fdDuration').value;
	const errorMessage = document.getElementById('fdErrorMessage');

	if (amount == null || !Number.isInteger(Number(amount)) || Number(amount) <= 10000) {
		errorMessage.style.display = 'block';
		errorMessage.innerHTML = "Please enter an amount of ₹10,000 or more."
		return;
	} else {
		errorMessage.style.display = 'none';
	}
	const fdRequest = {
		accountNumber: accountNumber,
		amount: amount,
		duration: duration
	}
	console.log(fdRequest);
	const response = await fetch('/Bank_Application/api/FixedDeposit', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(fdRequest)
	});
	const result = await response.json();
	if (result.message.includes('Session expired') || result.message == 'Invalid Access token') {
		document.querySelector('body').style.display = 'none';
		deleteAllCookies();
		window.location.href = "error.html";
	}
	if (result.message == 'success') {
		toggleModal('fixedDepositModel');
		document.getElementById('successMessage').innerHTML = "Amount deposited!";
		document.getElementById('successModal').style.display = 'flex';
	} else {
		errorMessage.style.display = "block";
		errorMessage.textContent = result.message;
	}
}