package keyring;


/*
Keyring for Android
Copyright (C) 2011 Ron Riley
(android.keyring@gmail.com)

Keyring for Android is based on: 

KeyringEditor
Copyright 2004 Markus Griessnig
Vienna University of Technology
Institute of Computer Technology

KeyringEditor is based on:
Java Keyring v0.6
Copyright 2004 Frank Taylor <keyring@lieder.me.uk>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

// 02.02.2011 minor changes RR

// Crypto.java

// 29.10.2004

// 06.11.2004: removed entryId from decrypt()
// 17.11.2004: setPassword uses char[] (security reason); enrypt rest=0 bug
// 01.12.2004: Keyring database format 5 support added
// 04.12.2004: AES support added

import java.io.UnsupportedEncodingException;
import java.nio.charset.CharacterCodingException;
import java.security.AlgorithmParameters;
import java.security.MessageDigest;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


/**
 * This class is used to encrypt and decrypt entries.
 */
public class Crypto {
	
	static final boolean DEBUG= false;
	/**
	 * Check for odd parity
	 */
	protected static final int odd_parity[]={
		  1,  1,  2,  2,  4,  4,  7,  7,  8,  8, 11, 11, 13, 13, 14, 14,
		 16, 16, 19, 19, 21, 21, 22, 22, 25, 25, 26, 26, 28, 28, 31, 31,
		 32, 32, 35, 35, 37, 37, 38, 38, 41, 41, 42, 42, 44, 44, 47, 47,
		 49, 49, 50, 50, 52, 52, 55, 55, 56, 56, 59, 59, 61, 61, 62, 62,
		 64, 64, 67, 67, 69, 69, 70, 70, 73, 73, 74, 74, 76, 76, 79, 79,
		 81, 81, 82, 82, 84, 84, 87, 87, 88, 88, 91, 91, 93, 93, 94, 94,
		 97, 97, 98, 98,100,100,103,103,104,104,107,107,109,109,110,110,
		112,112,115,115,117,117,118,118,121,121,122,122,124,124,127,127,
		128,128,131,131,133,133,134,134,137,137,138,138,140,140,143,143,
		145,145,146,146,148,148,151,151,152,152,155,155,157,157,158,158,
		161,161,162,162,164,164,167,167,168,168,171,171,173,173,174,174,
		176,176,179,179,181,181,182,182,185,185,186,186,188,188,191,191,
		193,193,194,194,196,196,199,199,200,200,203,203,205,205,206,206,
		208,208,211,211,213,213,214,214,217,217,218,218,220,220,223,223,
		224,224,227,227,229,229,230,230,233,233,234,234,236,236,239,239,
		241,241,242,242,244,244,247,247,248,248,251,251,253,253,254,254};

	// Keyring database format 4
	/**
	 * Salt size in byte (Database format 4)
	 */
	protected static final int SALT_SIZE = 4;
	/**
	 * Maximum size of salt + password in byte (Database format 4)
	 */
	protected static final int MD5_CBLOCK = 64;
	/**
	 * MD5 hash size in byte (Database format 4)
	 */
	protected static final int MD5_DIGEST_LENGTH = 16;
	/**
	 * Cipher block size in byte (Database format 4)
	 */
	protected static final int KDESBLOCKSIZE = 8;

	// Keyring database format 4
	/**
	 * Password information (Database format 4)
	 */
	protected byte[] recordZero; // password information
	/**
	 * Databse version
	 */
	private int version;

	// Keyring database format 5
	/**
	 * Salt (Database format 5)
	 */
	protected byte[] salt = new byte[8];
	/**
	 * Hash (Database format 5)
	 */
	protected byte[] hash = new byte[8];
	/**
	 * Iterations (Database format 5)
	 */
	protected int iter;
	/**
	 * Cipher type (Database format 5),
	 * NO_CIPHER = 0,
	 * DES3_EDE_CBC_CIPHER = 1,
	 * AES_128_CBC_CIPHER = 2,
	 * AES_256_CBC_CIPHER = 3
	 */
	protected int type; // keyring: cipher

public int getType() {
		return type;
	}

public int getIterations() {
	return iter;
}

static{
	if (DEBUG){
		Provider provider[] = Security.getProviders();
		for (int i = 0; i< provider.length; i++){
			System.out.println("Provider: " + provider[i].getInfo());
		}
	}

}

	protected SecretKey key = null;
	protected Cipher cipher = null;

	/**
	 * Constructor for database format 4.
	 *
	 * @param recordZero Password information
	 * @param version equals 4
	 */
	public Crypto(byte[] recordZero, int version) {
		this.recordZero = recordZero; // null if Keyring database format 5
		this.version = version;
	}

	/**
	 * Constructor for database format 5.
	 *
	 * @param recordZero equals null
	 * @param version equals 5
	 * @param salt Salt
	 * @param hash Hash
	 * @param iter Iterations
	 * @param type Cipher type
	 */
	public Crypto(byte[] recordZero, int version, byte[] salt, byte[] hash, int iter, int type) {
		this.recordZero = recordZero; // null if Keyring database format 5
		this.version = version;
		this.salt = salt;
		this.hash = hash;
		this.iter = iter;
		this.type = type;
	}

	// setPassword ------------------------------------------------
	//
	// Source: keyring-link-0.1.1/keyring.c (gnukeyring.sourceforge.net)
	//
	// The master password is not stored in the database. Instead,
	// an MD5 hash of the password and a random 32-bit salt is stored and
	// checked against entered values. (Keyring crypto)
	//
	/**
	 * This method calls the setPassword method according to database version.
	 *
	 * @param password Database password
	 */
	public void setPassword(char[] password) throws Exception {
		switch(version) {
			case 4: setPassword_4(password); break;
			case 5: setPassword_5(password); break;
		}
	}

	/**
	 * This method checks the entered password and generates record encryption key for database format 5.
	 *
	 * @param password Database password
	 */
	public void setPassword_5(char[] password) throws Exception {
		int index;
		int[] cipherlen = {0, 24, 16, 32}; // keylength in byte
		byte[] pass = new byte[password.length];

		for(int i=0;i<password.length;i++) {
			pass[i] = (byte)(0xFF & password[i]);
		}

		// PKCS#5 PBKDF2
		// Key Derivation function
		byte[] deskey = pbkdf2(pass, salt, iter, cipherlen[type]);

		// set odd parity
		if(type == 1) { // TripleDES
			for(int i=0; i<24; i++) {
				index = (int)(0xff & deskey[i]);
				deskey[i] = (byte)odd_parity[index];
			}
		}

		// SHA1
		byte[] digest = getMessageDigest(deskey, salt);

		byte[] help = Model.sliceBytes(digest, 0, hash.length);

		// check password
		if(!Arrays.equals(hash, help)) {
			throw new Exception("Password incorrect.");
		}

		// for security reason set each element to zero
		for(int i=0;i<pass.length;i++) {
			pass[i] = 0;
		}

		// setup cipher according to cipher type
		switch(type) {
			case 1:
  		      	key = SecretKeyFactory.getInstance("DESede").generateSecret(new DESedeKeySpec(deskey));
				//cipher = Cipher.getInstance("TripleDES/CBC/NoPadding","BC");
				cipher = Cipher.getInstance("DESede/CBC/NoPadding");

				break;
			case 2:
 				key = new SecretKeySpec(Model.sliceBytes(deskey, 0, 16), "AES"); // 128 bit
				cipher = Cipher.getInstance("AES/CBC/NoPadding");
				break;
			case 3:
 				key = new SecretKeySpec(Model.sliceBytes(deskey, 0, 32), "AES"); // 256 bit
				cipher = Cipher.getInstance("AES/CBC/NoPadding");
				break;
			default:
				throw new Exception("Cipher " + type + " not supported.");
		}
	}

	/**
	 * This method returns a SHA1 Message digest.
	 *
	 * @param key Key
	 * @param salt Salt
	 *
	 * @return SHA1 Message digest
	 */
	public byte[] getMessageDigest(byte[] key, byte[] salt) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA1");
		md.update(key);
		md.update(salt);

		return md.digest();
	}

	/**
	 * This method is an implementation of PKCS#5 PBKDF2.
	 *
	 * @param pass Database password
	 * @param salt Salt
	 * @param iter Iterations
	 * @param keylen Keylength of choosen cipher type
	 *
	 * @return Record encryption key
	 */
	public byte[] pbkdf2(byte[] pass, byte[] salt, int iter, int keylen) throws Exception {
		// PKCS#5 PBKDF2
		// Key Derivation function
		int SHA_DIGEST_LENGTH = 20;
		int blocklen;
		int i = 1;
		byte itmp[] = new byte[4];
		int pos = 0;
		byte digtmp[] = new byte[SHA_DIGEST_LENGTH];
		byte p[] = new byte[keylen];
		int j, k;

	    Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec key = new SecretKeySpec(pass, "HmacSHA1");
        mac.init(key);

		while(keylen > 0) {

			if(keylen > SHA_DIGEST_LENGTH)
				blocklen = SHA_DIGEST_LENGTH;
			else
				blocklen = keylen;

			itmp[0] = (byte)(0xff & (i >> 24));
			itmp[1] = (byte)(0xff & (i >> 16));
			itmp[2] = (byte)(0xff & (i >> 8));
			itmp[3] = (byte)(0xff & i);

			mac.reset();
			mac.update(salt);
			digtmp = mac.doFinal(itmp);
			System.arraycopy(digtmp, 0, p, pos, blocklen);

			for(j = 1; j < iter; j++) {
				mac.reset();
				digtmp = mac.doFinal(digtmp);

				for(k = 0; k < blocklen; k++) p[pos+k] ^= digtmp[k];
			}

			keylen = keylen - blocklen;
			pos = pos + blocklen;
			i++;
		}

		return p;
	}

	/**
	 * This method is an implementation of RFC 2104 (HMAC). Not used.
	 */
	public byte[] hmac(byte[] text, byte[] key, String hashfunction) throws Exception {
		// rfc 2104
		int BLOCKSIZE = 64; // byte length
		byte[] ipad = new byte[BLOCKSIZE];
		byte[] opad = new byte[BLOCKSIZE];
		byte[] digest;

		Arrays.fill(ipad, (byte)0x00);
		Arrays.fill(opad, (byte)0x00);
		System.arraycopy(key, 0, ipad, 0, key.length);
		System.arraycopy(key, 0, opad, 0, key.length);

		for(int i=0;i<BLOCKSIZE;i++) {
			ipad[i] = (byte)(0x36 ^ ipad[i]);
			opad[i] = (byte)(0x5C ^ opad[i]);
		}

		// hashfunction = "MD5" or "SHA1"
		MessageDigest md = MessageDigest.getInstance(hashfunction);
		md.update(ipad);
		md.update(text);
		digest = md.digest();

		md.reset();
		md.update(opad);
		md.update(digest);
		digest = md.digest();

		return digest;
	}

	/**
	 * This method checks the entered password and generates record encryption key for database format 4.
	 *
	 * @param password Database password
	 */
	public void setPassword_4(char[] password) throws Exception {
		byte[] hash;
		byte[] desKeyData = new byte[24]; // 8 byte * 3
		byte[] snib;
		int i;

		byte[] pass = new byte[password.length];

		for(i=0;i<password.length;i++) {
			pass[i] = (byte)(0xFF & password[i]);
		}

		// for security reason set each element to zero
		for(i=0;i<password.length;i++) {
			password[i] = 0;
		}

		// Keyring supports passwords of up to 40 characters
		if(pass.length > 40) {
			throw new Exception("Password too long.");
		}

		// check password
		hash = checkPasswordHash_4(recordZero, pass);

		if(!Arrays.equals(hash, Model.sliceBytes(recordZero, SALT_SIZE, MD5_DIGEST_LENGTH))) {
			throw new Exception("Password incorrect.");
		}
		if (DEBUG) System.out.println("password ok");//TODO

		// --------------------------------------------------------
		// generate record encryption key
		// --------------------------------------------------------

		/*
		The master password is also used to generate a record encryption key.
		The 128-bit MD5 hash of the master password is split into two 64-bit keys, K1 and K2.
		(DES ignores the top bit of each byte, so the key has 112 effective unknown bits.)
		These are used to generate record data encrypted as Enc(K1, Dec(K2, Enc(K1, Data))).
		Each 8-byte data block is independently encrypted by the same key.
		(Keyring crypto)
		*/

		// calc_snib()
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(pass);
		snib = md.digest(); // 128 bit md5 hash
		//System.out.println("snib.length = " + snib.length);

		// generate the DES keypair (snib = A,B; desKeyData = A,B,A)
		for(i=0; i<16; i++) {
			desKeyData[i] = snib[i];

			if(i < 8) {
				desKeyData[i + 16] = snib[i];
			}
		}

		// setup SecretKey and Cipher
		key = SecretKeyFactory.getInstance("DESede").generateSecret(new DESedeKeySpec(desKeyData));

		cipher = Cipher.getInstance("DESede/ECB/NoPadding");
		//cipher = Cipher.getInstance("DESede/CBC/NoPadding");


		// ECB Electronic Codebook Mode

		// for security reason set each element to zero
		for(i=0;i<pass.length;i++) {
			pass[i] = 0;
		}
	}

	/**
	 * This method generates a MD5 hash of the password and the salt.
	 *
	 * @param pass Database password
	 * @param salt Salt
	 *
	 * @return MD5 hash
	 */
	protected byte[] checkPasswordHash_4(byte[] salt, byte[] pass) throws Exception {
		byte[] digest = new byte[MD5_DIGEST_LENGTH]; // 128 bit md5 hash
		byte[] msg = new byte[MD5_CBLOCK]; // salt + password
		int i;

		Arrays.fill(msg, (byte)0);

		// 32 bit salt
		for(i=0; i<SALT_SIZE; i++) {
			msg[i] = salt[i];
		}

		// strncpy(msg + kSaltSize, pass, MD5_CBLOCK - 1 - kSaltSize);
		for(i=0; i<pass.length; i++) {
			msg[i + SALT_SIZE] = pass[i]; // S A L T P A S S W O R D 0 ...
		}

		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(msg);
		digest = md.digest(); // output: 128 bit digest of entered password

		// return !memcmp(digest, rec0+kSaltSize, MD5_DIGEST_LENGTH);
		return digest;
	}

	// decrypt ----------------------------------------------------
	/**
	 * This method calls the decrypt method according to database version.
	 *
	 * @param cipherText Text to decrypt
	 * @param fieldName Field to return (account, password, notes, datetype)
	 * @param iv Initialisation vector (only in database format 5)
	 *
	 * @return String with decrypted text or null if no fieldName was specified
	 * @throws UnsupportedEncodingException 
	 */
	public Object decrypt(byte[] cipherText, String fieldName, byte[] iv) throws UnsupportedEncodingException {
		Object temp = null;

		switch(version) {
			case 4: temp = decrypt_4(cipherText, fieldName); break;
			case 5: temp = decrypt_5(cipherText, fieldName, iv); break;
		}

		return temp;
	}

	// Keyring database format 5
	/**
	 * This method decrypts a ciphertext in database format 5.
	 *
	 * @param encrypted Text to decrypt
	 * @param fieldName Field to return (account, password, notes, datetype)
	 * @param iv Initialisation vector (only in database format 5)
	 *
	 * @return String with decrypted text or null if no fieldName was specified
	 * @throws UnsupportedEncodingException 
	 * @throws CharacterCodingException 
	 */
	public Object decrypt_5(byte[] encrypted, String fieldName, byte[] iv) throws UnsupportedEncodingException {
        String account = null;
        String password = null;
        String notes = null;
        byte[] datetype = null;
        byte[] plain = null;
		int pos = 0;
		int len;
		int reallen;
		int label;

	    AlgorithmParameters params = null;

	    try {
			// initialize
        	switch(type) {
        		case 1: // TripleDES
        			params = AlgorithmParameters.getInstance("DES");
					params.init(new IvParameterSpec(iv));
					cipher.init(Cipher.DECRYPT_MODE, key, params);
					plain = cipher.update(encrypted);
        			break;
        		case 2: // AES 128 bit
        		case 3: // AES 256 bit
        			params = AlgorithmParameters.getInstance("AES");
					params.init(new IvParameterSpec(iv));
					cipher.init(Cipher.DECRYPT_MODE, key, params);
					plain = cipher.doFinal(encrypted);
					//plain = cipher.update(encrypted);

        			break;
        	}
		}
		catch(Exception e) {
	    	e.printStackTrace(System.err);
	    	return "Could not decrypt data.";
		}

		// mg
		//Model.printHexByteArray("decrypt_5", encrypted);
		//Model.printHexByteArray("decrypt_5", plain);

		len = (int)Model.sliceNumber(plain, pos, 2); // length of field

		while(len != 0xffff) {
			reallen = (len + 1) & ~1; // padding for next even address

			label = (int)Model.sliceNumber(plain, pos + 2, 1);

			//System.out.println(pos + ": type=" + type + ", len" + len);

			switch(label) {
				case 1: account = Model.sliceString(plain, pos + 4, len); break;
				case 2: password = Model.sliceString(plain, pos + 4, len); break;
				case 3: datetype = Model.sliceBytes(plain, pos + 4, 2); break;
				case 255: notes = Model.sliceString(plain, pos + 4, len); break;
			}

			pos = pos + reallen + 4;

			if(pos < (plain.length - 2))
				len = (int)Model.sliceNumber(plain, pos, 2);
			else
				len = 0xffff;
		}

		if(fieldName.equals("account")) return (Object)account;
		if(fieldName.equals("password")) return (Object)password;
		if(fieldName.equals("notes")) return (Object)notes;
		if(fieldName.equals("datetype")) return (Object)datetype;

		return null; // no fieldname specified
	}

	/**
	 * This method decrypts a ciphertext in database format 4.
	 *
	 * @param encrypted Text to decrypt
	 * @param fieldName Field to return (account, password, notes, datetype)
	 *
	 * @return String with decrypted text or null if no fieldName was specified
	 * @throws UnsupportedEncodingException 
	 */
	public Object decrypt_4(byte[] cipherText, String fieldName) throws UnsupportedEncodingException{
	    int posPlain = 0;
		int nextItem = 0;
		int i, j;
		int len = cipherText.length;
		int rest;
		byte[] plainText = new byte[len];
		byte[] buffer;
		byte[] buffer2;

		try {
			
			cipher.init(Cipher.DECRYPT_MODE, key);

			// ECB: 8 byte blocks
			for(i=0; i<(len / KDESBLOCKSIZE); i++) {
				buffer = cipher.update(cipherText, i * KDESBLOCKSIZE, KDESBLOCKSIZE);
				for(j=0; j<buffer.length; j++) {
					plainText[posPlain++] = buffer[j];
				}
			}

			rest = len % KDESBLOCKSIZE;

			buffer = new byte[KDESBLOCKSIZE];
			for(i=0; i<KDESBLOCKSIZE; i++) {
				buffer[i] = (i < rest) ? cipherText[len - rest + i] : 0;
				// zero padding to get 8 bytes
			}

			buffer2 = cipher.doFinal(buffer);
			for(j=0; j<rest; j++) {
				plainText[posPlain++] = buffer2[j];
			}
		}
		catch(Exception e) {
			return "Could not decrypt data.";
		}

		//Model.printHexByteArray("decrypt", plainText);
		// get account, password & notes
		if (DEBUG) System.out.println("WHOLE STRING: " + new String((byte[]) plainText,Model.PALM_CHARSET));
		String account = Model.sliceString(plainText,nextItem, -1);
		int accountLength = account.getBytes(Model.PALM_CHARSET).length;
		//nextItem += account.length() + 1;
		nextItem += accountLength + 1;

		
		if (DEBUG) System.out.println ("SLICED account: " + account + "account length is: " + account.length());
		if (DEBUG) System.out.println("account bytes length is " + account.getBytes(Model.PALM_CHARSET).length);
		
		String password = Model.sliceString(plainText, nextItem, -1);
		int passwordLength = password.getBytes(Model.PALM_CHARSET).length;

		//nextItem += password.length() + 1;
		nextItem += passwordLength + 1;

		if (DEBUG) System.out.println ("SLICED password: " + password);


		String notes = Model.sliceString(plainText, nextItem, -1);
		int notesLength = notes.getBytes(Model.PALM_CHARSET).length;

		//nextItem += notes.length() + 1;
		nextItem += notesLength + 1;


		
		if (DEBUG) System.out.println ("SLICED notes: " + notes);

		// early version of palm may not have date
		byte[] datetype = new byte[0];
		try{
			datetype = Model.sliceBytes(plainText, nextItem, 2);
		}catch (ArrayIndexOutOfBoundsException noDate){
			if (DEBUG) System.out.println("Caught no date exception");
		}

		if(fieldName.equals("account")) return (Object)account;
		if(fieldName.equals("password")) return (Object)password;
		if(fieldName.equals("notes")) return (Object)notes;
		if(fieldName.equals("datetype")) return (Object)datetype;

		return (Object)plainText; // no fieldname specified
	}

	// encrypt ----------------------------------------------------
	/**
	 * This method calls the encrypt method according to database version.
	 *
	 * @param plainText Text to encrypt
	 *
	 * @return Encrypted text
	 */
	public byte[] encrypt(byte[] plainText) throws Exception {
		byte[] temp = null;

		switch(version) {
			case 4: temp = encrypt_des_aes(plainText, 8); break;
			case 5:
				switch(type) {
					case 1: temp = encrypt_des_aes(plainText, 8); break;
					case 2: temp = encrypt_des_aes(plainText, 16); break;
					case 3: temp = encrypt_des_aes(plainText, 16); break;
				}
				break;
		}

		return temp;
	}

	/**
	 * This method encrypts a text.
	 *
	 * @param plainText Text to encrypt
	 * @param blocksize AES 16 byte, TripleDES 8 byte
	 *
	 * @return 16 byte IV + Encrypted text
	 */
	public byte[] encrypt_des_aes(byte[] plainText, int blocksize) throws Exception {
		if (DEBUG) System.out.println ("enc_des_aes: " + plainText + " block " + blocksize);
		//[B@43e86e18
		
	 	int clen;
		int i, j;
		int cpos = 0;
		int rest;
		byte[] buffer;
		byte[] buffer2;
		byte[] cipherText;
		byte[] iv;

		int plen = plainText.length;

		// blocksize byte blocks
		// TripleDES 8 byte
		// AES 16 byte
		if(plen % blocksize != 0) {
			clen = plen + (blocksize - (plen % blocksize));
		}
		else {
			clen = plen;
		}

		cipherText = new byte[clen];


		// initialize cipher
		cipher.init(Cipher.ENCRYPT_MODE, key);

		iv = cipher.getIV();


		//if(iv != null) {
		//	System.out.println("iv: " + iv.length);
		//}

		// encrypt data in length of blocksize
		for(i=0; i<(plen / blocksize); i++) {
			buffer = cipher.update(plainText, i * blocksize, blocksize);
			for(j=0; j<buffer.length; j++) {
				cipherText[cpos++] = buffer[j];
			}
		}

		// last block of data does not have full blocksize
		rest = plen % blocksize;
		if(rest != 0) {
			buffer = new byte[blocksize];
			for(i=0; i<blocksize; i++) {
				buffer[i] = (i < rest) ? plainText[plen - rest + i] : 0;
			}

			buffer2 = cipher.doFinal(buffer);

			for(j=0; j<blocksize; j++) {
				cipherText[cpos] = buffer2[j];
				cpos++;
			}
		}

		// return: 16 byte iv + cipherText
		byte[] temp = new byte[16 + cipherText.length];
		Arrays.fill(temp, (byte)0);

		// Keyring database format 5
		if(iv != null) {
			System.arraycopy(iv, 0, temp, 0, iv.length);
		}

		System.arraycopy(cipherText, 0, temp, 16, cipherText.length);
		return temp;
	}
}
