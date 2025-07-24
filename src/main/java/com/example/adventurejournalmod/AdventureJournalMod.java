package com.example.adventurejournalmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdventureJournalMod implements ModInitializer {
    public static final String MOD_ID = "starterbookmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // Mapa para armazenar o conteúdo dos Diários por jogador
    private static final Map<UUID, WritableBookContentComponent> savedBookContents = new HashMap<>();
    
    // Mapa para controlar quais jogadores já receberam o Diário
    private static final Map<UUID, Boolean> playersWithBook = new HashMap<>();
    
    @Override
    public void onInitialize() {
        System.out.println("=".repeat(50));
        System.out.println("STARTERBOOKMOD FOI CARREGADO!");
        System.out.println("Implementando funcionalidades...");
        System.out.println("=".repeat(50));
        
        // Para single player - quando jogador se conecta
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            LOGGER.info("Jogador conectou: {}", player.getName().getString());
            
            // Agenda para garantir que o jogador está totalmente carregado
            server.execute(() -> {
                checkAndGiveBook(player, "JOIN");
            });
        });
        
        // Para garantir que funciona em respawn também
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            LOGGER.info("Jogador respawnou: {} (alive: {})", newPlayer.getName().getString(), alive);
            
            // Se o jogador morreu, precisamos dar o Diário de volta
            if (!alive) {
                newPlayer.getServer().execute(() -> {
                    ensureStarterBookExists(newPlayer);
                });
            }
        });
        
        // Monitora periodicamente o conteúdo dos Diários (salva automaticamente)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // A cada 5 segundos (100 ticks)
            if (server.getTicks() % 100 == 0) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    saveBookContentPeriodically(player);
                }
            }
        });
        
        // Salva o conteúdo do Diário ANTES do jogador morrer (muito importante!)
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                LOGGER.info("========== JOGADOR VAI MORRER (ALLOW_DEATH) ==========");
                LOGGER.info("Salvando conteúdo do Diário ANTES da morte: {}", player.getName().getString());
                saveBookContent(player);
                LOGGER.info("======================================================");
            }
            return true; // Permite a morte
        });
        
        // Para garantir que o Diário não seja perdido na morte
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                LOGGER.info("========== COPY_FROM DISPARADO ==========");
                LOGGER.info("Jogador morreu (COPY_FROM): {}", newPlayer.getName().getString());
                // Não precisa salvar aqui, já foi salvo no ALLOW_DEATH
                LOGGER.info("========================================");
            }
        });
        
        LOGGER.info("StarterBookMod carregado com todas as funcionalidades!");
    }
    
    private void checkAndGiveBook(ServerPlayerEntity player, String method) {
        LOGGER.info("[{}] Verificando jogador: {}", method, player.getName().getString());
        
        // Verifica se já demos o Diário para este jogador antes
        Boolean alreadyGiven = playersWithBook.get(player.getUuid());
        if (alreadyGiven != null && alreadyGiven) {
            LOGGER.info("Jogador {} já recebeu o Diário inicial antes", player.getName().getString());
            // Se já recebeu antes, só restaura o conteúdo se não tiver Diário
            if (!hasAnyBook(player)) {
                LOGGER.info("Diário perdido, restaurando para {}", player.getName().getString());
                giveStarterBook(player, method);
                restoreBookContent(player);
            }
            return;
        }
        
        // Verifica se o jogador já tem um Diário (qualquer Diário)
        if (hasAnyBook(player)) {
            LOGGER.info("Jogador {} já possui um Diário", player.getName().getString());
            // Marca que já tem Diário
            playersWithBook.put(player.getUuid(), true);
            return;
        }
        
        // Se é um inventário vazio (primeiro spawn) ou não tem Diário
        boolean isEmpty = player.getInventory().isEmpty();
        LOGGER.info("Inventário está vazio: {}", isEmpty);
        
        if (isEmpty || method.equals("JOIN")) {
            LOGGER.info("Dando Diário para {} via método {}", player.getName().getString(), method);
            giveStarterBook(player, method);
            // Marca que já deu o Diário
            playersWithBook.put(player.getUuid(), true);
        }
    }
    
    private void ensureStarterBookExists(ServerPlayerEntity player) {
        LOGGER.info("Garantindo que {} tenha o Diário após morte", player.getName().getString());
        
        if (!hasAnyBook(player)) {
            LOGGER.info("Diário perdido na morte, restaurando para {}", player.getName().getString());
            giveStarterBook(player, "DEATH_RESTORE");
        } else {
            LOGGER.info("{} ainda possui um Diário", player.getName().getString());
        }
        
        // SEMPRE tenta restaurar o conteúdo salvo após morte
        restoreBookContent(player);
    }
    
    private void saveBookContentPeriodically(ServerPlayerEntity player) {
        // Salva silenciosamente sem logs excessivos
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            
            if (stack.isOf(Items.WRITABLE_BOOK)) {
                WritableBookContentComponent content = stack.get(DataComponentTypes.WRITABLE_BOOK_CONTENT);
                if (content != null && !content.pages().isEmpty()) {
                    // Só salva se há conteúdo real
                    boolean hasRealContent = content.pages().stream()
                        .anyMatch(page -> !page.raw().trim().isEmpty());
                    
                    if (hasRealContent) {
                        savedBookContents.put(player.getUuid(), content);
                        // Log apenas uma vez a cada minuto (1200 ticks)
                        if (player.getServer().getTicks() % 1200 == 0) {
                            LOGGER.info("💾 Conteúdo do Diário auto-salvo para {}", player.getName().getString());
                        }
                    }
                }
                break; // Só precisa do primeiro Diário
            }
        }
    }
    
    private void saveBookContent(ServerPlayerEntity player) {
        LOGGER.info(">>> Iniciando saveBookContent para {}", player.getName().getString());
        LOGGER.info("Tamanho do inventário: {}", player.getInventory().size());
        
        // Procura pelo Diário e salva seu conteúdo
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            LOGGER.info("Slot {}: {} (quantidade: {})", i, stack.getItem().toString(), stack.getCount());
            
            if (stack.isOf(Items.WRITABLE_BOOK)) {
                LOGGER.info(">>> Diário ENCONTRADO no slot {}", i);
                
                WritableBookContentComponent content = stack.get(DataComponentTypes.WRITABLE_BOOK_CONTENT);
                if (content != null) {
                    List<RawFilteredPair<String>> pages = content.pages();
                    LOGGER.info("Conteúdo encontrado! Páginas: {}", pages.size());
                    
                    for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
                        String pageContent = pages.get(pageIndex).raw();
                        LOGGER.info("Página {}: '{}'", pageIndex, pageContent);
                    }
                    
                    savedBookContents.put(player.getUuid(), content);
                    LOGGER.info("✅ CONTEÚDO SALVO COM SUCESSO para {}", player.getName().getString());
                    return;
                } else {
                    LOGGER.info("Diário encontrado mas sem conteúdo (WritableBookContentComponent é null)");
                }
            }
        }
        LOGGER.info("❌ Nenhum Diário encontrado no inventário");
    }
    
    private void restoreBookContent(ServerPlayerEntity player) {
        LOGGER.info(">>> Iniciando restoreBookContent para {}", player.getName().getString());
        
        WritableBookContentComponent savedContent = savedBookContents.get(player.getUuid());
        if (savedContent == null) {
            LOGGER.info("❌ Nenhum conteúdo salvo encontrado para {}", player.getName().getString());
            return;
        }
        
        LOGGER.info("✅ Conteúdo salvo encontrado! Páginas: {}", savedContent.pages().size());
        
        // Log do conteúdo que será restaurado
        for (int pageIndex = 0; pageIndex < savedContent.pages().size(); pageIndex++) {
            String pageContent = savedContent.pages().get(pageIndex).raw();
            LOGGER.info("Restaurando página {}: '{}'", pageIndex, pageContent);
        }
        
        // Procura pelo Diário e restaura o conteúdo
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.WRITABLE_BOOK)) {
                LOGGER.info(">>> Restaurando conteúdo no Diário do slot {}", i);
                
                stack.set(DataComponentTypes.WRITABLE_BOOK_CONTENT, savedContent);
                LOGGER.info("✅ CONTEÚDO RESTAURADO COM SUCESSO para {}", player.getName().getString());
                
                player.sendMessage(
                    Text.literal("§a✅ O conteúdo do seu diário foi restaurado!").formatted(Formatting.GREEN), 
                    false
                );
                
                // NÃO remove o conteúdo salvo - mantém para futuras mortes
                return;
            }
        }
        LOGGER.info("❌ Nenhum Diário encontrado para restaurar o conteúdo");
    }
    
    private boolean hasAnyBook(ServerPlayerEntity player) {
        // Simplesmente verifica se o jogador tem qualquer Diário
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.WRITABLE_BOOK) || stack.isOf(Items.WRITTEN_BOOK)) {
                return true;
            }
        }
        return false;
    }
    
    private void giveStarterBook(ServerPlayerEntity player, String method) {
        LOGGER.info("==========================================");
        LOGGER.info("DANDO Diário PARA: {}", player.getName().getString());
        LOGGER.info("MÉTODO: {}", method);
        LOGGER.info("==========================================");
        
        // Cria um Book and Quill especial
        ItemStack bookAndQuill = new ItemStack(Items.WRITABLE_BOOK);
        
        // Marca o Diário como especial (apenas para identificação nos logs)
        NbtCompound customNbt = new NbtCompound();
        customNbt.putString("starterbookmod_origin", method);
        customNbt.putLong("starterbookmod_created", System.currentTimeMillis());
        
        bookAndQuill.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customNbt));
        
        // Define um nome customizado para o Diário
        bookAndQuill.set(DataComponentTypes.CUSTOM_NAME, 
            Text.literal("Diário de Aventuras").formatted(Formatting.GOLD));
        
        LOGGER.info("Diário especial criado: {}", bookAndQuill.getItem().toString());
        
        // Tenta adicionar o Diário ao inventário do jogador
        boolean added = player.getInventory().insertStack(bookAndQuill);
        LOGGER.info("Diário adicionado ao inventário: {}", added);
        
        if (added) {
            // Mensagens diferentes dependendo do método
            if (method.equals("JOIN")) {
                // Mensagens de boas-vindas apenas no primeiro spawn
                player.sendMessage(
                    Text.literal("§6✨ BEM-VINDO AO MUNDO! ✨").formatted(Formatting.GOLD), 
                    false
                );
                
                player.sendMessage(
                    Text.literal("§eVocê recebeu um Diário de Aventuras especial!").formatted(Formatting.YELLOW), 
                    false
                );
                
                player.sendMessage(
                    Text.literal("§a§l[INFO] Use este diário para registrar suas aventuras! O conteúdo será preservado mesmo após a morte.").formatted(Formatting.GREEN), 
                    false
                );
            } else if (method.equals("DEATH_RESTORE")) {
                // Mensagem diferente para respawn após morte
                player.sendMessage(
                    Text.literal("§e📖 Seu Diário de Aventuras foi restaurado!").formatted(Formatting.YELLOW), 
                    false
                );
            }
            
            LOGGER.info("✅ SUCESSO: Diário especial dado para {}", player.getName().getString());
        } else {
            // Se o inventário estiver cheio, dropa no chão
            player.dropItem(bookAndQuill, false);
            LOGGER.info("⚠️ Inventário cheio, Diário dropado no chão");
            
            player.sendMessage(
                Text.literal("§cInventário cheio! Diário de Aventuras dropado no chão.").formatted(Formatting.RED), 
                false
            );
        }
        
        LOGGER.info("==========================================");
    }
}