package org.jikesrvm.objectmodel;


import org.jikesrvm.VM;
import org.jikesrvm.mm.mminterface.Selected;
import org.jikesrvm.mm.mmtk.ActivePlan;
import org.jikesrvm.runtime.BootRecord;
import org.jikesrvm.runtime.Magic;
import org.jikesrvm.runtime.SysCall;
import org.jikesrvm.scheduler.RVMThread;
import org.mmtk.plan.MutatorContext;
import org.mmtk.plan.Plan;
import org.mmtk.policy.Space;
import org.mmtk.policy.Space.SpaceVisitor;
import org.mmtk.utility.Log;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.Extent;
import org.vmmagic.unboxed.ObjectReference;
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
  }
  
  private static void writeType(byte type) {
  	switch (type) {
  	  case Type.UNMAPPED: Log.write("UNMAPPED"); break;
    	case Type.PAGE:  Log.write("PAGE");  break;
    	case Type.SPACE: Log.write("SPACE"); break;
    	case Type.BLOCK: Log.write("BLOCK"); break;
    	case Type.CELL:  Log.write("CELL");  break;
    	case Type.STATUS_WORD: Log.write("STATUS_WORD"); break;
    	default: Log.write(type);
    }
  }
  
  private static boolean error = false;
  
  @SuppressWarnings({ "static-access" })
  public
  static void a2b(Address addr, int extent, byte expectedCurrType, byte newType) {
    
    //Log.write("Mark("); Log.write(addr);
    //Log.write(", "); Log.write(extent);
    //Log.write(", "); Log.write(expectedCurrType);
    //Log.write(", "); Log.write(increment); Log.writeln(")");
    
    for (int i = 0; i < extent; i++) {
      if (!error) {
        
        if (!error) {
          byte currType = getBits(0, addr.plus(i));
          if (currType != expectedCurrType) {
            Log.writeln("Invalid Type transition.");
            Log.write("  shadowMap["); Log.write(addr.plus(i)); Log.write("] = ");
            writeType(currType);
            Log.writeln();
            Log.write("  Expected Type = ");
            writeType(expectedCurrType);
            
            for (int j = -2048; j < 2048; j++) {
              byte ty = getBits(0, addr.plus(j));
              if (j % 64 == 0) {
                Log.writeln();
                Log.write(addr.plus(j));
                Log.write(": ");
              }
              Log.writeHexChars(Word.fromIntZeroExtend(ty), 1);
            }
            
            Log.flush();
            error = true;
            RVMThread.dumpStack();
          }
        }
      }
      // TODO: Set all bits at the same time.
      setBits(0, addr.plus(i), newType);
    }
  }
  
  public static void freeCell(ObjectReference object) {
  	Address endAddr = ObjectModel.getObjectEndAddress(object);
    Address startAddr = ObjectModel.objectStartRef(object);
    unmarkData(startAddr, endAddr.minus(startAddr.toInt()).toInt(), Type.CELL);
  }
  
  public static void freeCell(Address addr) {
    ObjectReference obj = ObjectModel.getObjectFromStartAddress(addr);
    freeCell(obj);
  }

  public static void markData(Address addr, int extent, byte newType) {
    a2b(addr, extent, (byte)(newType - 1), newType);
  }
    
  public static void unmarkData(Address addr, int extent, byte oldType) {
    a2b(addr, extent, oldType, (byte)(oldType - 1));
  }
  
  public static void canRead(byte type, boolean flag) {
    canReadType(0, type, flag);
  }
  public static void canWrite(byte type, boolean flag) {
    canReadType(0, type, flag);
  }
  
  public static void canReadWrite(byte type, boolean flag) {
    canRead(type, flag);
    canWrite(type, flag);
  }

  public static void statusWord2Unmapped(Address addr) {
    ObjectReference ref = ObjectModel.getObjectFromStartAddress(addr);
    statusWord2Unmapped(ref);
  }
  
  public static void deinitializeHeader(ObjectReference object) {
    org.jikesrvm.objectmodel.JavaHeader.deinitializeHeader(object);
  }
  public static void unmapped2StatusWord(ObjectReference o) {
    org.jikesrvm.objectmodel.JavaHeader.unmapped2StatusWord(o);
  }
  public static void statusWord2Unmapped(ObjectReference o) {
    org.jikesrvm.objectmodel.JavaHeader.statusWord2Unmapped(o);
  }

  public static void initializeMap(int shadowMapID) {
    if (VM.runningVM)
      SysCall.sysCall.sysPermcheckInitializeMap(shadowMapID);
  }

  public static void destroyMap(int shadowMapID) {
    if (VM.runningVM)
      SysCall.sysCall.sysPermcheckDestroyMap(shadowMapID);
  }

  public static boolean getBit(int shadowMapID, Address a, int offset) {
    if (VM.runningVM)
      return (1 == SysCall.sysCall.sysPermcheckGetBit(shadowMapID,a,offset));
    return false;
  }

  public static void unmarkBit(int shadowMapID, Address a, int offset) {
    if (VM.runningVM)
      SysCall.sysCall.sysPermcheckUnmarkBit(shadowMapID,a,offset);
  }

  public static void markBit(int shadowMapID, Address a, int offset) {
    if (VM.runningVM)
      SysCall.sysCall.sysPermcheckMarkBit(shadowMapID,a,offset);
  }

  public static void setBits(int shadowMapID, Address a, byte mbits) {
    if (VM.runningVM)
      SysCall.sysCall.sysPermcheckSetBits(shadowMapID,a,mbits);
  }

  public static byte getBits(int shadowMapID, Address a) {
    if (VM.runningVM)
      return SysCall.sysCall.sysPermcheckGetBits(shadowMapID,a);
    return 0;
  }

  public static void setBytes(int shadowMapID, Address start, int size, byte mbits) {
    if (VM.runningVM) {
      for (int i = 0; i < size; i++) {
        setBits(shadowMapID, start.plus(i), mbits);
      }
    }
  }

  public static void newFunction(Address start, int size, byte[] descriptor, int[] lm, int lm_length) {
    if (VM.runningVM)
      SysCall.sysCall.sysPermcheckNewFunction(start, size, descriptor, descriptor.length, lm, lm_length);
  }

  // TODO: [karl] learn types readable and writeable in the boot image with these calls during boot image writer
  public static void canReadType(int shadowMapID, byte mbits, boolean flag) {
    if (VM.runningVM)
      SysCall.sysCall.sysPermcheckCanReadType(shadowMapID, mbits, flag);
  }

  public static void canWriteType(int shadowMapID, byte mbits, boolean flag) {
    if (VM.runningVM)
      SysCall.sysCall.sysPermcheckCanWriteType(shadowMapID, mbits, flag);
  }

  public static final int FUNCTION_MAP = 6;
  
  public static void boot(BootRecord the_boot_record) {
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
        
        unmapped2StatusWord(current);
        Log.writeln("boot] tib=", Magic.objectAsAddress(tib));
        Log.flush();
    }
  }
}
