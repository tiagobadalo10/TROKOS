package server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/*
 * Class that Encrypts and Decrypts Data
 * 
 * @author Tiago Badalo - fc55311
 * @author Duarte Costa - fc54441
 * @author Francisco Fiaes - fc53711
 *
 */
public class Cypher {

    private SecretKey key;
    private String passwordCifra;
    private byte[] params;

    /**
     * Creates Instance of Cypher
     * 
     * @param passwordCifra Password to be used by class
     */
    public Cypher(String passwordCifra) {
        this.passwordCifra = passwordCifra;
        generateKey();
    }

    /**
     * Generates key to be used for Encryptions and Decryptions
     * 
     * @return SecreteKey
     */
    public SecretKey generateKey() {

        byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78, (byte) 0x99, (byte) 0x52, (byte) 0x3e,
                (byte) 0xea,
                (byte) 0xf2 };

        PBEKeySpec keySpec = new PBEKeySpec(this.passwordCifra.toCharArray(), salt, 20);
        SecretKeyFactory kf;
        SecretKey key = null;
        try {
            kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
            key = kf.generateSecret(keySpec);
            this.key = key;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return key;
    }

    /**
     * Decrypts data from a file
     * 
     * @param f        File
     * @param filetype File Specification (identificators or users)
     * @return Data decrypted
     */
    protected String decryptDataFile(File f, String filetype) {
        Cipher c;
        try {
            AlgorithmParameters p = AlgorithmParameters.getInstance("PBEWithHmacSHA256AndAES_128");
            c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
            File paramsFile = new File(filetype + "-params.txt");

            if (paramsFile.exists()) {
                FileInputStream fileInputStream = new FileInputStream(paramsFile);
                ObjectInputStream ois = new ObjectInputStream(fileInputStream);
                byte[] values = (byte[]) ois.readObject();
                this.params = values;
                ois.close();
            } else {
                return null;
            }

            p.init(this.params);
            c.init(Cipher.DECRYPT_MODE, this.key, p);

            byte[] b = new byte[(int) f.length()];
            int x;

            FileInputStream fis = new FileInputStream(f);
            CipherInputStream cis = new CipherInputStream(fis, c);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            while ((x = cis.read(b)) >= 0) {
                baos.write(b, 0, x);
            }

            String returnValue = baos.toString();
            cis.close();
            baos.close();
            return returnValue;
        } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Encrypts data from a File
     * 
     * @param f        File
     * @param data     Data to be encrypted
     * @param filetype File Specification (identificators or users)
     */
    protected void encryptDataFile(File f, String data, String filetype) {

        Cipher c;

        try {

            c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
            c.init(Cipher.ENCRYPT_MODE, this.key);

            File paramsFile = new File(filetype + "-params.txt");

            if (paramsFile.exists())
                paramsFile.delete();

            if (paramsFile.createNewFile()) {
                FileOutputStream fos = new FileOutputStream(paramsFile);
                ObjectOutputStream outputStream = new ObjectOutputStream(fos);
                outputStream.writeObject(c.getParameters().getEncoded());
                outputStream.close();
            }

            FileOutputStream fosCypher = new FileOutputStream(f);
            CipherOutputStream cos = new CipherOutputStream(fosCypher, c);

            data = data + "\0";

            cos.write(data.getBytes());
            cos.close();

        } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            e.printStackTrace();

        }
    }

}
