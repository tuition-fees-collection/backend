# Identity & security — teammate guide

Branch: `feat/dev-a-identity-security`

This branch adds login, JWT authentication, role-based authorization, national-ID lookup, staff/institution admin, and security audit logging.

You do **not** implement your own auth checks. Inject the identity services below and call them at the start of your use case.

---

## How authentication works

1. Client calls `POST /api/v1/auth/login` with username and password.
2. On success they receive a Bearer JWT (valid **15 minutes**).
3. Every other API request must send:

```http
Authorization: Bearer <accessToken>
```

Public routes (no token):

- `POST /api/v1/auth/login`
- Swagger UI and `/v3/api-docs`

Everything else requires a valid JWT. A missing/invalid/expired token returns **401**. A valid token with the wrong role/scope returns **403** (`AccessDeniedException`).

Deactivating a staff account increments `token_version`, so existing JWTs stop working immediately.

Required local env vars (the app will not start without them):

```sh
export JWT_SECRET_BASE64=...
export NATIONAL_ID_HMAC_KEY_BASE64=...
```

Test profile already has dummy keys. Never commit real secrets.

---

## Roles and who they are

| `RoleCode` | `ActorType` | Meaning |
| --- | --- | --- |
| `PAYER` | `GUARDIAN` | Guardian paying for their linked students |
| `BANK_OFFICER` | `BACKOFFICE_EMPLOYEE` | Bank employee searching / collecting |
| `INSTITUTION_OFFICER` | `INSTITUTION_USER` | School/university staff, scoped to one institution |
| `ADMIN` | `BACKOFFICE_EMPLOYEE` | Identity and staff administration only |

Current actor (from the JWT):

```java
public record AuthenticatedActor(
        UUID accountId,
        UUID actorId,          // guardian / employee / institution-user id
        ActorType actorType,
        RoleCode role,
        UUID institutionId,    // set only for INSTITUTION_OFFICER
        String branch          // set for some back-office staff
) {}
```

---

## Functions you must call in your work

These two interfaces are the contract for billing, payments, receipts, and other feature work.

### 1. `IdentityAccessService` — call this first in every secured use case

Package: `com.tuitionnetwork.tuitionnetwork.identity.service`

| Method | Who may pass | When to call |
| --- | --- | --- |
| `currentActor()` | Any authenticated caller | Read who is calling (ids, role, institution, branch) |
| `requireStudentAccess(studentId)` | `PAYER` who is linked to that student | Before reading/writing one student's fees, instalments, receipts |
| `requireInstitutionAccess(institutionId)` | `INSTITUTION_OFFICER` of that institution | Before institution-scoped billing or student lists |
| `requireBankOfficer()` | `BANK_OFFICER` only | Before bank-officer collection / search flows |
| `requireAdmin()` | `ADMIN` only | Before staff or institution administration |

Denied calls are audited as `AUTHORIZATION_DENIED` and throw `AccessDeniedException`.

```java
@Service
public class FeeQueryService {

    private final IdentityAccessService identityAccess;
    private final FeeRepository fees;

    public FeeSnapshot getStudentFee(UUID studentId) {
        identityAccess.requireStudentAccess(studentId);
        return fees.findByStudentId(studentId);
    }

    public List<FeeSnapshot> listInstitutionFees(UUID institutionId) {
        identityAccess.requireInstitutionAccess(institutionId);
        return fees.findByInstitutionId(institutionId);
    }

    public List<FeeSnapshot> listForCollection() {
        identityAccess.requireBankOfficer();
        AuthenticatedActor officer = identityAccess.currentActor();
        return fees.findOpenForBranch(officer.branch());
    }
}
```

**Which check to use**

- Payer app, one student → `requireStudentAccess(studentId)`
- Institution portal → `requireInstitutionAccess(institutionId)`
- Bank back office → `requireBankOfficer()`
- Admin screens → `requireAdmin()`
- Need the caller's ids after a check → `currentActor()`

Do not compare `actor.role()` yourself if one of the `require*` methods already covers it.

---

### 2. `IdentityQueryService` — student/guardian resolution

Use this instead of querying guardian or national-ID tables yourself.

| Method | Who may pass | Returns |
| --- | --- | --- |
| `getCurrentPayerStudentIds()` | Current `PAYER` / `GUARDIAN` | Student IDs the logged-in payer may see |
| `resolveGuardianSearchScope(NationalId)` | `BANK_OFFICER` | Guardian id + that guardian's student ids |

`NationalId` must be exactly **14 digits**. Invalid input throws `IllegalArgumentException`.

Unknown national ID throws `GuardianNotFoundException`.

National IDs are HMAC-hashed before lookup and the lookup is audited. Never store or log the raw national ID.

```java
// Payer dashboard: list only their students
Set<UUID> studentIds = identityQuery.getCurrentPayerStudentIds();

// Bank officer search by national ID
GuardianSearchScope scope =
        identityQuery.resolveGuardianSearchScope(new NationalId("29801011234567"));
UUID guardianId = scope.guardianId();
Set<UUID> studentIds = scope.studentIds();
```

---

### 3. `AuditService` — only for new security-sensitive events

Identity already audits login, denied access, national-ID lookup, and admin staff/institution changes.

Call `AuditService` only if your feature adds a **new** security-relevant action (for example a privileged override). Reuse `AuditAction` values; do not invent a parallel audit table.

| Method | Use |
| --- | --- |
| `recordAuthenticated(action, actor)` | Logged-in actor did something sensitive |
| `recordNationalIdLookup(actor, nationalIdHmac)` | Already used by identity lookup — do not call this yourself |
| `recordUnauthenticated(action)` | Failures with no logged-in actor (login failures) |

---

## What you should **not** call

These are identity internals. Do not inject them from billing/payments/etc.

- `AuthService` / `TokenIssuer` — login only
- `CurrentActorProvider` — use `IdentityAccessService.currentActor()`
- `NationalIdHasher` — use `IdentityQueryService.resolveGuardianSearchScope`
- `AuthAccountRepository`, guardian lookup repositories, JWT validators
- Raw `SecurityContextHolder` / JWT claim parsing

Do not add `@PreAuthorize` role strings as a second policy. The `require*` methods are the authorization API.

---

## HTTP APIs (frontend / Postman)

Swagger: http://localhost:8080/swagger-ui.html  
Use **Authorize** and paste the JWT from login.

### Login

`POST /api/v1/auth/login`

```json
{ "username": "officer.one", "password": "secret" }
```

Success `200`:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresInSeconds": 900
}
```

Failure `401`:

```json
{
  "code": "AUTHENTICATION_FAILED",
  "message": "Invalid credentials."
}
```

Login also locks an account after **5** failed attempts for **15 minutes**. Locked and inactive accounts get the same generic 401.

### Admin — accounts (ADMIN JWT required)

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/admin/accounts/backoffice` | Create login for an existing back-office employee |
| `POST` | `/api/v1/admin/accounts/institution` | Create login for an existing institution user |
| `PATCH` | `/api/v1/admin/accounts/{id}/deactivate` | Disable staff login and revoke JWTs |
| `PATCH` | `/api/v1/admin/accounts/{id}/reactivate` | Re-enable staff login |

### Admin — back-office staff (ADMIN JWT required)

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/admin/backoffice-employees` | List bank/admin employees |
| `POST` | `/api/v1/admin/backoffice-employees` | Create employee + login (`fullName`, `branch`, `role`, `username`, `password`) |
| `PATCH` | `/api/v1/admin/backoffice-employees/{employeeId}/role` | Change role (`BANK_OFFICER` or `ADMIN`) |

### Admin — institutions (ADMIN JWT required)

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/admin/institutions` | List institutions |
| `POST` | `/api/v1/admin/institutions` | Create institution |
| `PATCH` | `/api/v1/admin/institutions/{institutionId}` | Update name / type / status |
| `GET` | `/api/v1/admin/institutions/{institutionId}/users` | List institution officers |
| `POST` | `/api/v1/admin/institutions/{institutionId}/users` | Create institution officer + login |

Admin services already call `requireAdmin()`. You do not add another check in those controllers.

---

## Annotate new controllers

Protected business endpoints should declare Bearer auth so they appear correctly in Swagger:

```java
@RestController
@RequestMapping("/api/v1/fees")
@SecurityRequirement(name = "bearerAuth")
public class FeeController {
    // inject your feature service; that service calls IdentityAccessService
}
```

Authorization belongs in the **service** layer, not only on the controller.

---

## Typical feature mapping

| Your work | Call |
| --- | --- |
| Payer sees own outstanding fees | `getCurrentPayerStudentIds()` then query those students |
| Payer opens one student | `requireStudentAccess(studentId)` |
| Institution officer lists their institution's students/fees | `requireInstitutionAccess(institutionId)` |
| Bank officer searches a guardian by national ID | `resolveGuardianSearchScope(nationalId)` |
| Bank officer works a payment/collection case | `requireBankOfficer()` then `currentActor()` for branch if needed |
| Create/disable staff or institutions | use the admin HTTP APIs (already authorized) |

---

## Errors to handle

| Condition | Result |
| --- | --- |
| No / bad / expired JWT | `401` |
| Wrong role or no relationship to the resource | `AccessDeniedException` → treat as `403` |
| National ID not 14 digits | `IllegalArgumentException` → `400` |
| National ID not found | `GuardianNotFoundException` |
| Admin username/password/profile invalid | `400` `INVALID_*_REQUEST` |
| Duplicate username / conflict | `409` |

If you call identity methods with no JWT in the security context, `currentActor()` throws `IllegalStateException("No authenticated actor")`.

---

## Checkout

```sh
git fetch origin
git checkout feat/dev-a-identity-security
```

New Flyway migrations on this branch: `V5` auth accounts, `V6` role seed (`PAYER`, `BANK_OFFICER`, `INSTITUTION_OFFICER`, `ADMIN`), `V7` unauthenticated audit events. Do not edit those files; add `V8+` for your schema.

Questions about a new endpoint: decide the role first, then call the matching `require*` or `IdentityQueryService` method. If none fits, ask before adding a new authorization rule.
