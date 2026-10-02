# Budgeting — API financeira com comandos de voz

Projeto do desafio DIO **“Desenvolvendo sua API Inteligente com Reconhecimento de Fala e Spring Boot”**. A aplicação recebe comandos de voz para criar ou consultar transações financeiras, integra modelos de IA com funções reais da aplicação e persiste os dados em MySQL.

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

O projeto utiliza uma versão milestone do Spring AI. Esta documentação descreve o código atual; a execução completa ainda precisa ser validada no ambiente de entrega.

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

REST e Tool Calling compartilham os casos de uso `PersistTransactionUseCase` e `ListTransactionsByCategoryUseCase`. Atualmente, a orquestração do fluxo de voz está no `TransactionController`.

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
| Host / porta externa | `localhost:3308` |
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

O fluxo financeiro completo ainda não foi validado nesta análise. Não reenvie automaticamente um comando de criação após falha de síntese: a transação pode já ter sido salva, e ainda não há idempotência.

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

Há testes locais de valores monetários, UUID, validações, respostas de erro HTTP e falha de síntese após uma operação simulada. Eles usam mocks dos modelos, sem chamadas externas. A seleção real de ferramentas pelo modelo e o fluxo financeiro completo ainda precisam de validação. O teste matemático de Tool Calling não comprova criação ou consulta financeira. Há um relatório anterior de TTS aprovado em 01/10/2026; ele comprova apenas aquela execução.

## Validações e erros

- Transações exigem descrição não vazia com até 255 caracteres, categoria válida e valor positivo em centavos. As regras são aplicadas no caso de uso, tanto para REST quanto para ferramentas de IA.
- O áudio deve ser não vazio e ter até 10 MB. Extensões aceitas: mp3, mp4, mpeg, mpga, m4a, wav e webm. A extensão não comprova o conteúdo; a decodificação é feita pelo provedor.
- Requisições inválidas retornam HTTP 400; arquivos muito grandes, 413; formatos não suportados, 415; falhas de transcrição, interpretação ou síntese, 502.
- Erros retornam JSON com `status`, `code`, `message` e `transactionMayHaveBeenSaved`, inclusive nos endpoints que retornam áudio quando bem-sucedidos.

Exemplo de erro de validação:

```json
{"status":400,"code":"INVALID_INPUT","message":"O valor deve ser positivo e informado em centavos.","transactionMayHaveBeenSaved":false}
```

Se houver falha depois de iniciar a interpretação ou na geração de voz, `transactionMayHaveBeenSaved` será `true`. Isso indica possibilidade, não confirmação de persistência. Consulte as transações antes de reenviar: não há retry automático nem idempotência nesta etapa.

## Melhoria para a entrega

Uma melhoria funcional já implementada é a validação compartilhada de entradas e a padronização de erros: transações inválidas são rejeitadas antes da persistência, áudios inválidos não são enviados ao provedor e falhas de geração de voz orientam a consulta antes de reenviar. Há testes locais para esses comportamentos; a demonstração após reiniciar a aplicação ainda está pendente. Esta melhoria pode ser usada na entrega, caso seja a escolhida.

A proposta atual é auditoria mínima: registrar data/hora de criação e canal de origem (`REST` ou `VOICE`) nas transações e exibir esses metadados na consulta. Ela depende de implementação e validação. Validações antes de salvar ou novas consultas financeiras também são alternativas compatíveis com o desafio.

Após escolher e implementar a melhoria, atualizar esta seção com o problema resolvido, o comportamento final, exemplos e os resultados dos testes. Autenticação, storage de áudio, integrações externas e MCP Server são propostas posteriores e opcionais.

## Pendências conhecidas

- Revisar a separação entre orquestração de voz, configuração do ChatClient e adaptadores de ferramentas. O tratamento das operações de áudio foi extraído para `AudioOperations` durante a implementação das validações.
- Testar criação, consulta e resposta em áudio de ponta a ponta.

Na análise inicial, o Java ativo era 11, a chave estava ausente, o download do Gradle foi bloqueado por restrição de rede e o acesso ao Docker foi negado. Essas são condições daquele ambiente de análise, não requisitos do projeto.

## Aprendizado durante o desafio

O projeto permite estudar como conectar reconhecimento de fala, modelos de linguagem e Tool Calling aos casos de uso de uma aplicação com persistência. A análise inicial mostrou a importância de verificar unidades monetárias, contratos das ferramentas e resultados no banco, além de ouvir a resposta gerada.

Ao concluir a implementação, registrar aqui os aprendizados pessoais: por que a melhoria foi escolhida, quais dificuldades surgiram, como foram resolvidas e quais testes demonstram o resultado.

## Referências e entrega

- [Trilha Spring Boot da DIO](https://github.com/digitalinnovationone/dio-spring-boot-learning-track)
- [Projeto base Spring AI — Budgeting](https://github.com/digitalinnovationone/dio-spring-boot-learning-track/blob/main/05-spring-ai/README.md)

Para entregar, disponibilize sua versão em um repositório próprio ou fork no GitHub, com este README atualizado, uma melhoria concluída e evidências dos testes. Revise os arquivos antes de publicar para evitar incluir chaves e outros dados privados.
