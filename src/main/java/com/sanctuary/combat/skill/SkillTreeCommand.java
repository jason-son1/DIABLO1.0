package com.sanctuary.combat.skill;

import com.sanctuary.combat.SanctuaryCombat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 스킬 트리 테스트 명령어
 * /skilltree info - 현재 스킬 트리 상태
 * /skilltree invest <skillId> - 스킬 포인트 투자
 * /skilltree refund <skillId> - 스킬 포인트 회수
 * /skilltree reset - 스킬 트리 초기화
 * /skilltree list - 직업별 스킬 목록
 */
public class SkillTreeCommand implements CommandExecutor {

    private final SanctuaryCombat combatModule;
    private final SkillTreeManager skillTreeManager;

    public SkillTreeCommand(SanctuaryCombat combatModule, SkillTreeManager skillTreeManager) {
        this.combatModule = combatModule;
        this.skillTreeManager = skillTreeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "info" -> showInfo(player);
            case "invest" -> {
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /skilltree invest <skillId>");
                    return true;
                }
                investPoint(player, args[1]);
            }
            case "refund" -> {
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /skilltree refund <skillId>");
                    return true;
                }
                refundPoint(player, args[1]);
            }
            case "reset" -> resetSkillTree(player);
            case "list" -> listSkills(player);
            case "addpoints" -> {
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /skilltree addpoints <amount>");
                    return true;
                }
                addPoints(player, args[1]);
            }
            default -> showHelp(player);
        }

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage("§6===== 스킬 트리 명령어 =====");
        player.sendMessage("§e/skilltree info §7- 현재 상태");
        player.sendMessage("§e/skilltree invest <id> §7- 포인트 투자");
        player.sendMessage("§e/skilltree refund <id> §7- 포인트 회수");
        player.sendMessage("§e/skilltree reset §7- 전체 초기화");
        player.sendMessage("§e/skilltree list §7- 스킬 목록");
        player.sendMessage("§e/skilltree addpoints <n> §7- 포인트 추가 (테스트)");
    }

    private void showInfo(Player player) {
        var entity = combatModule.getCore().getEntityManager().get(player.getUniqueId());
        if (entity == null) {
            player.sendMessage("§c엔티티 정보를 찾을 수 없습니다.");
            return;
        }

        SkillComponent skillComp = entity.getComponent(SkillComponent.class);
        if (skillComp == null) {
            player.sendMessage("§c스킬 컴포넌트가 없습니다.");
            return;
        }

        player.sendMessage("§6===== 스킬 트리 상태 =====");
        player.sendMessage("§e사용 가능 포인트: §f" + skillComp.getAvailablePoints());
        player.sendMessage("§e투자된 포인트: §f" + skillComp.getTotalInvestedPoints());

        var ranks = skillComp.getAllSkillRanks();
        if (ranks.isEmpty()) {
            player.sendMessage("§7투자된 스킬이 없습니다.");
        } else {
            player.sendMessage("§e투자된 스킬:");
            ranks.forEach((id, rank) -> {
                SkillData skill = skillTreeManager.getSkillData(id);
                String name = skill != null ? skill.getName() : id;
                player.sendMessage("  §f" + name + " §7Rank " + rank);
            });
        }
    }

    private void investPoint(Player player, String skillId) {
        var entity = combatModule.getCore().getEntityManager().get(player.getUniqueId());
        if (entity == null) {
            player.sendMessage("§c엔티티 정보를 찾을 수 없습니다.");
            return;
        }

        var result = skillTreeManager.investPoint(entity, skillId.toUpperCase());
        switch (result) {
            case SUCCESS -> player.sendMessage("§a스킬 포인트 투자 성공: " + skillId);
            case FAIL_REQUIREMENTS -> player.sendMessage("§c조건을 충족하지 않습니다.");
            default -> player.sendMessage("§c투자 실패: " + result);
        }
    }

    private void refundPoint(Player player, String skillId) {
        var entity = combatModule.getCore().getEntityManager().get(player.getUniqueId());
        if (entity == null) {
            player.sendMessage("§c엔티티 정보를 찾을 수 없습니다.");
            return;
        }

        var result = skillTreeManager.refundPoint(entity, skillId.toUpperCase());
        switch (result) {
            case SUCCESS -> player.sendMessage("§a스킬 포인트 회수 성공: " + skillId);
            case FAIL_DEPENDENT -> player.sendMessage("§c이 스킬에 의존하는 다른 스킬이 있습니다.");
            default -> player.sendMessage("§c회수 실패: " + result);
        }
    }

    private void resetSkillTree(Player player) {
        var entity = combatModule.getCore().getEntityManager().get(player.getUniqueId());
        if (entity == null) {
            player.sendMessage("§c엔티티 정보를 찾을 수 없습니다.");
            return;
        }

        skillTreeManager.respec(entity);
        player.sendMessage("§a스킬 트리가 초기화되었습니다!");
    }

    private void listSkills(Player player) {
        var entity = combatModule.getCore().getEntityManager().get(player.getUniqueId());
        if (entity == null) {
            player.sendMessage("§c엔티티 정보를 찾을 수 없습니다.");
            return;
        }

        var identity = entity.getComponent(com.sanctuary.core.ecs.component.IdentityComponent.class);
        String className = identity != null ? identity.getJob() : "BARBARIAN";

        List<SkillData> skills = skillTreeManager.getSkillsForClass(className);
        if (skills.isEmpty()) {
            player.sendMessage("§c스킬 데이터가 로드되지 않았습니다.");
            return;
        }

        player.sendMessage("§6===== " + className + " 스킬 목록 =====");
        skills.forEach(skill -> {
            String categoryColor = switch (skill.getCategory()) {
                case "BASIC" -> "§a";
                case "CORE" -> "§e";
                case "DEFENSIVE" -> "§b";
                case "ULTIMATE" -> "§6";
                default -> "§7";
            };
            player.sendMessage(categoryColor + "[" + skill.getCategory() + "] §f" +
                    skill.getName() + " §7(" + skill.getId() + ")");
        });
    }

    private void addPoints(Player player, String amountStr) {
        try {
            int amount = Integer.parseInt(amountStr);
            var entity = combatModule.getCore().getEntityManager().get(player.getUniqueId());
            if (entity == null) {
                player.sendMessage("§c엔티티 정보를 찾을 수 없습니다.");
                return;
            }

            SkillComponent skillComp = entity.getComponent(SkillComponent.class);
            if (skillComp == null) {
                skillComp = new SkillComponent();
                entity.attach(skillComp);
            }

            skillComp.addPoints(amount);
            player.sendMessage("§a스킬 포인트 " + amount + "점 추가! (현재: " + skillComp.getAvailablePoints() + ")");
        } catch (NumberFormatException e) {
            player.sendMessage("§c올바른 숫자를 입력하세요.");
        }
    }
}
