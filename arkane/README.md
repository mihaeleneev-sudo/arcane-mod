# Arcane (Fabric 1.20.1)
Сборка: GitHub Actions (.github/workflows/build.yml) или `gradle wrapper --gradle-version 8.5` + `./gradlew build`.
Готовый мод: build/libs/arcane-1.2.0.jar (не sources). Нужен Fabric API 0.92.2+1.20.1.
Мод нужен и на сервере, и у всех игроков.

Управление: Alt - превращение, R - способность Эйро (бросок блока). Клавиши меняются в настройках управления.
Выдать силу: /give @s arcane:arcane_shard (Саир) или arcane:eiro_heart (Эйро), либо /arcane character sair|eiro|none
Сердце Эйро лежит в сундуках адской крепости и бастионов. Конфиг: config/arcane.properties (shardChance, eiroHeartChance, legendName)
