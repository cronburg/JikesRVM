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

#include "sys.h"
#include <sys/mman.h> // mmap

#ifdef PERMCHECK_ON_PIN
char *maps = NULL;
#include "intercept.h"
#else // PERMCHECK_ON_PIN
#ifdef PERMCHECK_ON_LIGHTWEIGHT

// TODO:
#define assert(foo) // ignore assertions for now

void  shadow_free(void* addr) {
  // TODO
  sysExit(EXIT_STATUS_SYSCALL_TROUBLE);
}

// TODO
#define _page_align(x) x

void *shadow_malloc(size_t size) {
  // Do I need a custom allocator here? The freeing behavior of these calls to
  // shadow_malloc() are significantly different from that of the application.
  // Namely, at present shadow_free *never* gets called thereby fragmenting what
  // could be a contiguous application heap every time shadow_malloc() gets
  // called i.e. when the application allocates in a new virtual address space
  // shadow map.
  //void* base = sbrk(_page_align(size));
  void* base = mmap(NULL, _page_align(size), PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, -1, 0);
#ifdef CHISEL_SHOW_ALLOC_CALLS
  fprintf(stderr, "shadow_malloc] %p:%lu\n", base, size);
#endif // CHISEL_SHOW_ALLOC_CALLS
  return base;
}

void *shadow_calloc(size_t nmemb, size_t size) {
  //void* base = sbrk(_page_align(nmemb * size));
  void* base = mmap(NULL, _page_align(nmemb * size), PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, -1, 0);
  memset(base, 0, _page_align(nmemb * size));
  return base; //calloc(nmemb, size);
}

void  shadow_memcpy(void* dst, void* src, size_t size) { memcpy(dst,src,size); }
void  shadow_out_of_memory() {
  printf("ERROR: Ran out of memory while allocating shadow memory.\n");
  sysExit(EXIT_STATUS_SYSCALL_TROUBLE);
}

#include "shadow-32.c"
#include "permcheck-lightweight.c"
ShadowMap *maps = NULL;
#define runMe(FNCN) (FNCN); return (volatile int) shadowMapID;

#else // No pin, no lightweight:
#define runMe(FNCN) shadowMapID; return (volatile int) shadowMapID;

#endif // PERMCHECK_ON_LIGHTWEIGHT

EXTERNAL int sysPermcheckInitializeMap(volatile int shadowMapID) {
  runMe(init_map(shadowMapID));
}
EXTERNAL int sysPermcheckDestroyMap(volatile int shadowMapID) {
  runMe(destroy_map(shadowMapID));
}

EXTERNAL int  sysPermcheckGetBit(volatile int shadowMapID, Address a, int offset) {
  shadowMapID = runMe(get_bit(shadowMapID, a, offset));
}

EXTERNAL int sysPermcheckUnmarkBit(volatile int shadowMapID, Address a, int offset) {
  runMe(unmark_bit(shadowMapID, a, offset));
}

EXTERNAL int sysPermcheckMarkBit(volatile int shadowMapID, Address a, int offset) {
  runMe(mark_bit(shadowMapID, a, offset));
}
EXTERNAL int sysPermcheckSetBits(volatile int shadowMapID, Address a, char mbits) {
  runMe(set_bits(shadowMapID, a, mbits));
}
EXTERNAL char sysPermcheckGetBits(volatile int shadowMapID, Address a) {
  shadowMapID = runMe(get_bits(shadowMapID, a));
}
EXTERNAL int sysPermcheckCanReadType(volatile int shadowMapID, char mbits, unsigned int flag) {
}
EXTERNAL int sysPermcheckCanWriteType(volatile int shadowMapID, char mbits, unsigned int flag) {
}

// Register a new function - all subsequent RegisterLineNumber calls correspond to the function
// given to the most recent call to this function. See 'sysPermcheckNewFunction' for usage pattern
EXTERNAL void sysPermcheckRegisterFunction(Address start, int size, char* descriptor, int descr_length) {
}

// Addresses [a, a + sz) contain machine code corresponding to the given line_number.
EXTERNAL void sysPermcheckRegisterLineNumber(Address a, int line_number, int sz) {
}

EXTERNAL void sysPermcheckNewFunction(Address start, int size, char* descriptor, int descr_length, int* line_numbers, int line_numbers_length) {
  int i;
  sysPermcheckRegisterFunction(start, size, descriptor, descr_length);
  for (i = 0; i < line_numbers_length; i++) {
    sysPermcheckRegisterLineNumber((Address)(start + i), line_numbers[2 * i], line_numbers[2 * i + 1]);
  }
}

#endif // !PERMCHECK_ON_PIN

void init_chisel(void* mem) {
#ifdef PERMCHECK_ON_LIGHTWEIGHT
  permcheck_init_maps(1, (Addr)mem);
#else // PERMCHECK_ON_PIN? TODO

#endif
}

enum _maps
  { CHISEL_TYPE_MAP = 0
  };
typedef enum _maps stateMapNum;

