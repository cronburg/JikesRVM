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
import org.mmtk.utility.heap.layout.HeapLayout;
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
  
  public class JavaType {
    public static final byte BOOLEAN = (byte) 0xf8;
    public static final byte BYTE    = (byte) 0xf9;
    public static final byte CHAR    = (byte) 0xfa;
    public static final byte SHORT   = (byte) 0xfb;
    public static final byte INT     = (byte) 0xfc;
    public static final byte LONG    = (byte) 0xfd;
    public static final byte FLOAT   = (byte) 0xfe;
    public static final byte DOUBLE  = (byte) 0xff;
  }
  
  private static void writeType(byte type) {
  	switch (type) {
  	  case Type.UNMAPPED: Log.write("UNMAPPED"); break;
    	case Type.PAGE:  Log.write("PAGE");  break;
    	case Type.SPACE: Log.write("SPACE"); break;
    	case Type.BLOCK: Log.write("BLOCK"); break;
    	case Type.CELL:  Log.write("CELL");  break;
      case Type.LOCK_WORD:  Log.write("LOCK_WORD");  break;
    	case Type.STATUS_WORD: Log.write("STATUS_WORD"); break;
      case Type.BLOCK_META: Log.write("BLOCK_META"); break;
      case Type.FREE_PAGE: Log.write("FREE_PAGE"); break;
      case Type.FREE_CELL: Log.write("FREE_CELL"); break;
      case Type.FREE_CELL_NEXT_POINTER: Log.write("FREE_CELL_NEXT_POINTER"); break;
      case Type.LARGE_OBJECT_SPACE: Log.write("LARGE_OBJECT_SPACE"); break;
      case Type.LARGE_OBJECT_HEADER: Log.write("LARGE_OBJECT_HEADER"); break;
      case Type.FREE_SPACE: Log.write("FREE_SPACE"); break;
      case Type.SHARED_DEQUE: Log.write("SHARED_DEQUEUE"); break;
      case Type.HASH_TABLE: Log.write("HASH_TABLE"); break;
      case Type.IMMIX_BLOCK: Log.write("IMMIX_BLOCK"); break;
      case Type.TIB_POINTER: Log.write("TIB_POINTER"); break;
      case Type.ALIGNMENT_FILL: Log.write("ALIGNMENT_FILL"); break;
      case Type.ARRAY_LENGTH: Log.write("ARRAY_LENGTH"); break;
      case Type.BUMP_POINTER_NEXT_REGION: Log.write("BUMP_POINTER_NEXT_REGION"); break;
      case Type.BUMP_POINTER_DATA_END: Log.write("BUMP_POINTER_DATA_END"); break;
      case Type.BUMP_POINTER_REGION_LIMIT: Log.write("BUMP_POINTER_REGION_LIMIT"); break;
      case Type.BUMP_POINTER_CARD_META: Log.write("BUMP_POINTER_CARD_META"); break;
      case JavaType.BOOLEAN: Log.write("BOOLEAN"); break;
      case JavaType.BYTE: Log.write("BYTE"); break;
      case JavaType.CHAR: Log.write("CHAR"); break;
      case JavaType.SHORT: Log.write("SHORT"); break;
      case JavaType.INT: Log.write("INT"); break;
      case JavaType.LONG: Log.write("LONG"); break;
      case JavaType.FLOAT: Log.write("FLOAT"); break;
      case JavaType.DOUBLE: Log.write("DOUBLE"); break;
    	default: Log.write(type);
    }
  }
  
  @Entrypoint
  protected static int lock = 0;
  
  private static Offset lockOffset = Offset.max();
  
  @Inline
  public static void acquireLock() {
    if (!lockOffset.isMax()) {
      while(!Synchronization.testAndSet(Magic.getJTOC(), lockOffset, 1)) {
        ;
      }
    }
  }
  
  @Inline
  public static void releaseLock() {
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
        Log.write(":");
      }
      if ((m % 64) % Constants.BYTES_IN_WORD == 0) {
        Log.write(" ");
      }
      Log.writeHexChars(Word.fromIntZeroExtend(ty), 2);
    }
    Log.writeln();
  }
  
  private static void spaceInfo(Address addr) {
    Space spaceIn = Space.getSpaceForAddress(addr);
    Log.write("          Space = ");
    Log.write(spaceIn.getName());
    Log.write(", descr=", spaceIn.getDescriptor());
    Log.write(", chunk idx=", HeapLayout.vmMap.getIndex(addr));
    Log.writeln();
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
    
    spaceInfo(addr);
    
    writeSurroundingPage(addr, extent > 1024 ? 1024 : extent, i);
    
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
    
    spaceInfo(addr);
    
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

      /*
      Space spaceIn = Space.getSpaceForAddress(addr);
      Log.write("Mark("); Log.write(addr);
      Log.write(", "); Log.write(spaceIn.getName());
      Log.write(", "); Log.write(extent);
      Log.write(", "); writeType(expectedCurrType);
      Log.write(" -> "); writeType(newType);
      Log.write(", "); Log.write(")");
      Log.flush();*/
      
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
                writeBad(addr.plus(i), expectedCurrType, newType, currType, i, extent);
              }
            } else {
              for (int j = 0; j < expectedCurrTypes.length; j++) {
                if (currType == expectedCurrTypes[j]) break;
                if (j == expectedCurrTypes.length - 1) {
                  //Log.write("UhOh]"); writeType(currType); Log.writeln("=", currType);
                  writeBad(addr.plus(i), expectedCurrTypes, newType, currType, i, j, extent);
                }
              }
            }
          }
        }
        // TODO: Set all bits at the same time.
        setBits(0, addr.plus(i), newType);
      }
      /*
      Log.writeln(" - releasing lock.");
      Log.flush();*/
      releaseLock();
    }
  }
  
  @Inline
  public static void freeCell(ObjectReference object) {
  	//Address endAddr = ObjectModel.getObjectEndAddress(object);
    //Address startAddr = ObjectModel.objectStartRef(object);
    //unmarkData(startAddr, endAddr.minus(startAddr.toInt()).toInt(), Type.CELL);
    
    //statusWord2Block(object);
    a2b(object.toAddress(), Constants.BYTES_IN_ADDRESS, Type.TIB_POINTER, Type.FREE_CELL);
    a2b(object.toAddress().plus(STATUS_OFFSET), Constants.BYTES_IN_ADDRESS, Type.STATUS_WORD, Type.FREE_CELL);
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
    a2b(o.toAddress().plus(STATUS_OFFSET), Constants.BYTES_IN_WORD, Type.UNMAPPED, Type.STATUS_WORD);
  }
  private static byte[] swORb = {Type.STATUS_WORD, Type.BLOCK, Type.UNMAPPED};
  
  @Inline
  public static void statusWord2Block(ObjectReference o) {
    many2b(o.toAddress().plus(STATUS_OFFSET), Constants.BYTES_IN_WORD, swORb, Type.BLOCK);
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
