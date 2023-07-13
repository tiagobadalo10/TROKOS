package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

/*
 * Class that stores information about users payments
 * 
 * @author Tiago Badalo - fc55311
 * @author Duarte Costa - fc54441
 * @author Francisco Fiaes - fc53711
 *
 */

public class UsersPayments {

	private HashMap<String, ArrayList<PaymentRequest>> payments;

	public UsersPayments() {
		payments = new HashMap<>();

	}

	/**
	 * Get pending payment request associated to requestID
	 * 
	 * @param requestID ID associated to a pending payment request
	 * @return Pending payment request or empty
	 */

	protected Optional<PaymentRequest> getPayment(int requestID) {

		Optional<PaymentRequest> op = Optional.empty();

		for (ArrayList<PaymentRequest> listPayments : payments.values()) {
			for (PaymentRequest pr : listPayments) {
				if (pr.getRequestID() == requestID) {
					op = Optional.of(pr);
				}
			}
		}

		return op;
	}

	/**
	 * Get list of all pending payment requests associated to clientID
	 * 
	 * @param clientID User who will pay the request
	 * @return List of all pending payment requests associated to clientID or empty
	 */

	protected Optional<ArrayList<PaymentRequest>> userPayments(String clientID) {

		return Optional.ofNullable(payments.get(clientID));
	}

	/**
	 * Add a payment request associated to clientID
	 * 
	 * @param clientID receiver
	 * @param pr       payment request
	 */
	protected void addPayment(String clientID, PaymentRequest pr) {
		if (payments.get(clientID) == null) {
			ArrayList<PaymentRequest> paymentsUser = new ArrayList<>();
			paymentsUser.add(pr);
			payments.put(clientID, paymentsUser);
		} else {
			payments.get(clientID).add(pr);
		}
	}

}
