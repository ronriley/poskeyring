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
// RR minor changes

// Model.java

// 29.10.2004

// 31.10.2004: backup header and categories for saveData()
// 02.11.2004: class entry changed; added parameter -e; added parameter -d
// 03.11.2004: added getDateType()
// 04.11.2004: added getDataFormat()
// 06.11.2004: added debugByteArray()
// 08.11.2004: Categories-Array with 276 byte
// 11.11.2004: addes elements(), getCategoryName(), getCategories(); loadData - empty title possible
// 17.11.2004: setPassword uses char[] (security reason)
// 23.11.2004: added saveEntriesToFile()
// 24.11.2004: updated saveEntriesToFile(); updated saveData()
// 30.11.2004: printHexByteArray() added
// 01.12.2004: Keyring database format 5 support added
// 02.12.2004: toRecordFormat5() added
// 05.12.2004: convertDatabase() added
// 07.12.2004: convertDatabase() updated
// 12.01.2004: writeNewDatabase() added
// 07.09.2005: loadDatabase() ignores deleted record table entries
// 23.09.2005: added getNewUniqueId()

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.CharacterCodingException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.androidron.keyring.CategoryException;
import com.androidron.keyring.CategoryNameTooLongException;
import com.androidron.keyring.TooManyCategoriesException;


/**
 * This class is used to load and save Keyring databases.
 *
 */
public class Model {
	

	public static final boolean DEBUG = false;
	
	private boolean withErrors;
	
	public boolean isWithErrors() {
		return withErrors;
	}
	
	public String showErrorEntries(){
		return sbErrors.toString();
	}
	
	private StringBuffer sbErrors = new StringBuffer();

	public static String PALM_CHARSET;//="ISO-8859-1";

	public Model (String charset){
		PALM_CHARSET=charset;
	}
	
	// ----------------------------------------------------------------
	// variables
	// ----------------------------------------------------------------

	/**
	 * Field type & creator in PDB header information: Used by Palm OS to
	 * determine the application for the database. I am not sure if v2.0 will
	 * use the same creator-name as v1.2.2. At the moment v2.0-pre1 uses a
	 * different creator-name.
	 * 
	 * v2.0-pre4 uses the same creator-name.
	 */
	private static String applcreator4 = "GkyrGtkr";
	private static String applcreator5 = "GkyrGtkr";
	public static int UNFILED_CATEGORY = 0;// TODO make a category class
	public static int CATEGORY_MAXLENGTH = 15;
	public static int MAX_NUM_CATEGORIES = 16;
	public static final String ALL_CATEGORY = "ALL";

	// saveEntriesToFile()
//	/**
//	 * Filename of CSV File
//	 */
//	private static String csvFilename = "keyring.csv"; // default
//
//	/**
//	 * CSV-Separator
//	 */
//	private static char csvSeparator = ';'; // default

	// PDB header information (readPDBHeader)
	/**
	 * Header of Keyring database
	 */
	private byte[] pdbHeader = new byte[78];

	/**
	 * Categories in Keyring database
	 */
	private byte[] pdbCategories = initialisePdbCategories();

	private byte[] initialisePdbCategories() {
		return new byte[276];
	}

	private String pdbName; // 32
	private int pdbFlags; // 2 (unsigned)

	/**
	 * Keyring database version
	 */
	protected int pdbVersion; // 2 (unsigned) // Keyring database format

	// NEW
	public int getPdbVersion() {
		return pdbVersion;
	}

	private long pdbModNumber; // 4 (unsigned), modification number
	private int pdbSortInfoOffset; // 4
	private String pdbType; // 4
	private String pdbCreator; // 4
	private int pdbAppInfoOffset; // 4

	/**
	 * Number of records in the keyring database
	 */
	private int pdbNumRecords; // 2

	// Keyring database format 4
	private int recordZeroAttribute;
	private int recordZeroUniqueId;
	private int recordZeroLength;

	/**
	 * Vector to entry objects
	 */
	// private Vector<Entry> entries = new Vector<Entry>(); // Java 1.5
	// private Vector entries = new Vector(); // reference to entry objects
	private HashMap<Integer, Entry> entries = new HashMap<Integer, Entry>(); // reference to entry objects

	// private Iterator elements;

	/** categories holds 'real' categories, displayCategories adds 'ALL' */
	// private Vector displayCategories = null;

	/**
	 * Vector to category strings
	 */
	 private Vector<String> categories = new Vector<String>(); // Java 1.5

	/**
	 * Reference to class Crypto
	 */
	public Crypto crypto; // Gui.java

	// ----------------------------------------------------------------
	// public ---------------------------------------------------------
	// ----------------------------------------------------------------

	// writeNewDatabase -----------------------------------------------
	/**
	 * This method dumps a minimal database with password "test".
	 * 
	 * @param filename
	 *            New database filename
	 */
	public static void writeNewDatabase(FileOutputStream fp) {
		int[] header = { 0x4B, 0x65, 0x79, 0x73, 0x2D, 0x47, 0x74, 0x6B, 0x72,
				0x00, 0x6B, 0x72, 0x5F, 0x61, 0x70, 0x70, 0x6C, 0x5F, 0x61,
				0x36, 0x38, 0x6B, 0x00, 0x00, 0x73, 0x79, 0x73, 0x70, 0x04,
				0x00, 0x73, 0x70, 0x00, 0x08, 0x00, 0x04, 0xBD, 0xDB, 0x65,
				0x06, 0xBD, 0xDB, 0x65, 0x0D, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x0E, 0x00, 0x00, 0x00, 0x60, 0x00, 0x00, 0x00,
				0x00, 0x47, 0x6B, 0x79, 0x72, 0x47, 0x74, 0x6B, 0x72, 0x00,
				0xB7, 0x30, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00,
				0x00, 0x01, 0x74, 0x50, 0xB7, 0x30, 0x01, 0x00, 0x00, 0x01,
				0x88, 0x40, 0xB7, 0x30, 0x02, 0x00, 0x00, 0x1F, 0x1F };

		int[] data = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
				0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x0F, 0x00, 0xB0,
				0xDA, 0x43, 0x4A, 0x91, 0x55, 0x12, 0xEC, 0xD5, 0x96, 0xCD,
				0x21, 0x9A, 0xFC, 0x2D, 0x01, 0x9C, 0x2F, 0xC7, 0x13, 0x61,
				0x00, 0xF0, 0x3B, 0x16, 0xCC, 0x25, 0xCF, 0x49, 0xC0 };

		int i;
		byte[] cat = new byte[256];
		byte[] cat1 = null;
		try {
			cat1 = (new String("no category")).getBytes(PALM_CHARSET);
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
	

		// open new database
		try {
			
			// write header
			for (i = 0; i < header.length; i++) {
				fp.write((byte) header[i]);
			}

			// write category-names
			Arrays.fill(cat, (byte) 0x00);
			System.arraycopy(cat1, 0, cat, 0, cat1.length);
			fp.write(cat, 0, 256);

			// write password information and record 1
			for (i = 0; i < data.length; i++) {
				fp.write((byte) data[i]);
			}

			fp.close();
		} catch (Exception e) {
			System.err.println("Caught Exception: " + e.getMessage());
		}
	}

	// entries --------------------------------------------------------
	/**
	 * This method adds an entry to the vector entries.
	 * 
	 * @param entry
	 *            Entry object
	 */
	public void addEntry(Entry entry) {
		entries.put(entry.getUniqueId(), entry);
	}

	/**
	 * This method removes an entry from the vector entries.
	 * 
	 * @param entry
	 *            Entry object
	 */
	public void removeEntry(Entry entry) {
		entries.remove(entry.getUniqueId());
	}

	/**
	 * This method returns the size of vector entries.
	 * 
	 * @return Size of vector entries
	 */
	public int getEntriesSize() {
		return entries.size();
	}

	/**
	 * This method returns the vector entries.
	 * 
	 * @return Vector entries
	 */

	public Map<Integer, Entry> getEntries() {
		return entries;
	}

	/**
	 * This method returns the enumeration of entries.
	 * 
	 * @return Iterator of entries TODO optimise this code, don't recalculate
	 *         this every time
	 */

	public Iterator<Entry> getElements() {
		if (DEBUG) {
			System.out.println("getElements() entriesKeySet: "
					+ entries.keySet());
		}
		
		return entries.values().iterator();
	}

	// categories -----------------------------------------------------
	/**
	 * This method returns a category name from the vector categories.
	 * 
	 * @param category
	 *            Index of category in vector categories
	 * 
	 * @return Category name
	 */
	public String getCategoryName(int category)
			throws ArrayIndexOutOfBoundsException {
		return (String) categories.get(category);
	}

	/**
	 * This method sets the vector categories to the specified vector.
	 * 
	 * @param myCategories
	 *            New category vector
	 */
	public void setCategories(Vector<String> myCategories) {
		categories = myCategories;

	}

	/**
	 * This method returns the vector categories.
	 * 
	 * @return Vector categories
	 */
	public Vector<String> getCategories() { 
		return categories;
	}
	
	public Vector<String> getDisplayCategories() { 
		@SuppressWarnings("unchecked")
		Vector<String> v = (Vector<String>)categories.clone();
		v.add(ALL_CATEGORY);
		return v;
	}

	// loadData -------------------------------------------------------
	/**
	 * This method loads a Keyring database and generates entry objects for each
	 * account.
	 * 
	 * @param filename
	 *            Keyring database
	 * 
	 */
	public void loadData(String filename) throws Exception {

		// read database
		File db = new File(filename);
		FileInputStream fp = new FileInputStream(db);
		loadData(fp);
	}

	public void loadData(FileInputStream fp) throws Exception {

		byte[] data;
		byte[] encrypted = null;
		int bufferSize = 100 * 1024;
		int entryLength;
		int pdbLength;
		int emptyTitle = 0;
		int start = 0;
		int len;
		int reallen;
		byte[] iv = null;
		String title = null;
		boolean entryError = false;

		// record entry descriptors
		int pdbOffset[]; // 4
		int pdbAttribute[]; // 1
		int pdbUniqueId[]; // 3

		// initialisation
		entries.clear();
		categories.clear();

		data = new byte[bufferSize];

		pdbLength = fp.read(data);

		if (pdbLength == bufferSize) {
			throw new Exception("File too large.");
		}

		fp.close();

		if (DEBUG) {
			System.out.println("\n========== loadData() ==========\n");
		}

		// read header
		pdbHeader = sliceBytes(data, 0, 78);
		pdbName = sliceString(data, 0, 32);
		pdbFlags = (int) sliceNumber(data, 32, 2);
		pdbVersion = (int) sliceNumber(data, 34, 2); // 12 byte time information
		pdbModNumber = sliceNumber(data, 48, 4);
		pdbAppInfoOffset = (int) sliceNumber(data, 52, 4);
		pdbSortInfoOffset = (int) sliceNumber(data, 56, 4);
		pdbType = sliceString(data, 60, 4);
		pdbCreator = new String(data, 64, 4); // 8 byte unknown
		pdbNumRecords = (int) sliceNumber(data, 76, 2);

		// check Keyring database format
		if (!(pdbVersion == 4 || pdbVersion == 5)) {
			throw new Exception("Wrong Keyring database format: " + pdbVersion);
		}

		// offsets
		pdbOffset = new int[pdbNumRecords];
		pdbAttribute = new int[pdbNumRecords];
		pdbUniqueId = new int[pdbNumRecords];

		for (int i = 0; i < pdbNumRecords; i++) {
			pdbOffset[i] = (int) sliceNumber(data, 78 + (i * 8), 4);
			pdbAttribute[i] = (int) (sliceNumber(data, 78 + 4 + (i * 8), 1));
			pdbUniqueId[i] = (int) sliceNumber(data, 78 + 4 + 1 + (i * 8), 3);

			if (DEBUG) {
				System.out.println(i + ": " + pdbOffset[i] + " / "
						+ pdbAttribute[i] + " / " + pdbUniqueId[i]);
			}
		}
		printPDBHeader(); // store db details in case of problems can report

		pdbCategories = sliceBytes(data, pdbAppInfoOffset, 276);

		// determine the category list
		for (int i = 0; i < 16; i++) {
			String categoryName = sliceString(data, pdbAppInfoOffset + 2
					+ (16 * i), 16);

			if (!categoryName.equals("")) {
				categories.add(categoryName);
			}
		}

		if (pdbVersion == 5) {
			

			byte[] salt = sliceBytes(data, pdbAppInfoOffset + 276, 8);
			int iter = (int) sliceNumber(data, pdbAppInfoOffset + 276 + 8, 2);
			int cipher = (int) sliceNumber(data,
					pdbAppInfoOffset + 276 + 8 + 2, 2);
			byte[] hash = sliceBytes(data, pdbAppInfoOffset + 276 + 8 + 2 + 2,
					8);

			// initialize crypto Object
			crypto = new Crypto(null, 5, salt, hash, iter, cipher);

			start = 0; // start with first record

			switch (cipher) {
			case 1:
				break; // triple des
			case 2:
				break; // aes 128 bit
			case 3:
				break; // aes 256 bit
			default:
				throw new Exception("No cipher not supported.");
			}
		}

		if (pdbVersion == 4) {
			if (pdbNumRecords <= 1) { // only password information
				//throw new Exception("No real data.");
			}

			recordZeroAttribute = pdbAttribute[0];
			recordZeroUniqueId = pdbUniqueId[0];
			recordZeroLength = pdbOffset[1] - pdbOffset[0];

			// load up password information (entry 0)
			crypto = new Crypto(sliceBytes(data, pdbOffset[0], pdbOffset[1]
					- pdbOffset[0]), 4);

			start = 1; // start with second record
		}

		// example (Keyring database format 4):
		// numberOfEntries = 4
		// entry 0 = password information
		// entry 1
		// entry 2
		// entry 3
		if (DEBUG)
			System.out.println("start: " + start + " pdbNumRecords: "
					+ pdbNumRecords);
		for (int i = start; i < pdbNumRecords; i++) {
			if (DEBUG)
				System.out.println("doing entry: " + i);
			entryError = false; // ok so far
			// check record attribute
			// if((pdbAttribute[i] & 0xF0) == 0x40) { // many (all that I have
			// seen) valid records fail this test, what is this for?
			// determine entry length
			if (i == pdbNumRecords - 1) {
				entryLength = pdbLength - pdbOffset[i];
			} else {
				entryLength = pdbOffset[i + 1] - pdbOffset[i];
			}

			 if(DEBUG) {
			 System.out.println("i=" + i + ": " + pdbOffset[i] + " / " +
			 entryLength);
			 }

			if (pdbVersion == 4) { // Keyring database format 4
				// title + \0 + encrypted data
				title = sliceString(data, pdbOffset[i], -1);
				if (DEBUG) System.out.println("TITLE length is: " + title.length() + " bytes is " + title.getBytes(Model.PALM_CHARSET).length);
				int titleLength = title.getBytes(Model.PALM_CHARSET).length;
				iv = null;
				try{ //throw new Exception ("debugging");
					encrypted = sliceBytes(data, pdbOffset[i] + titleLength + 1,
						entryLength - titleLength - 1);
				}catch (Exception sliceBytesException){
					entryError = true;
					String failingTitle = title != null || title.trim().length() > 0 ? title : "BLANK TITLE";
					sbErrors.append(withErrors ? ", " + failingTitle : failingTitle);
					sbErrors.append(" entry[" + (i + 1) +"]");
					withErrors = true;
					sliceBytesException.printStackTrace();
					encrypted = new byte[pdbOffset[i] + titleLength + 1];
					for (int n = 0; n < encrypted.length; n++){
						encrypted[n] = 0;
					}
				}
			}

			if (pdbVersion == 5) {
				// get length of field
				len = (int) sliceNumber(data, pdbOffset[i], 2);
				reallen = (len + 1) & ~1; // padding for next even address

				title = sliceString(data, pdbOffset[i] + 4, len);

				int ivlen = 8; // tripledes
				if (crypto.type == 2 || crypto.type == 3)
					ivlen = 16; // aes

				iv = sliceBytes(data, pdbOffset[i] + reallen + 4, ivlen);
				encrypted = sliceBytes(data,
						pdbOffset[i] + reallen + 4 + ivlen, entryLength
								- (reallen + 4 + ivlen));
			}

			if (!entryError){
				// Keyring: empty title possible
				if (title.equals("")) {
					title = "#" + (emptyTitle++);
				}
	
				// generate entry object
				Entry myEntry = new Entry(i, title, pdbAttribute[i] & 15,
						encrypted, crypto, pdbAttribute[i], pdbUniqueId[i],
						entryLength, iv);
				if (DEBUG)
					System.out.println("adding " + myEntry + " entry length: " + entryLength);
	
				entries.put(myEntry.uniqueId, myEntry);
	
				if (DEBUG) {
					System.out.println("added " + myEntry + " with uid "
							+ myEntry.uniqueId);
					System.out.println("added " + entries.get(myEntry.uniqueId));
	
				}
			}else {
				if (DEBUG) System.out.println("ignored " + title);
			}

		}
	}

	// saveData -------------------------------------------------------
	/**
	 * This method calls the saveData method according to database version
	 * (pdbVersion).
	 * 
	 * @param filename
	 *            Keyring database
	 */
	public void saveData(String filename) throws Exception {
		if (DEBUG) {
			System.out.println("saveData");
		}

		switch (pdbVersion) {
		case 4:
			saveData_4(filename);
			break;
		case 5:
			saveData_5(filename);
			break;
		}
	}

	/**
	 * This method calls the saveData method according to database version
	 * (pdbVersion).
	 * 
	 * @param fileOutputStream
	 *            Keyring database
	 */
	public void saveData(FileOutputStream fileOutputStream) throws Exception {
		if (DEBUG) {
			System.out.println("saveData from fileOutputStream, pdbVersion: "
					+ pdbVersion);
		}

		switch (pdbVersion) {
		case 4:
			saveData_4(fileOutputStream);
			break;
		case 5:
			saveData_5(fileOutputStream);
			break;
		}
	}

	/**
	 * This method saves all entries in the specified database (Database format
	 * 4).
	 * 
	 * @param filename
	 *            Keyring database
	 */
	public void saveData_4(String filename) throws Exception {
		File db;
		FileOutputStream fp;

		// open new database
		db = new File(filename);
		fp = new FileOutputStream(db);
		saveData_4(fp);
	}

	private void saveData_4(FileOutputStream fp) throws Exception {
		if (DEBUG) {
			System.out.println("saveData_4 fos");
			if (DEBUG) {
				System.out.println("numRecords: " + entries.size());
			}
		}
		int offset = 0;
		pdbAppInfoOffset = 78 + 8 * entries.size() + 2 + 8; // + 8 for
															// recordZero
		pdbNumRecords = entries.size() + 1;
		offset = pdbAppInfoOffset + 276;

		// write header
		fp.write(pdbHeader, 0, 52);
		fp.write(numberToByte(pdbAppInfoOffset, 4), 0, 4);
		fp.write(pdbHeader, 56, 20);
		fp.write(numberToByte(pdbNumRecords, 2), 0, 2); // + 1 for recordZero

		// write offset recordZero
		fp.write(numberToByte(offset, 4), 0, 4);
		fp.write(numberToByte(recordZeroAttribute, 1), 0, 1);
		fp.write(numberToByte(recordZeroUniqueId, 3), 0, 3);

		offset += recordZeroLength;

		// write offsets
		// for(Enumeration e = entries.elements(); e.hasMoreElements(); ) {
		for (Iterator<?> e = getElements(); e.hasNext();) {
			Entry entry = (Entry) e.next();

			fp.write(numberToByte(offset, 4), 0, 4);
			fp.write(numberToByte(entry.getAttribute(), 1), 0, 1); // category
			fp.write(numberToByte(entry.uniqueId, 3), 0, 3);

			if (DEBUG) {
				System.out.println("saveData4: " + offset + ", "
						+ entry.getAttribute() + ", " + entry.uniqueId);
			}

			offset += entry.getRecordLength();
		}

		fp.write((int) 0x0000);
		fp.write((int) 0x0000);

		// write categories
		updateCategories(); // Categories in Gui.java are editable
		fp.write(pdbCategories, 0, 276);

		// write password information
		fp.write(crypto.recordZero);

		// write records
		// for(Enumeration e = entries.elements(); e.hasMoreElements(); ) {
		for (Iterator<?> e = getElements(); e.hasNext();) {

			Entry entry = (Entry) e.next();
			fp.write(entry.getTitle(false).getBytes(Model.PALM_CHARSET));
			//fp.write(entry.getTitle().getBytes(Model.ANDROID_CHARSET));
			fp.write(0x00);
			fp.write(entry.getEncrypted());
		}
		fp.flush();// new
		fp.close();
	}

	/**
	 * This method saves all entries in the specified database (Database format
	 * 5).
	 * 
	 * @param filename
	 *            Keyring database
	 */
	public void saveData_5(String filename) throws Exception {
		File db = new File(filename);
		FileOutputStream fp = new FileOutputStream(db);
		saveData_5(fp);
	}

	private void saveData_5(FileOutputStream fp) throws Exception {
		if (DEBUG) {
			System.out.println("saveData_5 fos");
			if (DEBUG) {
				System.out.println("numRecords: " + entries.size());
			}
		}

		int offset = 0;

		pdbAppInfoOffset = 78 + 8 * entries.size() + 2;
		pdbNumRecords = entries.size();
		offset = pdbAppInfoOffset + 276 + 20; // salt hash type

		// write header
		fp.write(pdbHeader, 0, 52);
		fp.write(numberToByte(pdbAppInfoOffset, 4), 0, 4);
		fp.write(pdbHeader, 56, 20);
		fp.write(numberToByte(pdbNumRecords, 2), 0, 2); // + 1 for recordZero

		// write offsets
		// for(Enumeration e = entries.elements(); e.hasMoreElements(); ) {
		for (Iterator<?> e = getElements(); e.hasNext();) {

			Entry entry = (Entry) e.next();

			fp.write(numberToByte(offset, 4), 0, 4);
			fp.write(numberToByte(entry.getAttribute(), 1), 0, 1); // category
			fp.write(numberToByte(entry.uniqueId, 3), 0, 3);

			offset += entry.getRecordLength();
		}

		fp.write((int) 0x0000);
		fp.write((int) 0x0000);

		// write categories
		updateCategories(); // Categories in Gui.java are editable
		fp.write(pdbCategories, 0, 276);

		// write SALT HASH TYPE (db_format.txt)
		fp.write(crypto.salt);
		fp.write(numberToByte(crypto.iter, 2));
		fp.write(numberToByte(crypto.type, 2));
		fp.write(crypto.hash);

		// write records
		// for(Enumeration e = entries.elements(); e.hasMoreElements(); ) {
		for (Iterator<?> e = getElements(); e.hasNext();) {

			Entry entry = (Entry) e.next();

			fp.write(convertStringToField(entry.getTitle(false), 0));
			fp.write(entry.getIv());
			fp.write(entry.getEncrypted());
		}

		fp.close();
	}

//	// convertDatabase ------------------------------------------------
//	/**
//	 * This method calls convertTo method according to database format.
//	 * 
//	 * @param from
//	 *            Database format of loaded database
//	 * @param to
//	 *            Convert to database format
//	 * @param filename
//	 *            New keyring database
//	 * @param pw
//	 *            Password of new database
//	 * @param type
//	 *            Cipher type (for database format 5)
//	 * @param iter
//	 *            Iterations (for database format 5)
//	 */
//	public void convertDatabase(int from, int to, String filename, char[] pw,
//			int type, int iter) throws Exception {
//		switch (to) {
//		case 4:
//			convertTo_4(from, filename, pw);
//			break;
//		case 5:
//			convertTo_5(from, filename, pw, type, iter);
//			break;
//		default:
//			return;
//		}
//	}
	
	/**
	 * This method calls convertTo method according to database format.
	 * 
	 * @param from
	 *            Database format of loaded database
	 * @param to
	 *            Convert to database format
	 * @param fileOutputStream
	 *            New keyring database
	 * @param pw
	 *            Password of new database
	 * @param type
	 *            Cipher type (for database format 5)
	 * @param iter
	 *            Iterations (for database format 5)
	 */
	public void convertDatabase(int from, int to, FileOutputStream fileOutputStream, char[] pw,
			int type, int iter) throws Exception {
		switch (to) {
		case 4:
			convertTo_4(from, fileOutputStream, pw);
			break;
		case 5:
			convertTo_5(from, fileOutputStream, pw, type, iter);
			break;
		default:
			return;
		}
	}
	
	
	/**
	 * This method converts all entries to database format 4 and saves to
	 * specified database.
	 * 
	 * @param from
	 *            Database format of loaded database
	 * @param filename
	 *            New keyring database
	 * @param pw
	 *            Password of new database
	 */

		public void convertTo_4(int from, String filename, char[] pw)
		throws Exception {
			// Keyring database format 4
			File db;
			FileOutputStream fp;
			db = new File(filename);
			fp = new FileOutputStream(db);
			convertTo_4(from,fp,pw);

	}

	/**
	 * This method converts all entries to database format 4 and saves to
	 * specified database.
	 * 
	 * @param from
	 *            Database format of loaded database
	 * @param filename
	 *            New keyring database
	 * @param pw
	 *            Password of new database
	 */

		
		public void convertTo_4(int from, FileOutputStream fp, char[] pw)
		throws Exception {

		// Keyring database format 4
		//File db;
		//FileOutputStream fp;
		int i;
		int offset = 0;
		byte[] recordzero = new byte[20];
		byte[] pass = new byte[pw.length];
		byte[] salt = new byte[4];
		byte[] record = null;
		byte[] ciphertext = null;
		Crypto converted = null;

		// open new database
	//	db = new File(filename);
	//	fp = new FileOutputStream(db);

		pdbAppInfoOffset = 78 + 8 * entries.size() + 2 + 8; // + 8 for
															// recordZero
		pdbNumRecords = entries.size() + 1;
		offset = pdbAppInfoOffset + 276;

		// create record zero
		Arrays.fill(recordzero, (byte) 0);

		// Keyring supports passwords of up to 40 characters
		if (pw.length > 40) {
			throw new Exception("Password too long.");
		}

		// convert password from char to byte
		for (i = 0; i < pw.length; i++) {
			pass[i] = (byte) (0xff & pw[i]);
		}

		// get salt
		switch (from) {
		case 4: // convert from 4 to 4 (changing password)
			for (i = 0; i < 4; i++) {
				salt[i] = crypto.recordZero[i]; // get old salt
				recordzero[i] = crypto.recordZero[i];
			}

			break;

		case 5:
			// take first 4 bytes from format 5 salt
			for (i = 0; i < 4; i++) {
				salt[i] = crypto.salt[i];
				recordzero[i] = crypto.salt[i];
			}

			break;
		}

		// get hash from password
		byte[] hash = crypto.checkPasswordHash_4(salt, pass);

		// fill recordzero
		for (i = 0; i < 16; i++) {
			recordzero[i + 4] = hash[i];
		}

		// new crypto object
		converted = new Crypto(recordzero, 4);
		converted.setPassword(pw);

		Arrays.fill(pw, (char) 0);
		Arrays.fill(pass, (byte) 0);

		// write header
		fp.write(pdbHeader, 0, 34);
		fp.write(numberToByte(4, 2), 0, 2); // write new version
		fp.write(pdbHeader, 36, 16);
		fp.write(numberToByte(pdbAppInfoOffset, 4), 0, 4);
		// fp.write(pdbHeader, 56, 20);
		fp.write(pdbHeader, 56, 4); // sort info offset
		fp.write(applcreator4.getBytes(Model.PALM_CHARSET)); // type, creator
		fp.write(pdbHeader, 68, 8); // sort info offset
		fp.write(numberToByte(pdbNumRecords, 2), 0, 2); // + 1 for recordZero

		// write offset recordZero
		fp.write(numberToByte(offset, 4), 0, 4);
		fp.write(numberToByte(80, 1), 0, 1);
		fp.write(numberToByte(0, 3), 0, 3);
		offset += 20;

		// write offsets
		for (Iterator<?> e = getElements(); e.hasNext();) {
			Entry entry = (Entry) e.next();

			fp.write(numberToByte(offset, 4), 0, 4);
			fp.write(numberToByte(entry.getAttribute(), 1), 0, 1); // category
			fp.write(numberToByte(entry.uniqueId, 3), 0, 3);

			// decrypt and encrypt records
			record = Model.toRecordFormat4(entry.getAccount() + "\0"
					+ entry.getPassword() + "\0" + entry.getNotes() + "\0");

			ciphertext = converted.encrypt(record);

			entry.setEncrypted(sliceBytes(ciphertext, 16,
					ciphertext.length - 16)); // 16 byte iv ignored

			offset += entry.getTitle().length() + 1
					+ entry.getEncrypted().length;
		}

		fp.write((int) 0x0000);
		fp.write((int) 0x0000);

		// write categories
		updateCategories();
		fp.write(pdbCategories, 0, 276);

		// write password information
		fp.write(recordzero);

		// write records
		for (Iterator<?> e = getElements(); e.hasNext();) {
			Entry entry = (Entry) e.next();

			fp.write(entry.getTitle().getBytes(Model.PALM_CHARSET));
			fp.write(0x00);
			fp.write(entry.getEncrypted());
		}

		converted = null;

		fp.close();
	}

		/**
		 * This method converts all entries to database format 5 and saves to
		 * specified database.
		 * 
		 * @param from
		 *            Database format of loaded database
		 * @param filename
		 *            New keyring database
		 * @param pw
		 *            Password of new database
		 * @param type
		 *            Cipher type (for database format 5)
		 * @param iter
		 *            Iterations (for database format 5)
		 */
		public void convertTo_5(int from, String filename, char[] pw, int type,
				int iter) throws Exception {
			// Keyring database format 5
			File db;
			FileOutputStream fp;
			db = new File(filename);
			fp = new FileOutputStream(db);
			convertTo_5(from,fp,pw,type,iter);
		}
	/**
	 * This method converts all entries to database format 5 and saves to
	 * specified database.
	 * 
	 * @param from
	 *            Database format of loaded database
	 * @param filename
	 *            New keyring database
	 * @param pw
	 *            Password of new database
	 * @param type
	 *            Cipher type (for database format 5)
	 * @param iter
	 *            Iterations (for database format 5)
	 */
	public void convertTo_5(int from, FileOutputStream fp, char[] pw, int type,
			int iter) throws Exception {
		// Keyring database format 5
		//File db;
		//FileOutputStream fp;
		int i;
		int offset = 0;
		byte[] record = null;
		byte[] ciphertext = null;
		Crypto converted = null;
		int[] cipherlen = { 0, 24, 16, 32 }; // keylength in byte
		byte[] pass = new byte[pw.length];
		byte[] salt = new byte[8];
		int index;

		for (i = 0; i < pw.length; i++) {
			pass[i] = (byte) (0xFF & pw[i]);
		}

		// open new database
		//db = new File(filename);
		//fp = new FileOutputStream(db);

		pdbAppInfoOffset = 78 + 8 * entries.size() + 2;
		pdbNumRecords = entries.size();
		offset = pdbAppInfoOffset + 276 + 20; // salt hash type

		switch (from) {
		case 4:
			for (i = 0; i < 4; i++) {
				salt[i] = crypto.recordZero[i];
				salt[i + 4] = crypto.recordZero[i];
			}
			break;

		case 5:
			for (i = 0; i < 8; i++) {
				salt[i] = crypto.salt[i];
			}
			break;
		}

		// PKCS#5 PBKDF2
		// Key Derivation function
		byte[] deskey = crypto.pbkdf2(pass, salt, iter, cipherlen[type]);

		// set odd parity
		if (type == 1) { // TripleDES
			for (i = 0; i < 24; i++) {
				index = (int) (0xff & deskey[i]);
				deskey[i] = (byte) Crypto.odd_parity[index];
			}
		}

		// SHA1
		byte[] digest = crypto.getMessageDigest(deskey, salt);

		byte[] hash = Model.sliceBytes(digest, 0, 8);

		converted = new Crypto(null, 5, salt, hash, iter, type);
		converted.setPassword(pw);

		Arrays.fill(pw, (char) 0);
		Arrays.fill(pass, (byte) 0);

		// write header
		fp.write(pdbHeader, 0, 34);
		fp.write(numberToByte(5, 2), 0, 2); // write new version
		fp.write(pdbHeader, 36, 16);
		fp.write(numberToByte(pdbAppInfoOffset, 4), 0, 4); // application info
															// offset
		// fp.write(pdbHeader, 56, 20);
		fp.write(pdbHeader, 56, 4); // sort info offset
		fp.write(applcreator5.getBytes(Model.PALM_CHARSET)); // type, creator
		fp.write(pdbHeader, 68, 8); // sort info offset

		fp.write(numberToByte(pdbNumRecords, 2), 0, 2); // + 1 for recordZero

		// write offsets
		// for(Enumeration e = getElements(); e.hasMoreElements(); ) {
		for (Iterator<?> e = getElements(); e.hasNext();) {
			Entry entry = (Entry) e.next();
			// Entry entry = (Entry)e.nextElement();

			fp.write(numberToByte(offset, 4), 0, 4);
			fp.write(numberToByte(entry.getAttribute(), 1), 0, 1); // category
			fp.write(numberToByte(entry.uniqueId, 3), 0, 3);

			// decrypt and encrypt records
			record = Model.toRecordFormat5(entry.getAccount(),
					entry.getPassword(), entry.getNotes());

			ciphertext = converted.encrypt(record);

			// extract iv
			int ivlen = 8;
			if (type != 1) { // TripleDES
				ivlen = 16; // AES128, AES256
			}

			entry.setIv(sliceBytes(ciphertext, 0, ivlen));
			entry.setEncrypted(Model.sliceBytes(ciphertext, 16,
					ciphertext.length - 16));

			offset += (Model.convertStringToField(entry.getTitle(), 0)).length
					+ ivlen + entry.getEncrypted().length;
		}

		fp.write((int) 0x0000);
		fp.write((int) 0x0000);

		// write categories
		updateCategories();
		fp.write(pdbCategories, 0, 276);

		// write SALT HASH TYPE (db_format.txt)
		fp.write(salt);
		fp.write(numberToByte(iter, 2));
		fp.write(numberToByte(type, 2));
		fp.write(hash);

		// write records
		// for(Enumeration e = getElements(); e.hasMoreElements(); ) {
		for (Iterator<?> e = getElements(); e.hasNext();) {
			Entry entry = (Entry) e.next();
			// Entry entry = (Entry)e.nextElement();

			fp.write(convertStringToField(entry.getTitle(), 0));
			fp.write(entry.getIv());
			fp.write(entry.getEncrypted());
		}

		converted = null;

		fp.close();
	}
//
	// toRecordFormat4 ------------------------------------------------
	/**
	 * This method adds todays date (DateType format) to decrypted data (for
	 * database format 4).
	 * 
	 * @param data
	 *            Example: Account + \0 + Password + \0 + Notes + \0
	 * 
	 * @return data + todays date in datetype format
	 * @throws UnsupportedEncodingException 
	 */
	public static byte[] toRecordFormat4(String data) throws UnsupportedEncodingException {
		if (DEBUG) System.out.println("toRecordFormat4 gets: " + data);
		byte[] today = getDateType();
		byte[] buffer = data.getBytes(Model.PALM_CHARSET);
		byte[] result = new byte[buffer.length + 2];

		System.arraycopy(buffer, 0, result, 0, buffer.length);
		result[buffer.length] = today[1];
		result[buffer.length + 1] = today[0];

		return result;
	}

	// toRecordFormat5 ------------------------------------------------
	/**
	 * This method adds todays date (DateType format) to decrypted data (for
	 * database format 5).
	 * 
	 * @param account
	 *            Entry account
	 * @param password
	 *            Entry password
	 * @param notes
	 *            Entry notes
	 * 
	 * @return decrypted data in database format 5
	 * @throws UnsupportedEncodingException 
	 */
	public static byte[] toRecordFormat5(String account, String password,
			String notes) throws UnsupportedEncodingException {
		// Format:
		// field (account)
		// field (password)
		// field (notes)
		// field (datetype)
		// 0xff
		// 0xff
		// random padding to multiple of 8 bytes
		byte[] datetype = { 0x00, 0x02, 0x03, 0x00, 0x00, 0x00 };

		byte[] field1 = account.getBytes(Model.PALM_CHARSET);
		byte[] field2 = password.getBytes(Model.PALM_CHARSET);
		byte[] field3 = notes.getBytes(Model.PALM_CHARSET);

		int lenField1 = field1.length;
		int lenField2 = field2.length;
		int lenField3 = field3.length;

		if (lenField1 != 0) {
			field1 = convertStringToField(account, 1);
			lenField1 = field1.length;
		}

		if (lenField2 != 0) {
			field2 = convertStringToField(password, 2);
			lenField2 = field2.length;
		}

		if (lenField3 != 0) {
			field3 = convertStringToField(notes, 255);
			lenField3 = field3.length;
		}

		byte[] now = getDateType();
		datetype[4] = now[1];
		datetype[5] = now[0];

		int padding = (lenField1 + lenField2 + lenField3 + 6 + 2) % 8;
		byte[] result = new byte[lenField1 + lenField2 + lenField3 + 6 + 2
				+ padding];
		Arrays.fill(result, (byte) 0xff);

		if (lenField1 != 0) {
			System.arraycopy(field1, 0, result, 0, lenField1);
		}

		if (lenField2 != 0) {
			System.arraycopy(field2, 0, result, lenField1, lenField2);
		}

		if (lenField3 != 0) {
			System.arraycopy(field3, 0, result, lenField1 + lenField2,
					lenField3);
		}

		System.arraycopy(datetype, 0, result,
				lenField1 + lenField2 + lenField3, 6);

		return result;
	}
//
	/**
	 * This method converts a string in the format used by database format 5
	 * (Field).
	 * 
	 * @param field
	 *            Text
	 * @param label
	 *            Label information (account=1, password=2, notes=255)
	 * 
	 * @return Field
	 * @throws UnsupportedEncodingException 
	 */
	public static byte[] convertStringToField(String field, int label) throws UnsupportedEncodingException {
		// Format:
		// 2 byte length of field
		// 1 byte label
		// 1 byte 0x00
		// data
		// 0/1 padding for next even address
		byte[] buffer = field.getBytes(Model.PALM_CHARSET);
		int padding = 0;
		int len = buffer.length;

		if ((len % 2) == 1) {
			padding = 1;
		}

		byte[] result = new byte[4 + len + padding];
		Arrays.fill(result, (byte) 0);

		System.arraycopy(numberToByte(len, 2), 0, result, 0, 2);
		System.arraycopy(numberToByte(label, 1), 0, result, 2, 1);
		result[3] = (byte) 0x00;
		System.arraycopy(buffer, 0, result, 4, len);

		return result;
	}

	// getNewUniqueId -------------------------------------------------
	/**
	 * This method searches the entries for the highest id.
	 * 
	 * @return New unique id
	 */
	public int getNewUniqueId() {
		int id = 0;

		// for(Enumeration e = getElements(); e.hasMoreElements(); ) {
		//
		//
		// Entry entry = (Entry)e.nextElement();
		for (Iterator<?> e = getElements(); e.hasNext();) {
			Entry entry = (Entry) e.next();

			if (entry.getUniqueId() > id) {
				id = entry.getUniqueId();
			}
		}

		id = id + 1;

		return (id);
	}

//	// saveEntriesToFile ----------------------------------------------
//	/**
//	 * This method saves all entries to a csv file.
//	 * 
//	 * @param filename
//	 *            CSV file
//	 */
//	public void saveEntriesToFile(String filename) throws Exception {
//		csvFilename = filename;
//
//		File outputFile = new File(csvFilename);
//		FileWriter out = new FileWriter(outputFile);
//
//		// for(Enumeration e = getElements(); e.hasMoreElements(); ) {
//		// Entry entry = (Entry)e.nextElement();
//
//		for (Iterator<?> e = getElements(); e.hasNext();) {
//			Entry entry = (Entry) e.next();
//
//			String buffer = "" + '"' + entry.getEntryId() + '"' + csvSeparator
//					+ '"' + categories.elementAt(entry.getCategory()) + '"'
//					+ csvSeparator + '"' + entry.getTitle() + '"'
//					+ csvSeparator + '"' + entry.getAccount() + '"'
//					+ csvSeparator + '"' + entry.getPassword() + '"'
//					+ csvSeparator + '"' + entry.getDate() + '"' + "\n";
//
//			out.write(buffer.toCharArray());
//		}
//
//		out.close();
//	}

//	public void setCsvSeparator(char sep) {
//		Model.csvSeparator = sep;
//	}
//
//	public void setCsvFilename(String filename) {
//		Model.csvFilename = filename;
//	}
//
//	public String getCsvFilename() {
//		return csvFilename;
//	}

	// tools ----------------------------------------------------------
	/**
	 * This method converts a long into a byte array.
	 * 
	 * @param number
	 *            Number
	 * @param len
	 *            Size of byte array
	 * 
	 * @return Byte array representation of number
	 */
	public static byte[] numberToByte(long number, int len) {
		int i, shift;
		byte[] buffer = new byte[len];

		for (i = 0, shift = ((len - 1) * 8); i < len; i++, shift -= 8) {
			buffer[i] = (byte) (0xFF & (number >> shift));
		}

		return buffer;
	}

	/**
	 * This method converts a byte to int.
	 * 
	 * @param b
	 *            Byte
	 * 
	 * @return Int representation of Byte
	 */
	public static int unsignedByteToInt(byte b) {
		return (int) (b & 0xFF);
	}

	/**
	 * This method slices a byte array from an byte array.
	 * 
	 * @param data
	 *            Byte array
	 * @param start
	 *            Index to start from
	 * @param length
	 *            Length of byte array to slice out
	 * 
	 * @return Byte array
	 */
	public static byte[] sliceBytes(byte[] data, int start, int length) {
		byte[] bytes = new byte[length];

		for (int i = 0; i < length; i++) {
			bytes[i] = data[start + i];
		}

		return bytes;
	}

	/**
	 * This method slices a byte array from an byte array and converts it to a
	 * long.
	 * 
	 * @param data
	 *            Byte array
	 * @param start
	 *            Index to start from
	 * @param length
	 *            Length of byte array to slice out
	 * 
	 * @return Long representation of the byte array
	 */
	public static long sliceNumber(byte[] data, int start, int length) {
		long value = 0, factor = 1;

		for (int i = 0; i < length; i++) {
			value += (long) (unsignedByteToInt(data[start + length - (i + 1)]) * factor);
			factor *= 256;
		}

		return value;
	}

	/**
	 * This method slices a byte array from an byte array and converts it to a
	 * string.
	 * 
	 * @param data
	 *            Byte array
	 * @param start
	 *            Index to start from
	 * @param length
	 *            Length of byte array to slice out
	 * 
	 * @return String representation of the byte array
	 * @throws UnsupportedEncodingException 
	 * @throws CharacterCodingException 
	 */
	public static String sliceString(byte[] data, int start, int length) throws UnsupportedEncodingException{

		int realLength = 0;

		if (length == -1) {
			// no specific max length (make it to the end of the array)
			length = data.length - start;
		}
		char terminator = 0;
		while (realLength < length && data[start + realLength] != terminator ) {
			realLength++;
		}

		return new String((byte[]) data, start, realLength,Model.PALM_CHARSET);
	}
	
	
//	/**
//	 * not used
//	 */
//	public static void printByteArray(String info, byte[] buffer) {
//		System.out.print("printByteArray " + info + " (" + buffer.length
//				+ "): ");
//		for (int i = 0; i < buffer.length; i++) {
//			System.out.print((int) (buffer[i] & 0xFF) + " ");
//		}
//		System.out.println();
//	}
//
//	/**
//	 * not used
//	 */
//	public static void printHexByteArray(String info, byte[] buffer) {
//		char[] hexNumbers = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
//				'a', 'b', 'c', 'd', 'e', 'f' };
//		int zahl, rest;
//
//		System.out.println("printHexByteArray " + info + " (" + buffer.length
//				+ "): ");
//
//		for (int i = 0; i < buffer.length; i++) {
//
//			zahl = (int) (buffer[i] & 0xFF) / 16;
//			rest = (int) (buffer[i] & 0xFF) % 16;
//
//			System.out.print("" + hexNumbers[zahl] + "" + hexNumbers[rest]
//					+ " ");
//		}
//
//		System.out.println();
//	}

	// ----------------------------------------------------------------
	// private --------------------------------------------------------
	// ----------------------------------------------------------------

	/*
	 * private static void printUsage() { System.err.println("Usage:");
	 * System.err.println("View entries: java Model database.pdb password");
	 * System.err.println("   Add entry: ... -n title account password");
	 * System.err.println("  Edit entry: ... -e id title account passwort");
	 * System.err.println("Delete entry: ... -d id"); }
	 */

	/**
	 * not used
	 */
	public void printPDBHeader() {

		sbErrors.append("PDB Name: " + pdbName);
		sbErrors.append("; PDB Flags: " + pdbFlags);
		sbErrors.append("; PDB Version: " + pdbVersion);
		sbErrors.append("; PDB Modification Number: " + pdbModNumber);
		sbErrors.append("; PDB AppInfoOffset: " + pdbAppInfoOffset);
		sbErrors.append("; PDB SortInfoOffset: " + pdbSortInfoOffset);
		sbErrors.append("; PDB Type: " + pdbType);
		sbErrors.append("; PDB Creator: " + pdbCreator);
		sbErrors.append("; Charset: " + PALM_CHARSET);
		sbErrors.append("; PDB NumberOfRecords: " + (pdbNumRecords -1 ) + "; Failed to import:  ");
	
	}

//	/**
//	 * not used 
//	 * @throws UnsupportedEncodingException 
//	 */
//	// private void printEntries() {
//	public void printEntries() throws UnsupportedEncodingException {
//
//		int i = 0;
//
//		for (Enumeration<String> c = categories.elements(); c.hasMoreElements();) {
//			String help = (String) c.nextElement();
//
//			System.out.println("Category " + (i++) + ": " + help);
//		}
//		System.out.println();
//
//		for (Iterator<?> e = getElements(); e.hasNext();) {
//			Entry entry = (Entry) e.next();
//
//			System.out.println(entry.getInfo());
//		}
//		System.out.println();
//	}

	// DateType -------------------------------------------------------
	/**
	 * This method return todays date in DateType format.
	 * 
	 * @return Todays date in DateType format (byte[2])
	 */
	private static byte[] getDateType() {
		int day, month, year;
		int[] intResult = new int[2];
		byte[] byteResult = new byte[2];

		Calendar rightNow = new GregorianCalendar();

		day = rightNow.get(Calendar.DAY_OF_MONTH);
		month = rightNow.get(Calendar.MONTH) + 1; // Calender month from 0 to 11
		year = rightNow.get(Calendar.YEAR) - 1904; // DateType year since 1904

		day = (day & 0x1F); // 5 bit
		month = (month & 0x0F); // 4 bit
		year = (year & 0x7F); // 7 bit

		// DateType (2 bytes): 7 bit year, 4 bit month, 5 bit day
		intResult[0] = day | ((month & 0x07) << 5);
		intResult[1] = (year << 1) | ((month & 0x08) >> 3);

		// System.out.println(intResult[1] + " " + intResult[0]);
		byteResult[0] = (byte) intResult[0];
		byteResult[1] = (byte) intResult[1];

		return byteResult;
	}

	// updateCategories - saveData()
	/**
	 * This method updates the categories in variable pdbCategories according to
	 * vector categories.
	 * @throws UnsupportedEncodingException 
	 */
	private void updateCategories(Vector<String> categories) throws UnsupportedEncodingException {
		byte[] cat = new byte[16];
		// pdbCategories = initialisePdbCategories();
		int index = 0;

		for (Enumeration<String> c = categories.elements(); c.hasMoreElements();) {
			String strCategory = (String) c.nextElement();
			byte[] temp = strCategory.getBytes(Model.PALM_CHARSET);

			// resize to 16 byte
			for (int i = 0; i < 16; i++) {
				if (i < temp.length)
					cat[i] = temp[i];
				else
					cat[i] = 0x00;
			}

			// overwrite old categories
			System.arraycopy(cat, 0, pdbCategories, 2 + (index * 16), 16);
			index++;
		}
		for (int j = index; j < 16; j++) {
			String strCategory = "";
			byte[] temp = strCategory.getBytes(Model.PALM_CHARSET);
		

			// resize to 16 byte
			for (int i = 0; i < 16; i++) {
				if (i < temp.length)
					cat[i] = temp[i];
				else
					cat[i] = 0x00;
			}

			// overwrite old categories
			System.arraycopy(cat, 0, pdbCategories, 2 + (j * 16), 16);
		}
	}

	private void updateCategories() throws UnsupportedEncodingException {
		updateCategories(categories);
	}

	private void reassignEntryCategories(int index, int newIndex) {
		Iterator<Entry> i = entries.values().iterator();
		Entry entry = null;
		while (i.hasNext()) {
			entry = (Entry) i.next();
			if (entry.getCategory() == index) {
				entry.setCategory(newIndex);
				if (DEBUG)
					System.out.println(entry + " has index "
							+ entry.getCategory() + "set to "
							+ newIndex);

			} else if (entry.getCategory() > index) {

				entry.setCategory(entry.getCategory() - 1);
				if (DEBUG)
					System.out.println(entry + " has index "
							+ entry.getCategory() + "set to - 1 ");

			}
		}

	}
	
	public void reAssignCategory(String fromCategory, String toCategory) throws CategoryException, UnsupportedEncodingException {
		int index = categories.indexOf(fromCategory);
		if (index == UNFILED_CATEGORY) {
			throw new CategoryException("Cannot remove "
					+ categories.get(index));// TODO lookup string value
		}
		int toIndex = categories.indexOf(toCategory);
		reassignEntryCategories(index, toIndex);
		categories.remove(index);
		updateCategories(categories);

	}
	
	public void addCategory(String category) throws CategoryNameTooLongException, TooManyCategoriesException, UnsupportedEncodingException{

		if (category == null || category.trim().length() == 0 || categories.contains(category)){
			return;
		} 
		if (category.length() > CATEGORY_MAXLENGTH){
			throw new CategoryNameTooLongException("category name exceeds maxlength of " + CATEGORY_MAXLENGTH);//TODO lookup
		}
		if (categories.size() == MAX_NUM_CATEGORIES){
			throw new TooManyCategoriesException("Only " + MAX_NUM_CATEGORIES + " supported");//TODO lookup
		}
		categories.add(category);
		updateCategories(categories);
	}

	public void removeCategory(String category) throws CategoryException, UnsupportedEncodingException {

		int index = categories.indexOf(category);
		if (index == UNFILED_CATEGORY) {
			throw new CategoryException("Cannot remove "
					+ categories.get(index));// TODO lookup string value
		}

		reassignEntryCategories(index, UNFILED_CATEGORY);
		categories.remove(index);
		updateCategories(categories);

	}

	public void updateCategory(String category, int index)
			throws CategoryException, UnsupportedEncodingException {
		// TODO - max size category 15
		if (category == null) {
			category = "";
		}
		if (category.trim().equals("")) {

			if (index == UNFILED_CATEGORY) {
				throw new CategoryException("Cannot remove "
						+ categories.get(index));// TODO lookup string value
			} else {
				removeCategory((String) categories.get(index));
				return;
			}
		}
		categories.set(index, category);

	}

	public Entry createEntry(char[] title, int category, char[] account,
			char[] password, char[] notes) {
		Entry myEntry = null;
		byte[] record = null;
		byte[] ciphertext;
		int recordLength = 0;
		int newEntryId = getEntriesSize() + 1;
		int ivLen = 8; // for TripleDES
		int id; // unique id

		try {
			// new entry
			if (account != null) {
				// set record format & IV length
				switch (getPdbVersion()) {
				case 4:
					record = Model.toRecordFormat4( // account + password +
													// notes
							(new String(account)) + "\0"
									+ (new String(password)) + "\0"
									+ (new String(notes)) + "\0");
					break;
				case 5:
					record = Model.toRecordFormat5((new String(account)),
							(new String(password)), (new String(notes)));

					if (crypto.getType() != 1) { // TripleDES
						ivLen = 16; // AES128, AES256
					}

					break;
				}

				// encrypt record
				//System.out.println("record is: " + record);
				ciphertext = crypto.encrypt(record);

				int len = title.length + ciphertext.length - 16;
				switch (getPdbVersion()) {
				case 4:
					recordLength = len + 1;
					break;
				case 5:
					recordLength = len + 4 + (len % 2) + ivLen;
					break;
				}

				// get new unique id
				id = getNewUniqueId();

				// new entry object
				myEntry = new Entry(
						newEntryId,
						new String(title),
						category,
						Model.sliceBytes(ciphertext, 16, ciphertext.length - 16),
						crypto, category | 0x40, id, recordLength, Model
								.sliceBytes(ciphertext, 0, ivLen)); 

				// register new entry to vector entries
				addEntry(myEntry);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return myEntry;
	}
	
	private int getByteArrayLength(char[] c) {
		int n = -1;
		try {
			n = new String(c).getBytes(Model.PALM_CHARSET).length;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return n;
	}
	
	public void updateEntry(int id, char[] title, char[] account,
			char[] password, char[] notes, int categoryIndex) {
		byte[] record = null;
		byte[] ciphertext;
		int recordLength = 0;
		int ivLen = 8; // for TripleDES

		try {

			Entry myEntry = null;
			Iterator<?> i = getEntries().values().iterator();
			boolean found = false;
			while (i.hasNext() && !found) { // TODO call by key
				myEntry = (Entry) i.next();
				if (myEntry.getUniqueId() == id) {
					found = true;
				} else {
					myEntry = null;
				}
			}

			// set record format & IV length
			switch (getPdbVersion()) {
			case 4:
				record = Model.toRecordFormat4((new String(account)) + "\0"
						+ // account + password + notes
						(new String(password)) + "\0" + (new String(notes))
						+ "\0");

				break;
			case 5:
				record = Model.toRecordFormat5(

				(new String(account)), // account + password + notes
						(new String(password)), (new String(notes)));

				if (crypto.getType() != 1) { // TripleDES
					ivLen = 16; // AES128, AES256
				}

				break;
			}

			// encrypt record
			ciphertext = crypto.encrypt(record);

			//int len = title.length + ciphertext.length - 16;
			int len = getByteArrayLength(title) + ciphertext.length - 16;


			switch (getPdbVersion()) {
			case 4:
				recordLength = len + 1;
				break;
			case 5:
				recordLength = len + 4 + (len % 2) + ivLen;
				break;
			}
			if (DEBUG) System.out.println("updating, recordLentgh: " + recordLength );
			// save changes
			myEntry.setTitle(new String(title));
			myEntry.setEncrypted(Model.sliceBytes(ciphertext, 16,
					ciphertext.length - 16));
			myEntry.setCategory(categoryIndex);
			myEntry.setAttribute(categoryIndex | 0x40); // record ok
			myEntry.setRecordLength(recordLength);
			myEntry.setIv(Model.sliceBytes(ciphertext, 0, ivLen));

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
