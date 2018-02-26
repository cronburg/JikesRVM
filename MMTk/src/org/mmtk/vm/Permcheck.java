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
  public static final byte PAGE_LEVEL = 0x01;
  public static final byte SPACE_LEVEL = 0x02;
  public static final byte BLOCK_LEVEL = 0x03;
  public static final byte CELL_LEVEL = 0x04;
  public static final byte STATUS_WORD_LEVEL = 0x05;
  
  private static void writeLevel(int level) {
  	switch (level) {
    	case PAGE_LEVEL:  Log.write("PAGE");  break;
    	case SPACE_LEVEL: Log.write("SPACE"); break;
    	case BLOCK_LEVEL: Log.write("BLOCK"); break;
    	case CELL_LEVEL:  Log.write("CELL");  break;
    	case STATUS_WORD_LEVEL: Log.write("STATUS_WORD"); break;
    }
  }
  
  @SuppressWarnings({ "static-access" })
  private static void Mark(Address addr, int extent, byte expectedCurrLevel, int increment) {
    
    //Log.write("Mark("); Log.write(addr);
    //Log.write(", "); Log.write(extent);
    //Log.write(", "); Log.write(expectedCurrLevel);
    //Log.write(", "); Log.write(increment); Log.writeln(")");
    
    boolean error = false;
    for (int i = 0; i < extent; i++) {
      byte currLevel = VM.memory.PermcheckGetBits(0, addr.plus(i));
      
      if (!error && currLevel != expectedCurrLevel
        && !(increment == -1 && expectedCurrLevel == CELL_LEVEL)) {
        Log.writeln("Invalid level transition.");
        Log.write("  increment="); Log.writeln(increment);
        Log.write("  shadowMap["); Log.write(addr.plus(i)); Log.write("] = ");
        writeLevel(currLevel);
        Log.write("  Expected Level = "); Log.writeln(expectedCurrLevel);
        Log.flush();
        error = true;
        
        VM.assertions.dumpStack();
      }
      VM.memory.PermcheckSetBits(0, addr.plus(i), (byte)(expectedCurrLevel + increment));
    }
  }
  
  public static void freeCell(ObjectReference object) {
    Address endAddr = VM.objectModel.getObjectEndAddress(object);
    Address startAddr = VM.objectModel.objectStartRef(object);
    UnmarkData(startAddr, endAddr.minus(startAddr.toInt()).toInt(), CELL_LEVEL);
  }
  
  public static void freeCell(Address addr) {
    ObjectReference obj = VM.objectModel.getObjectFromStartAddress(addr);
    freeCell(obj);
  }

  public static void MarkData(Address addr, int extent, byte newLevel) {
    Mark(addr, extent, (byte)(newLevel - 1), 1);
  }
    
  public static void UnmarkData(Address addr, int extent, byte oldLevel) {
    Mark(addr, extent, oldLevel, -1);
  }
  
}
