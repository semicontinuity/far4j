#include <windows.h>
#include <string.h>
#include <stdio.h>
#include <conio.h>
#include <sys/stat.h>

#include <tchar.h>

#include "jni.h"

// -----------------------------------------------------------------------------

jboolean debug = JNI_FALSE;

#ifdef WIN32
#define PATHSEP TEXT("\\")
#else
#define PATHSEP TEXT("/")
#endif

#define INIT_MAX_KNOWN_VMS 10

struct vmdesc {
    _TCHAR *name;
#define VM_UNKNOWN -1
#define VM_KNOWN 0
#define VM_ALIASED_TO 1
#define VM_WARN 2
#define VM_ERROR 3
    int flag;
    _TCHAR *alias;
};

#define JVM_DLL TEXT("jvm.dll")
#define JAVA_DLL TEXT("java.dll")
#define MAXPATHLEN MAX_PATH

/*
 * Pointers to the needed JNI invocation API, initialized by LoadJavaVM.
 */
typedef jint (JNICALL *CreateJavaVM_t)(JavaVM **pvm, void **env, void *args);
typedef jint (JNICALL *GetDefaultJavaVMInitArgs_t)(void *args);

typedef struct {
    CreateJavaVM_t CreateJavaVM;
    GetDefaultJavaVMInitArgs_t GetDefaultJavaVMInitArgs;
} InvocationFunctions;


// =============================================================================

JavaVM *vm;       /* denotes a Java VM */
JNIEnv *env;       /* pointer to native method interface */
InvocationFunctions ifn;

// =============================================================================


const _TCHAR *
GetArch()
{
#ifdef _WIN64
    return TEXT("ia64");
#else
    return TEXT("i386");
#endif
}

// registry access
// ------------------------------------------------------------------------------------

#define JRE_KEY	    TEXT("Software\\JavaSoft\\Java Runtime Environment")
#define DOTRELEASE  TEXT("1.7" )


static jboolean
GetStringFromRegistry(HKEY key, LPCTSTR name, LPBYTE buf, jint bufsize)
{
    DWORD type, size;

    if (RegQueryValueExW(key, name, 0, &type, 0, &size) == 0
	&& type == REG_SZ
	&& (size < (unsigned int)bufsize)) {
	if (RegQueryValueEx(key, name, 0, 0, buf, &size) == 0) {
	    return JNI_TRUE;
	}
    }
    return JNI_FALSE;
}


static jboolean
GetPublicJREHome(_TCHAR *buf, jint bufsize)
{
//    log(TEXT("> GetPublicJREHome"));
    HKEY key, subkey;
    _TCHAR version[MAXPATHLEN];

    /* Find the current version of the JRE */
    if (RegOpenKeyEx(HKEY_LOCAL_MACHINE, JRE_KEY, 0, KEY_READ, &key) != 0) {
	log(TEXT("Error opening registry key '") JRE_KEY TEXT("'\n"));
	return JNI_FALSE;
    }

    if (!GetStringFromRegistry(key, TEXT("CurrentVersion"),
			       (unsigned char*)version, sizeof(version))) {
	log(TEXT("Failed reading value of registry key:\n\t") JRE_KEY TEXT("\\CurrentVersion\n"));
	RegCloseKey(key);
	return JNI_FALSE;
    }

/*
    if (_tcscmp(version, DOTRELEASE) != 0) {
	log ( "Registry key '" JRE_KEY "\\CurrentVersion'\nhas "
		"value '<?>', but '" DOTRELEASE "' is required.\n");
	RegCloseKey(key);
	return JNI_FALSE;
    }
*/
    /* Find directory where the current version is installed. */
    if (RegOpenKeyEx(key, version, 0, KEY_READ, &subkey) != 0) {
	log (TEXT("Error opening registry key '") JRE_KEY TEXT("<version>\n"));
	RegCloseKey(key);
	return JNI_FALSE;
    }

    if (!GetStringFromRegistry(subkey, TEXT("JavaHome"), (LPBYTE)buf, bufsize)) {
	log(TEXT("Failed reading value of registry key:\n\t") JRE_KEY TEXT("<version>\\JavaHome\n"));
	RegCloseKey(key);
	RegCloseKey(subkey);
	return JNI_FALSE;
    }

    if (debug) {
	char micro[MAXPATHLEN];
	if (!GetStringFromRegistry(subkey, TEXT("MicroVersion"), (unsigned char*)micro, sizeof(micro))) {
	    //printf("Warning: Can't read MicroVersion\n");
	    micro[0] = '\0';
	}
	//printf("Version major.minor.micro = %s.%s\n", version, micro);
    }

    RegCloseKey(key);
    RegCloseKey(subkey);
//    log(TEXT("< GetPublicJREHome"));
    return JNI_TRUE;
}

// ------------------------------------------------------------------------------------


jboolean
GetApplicationHome(_TCHAR *buf, jint bufsize)
{
    _TCHAR *cp;
    GetModuleFileName(0, buf, bufsize);
    *_tcsrchr(buf, '\\') = '\0'; /* remove .exe file name */
    if ((cp = _tcsrchr(buf, '\\')) == 0) {
	/* This happens if the application is in a drive root, and
	 * there is no bin directory. */
	buf[0] = '\0';
	return JNI_FALSE;
    }
    *cp = '\0';  /* remove the bin\ part */
    return JNI_TRUE;
}

/*
 * Find path to JRE based on .exe's location or registry settings.
 */
jboolean
GetJREPath(_TCHAR *path, jint pathsize)
{
//    _TCHAR javadll[MAX_PATH];
//    struct stat s;

//    log(TEXT("> GetJREPath"));
/*
    if (GetApplicationHome(path, pathsize)) {
	// Is JRE co-located with the application?
	_stprintf(javadll, "%s\\bin\\" JAVA_DLL, path);
	if (stat(javadll, &s) == 0) {
	    goto found;
	}

	// Does this app ship a private JRE in <apphome>\jre directory? 
	_stprintf(javadll, "%s\\jre\\bin\\" JAVA_DLL, path);
	if (stat(javadll, &s) == 0) {
	    _tcscat(path, "\\jre");
	    goto found;
	}
    }
*/
    /* Look for a public JRE on this machine. */
    if (GetPublicJREHome(path, pathsize)) {
	goto found;
    }

    //fprintf(stderr, "Error: could not find " JAVA_DLL "\n");
    return JNI_FALSE;

 found:
//    if (debug) printf("JRE path is %s\n", path);
//    log(TEXT("< GetJREPath"));
    return JNI_TRUE;
}

/*
 * Given a JRE location and a JVM type, construct what the name the
 * JVM shared library will be.  Return true, if such a library
 * exists, false otherwise.
 */
jboolean
GetJVMPath(const _TCHAR *jrepath, const _TCHAR *jvmtype,
	   _TCHAR *jvmpath, jint jvmpathsize)
{
    //struct _stat s;
    if (_tcschr(jvmtype, '/') || _tcsrchr(jvmtype, '\\')) {
//	_stprintf(jvmpath, TEXT("%s\\") JVM_DLL, jvmtype);
    } else {
//	_stprintf(jvmpath, TEXT("%s\\bin\\%s\\") JVM_DLL, jrepath, jvmtype);
    }
//    if (_tstat(jvmpath, &s) == 0) {
	return JNI_TRUE;
//    } else {
//	return JNI_FALSE;
//    }
}


static struct vmdesc *knownVMs = NULL;
static int knownVMsCount = 0;
static int knownVMsLimit = 0;

static void
GrowKnownVMs(int minimum)
{
    struct vmdesc* newKnownVMs;
    int newMax;

    newMax = (knownVMsLimit == 0 ? INIT_MAX_KNOWN_VMS : (2 * knownVMsLimit));
    if (newMax <= minimum) {
        newMax = minimum;
    }
    newKnownVMs = (struct vmdesc*) malloc(newMax * sizeof(struct vmdesc));
    if (knownVMs != NULL) {
        memcpy(newKnownVMs, knownVMs, knownVMsLimit * sizeof(struct vmdesc));
    }
    free(knownVMs);
    knownVMs = newKnownVMs;
    knownVMsLimit = newMax;
}


/* Returns index of VM or -1 if not found */
static int
KnownVMIndex(const _TCHAR* name)
{
    int i;
    if (_tcsncmp(name, TEXT("-J"), 2) == 0) name += 2;
    for (i = 0; i < knownVMsCount; i++) {
        if (!_tcscmp(name, knownVMs[i].name)) {
            return i;
        }
    }
    return -1;
}

static void
FreeKnownVMs()
{
    int i;
    for (i = 0; i < knownVMsCount; i++) {
        free(knownVMs[i].name);
        knownVMs[i].name = NULL;
    }
    free(knownVMs);
}




/*
 * Load a jvm from "jvmpath" and intialize the invocation functions.
 */
jboolean
LoadJavaVM(const _TCHAR *jvmpath, InvocationFunctions *ifn)
{
    HINSTANCE handle;

//    if (debug) {
//	printf("JVM path is %s\n", jvmpath);
//    }

    /* Load the Java VM DLL */
    if ((handle = LoadLibrary(jvmpath)) == 0) {
//	fprintf(stderr, "Error loading: %s\n", jvmpath);
	return JNI_FALSE;
    }

    /* Now get the function addresses */
    ifn->CreateJavaVM =
	(CreateJavaVM_t) GetProcAddress(handle, "JNI_CreateJavaVM");
    ifn->GetDefaultJavaVMInitArgs =
	(GetDefaultJavaVMInitArgs_t) GetProcAddress(handle, "JNI_GetDefaultJavaVMInitArgs");
    if (ifn->CreateJavaVM == 0 || ifn->GetDefaultJavaVMInitArgs == 0) {
//	fprintf(stderr, "Error: can't find JNI interfaces in: %s\n", jvmpath);
	return JNI_FALSE;
    }

    return JNI_TRUE;
}


/*
 * Returns a pointer to a block of at least 'size' bytes of memory.
 * Prints error message and exits if the memory could not be allocated.
 */
static void *
MemAlloc(size_t size)
{
    void *p = malloc(size);
    if (p == 0) {
//	perror("malloc");
//	exit(1);
    }
    return p;
}


/*
 * List of VM options to be specified when the VM is created.
 */
static JavaVMOption *options;
static int numOptions = 0, maxOptions = 0;

/*
 * Adds a new VM option with the given given name and value.
 */
static void
AddOption(char *str, void *info)
{
//    log(TEXT("Adding JVM option: "));

/*
    size_t buflen = strlen(str) * 2 + 2;
    wchar_t *debugstr = reinterpret_cast<wchar_t*> (MemAlloc(buflen));
    MultiByteToWideChar(
        CP_ACP,
        0,
        str,
        -1,
        debugstr,
        strlen(str) + 1
    );
    log(debugstr);
    free(debugstr);
*/

    /*
     * Expand options array if needed to accomodate at least one more
     * VM option.
     */
    if (numOptions >= maxOptions) {
	if (options == 0) {
	    maxOptions = 4;
	    options = reinterpret_cast<struct JavaVMOption*> (MemAlloc(maxOptions * sizeof(JavaVMOption)));
	} else {
	    JavaVMOption *tmp;
	    maxOptions *= 2;
	    tmp = reinterpret_cast<struct JavaVMOption*> (MemAlloc(maxOptions * sizeof(JavaVMOption)));
	    memcpy(tmp, options, numOptions * sizeof(JavaVMOption));
	    free(options);
	    options = tmp;
	}
    }
    options[numOptions].optionString = str;
    options[numOptions++].extraInfo = info;
}


static void
SetClassPath(const wchar_t *s) {
    size_t buflen = wcslen(s) * 2 + 2;
    char *def = reinterpret_cast<char*> (MemAlloc(buflen));
    strcpy(def, "-Djava.class.path=");
    char *dst = def + 18/*sizeof("-Djava.class.path=")*/;


    WideCharToMultiByte(
      CP_ACP,
      (size_t)0,
      s,
      (size_t)-1,
      dst,
      buflen,
      NULL,
      NULL
    );
    AddOption(def, NULL);
}


/*
 * Initializes the Java Virtual Machine. Also frees options array when
 * finished.
 */
static jboolean
InitializeJVM(JavaVM **pvm, JNIEnv **penv, InvocationFunctions *ifn)
{
    JavaVMInitArgs args;
    jint r;

    memset(&args, 0, sizeof(args));
    args.version  = JNI_VERSION_1_2;
    args.nOptions = numOptions;
    args.options  = options;
    args.ignoreUnrecognized = JNI_FALSE;
/*
    if (debug) {
	int i = 0;
	printf("JavaVM args:\n    ");
	printf("version 0x%08lx, ", (long)args.version);
	printf("ignoreUnrecognized is %s, ",
	       args.ignoreUnrecognized ? "JNI_TRUE" : "JNI_FALSE");
	printf("nOptions is %ld\n", (long)args.nOptions);
	for (i = 0; i < numOptions; i++)
	    printf("    option[%2d] = '%s'\n",
		   i, args.options[i].optionString);
    }
*/
    r = ifn->CreateJavaVM(pvm, (void **)penv, &args);
    free(options);
    return r == JNI_OK;
}

int initializeJava ()
{
    _TCHAR jrepath[MAXPATHLEN], jvmpath[MAXPATHLEN];
    /* Find out where the JRE is that we will be using. */
    log (TEXT("> initializeJava"));
    if (!GetJREPath(jrepath, sizeof(jrepath))) {
	//fprintf(stderr, "Error: could not find Java 2 Runtime Environment.\n");
	return 2;
    }

    log (TEXT("| JRE path found"));
    log (jrepath);

    /* Find the specified JVM type */
//    if (ReadKnownVMs(jrepath) < 1) {
	//fprintf(stderr, "Error: no known VMs. (check for corrupt jvm.cfg file)\n");
//	exit(1); // TODO?
//    }
//    jvmtype = CheckJvmType(&argc, &argv);

    jvmpath[0] = '\0';
    _TCHAR * jvmtype = TEXT("client");
    if (!GetJVMPath(jrepath, jvmtype, jvmpath, sizeof(jvmpath))) {
        log ("| Problem!");
	//fprintf(stderr, "Error: no `%s' JVM at `%s'.\n", jvmtype, jvmpath);
	return 4;
    }

    ifn.CreateJavaVM = 0;
    ifn.GetDefaultJavaVMInitArgs = 0;
    if (!LoadJavaVM(jvmpath, &ifn)) {
//        status = 1;
log ("| Problem 6!");
//fatalProblem ("Problem\nCannot load Java VM");
	return 6;
    }
//    printf("JVM was found\n");

    if (!InitializeJVM(&vm, &env, &ifn)) {
//	fprintf(stderr, "Could not create the Java virtual machine.\n");
//        status = 3;
log ("| Problem 1!");
//fatalProblem ("Problem\nCould not create the Java virtual machine.");
	return 1;
    }

//    printf("JVM was initialized\n");
//    printf ("| vm = %d\n", vm);
//    printf ("| env = %d\n", env);
     log ("< initializeJava");
     return 0;
}
