# OpenOnion Auth：Agent 通信、同步与信任边界

## OVERVIEW

```text
OpenOnion Auth 不是“把所有密钥同步给 AI”的容器。

它由三个可独立替换的平面组成：

┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
│  Credential      │   │  Approval        │   │  Transport       │
│  凭据平面         │   │  批准平面         │   │  传输平面         │
│                  │   │                  │   │                  │
│ TOTP secret      │   │ 请求内容与风险说明  │   │ 加密 inbox / relay│
│ 加密保险库        │   │ 人点 Allow / Deny │   │ push / poll / LAN│
│ 多设备密钥封装     │   │ 有效期与撤销        │   │ 不接触明文秘密     │
└──────────────────┘   └──────────────────┘   └──────────────────┘

v0.1 只实现第一个平面的本地版本。
Agent 请求、网络 relay 和跨设备同步都不进入 v0.1 的信任边界。
```

## FLOW DIAGRAM

### 1. 今天的本地 TOTP

```text
网站注册 MFA
     │
     │ QR = otpauth://totp/...?secret=K_site
     ▼
┌─────────────────────────────┐
│ OpenOnion Auth（手机）       │
│                             │
│ Android Keystore ──wrap──┐  │
│                          ▼  │
│                  Enc(K_site)│
│                             │
│ OTP = Truncate(             │
│   HMAC(K_site, time / 30s)) │
└──────────────┬──────────────┘
               │ 6 / 8 位短码
               ▼
              人输入网站

K_site 是网站随机发的共享秘密，不是 OpenOnion 私钥。
```

### 2. 下一阶段：Agent 请求，人只点一次 Allow

```text
┌──────────── Agent runtime ─────────────┐
│                                       │
│  LLM / planner                        │
│      │ use_totp(ref, origin, action)  │
│      ▼                                │
│  Security broker / browser executor   │
│      │                                │
│      │ ① Agent 身份密钥签名请求         │
└──────┼────────────────────────────────┘
       │ ② 加密给手机设备公钥
       ▼
┌───────────────────────────────────────┐
│ Untrusted relay / encrypted inbox     │
│                                       │
│ 只见：request_id、密文、过期时间、路由   │
│ 不见：网站账户、TOTP secret、OTP code   │
└──────┬────────────────────────────────┘
       │ ③ push 只通知“有新请求”，App 拉密文
       ▼
┌──────────────── OpenOnion Auth ───────┐
│ ④ 解密 + 验 Agent 签名 + 检查重放       │
│                                       │
│   Agent A 要登录 example.com          │
│   账户：alice@example.com             │
│   动作：填写一次 MFA                   │
│   有效：60 秒                          │
│                                       │
│          [Deny]        [Allow]         │
│                           │             │
│ ⑤ 人的设备密钥签批准结果 ──┘             │
└──────┬────────────────────────────────┘
       │ ⑥ 回传一次性、绑定 origin/request_id 的授权
       ▼
┌──────────── Agent runtime ─────────────┐
│ Security broker 接收，不返回模型上下文   │
│      │                                │
│      ├─ 生成/接收短时 OTP              │
│      ├─ 直接填入绑定的网站 origin       │
│      └─ 立即销毁；不日志、不遥测          │
└───────────────────────────────────────┘
```

这里可以使用 inbox，但 inbox 只是“不可信邮局”。请求和批准都端到端加密、
双方签名、带 nonce 与过期时间。relay 被攻破不应泄露 TOTP secret，也不能伪造批准。

### 3. Google 式多设备同步

```text
                         同步服务器
                  ┌─────────────────────┐
                  │ Enc(K_site, K_vault)│
                  │ Enc(K_vault, D_A)   │
                  │ Enc(K_vault, D_B)   │
                  └──────┬────────┬─────┘
                         │        │
               只同步密文│        │只同步密文
                         ▼        ▼
                   ┌────────┐  ┌────────┐
                   │手机 A   │  │手机 B   │
                   │私钥 D_A │  │私钥 D_B │
                   └────────┘  └────────┘
                         ▲        ▲
                         └──批准新设备──┘

                   默认不存在这一条：

                  Enc(K_vault, Agent key)  ✗
```

新手机加入时，由已有手机批准，或使用独立恢复秘密。每台手机有自己的不可导出设备
密钥；服务器只保存密文。这样能同步，但不会把“拿到 Agent 私钥”等同于“拿到人的
全部 MFA”。

### 4. AI 与系统各自决定什么

```text
人设定的授权上限（不可被 AI 放宽）
┌────────────────────────────────────────┐
│ 哪个 Agent 公钥                         │
│ 哪些 credential_ref / origin / action  │
│ 每次询问 / 30 分钟内允许 / 一直允许      │
│ 最大次数、有效期、撤销条件                │
└───────────────────┬────────────────────┘
                    │
                    ▼
AI 可以在上限内做风险判断
┌────────────────────────────────────────┐
│ 可以选择更谨慎：主动要求人再次批准         │
│ 可以解释：为什么这次登录看起来可疑          │
│ 不可以选择更宽松：绕过人的授权上限           │
└───────────────────┬────────────────────┘
                    │
                    ▼
独立 broker / Auth App 做机械执行
┌────────────────────────────────────────┐
│ 验签、origin binding、过期、nonce、次数、撤销│
│ 不依赖模型“记得遵守”                       │
└────────────────────────────────────────┘
```

## KEY POINTS

```text
1. 开放架构是对的：transport、policy engine、Agent runtime 都应可替换。

2. 但不能让 AI 单独定义自己的权限上限。
   被提示注入或被替换的 Agent 会自然地把自己判断为“低风险”。
   AI 负责判断与建议；人签发边界；独立组件强制执行。

3. 只靠 Agent 私钥可以识别 Agent，也能解开“明确加密给 Agent”的数据；
   它不能安全地自动恢复人的整个 TOTP 保险库。

4. 如果人明确把 K_vault 封装给 Agent，技术上当然能实现全同步；
   但这等于把 Agent 提升为完整 Authenticator，之后不再具有“每次人批准”的保证。

5. SLIP 的适合位置：
   - SLIP-0010：从 OpenOnion 根身份派生不同 Agent 的签名身份；
   - SLIP-0021：在受控根秘密下派生 vault / recovery 等对称子密钥；
   - 网站 K_site：仍按网站 QR 导入，不从任何 SLIP 节点推导。

6. 手机上的设备私钥优先直接由 Android Keystore 生成且不可导出；
   不把 BIP-39 seed 或 SLIP master node 放进 Auth App。
```

## DEPENDENCIES

```text
v0.1（本仓库）
├── RFC 6238 TOTP
├── otpauth URI
├── Android Keystore
├── AES-256-GCM local vault
└── QR scanner

v0.2（先写协议和跨语言 fixtures）
├── Agent/device pairing
├── canonical signed request schema
├── encrypted inbox relay
├── nonce / expiry / replay store
├── origin-bound credential-use capability
└── secure browser executor boundary

未来同步
├── account vault root
├── per-device key wrapping
├── add-device approval / recovery
├── revocation and key rotation
└── encrypted backup format
```
