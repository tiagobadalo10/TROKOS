package server;

import java.io.File;

/*
 * Class that handles the id associated with each payment request
 * 
 * @author Tiago Badalo - fc55311
 * @author Duarte Costa - fc54441
 * @author Francisco Fiaes - fc53711
 *
 */

public class Identificators {

	private File f;
	private int requestID;
	private int checkID;
	private Cypher c;
	private long currentBlock;

	/**
	 * Creates a file with the values of checkID and requestID, to be used by the
	 * system
	 * 
	 * @param f File to be written
	 */
	protected Identificators(File f, Cypher c) {
		this.f = f;
		this.requestID = 1;
		this.checkID = 1;
		this.c = c;
		this.currentBlock = 0;
	}

	/**
	 * Get checkID and requestID form the file
	 */
	protected void getIDsFromFile() {
		String data = c.decryptDataFile(f, "ids");
		if (data != null && data.length() != 0) {
			String[] ids = data.split(":");
			this.requestID = Integer.parseInt(ids[0]);
			this.checkID = Integer.parseInt(ids[1]);
			String currentBlock = ids[2].replaceAll("[^0-9]", "");
			this.currentBlock = Long.parseLong(currentBlock);
		}
	}

	/**
	 * Updates the values of the requestID
	 * 
	 */
	protected void updateRequestID() {
		this.requestID = this.requestID + 1;
		c.encryptDataFile(f, this.requestID + ":" + this.checkID + ":" + this.currentBlock + "\n", "ids");
	}

	/**
	 * Updates the value of checkID
	 * 
	 */
	protected void updateCheckID() {
		this.checkID = this.checkID + 1;
		c.encryptDataFile(f, this.requestID + ":" + this.checkID + ":" + this.currentBlock + "\n", "ids");
	}

	/**
	 * Get of the current requestID
	 * 
	 * @return requestID
	 */
	protected int getRequestID() {
		return this.requestID;
	}

	/**
	 * Gets the current checkID
	 * 
	 * @return checkID
	 */
	protected int getCheckID() {
		return this.checkID;
	}

	/**
	 * Gets the current block identificator
	 * 
	 * @return Block identificator
	 */
	protected long getCurrentBlock() {
		return this.currentBlock;
	}

	/**
	 * Updates the current block identificator and encrypts new data
	 */
	protected void updateCurrentBlock() {
		this.currentBlock = this.currentBlock + 1;
		c.encryptDataFile(f, this.requestID + ":" + this.checkID + ":" + this.currentBlock + "\n", "ids");
	}

}
