# QAR 安全系统 (QAR Security System)

QAR 安全系统是一个面向航空领域 QAR（快速访问记录器）数据的综合性安全管理平台。系统采用**传输层加密架构**，核心目标是实现高强度数据加密存储、细粒度的基于角色的访问控制（RBAC）以及完整可追溯的审计日志。

***

## 项目架构

项目基于 **Spring Boot 4.x** 构建的 Web 服务，采用前后端分离架构：

### 主业务系统 (securitysystem)

- **端口**: `8101`
- **主要职责**:
  - 用户认证与授权（无状态 Session + HttpOnly Cookie）
  - 管理员后台（人员管理、注册审批、全量文件导出）
  - 文件存储与管理
  - 系统反馈收集与处理
  - 全局 API 审计日志记录
  - 传输层加密（TLS）

***

## 核心特性

### 安全架构

- **传输层加密**: 使用 Web Crypto API 实现 TLS 传输加密，保护数据传输安全
- **AES-256-GCM 加密**: 采用 AES-256-GCM 算法进行数据加密，确保数据机密性与防篡改
- **会话密钥管理**: 每次会话动态生成密钥，确保传输安全

### 用户管理

- **严格的准入审批流**: 新用户注册采用"申请-审批"机制，必须由管理员审核通过后方可正式创建账号
- **档案库匹配**: 注册信息必须与系统预设的人员档案库匹配（工号、姓名、身份证后四位、联系方式等）
- **动态数据种子**: 系统启动时自动加载 `person_seed.csv` 初始化人员档案

### 安全防护

- **双重 CSRF 防护**: 在 Stateless 架构下结合 CookieCsrfTokenRepository，支持单页应用(SPA)的安全调用
- **UTF-8 全链路编码**: 解决中文数据在存储、传输、显示过程中的编码问题
- **审计日志**: 完整记录用户操作，支持追溯

### 数据持久化

- **数据库**: H2 内存数据库，数据存储在内存中
- **文件存储**: 上传的文件存储在 `securitysystem/securitysystem/data/uploads/` 目录

***

## 快速开始

### 环境要求

- JDK 21
- Maven 3.9+
- 现代浏览器（支持 ES6+）

### 启动主业务系统

进入 `securitysystem/securitysystem` 目录，启动 Spring Boot 应用：

```bash
cd securitysystem/securitysystem
.\mvnw spring-boot:run
```

主系统将在 `http://localhost:8101` 启动。

### 访问系统

- **入口**: 打开浏览器访问 http://localhost:8101/auth.html
- **初始管理员账号**: `admin`
- **初始管理员密码**: `CAUCqar`

***

## 用户指南

### 用户注册流程

1. 访问登录页面，点击"注册账号"
2. 填写个人信息（必须与档案库匹配）：
   - 工号（作为登录账号）
   - 姓名
   - 身份证后四位
   - 联系方式
   - 航司
   - 职位
   - 部门
3. 设置密码并确认
4. 提交申请，等待管理员审批
5. 审批通过后即可使用工号登录

### 文件上传流程

1. 登录系统后进入工作台
2. 选择要上传的文件
3. 前端自动进行传输加密
4. 加密完成后上传至服务器

### 文件下载流程

1. 在工作台选择要下载的文件
2. 服务器返回加密数据
3. 前端自动解密后用户获得原始文件

***

## API 接口

### 认证接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/auth/register` | POST | 用户注册申请 |
| `/api/auth/login` | POST | 用户登录 |
| `/api/auth/logout` | POST | 用户登出 |
| `/api/auth/me` | GET | 获取当前用户信息 |

### 文件接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/files/upload` | POST | 上传文件 |
| `/api/files/download/{id}` | GET | 下载文件 |
| `/api/files` | GET | 获取文件列表 |
| `/api/files/stats` | GET | 获取文件统计 |

### 管理员接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/users` | GET | 获取用户列表 |
| `/api/admin/persons` | GET | 获取人员档案列表 |
| `/api/admin/files` | GET | 获取所有文件列表 |
| `/api/admin/account-requests` | GET | 获取待审批账号申请 |
| `/api/admin/account-requests/{id}/approve` | POST | 批准账号申请 |
| `/api/admin/account-requests/{id}/reject` | POST | 驳回账号申请 |

***

## 项目结构

```
QAR/
├── securitysystem/securitysystem/ # 主业务系统
│   ├── src/main/java/com/qar/securitysystem/
│   │   ├── config/                # 配置类
│   │   ├── controller/            # 控制器
│   │   ├── dto/                   # 数据传输对象
│   │   ├── model/                 # 实体模型
│   │   ├── repo/                  # 数据访问层
│   │   ├── security/              # 安全组件
│   │   ├── service/               # 业务服务
│   │   └── startup/               # 启动初始化
│   ├── src/main/resources/
│   │   ├── static/                # 前端静态文件
│   │   ├── application.properties # 应用配置
│   │   └── person_seed.csv        # 人员档案种子数据
│   └── data/                      # 数据存储目录
│       └── uploads/               # 文件存储
│
├── README.md                      # 项目说明文档
└── wireshark_capture_guide.md     # Wireshark抓包指南
```

***

## 技术栈

- **后端**: Java 21, Spring Boot 4.x, Spring Security, Spring Data JPA, H2 Database
- **前端**: HTML5, Vanilla JavaScript, CSS3, Web Crypto API
- **加密**: AES-256-GCM, RSA-OAEP
- **构建**: Maven Wrapper

***

## 安全设计

### 数据流向

```
[用户文件] 
    ↓ (传输层加密 TLS)
[加密数据] 
    ↓ (HTTPS传输)
[服务器存储]
    ↓ (用户请求下载)
[加密数据返回]
    ↓ (传输层解密)
[用户获得原始文件]
```

### 密钥管理

- **传输密钥**: 由浏览器 Web Crypto API 生成，会话级别
- **RSA密钥对**: 用于密钥交换，每次会话重新生成
- **服务器**: 仅存储数据，无法获取明文

***

## 最新进展与修复

### v1.0.0 (当前版本)

- **[Security] 修复管理员审批 403 错误**:
  - 修复了 Spring Security 6 中默认开启 XOR CSRF 令牌保护导致前端（SPA）请求被拦截的问题
  - 配置 `CsrfTokenRequestAttributeHandler` 禁用了 XOR 保护并解决了延迟加载问题，确保管理员能够正常通过/驳回注册申请
  - 统一将安全配置中的 `.hasRole("ADMIN")` 调整为 `.hasAuthority("ROLE_ADMIN")` 以精准匹配权限

- **[Encoding] UTF-8 全链路编码**:
  - 解决了中文数据在存储、传输、显示过程中的乱码问题
  - 添加了全局字符编码过滤器，确保所有HTTP请求和响应都使用UTF-8编码
  - 配置了服务器端编码设置，保证JSON响应正确显示中文

- **[Feature] 传输层加密**:
  - 实现了完整的传输层加密流程
  - 使用 Web Crypto API 进行 AES-256-GCM 加密
  - 支持 RSA-OAEP 密钥交换

- **[Feature] 文件统计功能**:
  - 添加了文件统计接口 `/api/files/stats`
  - 显示已上传文件和可用数据数量

***

## 开发与测试

### Wireshark 抓包验证

可以使用 Wireshark 捕获和分析系统的加密通信，验证安全性。详细操作指南请参考：

👉 [Wireshark 抓包指南](./wireshark_capture_guide.md)

**验证要点：**
- ✓ 传输层加密正常工作
- ✓ 数据在传输过程中加密
- ✓ 会话密钥正确管理

### 测试账号

系统预设了以下测试人员档案（`person_seed.csv`）：

| 工号 | 姓名 | 身份证后四位 | 联系方式 | 航司 | 职位 | 部门 |
|------|------|--------------|----------|------|------|------|
| 20260001 | 张三 | 1234 | 13800000000 | CAUC | 机长 | 飞行一部 |
| 20260002 | 李四 | 5678 | 13900000000 | CAUC | 副驾驶 | 飞行二部 |
| 20260003 | 王五 | 0000 | - | CAUC | 签派员 | 运行控制部 |

***

## 许可证

本项目仅供学习和研究使用。
