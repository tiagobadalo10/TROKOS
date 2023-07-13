package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/*
 * Class representing a group for shared payments
 * 
 * @author Tiago Badalo - fc55311
 * @author Duarte Costa - fc54441
 * @author Francisco Fiaes - fc53711
 *
 */

public class Group {

	private String groupID;
	private String owner;
	private List<String> members;
	private HashMap<Integer, List<PaymentRequest>> groupRequests;

	protected Group(String groupID, String owner) {
		this.groupID = groupID;
		this.owner = owner;
		this.members = new ArrayList<>();
		this.groupRequests = new HashMap<>();
	}

	/**
	 * 
	 * @return owner of group
	 */
	protected String getOwner() {
		return owner;
	}

	/**
	 * Add a new member to the group
	 * 
	 * @param userID User of Trokos
	 * @return message if owner could add the userID into the group or not
	 */
	protected String addMember(String userID) {
		if (members.contains(userID)) {
			return userID + " already is part of the group.";
		}
		members.add(userID);
		return userID + " is now part of your group " + groupID;
	}

	/**
	 * Calculate the amount that each member of group should pay in shared payment
	 * request
	 * 
	 * @param amount Total amount of shared payment request
	 * @return amount for each member of group
	 */
	protected float getDivideAmount(float amount) {
		if (this.members.isEmpty()) {
			return (float) 0.0;
		}
		return amount / this.members.size();
	}

	/**
	 * Associate one payment request to the group payment request
	 * 
	 * @param listPayments List of payments associated to one group payment request
	 * @return dividePaymentCounter
	 */
	protected String addPaymentToGroup(List<PaymentRequest> lp) {
		groupRequests.put(lp.get(0).getCheckID(), lp);
		return "Payment added to the Group Payment\n";
	}

	/**
	 * Check status of all group payment requests
	 * 
	 * @return status Status of group payment requests
	 */
	protected String getStatus() {
		if (groupRequests.values().size() == 0)
			return "There isn't Group Payments in Group " + groupID + " yet.\n";

		StringBuilder sb = new StringBuilder("Status on the Group " + groupID + "'s Payments:\n\n");
		ArrayList<String> nonPaid = new ArrayList<>();
		int checkID = 0;
		for (List<PaymentRequest> lp : groupRequests.values()) {
			for (PaymentRequest p : lp) {
				if (p.getStatus() == Status.PENDING) {
					nonPaid.add(p.getClientID());
				}
				checkID = p.getCheckID();
			}

			if (!nonPaid.isEmpty()) {
				sb.append("Check " + checkID + ":\n");
				for (String user : nonPaid) {
					sb.append("-> " + user + " didn't pay yet!\n");
				}
				nonPaid.clear();
			}
		}
		if (sb.toString().equals("Status on the Group " + groupID + "'s Payments:\n\n")) {
			return "All " + groupID + "'s payments are paid to " + owner + "\n";
		}
		return sb.toString();
	}

	/**
	 * Gets the current group ID
	 * 
	 * @return groupID associated to group
	 */
	protected String getGroupID() {
		return this.groupID;
	}

	/**
	 * Gets the members of the current group
	 * 
	 * @return members associated to the group
	 */
	protected List<String> getMembers() {
		List<String> membersG = new ArrayList<>();
		for (String member : members) {
			membersG.add(member);
		}

		return membersG;
	}

	/**
	 * Change status of all payment requests associated to a shared payment request
	 * 
	 * @param reqID
	 */
	protected void payGroupRequest(String reqID) {
		for (List<PaymentRequest> lp : groupRequests.values()) {
			for (PaymentRequest p : lp) {
				if (String.valueOf(p.getRequestID()).equals(reqID)) {
					p.setStatus(Status.FINISHED);
				}
			}
		}
	}

	/**
	 * Returns the number of the current divide payment has been mada on a group, by
	 * request id.
	 * 
	 * @param reqID
	 * @return int DividePaymentCounter
	 */
	protected Optional<Integer> getDividePaymentCounterByRequestId(String reqID) {
		for (Map.Entry<Integer, List<PaymentRequest>> entry : groupRequests.entrySet()) {
			for (PaymentRequest p : entry.getValue()) {
				if (String.valueOf(p.getRequestID()).equals(reqID)) {
					return Optional.of(entry.getKey());
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * Get amount of a dividePayment that was made
	 * 
	 * @param dividePaymentCounter
	 * @return amount that was divided
	 */
	protected Optional<Float> getAmountByDividePaymentCounter(Integer dividePaymentCounter) {
		List<PaymentRequest> payments = groupRequests.get(dividePaymentCounter);
		return Optional.of(payments.get(0).getAmount() * payments.size());
	}

	/**
	 * Adds a PaymentRequest the list of PaymentRequests associated to a checkID in
	 * groupRequests
	 * 
	 * @param p PaymentRequest that is added to the list of PaymentRequests
	 *          associated to a checkID in groupRequests
	 */
	protected void fillPayment(PaymentRequest p) {
		if (this.groupRequests.keySet().contains(p.getCheckID())) {
			this.groupRequests.get(p.getCheckID()).add(p);
		} else {
			List<PaymentRequest> newList = new ArrayList<>();
			newList.add(p);
			this.groupRequests.put(p.getCheckID(), newList);
		}
	}
}
