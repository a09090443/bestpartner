#!/usr/bin/env bash
#
# BestPartner Harness 漂移掃描（Harness 原則 6：Entropy Management）— 原生 bash 版。
#
# 可重複執行的程序，逐項對照 .claude/rules/ 的黃金規範，掃描跨模組慣例漂移。
# 輸出「現況 vs 規範 vs 建議」表，並區分「歷史基線」與「本次新漂移」。
#
#   - 結構性不變量（分層依賴、命名、@Entity 位置）由 ArchUnit 在 build 階段強制，
#     本掃描補足 ArchUnit 不易檢查的慣例（i18n 寫死字串、版本硬編碼、套件拼字、CDI 標註），
#     並涵蓋前端跨界契約（NodeType 前後端一致）與前端測試命名（.test.ts）。
#   - 歷史基線（既存且刻意容忍的漂移）預先登錄於下方 Baseline 陣列，不視為失敗。
#   - 偵測到「新漂移」時以 exit code 1 結束，可作為 CI 把關。
#
# ⚠️ 本檔與 harness-drift-scan.ps1 是同一套規則的兩份實作（Windows 用 .ps1，
#    Linux/macOS 用本檔）。修改任一規則或基線時**兩份都要改**；
#    CI 會同時執行兩者並比對結果，不一致即擋下。詳見 scripts/README.md。
#
# 用法：./scripts/harness-drift-scan.sh
#
set -uo pipefail

# 本腳本位於 scripts/，掃描目標是整個 repo，故 root 取上一層。
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
srcMain="$root/bestpartner-service/src/main/kotlin/tw/zipe/bastpartner"
buildGradle="$root/bestpartner-service/build.gradle.kts"
uiSrc="$root/bestpartner-ui/src"
nodeTypeKt="$srcMain/enumerate/NodeType.kt"
nodeTypeTs="$uiSrc/types/workflow.ts"

# ---- 歷史基線（既存且刻意容忍的漂移；新增程式碼不得再擴大）----
# @Entity 但無 Entity 後綴（嵌入式 *Id 類為 @Embeddable，另行排除）
BASELINE_ENTITY_NO_SUFFIX=('LLMMcpUserSetting')
# 仍以字面字串拋例外、尚未改用 AppMessage 的檔案
BASELINE_I18N_FILES=('LLMStore.kt' 'LLMResource.kt')
# build.gradle.kts 仍硬編碼版本的依賴 artifact
BASELINE_VERSION_ARTS=('jsqlparser')
# 前端既存以 .spec.ts 命名的測試檔（新測試一律 .test.ts，見 frontend-conventions.md）
BASELINE_SPEC_TS=('ExecutionResultDrawer.spec.ts' 'OutputForm.spec.ts' 'execution.spec.ts')

# 顏色（非 TTY 時停用，避免污染 CI log）
if [[ -t 1 ]]; then
  C_CYAN=$'\033[36m'; C_YELLOW=$'\033[33m'; C_RED=$'\033[31m'
  C_GREEN=$'\033[32m'; C_GRAY=$'\033[90m'; C_OFF=$'\033[0m'
else
  C_CYAN=''; C_YELLOW=''; C_RED=''; C_GREEN=''; C_GRAY=''; C_OFF=''
fi

findings=()
add_finding() { findings+=("$1|$2|$3|$4"); }

# 判斷元素是否在陣列中：contains <needle> <haystack...>
contains() {
  local needle="$1"; shift
  local item
  for item in "$@"; do [[ "$item" == "$needle" ]] && return 0; done
  return 1
}

# ---- 規則 1：套件拼字必須是 bastpartner（非 basepartner）----
typo_hits="$(grep -rn --include='*.kt' 'tw\.zipe\.basepartner' "$srcMain" 2>/dev/null || true)"
if [[ -n "$typo_hits" ]]; then
  while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    file="$(basename "${line%%:*}")"
    rest="${line#*:}"; lineno="${rest%%:*}"
    add_finding '套件拼字' 'NEW' "$file:$lineno" '改為 tw.zipe.bastpartner（少一個 e）'
  done <<< "$typo_hits"
else
  add_finding '套件拼字' 'OK' '0 處誤用 basepartner' '—'
fi

# ---- 規則 2：業務訊息不得寫死字串（應用 AppMessage + i18n）----
i18n_hits="$(grep -rn --include='*.kt' -E '(ServiceException|LLMException)\("' "$srcMain" 2>/dev/null || true)"
if [[ -n "$i18n_hits" ]]; then
  # 依檔名分組計數
  while IFS='|' read -r fname count; do
    [[ -z "$fname" ]] && continue
    if contains "$fname" "${BASELINE_I18N_FILES[@]}"; then
      add_finding 'i18n 寫死字串' 'BASELINE' "$fname（$count 處）" '排程改用 AppMessage（i18n-messages.md）'
    else
      add_finding 'i18n 寫死字串' 'NEW' "$fname（$count 處）" '改用 AppMessage enum，勿寫死字串'
    fi
  done < <(echo "$i18n_hits" | while IFS= read -r l; do basename "${l%%:*}"; done | sort | uniq -c |
           awk '{ printf "%s|%s\n", $2, $1 }')
else
  add_finding 'i18n 寫死字串' 'OK' '無新增寫死字串' '—'
fi

# ---- 規則 3：build.gradle.kts 版本不得硬編碼（plugins 區塊除外）----
ver_hits="$(grep -nE 'implementation\("[^"]*:[0-9]+\.[0-9]+' "$buildGradle" 2>/dev/null || true)"
if [[ -n "$ver_hits" ]]; then
  while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    lineno="${line%%:*}"
    # 取 implementation("<coord>:<version> 中 <coord> 的最後一段作為 artifact 名
    coord="$(echo "$line" | sed -nE 's/.*implementation\("([^"]*):[0-9]+\.[0-9]+.*/\1/p')"
    art="${coord##*:}"
    if contains "$art" "${BASELINE_VERSION_ARTS[@]}"; then
      add_finding '版本硬編碼' 'BASELINE' "$art（行 $lineno）" '排程移版本至 gradle.properties'
    else
      add_finding '版本硬編碼' 'NEW' "$art（行 $lineno）" '版本移至 gradle.properties，以 $xxxVersion 引用'
    fi
  done <<< "$ver_hits"
else
  add_finding '版本硬編碼' 'OK' '無硬編碼版本' '—'
fi

# ---- 規則 4：@Entity 類別命名應以 Entity 結尾（嵌入式 *Id 排除）----
entityDir="$srcMain/entity"
if [[ -d "$entityDir" ]]; then
  for f in "$entityDir"/*.kt; do
    [[ -e "$f" ]] || continue
    name="$(basename "$f" .kt)"
    grep -q '@Entity' "$f" || continue
    [[ "$name" == *Entity ]] && continue
    [[ "$name" == *Id ]] && continue
    if contains "$name" "${BASELINE_ENTITY_NO_SUFFIX[@]}"; then
      add_finding 'Entity 命名' 'BASELINE' "$name" '排程重新命名為 XxxEntity（破壞性，另行評估）'
    else
      add_finding 'Entity 命名' 'NEW' "$name" '@Entity 類別請命名為 XxxEntity.kt'
    fi
  done
fi

# ---- 規則 5：service / repository 套件類別必須標 @ApplicationScoped（Base 類除外）----
cdi_new=0
for pkg in service repository; do
  pkgDir="$srcMain/$pkg"
  [[ -d "$pkgDir" ]] || continue
  for f in "$pkgDir"/*.kt; do
    [[ -e "$f" ]] || continue
    name="$(basename "$f" .kt)"
    [[ "$name" == Base* ]] && continue
    if ! grep -q '@ApplicationScoped' "$f"; then
      add_finding "CDI 標註($pkg)" 'NEW' "$name" '加上 @ApplicationScoped'
      cdi_new=1
    fi
  done
done
[[ $cdi_new -eq 0 ]] && add_finding 'CDI 標註' 'OK' 'service/repository 皆已標註' '—'

# ---- 規則 6：SQL 測試資料不得含真實 API 金鑰 ----
# 無基線——真實金鑰任何時候都應為 0，於本機 commit 前即攔下，補 GitHub push protection 的事後把關。
sqlDir="$root/docs/sql"
secret_pattern='sk-or-v1-[A-Za-z0-9]{20,}|sk-proj-[A-Za-z0-9_-]{20,}|sk-[A-Za-z0-9]{40,}'
secret_hit=0
if [[ -d "$sqlDir" ]]; then
  for f in "$sqlDir"/*.sql; do
    [[ -e "$f" ]] || continue
    n="$(grep -cE "$secret_pattern" "$f" 2>/dev/null || true)"
    if [[ "${n:-0}" -gt 0 ]]; then
      secret_hit=1
      add_finding '金鑰外洩' 'NEW' "$(basename "$f")（$n 行）" '改用佔位符 sk-or-v1-xxx／sk-proj-xxx，勿提交真實金鑰'
    fi
  done
fi
[[ $secret_hit -eq 0 ]] && add_finding '金鑰外洩' 'OK' 'SQL 無真實金鑰' '—'

# ---- 規則 7：.claude/CLAUDE.md 須以 @import 展開根目錄 AGENTS.md（單一事實來源）----
# AGENTS.md 是唯一的導覽地圖；CLAUDE.md 只保留一條 @../AGENTS.md import。
# 檢查 CLAUDE.md 是否含該 import 行，防止有人又塞回完整副本或清空薄殼。無基線——脫鉤任何時候都應攔下。
agentsMd="$root/AGENTS.md"
claudeMd="$root/.claude/CLAUDE.md"
importLine='@../AGENTS.md'
if [[ -f "$agentsMd" && -f "$claudeMd" ]]; then
  if grep -qF "$importLine" "$claudeMd"; then
    add_finding '導覽一致性' 'OK' '.claude/CLAUDE.md 以 @import 展開 AGENTS.md' '—'
  else
    add_finding '導覽一致性' 'NEW' '.claude/CLAUDE.md 缺少 @import' '.claude/CLAUDE.md 應含「@../AGENTS.md」import，指向根目錄 AGENTS.md'
  fi
else
  add_finding '導覽一致性' 'NEW' '缺少 AGENTS.md 或 .claude/CLAUDE.md' '兩份導覽文件皆須存在'
fi

# ---- 規則 8：Workflow NodeType 跨界一致（後端 enum ≡ 前端 union）----
# 無基線——節點型別是前後端硬契約，任一邊新增/移除卻未同步，畫布與引擎即失聯。
if [[ -f "$nodeTypeKt" && -f "$nodeTypeTs" ]]; then
  # 後端：取 enum class NodeType { ... } 大括號內的大寫識別字
  kt_members="$(sed -nE '/enum class NodeType[[:space:]]*\{/,/^\}/p' "$nodeTypeKt" |
                grep -oE '\b[A-Z][A-Z0-9_]+\b' | sort -u)"
  # 前端：export type NodeType = 之後的 'XXX' 字面，止於下一個空白行
  ts_members="$(awk '/export type NodeType[[:space:]]*=/{f=1} f{if (/^[[:space:]]*$/) exit; print}' "$nodeTypeTs" |
                grep -oE "'[A-Z0-9_]+'" | tr -d "'" | sort -u)"

  if [[ -z "$kt_members" || -z "$ts_members" ]]; then
    add_finding 'NodeType 契約' 'NEW' '無法解析 NodeType 定義' '檢查 NodeType.kt 與 types/workflow.ts 格式'
  else
    only_kt="$(comm -23 <(echo "$kt_members") <(echo "$ts_members") | paste -sd, -)"
    only_ts="$(comm -13 <(echo "$kt_members") <(echo "$ts_members") | paste -sd, -)"
    if [[ -z "$only_kt" && -z "$only_ts" ]]; then
      count="$(echo "$kt_members" | wc -l | tr -d ' ')"
      add_finding 'NodeType 契約' 'OK' "前後端一致（$count 種）" '—'
    else
      detail=''
      [[ -n "$only_kt" ]] && detail="僅後端有：$only_kt"
      [[ -n "$only_ts" ]] && detail="${detail:+$detail；}僅前端有：$only_ts"
      add_finding 'NodeType 契約' 'NEW' "$detail" '同步後端 NodeType.kt 與前端 types/workflow.ts，兩邊成員須一致'
    fi
  fi
else
  add_finding 'NodeType 契約' 'NEW' '缺少 NodeType.kt 或 types/workflow.ts' '兩份節點型別定義皆須存在'
fi

# ---- 規則 9：前端新測試檔一律 .test.ts（既存 .spec.ts 為基線）----
if [[ -d "$uiSrc" ]]; then
  new_spec=0; base_spec=0
  while IFS= read -r f; do
    [[ -z "$f" ]] && continue
    n="$(basename "$f")"
    if contains "$n" "${BASELINE_SPEC_TS[@]}"; then
      base_spec=$((base_spec + 1))
    else
      add_finding '測試命名' 'NEW' "$n" '新測試檔請以 .test.ts 命名，勿新增 .spec.ts'
      new_spec=$((new_spec + 1))
    fi
  done < <(find "$uiSrc" -name '*.spec.ts' -type f 2>/dev/null)

  if [[ $base_spec -gt 0 && $new_spec -eq 0 ]]; then
    add_finding '測試命名' 'BASELINE' "$base_spec 個既存 .spec.ts" '新測試改用 .test.ts（不追溯改名既有檔）'
  elif [[ $new_spec -eq 0 ]]; then
    add_finding '測試命名' 'OK' '無新增 .spec.ts' '—'
  fi
fi

# ---- 輸出 ----
echo
echo "${C_CYAN}=== BestPartner Harness 漂移掃描 ===${C_OFF}"
echo

printf '%s\n' "${findings[@]}" | sort | awk -F'|' -v ok="✓ OK" -v nw="⚠ NEW" '
  BEGIN { printf "%-16s %-9s %-42s %s\n", "Rule", "Status", "Detail", "Advice"
          printf "%-16s %-9s %-42s %s\n", "----", "------", "------", "------" }
  {
    st = ($2 == "NEW") ? nw : ($2 == "BASELINE") ? "BASELINE" : ok
    printf "%-16s %-9s %-42s %s\n", $1, st, $3, $4
  }'

new_count=0; base_count=0
for f in "${findings[@]}"; do
  status="$(echo "$f" | cut -d'|' -f2)"
  [[ "$status" == 'NEW' ]] && new_count=$((new_count + 1))
  [[ "$status" == 'BASELINE' ]] && base_count=$((base_count + 1))
done

echo
echo "${C_YELLOW}基線 ${base_count} 項、新漂移 ${new_count} 項。${C_OFF}"
echo "${C_GRAY}提醒：結構性不變量（分層、命名、@Entity 位置）另由 ArchitectureTest.kt 強制：${C_OFF}"
echo "${C_GRAY}  cd bestpartner-service; ./gradlew test --tests \"tw.zipe.bastpartner.architecture.ArchitectureTest\"${C_OFF}"

if [[ $new_count -gt 0 ]]; then
  echo
  echo "${C_RED}發現 ${new_count} 項新漂移，請修正或（確認可接受後）登錄為基線。${C_OFF}"
  exit 1
fi
echo
echo "${C_GREEN}無新漂移。${C_OFF}"
exit 0
