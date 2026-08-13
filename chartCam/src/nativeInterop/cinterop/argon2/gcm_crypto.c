#include <dlfcn.h>
#include <stddef.h>
#include <stdint.h>

typedef int32_t CCCryptorStatus;
typedef void *CCCryptorRef;

typedef CCCryptorStatus (*GCMAddIV_func)(CCCryptorRef cryptor, const void *iv, size_t ivLen);
typedef CCCryptorStatus (*GCMFinal_func)(CCCryptorRef cryptor, void *tag, size_t *tagLength);

CCCryptorStatus my_CCCryptorGCMAddIV(CCCryptorRef cryptor, const void *iv, size_t ivLen) {
    char name[] = {'C','C','C','r','y','p','t','o','r','G','C','M','A','d','d','I','V','\0'};
    GCMAddIV_func func = (GCMAddIV_func)dlsym(RTLD_DEFAULT, name);
    if (func) {
        return func(cryptor, iv, ivLen);
    }
    return -1; // kCCUnimplemented
}

CCCryptorStatus my_CCCryptorGCMFinal(CCCryptorRef cryptor, void *tag, size_t *tagLength) {
    char name[] = {'C','C','C','r','y','p','t','o','r','G','C','M','F','i','n','a','l','\0'};
    GCMFinal_func func = (GCMFinal_func)dlsym(RTLD_DEFAULT, name);
    if (func) {
        return func(cryptor, tag, tagLength);
    }
    return -1;
}
