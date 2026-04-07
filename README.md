# QAR 安全系统 (QAR Security System)

QAR 安全系统是一个面向航空领域 QAR（快速访问记录器）数据的综合性安全管理平台。系统采用前后端分离和微服务化架构，核心目标是实现高强度的数据加密存储、细粒度的基于角色的访问控制（RBAC）以及完整可追溯的审计日志。

***

## 项目架构

项目由两个核心服务和辅助工具集组成：

### 1. `securitysystem` (主业务系统)

基于 **Spring Boot 3.x/4.x** 构建的 Web 服务，负责核心业务逻辑。

- **端口**: `8080`
- **主要职责**:
  - 用户认证与授权（无状态 Session + HttpOnly Cookie）。
  - 管理员后台（人员管理、注册审批、全量文件导出）。
  - 文件上传、下载及管理。
  - 系统反馈收集与处理。
  - 全局 API 审计日志记录。

### 2. `local-crypto-service` (本地加密服务)

一个轻量级的独立 Java 服务，专职处理高强度的加密运算。

- **端口**: `18234`
- **主要职责**:
  - 提供 `/encrypt` 和 `/decrypt` 等 RESTful 接口。
  - 采用 **BouncyCastle** 引擎实现 `AES-256-GCM` 认证加密。
  - 密钥生成、IV 向量管理以及模拟 L-ABE（属性基加密）的密钥封装。

***

## 核心特性

- **高强度加密保护**: 全面采用 BouncyCastle 提供的 AES-256-GCM 算法，确保数据机密性与防篡改。
- **严格的准入审批流**: 新用户注册采用“申请-审批”机制，必须由管理员审核通过后方可正式创建账号。
- **动态数据种子**: 系统启动时自动加载 `person_seed.csv` 初始化人员档案，并自动生成/更新超级管理员账号。
- **双重 CSRF 防护**: 在 Stateless 架构下结合 CookieCsrfTokenRepository，支持单页应用(SPA)的安全调用。
- **专项 Excel 处理**: 提供针对 Excel 文件内容的解析与加密接口，满足特定数据保护需求。

***

## 快速开始

### 环境要求

- JDK 17 或 21 (系统配置为 Java 21)
- Maven 3.9+

### 1. 启动本地加密服务

进入 `local-crypto-service` 目录，编译并启动服务：

```bash
cd local-crypto-service
mvnw clean package -DskipTests
start.bat  # Windows 环境下运行
```

*服务将在* *`http://127.0.0.1:18234`* *启动，可通过* *`/health`* *接口验证。*

### 2. 启动主业务系统

进入 `securitysystem/securitysystem` 目录，启动 Spring Boot 应用：

```bash
cd securitysystem/securitysystem
mvnw spring-boot:run
```

*主系统将在* *`http://localhost:8080`* *启动。*

### 3. 访问系统

- **入口**: 打开浏览器访问 <http://localhost:8080/auth.html>
- **初始管理员账号**: `admin`
- **初始管理员密码**: `CAUCqar`

***

## 最新进展与修复

- **\[Security] 修复管理员审批 403 错误**:
  - 修复了 Spring Security 6 中默认开启 XOR CSRF 令牌保护导致前端（SPA）请求被拦截的问题。
  - 配置 `CsrfTokenRequestAttributeHandler` 禁用了 XOR 保护并解决了延迟加载问题，确保管理员能够正常通过/驳回注册申请。
  - 统一将安全配置中的 `.hasRole("ADMIN")` 调整为 `.hasAuthority("ROLE_ADMIN")` 以精准匹配权限。
- **\[Crypto] BouncyCastle 引擎集成**:
  - 将加解密核心从 JDK 默认 JCE 切换为 BouncyCastle (`bcprov-jdk18on`)。
  - 修复了因为打包成 Fat JAR 导致 BC 签名验证失败（`JCE cannot authenticate the provider BC`）的问题，改用 `maven-dependency-plugin` 独立加载依赖。
  - 废弃硬编码的测试密钥，实现了真正的 AES-256 随机密钥生成与模拟 L-ABE 的动态封装。

***

## 技术栈

- **后端**: Java 21, Spring Boot, Spring Security, Spring Data JPA, H2 Database (In-Memory)
- **前端**: HTML5, Vanilla JavaScript, CSS3
- **加密**: BouncyCastle, javax.crypto
- **构建**: Maven Wrapper

