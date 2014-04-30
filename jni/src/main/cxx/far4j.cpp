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
 Функция GetMsg возвращает строку сообщения из языкового файла.
 А это надстройка над Info.GetMsg для сокращения кода :-)
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
Функция SetStartupInfoW вызывается один раз, перед всеми
другими функциями. Она передается плагину информацию,
необходимую для дальнейшей работы.
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

    log (TEXT("| OK"));
}

/*
Функция GetPluginInfoW вызывается для получения информации о плагине
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
    //PanelMode panelModes[10];
    //pchar* panelModeColumnTitles[10];
    //KeyBarTitles keyBarTitles;
    //InfoPanelLine* infoPanelLines;
};


/*
  Функция OpenPluginW вызывается при создании новой копии плагина.
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
