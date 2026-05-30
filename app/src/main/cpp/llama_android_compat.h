#pragma once

#if defined(__ANDROID__)
#include <stddef.h>
#include <sys/mman.h>

#if !defined(POSIX_MADV_RANDOM)
#define POSIX_MADV_RANDOM MADV_RANDOM
#endif

#if !defined(POSIX_MADV_WILLNEED)
#define POSIX_MADV_WILLNEED MADV_WILLNEED
#endif

#if !defined(_POSIX_ADVISORY_INFORMATION)
static inline int posix_madvise(void *addr, size_t len, int advice) {
    return madvise(addr, len, advice);
}
#endif
#endif
