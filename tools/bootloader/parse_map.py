#!/usr/bin/env python2.7

# Extract all method names and entry points from RVM.map file
# Output as stabs debugging information for input into GNU assembler as.
# Based on parse_map.perl.

import sys
import re
import struct # for binary line number file

map_fn = sys.argv[1] # RVM.map
asm_fn = sys.argv[2] # JikesRVM-symbols.s
bin_fn = sys.argv[2][:-2] + '-lines.bin'

print "Processing %s"%(map_fn,)
print "Generating %s"%(asm_fn,)

with open(map_fn, 'r') as map_fd, open(asm_fn, 'w') as asm_fd:
  
  # stabs file header directives. invalid file info - doesn't matter.
  h  = '.stabs "/DUMMY/",100,0,0,.Ltext0\n'       \
     + '.stabs "dumymap.s",100,0,315,.Ltext0\n        \n' \
     + '.text\n.Ltext0:\n\n'
  asm_fd.write(h)

  # Skip JTOC entries (for now)
  while True:
    line = map_fd.readline()
    if line.startswith('Method Map'): break

  methods   = []  # [(Address, MethodName, LineNumberDict), MethodSize]
  line_nums = []  # Instruction Address --> Source Line Number
  method_szs = {} # Method Instruction Address --> Method Size
  code_patt = re.compile(r'\.\s+\.\s+code\s+(0x[0-f]+)\s+< \S+, L(\S+); >.(\S+) \((\S*)\)(\S+)')
  size_patt = re.compile(r'\.\s+\.\s+methodsz\s+(0x[0-f]+)\s+(\d+)')
  line_patt = re.compile(r'\.\s+\.\s+lines\s+(0x[0-f]+)\s+(\d+),(\d+)')
  count = 0
  for line in map_fd.readlines():
    
    m = code_patt.match(line)
    if m is None:
      m2 = line_patt.match(line)
      if m2 is None:
        
        m3 = size_patt.match(line)
        if m3 is None:
          if "0x" in line and "code" in line:
            sys.stderr.write("Warning, skipping: %s\n"%(line,))
          continue
      
        offset,sz = m3.groups()
        method_szs[offset] = sz
        continue

      offset,line_num,instr_bytes = m2.groups()
      #print offset,line_num
      line_nums.append((offset, line_num, instr_bytes))
      continue

    offset,typ,method,args,ret = m.groups()

    # Mangle the init method name
    method = method.replace("<init>", "__init")
    
    # Replace '/'s with '.'s in type name
    typ = typ.replace("/", ".")

    # Note that GNU assembler labels
    # have a smaller set of allowable chars
    # than JikesRVM method names
    # See info:as "Symbol Names" for full details
    # See rvm/src/org/jikesrvm/classloader/NativeMethod.java
    # for details of mangling algorithm
    # Note that we only mangle args, not classname/methodname
    sig = "%s__%s" % (args,ret)
    sig = sig.replace('[', '_3') \
             .replace(';', '_2') \
             .replace('/', '_')

    # Fully mangled name:
    mangled = "%s.%s__%s.%s" % (typ,method,sig,offset)
    try:
      methods.append((offset, mangled, line_nums, method_szs[offset]))
    except KeyError as e:
      sys.stderr.write("Warning: skipping " + mangled + "\n")

    e = '.stabs "%s:F(0,0)",36,0,0,%s\n' % (mangled, offset) \
      + '.type %s,function\n' % (mangled,) \
      + '.global %s\n' % (mangled,) \
      + '%s:\n  nop\n' % (mangled,)
    
    for (offset,line_num,instr_bytes) in line_nums:
      e = e + '.stabn 68,0,%s,%s\n' % (line_num, offset)
    
    e = e \
      + '.Lscope%d:       \n' % (count,) \
      + '  .stabs "",36,0,0,.Lscope%d-%s\n\n' % (count, mangled)

    asm_fd.write(e)
    count += 1
    line_nums = []

# Sort methods in address-order to determine sizes - not anymore...
methods = sorted(methods, key=lambda tpl: tpl[0])
with open(bin_fn, 'w') as bin_fd:
  for (offset,mangled,line_nums,method_sz) in methods:
    dat = struct.pack('=QII', int(offset, 16), int(method_sz), len(line_nums))
    for (line_offset, line_num, instr_bytes) in line_nums:
      dat += struct.pack('=QII', int(line_offset, 16), int(line_num), int(instr_bytes))
    dat += mangled + '\n'
    bin_fd.write(dat)

