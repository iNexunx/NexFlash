#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <string.h>
#include <stddef.h>

#include <string>
#include <vector>
#include <sstream>

namespace usb_bridge {

static int connect_bridge() {
    int fd = socket(AF_LOCAL, SOCK_STREAM, 0);
    if (fd < 0) return -1;
    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_LOCAL;
    // abstract namespace: name[0]=0, rest is "adbify_usb_bridge"
    const char *name = "adbify_usb_bridge";
    size_t namelen = strlen(name);
    memcpy(addr.sun_path + 1, name, namelen);
    socklen_t len = (socklen_t)(offsetof(struct sockaddr_un, sun_path) + 1 + namelen);
    if (connect(fd, (struct sockaddr *)&addr, len) < 0) {
        close(fd);
        return -1;
    }
    return fd;
}

static bool write_u16(int fd, unsigned short v) {
    unsigned char b[2] = {(unsigned char)((v >> 8) & 0xff), (unsigned char)(v & 0xff)};
    return write(fd, b, 2) == 2;
}

static bool write_utf(int fd, const std::string &s) {
    if (s.size() > 0xFFFF) return false;
    unsigned short len = (unsigned short)s.size();
    if (!write_u16(fd, len)) return false;
    if (len == 0) return true;
    const char *p = s.data();
    size_t left = s.size();
    while (left > 0) {
        ssize_t w = write(fd, p, left);
        if (w <= 0) return false;
        p += w;
        left -= (size_t)w;
    }
    return true;
}

static bool read_u16(int fd, unsigned short &v) {
    unsigned char b[2];
    ssize_t r = read(fd, b, 2);
    if (r != 2) return false;
    v = (unsigned short)((b[0] << 8) | b[1]);
    return true;
}

static bool read_utf(int fd, std::string &out) {
    unsigned short len = 0;
    if (!read_u16(fd, len)) return false;
    out.assign(len, '\0');
    size_t got = 0;
    while (got < len) {
        ssize_t r = read(fd, &out[got], len - got);
        if (r <= 0) return false;
        got += (size_t)r;
    }
    return true;
}

__attribute__((visibility("default"))) std::vector<std::string> get_devices() {
    std::vector<std::string> result;
    int fd = connect_bridge();
    if (fd < 0) return result;
    if (!write_u16(fd, 0)) { close(fd); return result; }
    // server writes UTF if not empty, then closes or EOF
    std::string names;
    if (read_utf(fd, names) && !names.empty()) {
        size_t start = 0;
        while (start <= names.size()) {
            size_t pos = names.find(':', start);
            std::string item = names.substr(start, pos == std::string::npos ? std::string::npos : pos - start);
            if (!item.empty()) result.push_back(item);
            if (pos == std::string::npos) break;
            start = pos + 1;
        }
    }
    close(fd);
    return result;
}

__attribute__((visibility("default"))) int get_fd(const std::string &name) {
    int fd = connect_bridge();
    if (fd < 0) return -1;
    if (!write_u16(fd, 1) || !write_utf(fd, name)) { close(fd); return -1; }
    char buf[1];
    struct iovec iov = {buf, 1};
    char cmsgbuf[CMSG_SPACE(sizeof(int))];
    memset(cmsgbuf, 0, sizeof(cmsgbuf));
    struct msghdr msg;
    memset(&msg, 0, sizeof(msg));
    msg.msg_iov = &iov;
    msg.msg_iovlen = 1;
    msg.msg_control = cmsgbuf;
    msg.msg_controllen = sizeof(cmsgbuf);
    ssize_t r = recvmsg(fd, &msg, 0);
    if (r <= 0) { close(fd); return -1; }
    int outfd = -1;
    for (struct cmsghdr *cmsg = CMSG_FIRSTHDR(&msg); cmsg; cmsg = CMSG_NXTHDR(&msg, cmsg)) {
        if (cmsg->cmsg_level == SOL_SOCKET && cmsg->cmsg_type == SCM_RIGHTS) {
            size_t n = (cmsg->cmsg_len - CMSG_LEN(0)) / sizeof(int);
            if (n > 0) memcpy(&outfd, CMSG_DATA(cmsg), sizeof(int));
        }
    }
    close(fd);
    return outfd;
}

__attribute__((visibility("default"))) std::string get_serial(const std::string &name) {
    std::string serial;
    int fd = connect_bridge();
    if (fd < 0) return serial;
    if (!write_u16(fd, 2) || !write_utf(fd, name)) { close(fd); return serial; }
    read_utf(fd, serial);
    close(fd);
    return serial;
}

} // namespace usb_bridge
