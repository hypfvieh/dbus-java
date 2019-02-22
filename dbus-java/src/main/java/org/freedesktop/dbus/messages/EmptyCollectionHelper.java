/*
   D-Bus Java Implementation
   Copyright (c) 2019 Technolution

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.messages;

import java.util.Arrays;

final class EmptyCollectionHelper {

	/**
	 * This function determine the new offset in signature for empty collections.
	 *  Normally the element inside a collection determines the new offset, 
	 *  however in case of empty collections there is no element to determine the sub signature of the list
	 *  so this function determines which part of the signature to skip
	 * @param sigb the total signature
	 * @param currentOffset the current offset within the signature
	 * @return the index of the last element of the collection (subtype)
	 */
	static int determineSignatureOffsetEmptyCollection(byte[] sigb, int currentOffset) {
		byte[] restSigbytes = Arrays.copyOfRange(sigb, currentOffset, sigb.length);
		String sigSubString = new String(restSigbytes);
		
		// End of string so can't have any more offset
		if (sigSubString.isEmpty()) {
			return currentOffset;
		}
		
		// find end of structure
		// meaning the end of the sub structure
		int i = 0;
		int structsDepth = 0;
		int dictDepth = 0;
		
		ECollectionSubType subTypeCategory = determineCollectionSubType((char)sigb[i]); 
		for(char chr : sigSubString.toCharArray()) {
			//book keeping of depth of nested structures to solve opening closing bracket problem
			switch (chr) {
				case '(':
					structsDepth ++;
					break;
				case ')':
					structsDepth --;
					break;
				case '{':
					dictDepth ++;
					break;
				case '}':
					dictDepth --;
					break;
				default:
					break;
			}
		
			// determine the case on when to return since subtype is complete
			switch (subTypeCategory) {
				case STRUCT: 
					if (structsDepth == 0) {
						return currentOffset + i;
					}
					break;
				case DICT: 
					if (dictDepth == 0) {
						return currentOffset + i;
					}
					break;
				case ARRAY:
					//array is a strange type since it has uses not brackets but the next type is part of the array
					// for example aa(ii) means array of arrays of struct of two ints.
					return determineSignatureOffsetEmptyCollection(sigb, currentOffset + i + 1);	
				case PRIMATIVE:
				default:
					return currentOffset + i ; 
			}
			i++;
		}
		throw new IllegalStateException("Unable to parse signature for empty collection");
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
				return ECollectionSubType.PRIMATIVE;
		}
	}


	/**
	 * Internal Enumeration used to group the types of element
	 */
	private enum ECollectionSubType {
		STRUCT,
		DICT,
		ARRAY,
		PRIMATIVE
	}	
}
