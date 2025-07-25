# Exemplo de Estrutura de Dados - Adventure Journal Mod

## 📁 Localização dos Arquivos

Os dados são salvos no diretório do mundo:
```
saves/
├── [Nome do Mundo]/
│   ├── adventure_journal/
│   │   ├── [UUID-do-jogador-1].json
│   │   ├── [UUID-do-jogador-2].json
│   │   └── ...
│   ├── level.dat
│   ├── playerdata/
│   └── ...
```

## 📄 Exemplo de Arquivo JSON

**Arquivo**: `saves/MeuMundo/adventure_journal/f47ac10b-58cc-4372-a567-0e02b2c3d479.json`

```json
{
  "lastDeathInfo": "§c=== ÚLTIMA MORTE ===§r\nData: 24/07/2025 16:30:45\nLocal: 128, 64, -256\nDimensão: minecraft:overworld\nCausa: morto por Zombie\n§7Registrado automaticamente§r",
  "pointsOfInterest": [
    {
      "name": "MinhaBase",
      "description": "Base principal com farms automáticos",
      "position": {
        "x": 100,
        "y": 64,
        "z": -200
      },
      "dimension": "minecraft:overworld",
      "timestamp": "24/07/2025 15:30"
    },
    {
      "name": "Diamantes",
      "description": "Caverna rica em diamantes encontrada",
      "position": {
        "x": -45,
        "y": 12,
        "z": 78
      },
      "dimension": "minecraft:overworld", 
      "timestamp": "24/07/2025 16:15"
    },
    {
      "name": "PortalNether",
      "description": "Portal para o Nether próximo à base",
      "position": {
        "x": 150,
        "y": 65,
        "z": -180
      },
      "dimension": "minecraft:overworld",
      "timestamp": "24/07/2025 16:20"
    }
  ]
}
```

## 🔧 Funcionamento do Sistema

### 1. **Salvamento Automático**
- **Quando**: Após cada morte ou adição de POI
- **Como**: Serialização JSON usando Gson
- **Onde**: Arquivo específico do jogador no mundo

### 2. **Carregamento Automático** 
- **Quando**: Ao entrar no mundo
- **Como**: Deserialização JSON para objetos Java
- **Fallback**: Se arquivo não existe, cria dados vazios

### 3. **Estrutura dos Dados**

#### PlayerAdventureData:
- `lastDeathInfo`: String formatada com informações da última morte
- `pointsOfInterest`: Lista de pontos de interesse

#### PointOfInterest:
- `name`: Nome personalizado do local
- `description`: Descrição detalhada
- `position`: Coordenadas serializáveis (SerializableBlockPos)
- `dimension`: ID da dimensão (overworld, nether, end)
- `timestamp`: Data/hora de criação

#### SerializableBlockPos:
- `x`, `y`, `z`: Coordenadas como inteiros
- Conversível para/de BlockPos do Minecraft

## 🛡️ Vantagens do Sistema

1. **Persistência**: Dados nunca são perdidos
2. **Legibilidade**: JSON é facilmente editável
3. **Backup**: Arquivos podem ser copiados/restaurados
4. **Independência**: Cada jogador tem seus próprios dados
5. **Separação**: Dados isolados por mundo
6. **Performance**: Carregamento rápido apenas dos dados necessários

## 📋 Exemplo de Fluxo

1. **Jogador entra no mundo**
   - Sistema procura arquivo `[UUID].json`
   - Se existe: carrega dados
   - Se não existe: cria dados vazios

2. **Jogador adiciona POI**
   ```
   /poi add MinhaFarm "Farm de trigo automatizada"
   ```
   - POI é adicionado à lista na memória
   - Dados são salvos no arquivo JSON
   - Livro é atualizado com nova informação

3. **Jogador morre**
   - Informações da morte são capturadas
   - `lastDeathInfo` é atualizada na memória
   - Dados são salvos no arquivo JSON
   - Livro é atualizado com nova informação

4. **Jogador sai e entra novamente**
   - Sistema carrega arquivo JSON
   - Dados são restaurados na memória
   - Livro é criado com todas as informações salvas
