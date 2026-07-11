# Blog 与训练中心前端整合 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在同一域名下保留 Vue 2 Blog，并交付使用 Blog 主题、接入单体 Blog API 的 React 桌面端训练中心和统一训练管理页面。

**Architecture:** Nginx 按路径托管两个静态产物：Vue Blog 负责 `/`，React 训练中心负责 `/training/**`，浏览器通过 `/api/**` 访问唯一 Blog API。两套前端共享同源 JWT 和用户摘要，但各自实现相同结构与设计变量的顶栏；React 保留现有高密度训练查询排版，并将管理功能收敛为用户管理和训练数据管理两个模块。

**Tech Stack:** Java 21、Spring Boot 3.5.16、MyBatis、JUnit 5、Mockito、React 18、TypeScript 5.7、Vite 6、Vitest、Testing Library、Vue 2.6、Vue CLI 4、Nginx 1.27、Docker Compose。

## Global Constraints

- 只在 `/Users/bytedance/.codex/worktrees/03d7/custacm-platform` 的 `feature_blog-integration` 分支工作，不切换或修改 `main`。
- 未经用户明文命令不得 commit 或 push；本计划中的每个任务以 diff/status 检查点结束，不执行提交。
- 初版只支持 1280px 到 2560px 桌面浏览器，验收重点为 1440×900 和 1920×1080；不实现移动端导航、触摸交互或卡片重排。
- 保留 Vue 2 Blog 和 React 训练前端两套构建，不引入微前端框架，不迁移 Blog CMS，不把 Blog 重写为 React。
- 训练页面保留现有筛选器、多人 rating 表格、单人查询和题目查询的信息密度与主要排版。
- 角色只使用 `ROLE_admin` 和 `ROLE_player`；业务身份只使用 `username`，不得保留 `studentIdentity` 或 `disable` 兼容层。
- 所有浏览器 API 请求使用同源 `/api/**`；Nginx 去掉 `/api` 后转发到 Blog API。
- 不记录或导出密码、JWT、Authorization 头、HS512 密钥、数据库密码或一次性明文密码。
- 新增 Java 业务逻辑和前端行为必须先写失败测试，再实现最小代码，再运行聚焦测试。
- Java 变更最终执行 `mvn clean verify` 和 `./scripts/check-test-policy.sh`；React 变更最终执行 `pnpm lint && pnpm test && pnpm typecheck && pnpm build`。

---

## File Structure Map

### Backend

- `platform-blog/upstream/nblog/blog-api/src/main/java/top/naccl/model/vo/TrainingUserSummary.java`：队员可读的精简训练目录响应。
- `platform-blog/upstream/nblog/blog-api/src/main/java/top/naccl/service/TrainingUserQueryService.java`：连接 Blog 用户与 OJ handle 账号，过滤可采集队员。
- `platform-blog/upstream/nblog/blog-api/src/main/java/top/naccl/controller/player/TrainingDataQueryController.java`：新增 `/users` 路由，继续承载训练查询 HTTP 适配。
- `platform-blog/upstream/nblog/blog-api/src/test/java/top/naccl/service/TrainingUserQueryServiceTest.java`：验证字段收敛、过滤和排序。
- `platform-blog/upstream/nblog/blog-api/src/test/java/top/naccl/TrainingDataRouteContractTest.java`：锁定新增路由合同。

### React 训练中心

- `frontend/src/api/client.ts`：Blog `Result` 解包、HTTP 错误、Bearer header 和 AbortSignal。
- `frontend/src/api/auth.ts`：登录、当前用户和当前用户资料/密码接口。
- `frontend/src/api/training.ts`：队员目录和五个训练查询接口。
- `frontend/src/api/admin.ts`：用户、handle、采集任务和数仓刷新接口。
- `frontend/src/auth/session.ts`：共享 `localStorage` 键及安全读写。
- `frontend/src/routing.ts`：`/training/**` 的纯路由解析和跳转辅助函数。
- `frontend/src/utils/runLimited.ts`：可取消、有限并发的多人统计队列。
- `frontend/src/hooks/useAuthSession.ts`：登录恢复、登录、退出和 401 处理。
- `frontend/src/hooks/usePlatformDashboard.ts`：组合训练查询和管理状态；移除双服务健康/module-info 状态。
- `frontend/src/components/AppShell.tsx`：Blog 风格全局顶栏和训练二级导航。
- `frontend/src/components/LoginPanel.tsx`：`/training/login` 登录页表单。
- `frontend/src/components/TrainingQueryPanel.tsx`：保留查询布局并迁移为 username/Blog API 数据。
- `frontend/src/components/AdminUserManagementPanel.tsx`：单一用户管理模块。
- `frontend/src/components/TrainingDataOpsPanel.tsx`：单一训练数据管理模块。
- `frontend/src/components/TrainingAdminPanel.tsx`：用户管理/训练数据管理双菜单页面壳。
- `frontend/src/App.tsx`：受保护路由与页面组合。
- `frontend/src/styles/theme.css`、`shell.css`、`dashboard.css`、`table.css`：Blog 主题变量、顶栏和桌面端高密度内容。

### Vue Blog

- `platform-blog/upstream/nblog/blog-view/src/auth/session.js`：读取/清理与 React 相同的登录键。
- `platform-blog/upstream/nblog/blog-view/src/plugins/axios.js`：改为同源 `/api/`。
- `platform-blog/upstream/nblog/blog-view/src/components/index/Nav.vue`：训练中心入口、账号摘要和退出。
- `platform-blog/upstream/nblog/blog-view/src/router/index.js`：原 `/login` 重定向到 `/training/login`。

### Deployment and docs

- `frontend/Dockerfile`：分别构建 React 和 Vue，最终生成一个 Nginx 镜像。
- `frontend/nginx.conf`：`/api`、`/training` 和 Blog 三层路由。
- `frontend/vite.config.ts`：`base=/training/` 和本地 `/api` 代理。
- `deploy/docker-compose.yml`、`deploy/.env.example`：增加前端服务和端口。
- `docs/api.md`、`docs/architecture.md`、`docs/authorization.md`、`docs/agent/context-map.md`、`frontend/README.md`、`frontend/AGENTS.md`、`platform-blog/README.md`、`platform-blog/AGENTS.md`、`deploy/README.md`、`deploy/AGENTS.md`、`deploy/UPDATE.md`、`docs/server-deployment.md`：同步最终边界和验证方式。

---

### Task 1: 增加队员可读的精简训练目录

**Files:**
- Create: `platform-blog/upstream/nblog/blog-api/src/main/java/top/naccl/model/vo/TrainingUserSummary.java`
- Create: `platform-blog/upstream/nblog/blog-api/src/main/java/top/naccl/service/TrainingUserQueryService.java`
- Create: `platform-blog/upstream/nblog/blog-api/src/test/java/top/naccl/service/TrainingUserQueryServiceTest.java`
- Modify: `platform-blog/upstream/nblog/blog-api/src/main/java/top/naccl/controller/player/TrainingDataQueryController.java`
- Modify: `platform-blog/upstream/nblog/blog-api/src/test/java/top/naccl/TrainingDataRouteContractTest.java`

**Interfaces:**
- Consumes: `UserMapper.findAll()` and `OjHandleAccountService.listAll()`.
- Produces: `TrainingUserQueryService.listCollectableUsers(): List<TrainingUserSummary>` and `GET /player/training-data/users`.
- `TrainingUserSummary` fields are exactly `String username`, `String nickname`, `List<String> ojNames`.

- [ ] **Step 1: Write the failing service test**

```java
package top.naccl.service;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.naccl.entity.User;
import top.naccl.mapper.UserMapper;
import top.naccl.model.vo.TrainingUserSummary;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingUserQueryServiceTest {
    @Mock UserMapper userMapper;
    @Mock OjHandleAccountService handleAccountService;

    @Test
    void returnsOnlyCollectableBoundUsersWithoutHandlesOrPrivateFields() {
        User active = user("player-a", "队员 A");
        User retired = user("player-b", "队员 B");
        when(userMapper.findAll()).thenReturn(List.of(retired, active));
        Instant now = Instant.parse("2026-07-11T00:00:00Z");
        when(handleAccountService.listAll()).thenReturn(List.of(
                new OjHandleAccount("player-a", Map.of("CODEFORCES", "secret-cf", "ATCODER", "secret-at"), true, now, now),
                new OjHandleAccount("player-b", Map.of("CODEFORCES", "retired-handle"), false, now, now)
        ));

        var result = new TrainingUserQueryService(userMapper, handleAccountService).listCollectableUsers();

        assertEquals(List.of(new TrainingUserSummary(
                "player-a", "队员 A", List.of("CODEFORCES", "ATCODER"))), result);
    }

    private static User user(String username, String nickname) {
        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        return user;
    }
}
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run:

```bash
mvn -pl platform-blog/upstream/nblog/blog-api -am \
  -Dtest=TrainingUserQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation fails because `TrainingUserSummary` and `TrainingUserQueryService` do not exist.

- [ ] **Step 3: Add the response record and query service**

```java
package top.naccl.model.vo;

import java.util.List;

public record TrainingUserSummary(String username, String nickname, List<String> ojNames) {
    public TrainingUserSummary {
        ojNames = List.copyOf(ojNames);
    }
}
```

```java
package top.naccl.service;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import org.springframework.stereotype.Service;
import top.naccl.entity.User;
import top.naccl.mapper.UserMapper;
import top.naccl.model.vo.TrainingUserSummary;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TrainingUserQueryService {
    private final UserMapper userMapper;
    private final OjHandleAccountService handleAccountService;

    public TrainingUserQueryService(UserMapper userMapper, OjHandleAccountService handleAccountService) {
        this.userMapper = userMapper;
        this.handleAccountService = handleAccountService;
    }

    public java.util.List<TrainingUserSummary> listCollectableUsers() {
        Map<String, User> users = userMapper.findAll().stream()
                .collect(Collectors.toMap(User::getUsername, Function.identity()));
        return handleAccountService.listAll().stream()
                .filter(account -> account.needCollect() && users.containsKey(account.username()))
                .map(account -> new TrainingUserSummary(
                        account.username(),
                        users.get(account.username()).getNickname(),
                        account.handles().keySet().stream().toList()))
                .sorted(Comparator.comparing(TrainingUserSummary::username))
                .toList();
    }
}
```

- [ ] **Step 4: Add the controller route and route-contract assertion**

Inject `TrainingUserQueryService` into `TrainingDataQueryController`, store it in a field, and extend the constructor:

```java
private final OjWarehouseQueryController delegate;
private final TrainingUserQueryService trainingUserQueryService;

public TrainingDataQueryController(
        OjAcceptedSummaryQueryService acceptedSummaryQueryService,
        OjSubmissionQueryService submissionQueryService,
        OjFirstAcceptedProblemQueryService firstAcceptedProblemQueryService,
        TrainingUserQueryService trainingUserQueryService
) {
    this.delegate = new OjWarehouseQueryController(
            acceptedSummaryQueryService, submissionQueryService, firstAcceptedProblemQueryService);
    this.trainingUserQueryService = trainingUserQueryService;
}
```

Add:

```java
@GetMapping("/users")
public Result users() {
    return Result.ok("获取成功", trainingUserQueryService.listCollectableUsers());
}
```

Update the route assertion to:

```java
assertEquals(Set.of(
        "/users",
        "/accepted-summary",
        "/submissions/by-user",
        "/submissions/by-problem",
        "/first-accepted/by-user",
        "/first-accepted/by-problem"),
        mappedPaths(TrainingDataQueryController.class, GetMapping.class));
```

- [ ] **Step 5: Run backend focused tests**

Run:

```bash
mvn -pl platform-blog/upstream/nblog/blog-api -am \
  -Dtest=TrainingUserQueryServiceTest,TrainingDataRouteContractTest,SecurityConfigTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: all selected tests pass; `SecurityConfigTest` continues to prove `/player/**` is player/admin-only.

- [ ] **Step 6: Review the task diff without committing**

Run:

```bash
git diff --check
git status --short
```

Expected: no whitespace errors; only the planned backend files and existing Blog integration changes are present. Do not commit.

---

### Task 2: Replace the React API and identity contracts with Blog contracts

**Files:**
- Create: `frontend/src/api/client.ts`
- Create: `frontend/src/api/auth.ts`
- Create: `frontend/src/api/training.ts`
- Create: `frontend/src/api/admin.ts`
- Create: `frontend/src/auth/session.ts`
- Modify: `frontend/src/types.ts`
- Rewrite test: `frontend/src/test/platform-api.test.ts`
- Create test: `frontend/src/test/session.test.ts`

**Interfaces:**
- Produces: `requestData<T>(path, init): Promise<T>`, `ApiError`, `authHeaders(token)`, and focused API functions.
- Produces storage keys `custacm.accessToken` and `custacm.user`.
- All downstream React code consumes `Username = string`, `AccountRole = 'ROLE_admin' | 'ROLE_player'`, `CurrentUser`, `AdminUserMutationResponse`, `TrainingUser`, and Blog training response types.

- [ ] **Step 1: Write failing API-envelope tests**

```ts
it('unwraps Blog Result data', async () => {
  stubFetch({ code: 200, errorCode: null, msg: 'ok', data: { username: 'player-a' } });
  await expect(requestData<{ username: string }>('/player/me')).resolves.toEqual({ username: 'player-a' });
});

it('throws stable Blog errorCode even when the HTTP response is JSON', async () => {
  stubFetch(
    { code: 403, errorCode: 'AUTH_FORBIDDEN', msg: '权限不足', data: null },
    403,
  );
  await expect(requestData('/admin/users')).rejects.toMatchObject({
    status: 403,
    errorCode: 'AUTH_FORBIDDEN',
    message: '权限不足',
  });
});

it('sends the batch-create body as a raw array', async () => {
  const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: [] });
  await batchCreateUsers('token', [{ username: 'player-a', role: 'ROLE_player' }]);
  expect(JSON.parse(String(fetchMock.mock.calls[0]?.[1]?.body))).toEqual([
    { username: 'player-a', role: 'ROLE_player' },
  ]);
});
```

- [ ] **Step 2: Write failing shared-session tests**

```ts
it('stores and removes the shared Blog/Training login keys', () => {
  writeSession('jwt', { username: 'player-a', nickname: 'A', avatar: '', email: '', role: 'ROLE_player' });
  expect(localStorage.getItem('custacm.accessToken')).toBe('jwt');
  expect(readSession()?.user.username).toBe('player-a');
  clearSession();
  expect(readSession()).toBeNull();
});

it('drops malformed user JSON without throwing', () => {
  localStorage.setItem('custacm.accessToken', 'jwt');
  localStorage.setItem('custacm.user', '{bad-json');
  expect(readSession()).toBeNull();
});
```

- [ ] **Step 3: Run the focused frontend tests and verify they fail**

Run:

```bash
cd frontend
pnpm test -- platform-api.test.ts session.test.ts
```

Expected: tests fail because the new focused API modules and session helpers do not exist.

- [ ] **Step 4: Implement the Blog Result client**

`client.ts` must implement this contract:

```ts
export interface BlogResult<T> {
  code: number;
  errorCode: string | null;
  msg: string;
  data: T;
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly errorCode: string | null,
    message: string,
    readonly body: unknown,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

const API_BASE = import.meta.env.VITE_API_BASE ?? '/api';

export function authHeaders(token?: string): HeadersInit {
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function requestData<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: { Accept: 'application/json', ...(init.headers ?? {}) },
  });
  const body = await response.json().catch(() => null) as BlogResult<T> | null;
  if (!response.ok || !body || body.code !== 200) {
    throw new ApiError(
      response.status,
      body?.errorCode ?? null,
      body?.msg || `HTTP ${response.status}`,
      body,
    );
  }
  return body.data;
}
```

JSON write helpers in `auth.ts` and `admin.ts` must add `Content-Type: application/json` and reuse `authHeaders`.

- [ ] **Step 5: Define exact new identity and response types**

Add the new role/identity definitions used by the focused Blog API modules:

```ts
export type Username = string;
export type AccountRole = 'ROLE_admin' | 'ROLE_player';

export interface CurrentUser {
  username: Username;
  nickname: string;
  avatar: string;
  email: string;
  role: AccountRole;
}

export interface TrainingUser {
  username: Username;
  nickname: string;
  ojNames: OjName[];
}

export interface AdminUserMutationResponse {
  user: CurrentUser & { id: number; createTime: string; updateTime: string };
  handles: Partial<Record<OjName, string>>;
  needCollect: boolean | null;
  generatedPassword: string | null;
  reloginRequired: boolean;
}
```

Define the Blog training response fields with `username`. Keep the old declarations and `api/platform.ts` temporarily so the untouched screens continue to compile during this independently testable migration task; Task 6 removes them after every consumer has moved. This is an implementation bridge only and is not shipped in the final state.

- [ ] **Step 6: Implement focused API functions**

The modules must export these exact functions:

```ts
// auth.ts
login(username: string, password: string, signal?: AbortSignal): Promise<{ token: string; user: CurrentUser }>;
getCurrentUser(token: string, signal?: AbortSignal): Promise<CurrentUser>;
changeCurrentPassword(token: string, oldPassword: string, newPassword: string): Promise<void>;

// training.ts
listTrainingUsers(token: string, signal?: AbortSignal): Promise<TrainingUser[]>;
getAcceptedSummary(token: string, username: string, range: TrainingQueryRange, ojName: OjName, signal?: AbortSignal): Promise<AcceptedSummary>;
getUserSubmissions(token: string, username: string, range: TrainingQueryRange, page: PageQuery, ojName: OjName, signal?: AbortSignal): Promise<UserSubmissionReport>;
getProblemSubmissions(token: string, problemKey: string, range: TrainingQueryRange, page: PageQuery, ojName: OjName, signal?: AbortSignal): Promise<ProblemSubmissionReport>;
getUserFirstAccepted(token: string, username: string, range: TrainingQueryRange, page: PageQuery, ojName: OjName, signal?: AbortSignal): Promise<UserFirstAcceptedReport>;
getProblemFirstAccepted(token: string, problemKey: string, range: TrainingQueryRange, page: PageQuery, ojName: OjName, signal?: AbortSignal): Promise<ProblemFirstAcceptedReport>;

// admin.ts
listAdminUsers(token: string): Promise<AdminUserMutationResponse[]>;
createUser(token: string, request: AdminUserCreateRequest): Promise<AdminUserMutationResponse>;
batchCreateUsers(token: string, requests: AdminUserCreateRequest[]): Promise<AdminUserMutationResponse[]>;
patchUser(token: string, username: string, request: AdminUserPatchRequest): Promise<AdminUserMutationResponse>;
updateOjHandles(token: string, username: string, request: OjHandlesUpdateRequest): Promise<AdminUserMutationResponse>;
deleteUser(token: string, username: string): Promise<void>;
startCollectionJob(token: string, request: CollectionJobStartRequest): Promise<CollectionJob>;
listCollectionJobs(token: string): Promise<CollectionJob[]>;
getCollectionJob(token: string, jobId: string): Promise<CollectionJob>;
refreshWarehouse(token: string, ojName: OjName, request: WarehouseRefreshRequest): Promise<WarehouseRefreshResult>;
```

- [ ] **Step 7: Implement shared session helpers**

```ts
const TOKEN_KEY = 'custacm.accessToken';
const USER_KEY = 'custacm.user';

export function writeSession(token: string, user: CurrentUser) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function readSession(): { token: string; user: CurrentUser } | null {
  const token = localStorage.getItem(TOKEN_KEY);
  const rawUser = localStorage.getItem(USER_KEY);
  if (!token || !rawUser) return null;
  try {
    const user = JSON.parse(rawUser) as CurrentUser;
    return user?.username && (user.role === 'ROLE_admin' || user.role === 'ROLE_player')
      ? { token, user }
      : null;
  } catch {
    clearSession();
    return null;
  }
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}
```

- [ ] **Step 8: Run the API and session tests**

Run:

```bash
cd frontend
pnpm test -- platform-api.test.ts session.test.ts
pnpm typecheck
```

Expected: focused tests and TypeScript compile pass while the old screens continue using the temporary old module.

- [ ] **Step 9: Review the task diff without committing**

Run `git diff --check && git status --short`. Do not commit.

---

### Task 3: Build the Blog-themed React shell, routes and login gate

**Files:**
- Create: `frontend/src/routing.ts`
- Create: `frontend/src/hooks/useAuthSession.ts`
- Create: `frontend/src/styles/theme.css`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/AppShell.tsx`
- Modify: `frontend/src/components/LoginPanel.tsx`
- Modify: `frontend/src/main.tsx`
- Modify: `frontend/src/styles.css`
- Modify: `frontend/src/styles/shell.css`
- Modify: `frontend/src/test/app-navigation.test.tsx`
- Create: `frontend/src/test/routing.test.ts`
- Create: `frontend/src/test/use-auth-session.test.tsx`

**Interfaces:**
- Consumes: Task 2 `login`, `getCurrentUser`, `readSession`, `writeSession`, `clearSession`.
- Produces: route type `TrainingRoute`, `useAuthSession()`, desktop `AppShell`, and `/training/login|multiple|single|problem|admin` rendering.

- [ ] **Step 1: Write failing pure route tests**

```ts
expect(parseTrainingRoute('/training')).toEqual({ page: 'multiple' });
expect(parseTrainingRoute('/training/single')).toEqual({ page: 'single' });
expect(parseTrainingRoute('/training/problem')).toEqual({ page: 'problem' });
expect(parseTrainingRoute('/training/admin')).toEqual({ page: 'admin', section: 'users' });
expect(safeReturnPath('https://evil.example/')).toBe('/training/multiple');
expect(safeReturnPath('/training/problem')).toBe('/training/problem');
```

- [ ] **Step 2: Write failing auth restoration tests**

```ts
it('revalidates a stored session through player/me', async () => {
  writeSession('jwt', storedUser);
  vi.mocked(getCurrentUser).mockResolvedValue(freshUser);
  const { result } = renderHook(() => useAuthSession());
  await waitFor(() => expect(result.current.status).toBe('authenticated'));
  expect(result.current.user).toEqual(freshUser);
});

it('clears a stored session when player/me returns 401', async () => {
  writeSession('expired', storedUser);
  vi.mocked(getCurrentUser).mockRejectedValue(new ApiError(401, 'AUTH_TOKEN_INVALID', 'expired', null));
  const { result } = renderHook(() => useAuthSession());
  await waitFor(() => expect(result.current.status).toBe('anonymous'));
  expect(readSession()).toBeNull();
});
```

- [ ] **Step 3: Run the route/auth tests and verify they fail**

Run `cd frontend && pnpm test -- routing.test.ts use-auth-session.test.tsx`.

Expected: failures because `routing.ts` and `useAuthSession.ts` are absent.

- [ ] **Step 4: Implement route parsing and safe navigation**

```ts
export type TrainingPage = 'login' | 'multiple' | 'single' | 'problem' | 'admin';
export type AdminSection = 'users' | 'training';
export interface TrainingRoute { page: TrainingPage; section?: AdminSection }

export function parseTrainingRoute(pathname: string, search = ''): TrainingRoute {
  const page = pathname.split('/').filter(Boolean)[1];
  if (page === 'login' || page === 'single' || page === 'problem') return { page };
  if (page === 'admin') {
    const section = new URLSearchParams(search).get('section');
    return { page: 'admin', section: section === 'training' ? 'training' : 'users' };
  }
  return { page: 'multiple' };
}

export function safeReturnPath(value: string | null): string {
  return value?.startsWith('/training/') && !value.startsWith('//')
    ? value
    : '/training/multiple';
}
```

Use `history.pushState` plus a `popstate` listener, matching the existing no-router-dependency approach.

- [ ] **Step 5: Implement `useAuthSession`**

The hook returns:

```ts
interface AuthSessionState {
  status: 'restoring' | 'anonymous' | 'authenticated';
  token: string | null;
  user: CurrentUser | null;
  signIn(username: string, password: string): Promise<void>;
  signOut(): void;
  changePassword(oldPassword: string, newPassword: string): Promise<void>;
}
```

It must optimistically load stored state, call `/player/me`, rewrite the fresh user summary, and clear state on `401`. It must not treat `403` or network errors as an automatic logout. `changePassword` calls Task 2 `changeCurrentPassword` and keeps both password values only in component state for the duration of the request.

- [ ] **Step 6: Replace the left-sidebar shell with the approved desktop shell**

`AppShell` structure must be:

```tsx
<div className="training-site">
  <header className="blog-topbar">
    <a className="site-name" href="/home">custacm wiki</a>
    <nav aria-label="站点导航">
      <a href="/home">首页</a>
      <a href="/archives">归档</a>
      <a className="is-active" href="/training/multiple">训练中心</a>
      <a href="/moments">动态</a>
      <a href="/about">关于</a>
    </nav>
    <AccountSummary />
  </header>
  <nav className="training-subnav" aria-label="训练中心导航">...</nav>
  <main className="training-main">{children}</main>
</div>
```

The second row contains `多人统计 / 单人查询 / 题目查询` and an admin-only `管理员操作`. Remove `context-sidebar`, unsupported module placeholders, workspace switcher, mobile hamburger rules and responsive card conversions.

The account summary opens a small desktop menu containing `修改密码` and `退出`. The password form validates that the two new-password inputs match before calling `changePassword`; it clears all password fields after success or close.

- [ ] **Step 7: Convert login from modal to `/training/login` page**

The login form submits `{username,password}` to Task 2 `login`; on success it writes the shared session and replaces history with `safeReturnPath(returnTo)`. It must never persist the password or include a remember-me flag unsupported by Blog API.

- [ ] **Step 8: Update navigation tests**

Tests must assert:

```ts
expect(screen.getByRole('link', { name: '训练中心' }).classList.contains('is-active')).toBe(true);
expect(screen.queryByText('暂未开放')).toBeNull();
expect(screen.queryByRole('complementary')).toBeNull();
expect(window.location.pathname).toBe('/training/single');
```

Add these role assertions:

```ts
it('hides admin navigation from a player', () => {
  mockAuth({ role: 'ROLE_player' });
  render(<App />);
  expect(screen.queryByRole('link', { name: '管理员操作' })).toBeNull();
});

it('opens the unified admin route for an administrator', async () => {
  mockAuth({ role: 'ROLE_admin' });
  render(<App />);
  await userEvent.click(screen.getByRole('link', { name: '管理员操作' }));
  expect(window.location.pathname).toBe('/training/admin');
});

it('redirects an anonymous protected visit to login with a local return path', () => {
  mockAnonymousAuth();
  window.history.replaceState(null, '', '/training/problem');
  render(<App />);
  expect(window.location.pathname).toBe('/training/login');
  expect(new URLSearchParams(window.location.search).get('returnTo')).toBe('/training/problem');
});
```

- [ ] **Step 9: Run shell and auth tests**

Run:

```bash
cd frontend
pnpm test -- routing.test.ts use-auth-session.test.tsx app-navigation.test.tsx
pnpm typecheck
```

Expected: selected tests pass.

- [ ] **Step 10: Review the task diff without committing**

Run `git diff --check && git status --short`. Do not commit.

---

### Task 4: Migrate the three training query pages and bounded multi-user loading

**Files:**
- Create: `frontend/src/utils/runLimited.ts`
- Create: `frontend/src/test/run-limited.test.ts`
- Modify: `frontend/src/hooks/usePlatformDashboard.ts`
- Modify: `frontend/src/components/TrainingQueryPanel.tsx`
- Modify: `frontend/src/test/use-platform-dashboard.test.tsx`
- Modify: `frontend/src/test/training-query-panel.test.tsx`
- Modify: `frontend/src/test/fixtures.ts`
- Modify: `frontend/src/styles/dashboard.css`
- Modify: `frontend/src/styles/table.css`

**Interfaces:**
- Consumes: Task 2 training API and Task 3 authenticated `{token,user}`.
- Produces: `runLimited`, `MultiUserSummaryRow`, and username-based multiple/single/problem state used by `TrainingQueryPanel`.

- [ ] **Step 1: Write the failing bounded-concurrency test**

```ts
it('never runs more than the requested number of workers', async () => {
  let active = 0;
  let maxActive = 0;
  const results = await runLimited([1, 2, 3, 4, 5], 2, async (value) => {
    active += 1;
    maxActive = Math.max(maxActive, active);
    await new Promise<void>((resolve) => setTimeout(resolve, 10));
    active -= 1;
    return value * 2;
  });
  expect(results).toHaveLength(5);
  expect(maxActive).toBe(2);
});
```

```ts
it('preserves input order and isolates a rejected item', async () => {
  const results = await runLimited([3, 1, 2], 2, async (value) => {
    if (value === 1) throw new Error('row failed');
    return value * 10;
  });
  expect(results[0]).toEqual({ status: 'fulfilled', value: 30 });
  expect(results[1]).toMatchObject({ status: 'rejected' });
  expect(results[2]).toEqual({ status: 'fulfilled', value: 20 });
});

it('stops after abort', async () => {
  const controller = new AbortController();
  controller.abort();
  await expect(runLimited([1], 1, async (value) => value, controller.signal))
    .rejects.toMatchObject({ name: 'AbortError' });
});
```

- [ ] **Step 2: Write failing dashboard tests for the new directory flow**

Stub Blog envelopes for `/api/player/training-data/users` and `/api/player/training-data/accepted-summary`. Assert that:

```ts
expect(summaryRequests).toHaveLength(1);
expect(summaryRequests[0].searchParams.get('username')).toBe('player-a');
expect(result.current.multiUserRows[0]).toMatchObject({
  user: { username: 'player-a' },
  status: 'ready',
});
```

For one accepted-summary stub, return HTTP 500 with `{code:500,errorCode:'TRAINING_QUERY_FAILED',msg:'查询失败',data:null}` and assert:

```ts
expect(result.current.multiUserRows.map((row) => row.status)).toEqual(['ready', 'error']);
expect(result.current.multiUserRows[0].summary?.username).toBe('player-a');
expect(result.current.multiUserRows[1]).toMatchObject({
  user: { username: 'player-b' },
  summary: null,
  errorMessage: '查询失败',
});
```

- [ ] **Step 3: Run query tests and verify they fail**

Run `cd frontend && pnpm test -- run-limited.test.ts use-platform-dashboard.test.tsx training-query-panel.test.tsx`.

- [ ] **Step 4: Implement the limited runner**

```ts
export type LimitedResult<T> =
  | { status: 'fulfilled'; value: T }
  | { status: 'rejected'; reason: unknown };

export async function runLimited<T, R>(
  items: readonly T[],
  limit: number,
  worker: (item: T, index: number) => Promise<R>,
  signal?: AbortSignal,
): Promise<Array<LimitedResult<R>>> {
  const results = new Array<LimitedResult<R>>(items.length);
  let nextIndex = 0;
  const runWorker = async () => {
    while (nextIndex < items.length) {
      if (signal?.aborted) throw new DOMException('Aborted', 'AbortError');
      const index = nextIndex++;
      try {
        results[index] = { status: 'fulfilled', value: await worker(items[index]!, index) };
      } catch (reason) {
        if (signal?.aborted) throw new DOMException('Aborted', 'AbortError');
        results[index] = { status: 'rejected', reason };
      }
    }
  };
  await Promise.all(Array.from({ length: Math.min(Math.max(1, limit), items.length) }, runWorker));
  return results;
}
```

- [ ] **Step 5: Replace the old public-user/handle-map loading**

Delete calls to old public users, public handle maps, health and module-info endpoints. On authenticated dashboard refresh:

1. Load `listTrainingUsers(token)`.
2. Filter users whose `ojNames` contain the current OJ.
3. Run accepted-summary requests with `runLimited(..., 6, ...)`.
4. Store each row as:

```ts
export interface MultiUserSummaryRow {
  user: TrainingUser;
  status: 'ready' | 'error';
  summary: AcceptedSummary | null;
  errorMessage: string | null;
}
```

Retain an `AbortController` for the current multi-user load. Abort it before changing OJ or range and on unmount.

- [ ] **Step 6: Migrate single and problem queries**

Use `username` query parameters and the five `/player/training-data/**` endpoints. Preserve existing date conversion, rating filters, page sizes, automatic filter application and latest-response-wins behavior.

- [ ] **Step 7: Update the multi-user table without redesigning it**

Keep the existing table order and rating cells. Replace identity rendering with:

```tsx
<strong>{row.user.nickname || row.user.username}</strong>
<small>{row.user.username} · {row.summary?.authorHandle ?? '查询失败'}</small>
```

Error rows retain their position, display `查询失败`, and provide a `重试` button that reloads only that username. Show `已完成 X/Y` while the queue is active. Preserve the sticky player column and horizontal rating scroll.

- [ ] **Step 8: Run query tests and typecheck**

Run:

```bash
cd frontend
pnpm test -- run-limited.test.ts use-platform-dashboard.test.tsx training-query-panel.test.tsx
pnpm typecheck
```

Expected: selected tests pass and no `studentIdentity` type remains in query code.

- [ ] **Step 9: Review the task diff without committing**

Run `git diff --check && git status --short`. Do not commit.

---

### Task 5: Consolidate account creation and editing into User Management

**Files:**
- Modify: `frontend/src/components/AdminUserManagementPanel.tsx`
- Modify: `frontend/src/hooks/usePlatformDashboard.ts`
- Modify: `frontend/src/test/admin-user-management-panel.test.tsx`
- Modify: `frontend/src/test/use-platform-dashboard.test.tsx`
- Modify: `frontend/src/styles/side-panel.css`
- Modify: `frontend/src/styles/table.css`

**Interfaces:**
- Consumes: Task 2 admin user functions and `AdminUserMutationResponse`.
- Produces: one `AdminUserManagementPanel` with create/batch actions and expandable user rows; no `view="create"|"edit"` prop.

- [ ] **Step 1: Write failing unified-panel tests**

Render the component once and assert that the same page contains the user table and a `创建用户` action. Clicking it must open the create form without changing route. Add tests that:

```ts
expect(screen.getByRole('heading', { name: '用户管理' })).not.toBeNull();
expect(screen.getByRole('button', { name: '创建用户' })).not.toBeNull();
expect(screen.getByRole('table', { name: '用户列表' })).not.toBeNull();
```

Submit a patch with `newUsername`, `nickname`, `email`, `avatar`, `role`, and blank `password`; assert the exact Blog request. Submit handle changes separately and assert `PUT /admin/users/{username}/oj-handles`.

- [ ] **Step 2: Run the component tests and verify they fail**

Run `cd frontend && pnpm test -- admin-user-management-panel.test.tsx use-platform-dashboard.test.tsx`.

- [ ] **Step 3: Replace old auth/handle orchestration**

Update dashboard actions to call:

```ts
batchCreateUsers(token, rows);
patchUser(token, username, patch);
updateOjHandles(token, username, { handles, needCollect });
deleteUser(token, username);
```

Delete OJ handle identity migration and purge-before-delete calls. Username changes are performed only through `patchUser`; delete is performed only through `deleteUser`.

- [ ] **Step 4: Merge create and edit layouts**

The module header contains `创建用户` and `批量导入` buttons. The default body is the user table. Creation opens an in-page side panel; editing continues to expand the selected table row. Fields are exactly:

```text
username, nickname, email, avatar, role, password, CODEFORCES handle,
ATCODER handle, needCollect
```

Role options are only `ROLE_admin` and `ROLE_player`. Remove all `root`, numeric-student-prefix, retired-role, immutable-identity and `disable` special cases.

- [ ] **Step 5: Handle generated passwords and relogin**

Show `generatedPassword` in a one-time result block with an explicit copy action. Keep it only in component state and clear it when the result block closes or the route changes. If `reloginRequired` is true for the current username, call `signOut()` after showing the success notice.

- [ ] **Step 6: Keep dangerous actions explicit**

Use `window.confirm` for user deletion and password reset. The delete confirmation must state that training rows are deleted while Blog articles/comments are retained.

- [ ] **Step 7: Run user-management tests**

Run:

```bash
cd frontend
pnpm test -- admin-user-management-panel.test.tsx use-platform-dashboard.test.tsx
pnpm typecheck
```

Expected: selected tests pass.

- [ ] **Step 8: Review the task diff without committing**

Run `git diff --check && git status --short`. Do not commit.

---

### Task 6: Consolidate collection, jobs and warehouse refresh into Training Data Management

**Files:**
- Create: `frontend/src/components/TrainingAdminPanel.tsx`
- Modify: `frontend/src/components/TrainingDataOpsPanel.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/hooks/usePlatformDashboard.ts`
- Modify: `frontend/src/test/training-data-ops-panel.test.tsx`
- Modify: `frontend/src/test/app-navigation.test.tsx`
- Modify: `frontend/src/styles/dashboard.css`
- Modify: `frontend/src/styles/table.css`
- Modify: `frontend/src/types.ts`
- Delete: `frontend/src/api/platform.ts`

**Interfaces:**
- Consumes: Task 2 `startCollectionJob`, `listCollectionJobs`, `getCollectionJob`, `refreshWarehouse` and Task 5 user-management component.
- Produces: `/training/admin?section=users|training`, with two top-level menu buttons and no other admin workspace routes.

- [ ] **Step 1: Write failing two-menu navigation tests**

```ts
window.history.pushState(null, '', '/training/admin?section=users');
render(<App />);
expect(screen.getAllByRole('tab')).toHaveLength(2);
expect(screen.getByRole('tab', { name: '用户管理' })).not.toBeNull();
expect(screen.getByRole('tab', { name: '训练数据管理' })).not.toBeNull();
expect(screen.queryByRole('tab', { name: '操作记录' })).toBeNull();
```

Click the training tab and assert `window.location.search === '?section=training'` and the collection/job UI is visible.

- [ ] **Step 2: Write failing warehouse-refresh test**

Select `ATCODER`, enter optional `batchId`, confirm the action, and assert:

```ts
expect(onRefreshWarehouse).toHaveBeenCalledWith('ATCODER', {
  batchId: 'batch-20260711',
  startFromTaskId: null,
});
```

- [ ] **Step 3: Run admin-training tests and verify they fail**

Run `cd frontend && pnpm test -- training-data-ops-panel.test.tsx app-navigation.test.tsx`.

- [ ] **Step 4: Implement the two-menu page shell**

`TrainingAdminPanel` receives `section`, `onSectionChange`, user-management props and training-management props. It renders exactly two ARIA tabs. `onSectionChange` updates the current URL query parameter without changing `/training/admin`.

- [ ] **Step 5: Migrate collection job requests**

Start batch jobs with:

```ts
{
  usernames: selectedUsernames,
  lookbackHours: unlimited ? UNLIMITED_LOOKBACK_HOURS : lookbackHours,
  refreshWarehouse,
  ojName,
}
```

Keep `UNLIMITED_LOOKBACK_HOURS = 1_000_000_000`; the backend requires a positive value and does not accept `null` as unlimited.

Poll `GET /admin/training-data/submission-collection-jobs/{jobId}` while the selected job is pending/running. Stop polling on terminal status, unmount, logout or route change. Never call the removed standalone purge or old Codeforces-only job routes.

- [ ] **Step 6: Add manual warehouse refresh**

Provide OJ, optional `batchId`, and optional `startFromTaskId`. Require confirmation, call `POST /admin/training-data/{ojName}/warehouse:refresh`, and render the returned SQL task result without logging raw payloads.

- [ ] **Step 7: Remove synthetic operation-record workspace**

Delete the old `records` admin route, service/module health cards, export task model and operation timeline from the main App. Collection job history remains inside training data management and is the only initial operation record surface.

After all consumers use `api/auth.ts`, `api/training.ts`, and `api/admin.ts`, delete `api/platform.ts` and remove the temporary old `StudentIdentity`, `disable`, health/module-info, purge-composition and dual-runtime types from `types.ts`.

- [ ] **Step 8: Run admin tests and typecheck**

Run:

```bash
cd frontend
pnpm test -- training-data-ops-panel.test.tsx app-navigation.test.tsx
pnpm typecheck
```

Expected: selected tests pass; only two admin tabs exist.

- [ ] **Step 9: Review the task diff without committing**

Run `git diff --check && git status --short`. Do not commit.

---

### Task 7: Add the Training Center entry and shared login summary to Vue Blog

**Files:**
- Create: `platform-blog/upstream/nblog/blog-view/src/auth/session.js`
- Modify: `platform-blog/upstream/nblog/blog-view/src/plugins/axios.js`
- Modify: `platform-blog/upstream/nblog/blog-view/src/components/index/Nav.vue`
- Modify: `platform-blog/upstream/nblog/blog-view/src/router/index.js`
- Modify: `platform-blog/upstream/nblog/blog-view/README.md`
- Generate: `platform-blog/upstream/nblog/blog-view/package-lock.json`

**Interfaces:**
- Consumes: shared keys from Task 2 and `/training/login|multiple` routes from Task 3.
- Produces: Blog topbar Training Center link, account summary, logout, same-origin API base.

- [ ] **Step 1: Add a deterministic Blog lockfile**

Run:

```bash
cd platform-blog/upstream/nblog/blog-view
npm install --package-lock-only --legacy-peer-deps
```

Expected: `package-lock.json` is generated without changing declared dependency versions.

- [ ] **Step 2: Add shared session helpers**

```js
const TOKEN_KEY = 'custacm.accessToken'
const USER_KEY = 'custacm.user'

export function readUser() {
  const token = window.localStorage.getItem(TOKEN_KEY)
  const raw = window.localStorage.getItem(USER_KEY)
  if (!token || !raw) return null
  try {
    const user = JSON.parse(raw)
    return user && user.username ? user : null
  } catch (_) {
    clearSession()
    return null
  }
}

export function clearSession() {
  window.localStorage.removeItem(TOKEN_KEY)
  window.localStorage.removeItem(USER_KEY)
}
```

- [ ] **Step 3: Change Blog API calls to same-origin `/api/`**

In `src/plugins/axios.js`, use:

```js
const request = axios.create({
  baseURL: '/api/',
  timeout: 10000,
})
```

Keep the existing anonymous comment `identification` header behavior. Do not attach the training JWT globally to public Blog requests.

- [ ] **Step 4: Add Training Center and account actions to `Nav.vue`**

Use a normal anchor because the target belongs to the React application:

```vue
<a href="/training/multiple" class="item">
  <i class="chart bar icon"></i>训练中心
</a>
```

Initialize `authUser: readUser()` in component data. On the right, show either `登录` linking to `/training/login` or `{nickname || username}` plus an `退出` button. Logout calls `clearSession()` and sets `authUser = null`.

- [ ] **Step 5: Redirect the legacy Blog login route**

Replace the lazy-loaded `/login` view route with a route guard redirect:

```js
{
  path: '/login',
  beforeEnter() {
    window.location.replace('/training/login')
  }
}
```

- [ ] **Step 6: Build the Vue Blog**

Run:

```bash
cd platform-blog/upstream/nblog/blog-view
NODE_OPTIONS=--openssl-legacy-provider npm ci --legacy-peer-deps
NODE_OPTIONS=--openssl-legacy-provider npm run build
```

Expected: Vue production build succeeds and emits `dist/`.

- [ ] **Step 7: Review the task diff without committing**

Run `git diff --check && git status --short`. Do not commit.

---

### Task 8: Build and route both frontends through one Nginx service

**Files:**
- Create: `frontend/Dockerfile`
- Modify: `frontend/nginx.conf`
- Modify: `frontend/vite.config.ts`
- Modify: `.dockerignore`
- Modify: `.gitignore`
- Modify: `deploy/docker-compose.yml`
- Modify: `deploy/.env.example`

**Interfaces:**
- Consumes: Vue build from Task 7, React routes from Task 3 and Blog API service name `blog-api:8090`.
- Produces: one `frontend` Compose service on `${FRONTEND_PORT}:80`.

- [ ] **Step 1: Write the expected Compose shape as a failing config check**

Before editing, run:

```bash
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config | rg 'frontend|FRONTEND_PORT'
```

Expected: no frontend service is found.

- [ ] **Step 2: Set the Vite base and local proxy**

`frontend/vite.config.ts` must include:

```ts
export default defineConfig({
  base: '/training/',
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
});
```

- [ ] **Step 3: Create the multi-stage frontend image**

```dockerfile
FROM node:20.19-alpine AS training-build
RUN corepack enable
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY frontend/ ./
RUN pnpm build

FROM node:20.19-alpine AS blog-build
ENV NODE_OPTIONS=--openssl-legacy-provider
WORKDIR /workspace/blog-view
COPY platform-blog/upstream/nblog/blog-view/package.json \
     platform-blog/upstream/nblog/blog-view/package-lock.json ./
RUN npm ci --legacy-peer-deps
COPY platform-blog/upstream/nblog/blog-view/ ./
RUN npm run build

FROM nginx:1.27-alpine
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=blog-build /workspace/blog-view/dist /usr/share/nginx/html/blog
COPY --from=training-build /workspace/frontend/dist /usr/share/nginx/html/training
```

- [ ] **Step 4: Replace Nginx routing**

```nginx
server {
  listen 80;
  server_name _;

  location ^~ /api/ {
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    rewrite ^/api/(.*)$ /$1 break;
    proxy_pass http://blog-api:8090;
  }

  location = /training {
    return 302 /training/multiple;
  }

  location ^~ /training/ {
    root /usr/share/nginx/html;
    try_files $uri $uri/ /training/index.html;
  }

  location / {
    root /usr/share/nginx/html/blog;
    try_files $uri $uri/ /index.html;
  }
}
```

- [ ] **Step 5: Add the Compose frontend service**

```yaml
  frontend:
    build:
      context: ..
      dockerfile: frontend/Dockerfile
    ports:
      - "${FRONTEND_PORT}:80"
    depends_on:
      blog-api:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "wget -q -O - http://localhost/ >/dev/null || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 20
```

Add `FRONTEND_PORT=3000` to `deploy/.env.example`. Add `.superpowers/` to both `.gitignore` and `.dockerignore` so local brainstorming artifacts are neither tracked nor copied into image build context.

- [ ] **Step 6: Validate Vite and Compose configuration**

Run:

```bash
cd frontend && pnpm build
cd ..
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config
```

Expected: React assets use `/training/`; Compose shows `blog-db`, `blog-redis`, `blog-api`, and `frontend` only.

- [ ] **Step 7: Build the frontend image**

Run:

```bash
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml build frontend
```

Expected: both Vue and React stages build and the final Nginx image is created.

- [ ] **Step 8: Review the task diff without committing**

Run `git diff --check && git status --short`. Do not commit.

---

### Task 9: Synchronize module and deployment documentation

**Files:**
- Modify: `frontend/AGENTS.md`
- Modify: `frontend/README.md`
- Modify: `platform-blog/AGENTS.md`
- Modify: `platform-blog/README.md`
- Modify: `platform-blog/upstream/nblog/blog-api/README.md`
- Modify: `deploy/AGENTS.md`
- Modify: `deploy/README.md`
- Modify: `deploy/UPDATE.md`
- Modify: `docs/api.md`
- Modify: `docs/architecture.md`
- Modify: `docs/authorization.md`
- Modify: `docs/agent/README.md`
- Modify: `docs/agent/context-map.md`
- Modify: `docs/server-deployment.md`
- Modify: `CHANGELOG.md`

**Interfaces:**
- Consumes: final paths, commands and responsibilities produced by Tasks 1–8.
- Produces: documentation that describes only implemented behavior and passes the repository doc-sync check.

- [ ] **Step 1: Update the frontend module documents**

Document:

- Blog `/` and Training `/training/**` ownership.
- `/api/**` same-origin proxy.
- username and two-role contract.
- `/training/admin` two-menu structure.
- desktop-only scope.
- exact `pnpm` and Vue build commands.
- file-level responsibilities for every created frontend file.

- [ ] **Step 2: Update Blog API and authorization docs**

Add `GET /player/training-data/users` with its three response fields and privacy constraints. Remove statements that the Vue frontends are outside scope once they are deployed. Keep Blog API direct paths separate from browser `/api` proxy paths.

- [ ] **Step 3: Update deployment docs**

Document the four-service stack, `FRONTEND_PORT`, Nginx routing, local URLs, health checks and update commands. Explicitly retain the warning against `docker compose down --volumes`.

- [ ] **Step 4: Update architecture/context/changelog**

Record that Blog API remains the only backend runtime while Vue Blog and React Training are two static frontends served by one Nginx process. Update `CHANGELOG.md` in natural Chinese and do not claim server deployment.

- [ ] **Step 5: Run doc checks**

Run:

```bash
./scripts/check-doc-sync.sh origin/main WORKTREE
git diff --check
```

Expected: doc-sync and whitespace checks pass.

- [ ] **Step 6: Review the task diff without committing**

Run `git status --short`. Do not commit.

---

### Task 10: Full verification and local desktop acceptance

**Files:**
- Modify only if verification reveals an in-scope defect: files already listed in Tasks 1–9.
- No new production feature files are expected.

**Interfaces:**
- Consumes: complete backend, Vue, React and Compose implementation.
- Produces: evidence that the approved desktop initial version works through real HTTP.

- [ ] **Step 1: Scan for removed identity and old runtime contracts**

Run:

```bash
rg -n 'studentIdentity|ROLE_disable|\bdisable\b|/api/auth|/api/training-data|auth-web|training-data-web' \
  frontend platform-blog/upstream/nblog/blog-view
```

Expected: no executable frontend code references old identity, roles or dual backend paths. Historical design/changelog text is reviewed separately and need not be mechanically rewritten.

- [ ] **Step 2: Run complete React checks**

Run:

```bash
cd frontend
pnpm lint
pnpm test
pnpm typecheck
pnpm build
```

Expected: all commands exit 0.

- [ ] **Step 3: Run the Vue production build**

Run:

```bash
cd platform-blog/upstream/nblog/blog-view
NODE_OPTIONS=--openssl-legacy-provider npm ci --legacy-peer-deps
NODE_OPTIONS=--openssl-legacy-provider npm run build
```

Expected: build exits 0.

- [ ] **Step 4: Run complete Java and policy checks**

Run from repository root:

```bash
mvn clean verify
./scripts/check-test-policy.sh
mvn clean package -DskipTests
```

Expected: tests, JaCoCo gates, policy checks and packaging all exit 0.

- [ ] **Step 5: Validate and start Compose without deleting volumes**

Run:

```bash
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml up -d --build
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml ps
```

Expected: `blog-db`, `blog-redis`, `blog-api`, and `frontend` are running; healthchecked services become healthy. Do not use `down --volumes`.

- [ ] **Step 6: Execute real HTTP authorization smoke tests**

Using bootstrap credentials from the local environment, verify:

```text
GET  /                         -> 200 Blog HTML
GET  /training/multiple        -> 200 React HTML
GET  /api/health               -> 200
GET  /api/player/training-data/users without token -> 401
POST /api/login                -> 200 Blog Result with token/user
GET  /api/player/me with player/admin token -> 200
GET  /api/player/training-data/users with player token -> 200
GET  /api/admin/users with player token -> 403
GET  /api/admin/users with admin token -> 200
```

Never print tokens or passwords; store the token only in a temporary shell variable and unset it after the smoke test.

- [ ] **Step 7: Execute desktop browser acceptance**

At 1440×900 and 1920×1080 verify:

1. Blog navigation and public article pages render.
2. Training Center link opens `/training/multiple` and unauthenticated users reach `/training/login`.
3. Login returns to the requested training page.
4. Player can use multiple, single and problem queries and cannot see admin navigation.
5. Multi-user table preserves filters, rating columns, sticky player column, partial failures and row retry.
6. Admin sees one page with only User Management and Training Data Management menus.
7. User create/edit/rename/reset/handle/delete flows match Blog API responses.
8. Collection jobs poll to terminal state and warehouse refresh requires confirmation.
9. Browser refresh preserves route and authentication.
10. Console contains no uncaught errors and no sensitive values.

- [ ] **Step 8: Inspect runtime logs for secrets**

Search logs for field names and known test markers, not secret values:

```bash
rg -n -i 'authorization:|bearer |password|accessToken|BLOG_TOKEN_SECRET|BLOG_DB_PASSWORD' logs
```

Expected: no credentials, JWTs, Authorization values, signing secrets or database passwords are present. Benign source field names must be inspected rather than blindly accepted.

- [ ] **Step 9: Run final repository checks**

Run:

```bash
./scripts/check-doc-sync.sh origin/main WORKTREE
git diff --check
git status --short
```

Expected: all checks pass and all intended changes remain uncommitted on `feature_blog-integration` until the user explicitly commands a commit or push.
