#include <jni.h>
#include <string>
#include <fstream>
#include <sstream>
#include <unistd.h>
#include <dirent.h>
#include <vector>
#include <set>
#include <map>



extern "C" {

// Helper to read a single-line file
std::string readFile(const std::string& path) {
    std::ifstream file(path);
    std::string content;
    std::getline(file, content);
    return content;
}

// // Thermal info collector
// std::string collectThermalInfo() {
//     std::string result;
//     const std::string basePath = "/sys/class/thermal/";
//     DIR* dir = opendir(basePath.c_str());

//     if (!dir) {
//         return "Unable to access /sys/class/thermal";
//     }

//     struct dirent* entry;
//     while ((entry = readdir(dir)) != nullptr) {
//         std::string name(entry->d_name);
//         if (name.find("thermal_zone") == 0) {
//             std::string tempPath = basePath + name + "/temp";
//             std::string typePath = basePath + name + "/type";

//             std::ifstream tempFile(tempPath);
//             std::ifstream typeFile(typePath);

//             std::string tempStr, typeStr;
//             if (tempFile && typeFile && std::getline(tempFile, tempStr) && std::getline(typeFile, typeStr)) {
//                 try {
//                     int tempVal = std::stoi(tempStr);
//                     float tempC = (tempVal > 1000) ? tempVal / 1000.0f : tempVal * 1.0f;
//                     result += typeStr + ": " + std::to_string(tempC) + " °C\n";
//                 } catch (...) {
//                     // Parsing error; skip
//                 }
//             }
//         }
//     }

//     closedir(dir);
//     if (result.empty()) {
//         result = "No accessible thermal data.";
//     }

//     return result;
// }

// // JNI method for Kotlin
// JNIEXPORT jstring JNICALL
// Java_com_tomertech_system_1status_1plugin_SystemStatusPlugin_collectThermalInfo(
//         JNIEnv *env, jobject /* this */) {
//     std::string info = collectThermalInfo();
//     return env->NewStringUTF(info.c_str());
// }



// CPU Model (e.g., "s5e8535")
const char* get_cpu_model() {
    static std::string model;
    model.clear();
    std::ifstream cpuinfo("/proc/cpuinfo");
    std::string line;
    while (getline(cpuinfo, line)) {
        if (line.find("Hardware") != std::string::npos || line.find("model name") != std::string::npos) {
            model = line.substr(line.find(":") + 2);
            break;
        }
    }
    return model.c_str();
}

// CPU Cores
int get_core_count() {
    return sysconf(_SC_NPROCESSORS_ONLN);
}

// CPU Architecture Version
const char* get_cpu_architecture() {
#if defined(__aarch64__)
    return "ARMv8-A (64-bit)";
#elif defined(__arm__)
    return "ARMv7-A (32-bit)";
#elif defined(__x86_64__)
    return "x86_64";
#elif defined(__i386__)
    return "x86";
#else
    return "Unknown";
#endif
}

// CPU Revision
const char* get_cpu_revision() {
    static std::string revision;
    revision.clear();
    std::ifstream cpuinfo("/proc/cpuinfo");
    std::string line;
    while (getline(cpuinfo, line)) {
        if (line.find("Revision") != std::string::npos) {
            revision = line.substr(line.find(":") + 2);
            break;
        }
    }
    return revision.c_str();
}

// Per-core live frequencies
const char* get_cpu_frequencies() {
    static std::string result;
    result.clear();

    int cores = get_core_count();
    for (int i = 0; i < cores; i++) {
        std::string path = "/sys/devices/system/cpu/cpu" + std::to_string(i) + "/cpufreq/scaling_cur_freq";
        std::ifstream file(path);
        std::string freq;
        if (file && std::getline(file, freq)) {
            int mhz = std::stoi(freq) / 1000;
            result += "CPU " + std::to_string(i) + ": " + std::to_string(mhz) + " MHz\n";
        } else {
            result += "CPU " + std::to_string(i) + ": N/A\n";
        }
    }
    return result.c_str();
}

// Clock speed range (min - max)
const char* get_cpu_clock_range() {
    static std::string result;
    result.clear();
    int minFreq = INT32_MAX, maxFreq = 0;
    int cores = get_core_count();

    for (int i = 0; i < cores; ++i) {
        std::string minPath = "/sys/devices/system/cpu/cpu" + std::to_string(i) + "/cpufreq/cpuinfo_min_freq";
        std::string maxPath = "/sys/devices/system/cpu/cpu" + std::to_string(i) + "/cpufreq/cpuinfo_max_freq";

        std::ifstream minFile(minPath), maxFile(maxPath);
        std::string minStr, maxStr;

        if (minFile && std::getline(minFile, minStr)) {
            int mhz = std::stoi(minStr) / 1000;
            minFreq = std::min(minFreq, mhz);
        }
        if (maxFile && std::getline(maxFile, maxStr)) {
            int mhz = std::stoi(maxStr) / 1000;
            maxFreq = std::max(maxFreq, mhz);
        }
    }

    if (minFreq < maxFreq) {
        result = std::to_string(minFreq) + " MHz - " + std::to_string(maxFreq) + " MHz";
    } else {
        result = "Unknown";
    }

    return result.c_str();
}

// big.LITTLE Detection
const char* get_big_little_info() {
    static std::string result;
    result.clear();
    std::set<int> uniqueFreqs;
    int cores = get_core_count();

    for (int i = 0; i < cores; i++) {
        std::string path = "/sys/devices/system/cpu/cpu" + std::to_string(i) + "/cpufreq/cpuinfo_max_freq";
        std::ifstream file(path);
        std::string line;
        if (file && std::getline(file, line)) {
            int mhz = std::stoi(line) / 1000;
            uniqueFreqs.insert(mhz);
        }
    }

    if (uniqueFreqs.size() > 1) {
        result = "Yes (Multiple clusters)";
    } else if (uniqueFreqs.size() == 1) {
        result = "No (Single cluster)";
    } else {
        result = "Unknown";
    }

    return result.c_str();
}

// Cluster architecture (A78 vs A55 etc.)
const char* get_cluster_architecture() {
    static std::string result;
    result.clear();
    std::map<std::string, int> coreMap;
    std::map<std::string, int> freqMap;

    std::ifstream cpuinfo("/proc/cpuinfo");
    std::string line;
    std::string model;
    int coreIndex = -1;

    while (getline(cpuinfo, line)) {
        if (line.find("processor") != std::string::npos) {
            coreIndex++;
        } else if (line.find("CPU part") != std::string::npos) {
            model = line.substr(line.find(":") + 2);
            coreMap[model]++;
        }
    }

    for (const auto& pair : coreMap) {
        std::string coreLabel = "ARM Cortex-";
        if (pair.first == "0xd05") coreLabel += "A55";
        else if (pair.first == "0xd41") coreLabel += "A78";
        else coreLabel += "Unknown";

        coreLabel += " x" + std::to_string(pair.second);
        result += coreLabel + "\n";
    }

    return result.c_str();
}

// Scaling Governor
const char* get_scaling_governor() {
    static std::string result;
    result.clear();
    int cores = get_core_count();

    for (int i = 0; i < cores; i++) {
        std::string path = "/sys/devices/system/cpu/cpu" + std::to_string(i) + "/cpufreq/scaling_governor";
        std::ifstream file(path);
        std::string line;
        if (file && std::getline(file, line)) {
            result += "CPU " + std::to_string(i) + ": " + line + "\n";
        }
    }
    return result.c_str();
}

// GPU Vendor/Renderer (Adreno or Mali)
const char* get_gpu_vendor() {
    static std::string vendor;

    vendor = readFile("/sys/class/kgsl/kgsl-3d0/gpu_model");
    if (vendor.empty()) vendor = readFile("/sys/class/misc/mali0/device/gpuinfo");
    if (vendor.empty()) vendor = "Unknown";
    return vendor.c_str();
}

const char* get_gpu_renderer() {
    return get_gpu_vendor();  // Same for Mali
}

// GPU Load
const char* get_gpu_load() {
    static std::string load;
    load = readFile("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage");

    if (load.empty()) {
        load = readFile("/sys/class/misc/mali0/device/utilization");
    }

    if (!load.empty()) {
        load += " %";
    } else {
        load = "0 %";
    }

    return load.c_str();
}

}
