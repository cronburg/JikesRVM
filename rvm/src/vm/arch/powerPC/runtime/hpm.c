/*
 * (C) Copyright IBM Corp. 2001
 *
 * $Id$
 *
 * @author Peter F. Sweeney
 */

/*
 * Description: 
 * This file provides an interface to the hardware performance monitor facilities 
 * on PowerPC architectures and are used by Jikes RVM both through sysCalls and JNI.
 * Before the JNI environment is initialized, this functionality must be accessed
 * via sysCalls.
 *
 * The inteface is based on "mythread" which creates a group for the calling
 * kernel thread.  The "mythread" interface does not require superuser privileges.
 * The "init" procedure must be invoked before all others.
 * 
 */
 
#include <stdio.h>
#include <string.h>
#include <sys/m_wait.h>
#include <sys/systemcfg.h>
#include <sys/processor.h>
#include "hpm.h"

pm_info_t Myinfo;	/* machine specific services */

pm_groups_info_t My_group_info;
pm_prog_t getprog;	/* storage to get events and mode */
pm_prog_t setprog;	/* storage to set events and mode */
pm_data_t mydata;

int threadapi = 0;

int init_enabled      = 0;	/* 1 after hpm_init is called and MyInfo initialized */
int get_data_enabled  = 0;	/* 1 after call to get_data */
int set_event_enabled = 0;	/* 1 after hpm_set_settings is called, 0 after hpm_delete_settings is called */


int debug = 0;
/*
 * This routine replicates sys_hpm.sys_hpm_init() functionality to provide JNI
 * access when not used through Jikes RVM, but by application code for example.
 * This routine initializes the Performance Monitor APIs, and 
 * must be called before any other API calls can be made.
 * If filter == PM_UNVERIFIED, accept anything.  Other alternatives are:
 *	PM_VERIFIED, PM_CAVEAT
 * This routine sets the Myinfo data structure, which can then be referenced later.
 * Called once per machine.
 * Sets fields of setprog to default values
 */
int
hpm_init(int my_filter) 
{
  int rc, i;

  if(debug>=1)fprintf(stdout,"hpm.hpm_init(%d) use %d\n", my_filter, my_filter|PM_GET_GROUPS);
  /* pm_init */ 
  if ( (rc = pm_init(my_filter|PM_GET_GROUPS, &Myinfo, &My_group_info)) != OK_CODE) {
    pm_error("pm_init", rc);
    exit(ERROR_CODE);
  }

  init_enabled = 1;
  if(debug>=1)printf("hpm_init: processor name is %s\n",Myinfo.proc_name);
  // Set counter to count nothing
  for (i = 0; i < MAX_COUNTERS; i++) {
    setprog.events[i] = COUNT_NOTHING;
  }

  // start with clean slate
  setprog.mode.w          = 0;

  return(OK_CODE);
}

/***********************************************************************************
 * CONSTRAINT: the following functions may be called only after hpm_init 
 * has been called and they accesses the static structure Myinfo
 ************************************************************************************/

/*
 * How many counters available?
 */
int 
hpm_number_of_counters() 
{
  if(init_enabled==0) {
    fprintf(stderr,"***hpm.hpm_number_of_counters() called before hpm_init()!***");
    exit(-1);
  }
  return Myinfo.maxpmcs;
}

/*
 * Return processor name.  Assume that hpm_init is called.
 */
char *
hpm_get_processor_name()
{
  if(init_enabled==0) {
    fprintf(stderr,"***hpm.isPower4() called before hpm_init()!***");
    exit(-1);
  }
  return Myinfo.proc_name;
}

/*
 * Is this a Power PC Power4 machine?
 */
int 
hpm_isPower4() 
{
  if(init_enabled==0) {
    fprintf(stderr,"***hpm.isPower4() called before hpm_init()!***");
    exit(-1);
  }
  if (strcmp(Myinfo.proc_name,"POWER4") == 0) {
    return 1;
  }
  return 0;
}
/*
 * Is this a Power PC Power3-II machine?
 */
int 
hpm_isPower3II() 
{
  if(init_enabled==0) {
    fprintf(stderr,"***hpm.isPower3II() called before hpm_init()!***");
    exit(-1);
  }
  if (strcmp(Myinfo.proc_name,"POWER3-II") == 0) {
    return 1;
  }
  return 0;
}
/*
 * Is this a Power PC Power3 machine?
 */
int 
hpm_isPower3() 
{
  if(init_enabled==0) {
    fprintf(stderr,"***hpm.isPower3() called before hpm_init()!***");
    exit(-1);
  }
  if (strcmp(Myinfo.proc_name,"POWER3") == 0) {
    return 1;
  }
  return 0;
}
/*
 * Is this a Power PC RS64-III machine?
 */
int 
hpm_isRS64III() 
{
  if(init_enabled==0) {
    fprintf(stderr,"***hpm.isRS64-III() called before hpm_init()!***");
    exit(-1);
  }
  if (strcmp(Myinfo.proc_name,"RS64-III") == 0) {
    return 1;
  }
  return 0;
}
/*
 * Is this a Power PC RS64-III machine?
 */
int 
hpm_is604e() 
{
  if(init_enabled==0) {
    fprintf(stderr,"***hpm.is604e() called before hpm_init()!***");
    exit(-1);
  }
  if(strcmp(Myinfo.proc_name,"604e") == 0) {
    return 1;
  }
  return 0;
}

/***********************************************************************************
 * CONSTRAINT: the following functions may be called only after hpm_init 
 * has been called and before hpm_set_settings has been called.
 ************************************************************************************/

/*
 * This routine replicates sys_hpm.sys_hpm_init() functionality to provide JNI
 * access when not used by Jikes RVM, but by application code for example.
 * This routine is called to set, in local variable setprog, the events to watch.
 * Must be called after hpm_init!
 * The result of calling this routine only takes effect after
 * hpm_set_settings is called.
 * TODO:
 * Arguments correspond to an enumerated type which
 * is mapped into a mnemonic name that is used to 
 * index into the Myinfo structure to determine the
 * correct counter, event pair to be set.
 */
int
hpm_set_event(int e1, int e2, int e3, int e4)
{
  int i;
  int rc;

  pm_events_t *evp;

  if(debug>=1){fprintf(stdout,"hpm.hpm_set_event(%d,%d,%d,%d)\n",e1,e2,e3,e4);fflush(stdout);}
  if(init_enabled==0) {
    fprintf(stderr,"***hpm_set_event() called before hpm_init()!***");
    exit(-1);
  }
  /* TODO: map enumeration to mnemonic and 
   * index mnemonic into Myinfo structure to 
   * find counter, event pair.
   */
  if (Myinfo.maxpmcs > 0) {
    setprog.events[0] = e1;
  }
  if (Myinfo.maxpmcs > 1) {
    setprog.events[1] = e2;
  }
  if (Myinfo.maxpmcs > 2) {
    setprog.events[2] = e3;
  }
  if (Myinfo.maxpmcs > 3) {
    setprog.events[3] = e4;
  }

  /* check validity of the event number */
  for (i = 0; i < Myinfo.maxpmcs; i++) {
    /* with the filter, it's possible that a counter has no
       matching event. 
    */
    if (Myinfo.maxevents[i] == 0)
      continue;
    
    evp = Myinfo.list_events[i];
    evp += Myinfo.maxevents[i] - 1;
    
    if ((setprog.events[i] < COUNT_NOTHING) || 
	(setprog.events[i] > evp->event_id)) {
      fprintf(stderr,"Event %d is invalid in counter %d\n", 
	      setprog.events[i], i+1);
      exit(ERROR_CODE);
    }		
  }

  set_event_enabled = 1;
  return(OK_CODE);
}
/*
 * This routine replicates sys_hpm.sys_hpm_init() functionality to provide JNI
 * access when not used by Jikes RVM, but by application code for example.
 * This routine is called to set, in local variable setprog, the events to watch.
 * Must be called after hpm_init!
 * The result of calling this routine only takes effect after
 * hpm_set_settings is called.
 * TODO:
 * Arguments correspond to an enumerated type which
 * is mapped into a mnemonic name that is used to 
 * index into the Myinfo structure to determine the
 * correct counter, event pair to be set.
 */
int
hpm_set_event_X(int e5, int e6, int e7, int e8)
{
  int i;
  int rc;

  pm_events_t *evp;

  if(debug>=1){fprintf(stdout,"hpm.hpm_set_event_X(%d,%d,%d,%d)\n",e5,e6,e7,e8);fflush(stdout);}
  /* TODO: map enumeration to mnemonic and 
   * index mnemonic into Myinfo structure to 
   * find counter, event pair.
   */
  if (Myinfo.maxpmcs > 4) {
    setprog.events[4] = e5;
  }
  if (Myinfo.maxpmcs > 5) {
    setprog.events[5] = e6;
  }
  if (Myinfo.maxpmcs > 6) {
    setprog.events[6] = e7;
  }
  if (Myinfo.maxpmcs > 7) {
    setprog.events[7] = e8;
  }

  /* turn counting on only for this process and all descendants.
  setprog.mode.w |= PM_PROCTREE;

  /* check validity of the event number */
  for (i = 0; i < Myinfo.maxpmcs; i++) {
    /* with the filter, it's possible that a counter has no
       matching event. 
    */
    if (Myinfo.maxevents[i] == 0)
      continue;
    
    evp = Myinfo.list_events[i];
    evp += Myinfo.maxevents[i] - 1;
    
    if ((setprog.events[i] < COUNT_NOTHING) || 
	(setprog.events[i] > evp->event_id)) {
      fprintf(stderr,"Event %d is invalid in counter %d\n", 
	      setprog.events[i], i+1);
      exit(ERROR_CODE);
    }		
  }

  set_event_enabled = 1;
  return(OK_CODE);
}

/*
 * Set the mode, in local variable setprog.
 * The result of calling this routine only takes effect after
 * hpm_set_settings is called.
 * The valid values for mode are defined in hpm.h.
 */
int 
hpm_set_mode(int mode) 
{
  int rc;

  if(debug>=1){fprintf(stdout,"hpm.hpm_set_mode(%d [0X%x])\n",mode,mode);}
  if(mode < 0 || mode > MODE_UPPER_BOUND) {
    fprintf(stderr,"***hpm.hpm_set_mode(%d) 0 > mode %d > %d\n",mode,mode,MODE_UPPER_BOUND);
    fflush(stdout);
  }
  if (mode & MODE_IS_GROUP) {
    if(debug>=1)fprintf(stdout,"hpm_set_mode(%d) MODE_IS_GROUP\n",mode);
    setprog.mode.b.is_group = 1;
  }
  if (mode & MODE_PROCESS) {
    if(debug>=1)fprintf(stdout,"hpm_set_mode(%d) MODE_PROCESS\n",mode);
    setprog.mode.b.process = 1;
  }
  if (mode & MODE_KERNEL) {
    if(debug>=1)fprintf(stdout,"hpm_set_mode(%d) MODE_KERNEL\n",mode);
    setprog.mode.b.kernel = 1;
  }
  if (mode & MODE_USER) {
    if(debug>=1)fprintf(stdout,"hpm_set_mode(%d) MODE_USER\n",mode);
    setprog.mode.b.user = 1;
  }
  if (! (mode & MODE_USER) && (mode & MODE_KERNEL)) {
    if(debug>=1)fprintf(stdout,"hpm_set_mode(%d) MODE_USER & MODE_KERNEL\n",mode);
    setprog.mode.b.kernel = 1;
    setprog.mode.b.user = 1;
  }
  if (mode & MODE_COUNT) {
    if(debug>=1)fprintf(stdout,"hpm_set_mode(%d) MODE_COUNT\n",mode);
    setprog.mode.b.count = 1;
  }
  if (mode & MODE_PROCTREE) {
    if(debug>=1)fprintf(stdout,"hpm_set_mode(%d) MODE_PROCTREE\n",mode);
    setprog.mode.b.proctree = 1;
  }
  if(debug>=1){
    fprintf(stdout,"hpm_set_mode(%d) translate to %d(0X%x)\n",
	    mode,setprog.mode.b,setprog.mode.b);
    fflush(stdout);
  }  
  return (OK_CODE);
}
/*
 * After init is called, and events and modes are set in the local variable setprog, 
 * call this routine to set HPM settings.
 * May call this multiple times only after calling hpm_delete_settings.
 */
int
hpm_set_settings()
{
  int rc;
  if(debug>=1){fprintf(stdout,"hpm.hpm_set_setting()\n");fflush(stdout);}
  if(init_enabled==0) {
    fprintf(stderr,"***hpm.hpm_set_setting() called before hpm_init()!***");
    exit(-1);
  }
  if ( (rc = pm_set_program_mythread(&setprog)) != OK_CODE) {
    pm_error("hpm_set_settings: pm_set_program_mythread ", rc);
    exit(ERROR_CODE);
  }
  return (OK_CODE);
}

/*
 * get counter's event
 */
int
hpm_get_event_id(int counter) 
{
  int evid;
  /*
  //  if(set_event_enabled==0) {
  //    fprintf(stderr,"***hpm.hpm_get_event_id(%d) called before hpm_set_event()!***\n",counter);
  //    exit(-1);
  //  }
  */
  if (Myinfo.maxpmcs <= counter) {
    fprintf(stderr,"***hpm.hpm_get_event_id(%d) called with counter value %d > max counters %d!***",
	    counter, counter, Myinfo.maxpmcs);
    exit(-1);
  }
  evid = setprog.events[counter];  // event number
  return evid;
}

/*
 * Get short description name for the event a counter is set to.
 */
char *
hpm_get_event_short_name(int counter) 
{
  int i, evid, group_num;
  pm_events_t *evp;
  pm_groups_t group;
  int *ev_list;

  if (Myinfo.maxpmcs <= counter) {
    fprintf(stderr,"***hpm.hpm_get_event_short_name(%d) called with counter value %d > max counters %d!***",
	    counter, counter, Myinfo.maxpmcs);
    exit(-1);
  }
  if(debug>=1)fprintf(stdout,"hpm_get_event_short_name(%d)\n",counter);
  if (setprog.mode.b.is_group) {
    group_num = setprog.events[0];
    group     = My_group_info.event_groups[group_num];
    evid = group.events[counter]; // event number
    if(debug>=1)fprintf(stdout,"hpm_get_event_short_name(%d) group evid %d\n",counter, evid);

  } else {
    evid = setprog.events[counter];  // event number
    if(debug>=1)fprintf(stdout,"hpm_get_event_short_name(%d)       evid %d\n",counter, evid);
  }
  if ( (evid == COUNT_NOTHING) || (Myinfo.maxevents[counter] == 0))
    fprintf(stdout,"event %2d: No event\n", evid);
  else {
    /* find pointer to the event */
    for (i = 0; i < Myinfo.maxevents[counter]; i++) {
      evp = Myinfo.list_events[counter]+i;
      if (evid == evp->event_id) {
	break;
      }
    }
    if(debug>=1){
      fprintf(stdout,"hpm_get_event_short_name(%d) i %d return %s\n",counter,i,evp->short_name);
      fflush(stdout);
    }
    return evp->short_name;
  }
  return "";
}

/***********************************************************************************
 * CONSTRAINT: the following functions may be called only after hpm_set_settings
 * has been called.
 ************************************************************************************/

/*
 * After hpm_set_settings is called, this routine unsets settings
 * making it possible to call hpm_set_event, hpm_set_eventX, hpm_set_mode and then
 * call hpm_set_settings again.
 */
int
hpm_delete_settings()
{
  int rc;
  if ( (rc = pm_delete_program_mythread()) != OK_CODE) {
    pm_error("hpm.hpm_delete_settings: pm_delete_program_mythread ", rc);
    exit(ERROR_CODE);
  }
  return (OK_CODE);
}

/*
 * This routine retrieves the HPM settings into the local variable setprog.
 * May be called only after a hpm_set_settings() is called.
 */
int 
hpm_get_settings() 
{
  int rc;
  if ( (rc = pm_get_program_mythread(&setprog)) != OK_CODE) {
    pm_error("hpm.hpm_get_settings: pm_get_program_mythread ", rc);
    exit(ERROR_CODE);
  }
  return (OK_CODE);
}


/*
 * Starts hpm counting. 
 * Alternatively, could turn on counting by getting program_mythread, 
 * setprog.mode.b.count = 1, and setting program_mythread.
 */
int
hpm_start_counting()
{
  int rc;

  //  fprintf(stdout,"hpm.hpm_start_counting() pthread id=%d\n",pthread_self());
  if ( (rc = pm_start_mythread()) != OK_CODE) {
    pm_error("hpm.hpm_start_counting: pm_start_mythread ", rc);
  }

  get_data_enabled = 0;
  return(OK_CODE);
}

/*
 * Stops hpm counting.
 * Assumes that hpm_start completed correctly.
 * After successful completion, counters no longer enabled.
 */
int 
hpm_stop_counting() 
{
  int rc;

  if ( (rc = pm_stop_mythread()) != OK_CODE) {
    pm_error("hpm.hpm_stop_counting: pm_stop_mythread ", rc);
    exit(ERROR_CODE);
  }
  /* counters disabled */
  get_data_enabled = 0;
  return(OK_CODE);	
}

/*
 * This routine is called to reset the counters to zero.
 * Do the counters have to be stopped before they can be reset?
 */
int
hpm_reset_counters()
{
  int rc;

  if ( (rc = pm_reset_data_mythread()) != OK_CODE) {
    pm_error("hpm.hpm_reset_counters: pm_reset_data_mythread ", rc);
    exit(ERROR_CODE);
  }
  get_data_enabled = 0;
  return(OK_CODE);
}
/*
 * Read the values of the counters
 * Cache counter values.
 * Return the number of counters.
 */
int
hpm_get_counters()
{
  int rc;

  if ( (rc = pm_get_data_mythread(&mydata)) != OK_CODE) {
    pm_error("hpm.hpm_get_counters: pm_get_data_mythread()", rc);
    exit(ERROR_CODE);
  }
  get_data_enabled = 1;
  return Myinfo.maxpmcs; 
}

/*
 * Read an HPM counter value.
 * Specify counter in range [1..maxCounters].
 */
long long
hpm_get_counter(int counter)
{
  int rc;

  long long value;
  if ( (counter < 0) || (counter > Myinfo.maxpmcs) ) {
     fprintf(stderr, "hpm.hpm_get_counter(%d): Invalid counter.\n",counter);
     exit(ERROR_CODE);
  }
  /* eliminate caching for time experiment */
  if (get_data_enabled == 0) { 
    hpm_get_counters();
  }
  value = mydata.accu[counter-1];
  return value;
}

/*
 * Print hardware performance monitors values.
 */
int 
hpm_print() 
{
  int rc;
  
  /* do I need this? */
  if ( (rc = pm_get_program_mythread(&getprog)) != OK_CODE)
    pm_error("hpm.hpm_print: pm_get_program_mythread", rc);
  
  if ( (rc = pm_get_data_mythread(&mydata)) != OK_CODE) {
    pm_error("hpm.hpm_print: pm_get_data_mythread", rc);
    return(ERROR_CODE);
  }

  hpm_print_events();
  print_data(&mydata);
  
  return(OK_CODE);	
}
/*
 * Print list of events
 */
int
hpm_print_events()
{
  /* print the results */
  print_header(getprog.mode, 0);
  if (getprog.mode.b.is_group) {
    print_group_events(getprog.events[0]);
  } else {
    print_events(getprog.events);
  }
}
/*
 * Test interface to HPM.
 */
static int test_value = 0;
int
hpm_test() 
{
  return test_value++;
}

/**
 * List all events associated with each counter.
 */
void
hpm_list_all_events()
{
  int i,j;
  int n_counters;
  int n_events;
  pm_events_t *events, event;

  if(init_enabled==0) {
    fprintf(stderr,"***hpm.hpm_list_all_events() called  before hpm_init()!***");
    exit(-1);
  }

  n_counters = Myinfo.maxpmcs;
  fprintf(stdout,"hpm.hpm_list_all_events() list events associated with %d counter\n",
	  n_counters);
  for (i=0; i< n_counters; i++) {
    events = Myinfo.list_events[i];
    n_events = Myinfo.maxevents[i];
    fprintf(stdout,  " counter %d, maxevents %d\n",i,n_events);      fflush(stdout);
    for (j=0; j<n_events; j++) {
      event = events[j];
      fprintf(stdout,"  %d: %d %c %s\n",j,event.event_id, event.status, event.short_name);
    }
  }
  fflush(stdout);
}

/**
 * List each event that is selected for a counter.
 */
void
hpm_list_selected_events()
{
  int i,j, rc;
  int n_counters;
  pm_events_t event, *evp;
  int *event_list, group_num, event_id;

  if(init_enabled==0) {
    fprintf(stderr,"***hpm.hpm_list_selected_events() called  before hpm_init()!***");
    exit(-1);
  }

  if ( (rc = pm_get_program_mythread(&getprog)) != OK_CODE)
    pm_error("hpm.hpm_list_selected_events: pm_get_program_mythread", rc);
  
  n_counters = Myinfo.maxpmcs;
  fprintf(stdout,"hpm.hpm_list_selected_events() list the event selected for each of the %d counters\n",n_counters); 

  if (getprog.mode.b.is_group) {
    event_list = get_group_event_list(getprog.events[0]);
  } else {
    event_list = getprog.events;
  }
  for (i=0; i< n_counters; i++) {
    /* get the event id from the list */
    event_id = event_list[i];
    if ( (event_id == COUNT_NOTHING) || (Myinfo.maxevents[i] == 0))
      fprintf(stdout,"  %d: event %2d: No event\n",i+1, event_id);
    else {
      /* find pointer to the event */
      for (j = 0; j < Myinfo.maxevents[i]; j++) {
	evp = Myinfo.list_events[i]+j;
	if (event_id == evp->event_id) {
	  break;
	}
      }
      fprintf(stdout,"  %d: event %2d: %c %s\n",i+1, event_id, evp->status, evp->short_name);
    }
  }
  fflush(stdout);
}

/* 
 * Function to convert filter of character form (entered from command line, 
 * e.g. -t v,u) to a numeric form.
 */
int
print_data(pm_data_t *data)
{
	int j;

	for (j=0; j<Myinfo.maxpmcs; j++) 
		fprintf(stdout, "%-8lld  ", data->accu[j]); 
	fprintf(stdout, "\n");
}


int
print_header(pm_mode_t mode, int threadapi)
{
	char mode_str[20];
	int thresh_value;

	fprintf(stdout,"*** Configuration :\n");
	if ( mode.b.user && mode.b.kernel )
		sprintf(mode_str, "%s", "kernel and user");
	else if (mode.b.user)
		sprintf(mode_str, "%s", "user only");
	else if (mode.b.kernel)
		sprintf(mode_str, "%s", "kernel only");

	fprintf(stdout, "Mode = %s; ", mode_str);

	if (!threadapi) {
		fprintf(stdout,"Process tree = ");
		if (mode.b.proctree)
			fprintf(stdout,"on; ");
		else
			fprintf(stdout,"off; ");
	}
	else {
		fprintf(stdout,"Process group = ");
		if (mode.b.process)
			fprintf(stdout,"on; ");
		else
			fprintf(stdout,"off; ");
	}
	fprintf(stderr,"Group events = ");
	if (mode.b.is_group)
	  fprintf(stderr,"on; ");
	else
	  fprintf(stderr,"off; ");

	fprintf(stdout,"Thresholding = ");
	if (mode.b.threshold > 0) 
		fprintf(stdout,"%d cycles (adjusted)\n", 
			mode.b.threshold * Myinfo.thresholdmult);
	else
		fprintf(stdout,"off \n"); 
}

/*
 * Return list of event for a group
 * Required for Power4
 * @param group_num   group number
 */
void 
get_group_event_list(int group_num)
{
  pm_groups_t group;
  if (My_group_info.maxgroups < group_num) {
    fprintf(stderr, "Invalid group number\n");
  }
  group = My_group_info.event_groups[group_num];
  fprintf(stderr, "Group %d %s\n", group.group_id, group.short_name);
  return group.events;
}

/*
 * Required for Power4
 */
void 
print_group_events(int group_num)
{
  pm_groups_t group;
  if (My_group_info.maxgroups < group_num) {
    fprintf(stderr, "Invalid group number\n");
  }
  group = My_group_info.event_groups[group_num];
  fprintf(stderr, "Group %d %s\n", group.group_id, group.short_name);
  print_events(group.events);
}

int
print_events(int *ev_list)
{
	/* for each event (pmc), print short name, accu */
	int	pmcid;		/* which pmc */
	int	evid;		/* event id */
	pm_events_t *evp;
	char	str[100];
	int	len;
	int	i;

	/* go through evs, get sname from table of events, print it */	
	for (pmcid = 0; pmcid < Myinfo.maxpmcs; pmcid++) {
		fprintf(stdout,"Counter %2d, ", pmcid+1); 
		/* get the event id from the list */
		evid = ev_list[pmcid];
		if ( (evid == COUNT_NOTHING) || (Myinfo.maxevents[pmcid] == 0))
			fprintf(stdout,"event %2d: No event\n", evid);
		else {
			/* find pointer to the event */
			for (i = 0; i < Myinfo.maxevents[pmcid]; i++) {
				evp = Myinfo.list_events[pmcid]+i;
				if (evid == evp->event_id) {
					break;
				}
			}
			
			fprintf(stdout,"event %2d: %s\n", evid, evp->short_name);
		}
	}

	fprintf(stdout,"\n*** Results :\n");

	str[0] = '\0';
	for (pmcid=0; pmcid<Myinfo.maxpmcs; pmcid++) {
		fprintf(stdout,"PMC%2d     ", pmcid+1);
		len = strlen(str);
		str[len] = ' ';
		sprintf(str+len,"%s","=====     ");
	}
	fprintf(stdout,"\n%s\n", str);	
}
