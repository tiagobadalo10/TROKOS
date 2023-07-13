package server;

import java.util.HashMap;
import java.util.Optional;

/*
 * Class that stores balances
 * 
 * @author Tiago Badalo - fc55311
 * @author Duarte Costa - fc54441
 * @author Francisco Fiaes - fc53711
 * 
 */
public class BalanceCatalog {
	private HashMap<String, String> balances;

	/**
	 * Creates instance of Balance Catalog
	 */
	public BalanceCatalog() {
		this.balances = new HashMap<>();
	}

	/**
	 * Adds balance to a user
	 * 
	 * @param userID Username
	 * @param amount User's Balance
	 */
	protected void putInfo(String userID, String amount) {
		this.balances.put(userID, amount);
	}

	/**
	 * Gets the balance of a user
	 * 
	 * @param userID Username
	 * @return Balance of a user
	 */
	protected Optional<String> getInfo(String userID) {
		Optional<String> op = Optional.empty();
		if (balances.get(userID) == null) {
			return op;
		}
		return Optional.of(balances.get(userID));
	}
}
