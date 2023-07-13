package server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.SignedObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that manages transactions of block chain across all threads
 */
public class TransactionsInfo {

	private static TransactionsInfo transactionsInfo = null;
	private List<SignedObject> signedTxs;
	private byte[] currentHash;
	private File currentFile;
	private FileOutputStream fos;
	private ObjectOutputStream outputStream;

	/**
	 * Creates an instance of Transaction Info
	 */
	public TransactionsInfo() {
		this.signedTxs = new ArrayList<>();
		this.currentHash = new byte[32];
	}

	/**
	 * Singleton
	 * 
	 * @return the same instance of the Object
	 */
	public static TransactionsInfo getInstance() {
		if (transactionsInfo == null)
			transactionsInfo = new TransactionsInfo();
		return transactionsInfo;
	}

	/**
	 * Gets the object that writes on block files
	 * 
	 * @return ObjectOutputStream
	 */
	public ObjectOutputStream getOutPutStream() {
		return this.outputStream;
	}

	/**
	 * Changes the current block file to be written
	 * 
	 * @param blockNumber Block identificator
	 * @throws IOException
	 */
	public void setCurrentFile(long blockNumber) throws IOException {
		this.currentFile = new File("./log/block_" + blockNumber + ".blk");
		if (this.currentFile.createNewFile()) {
			this.fos = new FileOutputStream(this.currentFile);
			this.outputStream = new ObjectOutputStream(fos);
		}

	}

	/**
	 * Returns the current block file
	 * 
	 * @return File
	 */
	public File getCurrentFile() {
		return this.currentFile;
	}

	/**
	 * Adds a new signed transaction
	 * 
	 * @param signedObject Transaction signed
	 */
	public void addSignedObject(SignedObject signedObject) {
		signedTxs.add(signedObject);
	}

	/**
	 * Gets the current number of transaction in the current block
	 * 
	 * @return
	 */
	public int getNumberOfTxs() {
		return signedTxs.size();
	}

	/**
	 * Called when a new block is created
	 */
	public void resetTxs() {
		signedTxs = new ArrayList<>();
	}

	/**
	 * Returns the hash of the current block
	 * 
	 * @return
	 */
	public byte[] getCurrentHash() {
		return currentHash;
	}

	/**
	 * Sets the hash of the current block
	 * 
	 * @param hash
	 */
	public void setCurrentHash(byte[] hash) {
		this.currentHash = hash;
	}
}
