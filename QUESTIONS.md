# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes — consolidate to a single consistent data-access pattern (e.g., repository layer using JPA/Quarkus Panache or Spring Data) and hide DB details behind interfaces. This reduces duplication, improves transaction handling, testability and maintainability.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
OpenAPI-first: pros — contract-driven, consistent models/stubs, faster client/server updates; cons — generated code can be verbose and harder to customize.
Hand-coded: pros — full control and easier custom behaviour; cons — risk of drifting from spec and more boilerplate.
Choice: prefer OpenAPI-first for public/critical APIs (generate models/controllers) and keep a thin hand-written service layer for business logic; hand-code only when flexibility is essential.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Prioritise: (1) unit tests for domain logic, (2) integration tests for persistence and API boundaries, (3) a few end-to-end/contract tests. Keep fast tests in CI and slow/integration suites scheduled or gated. Focus on behaviour and critical paths, add regression tests for bugs, measure coverage, and require tests for changes via PRs.
```