package org.wsy.devutils.codec;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * 字符串编码类
 */
public class StringCodec {

    private static final String DESALG = "DESede";

    /**
     * 转为URLsafe的BASE64串
     * @param source
     * @return
     */
    public static String toBase64(String source) {
        return Base64.encodeBase64URLSafeString(source.getBytes());
    }

    /**
     * 转为URLsafe的BASE64串
     * @param source
     * @return
     */
    public static String toBase64(byte[] source) {
        return Base64.encodeBase64URLSafeString(source);
    }


    /**
     * 将BASE64串还原
     * @param source
     * @return
     */
    public static String fromBase64(String source) {
        return new String(Base64.decodeBase64(source));
    }

    /**
     * 将BASE64串还原
     * @param source
     * @return
     */
    public static String fromBase64(byte[] source) {
        return new String(Base64.decodeBase64(source));
    }

    /**
     * 3DES算法加密
     * @param src
     * @param key
     * @return
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static byte[] DESEncode(byte[] src, String key)
            throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        SecretKey deskey = new SecretKeySpec(build3DesKey(key), DESALG);
        Cipher c1 = Cipher.getInstance(DESALG);
        c1.init(Cipher.ENCRYPT_MODE, deskey);
        return c1.doFinal(src);
    }

    /**
     * 3DES算法解密
     * @param src
     * @param key
     * @return
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static byte[] DESDecode(byte[] src, String key)
            throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        SecretKey deskey = new SecretKeySpec(build3DesKey(key), DESALG);
        Cipher c1 = Cipher.getInstance(DESALG);
        c1.init(Cipher.DECRYPT_MODE, deskey);
        return c1.doFinal(src);
    }

    private static byte[] build3DesKey(String keyStr) throws UnsupportedEncodingException {
        byte[] key = new byte[24]; // 声明一个24位的字节数组，默认里面都是0
        byte[] temp = keyStr.getBytes("UTF-8"); // 将字符串转成字节数组
        if (key.length > temp.length) {
            System.arraycopy(temp, 0, key, 0, temp.length);
        } else {
            System.arraycopy(temp, 0, key, 0, key.length);
        }
        return key;
    }

}
