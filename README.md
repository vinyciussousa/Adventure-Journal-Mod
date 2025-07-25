# Adventure Journal Mod

Um mod do Minecraft que fornece um **Diário de Aventuras** para os jogadores registrarem sua jornada.

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
- **Adição manual**: Jogadores podem adicionar pontos de interesse usando comandos
- **Descrição opcional**: Pode ser adicionado com ou sem descrição
- **Informações detalhadas**:
  - Nome personalizado do local
  - Descrição detalhada (ou padrão se não fornecida)
  - Coordenadas exatas
  - Dimensão
  - Data/hora de registro

### Personalização

#### Autor do Livro
- O autor do diário é automaticamente definido como o nome do jogador
- Cada jogador tem seu próprio diário personalizado

## 🎮 Comandos

### `/poi add <nome>` ou `/poi add <nome> <descrição>`
Adiciona um novo ponto de interesse na sua localização atual. A descrição é opcional.

**Exemplos:**
```
/poi add MinhaBase "Minha base principal com farms e armazenamento"
/poi add Diamantes "Caverna com veio de diamantes encontrada"
/poi add Vila "Vila de NPCs próxima ao spawn"
/poi add Spawn
/poi add Mina
```

### `/poi list`
Lista todos os pontos de interesse registrados no chat.

## 🔧 Como Funciona

1. **Ao entrar no mundo**: O jogador recebe automaticamente o Diário de Aventuras
2. **Morte automática**: Sempre que morrer, as informações da morte são registradas e salvas
3. **Adição de POIs**: Use o comando `/poi add` para marcar locais importantes (dados salvos automaticamente)
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

## 🎯 Casos de Uso

- **Exploração**: Marque locais interessantes durante suas aventuras
- **Mineração**: Registre veios de minérios encontrados
- **Construção**: Documente locais planejados para construções
- **Sobrevivência**: Mantenha registro de suas mortes para aprender e melhorar
- **Multiplayer**: Compartilhe informações de locais com outros jogadores

## 📋 Requisitos

- Minecraft 1.21.8
- Fabric Loader
- Fabric API

## 🚀 Instalação

1. Baixe o arquivo `.jar` do mod
2. Coloque na pasta `mods` do seu perfil Fabric
3. Inicie o jogo
4. O diário será dado automaticamente ao entrar em um mundo

---

**Desenvolvido para Minecraft Fabric 1.21.8**
