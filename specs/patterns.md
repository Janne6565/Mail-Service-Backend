# Coding Patterns & Conventions

## Lombok

Always use Lombok — never write getters/setters/constructors by hand.

| Use case | Annotation |
|---|---|
| Service / controller | `@RequiredArgsConstructor` (inject via `final` fields) |
| DTO (outbound) | `@Builder` + `@Value` or `@Data` |
| Logging | `@Slf4j` |
| Entity | `@Getter` + `@Setter` (avoid `@Data` on entities) |

## Controller pattern

Every resource has two files:

```
controller/v1/schema/FooApi.java          ← interface
controller/v1/implementation/FooController.java  ← implements FooApi
```

`FooApi.java` carries all annotations:
```java
@Tag(name = "Foo")
@RequestMapping("/api/v1/foo")
public interface FooApi {

    @Operation(summary = "Get a foo")
    @ApiResponse(responseCode = "200")
    @GetMapping("/{id}")
    ResponseEntity<FooDto> getFoo(@PathVariable String id, @AuthenticationPrincipal UserDetails user);
}
```

`FooController.java` is thin:
```java
@RestController
@RequiredArgsConstructor
public class FooController implements FooApi {

    private final FooService fooService;
    private final SecurityContextService securityContextService;

    @Override
    public ResponseEntity<FooDto> getFoo(String id, UserDetails user) {
        UserEntity u = securityContextService.requireUser();
        return ResponseEntity.ok(fooService.getFoo(id, u));
    }
}
```

## DTO conventions

- **Inbound** (`model/action/`): named `Create*Dto`, `Update*Dto`, `*Dto` for actions. Use `@NotBlank`, `@NotNull`, `@Valid` where applicable.
- **Outbound** (`model/core/`): named `*Dto`. Built with `@Builder`. Never expose entity internals (e.g., encrypted password).
- Updates use `Optional`-friendly patch semantics — `null` field = keep existing.

## Error handling

Use `ResponseStatusException` for all HTTP errors:

```java
throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection not found");
throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
```

Return 404 instead of 403 when the resource shouldn't be known to exist (no information leakage).

## Service conventions

- One service class per domain entity/aggregate.
- Services own all permission logic — controllers never check roles.
- Cross-entity cleanup (e.g., deleting a user → remove their access grants) goes in the service that owns the deletion, not in a cascade annotation, so it's explicit and testable.

## SMTP password encryption

Passwords are encrypted with AES-GCM via `CryptoService` before persistence and decrypted on retrieval by `MailDispatcher`. Never store or log the plaintext.

## Logging

Use `log.info` / `log.error` (from `@Slf4j`) for significant events only:
- Mail send failure: `log.error`
- Purged expired records: `log.info`

Never log sensitive data (passwords, tokens, email bodies).

## Testing

- Unit tests: plain JUnit 5, Mockito for mocks.
- Integration tests: `@SpringBootTest` with H2 + Hibernate `create-drop` (no Flyway). Use `application-test.properties` to override datasource.
- Test naming: `methodName_scenario_expectedResult`.
