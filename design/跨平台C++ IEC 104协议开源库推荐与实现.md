# 跨平台C++ IEC 104协议开源库推荐与实现

## 1. IEC 104协议开源库推荐

### 1.1 推荐库：lib60870-C

#### 1.1.1 库简介
lib60870-C是一个流行的开源库，完整实现了IEC 60870-5系列协议，包括IEC 60870-5-104协议。该库具有良好的跨平台兼容性，支持Windows、Linux、macOS等主流操作系统。

#### 1.1.2 主要特点
- **完整的IEC 104协议实现**：支持客户端和服务端模式
- **跨平台兼容性**：基于标准C实现，支持多种操作系统
- **活跃的社区维护**：定期更新，社区反馈及时
- **完善的文档**：提供详细的API文档和示例代码
- **高性能设计**：适用于高并发场景
- **易用的API**：简洁明了的接口设计

#### 1.1.3 许可证
MIT许可证，允许商业使用

#### 1.1.4 安装方式
- **Linux**：通过包管理器安装或源码编译
- **Windows**：使用Visual Studio编译或预编译库
- **CMake支持**：提供CMake构建脚本，便于跨平台编译

## 2. Iec104Aqui类设计与实现

### 2.1 头文件：AquiIec104.h

```cpp
#ifndef AQUI_IEC104_H
#define AQUI_IEC104_H

#include <string>
#include <functional>
#include <memory>
#include <lib60870/cs104_client.h>
#include <lib60870/cs104_server.h>
#include <lib60870/hal_time.h>

namespace Aqui {

/**
 * @brief IEC 104协议操作类，支持客户端和服务端模式
 */
class Iec104Aqui {
public:
    /**
     * @brief 连接状态枚举
     */
    enum class ConnectionState {
        DISCONNECTED,  ///< 断开连接
        CONNECTING,    ///< 正在连接
        CONNECTED,     ///< 已连接
        DISCONNECTING, ///< 正在断开连接
        ERROR          ///< 错误状态
    };

    /**
     * @brief 错误类型枚举
     */
    enum class ErrorType {
        NONE,           ///< 无错误
        CONNECTION_FAILED,  ///< 连接失败
        SEND_FAILED,    ///< 发送失败
        RECEIVE_FAILED, ///< 接收失败
        TIMEOUT,        ///< 超时
        INVALID_PARAM,  ///< 参数无效
        INTERNAL_ERROR  ///< 内部错误
    };

    /**
     * @brief 连接状态变化回调函数类型
     */
    using ConnectionStateCallback = std::function<void(ConnectionState state, const std::string& message)>;

    /**
     * @brief 接收数据回调函数类型
     */
    using DataReceivedCallback = std::function<void(const CS101_ASDU* asdu)>;

    /**
     * @brief 构造函数
     */
    Iec104Aqui();

    /**
     * @brief 析构函数
     */
    ~Iec104Aqui();

    /**
     * @brief 配置连接参数
     * @param ip IP地址
     * @param port 端口号
     * @param timeout 超时时间（毫秒）
     * @param isServer 是否为服务端模式
     */
    void configure(const std::string& ip, int port, int timeout, bool isServer = false);

    /**
     * @brief 初始化并启动连接
     * @return true 成功，false 失败
     */
    bool initialize();

    /**
     * @brief 停止连接
     */
    void stop();

    /**
     * @brief 发送单个遥测数据
     * @param ioAddress IO地址
     * @param value 遥测值
     * @param quality 质量码
     * @return true 成功，false 失败
     */
    bool sendMeasuredValue(int ioAddress, float value, int quality = 0);

    /**
     * @brief 发送单个遥信数据
     * @param ioAddress IO地址
     * @param value 遥信值（true/false）
     * @param quality 质量码
     * @return true 成功，false 失败
     */
    bool sendBinaryInput(int ioAddress, bool value, int quality = 0);

    /**
     * @brief 发送控制命令
     * @param ioAddress IO地址
     * @param command 命令值
     * @return true 成功，false 失败
     */
    bool sendControlCommand(int ioAddress, bool command);

    /**
     * @brief 设置连接状态变化回调
     * @param callback 回调函数
     */
    void setConnectionStateCallback(ConnectionStateCallback callback);

    /**
     * @brief 设置数据接收回调
     * @param callback 回调函数
     */
    void setDataReceivedCallback(DataReceivedCallback callback);

    /**
     * @brief 获取当前连接状态
     * @return 连接状态
     */
    ConnectionState getConnectionState() const;

    /**
     * @brief 获取最后一个错误
     * @return 错误类型
     */
    ErrorType getLastError() const;

    /**
     * @brief 获取最后一个错误描述
     * @return 错误描述
     */
    std::string getLastErrorDescription() const;

private:
    /**
     * @brief 客户端模式下的连接状态变化回调
     */
    static void clientConnectionHandler(void* parameter, CS104_Client connection, CS104_ClientConnectionEvent event);

    /**
     * @brief 客户端模式下的数据接收回调
     */
    static bool clientASDUHandler(void* parameter, CS104_Client connection, int address, CS101_ASDU* asdu);

    /**
     * @brief 服务端模式下的数据接收回调
     */
    static bool serverASDUHandler(void* parameter, CS104_Server server, int address, CS101_ASDU* asdu);

    /**
     * @brief 服务端模式下的连接状态变化回调
     */
    static void serverConnectionHandler(void* parameter, CS104_Server server, CS104_Client connection, bool connected);

    /**
     * @brief 更新连接状态
     */
    void updateConnectionState(ConnectionState state, const std::string& message = "");

    /**
     * @brief 设置错误信息
     */
    void setError(ErrorType error, const std::string& description = "");

    // 连接参数
    std::string m_ip;               ///< IP地址
    int m_port;                    ///< 端口号
    int m_timeout;                 ///< 超时时间（毫秒）
    bool m_isServer;               ///< 是否为服务端模式

    // 连接对象
    CS104_Client m_client;         ///< 客户端连接对象
    CS104_Server m_server;         ///< 服务端连接对象
    CS101_AppLayerParameters m_appLayerParams; ///< 应用层参数

    // 回调函数
    ConnectionStateCallback m_connectionCallback; ///< 连接状态变化回调
    DataReceivedCallback m_dataReceivedCallback; ///< 数据接收回调

    // 状态信息
    ConnectionState m_state;       ///< 当前连接状态
    ErrorType m_lastError;         ///< 最后一个错误
    std::string m_lastErrorDesc;   ///< 最后一个错误描述
};

} // namespace Aqui

#endif // AQUI_IEC104_H
```

### 2.2 实现文件：AquiIec104.cpp

```cpp
#include "AquiIec104.h"
#include <iostream>

namespace Aqui {

Iec104Aqui::Iec104Aqui() 
    : m_port(2404), m_timeout(3000), m_isServer(false),
      m_client(nullptr), m_server(nullptr),
      m_state(ConnectionState::DISCONNECTED),
      m_lastError(ErrorType::NONE) {
    // 初始化应用层参数
    CS101_AppLayerParameters_init(&m_appLayerParams);
    m_appLayerParams.rcvASDUAddress = 1; // 接收ASDU地址
    m_appLayerParams.sndASDUAddress = 1; // 发送ASDU地址
}

Iec104Aqui::~Iec104Aqui() {
    stop();
}

void Iec104Aqui::configure(const std::string& ip, int port, int timeout, bool isServer) {
    m_ip = ip;
    m_port = port;
    m_timeout = timeout;
    m_isServer = isServer;
}

bool Iec104Aqui::initialize() {
    // 检查是否已经初始化
    if (m_state != ConnectionState::DISCONNECTED) {
        setError(ErrorType::INVALID_PARAM, "Already initialized");
        return false;
    }

    updateConnectionState(ConnectionState::CONNECTING, "Initializing connection...");

    try {
        if (m_isServer) {
            // 服务端模式
            m_server = CS104_Server_create(m_appLayerParams, 10); // 最多10个客户端连接
            
            if (!m_server) {
                setError(ErrorType::INTERNAL_ERROR, "Failed to create server");
                updateConnectionState(ConnectionState::ERROR, "Failed to create server");
                return false;
            }

            // 设置连接处理回调
            CS104_Server_setConnectionHandler(m_server, serverConnectionHandler, this);
            
            // 设置ASDU处理回调
            CS104_Server_setASDUHandler(m_server, serverASDUHandler, this);
            
            // 启动服务端
            if (!CS104_Server_start(m_server, m_port)) {
                setError(ErrorType::CONNECTION_FAILED, "Failed to start server");
                CS104_Server_destroy(m_server);
                m_server = nullptr;
                updateConnectionState(ConnectionState::ERROR, "Failed to start server");
                return false;
            }

            updateConnectionState(ConnectionState::CONNECTED, "Server started successfully");
        } else {
            // 客户端模式
            m_client = CS104_Client_create();
            
            if (!m_client) {
                setError(ErrorType::INTERNAL_ERROR, "Failed to create client");
                updateConnectionState(ConnectionState::ERROR, "Failed to create client");
                return false;
            }

            // 设置连接超时
            CS104_Client_setConnectionTimeout(m_client, m_timeout);
            CS104_Client_setASDUReceivedTimeout(m_client, m_timeout);
            CS104_Client_setIdleTimeout(m_client, m_timeout * 3);

            // 设置连接处理回调
            CS104_Client_setConnectionHandler(m_client, clientConnectionHandler, this);
            
            // 设置ASDU处理回调
            CS104_Client_setASDUHandler(m_client, clientASDUHandler, this);
            
            // 连接到服务器
            if (!CS104_Client_connect(m_client, m_ip.c_str(), m_port)) {
                setError(ErrorType::CONNECTION_FAILED, "Failed to connect to server");
                CS104_Client_destroy(m_client);
                m_client = nullptr;
                updateConnectionState(ConnectionState::ERROR, "Failed to connect to server");
                return false;
            }

            // 这里不直接设置为CONNECTED，因为连接状态会通过回调更新
        }

        return true;
    } catch (const std::exception& e) {
        setError(ErrorType::INTERNAL_ERROR, std::string("Exception: ") + e.what());
        updateConnectionState(ConnectionState::ERROR, "Exception occurred");
        return false;
    }
}

void Iec104Aqui::stop() {
    if (m_state == ConnectionState::DISCONNECTED) {
        return;
    }

    updateConnectionState(ConnectionState::DISCONNECTING, "Stopping connection...");

    try {
        if (m_server) {
            // 停止服务端
            CS104_Server_stop(m_server);
            CS104_Server_destroy(m_server);
            m_server = nullptr;
        } else if (m_client) {
            // 断开客户端连接
            CS104_Client_disconnect(m_client);
            CS104_Client_destroy(m_client);
            m_client = nullptr;
        }

        updateConnectionState(ConnectionState::DISCONNECTED, "Connection stopped");
        setError(ErrorType::NONE);
    } catch (const std::exception& e) {
        setError(ErrorType::INTERNAL_ERROR, std::string("Exception during stop: ") + e.what());
        updateConnectionState(ConnectionState::ERROR, "Error during stop");
    }
}

bool Iec104Aqui::sendMeasuredValue(int ioAddress, float value, int quality) {
    if (m_state != ConnectionState::CONNECTED) {
        setError(ErrorType::INVALID_PARAM, "Not connected");
        return false;
    }

    try {
        CS101_ASDU* asdu = CS101_ASDU_create(m_appLayerParams, false, CS101_COT_PERIODIC, 0, 0, 0, false, false);
        
        if (!asdu) {
            setError(ErrorType::INTERNAL_ERROR, "Failed to create ASDU");
            return false;
        }

        // 添加遥测数据
        if (CS101_ASDU_addInformationObject(asdu, (InformationObject*)MeasuredValueShort_create(NULL, ioAddress, value, quality))) {
            bool success = false;
            
            if (m_server) {
                // 服务端模式：发送给所有连接的客户端
                success = CS104_Server_enqueueASDU(m_server, asdu);
            } else if (m_client) {
                // 客户端模式：发送给服务器
                success = CS104_Client_sendASDU(m_client, asdu);
            }

            CS101_ASDU_destroy(asdu);
            
            if (!success) {
                setError(ErrorType::SEND_FAILED, "Failed to send ASDU");
                return false;
            }

            return true;
        } else {
            CS101_ASDU_destroy(asdu);
            setError(ErrorType::INTERNAL_ERROR, "Failed to add information object");
            return false;
        }
    } catch (const std::exception& e) {
        setError(ErrorType::INTERNAL_ERROR, std::string("Exception during send: ") + e.what());
        return false;
    }
}

bool Iec104Aqui::sendBinaryInput(int ioAddress, bool value, int quality) {
    if (m_state != ConnectionState::CONNECTED) {
        setError(ErrorType::INVALID_PARAM, "Not connected");
        return false;
    }

    try {
        CS101_ASDU* asdu = CS101_ASDU_create(m_appLayerParams, false, CS101_COT_PERIODIC, 0, 0, 0, false, false);
        
        if (!asdu) {
            setError(ErrorType::INTERNAL_ERROR, "Failed to create ASDU");
            return false;
        }

        // 添加遥信数据
        if (CS101_ASDU_addInformationObject(asdu, (InformationObject*)SinglePointInformation_create(NULL, ioAddress, value, quality))) {
            bool success = false;
            
            if (m_server) {
                success = CS104_Server_enqueueASDU(m_server, asdu);
            } else if (m_client) {
                success = CS104_Client_sendASDU(m_client, asdu);
            }

            CS101_ASDU_destroy(asdu);
            
            if (!success) {
                setError(ErrorType::SEND_FAILED, "Failed to send ASDU");
                return false;
            }

            return true;
        } else {
            CS101_ASDU_destroy(asdu);
            setError(ErrorType::INTERNAL_ERROR, "Failed to add information object");
            return false;
        }
    } catch (const std::exception& e) {
        setError(ErrorType::INTERNAL_ERROR, std::string("Exception during send: ") + e.what());
        return false;
    }
}

bool Iec104Aqui::sendControlCommand(int ioAddress, bool command) {
    if (m_state != ConnectionState::CONNECTED) {
        setError(ErrorType::INVALID_PARAM, "Not connected");
        return false;
    }

    try {
        CS101_ASDU* asdu = CS101_ASDU_create(m_appLayerParams, false, CS101_COT_ACTIVATION, 0, 0, 0, false, false);
        
        if (!asdu) {
            setError(ErrorType::INTERNAL_ERROR, "Failed to create ASDU");
            return false;
        }

        // 添加控制命令
        if (CS101_ASDU_addInformationObject(asdu, (InformationObject*)SingleCommand_create(NULL, ioAddress, command, 0, 0))) {
            bool success = false;
            
            if (m_server) {
                success = CS104_Server_enqueueASDU(m_server, asdu);
            } else if (m_client) {
                success = CS104_Client_sendASDU(m_client, asdu);
            }

            CS101_ASDU_destroy(asdu);
            
            if (!success) {
                setError(ErrorType::SEND_FAILED, "Failed to send control command");
                return false;
            }

            return true;
        } else {
            CS101_ASDU_destroy(asdu);
            setError(ErrorType::INTERNAL_ERROR, "Failed to add control command");
            return false;
        }
    } catch (const std::exception& e) {
        setError(ErrorType::INTERNAL_ERROR, std::string("Exception during send control: ") + e.what());
        return false;
    }
}

void Iec104Aqui::setConnectionStateCallback(ConnectionStateCallback callback) {
    m_connectionCallback = callback;
}

void Iec104Aqui::setDataReceivedCallback(DataReceivedCallback callback) {
    m_dataReceivedCallback = callback;
}

Iec104Aqui::ConnectionState Iec104Aqui::getConnectionState() const {
    return m_state;
}

Iec104Aqui::ErrorType Iec104Aqui::getLastError() const {
    return m_lastError;
}

std::string Iec104Aqui::getLastErrorDescription() const {
    return m_lastErrorDesc;
}

void Iec104Aqui::clientConnectionHandler(void* parameter, CS104_Client connection, CS104_ClientConnectionEvent event) {
    Iec104Aqui* instance = static_cast<Iec104Aqui*>(parameter);
    
    switch (event) {
        case CS104_CONNECTION_OPENED: {
            instance->updateConnectionState(ConnectionState::CONNECTED, "Connection opened");
            break;
        }
        case CS104_CONNECTION_CLOSED: {
            instance->updateConnectionState(ConnectionState::DISCONNECTED, "Connection closed");
            break;
        }
        case CS104_CONNECTION_STARTDT_CON_RECEIVED: {
            instance->updateConnectionState(ConnectionState::CONNECTED, "Start data transfer received");
            break;
        }
        case CS104_CONNECTION_STOPDT_CON_RECEIVED: {
            instance->updateConnectionState(ConnectionState::DISCONNECTED, "Stop data transfer received");
            break;
        }
        case CS104_CONNECTION_TIMEOUT: {
            instance->setError(ErrorType::TIMEOUT, "Connection timeout");
            instance->updateConnectionState(ConnectionState::ERROR, "Connection timeout");
            break;
        }
        case CS104_CONNECTION_FAILED: {
            instance->setError(ErrorType::CONNECTION_FAILED, "Connection failed");
            instance->updateConnectionState(ConnectionState::ERROR, "Connection failed");
            break;
        }
        default:
            break;
    }
}

bool Iec104Aqui::clientASDUHandler(void* parameter, CS104_Client connection, int address, CS101_ASDU* asdu) {
    Iec104Aqui* instance = static_cast<Iec104Aqui*>(parameter);
    
    if (instance->m_dataReceivedCallback) {
        instance->m_dataReceivedCallback(asdu);
    }
    
    return true; // 继续处理其他ASDU
}

bool Iec104Aqui::serverASDUHandler(void* parameter, CS104_Server server, int address, CS101_ASDU* asdu) {
    Iec104Aqui* instance = static_cast<Iec104Aqui*>(parameter);
    
    if (instance->m_dataReceivedCallback) {
        instance->m_dataReceivedCallback(asdu);
    }
    
    return true; // 继续处理其他ASDU
}

void Iec104Aqui::serverConnectionHandler(void* parameter, CS104_Server server, CS104_Client connection, bool connected) {
    Iec104Aqui* instance = static_cast<Iec104Aqui*>(parameter);
    
    if (connected) {
        instance->updateConnectionState(ConnectionState::CONNECTED, "Client connected");
    } else {
        instance->updateConnectionState(ConnectionState::DISCONNECTED, "Client disconnected");
    }
}

void Iec104Aqui::updateConnectionState(ConnectionState state, const std::string& message) {
    m_state = state;
    
    if (m_connectionCallback) {
        m_connectionCallback(state, message);
    }
}

void Iec104Aqui::setError(ErrorType error, const std::string& description) {
    m_lastError = error;
    m_lastErrorDesc = description;
}

} // namespace Aqui
```

## 3. 使用示例

### 3.1 客户端模式示例

```cpp
#include "AquiIec104.h"
#include <iostream>
#include <thread>
#include <chrono>

using namespace Aqui;

int main() {
    // 创建Iec104Aqui实例
    Iec104Aqui iec104;
    
    // 配置连接参数（客户端模式）
    iec104.configure("127.0.0.1", 2404, 3000, false);
    
    // 设置连接状态回调
    iec104.setConnectionStateCallback([](Iec104Aqui::ConnectionState state, const std::string& message) {
        std::cout << "Connection state changed: " << static_cast<int>(state) << ": " << message << std::endl;
    });
    
    // 设置数据接收回调
    iec104.setDataReceivedCallback([](const CS101_ASDU* asdu) {
        std::cout << "Received ASDU with type: " << CS101_ASDU_getTypeID(asdu) << std::endl;
        // 处理接收到的数据
    });
    
    // 初始化连接
    if (iec104.initialize()) {
        std::cout << "Client initialized successfully" << std::endl;
        
        // 发送遥测数据示例
        iec104.sendMeasuredValue(1, 25.5, 0);
        
        // 发送遥信数据示例
        iec104.sendBinaryInput(2, true, 0);
        
        // 发送控制命令示例
        iec104.sendControlCommand(3, true);
        
        // 运行一段时间
        std::this_thread::sleep_for(std::chrono::seconds(10));
        
        // 停止连接
        iec104.stop();
    } else {
        std::cerr << "Failed to initialize client: " << iec104.getLastErrorDescription() << std::endl;
    }
    
    return 0;
}
```

### 3.2 服务端模式示例

```cpp
#include "AquiIec104.h"
#include <iostream>
#include <thread>
#include <chrono>

using namespace Aqui;

int main() {
    // 创建Iec104Aqui实例
    Iec104Aqui iec104;
    
    // 配置连接参数（服务端模式）
    iec104.configure("0.0.0.0", 2404, 3000, true);
    
    // 设置连接状态回调
    iec104.setConnectionStateCallback([](Iec104Aqui::ConnectionState state, const std::string& message) {
        std::cout << "Server state changed: " << static_cast<int>(state) << ": " << message << std::endl;
    });
    
    // 设置数据接收回调
    iec104.setDataReceivedCallback([&iec104](const CS101_ASDU* asdu) {
        std::cout << "Server received ASDU with type: " << CS101_ASDU_getTypeID(asdu) << std::endl;
        
        // 示例：收到控制命令后回复确认
        if (CS101_ASDU_getTypeID(asdu) == C_SC_NA_1) {
            // 发送确认
            iec104.sendBinaryInput(1, true, 0);
        }
    });
    
    // 初始化服务端
    if (iec104.initialize()) {
        std::cout << "Server initialized successfully, listening on port 2404" << std::endl;
        
        // 服务端持续运行
        while (true) {
            // 定期发送遥测数据示例
            iec104.sendMeasuredValue(1, 26.0, 0);
            std::this_thread::sleep_for(std::chrono::seconds(5));
        }
        
        // 注意：实际应用中需要适当的停止机制
        // iec104.stop();
    } else {
        std::cerr << "Failed to initialize server: " << iec104.getLastErrorDescription() << std::endl;
    }
    
    return 0;
}
```

## 4. 编译与链接

### 4.1 CMakeLists.txt示例

```cmake
cmake_minimum_required(VERSION 3.10)
project(Iec104AquiExample)

# 设置C++标准
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 查找lib60870-C库
find_library(LIB60870 NAMES lib60870c.so lib60870c.a 60870c.lib PATHS /usr/local/lib /usr/lib ${CMAKE_SOURCE_DIR}/lib)
find_path(LIB60870_INCLUDE_DIR NAMES lib60870.h PATHS /usr/local/include /usr/include ${CMAKE_SOURCE_DIR}/include)

# 包含头文件目录
include_directories(${LIB60870_INCLUDE_DIR} ${CMAKE_CURRENT_SOURCE_DIR})

# 添加源文件
set(SOURCES AquiIec104.cpp main.cpp)

# 创建可执行文件
add_executable(iec104_example ${SOURCES})

# 链接库
target_link_libraries(iec104_example ${LIB60870})

# 如果是Windows平台，添加必要的系统库
if(WIN32)
    target_link_libraries(iec104_example ws2_32)
endif()
```

### 4.2 编译命令示例（Linux）

```bash
g++ -std=c++11 -I/usr/local/include -L/usr/local/lib AquiIec104.cpp main.cpp -o iec104_example -l60870c
```

### 4.3 编译命令示例（Windows）

```cmd
cl /EHsc /I"C:\lib60870-C\include" AquiIec104.cpp main.cpp /link /LIBPATH:"C:\lib60870-C\lib" 60870c.lib ws2_32.lib
```

## 5. 库安装说明

### 5.1 Linux安装

```bash
# 克隆仓库
git clone https://github.com/mz-automation/lib60870.git
cd lib60870/lib60870-C

# 编译安装
mkdir build
cd build
cmake ..
make
sudo make install
```

### 5.2 Windows安装

1. 从GitHub下载lib60870-C源码：https://github.com/mz-automation/lib60870
2. 使用CMake生成Visual Studio解决方案
3. 编译生成库文件
4. 将头文件和库文件添加到项目中

## 6. 注意事项

1. **线程安全**：该实现不是线程安全的，在多线程环境中使用时需要添加适当的同步机制
2. **错误处理**：所有操作都会设置错误状态，可以通过`getLastError()`和`getLastErrorDescription()`获取详细错误信息
3. **资源管理**：类的析构函数会自动清理资源，确保正确调用析构函数
4. **回调函数**：回调函数会在库的内部线程中执行，避免在回调中执行长时间阻塞操作
5. **协议版本**：该实现基于IEC 60870-5-104协议，确保与通信方使用相同的协议版本
6. **防火墙设置**：确保通信端口（默认2404）在防火墙中开放

## 7. 扩展功能建议

1. 添加批量数据发送功能，提高发送效率
2. 实现更完善的错误恢复机制
3. 添加日志记录功能
4. 支持更多类型的ASDU数据
5. 实现数据缓存和重发机制
6. 添加统计信息（发送/接收数据包数量、错误率等）

该实现提供了一个完整的IEC 104协议封装，支持客户端和服务端模式，具备良好的跨平台兼容性和易用性，可以根据实际需求进行扩展和定制。