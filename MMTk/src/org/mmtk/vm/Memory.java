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

import org.mmtk.policy.ImmortalSpace;

import org.vmmagic.unboxed.*;
import org.vmmagic.pragma.*;

@Uninterruptible public abstract class Memory {

  /**
   * Allows for the VM to reserve space between HEAP_START()
   * and AVAILABLE_START() for its own purposes.  MMTk should
   * expect to encounter objects in this range, but may not
   * allocate in this range.
   *
   * MMTk expects the virtual address space between AVAILABLE_START()
   * and AVAILABLE_END() to be contiguous and unmapped.
   * Allows for the VM to reserve space between HEAP_END()
   * and AVAILABLE_END() for its own purposes.  MMTk should
   * expect to encounter objects in this range, but may not
   * allocate in this range.
   *
   * MMTk expects the virtual address space between AVAILABLE_START()
   * and AVAILABLE_END() to be contiguous and unmapped.
   *
   * @return The high bound of the memory that MMTk can allocate.
   */

  /**
   * Return the space associated with/reserved for the VM.  In the
   * case of Jikes RVM this is the boot image space.<p>
   *
   * @return The space managed by the virtual machine.
   */
  @Interruptible
  public abstract ImmortalSpace getVMSpace();

  /** Global preparation for a collection. */
  public abstract void globalPrepareVMSpace();

  /** Per-collector preparation for a collection. */
  public abstract void collectorPrepareVMSpace();

  /** Per-collector post-collection work. */
  public abstract void collectorReleaseVMSpace();

  /** Global post-collection work. */
  public abstract void globalReleaseVMSpace();

  /**
   * Sets the range of addresses associated with a heap.
   *
   * @param id the heap identifier
   * @param start the address of the start of the heap
   * @param end the address of the end of the heap
   */
  public abstract void setHeapRange(int id, Address start, Address end);

  /**
   * Demand zero mmaps an area of virtual memory.
   *
   * @param start the address of the start of the area to be mapped
   * @param size the size, in bytes, of the area to be mapped
   * @return 0 if successful, otherwise the system errno
   */
  public abstract int dzmmap(Address start, int size);

  /**
   * Protects access to an area of virtual memory.
   *
   * @param start the address of the start of the area to be mapped
   * @param size the size, in bytes, of the area to be mapped
   * @return <code>true</code> if successful, otherwise
   * <code>false</code>
   */
  public abstract boolean mprotect(Address start, int size);

  /**
   * Allows access to an area of virtual memory.
   *
   * @param start the address of the start of the area to be mapped
   * @param size the size, in bytes, of the area to be mapped
   * @return <code>true</code> if successful, otherwise
   * <code>false</code>
   */
  public abstract boolean munprotect(Address start, int size);


  /**
   * Zero a region of memory.
   *
   * @param useNT Use non temporal instructions (if available)
   * @param start Start of address range (inclusive)
   * @param len Length in bytes of range to zero
   */
  public abstract void zero(boolean useNT, Address start, Extent len);

  /**
   * Logs the contents of an address and the surrounding memory to the
   * error output.
   *
   * @param start the address of the memory to be dumped
   * @param beforeBytes the number of bytes before the address to be
   * included
   * @param afterBytes the number of bytes after the address to be
   * included
   */
  public abstract void dumpMemory(Address start, int beforeBytes,
      int afterBytes);

  /**
   * Ensures that all memory writes before this point are visible to all processors.
   * In JMM terminology, this would be a {@code StoreLoad} + {@code StoreStore} fence.
   */
  @Inline
  public abstract void fence();

  /**
   * Ensures that all memory reads before this point are visible to all processors.
   * In JMM terminology, this would be a {@code LoadLoad} + {@code LoadStore} fence.
   */
  @Inline
  public abstract void combinedLoadBarriers();

  /**
   * DBI interfaces between raw client requests and MMTk users
   **/
  public abstract void    PermcheckInitializeMap (int shadowMapID);
  public abstract void    PermcheckDestroyMap    (int shadowMapID);
  public abstract boolean PermcheckGetBit    (int shadowMapID, Address a, int offset);
  public abstract void    PermcheckUnmarkBit (int shadowMapID, Address a, int offset);
  public abstract void    PermcheckMarkBit   (int shadowMapID, Address a, int offset);
  public abstract void    PermcheckSetBits   (int shadowMapID, Address a, byte mbits);
  public abstract byte    PermcheckGetBits   (int shadowMapID, Address a); 
  public abstract void    PermcheckSetBytes  (int shadowMapID, Address start, int size, byte mbits);
  public abstract void    PermcheckCanReadType(int shadowMapID, byte mbits, boolean flag);
  public abstract void    PermcheckCanWriteType(int shadowMapID, byte mbits, boolean flag);

  /*
   * NOTE: The following methods must be implemented by subclasses of this
   * class, but are internal to the VM<->MM interface glue, so are never
   * called by MMTk users.
   */
  /** @return {@code true} if we are using the (traditional) 32-bit heap layout */
  protected abstract boolean getHeapLayout32Bit();
  /** @return The lowest address in the virtual address space known to MMTk */
  protected abstract Address getHeapStartConstant();
  /** @return The highest address in the virtual address space known to MMTk */
  protected abstract Address getHeapEndConstant();
  /** @return The lowest address in the contiguous address space available to MMTk  */
  protected abstract Address getAvailableStartConstant();
  /** @return The highest address in the contiguous address space available to MMTk */
  protected abstract Address getAvailableEndConstant();
  /** @return The log base two of the size of an address */
  protected abstract byte getLogBytesInAddressConstant();
  /** @return The log base two of the size of a word */
  protected abstract byte getLogBytesInWordConstant();
  /** @return The log base two of the size of an OS page */
  protected abstract byte getLogBytesInPageConstant();
  /** @return The log base two of the minimum allocation alignment */
  protected abstract byte getLogMinAlignmentConstant();
  /** @return The log base two of (MAX_ALIGNMENT/MIN_ALIGNMENT) */
  protected abstract byte getMaxAlignmentShiftConstant();
  /** @return The maximum number of bytes of padding to prepend to an object */
  protected abstract int getMaxBytesPaddingConstant();
  /** @return The value to store in alignment holes */
  protected abstract int getAlignmentValueConstant();

  /*
   * NOTE: These methods should not be called by anything other than the
   * reflective mechanisms in org.mmtk.vm.VM, and are not implemented by
   * subclasses. This hack exists only to allow us to declare the respective
   * methods as protected.
   */
  static boolean heapLayout32BitTrapdoor(Memory m) {
    return m.getHeapLayout32Bit();
  }
  static Address heapStartTrapdoor(Memory m) {
    return m.getHeapStartConstant();
  }
  static Address heapEndTrapdoor(Memory m) {
    return m.getHeapEndConstant();
  }
  static Address availableStartTrapdoor(Memory m) {
    return m.getAvailableStartConstant();
  }
  static Address availableEndTrapdoor(Memory m) {
    return m.getAvailableEndConstant();
  }
  static byte logBytesInAddressTrapdoor(Memory m) {
    return m.getLogBytesInAddressConstant();
  }
  static byte logBytesInWordTrapdoor(Memory m) {
    return m.getLogBytesInWordConstant();
  }
  static byte logBytesInPageTrapdoor(Memory m) {
    return m.getLogBytesInPageConstant();
  }
  static byte logMinAlignmentTrapdoor(Memory m) {
    return m.getLogMinAlignmentConstant();
  }
  static byte maxAlignmentShiftTrapdoor(Memory m) {
    return m.getMaxAlignmentShiftConstant();
  }
  static int maxBytesPaddingTrapdoor(Memory m) {
    return m.getMaxBytesPaddingConstant();
  }
  static int alignmentValueTrapdoor(Memory m) {
    return m.getAlignmentValueConstant();
  }
}
