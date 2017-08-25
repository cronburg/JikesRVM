/*
 *  This file is part of the Jikes RVM project (http://jikesrvm.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  See the COPYRIGHT.txt file distributed with this work for information
 *  regarding copyright ownership.
 */

#include "sys.h"

EXTERNAL void sysPermcheckInitializeMap(int shadowMapID) { /* Intercepted by PIN tool */ }
EXTERNAL void sysPermcheckDestroyMap(int shadowMapID) { /* Intercepted by PIN tool */ }

EXTERNAL int  sysPermcheckGetBit(int shadowMapID, Address a, int offset) {
    return (volatile int) shadowMapID;
}

EXTERNAL void sysPermcheckUnmarkBit(int shadowMapID, Address a, int offset) { /* Intercepted by PIN tool */ }
EXTERNAL void sysPermcheckMarkBit(int shadowMapID, Address a, int offset) {/* Intercepted by PIN tool */ }
EXTERNAL void sysPermcheckSetBits(int shadowMapID, Address a, char mbits) { /* Intercepted by PIN tool */ }
EXTERNAL char sysPermcheckGetBits(int shadowMapID, Address a) {
    return (volatile int) shadowMapID;
}

// Register a new function - all subsequent RegisterLineNumber calls correspond to the function
// given to the most recent call to this function. See 'sysPermcheckNewFunction' for usage pattern
EXTERNAL void sysPermcheckRegisterFunction(Address start, int size, char* descriptor, int descr_length) {
  /* Intercepted by PIN tool */
}

// Addresses [a, a + sz) contain machine code corresponding to the given line_number.
EXTERNAL void sysPermcheckRegisterLineNumber(Address a, int line_number, int sz) {
  /* Intercepted by PIN tool */
}

EXTERNAL void sysPermcheckNewFunction(Address start, int size, char* descriptor, int descr_length, int* line_numbers, int line_numbers_length) {
  /* Intercepted by PIN tool */
  sysPermcheckRegisterFunction(start, size, descriptor, descr_length);
  for (int i = 0; i < line_numbers_length; i++) {
    sysPermcheckRegisterLineNumber((Address)(start + i), line_numbers[2 * i], line_numbers[2 * i + 1]);
  }
}
