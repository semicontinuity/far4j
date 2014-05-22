#include <CRT/crt.hpp>
#include <stdlib.h>
#include <plugin.hpp>
#include "far4jLng.hpp"
#include "version.hpp"
#include <initguid.h>
#include "guid.hpp"
#include "far4j.hpp"
#include <tchar.h>


static struct PluginStartupInfo Info;


#include "logging-support.cxx"
#include "java-support.cxx"


const int DLL_FILE_NAME_LENGTH = 9; // far4j.dll

static _TCHAR classPath[2048];

static const _TCHAR* PLUGIN_CLASS_FILE_NAME = TEXT("plugin-class");


static _TCHAR pluginClassFileName[MAXPATHLEN];
static char pluginClass[128];




static jclass    jcls_AbstractPlugin;
static jclass    jcls_AbstractPluginInstance;
static jclass    jcls_Plugin;
static jclass    jcls_PluginPanelItem;

static jmethodID jmid_PluginInstance_getFlags;
static jmethodID jmid_PluginInstance_getFiles;
static jmethodID jmid_PluginInstance_setDirectory;
static jmethodID jmid_PluginInstance_getFindData;
static jmethodID jmid_PluginInstance_getStartPanelMode;

static jobject jobj_Plugin;
static jobject jobj_PluginReference;

static jobject jobj_PluginInstance;
static jobject jobj_PluginInstanceReference;


#if defined(__GNUC__)

#ifdef __cplusplus
extern "C"{
#endif
  BOOL WINAPI DllMainCRTStartup(HANDLE hDll,DWORD dwReason,LPVOID lpReserved);
#ifdef __cplusplus
};
#endif

BOOL WINAPI DllMainCRTStartup(HANDLE hDll,DWORD dwReason,LPVOID lpReserved)
{
	(void) lpReserved;
	(void) dwReason;
	(void) hDll;
	return TRUE;
}
#endif


void WINAPI GetGlobalInfoW(struct GlobalInfo *Info)
{
//log (TEXT("| GetGlobalInfoW"));
	Info->StructSize=sizeof(GlobalInfo);
	Info->MinFarVersion=FARMANAGERVERSION;
	Info->Version=PLUGIN_VERSION;
	Info->Guid=MainGuid;
	Info->Title=PLUGIN_NAME;
	Info->Description=PLUGIN_DESC;
	Info->Author=PLUGIN_AUTHOR;
}

/*
 ������� GetMsg ���������� ������ ��������� �� ��������� �����.
 � ��� ���������� ��� Info.GetMsg ��� ���������� ���� :-)
*/
const wchar_t *GetMsg(int MsgId)
{
	return Info.GetMsg(&MainGuid,MsgId);
}


/*
JNIEnv* create_vm(JavaVM ** jvm) {

    JavaVMInitArgs vm_args;
    int ret;
    JavaVMOption options;

    //Path to the java source code
    options.optionString = "-Djava.class.path=C:\\my.jar";
    vm_args.version = JNI_VERSION_1_6; //JDK version. This indicates version 1.6
    vm_args.nOptions = 1;
    vm_args.options = &options;
    vm_args.ignoreUnrecognized = 0;

    ret = JNI_CreateJavaVM(jvm, (void**)&env, &vm_args);

    if(ret < 0)
        log("Unable to Launch JVM");
    else
        log("Able to Launch JVM");
    return env;
}
*/

/*
������� SetStartupInfoW ���������� ���� ���, ����� �����
������� ���������. ��� ���������� ������� ����������,
����������� ��� ���������� ������.
*/
void WINAPI SetStartupInfoW(const struct PluginStartupInfo *psi)
{
	Info=*psi;


    size_t pluginFolderNameLength = _tcslen(Info.ModuleName) - DLL_FILE_NAME_LENGTH;

/*
    // Get the plugin class
    // =========================================================================
    // construct the name of the file containing the name of plugin class
    _tcsncpy (pluginClassFileName, Info.ModuleName, pluginFolderNameLength);
    _tcscpy (pluginClassFileName + pluginFolderNameLength, PLUGIN_CLASS_FILE_NAME);
    // read the classpath from file
    FILE* pluginClassFile = _tfopen(pluginClassFileName, TEXT("rt"));
    if (pluginClassFile == NULL) return; // TODO
    size_t pluginClassFileLength = fread(pluginClass, 1, sizeof(pluginClass)-1, pluginClassFile);
    pluginClass[pluginClassFileLength] = 0;
    fclose(pluginClassFile);
    log (TEXT("Plugin class OK"));
*/

    // construct classpath: all jars in the lib directory are added
    // =========================================================================
    WIN32_FIND_DATA FindFileData;
    HANDLE hFind = INVALID_HANDLE_VALUE;
    _TCHAR DirSpec[MAX_PATH + 1];
    DWORD dwError;
    _tcsncpy (DirSpec, Info.ModuleName, pluginFolderNameLength);
    _tcsncpy (DirSpec + pluginFolderNameLength, TEXT("*.jar"), 6);

//    log ("Searching files: ", DirSpec);

    hFind = FindFirstFile(DirSpec, &FindFileData);
    if (hFind != INVALID_HANDLE_VALUE) {
        if (classPath[0] != 0) _tcscat(classPath, TEXT(";"));
        _tcsncat (classPath, Info.ModuleName, pluginFolderNameLength);
        _tcscat  (classPath, FindFileData.cFileName);
        while (FindNextFile(hFind, &FindFileData) != 0) 
        {
            _tcscat  (classPath, TEXT(";"));
            _tcsncat (classPath, Info.ModuleName, pluginFolderNameLength);
            _tcscat  (classPath, FindFileData.cFileName);
        }
        FindClose(hFind);
    }

//    log(classPath);
    SetClassPath(classPath);

    _TCHAR jrepath[MAXPATHLEN];
    /* Find out where the JRE is that we will be using. */
    if (!GetJREPath(jrepath, sizeof(jrepath))) {
        log(TEXT("Could not find JRE path in registry\n"));
        return;
    }

    _tcscat(jrepath, TEXT("\\bin\\server\\jvm.dll"));

// sorry, hardcoded for a while
//    if (!LoadJavaVM(TEXT("C:\\Program Files\\Java\\jdk1.7.0_45\\jre\\bin\\server\\jvm.dll"), &ifn)) {
    if (!LoadJavaVM(jrepath, &ifn)) {
//    if (!LoadJavaVM(TEXT("C:\\Program Files\\Java\\jdk1.7.0_45\\jre\\bin\\java.dll"), &ifn)) {
//        status = 1;
        log (TEXT("| Problem 6!"));
//fatalProblem ("Problem\nCannot load Java VM");
	return;
    }

    if (!InitializeJVM(&vm, &env, &ifn)) {
//	fprintf(stderr, "Could not create the Java virtual machine.\n");
//        status = 3;
log ("| Problem 1!");
//fatalProblem ("Problem\nCould not create the Java virtual machine.");
	return;
    }

//    log (TEXT("| VM Initialized"));

//    if (vm) log (TEXT("| VM OK"));


    jcls_AbstractPlugin = env->FindClass ("org/farmanager/api/AbstractPlugin");
    if (jcls_AbstractPlugin == 0) {
        log (TEXT("jcls_AbstractPlugin == 0"));
        return;
    }
    jmethodID jmet_instance = env->GetStaticMethodID (jcls_AbstractPlugin, "instance", "()Lorg/farmanager/api/AbstractPlugin;");
    if (jmet_instance == 0) {
        log (TEXT("jmet_instance == 0"));
        return;
    }
    jobj_Plugin = env->CallStaticObjectMethod (jcls_AbstractPlugin, jmet_instance);
    if (jobj_Plugin == 0) {
        log (TEXT("jobj_Plugin := 0"));
        return;
    }
    jobj_PluginReference = env->NewGlobalRef(jobj_Plugin);
    if (jobj_PluginReference == 0) {
        log (TEXT("jobj_PluginReference := 0"));
        return;
    }

    jmethodID jmid_setModuleName = env->GetMethodID (jcls_AbstractPlugin, "setModuleName", "(Ljava/lang/String;)V");
    if (jmid_setModuleName == 0) {
        log (TEXT("jmid_setModuleName := 0"));
        return;
    }
    jstring jstr_ModuleName = env->NewString((const jchar*) psi->ModuleName, (jsize)_tcslen(psi->ModuleName));
    env->CallVoidMethod (jobj_PluginReference, jmid_setModuleName, jstr_ModuleName);


    jcls_AbstractPluginInstance = env->FindClass("org/farmanager/api/AbstractPluginInstance");
    if (jcls_AbstractPluginInstance == 0) {
        log (TEXT("jcls_AbstractPluginInstance == 0"));
        return;
    }

    // pre-initialize commonly used classes and methods
    jmid_PluginInstance_getFindData = env->GetMethodID (
        jcls_AbstractPluginInstance, "getFindData", "(I)[Lorg/farmanager/api/PluginPanelItem;");
    if (jmid_PluginInstance_getFindData == 0) {
        log (TEXT("jmid_PluginInstance_getFindData == 0"));
        return;
    }


    // it was 0 sometimes if initialized in the GetOpenPluginInfo (?)
    // TODO: there is a suspicion that these 0's occur if an exception was thrown!
    jmid_PluginInstance_getStartPanelMode = env->GetMethodID (
        jcls_AbstractPluginInstance, "getStartPanelMode", "()I");
    if (jmid_PluginInstance_getStartPanelMode == 0) {
        log (TEXT("jmid_PluginInstance_getStartPanelMode == 0"));
        return;
    }

    jcls_PluginPanelItem = env->FindClass ("org/farmanager/api/PluginPanelItem");
    if (jcls_PluginPanelItem == 0) {
        log (TEXT("jcls_PluginPanelItem == 0"));
        return;
    }
}


static void copyPluginMenuItem(
    struct PluginMenuItem *dst,
    const jclass jcls_PluginInfo,
    const jobject jobj_PluginInfo,
    const char *field)
{
    const jclass jcls_PluginMenuItem = env->FindClass("org/farmanager/api/jni/PluginMenuItem");
    if (jcls_PluginMenuItem == 0) {
        log (TEXT("jcls_PluginMenuItem == 0"));
        return;
    }
    const jfieldID fidGuid = env->GetFieldID(jcls_PluginMenuItem, "guid", "Ljava/util/UUID;");
    if (fidGuid == 0) {
        log(TEXT("fidGuid == 0"));
        return;
    }
    const jfieldID fidString = env->GetFieldID(jcls_PluginMenuItem, "string", "Ljava/lang/String;");
    if (fidString == 0) {
        log(TEXT("fidString == 0"));
        return;
    }

    const jclass jcls_UUID = env->FindClass("java/util/UUID");
    if (jcls_UUID == 0) {
        log (TEXT("jcls_UUID == 0"));
        return;
    }
    const jfieldID fidMSB = env->GetFieldID(jcls_UUID, "mostSigBits", "J");
    if (fidMSB == 0) {
        log(TEXT("fidMSB == 0"));
        return;
    }
    const jfieldID fidLSB = env->GetFieldID(jcls_UUID, "leastSigBits", "J");
    if (fidLSB == 0) {
        log(TEXT("fidLSB == 0"));
        return;
    }


    const jfieldID fid = env->GetFieldID (jcls_PluginInfo, field, "[Lorg/farmanager/api/jni/PluginMenuItem;");
    if (fid == 0) {
        log(TEXT("fid == 0"));
        return;
    }

    const jobjectArray array = (jobjectArray) env->GetObjectField(jobj_PluginInfo, fid);
    if (array == 0) {
        return;
    }
    else {
        const int length = env->GetArrayLength(array);
        const wchar_t**strings = (const wchar_t**)malloc(sizeof(wchar_t*)*length);
        dst->Count = length;
        dst->Guids = (GUID *)malloc(sizeof(GUID)*length);

        for (int i = 0; i < length; i++) {
            const jobject element = env->GetObjectArrayElement(array, i);
            // assert element != 0
            const jobject guid = (jobject) env->GetObjectField(element, fidGuid);
            const jlong msb = env->GetLongField(guid, fidMSB);
            const jlong lsb = env->GetLongField(guid, fidLSB);
            memcpy((void*)&dst->Guids[i].Data1, &lsb, 8);
            memcpy((void*)&dst->Guids[i].Data2, &msb, 8);

            const jstring string = (jstring) env->GetObjectField(element, fidString);
            const wchar_t* s = (const wchar_t*)env->GetStringChars(string, 0);  // TODO release
            strings[i] = s;
        }

        dst->Strings = strings;
    }
}


void WINAPI GetPluginInfoW(struct PluginInfo *Info) {
	Info->StructSize=sizeof(*Info);
	Info->Flags=PF_EDITOR;
	static const wchar_t *PluginMenuStrings[1];
	PluginMenuStrings[0]=GetMsg(MTitle);
	Info->PluginMenu.Guids=&MenuGuid;
	Info->PluginMenu.Strings=PluginMenuStrings;
	Info->PluginMenu.Count=ARRAYSIZE(PluginMenuStrings);

    const jmethodID jmid_getPluginInfo = env->GetMethodID (jcls_AbstractPlugin, "getPluginInfo", "()Lorg/farmanager/api/jni/PluginInfo;");
    if (jmid_getPluginInfo == 0) {
        log(TEXT("jmid_getPluginInfo := 0"));
        return;
    }
    const jobject jobj_PluginInfo = env->CallObjectMethod (jobj_PluginReference, jmid_getPluginInfo);
    if (jobj_PluginInfo != 0) {
        const jclass jcls_PluginInfo = env->FindClass ("org/farmanager/api/jni/PluginInfo");
        if (jcls_PluginInfo == 0) {
            log (TEXT("jcls_PluginInfo == 0"));
            return;
        }

        const jfieldID fidFlags         = env->GetFieldID (jcls_PluginInfo, "flags", "J");
        const jlong flags = env->GetLongField(jobj_PluginInfo, fidFlags);
        Info->Flags = (PLUGIN_FLAGS)flags;

        copyPluginMenuItem(&Info->DiskMenu, jcls_PluginInfo, jobj_PluginInfo, "diskMenu");
        copyPluginMenuItem(&Info->PluginMenu, jcls_PluginInfo, jobj_PluginInfo, "pluginMenu");
        copyPluginMenuItem(&Info->PluginConfig, jcls_PluginInfo, jobj_PluginInfo, "pluginConfig");

        const jfieldID fidCommandPrefix = env->GetFieldID (jcls_PluginInfo, "commandPrefix", "Ljava/lang/String;");
        if (fidCommandPrefix == 0) {
            log (TEXT("fidCommandPrefix == 0"));
            return;
        }

        const jstring jstr_CommandPrefix = (jstring)env->GetObjectField(jobj_PluginInfo, fidCommandPrefix);
        if (jstr_CommandPrefix != 0) {
            const wchar_t* s = (const wchar_t*)env->GetStringChars(jstr_CommandPrefix, 0);  // TODO release?
            Info->CommandPrefix = s;
        }
    }
    else {
        log(TEXT("jobj_PluginInfo == 0"));
    }
}


/**
 * An instance of this structure is allocated per every plugin instance.
 * A pointer to this structure is used as plugin instance handle.
 */
struct PluginInstanceData
{
    /** Reference to a java counterpart - an instance of AbstractPluginInstance */
    jobject instance;
    PanelMode panelModes[10];
    const _TCHAR** panelModeColumnTitles[10];
    //KeyBarTitles keyBarTitles;
    InfoPanelLine* infoPanelLines;
};

/**
 * Additional data attached to every PluginPanelItem.
 * A pointer to this structure is stored in PluginPanelItem.UserData
 */
struct PluginPanelItemData
{
    _TCHAR** customColumns;
    jstring* customColumnStrings; // we have to store pointers to jstrings to be able to release memory
    jstring jDescription;
};


/*
  ������� OpenPluginW ���������� ��� �������� ����� ����� �������.
*/
HANDLE WINAPI OpenW(const struct OpenInfo *OInfo) {
//    if (initJavaResult != 0) return INVALID_HANDLE_VALUE; // TODO?
//    log (TEXT("> OpenPlugin"));
    jmethodID jmid_AbstractPlugin_createInstance = env->GetMethodID (
        jcls_AbstractPlugin, "createInstance", "()Lorg/farmanager/api/AbstractPluginInstance;");
    if (jmid_AbstractPlugin_createInstance == 0) {
        log (TEXT("jmid_AbstractPlugin_createInstance == 0"));
        return 0;
    }

    jobj_PluginInstance = env->CallObjectMethod (jobj_Plugin, jmid_AbstractPlugin_createInstance);
    if (jobj_PluginInstance == 0) {
        log (TEXT("jobj_PluginInstance == 0"));
        return 0;
    }
    jobj_PluginInstanceReference = env->NewGlobalRef(jobj_PluginInstance);


    if (OInfo->OpenFrom == OPEN_COMMANDLINE) {
//        jfieldID jfid_commandLine = env->GetFieldID (jcls_AbstractPluginInstance, "commandLine",   "Ljava/lang/String;");
//        env->SetObjectField(jobj_PluginInstance, jfid_commandLine,
//            env->NewStringUTF (reinterpret_cast<char*> (item))); // TODO: think of GC, etc...
    }

    // init() is called after all data from OInfo are passed to plugin instance.
    jmethodID jmid_AbstractPluginInstance_init = env->GetMethodID (
        jcls_AbstractPluginInstance, "init", "()V");
    if (jmid_AbstractPluginInstance_init == 0) {
        log (TEXT("jmid_AbstractPluginInstance_init == 0"));
        return 0;
    }
    env->CallVoidMethod (jobj_PluginInstance, jmid_AbstractPluginInstance_init);

    PluginInstanceData* data = new PluginInstanceData;
    data->instance = jobj_PluginInstance;

//    log (TEXT("< OpenPlugin"));
    return reinterpret_cast<HANDLE> (data);
}


intptr_t WINAPI GetFindDataW(struct GetFindDataInfo *Info) {
    log ("> GetFindData");
    PluginInstanceData* data = reinterpret_cast<PluginInstanceData*> (Info->hPanel);
    jobject jobj_PluginInstance = data->instance;

    jobjectArray pluginPanelItemArray = (jobjectArray) env->CallObjectMethod (
        jobj_PluginInstance, jmid_PluginInstance_getFindData, Info->OpMode);
    //log ("pluginPanelItemArray:", (int)pluginPanelItemArray);
    if (pluginPanelItemArray == 0) return 0;

    int length = env->GetArrayLength (pluginPanelItemArray);
    // TODO: check for thrown exception
    //log ("length:", length);


    if (length > 0)
    {
    // *************************************************************************
    PluginPanelItem* items = new PluginPanelItem [length];
    // *************************************************************************
    log (TEXT("ITEMS:"), (int)items);
    // TODO: currently only 3 String-type columns can be used by plugins (name, owner, description)
    // TODO: this should be changed - just use custom columns always.. (perhaps leave name as PK to be able to navigate)
    jfieldID fidName              = env->GetFieldID (jcls_PluginPanelItem, "cFileName", "Ljava/lang/String;");
    jfieldID fid_dwFileAttributes = env->GetFieldID (jcls_PluginPanelItem, "dwFileAttributes", "I");
    jfieldID jfid_nFileSizeLow    = env->GetFieldID (jcls_PluginPanelItem, "nFileSizeLow",     "I");
    jfieldID jfid_nFileSizeHigh   = env->GetFieldID (jcls_PluginPanelItem, "nFileSizeHigh",    "I");
    jfieldID fidDescription       = env->GetFieldID (jcls_PluginPanelItem, "description", "Ljava/lang/String;");
    jfieldID fidOwner             = env->GetFieldID (jcls_PluginPanelItem, "owner", "Ljava/lang/String;");

    jfieldID fidCreationTime      = env->GetFieldID (jcls_PluginPanelItem, "lCreationTime", "J");
    jfieldID fidLastWriteTime     = env->GetFieldID (jcls_PluginPanelItem, "lLastWriteTime", "J");
    jfieldID jfid_numberOfLinks   = env->GetFieldID (jcls_PluginPanelItem, "numberOfLinks",    "I");
    jfieldID jfid_crc32           = env->GetFieldID (jcls_PluginPanelItem, "crc32",    "I");
    jfieldID jfid_customColumns   = env->GetFieldID (jcls_PluginPanelItem, "customColumns",    "[Ljava/lang/String;");

    for (int i=0; i < length; i++)
    {
        //log ("ITER");
        jobject element = env->GetObjectArrayElement (pluginPanelItemArray, i);
        //log ("element:");

        memset (&items[i], 0, sizeof(PluginPanelItem));

        PluginPanelItemData* userData = new PluginPanelItemData;
        items[i].UserData.Data = userData;
        memset (userData, 0, sizeof(PluginPanelItemData));

        // FileName
        // -----------------------------------------------------------------------------------
        jstring name = (jstring) env->GetObjectField (element, fidName);
        const _TCHAR* fileName = (const _TCHAR*)env->GetStringChars (name, 0);
        //log (cFileName);
        items[i].FileName = fileName;
        //env->ReleaseStringUTFChars (name, cFileName);

        // FileAttributes
        // -----------------------------------------------------------------------------------     
        jint j_FileAttributes = env->GetIntField (element, fid_dwFileAttributes);
        items[i].FileAttributes = j_FileAttributes;

        // FileSize
        // -----------------------------------------------------------------------------------     
        jint j_nFileSizeLow = env->GetIntField (element, jfid_nFileSizeLow);
        jint j_nFileSizeHigh = env->GetIntField (element, jfid_nFileSizeHigh);
        items[i].FileSize = (((unsigned long long)j_nFileSizeHigh)<<32) | (unsigned long long)j_nFileSizeLow;

        // CreationTime
        // -----------------------------------------------------------------------------------
        jlong j_lCreationTime = env->GetLongField (element, fidCreationTime);
        items[i].CreationTime.dwLowDateTime  = (DWORD)(j_lCreationTime);
        items[i].CreationTime.dwHighDateTime = (DWORD)(j_lCreationTime>>32);

        // LastWriteTime
        // -----------------------------------------------------------------------------------
        jlong j_lLastWriteTime = env->GetLongField (element, fidLastWriteTime);
        items[i].LastWriteTime.dwLowDateTime  = (DWORD)(j_lLastWriteTime);
        items[i].LastWriteTime.dwHighDateTime = (DWORD)(j_lLastWriteTime>>32);
        

        // Description
        // -----------------------------------------------------------------------------------
        jstring desc = (jstring) env->GetObjectField (element, fidDescription);
        userData->jDescription = desc;
        if (desc != NULL)
            items[i].Description = (const _TCHAR*)env->GetStringChars (desc, 0);

        // Owner
        // -----------------------------------------------------------------------------------
        jstring owner = (jstring) env->GetObjectField (element, fidOwner);
        if (owner != NULL)
            items[i].Owner = (const _TCHAR*)env->GetStringChars (owner, 0);


        // other fields
        // -----------------------------------------------------------------------------------
        jint j_numberOfLinks = env->GetIntField (element, jfid_numberOfLinks);
        items[i].NumberOfLinks = j_numberOfLinks;

        jint j_crc32 = env->GetIntField (element, jfid_crc32);
        items[i].CRC32 = j_crc32;

        jobjectArray customColumns = (jobjectArray) env->GetObjectField(
            element, jfid_customColumns);
        if (customColumns != 0)
        {
            int l = env->GetArrayLength (customColumns);

            items[i].CustomColumnNumber = l;            
            if (l > 0)
            {                
                //userData->customColumns = new _TCHAR*[l];
                //userData->customColumnStrings = new jstring[l];
                const _TCHAR** custom  = new const _TCHAR*[l];
                items[i].CustomColumnData = custom;
                for (int j = 0; j < l; j++) {
                    jstring columnData = (jstring) env->GetObjectArrayElement (customColumns, j);
//                    userData->customColumnStrings[j] = columnData;
//                    userData->customColumns[j] = columnData == NULL ? NULL : (_TCHAR*)env->GetStringChars (columnData, 0);
                    custom[j] = columnData == NULL ? NULL : (const _TCHAR*)env->GetStringChars (columnData, 0);
                }
            }
        }       
    }
    Info->PanelItem   = items;
    }

//    items[3].FindData.dwFileAttributes = FILE_ATTRIBUTE_SYSTEM;
//    strcpy (items[3].FindData.cFileName, "system");
    Info->ItemsNumber = length;
    //log("Populated items: ", length);

    log("< GetFindData");
    return TRUE;
}


void WINAPI FreeFindDataW(const struct FreeFindDataInfo *Info) {
}


intptr_t WINAPI SetDirectoryW(
  const struct SetDirectoryInfo *Info)
{
    //log ("> SetDirectory");
    //log ("| directory:");
    //log (dir);
    
    PluginInstanceData* data = reinterpret_cast<PluginInstanceData*> (Info->hPanel);
    jobject jobj_PluginInstance = data->instance;
//    jobject jobj_PluginInstance = reinterpret_cast<jobject> (hPlugin);


    jmethodID jmid_AbstractPluginInstance_setDirectory = env->GetMethodID (
        jcls_AbstractPluginInstance, "setDirectory", "(Ljava/lang/String;)V");
    //log ("| jmid_AbstractPluginInstance_setDirectory=", (int)jmid_AbstractPluginInstance_setDirectory);
    if (jmid_AbstractPluginInstance_setDirectory== NULL) return FALSE; // TODO proper panic
    //log ("| CallVoidMethod setDirectory");
    jstring jstr_dir = env->NewString ((const jchar*)Info->Dir, _tcslen(Info->Dir)); // TODO: think of GC, etc...
    env->CallVoidMethod (jobj_PluginInstance, jmid_AbstractPluginInstance_setDirectory, jstr_dir);

    //log ("< SetDirectory");
    return TRUE;
}


/**
 * GetOpenPanelInfoW
 * Fills the provided struct OpenPanelInfo:
 *   Flags = pluginInstance.getFlags()
 *   HostFile = pluginInstance.getHostFile()
 *   CurDir = pluginInstance.getCurDir()
 */
void WINAPI GetOpenPanelInfoW(struct OpenPanelInfo *openPluginInfo) {
    PluginInstanceData* data = reinterpret_cast<PluginInstanceData*> (openPluginInfo->hPanel);
    jobject jobj_PluginInstance = data->instance;

    // StructSize
    openPluginInfo->StructSize = sizeof(OpenPanelInfo);

    // Flags (NB now 64 bit!)
    jmethodID jmid_AbstractPluginInstance_getFlags = env->GetMethodID (
        jcls_AbstractPluginInstance, "getFlags", "()J");
    if (jmid_AbstractPluginInstance_getFlags == 0) {
        log(TEXT("jmid_AbstractPluginInstance_getFlags := 0"));
        return ;
    }
    jlong Flags = env->CallIntMethod (jobj_PluginInstance, jmid_AbstractPluginInstance_getFlags);
    openPluginInfo->Flags  = Flags;

    // HostFile
    // Name of the file used to emulate the file system.
    // If plugin does not emulate a file system based on a file, set this variable to NULL.
    jmethodID jmid_PluginInstance_getHostFile = env->GetMethodID (
        jcls_AbstractPluginInstance, "getHostFile", "()Ljava/lang/String;");
//    log ("| jmid_PluginInstance_getHostFile=", (int)jmid_PluginInstance_getHostFile);
    if (jmid_PluginInstance_getHostFile== NULL) return; // TODO
//    log ("| CallObjectMethod getHostFile");
    jstring jstr_HostFile = (jstring)env->CallObjectMethod (jobj_PluginInstance, jmid_PluginInstance_getHostFile);
    openPluginInfo->HostFile   = jstr_HostFile == NULL ? NULL : (const _TCHAR*)env->GetStringChars(jstr_HostFile, 0); // TODO not released!

    // CurDir
    // Current plugin directory.
    // If a plugin does not support a current directory, set it to an empty string.
    // If a plugin returns an empty string in this field, it will be closed automatically when Enter is pressed on "..".
    jmethodID jmid_PluginInstance_getCurDir = env->GetMethodID (
        jcls_AbstractPluginInstance, "getCurDir", "()Ljava/lang/String;");
//    log ("| jmid_PluginInstance_getCurDir=", (int)jmid_PluginInstance_getCurDir);
    if (jmid_PluginInstance_getCurDir== NULL) return; // TODO
//    log ("| CallObjectMethod getCurDir");
    jstring jstr_CurDir = (jstring)env->CallObjectMethod (jobj_PluginInstance, jmid_PluginInstance_getCurDir);
    openPluginInfo->CurDir = jstr_CurDir == NULL ? NULL : (const _TCHAR*)env->GetStringChars(jstr_CurDir, 0); // TODO not released!


    // Format
    // Name of the plugin format. It is displayed in the FAR copy dialog.
    jmethodID jmid_PluginInstance_getFormat      = env->GetMethodID (
        jcls_AbstractPluginInstance, "getFormat", "()Ljava/lang/String;");
//    log ("| jmid_PluginInstance_getFormat=", (int)jmid_PluginInstance_getFormat);
    if (jmid_PluginInstance_getFormat== NULL) return; // TODO
//    log ("| CallObjectMethod getFormat");
    jstring jstr_Format= (jstring)env->CallObjectMethod (jobj_PluginInstance, jmid_PluginInstance_getFormat);
    openPluginInfo->Format = jstr_Format == NULL ? NULL : (const _TCHAR*)env->GetStringChars(jstr_Format, 0); // TODO not released!

    // PanelTitle
    // Plugin panel title.
    jmethodID jmid_PluginInstance_getPanelTitle = env->GetMethodID (
        jcls_AbstractPluginInstance, "getPanelTitle", "()Ljava/lang/String;");
//    log ("| jmid_PluginInstance_getPanelTitle=", (int)jmid_PluginInstance_getPanelTitle);
    if (jmid_PluginInstance_getPanelTitle== NULL) return; // TODO
//    log ("| CallObjectMethod getPanelTitle");
    jstring jstr_PanelTitle = (jstring)env->CallObjectMethod (jobj_PluginInstance, jmid_PluginInstance_getPanelTitle);
    openPluginInfo->PanelTitle = jstr_PanelTitle == NULL ? NULL : (const _TCHAR*)env->GetStringChars(jstr_PanelTitle, 0); // TODO not released!

    // InfoLines
    // Address of an array of InfoPanelLine structures.
    // Each structure describes one line in the information panel.
    // If you do not need to display plugin dependent text in information panel, set InfoLines to NULL.

    // InfoLinesNumber
    // Number of InfoPanelLine structures.
    jmethodID jmid_AbstractPluginInstance_getInfoPanelLines = env->GetMethodID (
        jcls_AbstractPluginInstance, "getInfoPanelLines", "()[Lorg/farmanager/api/jni/FarInfoPanelLine;");
//    log ("| CallObjectMethod getInfoPanelLines");
    jobjectArray infoPanelLinesArray = (jobjectArray) env->CallObjectMethod (
        jobj_PluginInstance, jmid_AbstractPluginInstance_getInfoPanelLines);
//    log ("infoPanelLinesArray:", (int)infoPanelLinesArray);
    if (infoPanelLinesArray != NULL) {
        int length = env->GetArrayLength (infoPanelLinesArray);
//        log ("length:", length);
        openPluginInfo->InfoLinesNumber = length;
        if (length > 0) {
            data->infoPanelLines = new InfoPanelLine[length];
            openPluginInfo->InfoLines = data->infoPanelLines;

            jclass jcls_FarInfoPanelLine     = env->FindClass ("org/farmanager/api/jni/FarInfoPanelLine");
            jfieldID jfid_text      = env->GetFieldID (jcls_FarInfoPanelLine, "text",      "Ljava/lang/String;");
            jfieldID jfid_data      = env->GetFieldID (jcls_FarInfoPanelLine, "data",      "Ljava/lang/String;");
            jfieldID jfid_separator = env->GetFieldID (jcls_FarInfoPanelLine, "separator", "Z");
            for (int i=0; i < length; i++) {
                jobject element = env->GetObjectArrayElement (infoPanelLinesArray, i);

                jstring jstr_text = (jstring) env->GetObjectField (element, jfid_text);
                const _TCHAR* ctext = (const _TCHAR*) env->GetStringChars (jstr_text, 0);
                data->infoPanelLines[i].Text = ctext;
                //env->ReleaseStringUTFChars (jstr_text, ctext);

                jstring jstr_data = (jstring) env->GetObjectField (element, jfid_data);
                const _TCHAR* cdata = (const _TCHAR*) env->GetStringChars (jstr_data, 0);
                data->infoPanelLines[i].Data = cdata;
                //env->ReleaseStringUTFChars (jstr_data, cdata);

                jboolean jboo_separator = env->GetBooleanField (element, jfid_separator);
                data->infoPanelLines[i].Flags = jboo_separator ? IPLFLAGS_SEPARATOR : 0;
            }
        }
    }

    // ...

    // PanelModesArray
    // Address of an array of PanelMode structures. Using it, you can redefine view mode settings.
    // The first structure describes view mode 0, the second one describes mode 1 and so on.
    // If you do not need to define new panel modes, set PanelModesArray to NULL.
    //
    // PanelModesNumber
    // Number of  PanelMode structures

    // TODO: dynamic data structures are not freed! Can add lazy init...
    jmethodID jmid_AbstractPluginInstance_getPanelModes = env->GetMethodID (
        jcls_AbstractPluginInstance, "getPanelModes", "()[Lorg/farmanager/api/PanelMode;");
    //log ("| CallObjectMethod getPanelModes");
    jobjectArray panelModeArray = (jobjectArray) env->CallObjectMethod (
        jobj_PluginInstance, jmid_AbstractPluginInstance_getPanelModes);
    //log ("panelModeArray:", (int)panelModeArray);
    if (panelModeArray != 0)
    {
    int length = env->GetArrayLength (panelModeArray);
    //log ("length:", length);
    if (length > 0)
    {
        jclass jcls_PanelMode            = env->FindClass ("org/farmanager/api/PanelMode");
        jfieldID jfid_columnTypes        = env->GetFieldID (jcls_PanelMode, "columnTypes",    "Ljava/lang/String;");
        jfieldID jfid_columnWidths       = env->GetFieldID (jcls_PanelMode, "columnWidths",   "Ljava/lang/String;");
        jfieldID jfid_columnTitles       = env->GetFieldID (jcls_PanelMode, "columnTitles",   "[Ljava/lang/String;");
        jfieldID jfid_flags              = env->GetFieldID (jcls_PanelMode, "flags",          "J");
        jfieldID jfid_statusColumnTypes  = env->GetFieldID (jcls_PanelMode, "statusColumnTypes",    "Ljava/lang/String;");
        jfieldID jfid_statusColumnWidths = env->GetFieldID (jcls_PanelMode, "statusColumnWidths",   "Ljava/lang/String;");
        for (int i=0; i < length; i++) {
            const jobject element = env->GetObjectArrayElement(panelModeArray, i);
            // TODO: check element for NULL
            memset(&(data->panelModes[i]), 0, sizeof(PanelMode));

            const jlong flags = env->GetLongField (element, jfid_flags);
            data->panelModes[i].Flags = flags;

            jstring jstr_columnTypes = (jstring) env->GetObjectField (element, jfid_columnTypes);
            data->panelModes[i].ColumnTypes = (const _TCHAR*) env->GetStringChars (jstr_columnTypes, 0);
//            log ("| ColumnTypes=", data->panelModes[i].ColumnTypes);

            jstring jstr_columnWidths = (jstring) env->GetObjectField (element, jfid_columnWidths);
            data->panelModes[i].ColumnWidths = (const _TCHAR*) env->GetStringChars (jstr_columnWidths, 0);
//            log ("| ColumnWidths=", data->panelModes[i].ColumnWidths);

            jobjectArray joar_columnTitles = (jobjectArray) env->GetObjectField (element, jfid_columnTitles);
            if (joar_columnTitles != NULL)
            {
                int length = env->GetArrayLength (joar_columnTitles);
                if (length > 0)
                {
                    // TODO: now we do not release anything! We should!
                    data->panelModeColumnTitles[i] = new const _TCHAR*[length];
                    for (int j=0; j < length; j++)
                    {
                        jstring jstr_title = (jstring) env->GetObjectArrayElement (joar_columnTitles, j);
                        // TODO: handle NULL

                        data->panelModeColumnTitles[i][j] = (const _TCHAR*) env->GetStringChars (jstr_title, 0);
                        // TODO: memory
                    }
                    data->panelModes[i].ColumnTitles = data->panelModeColumnTitles[i];
                }
            }

            // TODO: GC!
            jstring jstr_statusColumnTypes = (jstring) env->GetObjectField (element, jfid_statusColumnTypes);
            if (jstr_statusColumnTypes != NULL)
                data->panelModes[i].StatusColumnTypes = (const _TCHAR*) env->GetStringChars (jstr_statusColumnTypes, 0);

            // TODO: GC!
            jstring jstr_statusColumnWidths = (jstring) env->GetObjectField (element, jfid_statusColumnWidths);
            if (jstr_statusColumnWidths != NULL)
                data->panelModes[i].StatusColumnWidths = (const _TCHAR*) env->GetStringChars (jstr_statusColumnWidths, 0);
        }
        openPluginInfo->PanelModesNumber = length;
        openPluginInfo->PanelModesArray = data->panelModes;
    }
    }

    // StartPanelMode
    // View mode that will be set directly after creating the plugin panel.
    // It must be in the '0'+<view mode number> form.
    // For example, '1' or 49 will set the view mode Brief.
    // If you do not wish to change the panel view mode after starting plugin, set StartPanelMode to 0.
//    log ("| jmid_PluginInstance_getStartPanelMode=", (int)jmid_PluginInstance_getStartPanelMode);
    if (jmid_PluginInstance_getStartPanelMode== NULL) return; // TODO
//    log ("| CallObjectMethod getStartPanelMode");
    openPluginInfo->StartPanelMode = env->CallIntMethod (jobj_PluginInstance, jmid_PluginInstance_getStartPanelMode);

    // ...
}


/**
 * The MakeDirectory function is called to create a new directory in the file system emulated by the plugin.
 * @return If the function succeeds, the return value must be 1.
 *         If the function fails, 0 should be returned.
 *         If the function was interrupted by the user, it should return -1.
 */
intptr_t WINAPI MakeDirectoryW(
    struct MakeDirectoryInfo *Info)
{
    const PluginInstanceData* data = reinterpret_cast<PluginInstanceData*> (Info->hPanel);
    jobject jobj_PluginInstance = data->instance;

    const jmethodID jmid_AbstractPluginInstance_makeDirectory = env->GetMethodID (
        jcls_AbstractPluginInstance, "makeDirectory", "(Ljava/lang/String;I)I");    
    if (jmid_AbstractPluginInstance_makeDirectory == NULL) return 0; // TODO panic better

    const jstring jstr_name = env->NewString ((const jchar*)Info->Name, _tcslen(Info->Name)); // TODO: think of GC, etc...
    const jint result = env->CallIntMethod (jobj_PluginInstance, jmid_AbstractPluginInstance_makeDirectory, jstr_name, (jint)Info->OpMode);

    return result;
}


/**
 * DeleteFiles
 */
intptr_t WINAPI DeleteFilesW(
    const struct DeleteFilesInfo *Info)
{
    PluginInstanceData* data = reinterpret_cast<PluginInstanceData*> (Info->hPanel);
    jobject jobj_PluginInstance = data->instance;

    // assume that itemsNumber>0, items!=NULL
    const jmethodID jmid_AbstractPluginInstance_deleteFile = env->GetMethodID(
        jcls_AbstractPluginInstance, "deleteFile", "(Ljava/lang/String;I)I");
    if (jmid_AbstractPluginInstance_deleteFile == NULL) return 0; // TODO panic better

    const jint jint_opMode = Info->OpMode;

    for (int i = 0; i < Info->ItemsNumber; i++) {
        const jstring jstr_cFileName = env->NewString ((const jchar*)Info->PanelItem[i].FileName, _tcslen(Info->PanelItem[i].FileName)); // TODO: think of GC, etc...
        const jint result = env->CallIntMethod (jobj_PluginInstance, jmid_AbstractPluginInstance_deleteFile, jstr_cFileName, jint_opMode);
        if (result != 1) return result; // TODO: not all files deleted?
    }
    return 1;
}


/**
 * ProcessPanelInputW
 */
intptr_t WINAPI ProcessPanelInputW(
    const struct ProcessPanelInputInfo *Info)
{
    PluginInstanceData* data = reinterpret_cast<PluginInstanceData*> (Info->hPanel);
    jobject jobj_PluginInstance = data->instance;

    jmethodID jmid_AbstractPluginInstance_processKey = env->GetMethodID (
        jcls_AbstractPluginInstance, "processKey", "(II)I");
    if (jmid_AbstractPluginInstance_processKey== 0) {
        log (TEXT("jmid_AbstractPluginInstance_processKey := 0"));
        return 0;
    }

    jmethodID jmid_AbstractPluginInstance_processEvent = env->GetMethodID (
        jcls_AbstractPluginInstance, "processEvent", "(IIII)I");
    if (jmid_AbstractPluginInstance_processEvent== 0) {
        log (TEXT("jmid_AbstractPluginInstance_processEvent:= 0"));
        return 0;
    }

    const WORD EventType = Info->Rec.EventType;
    if (EventType != KEY_EVENT) return 0;
    if (Info->Rec.Event.KeyEvent.bKeyDown != TRUE) return 0;

//    const jint result = env->CallIntMethod (jobj_PluginInstance, jmid_AbstractPluginInstance_processKey, (jint)Info->Rec.Event.KeyEvent.wVirtualKeyCode, (jint)Info->Rec.Event.KeyEvent.dwControlKeyState);
    const jint result = env->CallIntMethod (jobj_PluginInstance, jmid_AbstractPluginInstance_processEvent, EventType,
        Info->Rec.Event.KeyEvent.bKeyDown,
        (jint)Info->Rec.Event.KeyEvent.wVirtualKeyCode, (jint)Info->Rec.Event.KeyEvent.dwControlKeyState);
    return result;
}


// =============================================================================================
// Methods called from Java side
// =============================================================================================

extern "C" {

JNIEXPORT jint JNICALL Java_org_farmanager_api_AbstractPlugin_saveScreen
  (JNIEnv *env, jclass, jint x1, jint y1, jint x2, jint y2)
{
    return (jint)Info.SaveScreen(x1, y1, x2, y2);
}


JNIEXPORT void JNICALL Java_org_farmanager_api_AbstractPlugin_restoreScreen
  (JNIEnv *, jclass, jint hScreen)
{
    Info.RestoreScreen((HANDLE)hScreen);
}


JNIEXPORT jint JNICALL Java_org_farmanager_api_AbstractPlugin_message
  (JNIEnv* env, jclass, jint flags, jstring jstr_HelpTopic, jobjectArray jstr_Items, jint buttonsNumber)
{
    const wchar_t* helpTopic = jstr_HelpTopic == NULL ? NULL : (const wchar_t*) env->GetStringChars (jstr_HelpTopic, 0);
    const wchar_t* *items;
    if (jstr_Items != NULL) {
        const int l = env->GetArrayLength(jstr_Items);
        items = new const wchar_t*[l];
            
        for (int j = 0; j < l; j++) {
            const jstring jstr_item = (jstring) env->GetObjectArrayElement(jstr_Items, j);
            items[j] = jstr_item == NULL ? NULL : (const _TCHAR*)env->GetStringChars (jstr_item, 0);
            log(items[j]);
        }
    }
    else items = nullptr;
    
/*
    const intptr_t result = Info.Message(info.ModuleNumber,
        FMSG_ALLINONE|flags,
        "HelpTopic",
        (const char * const *)items,
        2, // ignored anyway
        buttonsNumber);
*/
    if (helpTopic != NULL) env->ReleaseStringChars (jstr_HelpTopic, (const jchar*)helpTopic);
    if (jstr_Items != NULL) {
        const int l = env->GetArrayLength(jstr_Items);
        for (int j = 0; j < l; j++) {
            if (items[j] != nullptr) {
                const jstring jstr_item = (jstring) env->GetObjectArrayElement(jstr_Items, j);
                env->ReleaseStringChars (jstr_item, (const jchar*)items[j]);
            }
        }
        delete[] items;
    }
    return 0;
}


JNIEXPORT jstring JNICALL Java_org_farmanager_api_AbstractPlugin_getCommandLine
  (JNIEnv *, jclass)
{    
    const size_t size = Info.PanelControl(PANEL_ACTIVE, FCTL_GETCMDLINE, 0, NULL);
    void *buffer = malloc(size);
    Info.PanelControl(PANEL_ACTIVE, FCTL_GETCMDLINE, size, (void*)buffer);
    return env->NewString ((const jchar*)buffer, size);
}


// FCTL_GETPANELINFO --> CurrentItem
JNIEXPORT jint JNICALL Java_org_farmanager_api_AbstractPlugin_getCurrentItem(JNIEnv *, jclass)
{
    struct PanelInfo panelInfo;
    if (Info.PanelControl(PANEL_ACTIVE, FCTL_GETPANELINFO, 0, &panelInfo) == FALSE) return -1;

    return (jint)panelInfo.CurrentItem;
}

JNIEXPORT jstring JNICALL Java_org_farmanager_api_AbstractPlugin_getAnotherPanelDirectory(JNIEnv *, jclass)
{
    size_t Size = Info.PanelControl(PANEL_ACTIVE, FCTL_GETPANELDIRECTORY, 0, nullptr);
    FarPanelDirectory* dir = static_cast<FarPanelDirectory*>(malloc(Size));
    dir->StructSize = sizeof(FarPanelDirectory);
    Info.PanelControl(PANEL_PASSIVE, FCTL_GETPANELDIRECTORY, Size, dir);
    jstring result = env->NewString((const jchar*)dir->Name, _tcslen(dir->Name));
    free(dir);
    return result;
}


JNIEXPORT void JNICALL Java_org_farmanager_api_AbstractPlugin_updatePanel
  (JNIEnv *, jclass)
{
    Info.PanelControl(PANEL_ACTIVE, FCTL_UPDATEPANEL, 1, NULL);
}

JNIEXPORT void JNICALL Java_org_farmanager_api_AbstractPlugin_redrawPanel
  (JNIEnv *, jclass)
{
    Info.PanelControl(PANEL_ACTIVE, FCTL_REDRAWPANEL, NULL, NULL);
}

JNIEXPORT void JNICALL Java_org_farmanager_api_AbstractPlugin_closePlugin
  (JNIEnv *, jclass)
{
    Info.PanelControl(PANEL_ACTIVE, FCTL_CLOSEPANEL, NULL, NULL);
}


// ---------------------------------------------------------------------
// helper

struct InitDialogItem
{
  int Type;
  int X1;
  int Y1;
  int X2;
  int Y2;
//  int Focus;
  int Selected;
  unsigned int Flags;
//  int DefaultButton;
  const wchar_t * const Data;
};


void InitDialogItems(
       const struct InitDialogItem *Init,
       struct FarDialogItem *Item,
       int ItemsNumber
)
{
  int I;
  const struct InitDialogItem *PInit=Init;
  struct FarDialogItem *PItem=Item;
  for (I=0; I < ItemsNumber; I++,PItem++,PInit++)
  {
    PItem->Type=(FARDIALOGITEMTYPES)PInit->Type;
    PItem->X1=PInit->X1;
    PItem->Y1=PInit->Y1;
    PItem->X2=PInit->X2;
    PItem->Y2=PInit->Y2;
//    PItem->Focus=PInit->Focus;
    PItem->Selected=PInit->Selected;

    PItem->History=nullptr;
    PItem->Mask=nullptr;

    PItem->Flags=(enum FARDIALOGITEMFLAGS)PInit->Flags;

//    PItem->DefaultButton=PInit->DefaultButton;

    if ((unsigned int)PInit->Data < 2000)
      PItem->Data = GetMsg((unsigned int)PInit->Data);
    else
      PItem->Data = PInit->Data;
  }
}


INT_PTR WINAPI ShowDialogProc(HANDLE hDlg, intptr_t Msg, intptr_t Param1, void *Param2)
{
	return Info.DefDlgProc(hDlg, Msg, Param1, Param2);
}


static bool ShowDialog1(bool bPluginPanels, bool bSelectionPresent)
{                           
	struct FarDialogItem DialogItems[] =
        { DI_BUTTON,  0, 19,  0,  0,
          0 , nullptr, nullptr, DIF_NONE, L"OK", 0, 0 };

	HANDLE hDlg = Info.DialogInit(&MainGuid, &DialogGuid, -1, -1, 66, 22, L"Contents",
	                              DialogItems, ARRAYSIZE(DialogItems), 0, 0,
	                              ShowDialogProc, (void *)0);

	if (hDlg == INVALID_HANDLE_VALUE)
		return false;

	intptr_t ExitCode = Info.DialogRun(hDlg);

	Info.DialogFree(hDlg);
	return false;
}


JNIEXPORT jint JNICALL Java_org_farmanager_api_AbstractPlugin_dialog
  (JNIEnv *env, jclass, jint x1, jint y1, jint x2, jint y2, jstring helpTopic, jobjectArray initDialogItems)
{
    if (initDialogItems == 0) return -1;  // TODO: this is an exceptional case, handle (or handle in java?)
    int length = env->GetArrayLength (initDialogItems);

    if (length > 0)
    {
        jclass jcls_InitDialogItem = env->FindClass ("org/farmanager/api/InitDialogItem");
        //log ("| jcls_InitDialogItem=", (int)jcls_InitDialogItem);

        jfieldID fid_type          = env->GetFieldID (jcls_InitDialogItem, "type", "I");
        jfieldID fid_x1            = env->GetFieldID (jcls_InitDialogItem, "x1", "I");
        jfieldID fid_y1            = env->GetFieldID (jcls_InitDialogItem, "y1", "I");
        jfieldID fid_x2            = env->GetFieldID (jcls_InitDialogItem, "x2", "I");
        jfieldID fid_y2            = env->GetFieldID (jcls_InitDialogItem, "y2", "I");
        jfieldID fid_selected      = env->GetFieldID (jcls_InitDialogItem, "selected", "I");
        jfieldID fid_flags         = env->GetFieldID (jcls_InitDialogItem, "flags", "I");
        jfieldID fid_data          = env->GetFieldID (jcls_InitDialogItem, "data", "Ljava/lang/String;");
        jfieldID fid_param          = env->GetFieldID (jcls_InitDialogItem, "param", "Ljava/lang/Object;");

        struct FarDialogItem* initItems = new FarDialogItem[length];
        for (int i=0; i < length; i++) {
        
            const jobject element = env->GetObjectArrayElement (initDialogItems, i);
            //log ("element: ", (int)element);

            // InitDialogItem.Type
            // -----------------------------------------------------------------------------------     
            const jint j_type = env->GetIntField (element, fid_type);
            initItems[i].Type = (FARDIALOGITEMTYPES)j_type;
            //log ("type: ", (int)j_type);

            // InitDialogItem.X1
            // -----------------------------------------------------------------------------------     
            jint j_x1 = env->GetIntField (element, fid_x1);
            initItems[i].X1 = j_x1;

            // InitDialogItem.Y1
            // -----------------------------------------------------------------------------------     
            jint j_y1 = env->GetIntField (element, fid_y1);
            initItems[i].Y1 = j_y1;

            // InitDialogItem.X2
            // -----------------------------------------------------------------------------------     
            jint j_x2 = env->GetIntField (element, fid_x2);
            initItems[i].X2 = j_x2;

            // InitDialogItem.Y2
            // -----------------------------------------------------------------------------------     
            jint j_y2 = env->GetIntField (element, fid_y2);
            initItems[i].Y2 = j_y2;

            // InitDialogItem.Flags
            // -----------------------------------------------------------------------------------     
            jint j_flags = env->GetIntField (element, fid_flags);
            initItems[i].Flags = j_flags;

            initItems[i].History = nullptr;
            initItems[i].Mask = nullptr;
            initItems[i].MaxLength = 0;
            initItems[i].UserData = 0;
            initItems[i].Reserved[0] = 0;
            initItems[i].Reserved[1] = 0;

            // InitDialogItem.Data
            // -----------------------------------------------------------------------------------
            jstring j_data = (jstring) env->GetObjectField (element, fid_data);
            const wchar_t* data = (const wchar_t*)env->GetStringChars (j_data, 0);  // TODO: garbage collection???
            //log ("DATA=", data);
            initItems[i].Data = data;

            // InitDialogItem.Param
            // -----------------------------------------------------------------------------------
            if (j_type == DI_COMBOBOX || j_type == DI_LISTBOX)
            {
                jobject j_param = env->GetObjectField (element, fid_param);
                if (j_param != NULL)
                {
                    FarList* listItems = new FarList;
                    //log ("Allocated FarList", (int)listItems);
                    
                    // j_param has type FarListItem[]
                    jobjectArray j_listItems = (jobjectArray)j_param;
                    int listItemsNumber = env->GetArrayLength (j_listItems);
                     
                    listItems->ItemsNumber = listItemsNumber;
                    listItems->Items = new FarListItem[listItemsNumber];
                    //log ("Allocated items", (int)listItems->Items);


                    memset(listItems->Items, 0, sizeof(FarListItem)*listItemsNumber);
                    jclass jcls_FLI = env->FindClass ("org/farmanager/api/jni/FarListItem");
                    jfieldID fidFLIText   = env->GetFieldID (jcls_FLI,  "text", "Ljava/lang/String;");
                    jfieldID fidFLIFlags = env->GetFieldID (jcls_FLI,  "flags", "I");

                    for (int itemNumber = 0; itemNumber < listItemsNumber; itemNumber++)
                    {
                       jobject j_fli = env->GetObjectArrayElement (j_listItems, itemNumber);
                       //log ("far list item: ", (int)j_fli);

                       // FarListItem.Text
                       // -----------------------------------------------------------------------------------     
                       jstring j_fli_text = (jstring) env->GetObjectField (j_fli, fidFLIText);
                       const wchar_t* fli_text = (const wchar_t*)env->GetStringUTFChars (j_fli_text, 0);
                       //strcpy (listItems->Items[itemNumber].Text, fli_text);
                       //env->ReleaseStringUTFChars (j_fli_text, fli_text);
                       listItems->Items[itemNumber].Text = fli_text;

                       // FarListItem.Flags
                       // -----------------------------------------------------------------------------------     
                       jint j_fli_flags = env->GetIntField (j_fli, fidFLIFlags);
                       listItems->Items[itemNumber].Flags = j_fli_flags;

                       // FarListItem.Reserved
                       // -----------------------------------------------------------------------------------     
                       listItems->Items[itemNumber].Reserved[0] = 0;
                       listItems->Items[itemNumber].Reserved[1] = 0;
                    }
                    initItems[i].ListItems = listItems;
                }
            }
            else /* assume checkbox, radiobutton.. */
            {
                // InitDialogItem.Selected
                // -----------------------------------------------------------------------------------     
                jint j_selected = env->GetIntField (element, fid_selected);
                initItems[i].Selected = j_selected;
            }

        }

	struct FarDialogItem DialogItems1[] =
        { DI_BUTTON,  0, 19,  0,  0,
          0 , nullptr, nullptr, DIF_NONE, L"OK", 0, 0 };

        HANDLE hDlg = Info.DialogInit(&MainGuid, &DialogGuid, x1, y1, x2, y2, L"Contents",
	                              initItems, length, 0, 0,
	                              ShowDialogProc, (void *)0);

	if (hDlg == INVALID_HANDLE_VALUE) {
            log(L"DialogInit failed");
	    return -1;
        }

	intptr_t ExitCode = Info.DialogRun(hDlg);
        //log("Propagate changes to java and release temp memory");
        for (int j=0; j < length; j++)
        {
            const jobject element = env->GetObjectArrayElement (initDialogItems, j);
//            log ("element: ", (int)element);
            const jint j_type = env->GetIntField (element, fid_type);
//            log ("type: ", (int)j_type);

            if (j_type == DI_COMBOBOX || j_type == DI_LISTBOX) {
//                log ("Deleting items", (int)DialogItems[j].ListItems->Items);
//                delete DialogItems[j].ListItems->Items;
// TODO: could not delete, it crashes! REWRITE!
//                log ("Combo.Selected=", (int)DialogItems[j].Selected);
//                log ("Combo.ListItems=", (int)DialogItems[j].ListItems);
//                delete DialogItems[j].ListItems;
//                log ("List poz:", DialogItems[j].ListPos);
                // As ListPos coincides with Selected, ListPos will be returned in "selected"
            }
            else {
                const wchar_t *data = ((const wchar_t *)Info.SendDlgMessage(hDlg,DM_GETCONSTTEXTPTR, j, 0));
                env->SetObjectField(element, fid_data, env->NewString((const jchar*) data, _tcslen(data)));
            }
//            log ("Data=", DialogItems[j].Data);
//            env->SetObjectField (element, fid_data, env->NewStringUTF(DialogItems[j].Data));
            env->SetIntField (element, fid_flags,(jint)initItems[j].Flags); // union with ListPos
        }


	Info.DialogFree(hDlg);
	return ExitCode;
    }
    return -1;
}

}
