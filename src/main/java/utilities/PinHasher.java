package utilities;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

class PinHasher {

    private byte[] salt;
    private String hash;

    PinHasher(int pin){
        String pinHash ="";
        try {
            this.salt = getSalt("SHA1PRNG", "SUN");
            pinHash = getSecurePassword(pin, salt);
        }catch (Exception e){
            System.out.println("Pin hashing failed: " + e.getMessage());
        }
        this.hash = pinHash;
    }

    PinHasher(int pin, byte[] salt){
        String pinHash ="";
        try {
            this.salt = salt;
            pinHash = getSecurePassword(pin, salt);
        }catch (Exception e){
            System.out.println("Pin hashing failed: " + e.getMessage());
        }
        this.hash = pinHash;
    }

    private String getSecurePassword(int pin, byte[] salt) {
        String generatedPassword = null;
        try {
            // Create MessageDigest atmInstance for MD5
            MessageDigest md = MessageDigest.getInstance("MD5");
            //Add password bytes to digest
            md.update(salt);
            //Get the hash's bytes
            byte[] bytes = md.digest(ByteBuffer.allocate(4).putInt(pin).array());
            //This bytes[] has bytes in decimal format;
            //Convert it to hexadecimal format
            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            //Get complete hashed password in hex format
            generatedPassword = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return generatedPassword;
    }

    //Add salt
    private byte[] getSalt(String algorithm, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        //Always use a SecureRandom generator
        SecureRandom sr = SecureRandom.getInstance(algorithm, provider);
        //Create array for salt
        byte[] salt = new byte[16];
        //Get a random salt
        sr.nextBytes(salt);
        //return salt
        return salt;

    }

    String getHash() {
        return hash;
    }


    byte[] getSalt() {
        return salt;
    }


}
