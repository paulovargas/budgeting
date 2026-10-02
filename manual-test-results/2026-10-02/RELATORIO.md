# Testes manuais — budgeting — 02/10/2026

## Atualização após as correções — 02/10/2026

As falhas descritas na execução original abaixo foram corrigidas no código:

- Método HTTP não suportado: resposta 405 `METHOD_NOT_ALLOWED`, preservando `Allow` e com `transactionMayHaveBeenSaved: false`.
- Valor fracionário: o DTO REST recebe `BigDecimal` e usa `longValueExact()` antes do caso de uso. Frações de centavo e valores fora do intervalo de long retornam 400, sem persistência. Valores inteiros continuam aceitos; `1234` retorna R$ 12,34.
- Prompt: `/api/chat` e `/api/chat-model` rejeitam ausência, vazio e espaços com 400 `INVALID_INPUT` antes de chamar os modelos.

Validação **automatizada local**, distinta da execução manual original: `InputValidationTest` e `TransactionOutputTest`, 14 testes, zero falhas/erros/skips. Gradle `BUILD SUCCESSFUL` usando JDK 25. Inclui MockMvc com os controladores reais e modelos/repositório simulados: 405 + Allow em ambas as rotas, frações/overflow/null sem salvar, valor inteiro preservado, prompts inválidos sem chamada ao modelo e prompt válido encaminhado. `git diff --check` sem erros.

A repetição manual no servidor após reinício está **pendente**: o Computer Use não conseguiu conectar ao canal nativo do Windows nesta etapa. Não houve reinício da aplicação, chamada real à OpenAI nem criação de novos registros nesta etapa de correção. As respostas e falhas na matriz abaixo pertencem à versão anterior e foram preservadas como evidência histórica.

Nova verificação às 20:07 (UTC−03): porta 8080 ainda atendida pelo processo 1176. GET `/api/sinthesize` continuou retornando 500 `INTERNAL_ERROR`, indicando que o servidor ainda não carregou a correção de 405. POST na mesma rota com `{"text":""}` e `Accept: audio/mp3` retornou 400 `INVALID_INPUT`, JSON, `transactionMayHaveBeenSaved: false`. Evidência local em `before-restart/http-results.json`. O teste de valor fracionário não foi reenviado nessa versão para evitar criar outro registro truncado. Reinício no IntelliJ continua pendente; o canal nativo do Computer Use permaneceu indisponível.

### Revalidação após reinício — 02/10/2026, 20:14 (UTC−03)

A aplicação reiniciada passou nos casos corrigidos, por HTTP real em `localhost:8080`:

- GET `/api/sinthesize`: 405 `METHOD_NOT_ALLOWED`, `Allow: POST`, flag false.
- GET `/transactions`: 405 `METHOD_NOT_ALLOWED`, `Allow: POST`, flag false.
- POST `/api/sinthesize` com `{"text":""}`: 400 `INVALID_INPUT`, flag false.
- GET `/api/chat?prompt=` e `/api/chat-model?prompt=`: 400 `INVALID_INPUT`, flag false.
- POST `/transactions` com `amount: 12.34`: 400 `INVALID_INPUT`; consulta subsequente confirmou que a descrição `NAO-DEVE-SALVAR-20261002` não foi persistida.
- Controle positivo com `amount: 1234`: 201, valor R$ 12,34; persistência confirmada em AUTO, UUID `6a4dfe0f-7fb9-475f-b662-61fa9e7ff587`, descrição `VALIDO-RETESTE-20261002`.

Horários: America/Sao_Paulo (UTC−03:00). Aplicação já iniciada pelo usuário no IntelliJ, em http://localhost:8080. Não houve alteração do código da aplicação nesta sessão. As alterações locais preexistentes foram preservadas.

## Execução e evidências

- Computer Use disponível por `node_repl` + `@oai/sky`. IntelliJ, Postman e Docker Desktop encontrados abertos. IntelliJ mostrou `Started BudgetingApplication`; socket 8080 em LISTEN pelo processo 1176.
- No Postman, o estado inicial observado tinha método personalizado `GETAD`, corpo `{"text":""}`, Content-Type `application/json`, Accept `audio/mp3` e resposta anterior 500. Não foi reenviado GETAD. O GET padrão foi testado separadamente por HTTP direto.
- POST `/api/sinthesize` executado no Postman com `{"text":"Teste de síntese de voz"}`: **200 OK**, **13,54 s**, **42,97 KB** exibidos pelo Postman e player com **2 s**. O áudio não foi ouvido pelo agente. A resposta ficou aberta no Postman.
- Demais casos executados com requisições HTTP reais, dirigidas por scripts Python/urllib ao servidor em execução. Sem mocks, sem reiniciar o servidor e sem executar a suíte automatizada Gradle. Scripts são evidências/reprodução dos testes manuais; executá-los novamente cria novos registros e faz chamadas ao provedor.
- `75` requisições registradas em `http-results.json`, além do POST do Postman: `71` com status esperado e `4` divergências. Essas contagens medem status HTTP; conteúdo e persistência foram verificados nos casos indicados abaixo.
- Respostas, horários, duração, cabeçalhos e entradas JSON estão em `http-results.json`. Áudios gerados foram salvos nesta pasta. Nas últimas requisições também foram preservados os bytes em Base64. Uma sondagem confirmou UTF-8 válido; caracteres estranhos no terminal PowerShell não foram tratados como defeito da API.

## Resultados funcionais confirmados

1. TTS: POST válido no Postman e outra síntese via HTTP direto retornaram 200. O segundo MP3 foi enviado à transcrição e retornou a frase de consulta solicitada.
2. Chat: GET `/api/chat?prompt=Responda%20apenas%20OK` e `/api/chat-model` com o mesmo parâmetro retornaram 200 e texto `OK`.
3. Transcrição: `recording-1.m4a` retornou 200 com “Gastei na farmácia rapidinho e deixei 80 reais em 3 itens.”. Arquivo `invalid.wav` com texto em vez de áudio retornou 502 `TRANSCRIPTION_FAILED`, sem indicação de persistência.
4. REST: criação de uma transação em cada categoria GROCERIES, PHARMA e AUTO retornou 201. Entrada `amount: 1234` retornou `value: 12.34`. As três respostas, incluindo UUID, descrição, categoria e valor, foram encontradas integralmente nas consultas subsequentes.
5. Voz/criação: POST `/transactions/ai` com `recording-1.m4a` retornou 200, `audio/mp3`, attachment `audio.mp3`, **126336 bytes**, em **9.801 s**. Comparação de todas as categorias antes/depois encontrou exatamente uma transação nova PHARMA de R$ 80,00.
6. Voz/consulta: áudio sintético perguntando pelos gastos de farmácia retornou 200, **521472 bytes**, em **11.852 s**. Nenhuma transação nova foi encontrada nas três categorias após a consulta. A resposta MP3 foi retranscrita com sucesso: ela listou os quatro valores PHARMA presentes no banco (12,34; 2,35; 80,00; 80,00). A descrição longa de teste sofreu alteração na retranscrição; não foi exigida fidelidade fonética do identificador.

## Falhas reproduzidas e investigação

### Método HTTP não suportado retorna 500

GET `/api/sinthesize` e GET `/transactions` retornaram 500 `INTERNAL_ERROR`, `transactionMayHaveBeenSaved: true`; esperado 405. A hipótese inicial foi reproduzida. `ApiExceptionHandler` possui captura genérica de Exception e não contém tratamento de `HttpRequestMethodNotSupportedException`. Essa leitura do código explica o resultado; não houve captura de stack trace específico dessa exceção. Sugestão: tratamento explícito de 405, cabeçalho Allow e flag false para requisições rejeitadas antes do controlador. Nenhuma correção aplicada.

### Valor fracionário em centavos é aceito e truncado

POST `/transactions` com `amount: 12.34` retornou 201 e `value: 0.12`, e o registro apareceu em GET GROCERIES. O contrato documenta valor inteiro em centavos; o teste esperava 400. `TrasactionRequest.amount` é `long`, e a validação verifica apenas se é positivo. A conversão de decimal para inteiro ocorre antes do caso de uso; atribuir a configuração exata de coerção do Jackson é uma inferência, pois ela não foi inspecionada em runtime. Sugestão: rejeitar coerção de números fracionários para inteiros e testar o contrato de desserialização. Nenhuma correção aplicada.

### ChatModel aceita prompt vazio

GET `/api/chat-model?prompt=` retornou 200 com “Hello! How can I assist you today?”. `/api/chat?prompt=` retornou 400; ambos sem parâmetro retornaram 400. Os controladores não possuem validação explícita de prompt. O status 400 esperado para texto vazio é um critério de consistência recomendado, e não uma regra desse endpoint descrita no README. Sugestão: validar `null`, vazio e espaços antes da chamada ao modelo. Nenhuma correção aplicada.

## Dados persistidos por esta sessão

Os registros abaixo permaneceram no banco. Não houve limpeza nem reenvio de criação após falha. O prefixo é `MANUAL-20261002-194747`; o registro de voz usa a descrição extraída pelo modelo.

| Origem | UUID | Categoria | Valor retornado (R$) | Descrição |
|---|---|---|---:|---|
| REST fracionário (falha) | 36de389d-c8dc-4593-8ff1-c415c0feca8e | GROCERIES | 0.12 | MANUAL-20261002-194747 |
| REST válido | 60122366-e05c-4cf4-972b-c4b462918d14 | GROCERIES | 12.34 | MANUAL-20261002-194747-GROCERIES |
| REST válido | 1d70fdd0-7e73-48ff-8f17-dea1b1f99b7c | PHARMA | 12.34 | MANUAL-20261002-194747-PHARMA |
| REST válido | 7dab7aa9-62cb-4c88-a6a5-ac65a08e8398 | AUTO | 12.34 | MANUAL-20261002-194747-AUTO |
| Voz | ee20b8c3-2a0a-47a9-8f42-e9ca8cd81338 | PHARMA | 80.00 | Compras na farmácia. |

## Matriz executada por HTTP direto

Os detalhes completos do corpo e do código de erro estão no JSON. Casos de upload incluem campo ausente/incorreto, arquivo vazio, extensão não suportada, tamanho de 10 MB + 1 byte e Content-Type incorreto, em ambos os endpoints multipart.

| Caso | Método e rota | Esperado | Obtido | Resultado |
|---|---|---:|---:|---|
| baseline-GROCERIES | `GET /transactions/GROCERIES` | 200 | 200 | OK |
| baseline-PHARMA | `GET /transactions/PHARMA` | 200 | 200 | OK |
| baseline-AUTO | `GET /transactions/AUTO` | 200 | 200 | OK |
| tts-get | `GET /api/sinthesize` | 405 | 500 | DIVERGÊNCIA |
| tts-empty | `POST /api/sinthesize` | 400 | 400 | OK |
| tts-blank | `POST /api/sinthesize` | 400 | 400 | OK |
| tts-missing | `POST /api/sinthesize` | 400 | 400 | OK |
| tts-null-text | `POST /api/sinthesize` | 400 | 400 | OK |
| tts-null-body | `POST /api/sinthesize` | 400 | 400 | OK |
| tts-no-body | `POST /api/sinthesize` | 400 | 400 | OK |
| tts-malformed | `POST /api/sinthesize` | 400 | 400 | OK |
| tts-wrong-media | `POST /api/sinthesize` | 415 | 415 | OK |
| tts-wrong-accept | `POST /api/sinthesize` | 406 | 406 | OK |
| transaction-empty-description | `POST /transactions` | 400 | 400 | OK |
| transaction-blank-description | `POST /transactions` | 400 | 400 | OK |
| transaction-missing-description | `POST /transactions` | 400 | 400 | OK |
| transaction-long-description | `POST /transactions` | 400 | 400 | OK |
| transaction-zero | `POST /transactions` | 400 | 400 | OK |
| transaction-negative | `POST /transactions` | 400 | 400 | OK |
| transaction-null-category | `POST /transactions` | 400 | 400 | OK |
| transaction-invalid-category | `POST /transactions` | 400 | 400 | OK |
| transaction-fractional-amount | `POST /transactions` | 400 | 201 | DIVERGÊNCIA |
| transaction-string-amount | `POST /transactions` | 400 | 400 | OK |
| transaction-missing-description | `POST /transactions` | 400 | 400 | OK |
| transaction-missing-amount | `POST /transactions` | 400 | 400 | OK |
| transaction-missing-category | `POST /transactions` | 400 | 400 | OK |
| transaction-empty-body | `POST /transactions` | 400 | 400 | OK |
| category-invalid | `GET /transactions/OTHER` | 400 | 400 | OK |
| category-lowercase | `GET /transactions/groceries` | 400 | 400 | OK |
| transactions-get | `GET /transactions` | 405 | 500 | DIVERGÊNCIA |
| transaction-wrong-media | `POST /transactions` | 415 | 415 | OK |
| create-GROCERIES | `POST /transactions` | 201 | 201 | OK |
| read-GROCERIES | `GET /transactions/GROCERIES` | 200 | 200 | OK |
| create-PHARMA | `POST /transactions` | 201 | 201 | OK |
| read-PHARMA | `GET /transactions/PHARMA` | 200 | 200 | OK |
| create-AUTO | `POST /transactions` | 201 | 201 | OK |
| read-AUTO | `GET /transactions/AUTO` | 200 | 200 | OK |
| transcribe-missing | `POST /api/transcribe` | 400 | 400 | OK |
| transcribe-empty | `POST /api/transcribe` | 400 | 400 | OK |
| transcribe-unsupported | `POST /api/transcribe` | 415 | 415 | OK |
| transcribe-wrong-field | `POST /api/transcribe` | 400 | 400 | OK |
| transcribe-oversized | `POST /api/transcribe` | 413 | 413 | OK |
| transcribe-wrong-media | `POST /api/transcribe` | 415 | 415 | OK |
| voice-missing | `POST /transactions/ai` | 400 | 400 | OK |
| voice-empty | `POST /transactions/ai` | 400 | 400 | OK |
| voice-unsupported | `POST /transactions/ai` | 415 | 415 | OK |
| voice-wrong-field | `POST /transactions/ai` | 400 | 400 | OK |
| voice-oversized | `POST /transactions/ai` | 413 | 413 | OK |
| voice-wrong-media | `POST /transactions/ai` | 415 | 415 | OK |
| chat-valid | `GET /api/chat?prompt=Responda%20apenas%20OK` | 200 | 200 | OK |
| chat-missing-prompt | `GET /api/chat` | 400 | 400 | OK |
| chat-blank-prompt | `GET /api/chat?prompt=` | 400 | 400 | OK |
| chat-model-valid | `GET /api/chat-model?prompt=Responda%20apenas%20OK` | 200 | 200 | OK |
| chat-model-missing-prompt | `GET /api/chat-model` | 400 | 400 | OK |
| chat-model-blank-prompt | `GET /api/chat-model?prompt=` | 400 | 200 | DIVERGÊNCIA |
| transcribe-invalid-content | `POST /api/transcribe` | 502 | 502 | OK |
| transcribe-valid-fixture | `POST /api/transcribe` | 200 | 200 | OK |
| encoding-error-probe | `POST /api/sinthesize` | 400 | 400 | OK |
| before-voice-GROCERIES | `GET /transactions/GROCERIES` | 200 | 200 | OK |
| before-voice-PHARMA | `GET /transactions/PHARMA` | 200 | 200 | OK |
| before-voice-AUTO | `GET /transactions/AUTO` | 200 | 200 | OK |
| voice-valid-fixture | `POST /transactions/ai` | 200 | 200 | OK |
| after-voice-GROCERIES | `GET /transactions/GROCERIES` | 200 | 200 | OK |
| after-voice-PHARMA | `GET /transactions/PHARMA` | 200 | 200 | OK |
| after-voice-AUTO | `GET /transactions/AUTO` | 200 | 200 | OK |
| tts-query-audio | `POST /api/sinthesize` | 200 | 200 | OK |
| transcribe-query-audio | `POST /api/transcribe` | 200 | 200 | OK |
| before-query-GROCERIES | `GET /transactions/GROCERIES` | 200 | 200 | OK |
| before-query-PHARMA | `GET /transactions/PHARMA` | 200 | 200 | OK |
| before-query-AUTO | `GET /transactions/AUTO` | 200 | 200 | OK |
| voice-query | `POST /transactions/ai` | 200 | 200 | OK |
| after-query-GROCERIES | `GET /transactions/GROCERIES` | 200 | 200 | OK |
| after-query-PHARMA | `GET /transactions/PHARMA` | 200 | 200 | OK |
| after-query-AUTO | `GET /transactions/AUTO` | 200 | 200 | OK |
| transcribe-voice-query-response | `POST /api/transcribe` | 200 | 200 | OK |

## Limites e casos não executados

- Não houve escuta humana nem avaliação subjetiva de pronúncia. A transcrição dos MP3 confirmou que o provedor conseguiu decodificá-los; não substitui escuta.
- Não foram simuladas falhas de banco, indisponibilidade do provedor, credencial inválida, falha de síntese após salvar, concorrência ou idempotência. Os testes automatizados locais preexistentes não foram usados como evidência de execução manual desses cenários.
- Não houve teste de exatamente 10 MB, descrição de exatamente 255 caracteres ou texto TTS muito longo; somente casos representativos e os limites inválidos registrados na matriz.
- A consulta por voz e os valores falados são evidência funcional. A invocação interna exata da ferramenta de consulta não foi comprovada por trace específico.
- Não se conclui que todas as validações possíveis estejam cobertas; todas as rotas identificadas nos controladores tiveram ao menos um caso válido executado.
