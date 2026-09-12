#!/usr/bin/env bash
#
# 將 bestpartner-service 打包為跨環境 Docker image，驗證可啟動，並匯出 tar — 原生 bash 版。
#
# 產出「一份 image 跑所有環境」的成品。環境差異不在打包階段決定，
# 而是在 docker run 時由 -e QUARKUS_PROFILE 與 --env-file 指定，
# 因此本腳本沒有、也不該有「環境」參數。
#
#   流程：讀版本 → 建 uber-jar(prod) → docker build → 啟動驗證 → 清理 → 匯出 tar
#
# 建置 profile 固定為 prod：建置時的 profile 會成為 image 的預設 runtime profile，
# 用 prod 才能讓部署時漏帶 QUARKUS_PROFILE 落在「Swagger 關閉、log INFO」的安全側。
#
# ⚠️ 本檔與 docker-package.ps1 是同一套流程的兩份實作（Windows 用 .ps1，
#    Linux/macOS 用本檔）。修改流程時**兩份都要改**，詳見 scripts/README.md。
#
# 用法：
#   ./scripts/docker-package.sh                     # 完整流程
#   ./scripts/docker-package.sh --no-tar            # 只建 image，不匯出
#   ./scripts/docker-package.sh --verify-port 18085 # 驗證埠被占用時
#   ./scripts/docker-package.sh --skip-verify       # 跳過啟動驗證（不建議）
#
set -euo pipefail

# --- 參數 ----------------------------------------------------------------
VERIFY_PORT=18080
SKIP_VERIFY=0
NO_TAR=0

usage() {
  sed -n '2,25p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
  exit "${1:-0}"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --verify-port) VERIFY_PORT="${2:?--verify-port 需要一個埠號}"; shift 2 ;;
    --skip-verify) SKIP_VERIFY=1; shift ;;
    --no-tar)      NO_TAR=1; shift ;;
    -h|--help)     usage 0 ;;
    *) echo "未知參數：$1" >&2; usage 1 ;;
  esac
done

# 本腳本位於 scripts/，建置目標在 repo 根目錄下，故 root 取上一層。
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
serviceDir="$root/bestpartner-service"
dockerfile='src/main/docker/Dockerfile.uber-jar'
verifyName='bestpartner-verify'
imageName='bestpartner-service'

if [[ -t 1 ]]; then
  C_CYAN=$'\033[36m'; C_GREEN=$'\033[32m'; C_RED=$'\033[31m'
  C_YELLOW=$'\033[33m'; C_OFF=$'\033[0m'
else
  C_CYAN=''; C_GREEN=''; C_RED=''; C_YELLOW=''; C_OFF=''
fi

write_step() { echo; echo "${C_CYAN}[$1] $2${C_OFF}"; }
write_ok()   { echo "${C_GREEN}    ✓ $1${C_OFF}"; }
write_fail() { echo "${C_RED}    ✗ $1${C_OFF}"; }
die()        { echo "${C_RED}$1${C_OFF}" >&2; exit 1; }

echo "=== BestPartner Docker 打包 ==="

# --- 前置檢查 ------------------------------------------------------------
write_step 0 '前置檢查'

[[ -d "$serviceDir" ]] || die "找不到 bestpartner-service 目錄：$serviceDir"

if [[ ! -f "$serviceDir/$dockerfile" ]]; then
  die "找不到 $dockerfile。
注意：src/main/docker 下其餘 Dockerfile 為 Quarkus 預設產生，對應
fast-jar / legacy-jar / native 佈局，與本專案 uber-jar 建置不符，不可替代。"
fi
write_ok 'Dockerfile.uber-jar 存在'

command -v docker >/dev/null 2>&1 || die '找不到 docker 指令，請先安裝 Docker 並確認已加入 PATH。'
docker info >/dev/null 2>&1 || die 'Docker daemon 未執行，請先啟動 Docker Desktop / dockerd。'
write_ok 'Docker daemon 可用'

command -v curl >/dev/null 2>&1 || die '找不到 curl，啟動驗證需要它（或以 --skip-verify 跳過）。'

# --- 讀版本（不硬編碼） ---------------------------------------------------
write_step 1 '讀取版本號'

buildGradle="$serviceDir/build.gradle.kts"
version="$(sed -nE 's/^version[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' "$buildGradle" | head -n 1)"
[[ -n "$version" ]] || die "無法從 $buildGradle 解析 version，請確認該檔含 version = \"x.y.z-SNAPSHOT\" 格式。"
write_ok "版本：$version"

# --- 建 uber-jar ---------------------------------------------------------
write_step 2 '建置 uber-jar（profile 固定 prod）'

cd "$serviceDir"

[[ -x ./gradlew ]] || die "./gradlew 無執行權限。請執行：
  git update-index --chmod=+x bestpartner-service/gradlew && chmod +x bestpartner-service/gradlew"

if ! ./gradlew clean build -x test \
      -Dquarkus.package.type=uber-jar \
      -Dorg.gradle.daemon=false \
      -Dquarkus.profile=prod; then
  die "Gradle 建置失敗。
若錯誤是 Unable to delete directory 'build'，通常為 IDE 或防毒鎖住 jar，
重跑一次多半即可解決。"
fi

jar="$serviceDir/build/$imageName-$version-runner.jar"
[[ -f "$jar" ]] || die "建置宣稱成功，但找不到產物：$jar"
jarMb=$(( $(wc -c < "$jar") / 1048576 ))
write_ok "uber-jar 產出（${jarMb} MB）"

# --- docker build --------------------------------------------------------
write_step 3 'docker build'

docker build -f "$dockerfile" -t "${imageName}:${version}" -t "${imageName}:latest" . \
  || die 'docker build 失敗'
write_ok "image 標記為 ${imageName}:${version} 與 ${imageName}:latest"

# --- 啟動驗證 ------------------------------------------------------------
cleanup_verify() { docker rm -f "$verifyName" >/dev/null 2>&1 || true; }

if [[ $SKIP_VERIFY -eq 1 ]]; then
  write_step 4 '啟動驗證（已依 --skip-verify 跳過）'
  echo "${C_YELLOW}    ⚠ 未驗證的 image 不保證跑得起來${C_OFF}"
else
  write_step 4 "啟動驗證（宿主埠 $VERIFY_PORT）"

  cleanup_verify
  trap cleanup_verify EXIT

  docker run -d --name "$verifyName" -p "${VERIFY_PORT}:80" "${imageName}:latest" >/dev/null \
    || die "容器啟動失敗（埠 $VERIFY_PORT 可能被占用，可用 --verify-port 指定其他埠）"

  # 存活探測用 /view/chat：公開且不受 profile 影響。
  # 不可用 /q/openapi 或 /swagger-ui——預設 prod 下它們回 404 是正確行為。
  ok=0
  for _ in $(seq 1 30); do
    sleep 1
    code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 \
            "http://localhost:${VERIFY_PORT}/view/chat" 2>/dev/null || true)"
    [[ "$code" == '200' ]] && { ok=1; break; }
  done

  if [[ $ok -ne 1 ]]; then
    write_fail '/view/chat 未回 200，容器 log 如下：'
    docker logs "$verifyName" 2>&1 | tail -n 25
    die '啟動驗證失敗'
  fi

  profile="$(docker logs "$verifyName" 2>&1 | sed -nE 's/.*Profile ([A-Za-z0-9_]+) activated.*/\1/p' | head -n 1)"
  write_ok "/view/chat 回 200（預設 profile：${profile:-未知}）"

  cleanup_verify
  trap - EXIT
  write_ok '暫存容器已清理'
fi

# --- 匯出 tar ------------------------------------------------------------
tarPath=''
if [[ $NO_TAR -eq 1 ]]; then
  write_step 5 '匯出 tar（已依 --no-tar 跳過）'
else
  write_step 5 '匯出 tar'

  rm -f build/*.tar
  stamp="$(date +%Y%m%d%H%M)"
  tarName="$imageName-$version-prod-$stamp.tar"
  tarPath="$serviceDir/build/$tarName"

  docker save -o "build/$tarName" "${imageName}:latest" || die 'docker save 失敗'

  tarMb=$(( $(wc -c < "$tarPath") / 1048576 ))
  write_ok "$tarName（${tarMb} MB）"
fi

# --- 回報 ----------------------------------------------------------------
echo
echo '=== 完成 ==='
echo "image : ${imageName}:${version}、${imageName}:latest"
if [[ -n "$tarPath" ]]; then
  echo "tar   : $tarPath"
  echo
  echo "${C_YELLOW}⚠ 此 tar 內含 JWT 簽章私鑰（privateKey.pem 隨 uber-jar 打包），${C_OFF}"
  echo "${C_YELLOW}  等同憑證，不可在不受控管道流通。${C_OFF}"
fi

cat <<EOF

檔名的 -prod 是「建置 profile」，不是部署目標。
這一份 image 全環境通用，跑哪個環境由 runtime 決定：

  docker run -d -p 80:80 -e QUARKUS_PROFILE=uat --env-file .env.uat \\
    -v bestpartner-data:/opt/bestpartner ${imageName}:latest

  profile         Swagger/OpenAPI   SQL 日誌   log
  (未指定)        關                關         INFO   ← 安全預設
  dev/docker/sit  開                開         DEBUG
  uat/prod        關                關         INFO

存活探測請用 /view/chat；/swagger-ui 與 /q/openapi 在 uat/prod 回 404 為正確行為。
EOF
