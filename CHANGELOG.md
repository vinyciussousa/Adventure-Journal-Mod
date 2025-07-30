# 📝 Changelog - Adventure Journal Mod

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

## [v1.1.0] - 2025-07-29

### ✨ **Novas Funcionalidades**
- **Sistema Completo de Gerenciamento de POIs**
  - `/poi remove <índice>` - Remove pontos de interesse por índice
  - `/poi edit <índice> <nome> [descrição]` - Edita nome e descrição de POIs existentes  
  - `/poi info <índice>` - Exibe informações detalhadas de um POI específico
  - `/poi list -d` - Modo detalhado da listagem com distâncias e dimensões
  - `/poi nearest` - Encontra o POI mais próximo na dimensão atual

### 🔧 **Melhorias**
- **Interface Aprimorada**
  - Substituição de azul claro (§b) por azul escuro (§9) para melhor visibilidade
  - Feedback detalhado em todos os comandos POI
  - Mensagens "antes/depois" em edições
  - Indicadores visuais para diferentes dimensões

- **Funcionalidades Avançadas**
  - Cálculo de distância em tempo real para POIs
  - Detecção automática de direção (Norte/Sul/Leste/Oeste)
  - Sugestões de comando de teleporte para POIs na mesma dimensão
  - Timestamp automático atualizado em edições
  - Validação robusta de índices e parâmetros

- **Experiência do Usuário**
  - Comandos com sintaxe flexível (nome obrigatório, descrição opcional)
  - Truncamento automático de descrições longas com aviso
  - Listagem atualizada após remoções
  - Documentação integrada no livro de aventuras

### 🛠️ **Melhorias Técnicas**
- Métodos auxiliares: `removePointOfInterest()`, `getPointOfInterest()`, `editPointOfInterest()`
- Persistência automática em todas as operações
- Atualização em tempo real do livro de aventuras
- Logs detalhados para administradores
- Tratamento de erros aprimorado

---

## [v1.0.0] - 2025-07-24

### 🎉 **Release Inicial**
- **Diário Automático de Aventuras**
  - Livro somente leitura com conteúdo dinâmico
  - Registro automático de mortes com detalhes completos
  - Sistema básico de Pontos de Interesse (POIs)

- **Comandos Básicos**
  - `/poi add <nome> [descrição]` - Adiciona pontos de interesse
  - `/poi list` - Lista todos os POIs registrados

- **Funcionalidades Core**
  - Persistência em arquivos JSON por jogador
  - Separação de dados por mundo
  - Atualização automática do livro
  - Interface colorida e organizada

- **Sistema de Mortes**
  - Captura automática de coordenadas, dimensão e causa
  - Timestamp detalhado de cada morte
  - Página dedicada no diário

---

## 🎯 **Roadmap Futuro**

### Considerações para v1.2.0
- Sistema de waypoints visuais no jogo
- Exportação/importação de POIs
- Comandos de teleporte integrados
- Interface gráfica opcional
- Suporte a imagens/screenshots nos POIs
- Sistema de categorias/tags para POIs

---

**Formato baseado em [Keep a Changelog](https://keepachangelog.com/)**