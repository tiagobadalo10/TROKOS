package server;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.google.zxing.EncodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/*
 * Class representing a payment request
 * 
 * @author Tiago Badalo - fc55311
 * @author Duarte Costa - fc54441
 * @author Francisco Fiaes - fc53711
 *
 */

public class PaymentRequest {

	private String userID;
	private String clientID;
	private File QRCode;
	private float amount;
	private Status status;
	private int requestID;
	private String groupID;
	private int checkID;

	/**
	 * Create a payment request between two clients
	 * 
	 * @param userID
	 * @param clientID
	 * @param amount    to be paid
	 * @param requestID of the payment request
	 */

	protected PaymentRequest(String userID, String clientID, float amount, int requestID) {
		this.userID = userID;
		this.clientID = clientID;
		this.amount = amount;
		this.status = Status.PENDING;
		this.requestID = requestID;
	}

	/**
	 * Create a payment request identified by QRCode
	 * 
	 * @param userID
	 * @param amount to be paid
	 * @param QRCode
	 */
	protected PaymentRequest(String userID, float amount, File QRCode) {
		this.userID = userID;
		this.amount = amount;
		this.QRCode = QRCode;

		Map<EncodeHintType, ErrorCorrectionLevel> hintMap = new HashMap<>();

		hintMap.put(EncodeHintType.ERROR_CORRECTION,
				ErrorCorrectionLevel.L);

		try {
			this.requestID = Integer.parseInt(MyQrCode.readQR(QRCode.getPath()));
		} catch (NumberFormatException | NotFoundException | IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create a payment request for a dividepayment of a group
	 * 
	 * @param userID
	 * @param clientID
	 * @param amount    to be paid
	 * @param groupID   associated to the group
	 * @param requestID of the payment request
	 * @param checkID   of the payment requests (dividepayment)
	 */
	protected PaymentRequest(String userID, String clientID, float amount, String groupID, int requestID, int checkID) {
		this.userID = userID;
		this.clientID = clientID;
		this.groupID = groupID;
		this.amount = amount;
		this.status = Status.PENDING;
		this.requestID = requestID;
		this.checkID = checkID;
	}

	/**
	 * Create a payment request with the content of QRCode as requestID
	 * 
	 * @param userID
	 * @param amount to be paid
	 * @param QRCode associated to image
	 */
	public PaymentRequest(String userID, float amount, int QRCode) {
		this.userID = userID;
		this.amount = amount;
		this.requestID = QRCode;
	}

	/**
	 * 
	 * @return groupID associated to the payment request
	 */

	protected String getGroupID() {
		return this.groupID;
	}

	/**
	 * 
	 * @return userID associated to the payment request (who created it)
	 */

	protected String getUserID() {
		return userID;
	}

	/**
	 * 
	 * @return clientID associated to the payment request (who will pay it)
	 */

	protected String getClientID() {
		return clientID;
	}

	/**
	 * 
	 * @return QRcode associated to the payment request
	 */

	protected File getQRCode() {
		return QRCode;
	}

	/**
	 * 
	 * @return amount associated to the payment request
	 */

	protected float getAmount() {
		return amount;
	}

	/**
	 * 
	 * @return requestID associated to the payment request
	 */

	protected int getRequestID() {
		return requestID;
	}

	/**
	 * 
	 * @return status of the payment request
	 */

	protected Status getStatus() {
		return status;
	}

	/**
	 * 
	 * @return checkID of the payment requests
	 */
	protected int getCheckID() {
		return checkID;
	}

	/**
	 * Change status of a payment request
	 * 
	 * @param status Status of a payment request
	 */

	protected void setStatus(Status status) {
		this.status = status;
	}

}
