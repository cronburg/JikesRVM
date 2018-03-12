package org.mmtk.vm;

import org.mmtk.vm.VM;
import org.mmtk.utility.Log;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.ObjectReference;

@Uninterruptible
public abstract class Permcheck {
  
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
          byte currType = VM.memory.PermcheckGetBits(0, addr.plus(i));
          if (currType != expectedCurrType) {
            Log.writeln("Invalid Type transition.");
            Log.write("  shadowMap["); Log.write(addr.plus(i)); Log.write("] = ");
            writeType(currType);
            Log.writeln();
            Log.write("  Expected Type = "); Log.writeln(expectedCurrType);
            Log.flush();
            error = true;
            VM.assertions.dumpStack();
          }
        }
      }
      // TODO: Set all bits at the same time.
      VM.memory.PermcheckSetBits(0, addr.plus(i), newType);
    }
  }
  
  public static void freeCell(ObjectReference object) {
  	Address endAddr = VM.objectModel.getObjectEndAddress(object);
    Address startAddr = VM.objectModel.objectStartRef(object);
    UnmarkData(startAddr, endAddr.minus(startAddr.toInt()).toInt(), Type.CELL);
  }
  
  public static void freeCell(Address addr) {
    ObjectReference obj = VM.objectModel.getObjectFromStartAddress(addr);
    freeCell(obj);
  }

  public static void MarkData(Address addr, int extent, byte newType) {
    a2b(addr, extent, (byte)(newType - 1), newType);
  }
    
  public static void UnmarkData(Address addr, int extent, byte oldType) {
    a2b(addr, extent, oldType, (byte)(oldType - 1));
  }
  
  public static void CanRead(byte type, boolean flag) {
    VM.memory.PermcheckCanReadType(0, type, flag);
  }
  public static void CanWrite(byte type, boolean flag) {
    VM.memory.PermcheckCanReadType(0, type, flag);
  }
  
  public static void CanReadWrite(byte type, boolean flag) {
    CanRead(type, flag);
    CanWrite(type, flag);
  }
  
}
