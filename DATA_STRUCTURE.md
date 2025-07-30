# 📊 Data Structure - Adventure Journal Mod

Documentação técnica da estrutura de dados utilizada pelo Adventure Journal Mod.

---

## 🗂️ Estrutura de Arquivos

### **Localização dos Dados**
```
saves/
└── [nome_do_mundo]/
    └── adventure_journal/
        ├── [player_uuid_1].json
        ├── [player_uuid_2].json
        └── ...
```

### **Exemplo de Caminho Completo**
```
saves/New World/adventure_journal/f47ac10b-58cc-4372-a567-0e02b2c3d479.json
```

---

## 📋 Estrutura JSON do Jogador

### **PlayerAdventureData**

```json
{
  "lastDeathInfo": "§6=== MORTE ===§r\n\n§cÚltima morte§r\n\n§7Morto por Zombie§r\n\n§aX: 100 Y: 64 Z: -50§r\n§9Overworld§r\n\n§829/07/2025 14:30§r",
  "pointsOfInterest": [
    {
      "name": "Minha Base",
      "description": "Base principal com farms",
      "position": {
        "x": 0,
        "y": 70,
        "z": 0
      },
      "dimension": "minecraft:overworld",
      "timestamp": "29/07/2025 14:25"
    },
    {
      "name": "Mina de Diamante",
      "description": "Rica em diamantes",
      "position": {
        "x": -200,
        "y": 12,
        "z": 150
      },
      "dimension": "minecraft:overworld",
      "timestamp": "29/07/2025 15:10"
    }
  ],
  "isFirstTime": false
}
```

---

## 🏗️ Classes de Dados

### **PlayerAdventureData**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `lastDeathInfo` | String | Informação formatada da última morte do jogador |
| `pointsOfInterest` | List\<PointOfInterest\> | Lista de todos os POIs do jogador |
| `isFirstTime` | boolean | Flag para detectar primeira vez do jogador |

#### **Métodos Principais**
- `addPointOfInterest(name, description, position, dimension)`
- `removePointOfInterest(index)` *(v1.1.0+)*
- `editPointOfInterest(index, newName, newDescription)` *(v1.1.0+)*
- `getPointOfInterest(index)` *(v1.1.0+)*

### **PointOfInterest**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `name` | String | Nome personalizado do POI |
| `description` | String | Descrição detalhada (máx. 35 chars) |
| `position` | SerializableBlockPos | Coordenadas do POI |
| `dimension` | String | ID da dimensão (minecraft:overworld, etc.) |
| `timestamp` | String | Data/hora de criação/edição (dd/MM/yyyy HH:mm) |

#### **Formato de Dimensões**
- `minecraft:overworld` → "Overworld"
- `minecraft:the_nether` → "Nether" 
- `minecraft:the_end` → "End"
- Outros → "Desconhecida"

### **SerializableBlockPos**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `x` | int | Coordenada X |
| `y` | int | Coordenada Y |
| `z` | int | Coordenada Z |

---

## 💾 Processo de Persistência

### **Salvamento Automático**
```java
// Trigger events que causam salvamento:
1. Adição de POI (/poi add)
2. Remoção de POI (/poi remove)         // v1.1.0+
3. Edição de POI (/poi edit)            // v1.1.0+
4. Morte do jogador
5. Primeira conexão do jogador
```

### **Carregamento de Dados**
```java
// Quando os dados são carregados:
1. Primeira conexão do jogador no mundo
2. Acesso aos dados quando não estão em cache
3. Cache em memória para performance
```

### **Gerenciamento de Cache**
```java
// Estrutura em memória:
Map<String, Map<UUID, PlayerAdventureData>> playerDataByWorld

// Limpeza automática:
- Quando servidor para (ServerLifecycleEvents.SERVER_STOPPING)
```

---

## 🔧 Identificação de Mundos

### **WorldID Generation**
```java
String worldId = worldName + "_" + worldPath.hashCode();
```

### **Exemplo**
```java
// Mundo: "New World"
// Path: "C:\Users\...\saves\New World"
// WorldID: "New World_-1234567890"
```

---

## 📖 Formato do Livro

### **Estrutura das Páginas**

#### **Página 1: Introdução**
```
§6=== DIÁRIO DE AVENTURAS ===§r

§9Bem-vindo!§r

Registra automaticamente:
• §cSua última morte§r
• §aPontos de interesse§r

§7Comandos POI:§r
§a/poi add <nome> <desc>§r
§a/poi list [-d]§r
§a/poi info <#>§r
§a/poi edit <#> <nome>§r
§a/poi remove <#>§r
§a/poi nearest§r
```

#### **Página 2: Última Morte**
```
§6=== MORTE ===§r

§c[Causa da morte]§r

§7[Detalhes]§r

§aX: [x] Y: [y] Z: [z]§r
§9[Dimensão]§r

§8[timestamp]§r
```

#### **Páginas 3+: POIs**
```
§6=== POI ===§r

§9[Nome do POI]§r

§7[Descrição]§r

§aX: [x] Y: [y] Z: [z]§r
§9[Dimensão]§r

§8[timestamp]§r
```

---

## 🎨 Sistema de Cores

### **Códigos de Formatação**
| Código | Cor | Uso |
|--------|-----|-----|
| `§6` | Dourado | Títulos e cabeçalhos |
| `§9` | Azul escuro | Nomes e elementos principais |
| `§a` | Verde claro | Coordenadas e confirmações |
| `§7` | Cinza | Texto secundário |
| `§8` | Cinza escuro | Timestamps |
| `§c` | Vermelho | Erros e mortes |
| `§e` | Amarelo | Distâncias e avisos |

---

## ⚡ Performance e Otimizações

### **Cache em Memória**
- Dados mantidos em memória por sessão
- Evita leitura repetitiva de arquivos
- Limpeza automática na parada do servidor

### **Limites de Dados**
- **Descrição POI**: Máximo 35 caracteres
- **Páginas do livro**: Limite 256 caracteres por página
- **Nome de arquivo**: UUID do jogador (garantia de unicidade)

### **Serialização**
```java
// Gson com pretty printing para debug
Gson gson = new GsonBuilder().setPrettyPrinting().create();
```

---

## 🔄 Versionamento de Dados

### **Compatibilidade**
- **v1.0.0 → v1.1.0**: 100% compatível
- Novos campos adicionados sem quebrar estrutura existente
- Migração automática e transparente

### **Evolução da Estrutura**
```json
// v1.0.0
{
  "lastDeathInfo": "...",
  "pointsOfInterest": [...],
  "isFirstTime": false
}

// v1.1.0 (mesma estrutura, novos métodos)
// Nenhuma mudança na estrutura JSON
// Novos métodos de manipulação apenas
```

---

**Esta documentação corresponde à versão 1.1.0 do Adventure Journal Mod**