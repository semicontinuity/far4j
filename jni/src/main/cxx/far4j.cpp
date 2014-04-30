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

static jmethodID jmid_Plugin_getFlags;
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

// sorry, hardcoded for a while
    if (!LoadJavaVM(TEXT("C:\\Program Files\\Java\\jdk1.7.0_45\\jre\\bin\\server\\jvm.dll"), &ifn)) {
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
    jstring jstr_ModuleName = env->NewString ((const jchar*) psi->ModuleName, _tcslen(psi->ModuleName));
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


    log (TEXT("| OK"));
}

/*
������� GetPluginInfoW ���������� ��� ��������� ���������� � �������
*/
void WINAPI GetPluginInfoW(struct PluginInfo *Info)
{
	Info->StructSize=sizeof(*Info);
	Info->Flags=PF_EDITOR;
	static const wchar_t *PluginMenuStrings[1];
	PluginMenuStrings[0]=GetMsg(MTitle);
	Info->PluginMenu.Guids=&MenuGuid;
	Info->PluginMenu.Strings=PluginMenuStrings;
	Info->PluginMenu.Count=ARRAYSIZE(PluginMenuStrings);
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
    log (TEXT("> OpenPlugin"));
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

    log (TEXT("< OpenPlugin"));
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


void WINAPI GetOpenPanelInfoW(struct OpenPanelInfo *openPluginInfo) {
    PluginInstanceData* data = reinterpret_cast<PluginInstanceData*> (openPluginInfo->hPanel);
    jobject jobj_PluginInstance = data->instance;

    // StructSize
    openPluginInfo->StructSize = sizeof(OpenPanelInfo);

    // Flags (NB now 64 bit!)
    jmethodID jmid_AbstractPluginInstance_getFlags = env->GetMethodID (
        jcls_AbstractPluginInstance, "getFlags", "()I");
    if (jmid_AbstractPluginInstance_getFlags == 0) {
        log(TEXT("jmid_AbstractPluginInstance_getFlags := 0"));
        return ;
    }
    jint Flags   = env->CallIntMethod (jobj_PluginInstance, jmid_AbstractPluginInstance_getFlags);
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
    jmethodID jmid_AbstractPluginInstance_getPanelModes= env->GetMethodID (
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
        jfieldID jfid_fullScreen         = env->GetFieldID (jcls_PanelMode, "fullScreen",     "I");
        jfieldID jfid_detailedStatus     = env->GetFieldID (jcls_PanelMode, "detailedStatus", "I");
        jfieldID jfid_statusColumnTypes  = env->GetFieldID (jcls_PanelMode, "statusColumnTypes",    "Ljava/lang/String;");
        jfieldID jfid_statusColumnWidths = env->GetFieldID (jcls_PanelMode, "statusColumnWidths",   "Ljava/lang/String;");
        for (int i=0; i < length; i++)
        {
            jobject element = env->GetObjectArrayElement (panelModeArray, i);
            // TODO: check element for NULL
            memset(&(data->panelModes[i]), 0, sizeof(PanelMode));

            jint jint_fullScreen = env->GetIntField (element, jfid_fullScreen);
            jint jint_detailedStatus = env->GetIntField (element, jfid_detailedStatus);
            data->panelModes[i].Flags = (jint_detailedStatus ? PMFLAGS_FULLSCREEN : 0) | (jint_detailedStatus ? PMFLAGS_DETAILEDSTATUS : 0);

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
