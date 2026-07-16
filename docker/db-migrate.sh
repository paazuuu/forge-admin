#!/bin/bash
# ============================================
# Forge Admin DB マイグレーション適用スクリプト
# ============================================
# MySQL 公式イメージの初期化フェーズ (/docker-entrypoint-initdb.d) で
# 01-init.sql の後に実行され、Flyway マイグレーションをバージョン順で適用する。
# マイグレーションは冪等 (IF NOT EXISTS / information_schema チェック) なため
# 初回初期化時の一度きりで完結する。
set -e

MIGRATION_DIR="/flyway-migrations"

if [ ! -d "$MIGRATION_DIR" ]; then
  echo "[db-migrate] migration ディレクトリが無いためスキップ: $MIGRATION_DIR"
  exit 0
fi

for f in $(ls "$MIGRATION_DIR"/V*.sql 2>/dev/null | sort -V); do
  echo "[db-migrate] applying: $(basename "$f")"
  mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < "$f"
done

echo "[db-migrate] すべてのマイグレーション適用完了"

# ============================================
# Flowable エンジン管理テーブルの初期化
# ============================================
# 全量初始化SQL.sql は Flowable の batch エンジンテーブル
# (flw_ru_batch / flw_ru_batch_part と FK 制約 FLW_FK_BATCH_PART_PARENT) を同梱するが、
# 対応する ACT_GE_PROPERTY のスキーマバージョンマーカーを含まない。このため
# forge-flow 起動時に Flowable 7.0.1 がフルスキーマ作成モードに入り、既存の batch
# テーブル/FK と衝突して "Duplicate foreign key constraint name 'FLW_FK_BATCH_PART_PARENT'"
# でクラッシュループする。これらのエンジンテーブルは Flowable 自身が起動時に正しい
# バージョンで再生成する（process エンジンの BatchDbSchemaManager が担当）ため、
# 初期化フェーズであらかじめ削除して衝突を防ぐ。冪等 (IF EXISTS)。
echo "[db-migrate] Flowable batch テーブルを削除 (起動時に Flowable が再生成)"
mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" <<'SQL'
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS flw_ru_batch_part;
DROP TABLE IF EXISTS flw_ru_batch;
SET FOREIGN_KEY_CHECKS = 1;
SQL

echo "[db-migrate] 初期化完了"
