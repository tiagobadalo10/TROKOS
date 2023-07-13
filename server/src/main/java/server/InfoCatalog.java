package server;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Optional;

/*
 * Class that stores information about users
 * 
 * @author Tiago Badalo - fc55311
 * @author Duarte Costa - fc54441
 * @author Francisco Fiaes - fc53711
 *
 */

public class InfoCatalog {

	private HashMap<String, String> users = new HashMap<>();
	private File f;
	private Cypher c;

	protected InfoCatalog(File f, Cypher c) {
		this.f = f;
		this.c = c;
	}

	/**
	 * Add into the file the registration data associated to the user
	 * 
	 */

	protected void putInfoFile() {
		String localData = getDataMemory();
		c.encryptDataFile(f, localData, "users");
	}

	/**
	 * Add new data into the local data structure
	 * 
	 * @param userID User of Trokos
	 * @param users  New users of userID
	 */

	protected void putInfo(String userID, String certificateName) {
		this.users.put(userID, certificateName);
	}

	/**
	 * Gets users associated to one user from the local data structure
	 * 
	 * @param userID User of Trokos
	 * @return users associated to userID
	 */
	protected Optional<String> getInfo(String userID) {
		Optional<String> op = Optional.empty();
		if (users.get(userID) == null) {
			return op;
		}
		return Optional.of(users.get(userID));
	}

	/**
	 * Check if userID is already a user of Trokos
	 * 
	 * @param userID User of Trokos
	 * @return true if server already has users about userID, otherwise false
	 */

	protected boolean containsID(String userID) {
		return this.users.containsKey(userID);
	}

	/**
	 * Gets users of each user from file and saves it on local data structure
	 * 
	 */
	protected void getInfoFromFile() {
		String data = c.decryptDataFile(f, "users");
		if (data == null || data.length() == 0)
			return;

		String[] informationRaw = data.split("\n");
		String[] information = Arrays.copyOf(informationRaw, informationRaw.length - 1);

		for (String d : information) {
			if (d.length() != 0 && !d.equals("\n")) {
				String[] str = d.split(":");
				users.put(str[0], str[1]);
			}
		}

	}

	/**
	 * Converts Hashmap to a String
	 * 
	 * @return String representation of the hashmap
	 */
	protected String getDataMemory() {
		String data = "";
		for (Entry<String, String> entry : users.entrySet()) {
			data += entry.getKey() + ":" + entry.getValue() + "\n";
		}

		return data;
	}

}
