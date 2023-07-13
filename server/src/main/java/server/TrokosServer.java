package server;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

/*
 * Class of server
 * 
 * @author Tiago Badalo - fc55311
 * @author Duarte Costa - fc54441
 * @author Francisco Fiaes - fc53711
 *
 */
public class TrokosServer {

	private int port;
	private String passwordCifra;
	private String keystore;
	private String passwordKeystore;

	private static final int DEFAULT_PORT = 45678;
	private static final int NUMBER_OF_ARGS = 4;
	private static final float INIT_BALANCE = 100;

	private ArrayList<String> transactions;

	/**
	 * 
	 */
	protected TrokosServer(String passwordCifra, String keystore, String passwordKeystore) {
		this.port = DEFAULT_PORT;
		this.passwordCifra = passwordCifra;
		this.keystore = keystore;
		this.passwordKeystore = passwordKeystore;
		this.transactions = new ArrayList<String>();
	}

	/**
	 * 
	 * @param port
	 */
	protected TrokosServer(int port, String passwordCifra, String keystore, String passwordKeystore) {
		this.port = port;
		this.passwordCifra = passwordCifra;
		this.keystore = keystore;
		this.passwordKeystore = passwordKeystore;
		this.transactions = new ArrayList<String>();
	}

	public static void main(String[] args) {

		System.out.println("\nServer is active.");
		TrokosServer server = null;
		if (args.length == NUMBER_OF_ARGS) {
			server = new TrokosServer(Integer.parseInt(args[0]), args[1], "./server-keystores/" + args[2], args[3]);
			System.setProperty("javax.net.ssl.keyStore", "./server-keystores/" + args[2]);
			System.setProperty("javax.net.ssl.keyStorePassword", args[3]);
		}
		if (args.length == NUMBER_OF_ARGS - 1) {
			server = new TrokosServer(args[0], "./server-keystores/" + args[1], args[2]);
			System.setProperty("javax.net.ssl.keyStore", "./server-keystores/" + args[1]);
			System.setProperty("javax.net.ssl.keyStorePassword", args[2]);
		}
		server.startServer();
	}

	/**
	 * Function that create threads on user connection
	 */
	protected void startServer() {

		ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
		SSLServerSocket sSoc = null;
		InfoCatalog users;
		BalanceCatalog balance = new BalanceCatalog();
		GroupsCatalog groups = new GroupsCatalog();
		Identificators identificators;
		UsersPayments payments = new UsersPayments();

		try {

			Cypher c = new Cypher(this.passwordCifra);

			File uf = new File("users.cif");
			uf.createNewFile();

			users = new InfoCatalog(uf, c);

			File rf = new File("identificators.cif");
			rf.createNewFile();

			identificators = new Identificators(rf, c);

			sSoc = (SSLServerSocket) factory.createServerSocket(this.port);
			FileInputStream fis = new FileInputStream(keystore);
			KeyStore ks = KeyStore.getInstance("JCEKS");
			ks.load(fis, this.passwordKeystore.toCharArray());
			PrivateKey privateKey = (PrivateKey) ks.getKey("server", this.passwordKeystore.toCharArray());

			users.getInfoFromFile();
			identificators.getIDsFromFile();

			if (checkBlockChainIntegrity(identificators)) {
				initState(identificators, balance);
			}

			while (true) {
				try {
					TransactionsInfo transactionsInfo = TransactionsInfo.getInstance();
					Socket inSoc = sSoc.accept();
					ServerThread newServerThread = new ServerThread(inSoc, users, balance, groups,
							payments, identificators, privateKey, transactionsInfo);
					newServerThread.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException
				| UnrecoverableKeyException e) {
			System.err.println(e.getMessage());
		}
	}

	protected void initState(Identificators identificators, BalanceCatalog balance) {
		Collections.sort(transactions, Collections.reverseOrder());
		restoreTransaction(this.transactions, balance);
	}

	protected boolean checkBlockChainIntegrity(Identificators identificators) {

		if (!new File("./log/block_" + 1 + ".blk").exists()) {
			return true;
		}
		for (long i = identificators.getCurrentBlock(); i >= 1; i--) {
			File block = new File("./log/block_" + i + ".blk");

			if (!block.exists()) {
				return false;
			}
			try {

				Signature s = Signature.getInstance("MD5withRSA");
				FileInputStream fis = new FileInputStream(keystore);
				KeyStore ks = KeyStore.getInstance("JCEKS");
				ks.load(fis, this.passwordKeystore.toCharArray());

				Certificate certificate = ks.getCertificate("server");
				PublicKey puServer = certificate.getPublicKey();

				s.initVerify(puServer);

				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(block));
				byte[] currentHash = (byte[]) ois.readObject();
				byte[] blockNumberBytes = (byte[]) ois.readObject();
				byte[] transactionNumberBytes = (byte[]) ois.readObject();

				long transactionsNumber = (long) deserialize(transactionNumberBytes);

				s.update(currentHash);
				s.update(blockNumberBytes);
				s.update(transactionNumberBytes);

				int j = 1;
				boolean reachedEndOfTransactions = false;
				while (j <= transactionsNumber && !reachedEndOfTransactions) {

					try {

						byte[] transactionBytes = (byte[]) ois.readObject();
						String transactionString = (String) deserialize(transactionBytes);
						this.transactions.add(transactionString);

						byte[] signature = (byte[]) ois.readObject();
						SignedObject signedTransaction = (SignedObject) deserialize(signature);

						s.update(transactionBytes);
						s.update(signature);

						String[] transactionStr = transactionString.split(":");
						String payer = transactionStr[0];

						File certificateFile = new File("certificates/" + payer + ".cer");

						if (!certificateFile.exists()) {
							System.out.println("Certificate doesn't exists for: " + payer);
							return false;
						}
						CertificateFactory cf = CertificateFactory.getInstance("X509");
						Certificate cert = cf.generateCertificate(new FileInputStream(certificateFile));

						PublicKey publicKey = cert.getPublicKey();
						if (!signedTransaction.verify(publicKey, Signature.getInstance("MD5withRSA"))) {
							System.out.println("Signature was not verified.");
							return false;
						}

					} catch (Exception e) {
						System.out.println("Reached the end of Transactions on initializing state.");
						reachedEndOfTransactions = true;
					}
					j++;
				}

				ois.close();

			} catch (ClassNotFoundException | IOException | NoSuchAlgorithmException | CertificateException
					| KeyStoreException | InvalidKeyException | SignatureException e) {
				e.printStackTrace();
				return false;
			}
		}

		return true;
	}

	protected void restoreTransaction(ArrayList<String> transactions, BalanceCatalog balance) {
		for (String t : transactions) {
			String[] transactionStr = t.split(":");
			String payer = transactionStr[0];
			String receiver = transactionStr[1];
			float amount = Float.parseFloat(transactionStr[2]);
			Optional<String> balanceC = balance.getInfo(payer);
			Optional<String> balanceU = balance.getInfo(receiver);
			if (!balanceC.isPresent()) {
				balance.putInfo(payer, String.valueOf(INIT_BALANCE));
				balanceC = balance.getInfo(payer);
			}
			String clientAmount = Float.toString(Float.parseFloat(balanceC.get()) - amount);
			balance.putInfo(payer, clientAmount);
			if (!balanceU.isPresent()) {
				balance.putInfo(receiver, String.valueOf(INIT_BALANCE));
				balanceU = balance.getInfo(receiver);
			}
			String userAmount = Float.toString(Float.parseFloat(balanceU.get()) + amount);
			balance.putInfo(receiver, userAmount);

		}

	}

	public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream is = new ObjectInputStream(in);
		return is.readObject();
	}
}
