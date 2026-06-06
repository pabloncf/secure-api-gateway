# Secure API Gateway — Guia de Fases

**Stack:** Spring Boot + Redis + PostgreSQL + Docker  
**Objetivo:** Construir um API Gateway seguro com autenticação JWT/OAuth2, rate limiting por tier, validação de input, logging de atividade suspeita e painel de métricas.

---

## Visão Geral da Arquitetura

O projeto simula um cenário real: você é o engenheiro responsável por proteger as APIs de uma empresa. Todo request externo passa pelo seu Gateway antes de chegar nos serviços internos. O Gateway decide: quem é você? Você tem permissão? Quantas requests você já fez? Seu input é seguro?

```
Cliente → [API Gateway] → Serviço Interno (simulado)
              │
              ├── JWT Auth Filter
              ├── Rate Limiter (Redis)
              ├── Input Validator
              ├── Suspicious Activity Logger
              └── Metrics Collector
```

O Gateway não é um serviço de negócio — ele é uma **camada de defesa**. Os "serviços internos" serão endpoints simples (tipo um CRUD de usuários) que existem apenas para dar ao Gateway algo para proteger.

---

## Fase 1 — Scaffolding e Infraestrutura Docker

**O que vai ser feito:**  
Criar o projeto Spring Boot do zero com Docker Compose orquestrando três containers: a aplicação, o PostgreSQL e o Redis. Nenhuma lógica de negócio ainda — o objetivo é ter o ambiente rodando e um único endpoint `GET /health` respondendo `200 OK`.

**Por que isso importa:**  
Recrutadores em Berlim esperam ver Docker no portfólio. Mais importante: você garante que qualquer pessoa pode rodar o projeto com um único `docker-compose up`, sem instalar Java, Postgres ou Redis na máquina local.

**O que você vai aprender:**
- Estrutura de um projeto Spring Boot com Gradle/Maven
- Multi-container com Docker Compose
- Health checks e variáveis de ambiente via `.env`

**Resultado esperado:**  
`docker-compose up` sobe os três containers e `curl localhost:8080/health` retorna `{"status": "UP"}`.

**Commit:** `feat: scaffold Spring Boot project with Docker Compose (Postgres + Redis)`

---

## Fase 2 — Modelo de Usuário e Autenticação JWT

**O que vai ser feito:**  
Criar o modelo `User` com roles (`FREE`, `PRO`, `ADMIN`) persistido no PostgreSQL. Implementar dois endpoints públicos: `POST /auth/register` e `POST /auth/login`. O login retorna um JWT assinado contendo o `userId`, `role` e `exp` (expiração). Criar um `JwtAuthFilter` que intercepta todas as requests protegidas, valida o token e injeta o usuário no `SecurityContext`.

**Por que isso importa:**  
JWT é o padrão de autenticação stateless para APIs modernas. O filtro que você vai construir é exatamente o tipo de componente que se discute em entrevistas técnicas de segurança: "como você garante que o token não foi adulterado?", "o que acontece quando o token expira?", "onde você armazena o secret?".

**O que você vai aprender:**
- Spring Security filter chain e como ela processa cada request
- Geração e validação de JWT com HMAC-SHA256
- Como o `SecurityContextHolder` funciona internamente
- Password hashing com BCrypt

**Conceitos de segurança envolvidos:**
- Tokens stateless vs. sessions: trade-offs
- Por que o secret do JWT nunca vai no código (vai na variável de ambiente)
- Expiração de tokens e a superfície de ataque de tokens de longa duração

**Resultado esperado:**  
Registrar um usuário, fazer login, receber o JWT, e acessar um endpoint protegido passando `Authorization: Bearer <token>`. Sem token ou com token inválido: `401 Unauthorized`.

**Commit:** `feat: implement JWT authentication with user roles and Spring Security filter`

---

## Fase 3 — Rate Limiting com Redis por Tier de Usuário

**O que vai ser feito:**  
Implementar um `RateLimitFilter` que roda antes de processar qualquer request. Ele consulta o Redis para verificar quantas requests o usuário fez na janela de tempo atual. Cada tier tem um limite diferente: `FREE` = 10 req/min, `PRO` = 100 req/min, `ADMIN` = ilimitado. Quando o limite é excedido, retorna `429 Too Many Requests` com headers `X-RateLimit-Limit`, `X-RateLimit-Remaining` e `X-RateLimit-Reset`.

**Por que isso importa:**  
Rate limiting é uma defesa primária contra abuso de API, brute force e DDoS na camada de aplicação. Usar Redis para isso é o padrão da indústria porque é atômico (evita race conditions) e rápido (operações em memória).

**O que você vai aprender:**
- Algoritmo de sliding window vs. fixed window vs. token bucket
- Operações atômicas no Redis com `INCR` e `EXPIRE`
- Como o Spring intercepta requests na filter chain antes do controller
- Headers HTTP de rate limiting (padrão IETF draft)

**Conceitos de segurança envolvidos:**
- Por que rate limiting por IP sozinho não basta (NATs compartilhados)
- Rate limiting como defesa contra credential stuffing no `/auth/login`
- A importância de limitar também endpoints não autenticados

**Resultado esperado:**  
Um usuário `FREE` fazendo 11 requests em um minuto recebe `429` na 11ª. Os headers mostram quanto falta para o reset. Um `ADMIN` nunca recebe `429`.

**Commit:** `feat: add Redis-based rate limiting with per-tier configuration`

---

## Fase 4 — Input Validation e Sanitização

**O que vai ser feito:**  
Adicionar validação rigorosa em todos os endpoints que recebem dados do usuário. Isso inclui: Bean Validation (`@Valid`, `@NotBlank`, `@Size`, `@Email`) nos DTOs, um filtro global de sanitização que rejeita payloads com padrões suspeitos (SQL injection, XSS, path traversal), e respostas de erro padronizadas com mensagens claras mas que não vazam detalhes internos.

**Por que isso importa:**  
Input validation é a primeira linha de defesa da OWASP Top 10. Em entrevistas de segurança, a pergunta clássica é: "o que acontece se eu mandar um JSON com um campo `username` de 10.000 caracteres?". Seu Gateway vai ter a resposta: rejeita antes de chegar no serviço.

**O que você vai aprender:**
- Bean Validation API do Jakarta e como customizar validators
- Patterns de sanitização: allowlist vs. blocklist (e por que allowlist é melhor)
- `@ControllerAdvice` para tratamento global de erros
- Como formatar respostas de erro sem expor stack traces

**Conceitos de segurança envolvidos:**
- OWASP Top 10: Injection (A03:2021)
- Por que sanitizar no Gateway é defesa em profundidade (o serviço interno pode ter bugs)
- Limitar tamanho de payload para evitar ataques de consumo de recursos

**Resultado esperado:**  
Enviar `{"username": "<script>alert(1)</script>"}` retorna `400 Bad Request` com uma mensagem clara. Enviar `{"username": "ab"}` (menos de 3 caracteres) retorna erro de validação. Nenhuma mensagem de erro expõe nomes de classes Java ou stack traces.

**Commit:** `feat: add input validation, sanitization filter, and standardized error responses`

---

## Fase 5 — Logging de Atividade Suspeita

**O que vai ser feito:**  
Implementar um sistema de logging estruturado que registra eventos de segurança em uma tabela `security_events` no PostgreSQL. Eventos capturados: login falho, token inválido/expirado, rate limit excedido, input rejeitado por sanitização. Cada evento grava timestamp, IP, user agent, endpoint acessado, tipo do evento e detalhes. Criar um endpoint `GET /admin/events` (apenas `ADMIN`) para consultar os eventos com filtros por tipo e período.

**Por que isso importa:**  
Logging de segurança é o que separa um sistema que "funciona" de um sistema que é auditável. Em compliance (GDPR, SOC2), a capacidade de responder "quem tentou acessar o que e quando" é obrigatória. Para recrutadores, mostra que você pensa além do código.

**O que você vai aprender:**
- Logging estruturado com SLF4J e formato JSON
- Event-driven architecture: publicar eventos sem acoplar os filtros ao repositório
- Spring Events (`ApplicationEventPublisher`) para desacoplamento
- Consultas temporais no PostgreSQL

**Conceitos de segurança envolvidos:**
- O que logar vs. o que nunca logar (nunca passwords, tokens completos, PII desnecessária)
- Detecção de padrões: múltiplos logins falhos do mesmo IP = possível brute force
- Auditability como requisito de compliance

**Resultado esperado:**  
Após 5 tentativas de login falho, `GET /admin/events?type=LOGIN_FAILED&last=1h` retorna os 5 eventos com IP, timestamp e endpoint. Nenhum campo contém a senha que foi tentada.

**Commit:** `feat: implement security event logging with admin query endpoint`

---

## Fase 6 — Painel de Métricas

**O que vai ser feito:**  
Criar um endpoint `GET /admin/metrics` que retorna métricas agregadas em JSON: total de requests por endpoint, taxa de rejeição por rate limit, número de logins falhos/sucedidos nas últimas 24h, top 5 IPs com mais requests, e distribuição de requests por tier. Opcionalmente, criar um frontend mínimo (uma página HTML estática servida pelo Spring Boot) com gráficos simples usando Chart.js.

**Por que isso importa:**  
Métricas transformam o Gateway de "uma ferramenta de bloqueio" em "um sistema de observabilidade". Mostrar que você pensa em monitoring e dashboards demonstra maturidade de engenheiro — você não só constrói, mas também opera e observa.

**O que você vai aprender:**
- Agregações SQL para métricas em tempo real
- Spring Boot Actuator e métricas customizadas com Micrometer
- Servir conteúdo estático no Spring Boot
- Trade-offs entre métricas em tempo real vs. pré-computadas

**Resultado esperado:**  
`GET /admin/metrics` retorna um JSON com as métricas agregadas. O dashboard HTML (se implementado) mostra gráficos de barras com requests por endpoint e um indicador de logins falhos.

**Commit:** `feat: add metrics endpoint and optional monitoring dashboard`

---

## Fase 7 — Testes e Cobertura

**O que vai ser feito:**  
Escrever testes em três camadas. Testes unitários para a lógica de JWT (geração, validação, expiração), para o algoritmo de rate limiting, e para os validators. Testes de integração com `@SpringBootTest` e Testcontainers (PostgreSQL e Redis reais em containers) para testar a filter chain completa. Testes de segurança específicos: tentar acessar endpoints de admin com token de `FREE`, tentar SQL injection nos inputs, tentar bypassar rate limit. Meta: 80%+ de cobertura.

**Por que isso importa:**  
Testes de segurança automatizados são raros em portfólios. A maioria dos devs testa "funciona?" — você vai testar "é seguro?". Isso é o que faz recrutadores pararem na sua página do GitHub.

**O que você vai aprender:**
- Testcontainers para testes de integração com infraestrutura real
- MockMvc para testar a filter chain do Spring Security
- JaCoCo para relatórios de cobertura
- Como pensar em testes adversariais (o que um atacante tentaria?)

**Resultado esperado:**  
`./gradlew test jacocoTestReport` roda todos os testes e gera um relatório com 80%+ de cobertura. Os testes de segurança documentam explicitamente os vetores de ataque testados.

**Commit:** `test: add unit, integration, and security tests with 80%+ coverage`

---

## Fase 8 — Documentação, CI/CD e Polimento Final

**O que vai ser feito:**  
Escrever um README profissional em inglês com: visão geral da arquitetura (com diagrama), decisões de segurança documentadas (por que JWT e não session, por que sliding window, por que allowlist), instruções de setup com Docker, exemplos de uso com `curl`, e badges de CI. Configurar GitHub Actions para rodar testes, gerar relatório de cobertura, e fazer build da imagem Docker em cada push. Adicionar um `SECURITY.md` explicando a política de disclosure de vulnerabilidades.

**Por que isso importa:**  
O README é a primeira coisa que um recrutador vê. Um README que explica decisões de segurança com clareza técnica sinaliza: "essa pessoa entende o que está fazendo e sabe comunicar". O CI/CD mostra disciplina de engenharia. O `SECURITY.md` é um toque que poucos portfólios têm.

**O que você vai aprender:**
- GitHub Actions para CI/CD com multi-stage (test → build → Docker)
- Como escrever documentação técnica que vende seu trabalho
- Diagramas com Mermaid no GitHub
- Security disclosure como prática profissional

**Resultado esperado:**  
O repositório no GitHub tem um README visualmente limpo com diagrama, badges verdes de CI, e uma seção "Security Decisions" que explica cada escolha. Push no `main` dispara o pipeline automaticamente.

**Commit:** `docs: add README with architecture diagram, security decisions, and CI/CD pipeline`

---

## Resumo das Fases

| Fase | Foco | Entregável |
|------|------|------------|
| 1 | Infraestrutura | Docker Compose + health check |
| 2 | Autenticação | JWT + Spring Security + roles |
| 3 | Rate Limiting | Redis + tiers + headers 429 |
| 4 | Input Validation | Sanitização + error handling |
| 5 | Security Logging | Eventos + endpoint de auditoria |
| 6 | Métricas | Dashboard + agregações |
| 7 | Testes | Unitários + integração + segurança |
| 8 | Documentação | README + CI/CD + SECURITY.md |

---

## Próximo Passo

Quando estiver pronto, peça para executar a **Fase 1**. Cada fase vai seguir o mesmo padrão do `envsafe`: prompt claro, objetivo de aprendizado, implementação incremental, validação com testes, e commit checkpoint.
