package RGcards.SportsCardProject.app;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Q2 {
    public static void main(String[] args) {
        encrypt("LUKE");
        encrypt("0911123456");
        encrypt("伊原力科技May4STech");

    }

    public static void encrypt(String text) {
        try {
            String key = "QX5PNYo8NgSIn7v46JomDqbb";
            String iv = "Z62BZzhu3NkkfBrX";

            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes("UTF-8"));

            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

            byte[] encrypted = cipher.doFinal(text.getBytes("UTF-8"));

            String encryptText = bytesToHex(encrypted);

            System.out.println("明文: " + text);
            System.out.println("密文: " + encryptText);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


}
