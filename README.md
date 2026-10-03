# Budgeting — API financeira com comandos de voz

Projeto do bootcamp DIO **CI&T - Java AI Copilot**, desenvolvido para o desafio **“Desenvolvendo sua API Inteligente com Reconhecimento de Fala e Spring Boot”**. A aplicação recebe comandos de voz para criar ou consultar transações financeiras, integra modelos de IA com funções reais da aplicação e persiste os dados em MySQL.

## Fluxo principal

1. O cliente envia um arquivo de áudio para `POST /transactions/ai`.
2. O `TranscriptionModel` transforma o áudio em texto.
3. O `ChatClient` interpreta o comando com um prompt de assistente financeiro.
4. O Tool Calling disponibiliza os casos de uso de criação e consulta por categoria.
5. Os casos de uso acessam o repositório e a persistência JPA/MySQL.
6. O `TextToSpeechModel` converte a resposta final em MP3, devolvido ao cliente.

As ferramentas executam operações reais no banco. A seleção da ferramenta depende da interpretação do modelo.

## Tecnologias

Versões declaradas nos arquivos do projeto:

| Tecnologia | Versão / configuração |
| --- | --- |
| Java | 25 |
| Spring Boot | 4.0.8 |
| Spring AI | 2.0.0-M4 |
| Gradle Wrapper | 9.7.1 |
| MySQL | Imagem Docker `mysql:9.6` |
| Persistência | Spring Data JPA / Hibernate |
| HTTP | Spring Web |
| Lombok | Plugin 9.2.0 |
| Testes | JUnit Jupiter e AssertJ via Spring Boot Test |
| Chat | OpenAI `gpt-4o-mini` |
| Transcrição | OpenAI `whisper-1`, idioma português |
| Síntese de voz | OpenAI `gpt-4o-mini-tts`, voz `nova`, MP3 |

O projeto utiliza uma versão milestone do Spring AI. Em 02/10/2026, o fluxo completo foi validado localmente com MySQL e chamadas reais à OpenAI: transcrição, Tool Calling para criação e consulta, persistência e resposta em MP3.

## Organização do código

```text
src/main/java/com/dio/budgeting/
├── application/                  # Casos de uso, entradas, saídas e ferramentas
├── domain/                       # Transação, identificador, categorias e contrato de repositório
├── infrastructure/
│   ├── http/                     # Endpoints financeiros e DTOs
│   └── persistence/              # Entidade JPA e adaptador de repositório
├── BudgetingApplication.java     # Inicialização e ChatClient genérico
└── *Controller.java              # Endpoints individuais de chat, transcrição e síntese

src/main/resources/
├── application.properties       # Configuração de IA e JPA
└── prompts/system-message.st    # Prompt do assistente financeiro

src/test/
├── java/com/dio/budgeting/        # Teste de contexto e integrações com OpenAI
└── resources/audio/              # Áudios de exemplo
```

REST e Tool Calling compartilham os casos de uso `PersistTransactionUseCase` e `ListTransactionsByCategoryUseCase`. `AudioOperations` concentra transcrição, interpretação e síntese; o `TransactionController` configura o `ChatClient` financeiro e monta a resposta HTTP.

## Como executar

### Pré-requisitos

- JDK 25 configurado em `JAVA_HOME`.
- Docker com suporte a containers Linux e Docker Compose, com o serviço em execução.
- Acesso à internet para baixar Gradle, dependências e imagem MySQL.
- Uma chave OpenAI com acesso aos modelos configurados. Chamadas reais podem gerar custos.
- Portas `8080` e `3307` disponíveis.

Execute os comandos na raiz do projeto. O Gradle Wrapper acompanha o repositório; não é necessário instalar Gradle separadamente.

### Windows / PowerShell

Configure o JDK e a chave apenas na sessão do terminal, substituindo os valores de exemplo:

```powershell
$env:JAVA_HOME = 'C:\caminho\para\jdk-25'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:OPENAI_API_KEY = 'sua-chave-openai'
java -version
docker compose up -d database
docker compose ps
```

Inicie a aplicação:

```powershell
.\gradlew.bat bootRun
```

O `gradlew.bat` protege os caminhos com aspas e foi validado na pasta com `&` usando Java 25. Como alternativa, o wrapper pode ser executado diretamente:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar .\gradle\wrapper\gradle-wrapper.jar bootRun
```

Essa alternativa ainda exige JDK compatível e acesso à distribuição do Gradle e às dependências.

### Linux / macOS

Com o JDK 25 configurado:

```bash
export OPENAI_API_KEY='sua-chave-openai'
docker compose up -d database
./gradlew bootRun
```

Se necessário, conceda permissão de execução ao script com `chmod +x gradlew`.

### Banco de dados

O Spring Boot possui integração de desenvolvimento com Docker Compose para obter a conexão a partir de `compose.yml`. O MySQL usa:

| Configuração | Valor local |
| --- | --- |
| Host / porta externa | `localhost:3307` |
| Banco | `transaction` |
| Usuário / senha | `app` / `app` |
| Volume | `transaction_data` |

São credenciais de desenvolvimento. O schema é atualizado por `spring.jpa.hibernate.ddl-auto=update`; não há migrações versionadas. Para encerrar os containers preservando os dados, use `docker compose down`.

A API usa a porta padrão `8080`. A chave OpenAI é necessária para configurar os modelos na inicialização, mesmo para testar os endpoints financeiros REST.

## Endpoints

| Método | Rota | Entrada | Saída |
| --- | --- | --- | --- |
| POST | `/transactions` | JSON com `description`, `category` e `amount` | Transação criada, HTTP 201 |
| GET | `/transactions/{category}` | Categoria no caminho | Lista de transações |
| POST | `/transactions/ai` | Multipart com campo `file` | Resposta MP3 |
| POST | `/api/transcribe` | Multipart com campo `file` | Texto transcrito |
| POST | `/api/sinthesize` | JSON com `text` | Áudio MP3 |
| GET | `/api/chat?prompt=...` | Texto no parâmetro `prompt` | Resposta textual via ChatClient |
| GET | `/api/chat-model?prompt=...` | Texto no parâmetro `prompt` | Resposta textual via ChatModel |

`/api/sinthesize` é a grafia existente no código. Os endpoints genéricos de chat não registram as ferramentas financeiras; elas são configuradas em `/transactions/ai`.

Categorias disponíveis: `GROCERIES` (mercado), `PHARMA` (farmácia) e `AUTO` (automóvel). O campo `amount` representa um inteiro em **centavos**: `8000` corresponde a R$ 80,00.

## Como testar manualmente

### Criar e consultar pelo REST

No PowerShell:

```powershell
$body = @{
    description = 'Compra no mercado'
    category = 'GROCERIES'
    amount = 8000
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/transactions' `
    -ContentType 'application/json' -Body $body

Invoke-RestMethod -Uri 'http://localhost:8080/transactions/GROCERIES'
```

Confirme que a criação retorna HTTP 201 e que o registro aparece na consulta. Reinicie a aplicação e consulte novamente para verificar a persistência.

A resposta converte centavos para reais com `BigDecimal`: `amount: 8000` resulta em `value: 80.00`. O campo `id` retorna apenas o UUID. A persistência mantém os valores originais em centavos; não é necessária migração de dados para esta correção.

### Testar o fluxo de voz

Grave um comando como “Gastei 80 reais no mercado” e envie o arquivo. Para usar o áudio de exemplo no Windows:

```powershell
curl.exe --fail-with-body -X POST 'http://localhost:8080/transactions/ai' `
    -F 'file=@src/test/resources/audio/recording-1.m4a' `
    --output resposta.mp3
```

No Linux/macOS, use `curl` e a continuação de linha `\`:

```bash
curl --fail-with-body -X POST http://localhost:8080/transactions/ai \
  -F 'file=@src/test/resources/audio/recording-1.m4a' \
  --output resposta.mp3
```

Após uma resposta HTTP bem-sucedida, reproduza `resposta.mp3` e consulte `/transactions/GROCERIES` para verificar a operação no banco. Para consultar por voz, grave outro áudio, como “Quais são meus gastos de mercado?”, e envie para a mesma rota. Se houver erro HTTP, o arquivo de saída pode conter uma resposta de erro em vez de áudio.

O fluxo foi validado com `recording-1.m4a`: a API transcreveu o comando, criou uma transação PHARMA de R$ 80,00, confirmou a persistência e devolveu MP3. Uma consulta por voz listou os gastos de farmácia sem criar nova transação. Não reenvie automaticamente um comando de criação após falha de síntese: a transação pode já ter sido salva, e ainda não há idempotência.

## Testes automatizados

Para executar somente os testes locais, sem banco ou chamadas à OpenAI:

```powershell
.\gradlew.bat test --tests com.dio.budgeting.TransactionOutputTest --tests com.dio.budgeting.InputValidationTest
```

Para executar todos os testes, incluindo integrações habilitadas no ambiente:

```powershell
.\gradlew.bat test
```

No caminho Windows com `&`, utilize:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar .\gradle\wrapper\gradle-wrapper.jar test
```

No Linux/macOS: `./gradlew test`. Os relatórios ficam em `build/reports/tests/test/index.html`.

Os testes atuais incluem carregamento do contexto, chat, transcrição dos áudios de exemplo, geração de fala e Tool Calling com operações matemáticas. Os testes de integração com OpenAI são condicionados à presença de `OPENAI_API_KEY`; com a chave definida, podem fazer chamadas reais e gerar custos. O teste de contexto não possui essa condição e depende da configuração dos modelos e do banco.

Há testes locais de valores monetários, UUID, validações, respostas de erro HTTP e falha de síntese após uma operação simulada. Eles usam mocks dos modelos, sem chamadas externas. Em 02/10/2026, `InputValidationTest` e `TransactionOutputTest` totalizaram 14 testes sem falhas. A seleção real das ferramentas financeiras também foi validada manualmente: criação por voz, consulta por categoria e resposta em MP3. O teste matemático de Tool Calling continua sendo apenas uma verificação isolada da infraestrutura.

## Validações e erros

- Transações exigem descrição não vazia com até 255 caracteres, categoria válida e valor positivo em centavos. As regras são aplicadas no caso de uso, tanto para REST quanto para ferramentas de IA.
- O áudio deve ser não vazio e ter até 10 MB. Extensões aceitas: mp3, mp4, mpeg, mpga, m4a, wav e webm. A extensão não comprova o conteúdo; a decodificação é feita pelo provedor.
- Requisições inválidas retornam HTTP 400; métodos não suportados, 405; respostas incompatíveis com `Accept`, 406; arquivos muito grandes, 413; formatos não suportados, 415; falhas de transcrição, interpretação ou síntese, 502.
- Erros retornam JSON com `status`, `code`, `message` e `transactionMayHaveBeenSaved`, inclusive nos endpoints que retornam áudio quando bem-sucedidos.

Exemplo de erro de validação:

```json
{"status":400,"code":"INVALID_INPUT","message":"O valor deve ser positivo e informado em centavos.","transactionMayHaveBeenSaved":false}
```

Se houver falha depois de iniciar a interpretação ou na geração de voz, `transactionMayHaveBeenSaved` será `true`. Isso indica possibilidade, não confirmação de persistência. Consulte as transações antes de reenviar: não há retry automático nem idempotência nesta etapa.

## Melhoria implementada: Validação e Padronização de Erros

A melhoria escolhida para o desafio foi tornar as entradas e falhas da API previsíveis e seguras. O projeto-base permitia que alguns dados inválidos chegassem às integrações ou fossem convertidos de forma silenciosa, enquanto exceções diferentes podiam terminar em uma resposta genérica HTTP 500.

| Problema observado | Comportamento implementado |
| --- | --- |
| Descrição vazia, categoria ausente ou valor não positivo | HTTP 400 antes da persistência |
| Valor fracionário em um campo de centavos, como `12.34` | HTTP 400, sem truncar para `12` e sem salvar |
| Prompt ou texto para síntese vazio | HTTP 400 antes de chamar o modelo |
| Áudio vazio, maior que 10 MB ou com extensão não aceita | HTTP 400, 413 ou 415 antes de chamar o provedor |
| Método HTTP incorreto | HTTP 405 com cabeçalho `Allow`, em vez de 500 |
| Formato de resposta incompatível com o cabeçalho `Accept` | HTTP 406 |
| Falha de transcrição, interpretação ou síntese | HTTP 502 com código estável e mensagem controlada |

As regras financeiras ficam no caso de uso compartilhado por REST e Tool Calling. A validação específica de áudio fica em `AudioValidation`, a orquestração em `AudioOperations` e a conversão das exceções para respostas HTTP em `ApiExceptionHandler`.

Todas as respostas de erro usam o mesmo contrato:

```json
{
  "status": 400,
  "code": "INVALID_INPUT",
  "message": "O texto para síntese é obrigatório.",
  "transactionMayHaveBeenSaved": false
}
```

O campo `transactionMayHaveBeenSaved` trata uma particularidade do fluxo com Tool Calling. Se a interpretação ou a síntese falhar depois que uma ferramenta pode ter criado a transação, a API retorna `true` e orienta consultar os registros antes de reenviar o comando. Isso reduz o risco de duplicar um gasto; não há retry automático.

### Evidências da melhoria

- `InputValidationTest` e `TransactionOutputTest`: 14 testes locais, sem falhas, erros ou skips.
- Revalidação real após reiniciar a aplicação: texto e prompts vazios retornaram 400; métodos incorretos, 405; valor fracionário, 400 sem persistência; valor inteiro válido continuou retornando 201.
- Fluxo completo validado com OpenAI e MySQL: criação por voz, consulta por voz sem gravação indevida e resposta em MP3.
- Resultados detalhados em [Relatório de testes manuais](manual-test-results/2026-10-02/RELATORIO.md).

Auditoria com data/hora e canal de origem permanece como evolução opcional, assim como autenticação, armazenamento de áudio, integrações externas e MCP Server.

## Pendências conhecidas

- Revisar a separação entre orquestração de voz, configuração do ChatClient e adaptadores de ferramentas. O tratamento das operações de áudio foi extraído para `AudioOperations` durante a implementação das validações.
- Centralizar também a configuração do `ChatClient` financeiro fora do controller e revisar os nomes legados `TrasactionRequest` e `/sinthesize` sem quebrar compatibilidade.

Na análise inicial, o Java ativo era 11, a chave estava ausente, o download do Gradle foi bloqueado por restrição de rede e o acesso ao Docker foi negado. Essas são condições daquele ambiente de análise, não requisitos do projeto.

## Aprendizado durante o desafio

O principal aprendizado foi que integrar um modelo não encerra o trabalho no retorno da IA: é necessário validar o contrato HTTP, a unidade monetária, a persistência e o efeito real das ferramentas. Um valor recebido como decimal em um campo de centavos chegou a ser convertido silenciosamente; a correção passou a exigir um inteiro exato antes do caso de uso.

Também ficou clara a diferença entre falhar antes e depois de uma ferramenta executar. Uma falha de síntese pode acontecer depois que a transação já foi salva, por isso a API informa essa possibilidade e orienta consultar antes de reenviar. Os testes combinaram mocks para cenários determinísticos com validação real de transcrição, Tool Calling, MySQL e áudio.

## Referências e entrega

- [Bootcamp CI&T - Java AI Copilot](https://www.dio.me/bootcamp/ci-t-java-ai-copilot)
- [Trilha Spring Boot da DIO](https://github.com/digitalinnovationone/dio-spring-boot-learning-track)
- [Projeto base Spring AI — Budgeting](https://github.com/digitalinnovationone/dio-spring-boot-learning-track/blob/main/05-spring-ai/README.md)

Este repositório é a minha entrega para o desafio. Escolhi melhorar as validações e o tratamento de erros porque, durante os testes, encontrei requisições inválidas retornando HTTP 500 e um valor decimal sendo truncado antes de ser salvo. Corrigi esses comportamentos, repeti os testes e registrei os resultados neste README e no relatório de testes manuais. A chave da OpenAI fica somente na variável de ambiente `OPENAI_API_KEY`.
