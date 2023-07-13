package server;

import java.util.ArrayList;
/**
 * Class that stores information about the groups of each user
 * 
 * @author Tiago Badalo - fc55311
 * @author Duarte Costa - fc54441
 * @author Francisco Fiaes - fc53711
 *
 */
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

public class GroupsCatalog {

	private HashMap<String, Group> groups;
	// <"checkID:amount", groupId>
	private HashMap<String, String> paymentsDone;

	/**
	 * Creates Groups Catalog
	 */
	protected GroupsCatalog() {
		this.groups = new HashMap<>();
		this.paymentsDone = new HashMap<>();
	}

	/**
	 * Adds new group payment
	 * 
	 * @param groupId Group identificator
	 * @param checkID Identificator of the group payment
	 * @param amount  Amount to be payed
	 */
	public void addGroupPayment(String groupId, int checkID, Float amount) {
		if (!paymentsDone.keySet().contains(checkID + ":" + amount))
			paymentsDone.put(checkID + ":" + amount, groupId);
	}

	/**
	 * Read from history file the payments done
	 * 
	 * @param groupID ID associated to the group
	 * @return A list of Strings containing checkID : amount
	 */
	protected List<String> getHistory(String groupID) {
		List<String> history = new ArrayList<>();

		for (String key : paymentsDone.keySet()) {
			if (groupID.equals(paymentsDone.get(key))) {
				String[] info = key.split(":");
				history.add(info[0] + ":" + info[1]);
			}
		}
		return history;
	}

	/**
	 * Create a new group
	 * 
	 * @param owner   Owner of group
	 * @param groupID ID associated to the group
	 * @return message if group has been created or not
	 */

	protected String newGroup(String owner, String groupID) {

		for (Entry<String, Group> group : groups.entrySet()) {
			if (group.getValue().getGroupID().equals(groupID)) {
				return "Group has not been created.";
			}
		}
		Group group = new Group(groupID, owner);
		groups.put(groupID, group);

		return "Group has been created.";
	}

	/**
	 * Add a user to the group
	 * 
	 * @param userID  User of Trokos
	 * @param groupID ID associated to the group
	 * @param owner   Owner of group
	 * @return message if owner could add a user to the group or not
	 */

	protected String addU(String userID, String groupID, String owner) {
		for (Entry<String, Group> group : groups.entrySet()) {
			if (group.getValue().getOwner().equals(owner) && group.getValue().getGroupID().equals(groupID)
					&& !group.getValue().getOwner().equals(userID)) {

				String msg = group.getValue().addMember(userID);

				return msg;
			}
		}

		return "User didn't enter the group!";
	}

	/**
	 * Visual representation of the userID participation in groups, divided by
	 * member and owner
	 * 
	 * @param userID User of Trokos
	 * @return A string to be printed
	 */

	protected String groups(String userID) {
		StringBuilder owner = new StringBuilder();
		StringBuilder members = new StringBuilder();
		owner.append("Owner:\n");
		members.append("Members:\n");
		for (Entry<String, Group> group : groups.entrySet()) {
			if (group.getValue().getOwner().equals(userID)) {
				owner.append("-> " + group.getValue().getGroupID() + "\n");
			} else {
				if (group.getValue().getMembers().contains(userID)) {
					members.append("-> " + group.getValue().getGroupID() + "\n");
				}
			}
		}
		return (owner.toString() + "\n" + members.toString());
	}

	/**
	 * Returns the a group information by groupID
	 * 
	 * @param groupID ID associated to the group
	 * @return Group instance
	 */
	protected Optional<Group> getGroupByID(String groupID) {
		Optional<Group> group = Optional.empty();
		for (Entry<String, Group> g : groups.entrySet()) {
			if (g.getValue().getGroupID().equals(groupID)) {
				group = Optional.of(g.getValue());
			}
		}
		return group;
	}
}
