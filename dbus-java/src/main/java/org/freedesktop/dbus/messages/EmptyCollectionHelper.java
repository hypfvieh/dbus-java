/*
   D-Bus Java Implementation
   Copyright (c) 2019 Technolution BV

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.messages;

import java.util.Arrays;

final class EmptyCollectionHelper {

	/**
	 * This function determine the new offset in signature for empty Dictionary/Map collections.
	 *  Normally the element inside a collection determines the new offset, 
	 *  however in case of empty collections there is no element to determine the sub signature of the list
	 *  so this function determines which part of the signature to skip
	 * @param sigb the total signature
	 * @param currentOffset the current offset within the signature
	 * @return the index of the last element of the collection (subtype)
	 */
	static int determineSignatureOffsetDict(byte[] sigb, int currentOffset) {
		return determineEndOfBracketStructure(sigb, currentOffset, '{' , '}');
	}
	
	/**
	 * This function determine the new offset in signature for empty Array/List collections.
	 *  Normally the element inside a collection determines the new offset, 
	 *  however in case of empty collections there is no element to determine the sub signature of the list
	 *  so this function determines which part of the signature to skip
	 * @param sigb the total signature
	 * @param currentOffset the current offset within the signature
	 * @return the index of the last element of the collection (subtype)
	 */
	static int determineSignatureOffsetArray(byte[] sigb, int currentOffset) {
		String sigSubString = determineSubSignature(sigb, currentOffset);
		
		// End of string so can't have any more offset
		if (sigSubString.isEmpty()) {
			return currentOffset;
		}
		
		ECollectionSubType newtype = determineCollectionSubType((char)sigb[currentOffset]);
		switch (newtype) {
			case ARRAY:
				// array in array so look at the next type
				return determineSignatureOffsetArray(sigb, currentOffset + 1);
			case DICT:
				return determineSignatureOffsetDict(sigb, currentOffset);
			case STRUCT:
				return determineSignatureOffsetStruct(sigb, currentOffset);
			case PRIMITIVE:
				//primitive is always one element so no need to skip more
				return currentOffset;
			default:
				break;
	
		}
		throw new IllegalStateException("Unable to parse signature for empty collection");
	}

	private static int determineSignatureOffsetStruct(byte[] sigb, int currentOffset) {
		return determineEndOfBracketStructure(sigb, currentOffset, '(' , ')');
	}

	/**
	 * This is a generic function to determine the end of a structure that has opening and closing characters.
	 * Currently used for Struct () and Dict {}
     * 
	 */
	private static int determineEndOfBracketStructure(byte[] sigb, int currentOffset, char openChar, char closeChar) {
		String sigSubString = determineSubSignature(sigb, currentOffset);
		
		// End of string so can't have any more offset
		if (sigSubString.isEmpty()) {
			return currentOffset;
		}
		int i = 0;
		int depth = 0;
		
		for(char chr : sigSubString.toCharArray()) {
			//book keeping of depth of nested structures to solve opening closing bracket problem
			if (chr == openChar) {
				depth ++;
			} else if (chr == closeChar) {
				depth --;					
			}
			if (depth == 0) {
				return currentOffset + i;
			}
			i++;
		}
		throw new IllegalStateException("Unable to parse signature for empty collection");
	}
	
	private static String determineSubSignature(byte[] sigb, int currentOffset) {
		byte[] restSigbytes = Arrays.copyOfRange(sigb, currentOffset, sigb.length);
		return new String(restSigbytes);
	}

	/**
	 * The starting type determines of a collection determines when it ends
	 * @param sig the signature letter of the type
	 */
	private static ECollectionSubType determineCollectionSubType(char sig) {
		switch (sig) {
			case '(':
				return ECollectionSubType.STRUCT;
			case '{':
				return ECollectionSubType.DICT;
			case 'a':
				return ECollectionSubType.ARRAY;
			default:
				// of course there can be other types but those shouldn't be allowed in this part of the signature
				return ECollectionSubType.PRIMITIVE;
		}
	}

	/**
	 * Internal Enumeration used to group the types of element
	 */
	enum ECollectionSubType {
		STRUCT,
		DICT,
		ARRAY,
		PRIMITIVE
	}	
}