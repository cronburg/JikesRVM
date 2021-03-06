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
  
  public class Type {
    public static final byte UNMAPPED = 0x00;
    public static final byte PAGE = 0x01;
    public static final byte SPACE = 0x02;
    public static final byte BLOCK = 0x03;
    public static final byte CELL = 0x04;
    public static final byte STATUS_WORD = 0x05;
    public static final byte BLOCK_META = 0x06;
    public static final byte FREE_PAGE = 0x07;
    public static final byte FREE_CELL = 0x08;
    public static final byte FREE_CELL_NEXT_POINTER = 0x09;
    public static final byte LARGE_OBJECT_SPACE = 0x0a;
    public static final byte LARGE_OBJECT_HEADER = 0x0b;
    public static final byte LOCK_WORD = 0x0c;
    public static final byte FREE_SPACE = 0x0d;
    public static final byte SHARED_DEQUE = 0x0e;
    public static final byte HASH_TABLE = 0x0f;
    public static final byte IMMIX_BLOCK = 0x10;
    public static final byte ALLOCATOR = 0x11;
    public static final byte TIB_POINTER = 0x12;
    public static final byte ALIGNMENT_FILL = 0x13;
    public static final byte ARRAY_LENGTH = 0x14;
    public static final byte BUMP_POINTER_REGION_LIMIT = 0x15;
    public static final byte BUMP_POINTER_NEXT_REGION = 0x16;
    public static final byte BUMP_POINTER_DATA_END = 0x17;
    public static final byte BUMP_POINTER_CARD_META = 0x18;
  }
  public static final byte[] BLOCK_OR_HIGHER = {Type.BLOCK, Type.STATUS_WORD, Type.CELL};
  public static final byte[] PAGE_OR_LOWER = {Type.PAGE, Type.UNMAPPED, Type.FREE_PAGE};
  public static final byte[] PAGE_OR_HIGHER =
    { Type.PAGE, Type.SPACE, Type.BLOCK, Type.CELL, Type.STATUS_WORD, Type.BLOCK_META
    , Type.FREE_PAGE, Type.FREE_CELL, Type.FREE_CELL_NEXT_POINTER, Type.LARGE_OBJECT_SPACE, Type.LARGE_OBJECT_HEADER};
  public static final byte[] BUMP_POINTER_FREE_TYPES =
    { Type.ALIGNMENT_FILL, Type.ARRAY_LENGTH, Type.TIB_POINTER, Type.STATUS_WORD, Type.FREE_CELL
    , Type.BUMP_POINTER_NEXT_REGION, Type.BUMP_POINTER_DATA_END, Type.BUMP_POINTER_REGION_LIMIT
    , Type.FREE_SPACE };
  public static final byte[] UNMAPPED_OR_PAGE =
    { Type.UNMAPPED, Type.PAGE };
  public abstract void a2b(Address addr, int extent, byte expectedCurrType, byte newType);
  public abstract void freeCell(ObjectReference object);
  public abstract void freeCell(Address addr);
  public abstract void markData(Address addr, int extent, byte newType);
  public abstract void unmarkData(Address addr, int extent, byte oldType);
  public abstract void canRead(byte type, boolean flag);
  public abstract void canWrite(byte type, boolean flag);
  public abstract void canReadWrite(byte type, boolean flag);
  public abstract void block2StatusWord(ObjectReference o);
  public abstract void statusWord2Block(ObjectReference o);
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
  public abstract void a2b(Address address, int metaDataBytesPerRegion, byte blockMeta, byte blockMeta2, boolean b);
  public abstract void statusWord2Block(Address addr);
  public abstract void a2b(Address rtn, Extent bytes, byte from, byte to);
  public abstract void a2b(Address rtn, Extent bytes, byte[] unmappedOrFreepage, byte page);
  public abstract void a2b(Address metaAddress, int bytes, byte[] from, byte to);
  public abstract void statusWord2Page(Address nodeToPayload);
  
  public abstract void acquireLock();
  public abstract void releaseLock();

}
