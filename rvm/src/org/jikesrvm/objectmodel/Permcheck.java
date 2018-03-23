package org.jikesrvm.objectmodel;


import org.jikesrvm.VM;
import org.jikesrvm.runtime.BootRecord;
import org.jikesrvm.runtime.Entrypoints;
import org.jikesrvm.runtime.Magic;
import org.jikesrvm.runtime.SysCall;
import org.jikesrvm.scheduler.RVMThread;
import org.jikesrvm.scheduler.Synchronization;
import org.mmtk.plan.Plan;
import org.mmtk.policy.Space;
import org.mmtk.utility.Constants;
import org.mmtk.utility.Log;
import org.vmmagic.pragma.Entrypoint;
import org.vmmagic.pragma.Inline;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.Extent;
import org.vmmagic.unboxed.ObjectReference;
import org.vmmagic.unboxed.Offset;
import org.vmmagic.unboxed.Word;

@Uninterruptible
public class Permcheck {
  
  // 4 -> 2 error (dealloc)
  // 1 -> 3 error (dealloc)
  public class Type {
    public static final byte UNMAPPED = 0x00;
    public static final byte PAGE = 0x01;
    public static final byte SPACE = 0x02;
    public static final byte BLOCK = 0x03;
    public static final byte CELL = 0x04;
    public static final byte LOCK_WORD = 0x05;
    public static final byte STATUS_WORD = 0x05;
    public static final byte BLOCK_META = 0x06;
    public static final byte FREE_PAGE = 0x07;
  }
  
  private static void writeType(byte type) {
  	switch (type) {
  	  case Type.UNMAPPED: Log.write("UNMAPPED"); break;
    	case Type.PAGE:  Log.write("PAGE");  break;
    	case Type.SPACE: Log.write("SPACE"); break;
    	case Type.BLOCK: Log.write("BLOCK"); break;
    	case Type.CELL:  Log.write("CELL");  break;
    	case Type.STATUS_WORD: Log.write("STATUS_WORD"); break;
      case Type.BLOCK_META: Log.write("BLOCK_META"); break;
      case Type.FREE_PAGE: Log.write("FREE_PAGE"); break;
    	default: Log.write(type);
    }
  }
  
  @Entrypoint
  protected static int lock = 0;
  
  private static Offset lockOffset = Offset.max();
  
  @Inline
  private static void acquireLock() {
    if (!lockOffset.isMax()) {
      while(!Synchronization.testAndSet(Magic.getJTOC(), lockOffset, 1)) {
        ;
      }
    }
  }
  
  @Inline
  private static void releaseLock() {
    if (!lockOffset.isMax()) {
      Synchronization.fetchAndStore(Magic.getJTOC(), lockOffset, 0);
    }
  }
  
  private static void writeTypes(byte[] types) {
    Log.write("(");
    for (int k = 0; k < types.length; k++) {
      writeType(types[k]);
      if (k != types.length - 1)
        Log.write(",");
    }
    Log.write(")");
  }
  
  @Inline
  private static void writeTypes(byte type) {
    writeType(type);
  }
  
  private static boolean error = false;
  
  @Inline
  @SuppressWarnings({ "static-access" })
  public
  static void a2b(Address addr, int extent, byte expectedCurrType, byte newType) {
    a2b(addr, extent, expectedCurrType, newType, true);
  }
  
  @Inline
  @SuppressWarnings({ "static-access" })
  public
  static void a2b(Address addr, int extent, byte expectedCurrType, byte newType, boolean check) {
    many2bCheck(addr, extent, null, expectedCurrType, newType, check);
  }
  
  @Inline
  public static void many2b(Address plus, int bytesInWord, byte[] bs, byte newType) {
    many2bCheck(plus, bytesInWord, bs, (byte)0, newType, true);
  }
  
  private static void writeSurroundingPage(Address addr, int extent, int faultOffset) {
    int maxOffset = 128;
    while (extent > maxOffset) {
      maxOffset += 128;
    }
    for (int m = -128; m < maxOffset; m++) {
      byte ty = getBits(0, addr.plus(m));
      if (m % 64 == 0) {
        Log.writeln();
        Log.write(addr.plus(m));
        Log.write(": ");
      }
      Log.writeHexChars(Word.fromIntZeroExtend(ty), 1);
    }
  }
  
  private static void writeBad(Address addr, byte[] expectedCurrTypes, byte newType, byte currType, int i, int j, int extent) {
    Log.write("[chisel]: Bad transition from ");
    writeTypes(expectedCurrTypes);
    Log.write("->");
    writeType(newType);
    Log.writeln(",");
    Log.write("          expected ");
    writeTypes(expectedCurrTypes);
    Log.write(" but got ");
    writeType(currType);
    Log.writeln(".");
    Log.write("          Faulting Address: ", addr.plus(i));
    Log.write(", ", i);
    Log.write(" bytes into [", addr);
    Log.write(",", addr.plus(extent));
    Log.writeln(")");
    Log.writeln("          nbytes = ", extent);
    
    writeSurroundingPage(addr, extent, i);
    
    Log.flush();
    error = true;
    RVMThread.dumpStack();
  }
  
  private static void writeBad(Address addr, byte expectedCurrType, byte newType, byte currType, int i, int extent) {
    Log.write("[chisel]: Bad transition from ");
    writeTypes(expectedCurrType);
    Log.write("->");
    writeType(newType);
    Log.writeln(",");
    Log.write("          expected ");
    writeTypes(expectedCurrType);
    Log.write(" but got ");
    writeType(currType);
    Log.writeln(".");
    Log.write("          Faulting Address: ", addr.plus(i));
    Log.write(", ", i);
    Log.write(" bytes into [", addr);
    Log.write(",", addr.plus(extent));
    Log.writeln(")");
    Log.writeln("          nbytes = ", extent);
    
    writeSurroundingPage(addr, extent, i);
    
    Log.flush();
    error = true;
    RVMThread.dumpStack();
  }
  
  @SuppressWarnings({ "static-access" })
  private static void many2bCheck(Address addr, int extent, byte[] expectedCurrTypes, byte expectedCurrType, byte newType, boolean check) {
    // Can't do any allocations in here because we're running in the VM but not necessarily VM.fullyBooted
    if (VM.runningVM) {
      acquireLock();
      
      //Log.write("Mark("); Log.write(addr);
      //Log.write(", "); Log.write(extent);
      //Log.write(", "); Log.write(expectedCurrType);
      //Log.write(", "); Log.write(increment); Log.writeln(")");
      
      
      
      for (int i = 0; i < extent; i++) {
        if (!error && check) {
          byte currType = getBits(0, addr.plus(i));
          
          // TODO: figure out how to get permcheck calls compiled away when not
          // compiling with permcheck enabled (probably with the annotations
          // and a bootimage compiler argument). For now though we ignore all
          // unmapped transitions (slowing down the runtime)
          if (currType != Type.UNMAPPED) {
            if (expectedCurrTypes == null) {
              if (currType != expectedCurrType) {
                writeBad(addr, expectedCurrType, newType, currType, i, extent);
              }
            } else {
              for (int j = 0; j < expectedCurrTypes.length; j++) {
                if (currType == expectedCurrTypes[j]) break;
                if (j == expectedCurrTypes.length - 1) {
                  //Log.write("UhOh]"); writeType(currType); Log.writeln("=", currType);
                  writeBad(addr, expectedCurrTypes, newType, currType, i, j, extent);
                }
              }
            }
          }
        }
        // TODO: Set all bits at the same time.
        setBits(0, addr.plus(i), newType);
      }
      releaseLock();
    }
  }
  
  @Inline
  public static void freeCell(ObjectReference object) {
  	//Address endAddr = ObjectModel.getObjectEndAddress(object);
    //Address startAddr = ObjectModel.objectStartRef(object);
    statusWord2Block(object);
    //unmarkData(startAddr, endAddr.minus(startAddr.toInt()).toInt(), Type.CELL);
  }
  
  @Inline
  public static void freeCell(Address addr) {
    ObjectReference obj = ObjectModel.getObjectFromStartAddress(addr);
    freeCell(obj);
  }

  @Inline
  public static void markData(Address addr, int extent, byte newType) {
    a2b(addr, extent, (byte)(newType - 1), newType);
  }
   
  @Inline
  public static void unmarkData(Address addr, int extent, byte oldType) {
    a2b(addr, extent, oldType, (byte)(oldType - 1));
  }
  
  @Inline
  public static void canRead(byte type, boolean flag) {
    canReadType(0, type, flag);
  }
  
  @Inline
  public static void canWrite(byte type, boolean flag) {
    canReadType(0, type, flag);
  }
  
  @Inline
  public static void canReadWrite(byte type, boolean flag) {
    canRead(type, flag);
    canWrite(type, flag);
  }

  @Inline
  public static void statusWord2Block(Address addr) {
    ObjectReference ref = ObjectModel.getObjectFromStartAddress(addr);
    statusWord2Block(ref);
  }
  
  protected static final Offset STATUS_OFFSET = org.jikesrvm.objectmodel.JavaHeader.getStatusOffset();
  
  @Inline
  public static void block2StatusWord(ObjectReference o) {
    Permcheck.a2b(o.toAddress().plus(STATUS_OFFSET), Constants.BYTES_IN_WORD, Permcheck.Type.UNMAPPED, Permcheck.Type.STATUS_WORD);
  }
  private static byte[] swORb = {Type.STATUS_WORD, Type.BLOCK, Type.UNMAPPED};
  
  @Inline
  public static void statusWord2Block(ObjectReference o) {
    Permcheck.many2b(o.toAddress().plus(STATUS_OFFSET), Constants.BYTES_IN_WORD, swORb, Type.BLOCK);
  }

  @Inline
  public static void initializeMap(int shadowMapID) {
    if (VM.runningVM)
      SysCall.sysCall.sysPermcheckInitializeMap(shadowMapID);
  }

  @Inline
  public static void destroyMap(int shadowMapID) {
    if (VM.runningVM)
      SysCall.sysCall.sysPermcheckDestroyMap(shadowMapID);
  }

  @Inline
  public static boolean getBit(int shadowMapID, Address a, int offset) {
    if (VM.runningVM)
      return (1 == SysCall.sysCall.sysPermcheckGetBit(shadowMapID,a,offset));
    return false;
  }

  @Inline
  public static void unmarkBit(int shadowMapID, Address a, int offset) {
    if (VM.runningVM)
      SysCall.sysCall.sysPermcheckUnmarkBit(shadowMapID,a,offset);
  }

  @Inline
  public static void markBit(int shadowMapID, Address a, int offset) {
    if (VM.runningVM)
      SysCall.sysCall.sysPermcheckMarkBit(shadowMapID,a,offset);
  }

  @Inline
  public static void setBits(int shadowMapID, Address a, byte mbits) {
    if (VM.runningVM)
      SysCall.sysCall.sysPermcheckSetBits(shadowMapID,a,mbits);
  }

  @Inline
  public static byte getBits(int shadowMapID, Address a) {
    if (VM.runningVM)
      return SysCall.sysCall.sysPermcheckGetBits(shadowMapID,a);
    return 0;
  }

  @Inline
  public static void setBytes(int shadowMapID, Address start, int size, byte mbits) {
    if (VM.runningVM) {
      for (int i = 0; i < size; i++) {
        setBits(shadowMapID, start.plus(i), mbits);
      }
    }
  }

  @Inline
  public static void newFunction(Address start, int size, byte[] descriptor, int[] lm, int lm_length) {
    if (VM.runningVM)
      SysCall.sysCall.sysPermcheckNewFunction(start, size, descriptor, descriptor.length, lm, lm_length);
  }

  // TODO: [karl] learn types readable and writeable in the boot image with these calls during boot image writer
  @Inline
  public static void canReadType(int shadowMapID, byte mbits, boolean flag) {
    if (VM.runningVM && VM.fullyBooted)
      SysCall.sysCall.sysPermcheckCanReadType(shadowMapID, mbits, flag);
  }

  @Inline
  public static void canWriteType(int shadowMapID, byte mbits, boolean flag) {
    if (VM.runningVM && VM.fullyBooted)
      SysCall.sysCall.sysPermcheckCanWriteType(shadowMapID, mbits, flag);
  }

  public static final int FUNCTION_MAP = 6;
  public static final byte[] BLOCK_OR_PAGE = {Type.BLOCK, Type.PAGE};
  
  public static void boot(BootRecord the_boot_record) {
    
    lockOffset = Entrypoints.permcheckLockField.getOffset();
    
    //Space.visitSpaces(v);
    //Plan p = Selected.Plan.get();
    Plan.vmSpace.boot();
    Plan.immortalSpace.boot();
    Plan.metaDataSpace.boot();
    Plan.loSpace.boot();
    Plan.sanitySpace.boot();
    Plan.nonMovingSpace.boot();
    Plan.smallCodeSpace.boot();
    Plan.largeCodeSpace.boot();
    // Only the main thread should be running at this point:
    RVMThread.getCurrentThread().bootPermcheck();
    
    /*
    Space[] spaces = Space.getSpaces();
    for (int i = 0; i < Space.getSpaceCount(); i++) {
      spaces[i].boot();
    }*/
  }
  
  public static void bootRef(ObjectReference current, Extent cellExtent) {
    TIB tib = ObjectModel.getTIB(current);
    Log.writeln("tib=", Magic.objectAsAddress(tib));
    Log.flush();
    if (!Magic.objectAsAddress(tib).isZero()
        && tib.length() > 0
        && Space.isMappedObject(ObjectReference.fromObject(tib.getType()))) {
        
      a2b(current.toAddress().plus(STATUS_OFFSET), Constants.BYTES_IN_WORD, Type.BLOCK, Type.STATUS_WORD, false);
      Log.writeln("boot] tib=", Magic.objectAsAddress(tib));
      Log.flush();
    }
  }

  @Inline
  public static void a2b(Address rtn, Extent bytes, byte from, byte to) {
    a2b(rtn, bytes.toInt(), from, to);
  }

  @Inline
  public static void a2b(Address rtn, Extent bytes, byte[] from, byte to) {
    many2b(rtn, bytes.toInt(), from, to);
  }

  @Inline
  public static void a2b(Address address, int bytes, byte[] from, byte to) {
    many2b(address, bytes, from, to);
  }

  @Inline
  public static void statusWord2Page(Address address) {
    a2b(address.plus(STATUS_OFFSET), Constants.BYTES_IN_WORD, Type.STATUS_WORD, Type.PAGE);
  }
  
}
