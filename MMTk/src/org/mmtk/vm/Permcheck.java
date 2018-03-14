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
package org.mmtk.vm;

import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.*;

@Uninterruptible
public abstract class Permcheck {
  
  public abstract void a2b(Address addr, int extent, byte expectedCurrType, byte newType);
  public abstract void freeCell(ObjectReference object);
  public abstract void freeCell(Address addr);
  public abstract void markData(Address addr, int extent, byte newType);
  public abstract void unmarkData(Address addr, int extent, byte oldType);
  public abstract void canRead(byte type, boolean flag);
  public abstract void canWrite(byte type, boolean flag);
  public abstract void canReadWrite(byte type, boolean flag);
  public abstract void statusWord2Unmapped(Address addr);
  public abstract void deinitializeHeader(ObjectReference object);
  public abstract void unmapped2StatusWord(ObjectReference o);
  public abstract void statusWord2Unmapped(ObjectReference o);
  public abstract void initializeMap(int shadowMapID);
  public abstract void destroyMap(int shadowMapID);
  public abstract boolean getBit(int shadowMapID, Address a, int offset);
  public abstract void unmarkBit(int shadowMapID, Address a, int offset);
  public abstract void markBit(int shadowMapID, Address a, int offset);
  public abstract void setBits(int shadowMapID, Address a, byte mbits);
  public abstract byte getBits(int shadowMapID, Address a);
  public abstract void setBytes(int shadowMapID, Address start, int size, byte mbits);
  public abstract void newFunction(Address start, int size, byte[] descriptor, int[] lm, int lm_length);
  public abstract void canReadType(int shadowMapID, byte mbits, boolean flag);
  public abstract void canWriteType(int shadowMapID, byte mbits, boolean flag);
  public abstract void bootRef(ObjectReference current, Extent cellExtent);

}
