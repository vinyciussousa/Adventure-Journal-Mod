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
        private String lastDeathInfo = "Nenhuma morte registrada ainda.";
        private List<PointOfInterest> pointsOfInterest = new ArrayList<>();
        
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
                
                return String.format("§6=== POI ===§r\n\n§e%s§r\n\n§7%s§r\n\n§aX: %d Y: %d Z: %d§r\n§b%s§r\n\n§8%s§r", 
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
        System.out.println("Versão: Diário Automático de Aventuras");
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
                                        Text.literal("§e⚠️ Descrição cortada para 35 caracteres.").formatted(Formatting.YELLOW),
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
                            
                            String description = "Local marcado"; // Descrição padrão mais concisa
                            
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
                        }
                        
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
        String timestamp = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        
        BlockPos deathPos = player.getBlockPos();
        String dimension = player.getWorld().getRegistryKey().getValue().toString();
        
        String cause = damageSource.getName();
        if (damageSource.getAttacker() != null) {
            cause = "morto por " + damageSource.getAttacker().getName().getString();
        } else if (damageSource.getSource() != null) {
            cause = "morto por " + damageSource.getSource().getName().getString();
        }
        
        return String.format("§c=== ÚLTIMA MORTE ===§r\n" +
                "Data: %s\n" +
                "Local: %d, %d, %d\n" +
                "Dimensão: %s\n" +
                "Causa: %s\n" +
                "§7Registrado automaticamente§r",
                timestamp, deathPos.getX(), deathPos.getY(), deathPos.getZ(), dimension, cause);
    }
    
    private void giveAdventureBook(ServerPlayerEntity player) {
        LOGGER.info("Dando Diário de Aventuras para: {}", player.getName().getString());
        
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
            player.sendMessage(
                Text.literal("§6✨ Diário de Aventuras atualizado! ✨").formatted(Formatting.GOLD),
                false
            );
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
                "§eBem-vindo!§r\n\n" +
                "Registra automaticamente:\n" +
                "• §cSua última morte§r\n" +
                "• §aPontos de interesse§r\n\n" +
                "§7Comandos:§r\n" +
                "§a/poi add <nome>§r\n" +
                "§a/poi add <nome> <desc>§r\n" +
                "§e/poi list§r §7- lista pontos§r";
        
        pages.add(RawFilteredPair.of(Text.literal(introPage)));
        
        // Página 2: Última morte
        pages.add(RawFilteredPair.of(Text.literal(data.getLastDeathInfo())));
        
        // Páginas 3+: Pontos de interesse (uma página por POI)
        if (data.getPointsOfInterest().isEmpty()) {
            pages.add(RawFilteredPair.of(Text.literal("§6=== PONTOS DE INTERESSE ===§r\n\n§7Nenhum ponto registrado.§r\n\n§eComandos:§r\n§a/poi add <nome>§r\n§a/poi add <nome> <desc>§r\n\n§7Adicione pontos interessantes em sua aventura!§r")));
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