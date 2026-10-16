# Adventure Journal Mod

> [!NOTE]
> **Status: descontinuado.** Projeto criado para uso pessoal. Não recebe mais atualizações e foi desenvolvido apenas para o Minecraft 1.21.8.
>
> Desenvolvido com auxílio de inteligência artificial, como experimento para avaliar na prática o desenvolvimento assistido por IA.

Um mod do Minecraft que fornece um **Diário de Aventuras** com sistema completo de gerenciamento de Pontos de Interesse (POIs).

> **English summary:** Minecraft mod built in Java with the Fabric API. It gives each player a read-only journal that is updated automatically with their last death (time, coordinates, dimension and cause) and with points of interest managed through `/poi` commands (add, edit, remove, info, list, nearest), with real-time distance and direction. Data is persisted per world and per player in JSON files. Personal project, built with AI assistance as an experiment in AI-assisted development; currently discontinued.

## 📖 Funcionalidades

### Diário Automático
- **Livro somente leitura**: Os jogadores recebem um livro que não pode ser editado manualmente
- **Preservação após morte**: O diário é automaticamente restaurado após a morte do jogador
- **Conteúdo dinâmico**: O livro é atualizado automaticamente com novas informações

### Funcionalidades do Diário

#### Página 1: Introdução
- Apresentação do mod
- Instruções de uso dos comandos
- Explicação das funcionalidades

#### Página 2: Última Morte
- **Registro automático**: Sempre que o jogador morre, as informações são automaticamente salvas
- **Detalhes inclusos**:
  - Data e hora da morte
  - Coordenadas exatas do local
  - Dimensão onde ocorreu
  - Causa da morte

#### Páginas 3+: Pontos de Interesse
- **Gerenciamento completo**: Adicione, edite, remova e consulte POIs
- **Descrição personalizável**: Pode ser adicionado com ou sem descrição
- **Informações detalhadas**:
  - Nome personalizado do local
  - Descrição detalhada (ou padrão se não fornecida)
  - Coordenadas exatas
  - Dimensão e timestamp
  - Cálculo de distância em tempo real

### Personalização

#### Autor do Livro
- O autor do diário é automaticamente definido como o nome do jogador
- Cada jogador tem seu próprio diário personalizado

## 🎮 Comandos

### **Sistema Básico de POIs**

#### `/poi add <nome>` ou `/poi add <nome> <descrição>`
Adiciona um novo ponto de interesse na sua localização atual. A descrição é opcional.

**Exemplos:**
```
/poi add MinhaBase "Minha base principal com farms e armazenamento"
/poi add Diamantes "Caverna com veio de diamantes encontrada"
/poi add Vila "Vila de NPCs próxima ao spawn"
/poi add Spawn
/poi add Mina
```

#### `/poi list` e `/poi list -d`
Lista todos os pontos de interesse registrados no chat.
- **Básico**: Mostra índice, nome e descrição
- **Detalhado (-d)**: Inclui coordenadas, dimensão, distância e indicadores visuais

### **Sistema Avançado de Gerenciamento (v1.1.0+)**

#### `/poi remove <índice>`
Remove um ponto de interesse específico pelo seu índice.

**Exemplos:**
```
/poi remove 1    # Remove o primeiro POI
/poi remove 3    # Remove o terceiro POI
```

#### `/poi edit <índice> <nome>` ou `/poi edit <índice> <nome> <descrição>`
Edita um ponto de interesse existente.

**Exemplos:**
```
/poi edit 1 "Nova Casa"
/poi edit 2 "Mina Premium" "Agora com redstone também!"
```

#### `/poi info <índice>`
Exibe informações detalhadas sobre um POI específico, incluindo:
- Nome e descrição
- Coordenadas exatas
- Dimensão atual
- Distância do jogador
- Data de criação/edição
- Comando de teleporte (se na mesma dimensão)

**Exemplos:**
```
/poi info 1      # Mostra detalhes do primeiro POI
/poi info 5      # Mostra detalhes do quinto POI
```

#### `/poi nearest`
Encontra e exibe o ponto de interesse mais próximo na dimensão atual, incluindo:
- Distância exata
- Direção aproximada (Norte/Sul/Leste/Oeste)
- Coordenadas
- Informações básicas do POI

## 🔧 Como Funciona

1. **Ao entrar no mundo**: O jogador recebe automaticamente o Diário de Aventuras
2. **Morte automática**: Sempre que morrer, as informações da morte são registradas e salvas
3. **Gerenciamento de POIs**: Use os comandos `/poi` para adicionar, editar, remover e consultar locais importantes
4. **Atualização automática**: O livro é automaticamente atualizado e substituído com as novas informações
5. **Persistência**: Todos os dados são salvos em arquivos JSON no diretório do mundo
6. **Carregamento**: Ao entrar novamente no mundo, os dados são automaticamente carregados

## ⚙️ Detalhes Técnicos

- **Separação por mundo**: Cada mundo mantém seus próprios dados de jogadores
- **Persistência**: Dados salvos em arquivos JSON no diretório `saves/[mundo]/adventure_journal/`
- **Carregamento automático**: Dados são carregados automaticamente ao entrar no mundo
- **Backup automático**: Cada jogador tem seu próprio arquivo (UUID.json)
- **Limpeza automática**: Cache de memória é limpo quando o servidor para
- **Livro escrito**: Usa `WRITTEN_BOOK` para prevenir edição pelos jogadores
- **Cálculos eficientes**: Distância e direção calculados em tempo real
- **Validação robusta**: Todos os comandos validam entradas e fornecem feedback claro

## 🎯 Casos de Uso

### **Exploração Avançada**
- Marque locais interessantes e edite descrições conforme explora mais
- Use `/poi nearest` para encontrar rapidamente o próximo destino
- Remova POIs temporários ou desnecessários para manter organização

### **Construção Profissional**
- Documente projetos com descrições detalhadas
- Edite informações conforme projetos evoluem
- Use `/poi info` para revisar detalhes antes de viajar para o local

### **Mineração Organizada**
- Registre veios encontrados com descrições específicas
- Calcule distâncias para otimizar rotas de mineração
- Mantenha registro atualizado de recursos disponíveis

### **Sobrevivência Estratégica**
- Analise padrões de morte para melhorar gameplay
- Localize rapidamente onde perdeu itens
- Mantenha registro de locais perigosos ou seguros

### **Multiplayer Colaborativo**
- Compartilhe coordenadas facilmente com outros jogadores
- Cada jogador mantém seu próprio sistema organizado
- Performance otimizada para servidores com muitos jogadores

## 📋 Requisitos

- Minecraft 1.21.8
- Fabric Loader
- Fabric API

## 🚀 Instalação

1. Baixe o arquivo `.jar` do mod na página de [Releases](https://github.com/vinyciussousa/Adventure-Journal-Mod/releases)
2. Coloque na pasta `mods` do seu perfil Fabric
3. Inicie o jogo
4. O diário será dado automaticamente ao entrar em um mundo

## 📝 Histórico de Versões

### v1.1.0 (última versão) - POI Management System
- Sistema completo de gerenciamento de POIs
- 6 comandos avançados (`add`, `list`, `remove`, `edit`, `info`, `nearest`)
- Cálculo de distância e direção em tempo real
- Interface aprimorada com melhor visibilidade
- Suporte a modo detalhado na listagem

### v1.0.0 - Release Inicial  
- Diário automático básico
- Registro de mortes
- Comandos básicos de POI (`add`, `list`)
- Sistema de persistência

---

**Desenvolvido para Minecraft Fabric 1.21.8**
