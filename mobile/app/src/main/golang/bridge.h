#ifndef CLASH_VERGE_REV_BRIDGE_H
#define CLASH_VERGE_REV_BRIDGE_H

#ifdef __cplusplus
extern "C" {
#endif

void coreInit(const char* homeDir, const char* versionName);
void reset();
void forceGc();
int startTun(int fd, int mtu);
void stopTun();
int loadConfig(const char* configPath);
long long queryTraffic();
char* getVersion();
void freeString(char* str);

#ifdef __cplusplus
}
#endif

#endif
