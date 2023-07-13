package client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Scanner;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/*
 * Class of client
 * 
 * @author Tiago Badalo - fc55311
 * @author Duarte Costa - fc54441
 * @author Francisco Fiaes - fc53711
 *
 */
public class Trokos {

	private Scanner sc = new Scanner(System.in);

	private String[] serverInfo = new String[2];
	private String truststore;
	private String keystore;
	private String passwordKeystore;
	private String userID;

	private PrivateKey privateKey;

	public static final int NUMBER_OF_ARGS = 5;

	/**
	 * Initializes a client Trokos with the server's address and his id
	 * but no password so he will do the register process
	 * 
	 * @param serverAddress the address for the connection to the server
	 * @param userID        his id for the login/register
	 */
	public Trokos(String serverAddress, String truststore, String keystore, String passwordKeystore, String userID) {
		portCheck(serverAddress);
		this.truststore = truststore;
		this.keystore = keystore;
		this.passwordKeystore = passwordKeystore;
		this.userID = userID;
	}

	public static void main(String[] args) {

		Trokos client = null;

		if (args.length == NUMBER_OF_ARGS) {
			client = new Trokos(args[0], "./truststore/" + args[1], "./client-keystores/" + args[2], args[3], args[4]);
		}

		System.setProperty("javax.net.ssl.trustStore", "./truststore/" + args[1]);

		client.startClient();
	}

	/**
	 * Starts the connection of this Client to the server, by opening the socket
	 * and sending the userID and the password to the server for the login/register
	 * processes
	 */
	public void startClient() {
		SocketFactory factory = SSLSocketFactory.getDefault();
		SSLSocket cSoc = null;

		try {
			cSoc = (SSLSocket) factory.createSocket(this.serverInfo[0], Integer.parseInt(this.serverInfo[1]));

			System.out.println("\nClient has connected to the server.");

			ObjectInputStream in = new ObjectInputStream(cSoc.getInputStream());
			ObjectOutputStream out = new ObjectOutputStream(cSoc.getOutputStream());

			out.writeObject(userID);

			long nonce = (long) in.readObject();
			String flag = (String) in.readObject();

			String str = String.valueOf(nonce);

			if ("U".equals(flag)) {

				FileInputStream fis = new FileInputStream(keystore);
				KeyStore ks = KeyStore.getInstance("JCEKS");
				ks.load(fis, this.passwordKeystore.toCharArray());
				Certificate certificate = ks.getCertificate(userID);
				this.privateKey = (PrivateKey) ks.getKey(userID, this.passwordKeystore.toCharArray());

				Signature s = Signature.getInstance("MD5withRSA");
				s.initSign((PrivateKey) privateKey);
				byte buf[] = str.getBytes();
				s.update(buf);

				out.writeObject(str);
				out.writeObject(s.sign());
				out.writeObject(certificate);

				fis.close();
			}

			if ("K".equals(flag)) {

				FileInputStream fis = new FileInputStream(keystore);
				KeyStore ks = KeyStore.getInstance("JCEKS");
				ks.load(fis, this.passwordKeystore.toCharArray());
				this.privateKey = (PrivateKey) ks.getKey(userID, this.passwordKeystore.toCharArray());

				Signature s = Signature.getInstance("MD5withRSA");
				s.initSign((PrivateKey) privateKey);
				byte buf[] = str.getBytes();
				s.update(buf);

				out.writeObject(s.sign());

				fis.close();
			}

			String authenticationMsg = (String) in.readObject();
			System.out.println(authenticationMsg);

			String authentication = (String) in.readObject();

			if ("A".equals(authentication)) {
				showMenu();
				System.out.print("Choose an option:\n");
				menuSelection(in, out);
			}

		} catch (IOException | ClassNotFoundException | KeyStoreException | NoSuchAlgorithmException
				| CertificateException | UnrecoverableKeyException | InvalidKeyException | SignatureException e) {
			System.err.println(e.getMessage());
		} finally {
			sc.close();
		}
		try {
			cSoc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Checks if the input from the user is a valid command for
	 * balance operation in the server
	 * 
	 * @param choice user's input command
	 * @return balance command for the server
	 */
	private static String balanceC(String choice) {
		String[] option = choice.split(" ");
		if (option.length != 1)
			return "Use: balance\n" + "Example: balance\n";
		else
			return "balance";
	}

	/**
	 * Checks if the input from the user is a valid command for
	 * makePayment operation in the server
	 * 
	 * @param choice user's input command
	 * @return makePayment command for the server
	 */
	private static String makePaymentC(String choice) {
		String[] option = choice.split(" ");
		if (option.length != 3) {
			return "Use: makepayment <userID> <amount>\n" + "Example: makepayment joao1 10.0\n";
		} else {
			if (Float.parseFloat(option[2]) <= 0.0) {
				return "Invalid Amount.\n";
			}
		}
		return "makepayment";
	}

	/**
	 * Checks if the input from the user is a valid command for
	 * requestPayment operation in the server
	 * 
	 * @param choice user's input command
	 * @return requestPayment command for the server
	 */
	private static String requestPaymentC(String choice) {
		String[] option = choice.split(" ");
		if (option.length != 3) {
			return "Use: requestpayment <userID> <amount>\n" + "Example: requestpayment joao1 10.0\n";
		} else {
			if (Float.parseFloat(option[2]) <= 0.0) {
				return "Invalid Amount.\n";
			}
		}
		return "requestpayment";
	}

	/**
	 * Checks if the input from the user is a valid command for
	 * payRequest operation in the server
	 * 
	 * @param choice user's input command
	 * @return payRequest command for the server
	 */
	private static String payRequestC(String choice) {
		String[] option = choice.split(" ");
		if (option.length != 2) {
			return "Use: payrequest <reqID>\n" + "Example: payrequest 1\n";
		} else {
			return "payrequest";
		}
	}

	/**
	 * Checks if the input from the user is a valid command for
	 * obtainQRCode operation in the server
	 * 
	 * @param choice user's input command
	 * @return obtainQRCode command for the server
	 */
	private static String obtainQRCodeC(String choice) {
		String[] option = choice.split(" ");
		if (option.length != 2) {
			return "Use: obtainQRcode <amount>\n" + "Example: obtainQRcode 10.0\n";
		} else {
			if (Float.parseFloat(option[1]) <= 0.0) {
				return "Invalid Amount.\n";
			}
		}
		return "obtainQRcode";
	}

	/**
	 * Checks if the input from the user is a valid command for
	 * confirmQRCode operation in the server
	 * 
	 * @param choice user's input command
	 * @return confirmQRCode command for the server
	 */
	private static String confirmQRCodeC(String choice) {
		String[] option = choice.split(" ");
		if (option.length != 2) {
			return "Use: confirmQRcode <QRcode>\n" + "Example: confirmQRcode 1\n";
		} else {
			if (Integer.parseInt(option[1]) <= 0) {
				return "Invalid QRcode.\n";
			}
		}
		return "confirmQRcode";
	}

	/**
	 * Checks if the input from the user is a valid command for
	 * newGroup operation in the server
	 * 
	 * @param choice user's input command
	 * @return newGroup command for the server
	 */
	private static String newGroupC(String choice) {
		String[] option = choice.split(" ");
		if (option.length != 2) {
			return "Use: newgroup <groupID>\n" + "Example: newgroup 1\n";
		} else {
			return "newgroup";
		}
	}

	/**
	 * Checks if the input from the user is a valid command for
	 * addu operation in the server
	 * 
	 * @param choice user's input command
	 * @return addu User To Group command for the server
	 */
	private static String addUC(String choice) {
		String[] option = choice.split(" ");
		if (option.length != 3) {
			return "Use: addu <userID> <groupID>\n" + "Example: addu joao1 1\n";
		} else {
			return "addu";
		}
	}

	/**
	 * Checks if the input from the user is a valid command for
	 * dividePayment operation in the server
	 * 
	 * @param choice user's input command
	 * @return dividePayment command for the server
	 */
	private static String dividePaymentC(String choice) {
		String[] option = choice.split(" ");
		if (option.length != 3) {
			return "Use: dividepayment <groupID> <amount>\n" + "Example: dividepayment 1 1.0\n";
		} else {
			try {
				if (Float.parseFloat(option[2]) <= 0.0) {
					return "Invalid Amount.\n";
				}
			} catch (NumberFormatException e) {
				return "Use: dividepayment <groupID> <amount>\n" + "Example: dividepayment 1 1.0\n";
			}
		}
		return "dividepayment";
	}

	/**
	 * Checks if the input from the user is a valid command for
	 * statusPayment operation in the server
	 * 
	 * @param choice user's input command
	 * @return statusPayment command for the server
	 */
	private static String statusPaymentsC(String choice) {
		String[] option = choice.split(" ");
		if (option.length != 2) {
			return "Use: statuspayments <groupID>\n" + "Example: statuspayments 1\n";
		} else {
			return "statuspayments";
		}
	}

	/**
	 * Checks if the input from the user is a valid command for
	 * history operation in the server
	 * 
	 * @param choice user's input command
	 * @return history command for the server
	 */
	private static String history(String choice) {
		String[] option = choice.split(" ");
		if (option.length != 2) {
			return "Use: history <groupID>\n" + "Example: history 1\n";
		} else {
			return "history";
		}
	}

	/**
	 * Checks if the serverAddress has a port
	 * otherwise its sets by default to 45678
	 * 
	 * @param serverAddress server address connection
	 */
	private void portCheck(String serverAddress) {
		String[] str = serverAddress.split(":");
		this.serverInfo[0] = str[0];
		if (str.length != 2) {
			this.serverInfo[1] = "45678";
		} else {
			this.serverInfo[1] = str[1];
		}
	}

	/**
	 * Prints the menu for the user in the console
	 */
	private static void showMenu() {
		StringBuilder sb = new StringBuilder();
		String[] functionalities = { "balance", "makepayment <userID> <amount>",
				"requestpayment <userID> <amount>", "viewrequests", "payrequest <reqID>",
				"obtainQRcode <amount>", "confirmQRcode <QRcode>", "newgroup <groupID>",
				"addu <userID> <groupID>", "groups", "dividepayment <groupID> <amount>",
				"statuspayments <groupID>", "history <groupID>", "quit" };
		sb.append("\n--------------------------------------\n");
		sb.append("                OPTIONS\n");
		sb.append("--------------------------------------\n\n");
		for (int i = 0; i < functionalities.length; i++) {
			sb.append(" | " + functionalities[i] + " \n");
		}
		System.out.println(sb.toString());
	}

	/**
	 * Gets the user's input and commands, and sends the command to the server
	 * for it do the respective operations, and it gives a feedback to the user
	 * 
	 * @param in  stream of data connected to the server that receives data/feedback
	 * @param out stream of data connected to the server that sends commands and
	 *            args
	 */
	private void menuSelection(ObjectInputStream in, ObjectOutputStream out) {
		try {
			String choice = sc.nextLine();
			String msg;
			while (!"quit".equals(choice) && !"q".equals(choice)) {
				switch (choice.split(" ")[0]) {

					case "balance":
					case "b":
						msg = balanceC(choice);
						if (!"balance".equals(msg)) {
							System.out.print(msg);
						} else {
							out.writeObject(choice);
							System.out.println(in.readObject());
						}
						break;

					case "makepayment":
					case "m":
						msg = makePaymentC(choice);
						if (!"makepayment".equals(msg)) {
							System.out.print(msg);
						} else {
							out.writeObject(choice);

							try {
								String[] values = choice.split(" ");
								Transaction transaction = new Transaction(userID, values[1],
										Float.parseFloat(values[2]));
								SignedObject signedTransaction = new SignedObject(transaction, this.privateKey,
										Signature.getInstance("MD5withRSA"));
								out.writeObject(signedTransaction);

							} catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
								e.printStackTrace();
							}

							msg = (String) in.readObject();
							System.out.println(msg);
						}
						break;

					case "requestpayment":
					case "r":
						msg = requestPaymentC(choice);
						if (!"requestpayment".equals(msg)) {
							System.out.print(msg);
						} else {
							out.writeObject(choice);
							msg = (String) in.readObject();
							System.out.println(msg);
						}
						break;

					case "viewrequests":
					case "v":
						out.writeObject(choice);
						msg = (String) in.readObject();
						System.out.print(msg);
						break;

					case "payrequest":
					case "p":

						msg = payRequestC(choice);
						if (!"payrequest".equals(msg)) {
							System.out.print(msg);
						} else {

							out.writeObject("v");

							String requestsRaw = (String) in.readObject();

							if (!"You dont have payment requests.\n".equals(requestsRaw) && !"".equals(requestsRaw)) {
								String requests[] = requestsRaw.split("\n");

								out.writeObject(choice);

								int i = 0;
								boolean found = false;
								while (i < requests.length && !found) {
									String[] request = requests[i].split(",");
									String reqID = request[0].replaceAll("[^0-9]", "");
									if (choice.split(" ")[1].equals(reqID)) {
										String receiver = request[1].split(": ")[1];
										Float amount = Float.parseFloat(request[2].split(": ")[1]);
										Transaction payRequest = new Transaction(userID, receiver, amount);
										SignedObject signedTransaction;
										try {
											signedTransaction = new SignedObject(payRequest, this.privateKey,
													Signature.getInstance("MD5withRSA"));
											out.writeObject(signedTransaction);
										} catch (InvalidKeyException | SignatureException
												| NoSuchAlgorithmException e) {
											e.printStackTrace();
										}
										found = true;
									}
									i++;
								}
								msg = (String) in.readObject();
								System.out.println(msg);
							}

							if (requestsRaw.length() == 0) {
								System.out.println("You dont have payment requests.");
							}

						}
						break;

					case "obtainQRcode":
					case "o":
						msg = obtainQRCodeC(choice);
						if (!"obtainQRcode".equals(msg)) {
							System.out.print(msg);
						} else {
							out.writeObject(choice);
							File f = (File) in.readObject();
							System.out.println("QRCode has been generated with sucess - " + f.getName());
						}
						break;

					case "confirmQRcode":
					case "c":
						msg = confirmQRCodeC(choice);
						if (!"confirmQRcode".equals(msg)) {
							System.out.print(msg);
						} else {
							out.writeObject(choice);

							String value = (String) in.readObject();
							if (!"There isnt a payment request associated to the request ID.".equals(value)) {
								String receiver = (String) in.readObject();
								Float amount = (Float) in.readObject();

								Transaction transactionConfirmQRCode = new Transaction(userID, receiver,
										amount);
								SignedObject signedTransaction;
								try {
									signedTransaction = new SignedObject(transactionConfirmQRCode, this.privateKey,
											Signature.getInstance("MD5withRSA"));
									out.writeObject(signedTransaction);
								} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
									e.printStackTrace();
								}

								System.out.println(in.readObject());
							}

							else {
								System.out.println(value);
							}

						}
						break;

					case "newgroup":
					case "n":
						msg = newGroupC(choice);
						if (!"newgroup".equals(msg)) {
							System.out.print(msg);
						} else {
							out.writeObject(choice);
							msg = (String) in.readObject();
							System.out.println(msg);
						}
						break;

					case "addu":
					case "a":
						msg = addUC(choice);
						if (!"addu".equals(msg)) {
							System.out.print(msg);
						} else {
							out.writeObject(choice);
							msg = (String) in.readObject();
							System.out.println(msg);
						}
						break;

					case "groups":
					case "g":
						out.writeObject(choice);
						msg = (String) in.readObject();
						System.out.print(msg);
						break;

					case "dividepayment":
					case "d":
						msg = dividePaymentC(choice);
						if (!"dividepayment".equals(msg)) {
							System.out.print(msg);
						} else {
							out.writeObject(choice);
							msg = (String) in.readObject();
							System.out.print(msg);
						}
						break;

					case "statuspayments":
					case "s":
						msg = statusPaymentsC(choice);
						if (!"statuspayments".equals(msg)) {
							System.out.print(msg);
						} else {
							out.writeObject(choice);
							msg = (String) in.readObject();
							System.out.print(msg);
						}
						break;

					case "history":
					case "h":
						msg = history(choice);
						if (!"history".equals(msg)) {
							System.out.print(msg);
						} else {
							out.writeObject(choice);
							msg = (String) in.readObject();
							System.out.print(msg);
						}
						break;

					default:
						System.out.println("Selected option does not exist.");
						break;
				}

				System.out.print("\nChoose an option:\n");
				choice = sc.nextLine();

				if ("quit".equals(choice)) {
					out.writeObject("quit");
				}
			}
		} catch (IOException |

				ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

}
