package com.example.adventurejournalmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.ArrayList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AdventureJournalMod implements ModInitializer {
    public static final String MOD_ID = "starterbookmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // Dados por mundo - armazena informações dos jogadores
    private static final Map<String, Map<UUID, PlayerAdventureData>> playerDataByWorld = new HashMap<>();
    
    // Classe para armazenar dados da aventura do jogador
    public static class PlayerAdventureData {
        private String lastDeathInfo = "§6=== MORTE ===§r\n\n§9Você ainda não morreu nesta aventura!§r\n\n§7Continue explorando e enfrentando desafios!";
        private List<PointOfInterest> pointsOfInterest = new ArrayList<>();
        private boolean isFirstTime = true; // Flag para detectar primeira vez
        
        public static class PointOfInterest {
            public String name;
            public String description;
            public SerializableBlockPos position;
            public String dimension;
            public String timestamp;
            
            public PointOfInterest(String name, String description, BlockPos position, String dimension) {
                this.name = name;
                this.description = description;
                this.position = new SerializableBlockPos(position);
                this.dimension = dimension;
                this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            }
            
            // Construtor vazio para Gson
            public PointOfInterest() {}
            
            public String getFormattedInfo() {
                // Layout otimizado para caber dentro do limite de 256 caracteres
                String dimName = dimension.contains("overworld") ? "Overworld" : 
                               dimension.contains("nether") ? "Nether" : 
                               dimension.contains("end") ? "End" : "Desconhecida";
                
                return String.format("§6=== POI ===§r\n\n§9%s§r\n\n§7%s§r\n\n§aX: %d Y: %d Z: %d§r\n§9%s§r\n\n§8%s§r", 
                    name, description, position.x, position.y, position.z, dimName, timestamp);
            }
        }
        
        // Classe para serializar BlockPos
        public static class SerializableBlockPos {
            public int x, y, z;
            
            public SerializableBlockPos(BlockPos pos) {
                this.x = pos.getX();
                this.y = pos.getY();
                this.z = pos.getZ();
            }
            
            // Construtor vazio para Gson
            public SerializableBlockPos() {}
            
            public BlockPos toBlockPos() {
                return new BlockPos(x, y, z);
            }
        }
        
        // Construtor vazio para Gson
        public PlayerAdventureData() {}
        
        public void setLastDeathInfo(String deathInfo) {
            this.lastDeathInfo = deathInfo;
        }
        
        public String getLastDeathInfo() {
            return lastDeathInfo;
        }
        
        public void addPointOfInterest(String name, String description, BlockPos position, String dimension) {
            pointsOfInterest.add(new PointOfInterest(name, description, position, dimension));
        }
        
        public List<PointOfInterest> getPointsOfInterest() {
            return pointsOfInterest;
        }
        
        // Método para remover POI por índice (1-based)
        public boolean removePointOfInterest(int index) {
            if (index < 1 || index > pointsOfInterest.size()) {
                return false; // Índice inválido
            }
            pointsOfInterest.remove(index - 1); // Remove (convertendo para 0-based)
            return true;
        }
        
        // Método para obter POI por índice (1-based) - útil para confirmações
        public PointOfInterest getPointOfInterest(int index) {
            if (index < 1 || index > pointsOfInterest.size()) {
                return null; // Índice inválido
            }
            return pointsOfInterest.get(index - 1); // Retorna (convertendo para 0-based)
        }
        
        // Método para editar POI por índice (1-based)
        public boolean editPointOfInterest(int index, String newName, String newDescription) {
            if (index < 1 || index > pointsOfInterest.size()) {
                return false; // Índice inválido
            }
            
            PointOfInterest poi = pointsOfInterest.get(index - 1);
            
            // Atualiza apenas os campos fornecidos (não nulos)
            if (newName != null && !newName.trim().isEmpty()) {
                poi.name = newName.trim();
            }
            
            if (newDescription != null && !newDescription.trim().isEmpty()) {
                // Limita a descrição a 35 caracteres
                if (newDescription.length() > 35) {
                    newDescription = newDescription.substring(0, 35);
                }
                poi.description = newDescription.trim();
            }
            
            // Atualiza o timestamp para mostrar quando foi editado
            poi.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            
            return true;
        }
        
        public boolean isFirstTime() {
            return isFirstTime;
        }
        
        public void setFirstTime(boolean firstTime) {
            this.isFirstTime = firstTime;
        }
    }
    
    // Métodos auxiliares para gerenciar dados por mundo
    private static String getWorldId(ServerPlayerEntity player) {
        String worldName = player.getServer().getSaveProperties().getLevelName();
        String worldPath = player.getServer().getRunDirectory().resolve("saves").resolve(worldName).toString();
        return worldName + "_" + worldPath.hashCode();
    }
    
    // Método para remover aspas do início e fim de uma string
    private static String removeQuotes(String input) {
        if (input == null || input.length() < 2) {
            return input;
        }
        
        // Remove aspas duplas
        if (input.startsWith("\"") && input.endsWith("\"")) {
            return input.substring(1, input.length() - 1);
        }
        
        // Remove aspas simples
        if (input.startsWith("'") && input.endsWith("'")) {
            return input.substring(1, input.length() - 1);
        }
        
        return input;
    }
    
    private static PlayerAdventureData getPlayerData(ServerPlayerEntity player) {
        String worldId = getWorldId(player);
        Map<UUID, PlayerAdventureData> worldData = playerDataByWorld.computeIfAbsent(worldId, k -> new HashMap<>());
        
        PlayerAdventureData data = worldData.get(player.getUuid());
        if (data == null) {
            // Tenta carregar do arquivo
            data = loadPlayerData(player);
            if (data == null) {
                data = new PlayerAdventureData();
            }
            worldData.put(player.getUuid(), data);
        }
        
        return data;
    }
    
    // Métodos de persistência
    private static Path getDataDirectory(ServerPlayerEntity player) {
        String worldName = player.getServer().getSaveProperties().getLevelName();
        Path worldDir = player.getServer().getRunDirectory().resolve("saves").resolve(worldName);
        Path modDir = worldDir.resolve("adventure_journal");
        
        try {
            Files.createDirectories(modDir);
        } catch (IOException e) {
            LOGGER.error("Erro ao criar diretório de dados: {}", e.getMessage());
        }
        
        return modDir;
    }
    
    private static Path getPlayerDataFile(ServerPlayerEntity player) {
        return getDataDirectory(player).resolve(player.getUuid().toString() + ".json");
    }
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private static void savePlayerData(ServerPlayerEntity player, PlayerAdventureData data) {
        try {
            Path dataFile = getPlayerDataFile(player);
            String json = gson.toJson(data);
            Files.write(dataFile, json.getBytes());
            LOGGER.info("Dados salvos para jogador {}: {}", player.getName().getString(), dataFile);
        } catch (IOException e) {
            LOGGER.error("Erro ao salvar dados do jogador {}: {}", player.getName().getString(), e.getMessage());
        }
    }
    
    private static PlayerAdventureData loadPlayerData(ServerPlayerEntity player) {
        try {
            Path dataFile = getPlayerDataFile(player);
            if (Files.exists(dataFile)) {
                String json = new String(Files.readAllBytes(dataFile));
                PlayerAdventureData data = gson.fromJson(json, PlayerAdventureData.class);
                LOGGER.info("Dados carregados para jogador {}: {}", player.getName().getString(), dataFile);
                return data;
            }
        } catch (IOException e) {
            LOGGER.error("Erro ao carregar dados do jogador {}: {}", player.getName().getString(), e.getMessage());
        }
        return null;
    }
    
    @Override
    public void onInitialize() {
        System.out.println("=".repeat(50));
        System.out.println("ADVENTURE JOURNAL MOD CARREGADO!");
        System.out.println("Versão: v1.1.0 - POI Management System");
        System.out.println("=".repeat(50));
        
        // Registra o comando para adicionar pontos de interesse
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("poi")
                .then(CommandManager.literal("add")
                    .then(CommandManager.argument("name", com.mojang.brigadier.arguments.StringArgumentType.string())
                        // Comando com descrição
                        .then(CommandManager.argument("description", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                String name = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name");
                                String description = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "description");
                                
                                // Remove aspas do início e fim se existirem
                                name = removeQuotes(name);
                                description = removeQuotes(description);
                                
                                // Limita a descrição a 35 caracteres para garantir que a página não ultrapasse o limite
                                if (description.length() > 35) {
                                    description = description.substring(0, 35);
                                    player.sendMessage(
                                        Text.literal("§e🚫 Descrição cortada para 35 caracteres.").formatted(Formatting.YELLOW),
                                        false
                                    );
                                }
                                
                                PlayerAdventureData data = getPlayerData(player);
                                BlockPos pos = player.getBlockPos();
                                String dimension = player.getWorld().getRegistryKey().getValue().toString();
                                
                                data.addPointOfInterest(name, description, pos, dimension);
                                savePlayerData(player, data); // Salva os dados após adicionar POI
                                updatePlayerBook(player);
                                
                                player.sendMessage(
                                    Text.literal("§a✅ Ponto de interesse '§6" + name + "§a' adicionado ao seu diário!").formatted(Formatting.GREEN),
                                    false
                                );
                                
                                LOGGER.info("Jogador {} adicionou ponto de interesse: {} - {}", 
                                    player.getName().getString(), name, description);
                                
                                return 1;
                            })
                        )
                        // Comando apenas com nome (descrição opcional)
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            String name = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name");
                            
                            // Remove aspas do início e fim se existirem
                            name = removeQuotes(name);
                            
                            String description = "==============="; // Descrição padrão mais concisa
                            
                            PlayerAdventureData data = getPlayerData(player);
                            BlockPos pos = player.getBlockPos();
                            String dimension = player.getWorld().getRegistryKey().getValue().toString();
                            
                            data.addPointOfInterest(name, description, pos, dimension);
                            savePlayerData(player, data); // Salva os dados após adicionar POI
                            updatePlayerBook(player);
                            
                            player.sendMessage(
                                Text.literal("§a✅ Ponto de interesse '§6" + name + "§a' adicionado ao seu diário!").formatted(Formatting.GREEN),
                                false
                            );
                            
                            LOGGER.info("Jogador {} adicionou ponto de interesse: {} (sem descrição)", 
                                player.getName().getString(), name);
                            
                            return 1;
                        })
                    )
                )
                .then(CommandManager.literal("list")
                    // Comando list básico
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                        PlayerAdventureData data = getPlayerData(player);
                        
                        if (data.getPointsOfInterest().isEmpty()) {
                            player.sendMessage(
                                Text.literal("§eVocê ainda não tem pontos de interesse registrados.").formatted(Formatting.YELLOW),
                                false
                            );
                        } else {
                            player.sendMessage(
                                Text.literal("§6=== Seus Pontos de Interesse ===").formatted(Formatting.GOLD),
                                false
                            );
                            
                            for (int i = 0; i < data.getPointsOfInterest().size(); i++) {
                                PlayerAdventureData.PointOfInterest poi = data.getPointsOfInterest().get(i);
                                player.sendMessage(
                                    Text.literal(String.format("§a%d. §6%s§r - %s", i + 1, poi.name, poi.description)).formatted(Formatting.WHITE),
                                    false
                                );
                            }
                            
                            player.sendMessage(
                                Text.literal("§7Use §a/poi list -d§7 para modo detalhado ou §a/poi info <#>§7 para mais informações.").formatted(Formatting.GRAY),
                                false
                            );
                        }
                        
                        return 1;
                    })
                    // Comando list detalhado
                    .then(CommandManager.literal("-d")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            PlayerAdventureData data = getPlayerData(player);
                            
                            if (data.getPointsOfInterest().isEmpty()) {
                                player.sendMessage(
                                    Text.literal("§eVocê ainda não tem pontos de interesse registrados.").formatted(Formatting.YELLOW),
                                    false
                                );
                                return 1;
                            }
                            
                            player.sendMessage(
                                Text.literal("§6=== Pontos de Interesse (Detalhado) ===").formatted(Formatting.GOLD),
                                false
                            );
                            
                            BlockPos playerPos = player.getBlockPos();
                            String currentDim = player.getWorld().getRegistryKey().getValue().toString();
                            
                            for (int i = 0; i < data.getPointsOfInterest().size(); i++) {
                                PlayerAdventureData.PointOfInterest poi = data.getPointsOfInterest().get(i);
                                
                                // Calcula distância
                                BlockPos poiPos = poi.position.toBlockPos();
                                double distance = Math.sqrt(playerPos.getSquaredDistance(poiPos));
                                
                                // Nome da dimensão
                                String dimName = poi.dimension.contains("overworld") ? "Overworld" : 
                                               poi.dimension.contains("nether") ? "Nether" : 
                                               poi.dimension.contains("end") ? "End" : "Desconhecida";
                                
                                // Indicador se está na mesma dimensão
                                String dimIndicator = currentDim.equals(poi.dimension) ? "§a✓" : "§c✗";
                                
                                player.sendMessage(
                                    Text.literal("§6" + (i + 1) + ". " + poi.name).formatted(Formatting.WHITE),
                                    false
                                );
                                player.sendMessage(
                                    Text.literal("   §7" + poi.description).formatted(Formatting.GRAY),
                                    false
                                );
                                player.sendMessage(
                                    Text.literal("   §8[" + poi.position.x + ", " + poi.position.y + ", " + poi.position.z + "] " + 
                                                dimIndicator + " " + dimName + " §e(" + String.format("%.1f", distance) + "m)").formatted(Formatting.GRAY),
                                    false
                                );
                                
                                // Adiciona linha separadora se não for o último
                                if (i < data.getPointsOfInterest().size() - 1) {
                                    player.sendMessage(
                                        Text.literal("   §8─────────────────────").formatted(Formatting.DARK_GRAY),
                                        false
                                    );
                                }
                            }
                            
                            player.sendMessage(
                                Text.literal("§7Use §a/poi info <#>§7 para informações completas.").formatted(Formatting.GRAY),
                                false
                            );
                            
                            LOGGER.info("Jogador {} listou POIs em modo detalhado", player.getName().getString());
                            
                            return 1;
                        })
                    )
                )
                .then(CommandManager.literal("remove")
                    .then(CommandManager.argument("index", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            int index = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "index");
                            
                            PlayerAdventureData data = getPlayerData(player);
                            
                            // Verifica se há POIs para remover
                            if (data.getPointsOfInterest().isEmpty()) {
                                player.sendMessage(
                                    Text.literal("§cVocê não tem pontos de interesse para remover.").formatted(Formatting.RED),
                                    false
                                );
                                return 1;
                            }
                            
                            // Verifica se o índice é válido
                            if (index < 1 || index > data.getPointsOfInterest().size()) {
                                player.sendMessage(
                                    Text.literal("§cÍndice inválido! Use um número entre 1 e " + data.getPointsOfInterest().size() + ".").formatted(Formatting.RED),
                                    false
                                );
                                return 1;
                            }
                            
                            // Obtém informações do POI antes de remover (para confirmação)
                            PlayerAdventureData.PointOfInterest poiToRemove = data.getPointOfInterest(index);
                            String poiName = poiToRemove.name;
                            
                            // Remove o POI
                            if (data.removePointOfInterest(index)) {
                                savePlayerData(player, data); // Salva os dados após remoção
                                updatePlayerBook(player); // Atualiza o livro
                                
                                player.sendMessage(
                                    Text.literal("§a✅ Ponto de interesse '§6" + poiName + "§a' removido com sucesso!").formatted(Formatting.GREEN),
                                    false
                                );
                                
                                // Se ainda há POIs, mostra a lista atualizada
                                if (!data.getPointsOfInterest().isEmpty()) {
                                    player.sendMessage(
                                        Text.literal("§7Lista atualizada:").formatted(Formatting.GRAY),
                                        false
                                    );
                                    for (int i = 0; i < data.getPointsOfInterest().size(); i++) {
                                        PlayerAdventureData.PointOfInterest poi = data.getPointsOfInterest().get(i);
                                        player.sendMessage(
                                            Text.literal(String.format("§a%d. §6%s§r", i + 1, poi.name)).formatted(Formatting.WHITE),
                                            false
                                        );
                                    }
                                } else {
                                    player.sendMessage(
                                        Text.literal("§7Sua lista de POIs está agora vazia.").formatted(Formatting.GRAY),
                                        false
                                    );
                                }
                                
                                LOGGER.info("Jogador {} removeu ponto de interesse: {}", 
                                    player.getName().getString(), poiName);
                            } else {
                                player.sendMessage(
                                    Text.literal("§cErro ao remover o ponto de interesse.").formatted(Formatting.RED),
                                    false
                                );
                            }
                            
                            return 1;
                        })
                    )
                )
                .then(CommandManager.literal("edit")
                    .then(CommandManager.argument("index", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                        .then(CommandManager.argument("name", com.mojang.brigadier.arguments.StringArgumentType.string())
                            // Comando completo: /poi edit <índice> <nome> <descrição>
                            .then(CommandManager.argument("description", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    int index = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "index");
                                    String newName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name");
                                    String newDescription = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "description");
                                    
                                    // Remove aspas do início e fim se existirem
                                    newName = removeQuotes(newName);
                                    newDescription = removeQuotes(newDescription);
                                    
                                    PlayerAdventureData data = getPlayerData(player);
                                    
                                    // Verifica se há POIs para editar
                                    if (data.getPointsOfInterest().isEmpty()) {
                                        player.sendMessage(
                                            Text.literal("§cVocê não tem pontos de interesse para editar.").formatted(Formatting.RED),
                                            false
                                        );
                                        return 1;
                                    }
                                    
                                    // Verifica se o índice é válido
                                    if (index < 1 || index > data.getPointsOfInterest().size()) {
                                        player.sendMessage(
                                            Text.literal("§cÍndice inválido! Use um número entre 1 e " + data.getPointsOfInterest().size() + ".").formatted(Formatting.RED),
                                            false
                                        );
                                        return 1;
                                    }
                                    
                                    // Obtém POI antes da edição
                                    PlayerAdventureData.PointOfInterest oldPoi = data.getPointOfInterest(index);
                                    String oldName = oldPoi.name;
                                    String oldDescription = oldPoi.description;
                                    
                                    // Edita o POI
                                    boolean wasDescriptionTruncated = newDescription.length() > 35;
                                    if (data.editPointOfInterest(index, newName, newDescription)) {
                                        savePlayerData(player, data); // Salva os dados após edição
                                        updatePlayerBook(player); // Atualiza o livro
                                        
                                        // Feedback sobre truncamento de descrição
                                        if (wasDescriptionTruncated) {
                                            player.sendMessage(
                                                Text.literal("§e🚫 Descrição cortada para 35 caracteres.").formatted(Formatting.YELLOW),
                                                false
                                            );
                                        }
                                        
                                        player.sendMessage(
                                            Text.literal("§a✅ Ponto de interesse editado com sucesso!").formatted(Formatting.GREEN),
                                            false
                                        );
                                        
                                        // Mostra as mudanças
                                        player.sendMessage(
                                            Text.literal("§7Antes: §6" + oldName + "§7 - " + oldDescription).formatted(Formatting.GRAY),
                                            false
                                        );
                                        
                                        PlayerAdventureData.PointOfInterest updatedPoi = data.getPointOfInterest(index);
                                        player.sendMessage(
                                            Text.literal("§7Agora: §6" + updatedPoi.name + "§7 - " + updatedPoi.description).formatted(Formatting.GRAY),
                                            false
                                        );
                                        
                                        LOGGER.info("Jogador {} editou POI #{}: '{}' para '{}'", 
                                            player.getName().getString(), index, oldName, updatedPoi.name);
                                    } else {
                                        player.sendMessage(
                                            Text.literal("§cErro ao editar o ponto de interesse.").formatted(Formatting.RED),
                                            false
                                        );
                                    }
                                    
                                    return 1;
                                })
                            )
                            // Comando apenas com nome: /poi edit <índice> <nome>
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                int index = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "index");
                                String newName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name");
                                
                                // Remove aspas do início e fim se existirem
                                newName = removeQuotes(newName);
                                
                                PlayerAdventureData data = getPlayerData(player);
                                
                                // Verifica se há POIs para editar
                                if (data.getPointsOfInterest().isEmpty()) {
                                    player.sendMessage(
                                        Text.literal("§cVocê não tem pontos de interesse para editar.").formatted(Formatting.RED),
                                        false
                                    );
                                    return 1;
                                }
                                
                                // Verifica se o índice é válido
                                if (index < 1 || index > data.getPointsOfInterest().size()) {
                                    player.sendMessage(
                                        Text.literal("§cÍndice inválido! Use um número entre 1 e " + data.getPointsOfInterest().size() + ".").formatted(Formatting.RED),
                                        false
                                    );
                                    return 1;
                                }
                                
                                // Obtém POI antes da edição
                                PlayerAdventureData.PointOfInterest oldPoi = data.getPointOfInterest(index);
                                String oldName = oldPoi.name;
                                
                                // Edita apenas o nome (mantém descrição)
                                if (data.editPointOfInterest(index, newName, null)) {
                                    savePlayerData(player, data); // Salva os dados após edição
                                    updatePlayerBook(player); // Atualiza o livro
                                    
                                    player.sendMessage(
                                        Text.literal("§a✅ Nome do ponto de interesse editado com sucesso!").formatted(Formatting.GREEN),
                                        false
                                    );
                                    
                                    // Mostra a mudança
                                    player.sendMessage(
                                        Text.literal("§7Antes: §6" + oldName).formatted(Formatting.GRAY),
                                        false
                                    );
                                    
                                    PlayerAdventureData.PointOfInterest updatedPoi = data.getPointOfInterest(index);
                                    player.sendMessage(
                                        Text.literal("§7Agora: §6" + updatedPoi.name).formatted(Formatting.GRAY),
                                        false
                                    );
                                    
                                    LOGGER.info("Jogador {} editou nome do POI #{}: '{}' para '{}'", 
                                        player.getName().getString(), index, oldName, updatedPoi.name);
                                } else {
                                    player.sendMessage(
                                        Text.literal("§cErro ao editar o ponto de interesse.").formatted(Formatting.RED),
                                        false
                                    );
                                }
                                
                                return 1;
                            })
                        )
                    )
                )
                .then(CommandManager.literal("info")
                    .then(CommandManager.argument("index", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            int index = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "index");
                            
                            PlayerAdventureData data = getPlayerData(player);
                            
                            // Verifica se há POIs
                            if (data.getPointsOfInterest().isEmpty()) {
                                player.sendMessage(
                                    Text.literal("§cVocê não tem pontos de interesse registrados.").formatted(Formatting.RED),
                                    false
                                );
                                return 1;
                            }
                            
                            // Verifica se o índice é válido
                            if (index < 1 || index > data.getPointsOfInterest().size()) {
                                player.sendMessage(
                                    Text.literal("§cÍndice inválido! Use um número entre 1 e " + data.getPointsOfInterest().size() + ".").formatted(Formatting.RED),
                                    false
                                );
                                return 1;
                            }
                            
                            // Obtém o POI
                            PlayerAdventureData.PointOfInterest poi = data.getPointOfInterest(index);
                            
                            // Calcula distância do jogador atual
                            BlockPos playerPos = player.getBlockPos();
                            BlockPos poiPos = poi.position.toBlockPos();
                            double distance = Math.sqrt(playerPos.getSquaredDistance(poiPos));
                            
                            // Nome da dimensão formatado
                            String dimName = poi.dimension.contains("overworld") ? "Overworld" : 
                                           poi.dimension.contains("nether") ? "Nether" : 
                                           poi.dimension.contains("end") ? "End" : "Desconhecida";
                            
                            // Mostra informações detalhadas
                            player.sendMessage(
                                Text.literal("§6=== Informações do POI #" + index + " ===").formatted(Formatting.GOLD),
                                false
                            );
                            player.sendMessage(
                                Text.literal("§9Nome: §f" + poi.name).formatted(Formatting.WHITE),
                                false
                            );
                            player.sendMessage(
                                Text.literal("§9Descrição: §f" + poi.description).formatted(Formatting.WHITE),
                                false
                            );
                            player.sendMessage(
                                Text.literal("§9Coordenadas: §aX: " + poi.position.x + " Y: " + poi.position.y + " Z: " + poi.position.z).formatted(Formatting.WHITE),
                                false
                            );
                            player.sendMessage(
                                Text.literal("§9Dimensão: §9" + dimName).formatted(Formatting.WHITE),
                                false
                            );
                            player.sendMessage(
                                Text.literal("§9Distância: §e" + String.format("%.1f", distance) + " blocos").formatted(Formatting.WHITE),
                                false
                            );
                            
                            // Direção aproximada com direções colaterais
                            int deltaX = poi.position.x - playerPos.getX();
                            int deltaZ = poi.position.z - playerPos.getZ();
                            String direction = "";
                            
                            // Calcula direções colaterais baseado em ambos os eixos
                            if (Math.abs(deltaX) > 15 && Math.abs(deltaZ) > 15) {
                                // Direções colaterais (quando há movimento significativo em ambos os eixos)
                                if (deltaX > 0 && deltaZ > 0) {
                                    direction = "Sudeste";
                                } else if (deltaX > 0 && deltaZ < 0) {
                                    direction = "Nordeste";
                                } else if (deltaX < 0 && deltaZ > 0) {
                                    direction = "Sudoeste";
                                } else {
                                    direction = "Noroeste";
                                }
                            } else {
                                // Direções cardeais (quando há movimento predominante em um eixo)
                                if (Math.abs(deltaX) > Math.abs(deltaZ)) {
                                    direction = deltaX > 0 ? "Leste" : "Oeste";
                                } else {
                                    direction = deltaZ > 0 ? "Sul" : "Norte";
                                }
                            }
                            
                            player.sendMessage(
                                Text.literal("§9Direção: §9" + direction).formatted(Formatting.WHITE),
                                false
                            );
                            player.sendMessage(
                                Text.literal("§9Criado/Editado: §8" + poi.timestamp).formatted(Formatting.WHITE),
                                false
                            );
                            
                            // Comando de teleporte (se estiver na mesma dimensão)
                            String currentDim = player.getWorld().getRegistryKey().getValue().toString();
                            if (currentDim.equals(poi.dimension)) {
                                player.sendMessage(
                                    Text.literal("§7Para teleportar: §a/tp " + poi.position.x + " " + poi.position.y + " " + poi.position.z).formatted(Formatting.GRAY),
                                    false
                                );
                            } else {
                                player.sendMessage(
                                    Text.literal("§7POI está em outra dimensão (" + dimName + ")").formatted(Formatting.GRAY),
                                    false
                                );
                            }
                            
                            LOGGER.info("Jogador {} consultou info do POI #{}: {}", 
                                player.getName().getString(), index, poi.name);
                            
                            return 1;
                        })
                    )
                )
                .then(CommandManager.literal("nearest")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                        PlayerAdventureData data = getPlayerData(player);
                        
                        if (data.getPointsOfInterest().isEmpty()) {
                            player.sendMessage(
                                Text.literal("§cVocê não tem pontos de interesse registrados.").formatted(Formatting.RED),
                                false
                            );
                            return 1;
                        }
                        
                        BlockPos playerPos = player.getBlockPos();
                        String currentDim = player.getWorld().getRegistryKey().getValue().toString();
                        
                        PlayerAdventureData.PointOfInterest nearestPoi = null;
                        double nearestDistance = Double.MAX_VALUE;
                        int nearestIndex = -1;
                        
                        // Encontra o POI mais próximo na mesma dimensão
                        for (int i = 0; i < data.getPointsOfInterest().size(); i++) {
                            PlayerAdventureData.PointOfInterest poi = data.getPointsOfInterest().get(i);
                            
                            // Só considera POIs na mesma dimensão
                            if (currentDim.equals(poi.dimension)) {
                                BlockPos poiPos = poi.position.toBlockPos();
                                double distance = Math.sqrt(playerPos.getSquaredDistance(poiPos));
                                
                                if (distance < nearestDistance) {
                                    nearestDistance = distance;
                                    nearestPoi = poi;
                                    nearestIndex = i + 1; // 1-based index
                                }
                            }
                        }
                        
                        if (nearestPoi == null) {
                            // Nenhum POI na dimensão atual
                            String dimName = currentDim.contains("overworld") ? "Overworld" : 
                                           currentDim.contains("nether") ? "Nether" : 
                                           currentDim.contains("end") ? "End" : "esta dimensão";
                            
                            player.sendMessage(
                                Text.literal("§eNenhum ponto de interesse encontrado em " + dimName + ".").formatted(Formatting.YELLOW),
                                false
                            );
                            
                            // Mostra quantos POIs existem em outras dimensões
                            int totalPois = data.getPointsOfInterest().size();
                            player.sendMessage(
                                Text.literal("§7Você tem " + totalPois + " POI(s) em outras dimensões. Use §a/poi list -d§7 para ver todos.").formatted(Formatting.GRAY),
                                false
                            );
                        } else {
                            // Mostra o POI mais próximo
                            player.sendMessage(
                                Text.literal("§6=== POI Mais Próximo ===").formatted(Formatting.GOLD),
                                false
                            );
                            player.sendMessage(
                                Text.literal("§a#" + nearestIndex + " - §6" + nearestPoi.name).formatted(Formatting.WHITE),
                                false
                            );
                            player.sendMessage(
                                Text.literal("§7" + nearestPoi.description).formatted(Formatting.GRAY),
                                false
                            );
                            player.sendMessage(
                                Text.literal("§9Distância: §e" + String.format("%.1f", nearestDistance) + " blocos").formatted(Formatting.WHITE),
                                false
                            );
                            player.sendMessage(
                                Text.literal("§9Coordenadas: §a[" + nearestPoi.position.x + ", " + nearestPoi.position.y + ", " + nearestPoi.position.z + "]").formatted(Formatting.WHITE),
                                false
                            );
                            
                            // Direção aproximada com direções colaterais
                            int deltaX = nearestPoi.position.x - playerPos.getX();
                            int deltaZ = nearestPoi.position.z - playerPos.getZ();
                            String direction = "";
                            
                            // Calcula direções colaterais baseado em ambos os eixos
                            if (Math.abs(deltaX) > 15 && Math.abs(deltaZ) > 15) {
                                // Direções colaterais (quando há movimento significativo em ambos os eixos)
                                if (deltaX > 0 && deltaZ > 0) {
                                    direction = "Sudeste";
                                } else if (deltaX > 0 && deltaZ < 0) {
                                    direction = "Nordeste";
                                } else if (deltaX < 0 && deltaZ > 0) {
                                    direction = "Sudoeste";
                                } else {
                                    direction = "Noroeste";
                                }
                            } else {
                                // Direções cardeais (quando há movimento predominante em um eixo)
                                if (Math.abs(deltaX) > Math.abs(deltaZ)) {
                                    direction = deltaX > 0 ? "Leste" : "Oeste";
                                } else {
                                    direction = deltaZ > 0 ? "Sul" : "Norte";
                                }
                            }
                            
                            player.sendMessage(
                                Text.literal("§9Direção: §9" + direction).formatted(Formatting.WHITE),
                                false
                            );
                            
                            player.sendMessage(
                                Text.literal("§7Use §a/poi info " + nearestIndex + "§7 para mais detalhes.").formatted(Formatting.GRAY),
                                false
                            );
                        }
                        
                        LOGGER.info("Jogador {} consultou POI mais próximo", player.getName().getString());
                        
                        return 1;
                    })
                )
            );
        });
        
        // Quando jogador se conecta
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            LOGGER.info("Jogador conectou: {}", player.getName().getString());
            
            server.execute(() -> {
                giveAdventureBook(player);
            });
        });
        
        // Quando jogador respawna após morte
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            LOGGER.info("Jogador respawnou: {} (alive: {})", newPlayer.getName().getString(), alive);
            
            if (!alive) {
                newPlayer.getServer().execute(() -> {
                    giveAdventureBook(newPlayer);
                });
            }
        });
        
        // Registra informações de morte
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                LOGGER.info("Jogador {} morreu", player.getName().getString());
                
                PlayerAdventureData data = getPlayerData(player);
                String deathInfo = generateDeathInfo(player, damageSource);
                data.setLastDeathInfo(deathInfo);
                savePlayerData(player, data); // Salva os dados após morte
                
                LOGGER.info("Informação de morte registrada para {}: {}", player.getName().getString(), deathInfo);
            }
            return true;
        });
        
        // Limpa dados quando servidor para
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Servidor parando - limpando dados da memória");
            playerDataByWorld.clear();
            LOGGER.info("Dados limpos!");
        });
        
        LOGGER.info("Adventure Journal Mod carregado com sucesso!");
    }
    
    private String generateDeathInfo(ServerPlayerEntity player, net.minecraft.entity.damage.DamageSource damageSource) {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        
        BlockPos deathPos = player.getBlockPos();
        String dimension = player.getWorld().getRegistryKey().getValue().toString();
        
        // Simplifica o nome da dimensão
        String dimName = dimension.contains("overworld") ? "Overworld" : 
                        dimension.contains("nether") ? "Nether" : 
                        dimension.contains("end") ? "End" : "Desconhecida";
        
        String cause = damageSource.getName();
        if (damageSource.getAttacker() != null) {
            cause = "Morto por " + damageSource.getAttacker().getName().getString();
        } else if (damageSource.getSource() != null) {
            cause = "Morto por " + damageSource.getSource().getName().getString();
        }
        
        // Limita a causa a 35 caracteres para consistência
        if (cause.length() > 35) {
            cause = cause.substring(0, 35);
        }
        
        return String.format("§6=== MORTE ===§r\n\n§c%s§r\n\n§7%s§r\n\n§aX: %d Y: %d Z: %d§r\n§9%s§r\n\n§8%s§r",
                "Última morte", cause, deathPos.getX(), deathPos.getY(), deathPos.getZ(), dimName, timestamp);
    }
    
    private void giveAdventureBook(ServerPlayerEntity player) {
        LOGGER.info("Dando Diário de Aventuras para: {}", player.getName().getString());
        
        PlayerAdventureData data = getPlayerData(player);
        boolean isFirstTime = data.isFirstTime();
        
        // Remove qualquer livro antigo primeiro
        removeOldBooks(player);
        
        // Cria o novo livro
        ItemStack adventureBook = createAdventureBook(player);
        
        boolean added = player.getInventory().insertStack(adventureBook);
        if (!added) {
            player.dropItem(adventureBook, false);
            player.sendMessage(
                Text.literal("§cInventário cheio! Diário de Aventuras dropado no chão.").formatted(Formatting.RED),
                false
            );
        } else {
            if (isFirstTime) {
                // Mensagem de apresentação para primeira vez
                player.sendMessage(
                    Text.literal("§6✨ Bem-vindo ao Adventure Journal! ✨").formatted(Formatting.GOLD),
                    false
                );
                player.sendMessage(
                    Text.literal("§9Seu diário pessoal foi criado e documentará toda sua jornada!").formatted(Formatting.BLUE),
                    false
                );
                player.sendMessage(
                    Text.literal("§7Use §a/poi add <nome>§7 para marcar locais importantes.").formatted(Formatting.GRAY),
                    false
                );
                player.sendMessage(
                    Text.literal("§7Use §a/poi list§7 para listar pontos (§a-d§7 para detalhes).").formatted(Formatting.GRAY),
                    false
                );
                player.sendMessage(
                    Text.literal("§7Use §a/poi info <#>§7 para informações detalhadas.").formatted(Formatting.GRAY),
                    false
                );
                player.sendMessage(
                    Text.literal("§7Use §a/poi edit <#> <nome>§7 para editar pontos.").formatted(Formatting.GRAY),
                    false
                );
                player.sendMessage(
                    Text.literal("§7Use §a/poi remove <#>§7 para remover pontos.").formatted(Formatting.GRAY),
                    false
                );
                player.sendMessage(
                    Text.literal("§7Use §a/poi nearest <#>§7 para encontrar pontos próximos.").formatted(Formatting.GRAY),
                    false
                );
                
                // Marca que não é mais a primeira vez
                data.setFirstTime(false);
                savePlayerData(player, data);
            } else {
                // Mensagem normal para outras ocasiões
                player.sendMessage(
                    Text.literal("§6✨ Diário de Aventuras atualizado! ✨").formatted(Formatting.GOLD),
                    false
                );
            }
        }
    }
    
    private void removeOldBooks(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if ((stack.isOf(Items.WRITTEN_BOOK) || stack.isOf(Items.WRITABLE_BOOK)) && 
                stack.contains(DataComponentTypes.CUSTOM_NAME) && 
                stack.getName().getString().contains("Diário de Aventuras")) {
                player.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }
    }
    
    private void updatePlayerBook(ServerPlayerEntity player) {
        // Remove o livro antigo e dá um novo atualizado
        removeOldBooks(player);
        
        ItemStack updatedBook = createAdventureBook(player);
        boolean added = player.getInventory().insertStack(updatedBook);
        
        if (!added) {
            player.dropItem(updatedBook, false);
        }
    }
    
    private ItemStack createAdventureBook(ServerPlayerEntity player) {
        PlayerAdventureData data = getPlayerData(player);
        
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        
        // Define o nome do livro
        book.set(DataComponentTypes.CUSTOM_NAME, 
            Text.literal("Diário de Aventuras").formatted(Formatting.GOLD));
        
        // Cria as páginas do livro
        List<RawFilteredPair<Text>> pages = new ArrayList<>();
        
        // Página 1: Introdução
        String introPage = "§6=== DIÁRIO DE AVENTURAS ===§r\n\n" +
                "§9Bem-vindo!§r\n\n" +
                "Registra automaticamente:\n" +
                "• §cSua última morte§r\n" +
                "• §aPontos de interesse§r\n\n" +
                "§7Comandos POI:§r\n" +
                "§a/poi add <nome> <desc>§r\n" +
                "§a/poi list [-d]§r\n" +
                "§a/poi info <#>§r\n" +
                "§a/poi edit <#> <nome>§r\n" +
                "§a/poi remove <#>§r\n" +
                "§a/poi nearest§r";
        
        pages.add(RawFilteredPair.of(Text.literal(introPage)));
        
        // Página 2: Última morte
        pages.add(RawFilteredPair.of(Text.literal(data.getLastDeathInfo())));
        
        // Páginas 3+: Pontos de interesse (uma página por POI)
        if (data.getPointsOfInterest().isEmpty()) {
            pages.add(RawFilteredPair.of(Text.literal("§6=== PONTOS DE INTERESSE ===§r\n\n§7Nenhum ponto registrado.§r\n\n§9Comandos:§r\n§a/poi add <nome> <desc>§r\n§a/poi list§r\n§7Adicione pontos interessantes em sua aventura!§r")));
        } else {
            // Adiciona uma página para cada POI
            for (PlayerAdventureData.PointOfInterest poi : data.getPointsOfInterest()) {
                pages.add(RawFilteredPair.of(Text.literal(poi.getFormattedInfo())));
            }
        }
        
        // Adiciona os componentes do livro
        WrittenBookContentComponent content = new WrittenBookContentComponent(
            RawFilteredPair.of("Diário de Aventuras"),  // título
            player.getName().getString(),  // autor (nome do jogador)
            0,  // geração
            pages,  // páginas
            true  // resolved (para permitir formatação)
        );
        
        book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, content);
        
        // Marca como especial
        NbtCompound customNbt = new NbtCompound();
        customNbt.putString("adventure_journal", "automatic");
        customNbt.putLong("created", System.currentTimeMillis());
        book.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customNbt));
        
        return book;
    }
}