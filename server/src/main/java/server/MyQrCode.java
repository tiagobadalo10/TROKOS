package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

/*
 * Class responsible for generating and reading QR codes
 * 
 * @author Tiago Badalo - fc55311
 * @author Duarte Costa - fc54441
 * @author Francisco Fiaes - fc53711
 * 
 */

public class MyQrCode {

	/**
	 * Create one QRCode
	 * 
	 * @param data    hidden in QRCode
	 * @param path    of QRCode
	 * @param charset
	 * @param height
	 * @param width
	 */
	protected static void createQR(String data, String path, String charset, int height, int width)
			throws WriterException, IOException {

		BitMatrix matrix = new MultiFormatWriter().encode(new String(data.getBytes(charset), charset),
				BarcodeFormat.QR_CODE, width, height);

		MatrixToImageWriter.writeToFile(matrix, path.substring(path.lastIndexOf('.') + 1), new File(path));
	}

	/**
	 * Read the hidden data of QRCode
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 */
	protected static String readQR(String path) throws IOException, NotFoundException {

		BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(ImageIO.read(
				new FileInputStream(path)))));

		Result result = new MultiFormatReader().decode(binaryBitmap);

		return result.getText();
	}

}
