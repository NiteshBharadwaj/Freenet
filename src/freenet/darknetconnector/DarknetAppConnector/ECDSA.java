/*
 * This is used to verify the EC signature. 
 * Though this app doesn't sign anything as of now, it verifies the signature.
 */
package freenet.darknetconnector.DarknetAppConnector;


import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.spongycastle.jce.provider.BouncyCastleProvider;
/**
 *
 * @author Illutionist
 */
public class ECDSA {
    
    public static boolean verify(String data,byte[] signature,byte[] publicKey) {
        boolean verify = false;
        try {
        	Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        	Security.addProvider(new BouncyCastleProvider());
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(publicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            PublicKey pubKey = keyFactory.generatePublic(pubKeySpec);
            byte[] buf = data.getBytes("UTF-8");
            Signature sig = Signature.getInstance("SHA1withECDSA", "BC");
            sig.initVerify(pubKey);
            sig.update(buf, 0,buf.length);
            verify = sig.verify(signature);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchProviderException ex) {
            Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeySpecException ex) {
            Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SignatureException ex) {
            Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ECDSA.class.getName()).log(Level.SEVERE, null, ex);
        }
        return verify;
    }
}
