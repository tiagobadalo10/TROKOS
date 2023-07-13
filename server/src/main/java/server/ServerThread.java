package server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*
 * Class of server threads
 * 
 * @author Tiago Badalo - fc55311
 * @author Duarte Costa - fc54441
 * @author Francisco Fiaes - fc53711
 *
 */

public class ServerThread extends Thread {

	private Socket socket;
	private InfoCatalog users;
	private BalanceCatalog balances;
	private GroupsCatalog groups;
	private UsersPayments payments;
	private Identificators ids;
	private Signature s;
	private PrivateKey privateKey;
	private MessageDigest digest;

	private static final float INIT_BALANCE = 100;
	private static final int HEIGHT = 200;
	private static final int WIDTH = 200;
	private TransactionsInfo transactionsInfo;
	private static final int MAX_TXS_NUMBER = 5;

	/**
	 * Creates new instance of ServerThread
	 */
	protected ServerThread(Socket inSoc, InfoCatalog users, BalanceCatalog balances, GroupsCatalog groups,
			UsersPayments payments, Identificators ids, PrivateKey privateKey,
			TransactionsInfo transactionsInfo) {

		socket = inSoc;

		this.users = users;
		this.balances = balances;
		this.groups = groups;
		this.payments = payments;
		this.ids = ids;
		this.transactionsInfo = transactionsInfo;
		this.privateKey = privateKey;

		try {
			this.digest = MessageDigest.getInstance("SHA-256");
			this.s = Signature.getInstance("MD5withRSA");
			this.s.initSign((PrivateKey) privateKey);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void run() {

		try {

			ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
			boolean authentication = false;

			try {

				String userID = (String) inStream.readObject();

				long nonce = new Random().nextLong();
				outStream.writeObject(nonce);

				if (!users.getInfo(userID).isPresent()) { // userID unknown
					outStream.writeObject("U");

					String clientNonceStr = (String) inStream.readObject();
					long clientNonce = Long.parseLong(clientNonceStr);

					byte signature[] = (byte[]) inStream.readObject();
					Certificate cer = (Certificate) inStream.readObject();
					byte[] certBytes = cer.getEncoded();

					File certificateFile = new File("./certificates/" + userID + ".cer");
					FileOutputStream outputStream = new FileOutputStream(certificateFile);
					outputStream.write(certBytes);
					outputStream.close();

					PublicKey publicKey = cer.getPublicKey();
					Signature s = Signature.getInstance("MD5withRSA");
					s.initVerify(publicKey);
					byte[] buf = clientNonceStr.getBytes();
					s.update(buf);

					if (s.verify(signature) && clientNonce == nonce) {
						this.users.putInfo(userID, userID + ".cer");
						this.balances.putInfo(userID, String.valueOf(INIT_BALANCE));

						this.users.putInfoFile();

						outStream.writeObject("Successful registration and authentication.");
						authentication = true;
					} else {
						outStream.writeObject("Unsuccessful registration and authentication.");
					}

				} else { // userID known
					outStream.writeObject("K");

					byte signature[] = (byte[]) inStream.readObject();

					Optional<String> certificateName = this.users.getInfo(userID);
					if (certificateName.isPresent()) {
						FileInputStream fis = new FileInputStream(
								"./certificates/" + certificateName.get());
						CertificateFactory cf = CertificateFactory.getInstance("X509");
						Certificate cer = cf.generateCertificate(fis);
						PublicKey publicKey = cer.getPublicKey();
						Signature s = Signature.getInstance("MD5withRSA");
						s.initVerify(publicKey);
						byte buf[] = String.valueOf(nonce).getBytes();
						s.update(buf);
						if (s.verify(signature)) {
							outStream.writeObject("Successful authentication.");
							authentication = true;
						} else {
							outStream.writeObject("Unsuccessful authentication.");
						}

					}
				}
				if (authentication) {
					outStream.writeObject("A"); // authenticated
					System.out.println("The Client " + userID + " has connected to the server.");
					if (!menuSelection(socket, inStream, outStream, userID)) {
						System.out.println("The Client " + userID + " has disconnected from the server.");
					}
				} else {
					outStream.writeObject("N"); // not
				}
			} catch (ClassNotFoundException | NoSuchAlgorithmException | InvalidKeyException | SignatureException
					| CertificateException e) {
				e.printStackTrace();
			}
			outStream.close();
			inStream.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Function that recieves the operations from the client
	 * 
	 * @param socket    Thread Socket
	 * @param inStream  Input Stream
	 * @param outStream Output Stream
	 * @param userID    user loggedin
	 */
	protected boolean menuSelection(Socket socket, ObjectInputStream inStream, ObjectOutputStream outStream,
			String userID) {

		try {
			String msg = (String) inStream.readObject();
			SignedObject signedTransaction;
			String val = null;
			while (!"quit".equals(msg)) {

				String[] messageSplited = msg.split(" ");

				switch (messageSplited[0]) {

					case "balance":
					case "b":
						outStream.writeObject(balance(userID));
						break;

					case "makepayment":
					case "m":

						signedTransaction = (SignedObject) inStream.readObject();

						val = makePayment(userID, msg.split(" ")[1], Float.parseFloat(msg.split(" ")[2]));

						outStream.writeObject(val);

						if (val.equals("The payment has been made."))
							addTransaction(signedTransaction);
						break;

					case "requestpayment":
					case "r":
						outStream.writeObject(
								requestPayment(userID, msg.split(" ")[1], Float.parseFloat(msg.split(" ")[2])));
						break;

					case "viewrequests":
					case "v":
						outStream.writeObject(viewRequests(userID));
						break;

					case "payrequest":
					case "p":

						signedTransaction = (SignedObject) inStream.readObject();

						val = payRequest(msg.split(" ")[1], userID);

						outStream.writeObject(val);

						if (val.equals("Payment has been made."))
							addTransaction(signedTransaction);

						break;

					case "obtainQRcode":
					case "o":
						File QRCode = obtainQRCode(userID, Float.parseFloat(messageSplited[1]));
						if (QRCode == null) {
							outStream.writeObject("Error creating QRCode");
						} else {
							outStream.writeObject(QRCode);
						}
						break;

					case "confirmQRcode":
					case "c":
						Optional<PaymentRequest> paymentOp = this.payments
								.getPayment(Integer.parseInt(messageSplited[1]));
						if (paymentOp.isPresent()) {
							outStream.writeObject("Payment request found.");

							PaymentRequest payment = paymentOp.get();
							Float amount = payment.getAmount();
							String receiver = payment.getUserID();
							outStream.writeObject(receiver);
							outStream.writeObject(amount);
							signedTransaction = (SignedObject) inStream.readObject();

							addTransaction(signedTransaction);

							outStream.writeObject(confirmQRcode(userID, Integer.parseInt(messageSplited[1])));
						} else {
							outStream.writeObject("There isnt a payment request associated to the request ID.");
						}

						break;

					case "newgroup":
					case "n":
						outStream.writeObject(newgroup(userID, messageSplited[1]));
						break;

					case "addu":
					case "a":
						outStream.writeObject(addu(messageSplited[1], messageSplited[2], userID));
						break;

					case "groups":
					case "g":
						outStream.writeObject(groups(userID));
						break;

					case "dividepayment":
					case "d":
						outStream.writeObject(
								dividePayment(messageSplited[1], Float.parseFloat(messageSplited[2]), userID));
						break;

					case "statuspayments":
					case "s":
						outStream.writeObject(statuspayments(messageSplited[1], userID));
						break;

					case "history":
					case "h":
						outStream.writeObject(history(messageSplited[1], userID));
						break;

					default:
						break;
				}

				msg = (String) inStream.readObject();
				if ("quit".equals(msg)) {
					socket.close();
					return false;
				}
			}
			return true;
		} catch (IOException | ClassNotFoundException e) {
			return false;
		}
	}

	/**
	 * Returns the userID's balance
	 * 
	 * @param userID String that identifies the user
	 * @return the user's balance
	 */
	protected float balance(String userID) {
		Optional<String> balance = balances.getInfo(userID);
		if (balance.isPresent()) {
			return Float.parseFloat(balance.get());
		}
		return -1;
	}

	/**
	 * Adds transaction to the current block.
	 * Signs Transaction when the number of transactions reaches de limit
	 * 
	 * @param signedTransaction Transaction to be added to the block
	 */
	protected void addTransaction(SignedObject signedTransaction) {
		transactionsInfo.addSignedObject(signedTransaction);
		try {
			// Create New Block
			if (transactionsInfo.getNumberOfTxs() == 1)
				createNewBlockFile();

			ObjectOutputStream outputStream = transactionsInfo.getOutPutStream();

			// Transaction
			outputStream.writeObject(serialize((String) signedTransaction.getObject().toString()));
			s.update(serialize((String) signedTransaction.getObject().toString()));
			digest.update(serialize((String) signedTransaction.getObject().toString()));

			// Signed Transaction
			byte[] signedTransactionBytes = serialize(signedTransaction);
			outputStream.writeObject(signedTransactionBytes);
			s.update(signedTransactionBytes);
			digest.update(signedTransactionBytes);

			if (transactionsInfo.getNumberOfTxs() == MAX_TXS_NUMBER) {
				transactionsInfo.resetTxs();

				outputStream.writeObject(s.sign()); // assinatura server
				digest.update(s.sign());

				outputStream.close();

				transactionsInfo.setCurrentHash(digest.digest());
			}

		} catch (IOException | SignatureException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates new block in blockchain.
	 * 
	 * @throws IOException
	 */
	protected void createNewBlockFile() throws IOException {

		try {
			this.s = Signature.getInstance("MD5withRSA");
			this.s.initSign((PrivateKey) privateKey);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			e.printStackTrace();
		}

		ids.updateCurrentBlock();

		long blockNumber = ids.getCurrentBlock();
		transactionsInfo.setCurrentFile(blockNumber);

		try {
			ObjectOutputStream outputStream = transactionsInfo.getOutPutStream();
			// Hash
			outputStream.writeObject(transactionsInfo.getCurrentHash());
			s.update(transactionsInfo.getCurrentHash());
			digest.update(transactionsInfo.getCurrentHash());
			// Block Number
			outputStream.writeObject(serialize(ids.getCurrentBlock()));
			s.update(serialize(ids.getCurrentBlock()));
			digest.update(serialize(ids.getCurrentBlock()));
			// Current Number of Txs
			outputStream.writeObject(serialize((long) MAX_TXS_NUMBER));
			s.update(serialize((long) transactionsInfo.getNumberOfTxs()));
			digest.update(serialize((long) transactionsInfo.getNumberOfTxs()));
		} catch (IOException | SignatureException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Converts an object to byte array.
	 * 
	 * @param obj Object to be converted
	 * @return byte[] array
	 * @throws IOException
	 */
	protected static byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(out);
		os.writeObject(obj);
		return out.toByteArray();
	}

	/**
	 * Function that pays a payment request associated to a user
	 * 
	 * @param clientID User who will pay to userID an amount
	 * @param userID   User who will receive an amount from clientID
	 * @param amount   amount that ClientID pays to userID
	 * @return Message to the client of the status of the operation
	 */
	protected String makePayment(String clientID, String userID, float amount) {
		Optional<String> balance = balances.getInfo(clientID);
		if (!users.containsID(userID)) {
			return "Unable to make a payment.\nThe receiver doesnt exist.";
		} else if (clientID.equals(userID)) {
			return "Unable to send money to yourself.";
		} else if (balance.isPresent()) {
			if (Float.parseFloat(balance.get()) < amount) {
				return "Unable to make a payment.\nYou dont have enough funds.";
			}
		} else if (!users.containsID(clientID)) {
			return "Unable to make a payment.\nYou are not recognized.";
		}

		try {

			Optional<String> balanceC = balances.getInfo(clientID);
			Optional<String> balanceU = balances.getInfo(userID);

			if (balanceC.isPresent()) {
				String clientAmount = Float.toString(Float.parseFloat(balanceC.get()) - amount);
				balances.putInfo(clientID, clientAmount);
			}

			if (balanceU.isPresent()) {
				String userAmount = Float.toString(Float.parseFloat(balanceU.get()) + amount);
				balances.putInfo(userID, userAmount);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "The payment has been made.";

	}

	/**
	 * Function that creates a payments request from a user to another
	 * 
	 * @param userID   User that created the payment request, current user
	 * @param clientID User that has received the payment request
	 * @param amount   of the payment
	 * @return Message to the client of the status of the operation
	 */
	protected String requestPayment(String userID, String clientID, float amount) {
		if (!users.containsID(clientID)) {
			return "User specified doesnt exist.";
		}
		if (userID.equals(clientID)) {
			return "Cannot make a request to yourself";
		}

		PaymentRequest newPayment = new PaymentRequest(userID, clientID, amount, ids.getRequestID());

		try {

			payments.addPayment(clientID, newPayment);

			ids.updateRequestID();

		} catch (Exception e) {
			e.printStackTrace();
			return "Error creating payment";
		}

		return "Request Payment has been made.";
	}

	/**
	 * Function that creates a visual representation of the current request payments
	 * that a user has
	 * 
	 * @param userID current user
	 * @return Message to the client with a visual representation of the payment
	 *         requests
	 */
	protected String viewRequests(String userID) {
		Optional<ArrayList<PaymentRequest>> userPayments = payments.userPayments(userID);

		if (!userPayments.isPresent() || userPayments.get().isEmpty()) {
			return "You dont have payment requests.\n";
		}

		StringBuilder sb = new StringBuilder();
		for (PaymentRequest paymentRequest : userPayments.get()) {
			if (paymentRequest.getClientID() != null) {
				sb.append("reqID: " + paymentRequest.getRequestID() + ", " + "Payment Creator: " +
						paymentRequest.getUserID() + ", " + "Amount: " + paymentRequest.getAmount() + "\n");
			}
		}
		return sb.toString();
	}

	/**
	 * Function that pays a payment request
	 * 
	 * @param reqID  identifier of the payment request
	 * @param userID current user
	 * @return Message to the client of the status of the operation
	 */
	protected String payRequest(String reqID, String userID) {
		Optional<PaymentRequest> pr = payments.getPayment(Integer.parseInt(reqID));
		if (!pr.isPresent()) {
			return "Payment request not found.";
		}
		if (balance(userID) < pr.get().getAmount()) {
			return "Balance is not enough.";
		}
		if (!pr.get().getClientID().equals(userID) || pr.get().getClientID() == null) {
			return "Payment is not available for you.";
		}
		Optional<Group> group = groups.getGroupByID(pr.get().getGroupID());

		if (group.isPresent()) {
			// atualiza os requests que são pagamentos de grupo
			group.get().payGroupRequest(reqID);
			// Write on file historyGroupsPayment
			Optional<Integer> op = group.get().getDividePaymentCounterByRequestId(reqID);
			if (op.isPresent()) {
				int checkID = op.get();
				Optional<Float> op1 = group.get().getAmountByDividePaymentCounter(checkID);
				if (op1.isPresent()) {
					float amount = op1.get();
					try {
						groups.addGroupPayment(group.get().getGroupID(), checkID, amount);

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		// atualiza esse payment do catalogo de payments
		makePayment(userID, pr.get().getUserID(), pr.get().getAmount());

		Optional<ArrayList<PaymentRequest>> userPayments = payments.userPayments(userID);
		if (!userPayments.isPresent()) {
			return "Payment request not found.";
		}
		userPayments.get().remove(pr.get());

		return "Payment has been made.";
	}

	/**
	 * Function that creates a QR Code that represents a payment request
	 * 
	 * @param userID current user
	 * @param amount of the payment request
	 * @return File image of the QR Code
	 */
	protected File obtainQRCode(String userID, float amount) {

		String data = String.valueOf(ids.getRequestID());
		String path = data + ".png";
		String charset = "UTF-8";

		try {
			MyQrCode.createQR(data, path, charset, HEIGHT, WIDTH);
			File f = new File(path);

			PaymentRequest pr = new PaymentRequest(userID, amount, f);

			payments.addPayment(userID, pr);

			ids.updateRequestID();
			return f;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Function that pays a payments request by QR Code
	 * 
	 * @param userID current user
	 * @param QRcode number of the payments request
	 * @return Message to the client of the status of the operation
	 */
	protected String confirmQRcode(String userID, int QRcode) {
		Optional<PaymentRequest> pr = payments.getPayment(QRcode);

		if (!pr.isPresent()) {
			return "The request identified by QR code does not exist.";
		} else {
			if (userID.equals(pr.get().getUserID())) {
				return "The request identified by QR code cannot be confirmed by the same client who created it.";
			}
			Optional<ArrayList<PaymentRequest>> userPayments = payments.userPayments(pr.get().getUserID());
			if (userPayments.isPresent()) {
				if (balance(userID) < pr.get().getAmount()) {
					userPayments.get().remove(pr.get());
					return "Insufficient funds.";
				}
				makePayment(userID, pr.get().getUserID(), pr.get().getAmount());
				userPayments.get().remove(pr.get());

			}
		}
		return "Sucessful Operation";
	}

	/**
	 * Function that creates a new group
	 * 
	 * @param userID  current user
	 * @param groupID group identification of the new group
	 * @return Message to the client of the status of the operation
	 */
	protected String newgroup(String userID, String groupID) {
		return groups.newGroup(userID, groupID);
	}

	/**
	 * Function that adds a user to a group
	 * 
	 * @param userID  current user
	 * @param groupID group identification
	 * @param owner   identification of the group
	 * @return Message to the client of the status of the operation
	 */
	protected String addu(String userID, String groupID, String owner) {

		if (!users.getInfo(userID).isPresent()) {
			return "The user you are trying to add doesnt exists.";
		}

		return groups.addU(userID, groupID, owner);
	}

	/**
	 * Function that show the groups of the current user
	 * 
	 * @param userID current user
	 * @return Message to the client of a visual representation of the groups
	 */
	protected String groups(String userID) {
		return groups.groups(userID);
	}

	/**
	 * Function that divides a payment within a group
	 * 
	 * @param groupID group identification
	 * @param amount  to be divided by the elements of the group
	 * @param userID  current user
	 * @return Message to the client of the status of the operation
	 */
	protected String dividePayment(String groupID, float amount, String userID) {
		Optional<Group> group = groups.getGroupByID(groupID);

		if (!group.isPresent()) {
			return "Cannot create a shared payment request.\n";
		}

		if (!group.get().getOwner().equals(userID)) {
			return "You are not the owner of the group.\n";
		}
		if (group.get().getMembers().size() == 0) {
			return "The group is empty, add members before dividing a payment.\n";
		}

		ArrayList<PaymentRequest> groupPayment = new ArrayList<>();
		float amountAvg = group.get().getDivideAmount(amount);
		for (String member : group.get().getMembers()) {
			PaymentRequest pr = new PaymentRequest(userID, member, amountAvg, groupID,
					ids.getRequestID(), ids.getCheckID());

			ids.updateRequestID();

			try {
				payments.addPayment(member, pr);
			} catch (Exception e) {
				e.printStackTrace();
			}
			groupPayment.add(pr);
		}

		group.get().addPaymentToGroup(groupPayment);
		ids.updateCheckID();
		return "Group " + groupID + "'s has now a shared payment request \n";
	}

	/**
	 * Status of all payments of a group
	 * 
	 * @param groupID group identification
	 * @param userID  current user
	 * @return Message to the client of a visual representation of the payments to
	 *         be maid
	 */
	protected String statuspayments(String groupID, String userID) {
		Optional<Group> group = groups.getGroupByID(groupID);
		if (!group.isPresent()) {
			return "This group does not exist, therefore it hasn't payments.\n";
		}

		if (!group.get().getOwner().equals(userID)) {
			return "You're not the owner of the group.\n";
		}

		return group.get().getStatus();
	}

	/**
	 * Function that shows payments of a group
	 * 
	 * @param groupID      group identification
	 * @param userIDuserID current user
	 * @return Message to the client of a visual representation of the payments made
	 *         by a group
	 */
	protected String history(String groupID, String userID) {
		Optional<Group> group = groups.getGroupByID(groupID);
		if (!group.isPresent()) {
			return "This group does not exist, therefore it hasn't a history.\n";
		}
		if (!group.get().getOwner().equals(userID)) {
			return "You're not the owner of the group.\n";
		}

		// Returns list with dividePaymentCounter : amount
		List<String> history = groups.getHistory(groupID);

		StringBuilder sb = new StringBuilder("History of payed payments of Group " + groupID + ":\n");

		for (String payment : history) {
			sb.append("-> Check " + payment.split(":")[0] + " of " + payment.split(":")[1]
					+ " euros is payed by every member.\n");
		}

		if (sb.toString().equals("History of payed payments of Group " + groupID + ":\n")) {
			return "Group " + groupID + "'s payments arent paid yet, so it hasnt a history!\n";
		}

		return sb.toString();
	}
}
