package org.jikesrvm.mm.mmtk;

import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.Extent;
import org.vmmagic.unboxed.ObjectReference;

@Uninterruptible
public final class Permcheck extends org.mmtk.vm.Permcheck {

  @Override
  public void a2b(Address addr, int extent, byte expectedCurrType, byte newType) {
    org.jikesrvm.objectmodel.Permcheck.a2b(addr, extent, expectedCurrType, newType);
  }

  @Override
  public void freeCell(ObjectReference object) {
    org.jikesrvm.objectmodel.Permcheck.freeCell(object);
  }

  @Override
  public void freeCell(Address addr) {
    org.jikesrvm.objectmodel.Permcheck.freeCell(addr);
  }

  @Override
  public void markData(Address addr, int extent, byte newType) {
    org.jikesrvm.objectmodel.Permcheck.markData(addr, extent, newType);
  }

  @Override
  public void unmarkData(Address addr, int extent, byte oldType) {
    org.jikesrvm.objectmodel.Permcheck.unmarkData(addr, extent, oldType);
  }

  @Override
  public void canRead(byte type, boolean flag) {
    org.jikesrvm.objectmodel.Permcheck.canRead(type, flag);
  }

  @Override
  public void canWrite(byte type, boolean flag) {
    org.jikesrvm.objectmodel.Permcheck.canWrite(type, flag);
  }

  @Override
  public void canReadWrite(byte type, boolean flag) {
    org.jikesrvm.objectmodel.Permcheck.canReadWrite(type, flag);
  }

  @Override
  public void statusWord2Unmapped(Address addr) {
    org.jikesrvm.objectmodel.Permcheck.statusWord2Unmapped(addr);
  }

  @Override
  public void deinitializeHeader(ObjectReference object) {
    org.jikesrvm.objectmodel.Permcheck.statusWord2Unmapped(object);
  }

  @Override
  public void unmapped2StatusWord(ObjectReference o) {
    org.jikesrvm.objectmodel.Permcheck.unmapped2StatusWord(o);
  }

  @Override
  public void statusWord2Unmapped(ObjectReference o) {
    org.jikesrvm.objectmodel.Permcheck.statusWord2Unmapped(o);
  }

  @Override
  public void initializeMap(int shadowMapID) {
    org.jikesrvm.objectmodel.Permcheck.initializeMap(shadowMapID);
  }

  @Override
  public void destroyMap(int shadowMapID) {
    org.jikesrvm.objectmodel.Permcheck.destroyMap(shadowMapID);
  }

  @Override
  public boolean getBit(int shadowMapID, Address a, int offset) {
    return org.jikesrvm.objectmodel.Permcheck.getBit(shadowMapID, a, offset);
  }

  @Override
  public void unmarkBit(int shadowMapID, Address a, int offset) {
    org.jikesrvm.objectmodel.Permcheck.unmarkBit(shadowMapID, a, offset);
  }

  @Override
  public void markBit(int shadowMapID, Address a, int offset) {
    org.jikesrvm.objectmodel.Permcheck.markBit(shadowMapID, a, offset);
  }

  @Override
  public void setBits(int shadowMapID, Address a, byte mbits) {
    org.jikesrvm.objectmodel.Permcheck.setBits(shadowMapID, a, mbits);
  }

  @Override
  public byte getBits(int shadowMapID, Address a) {
    return org.jikesrvm.objectmodel.Permcheck.getBits(shadowMapID, a);
  }

  @Override
  public void setBytes(int shadowMapID, Address start, int size, byte mbits) {
    org.jikesrvm.objectmodel.Permcheck.setBytes(shadowMapID, start, size, mbits);
  }

  @Override
  public void newFunction(Address start, int size, byte[] descriptor, int[] lm, int lm_length) {
    org.jikesrvm.objectmodel.Permcheck.newFunction(start, size, descriptor, lm, lm_length);
  }

  @Override
  public void canReadType(int shadowMapID, byte mbits, boolean flag) {
    org.jikesrvm.objectmodel.Permcheck.canReadType(shadowMapID, mbits, flag);
  }

  @Override
  public void canWriteType(int shadowMapID, byte mbits, boolean flag) {
    org.jikesrvm.objectmodel.Permcheck.canWriteType(shadowMapID, mbits, flag);
  }

  @Override
  public void bootRef(ObjectReference current, Extent cellExtent) {
    org.jikesrvm.objectmodel.Permcheck.bootRef(current, cellExtent);
  }

}
