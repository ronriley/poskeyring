package keyring;

import java.io.UnsupportedEncodingException;
import java.nio.charset.CharacterCodingException;

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

// Entry.java

// 29.10.2004

// 02.11.2004: add variables attribute, uniqueId & recordLength
// 06.11.2004: removed entryId from crypto.decrypt(); added getDate()
// 17.11.2004: added getAll()
// 23.11.2004: toString changed
// 24.11.2004: added setTitleSeparator()
// 01.12.2004: added iv (Keyring database format 5)
// 07.12.2004: using lastIndexOf for toString
// 23.09.2005: added getUniqueId()

// Keyring fields: title, category, account, password, notes, date

/**
 * This class is used to save and manipulate entries.
 */
public class Entry implements Comparable<Entry> {
	// ----------------------------------------------------------------
	// variables
	// ----------------------------------------------------------------

	/**
	 * Separates levels in an entry title for the tree view
	 */
	private static char SEPARATOR = '/'; // default

	/**
	 * Entry Id
	 */
	protected int entryId;

	/**
	 * Entry Title
	 */
	private String title;

	/**
	 * Index of category-name
	 */
	private int category;

	/**
	 * Encrypted data: account, password, notes, date
	 */
	private byte[] encrypted;

	/**
	 * IV-Vector of encrypted data
	 */
	private byte[] iv;

	/**
	 * Contains Index of category-name and information about if record is hidden
	 */
	private int attribute;

	/**
	 * Used by keyring database
	 */
	protected int uniqueId;

	/**
	 * Length of entry record in keyring database
	 */
	private int recordLength;

	/**
	 * Reference to class Crypto
	 */
	private Crypto crypto; // reference
	
//	private static int blanks; // number of entries with blank titles
//	
//	public static void resetBlanks(){
//		blanks = 0;
//	}

	// ----------------------------------------------------------------
	// constructor
	// ----------------------------------------------------------------
	/**
	 * Default constructor.
	 */
	public Entry(int entryId, String title, int category, byte[] encrypted, Crypto crypto,
		int attribute, int uniqueId, int recordLength, byte[] iv) {
		this.entryId = entryId;
		this.setTitle(title);
		this.setCategory(category);
		this.setEncrypted(encrypted);
		this.crypto = crypto;
		this.setAttribute(attribute);
		this.uniqueId = uniqueId;
		this.setRecordLength(recordLength);
		this.setIv(iv); // null if Keyring database format 4
	}

	// ----------------------------------------------------------------
	// public ---------------------------------------------------------
	// ----------------------------------------------------------------

	/**
	 * This method returns the unique id.
	 *
	 * @return Unique id
	 */
	public int getUniqueId() {
		return uniqueId;
	}

	/**
	 * This method sets the variable SEPARATOR.
	 *
	 * @param sep Entry title separator (Default: '/')
	 */
	public void setTitleSeparator(char sep) {
		SEPARATOR = sep;
	}

	// uncrypted fields

	/**
	 * This method returns the entry id.
	 *
	 * @return Entry id
	 */
	public int getEntryId() {
		return entryId;
	}

	/**
	 * This method returns the entry title.
	 *
	 * @return Entry title
	 */
	public String getTitle() {
		//if (title.equals("")) return "#" + blanks++;
		return title;
	}
	
	/**blank titles are allocated #NNN by keyring, don't
	 * store these placeholders, call this method with false when storing
	 * @param showBlankPlaceholders if false then return an empty string if a placeholder value
	 * @return
	 */
	public String getTitle(boolean showBlankPlaceholders){
		if (showBlankPlaceholders) return title;
		if (matchesPlaceholder(title)) return "";
		return title;
	}
	
	public static boolean matchesPlaceholder(String s){
		return s.matches(".*#.*")||s.matches(".*/.*");
	}

	/**
	 * This method returns the category-name index.
	 *
	 * @return Category-name index
	 */
	public int getCategory() {
		return category;
	}

	// crypted fields

	/**
	 * This method returns the decrypted account name.
	 *
	 * @return Account name
	 * @throws UnsupportedEncodingException 
	 * @throws CharacterCodingException 
	 */
	public String getAccount() throws UnsupportedEncodingException {
		String temp = (String)crypto.decrypt(getEncrypted(), "account", getIv());

		if(temp == null)
			return "";
		else
			return temp;
	}

	/**
	 * This method returns the decrypted password.
	 *
	 * @return Password
	 * @throws UnsupportedEncodingException 
	 */
	public String getPassword() throws UnsupportedEncodingException {
		String temp = (String)crypto.decrypt(getEncrypted(), "password", getIv());

		if(temp == null)
			return "";
		else
			return temp;
	}

	/**
	 * This method returns the decrypted notes.
	 *
	 * @return Notes
	 * @throws UnsupportedEncodingException 
	 */
	public String getNotes() throws UnsupportedEncodingException {
		String temp = (String)crypto.decrypt(getEncrypted(), "notes", getIv());

		if(temp == null)
			return "";
		else
			return temp;
	}

	/**
	 * This method returns the decrypted last modified date.
	 *
	 * @return Date in format dd.mm.yyy
	 * @throws UnsupportedEncodingException 
	 */
	public String getDate() throws UnsupportedEncodingException {
		byte[] buffer = (byte[])crypto.decrypt(getEncrypted(), "datetype", getIv());
		int d, m, y;
		//Model.printHexByteArray("getDate", buffer);

		try {
			y = ((buffer[0] & 0xFE) >> 1) + 1904;
			m = ((buffer[0] & 0x01) << 3) + ((buffer[1] & 0xE0) >> 5);
			d = (buffer[1] & 0x1F);
		}
		catch(Exception e) {
			return "11.11.2004"; // mg: because of wrong AES encryption in keyring pre-release
		}

		return d + "." + m + "." + y;
	}

	// for testing purpose
	public byte[] getAll() throws UnsupportedEncodingException {
		byte[] buffer = (byte [])crypto.decrypt(getEncrypted(), "", getIv());

		return buffer;
	}

	/**
	 * This method returns a string with all entry information.
	 *
	 * @return Entry information
	 * @throws UnsupportedEncodingException 
	 */
	public String getInfo() throws UnsupportedEncodingException {
		return "EntryUniqueId: " + uniqueId + " = " + getTitle() + " + " + getAccount() + " + " + getPassword() +
			" (" + getRecordLength() + ", "/* + getDate()*/ + ", " + getCategory() + ")";
	}

	// toString used by DefaultTreeModel in DynamicTree.java
	/**
	 * This method is used by DefaultTreeModel in DynamicTree.java to display the entry title.
	 * It uses variable SEPARATOR to get the last level of the title.
	 * Example: Labor1/PC1 => PC1
	 */
	public String toString() {
		int i = getTitle().lastIndexOf(SEPARATOR);

		if(i != -1) {
			return getTitle().substring(i + 1, getTitle().length());
		}
		else {
			return getTitle();
		}
	}

	// comparable for sorting entries (title)
	@Override
	public int compareTo(Entry e) {
		return (getTitle().toLowerCase()).compareTo((((Entry) e).getTitle()).toLowerCase());
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setEncrypted(byte[] encrypted) {
		this.encrypted = encrypted;
	}

	public byte[] getEncrypted() {
		return encrypted;
	}

	public void setCategory(int category) {
		this.category = category;
	}

	public void setAttribute(int attribute) {
		this.attribute = attribute;
	}

	public int getAttribute() {
		return attribute;
	}

	public void setRecordLength(int recordLength) {
		this.recordLength = recordLength;
	}

	public int getRecordLength() {
		return recordLength;
	}

	public void setIv(byte[] iv) {
		this.iv = iv;
	}

	public byte[] getIv() {
		return iv;
	}


}
