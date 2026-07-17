# BestPartner API 自動化測試執行器（PowerShell 版）
#
# ⚠️ 正式 API 測試流程請走 test-confirmation skill（見 .claude/rules/api-testing.md）。
#    本腳本僅為早期草稿，保留作歷史參考。

$repoRoot = Split-Path -Parent $PSScriptRoot

$BASE_URL = "http://localhost:80"
$ADMIN_EMAIL = "admin@bestpartner.com.tw"
$ADMIN_PASSWORD = "admin"
$USER_EMAIL = "user@bestpartner.com.tw"
$USER_PASSWORD = "user"

$testResults = @{
    timestamp = (Get-Date).ToString('o')
    environment = "dev"
    total_tests = 0
    passed = 0
    failed = 0
    skipped = 0
    tests = @()
}

function Login($email, $password) {
    try {
        $response = Invoke-WebRequest -Uri "$BASE_URL/login/" `
            -Method Post `
            -ContentType "application/json" `
            -Body (ConvertTo-Json @{email=$email; password=$password}) `
            -TimeoutSec 5 `
            -SkipHttpErrorCheck

        if ($response.StatusCode -eq 200) {
            $data = $response.Content | ConvertFrom-Json
            return $data.data
        }
        return $null
    }
    catch {
        Write-Host "❌ 登入失敗: $_" -ForegroundColor Red
        return $null
    }
}

function ExecuteTest($testId, $endpoint, $method, $headers, $payload, $expectedStatus) {
    try {
        $params = @{
            Uri = "$BASE_URL$endpoint"
            Method = $method
            ContentType = "application/json"
            TimeoutSec = 5
            SkipHttpErrorCheck = $true
        }

        if ($headers) {
            $params['Headers'] = $headers
        }

        if ($payload -and ($method -eq "POST" -or $method -eq "DELETE")) {
            $params['Body'] = (ConvertTo-Json $payload -Depth 10)
        }

        $response = Invoke-WebRequest @params
        $passed = $response.StatusCode -eq $expectedStatus

        $testResult = @{
            test_id = $testId
            endpoint = $endpoint
            method = $method
            expected_status = $expectedStatus
            actual_status = $response.StatusCode
            passed = $passed
            response_preview = $response.Content.Substring(0, [Math]::Min(200, $response.Content.Length))
        }

        $testResults.tests += $testResult
        $testResults.total_tests++

        if ($passed) {
            $testResults.passed++
            $status = "✅"
        } else {
            $testResults.failed++
            $status = "❌"
        }

        Write-Host "$status $testId`: $method $endpoint (期望: $expectedStatus, 實際: $($response.StatusCode))" -ForegroundColor $(if ($passed) { "Green" } else { "Red" })
        return $testResult
    }
    catch {
        $testResults.tests += @{
            test_id = $testId
            endpoint = $endpoint
            error = $_.ToString()
        }
        $testResults.failed++
        $testResults.total_tests++
        Write-Host "❌ $testId`: 例外 - $_" -ForegroundColor Red
        return $null
    }
}

# 主流程
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "BestPartner API 自動化測試" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 登入
Write-Host "`n[準備] 登入取得 Token..." -ForegroundColor Yellow
$adminToken = Login $ADMIN_EMAIL $ADMIN_PASSWORD
$userToken = Login $USER_EMAIL $USER_PASSWORD

if (-not $adminToken -or -not $userToken) {
    Write-Host "❌ 無法取得 Token，終止測試" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Admin Token: $($adminToken.Substring(0, 20))..." -ForegroundColor Green
Write-Host "✅ User Token: $($userToken.Substring(0, 20))..." -ForegroundColor Green

$adminHeaders = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $adminToken"
}

$userHeaders = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $userToken"
}

# AUTH 模組
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "[AUTH 模組] 認證測試" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

ExecuteTest "AUTH-003" "/login/" "POST" $null @{email=$ADMIN_EMAIL; password="wrongpassword"} 401
ExecuteTest "AUTH-004" "/login/" "POST" $null @{email="notexist@example.com"; password="test"} 401
ExecuteTest "AUTH-005" "/login/" "POST" $null @{password="admin"} 400
ExecuteTest "AUTH-006" "/login/" "POST" $null @{email=$ADMIN_EMAIL} 400
ExecuteTest "AUTH-007" "/login/" "POST" $null @{} 400
ExecuteTest "AUTH-009" "/login/check" "POST" @{"Authorization" = "Bearer invalid-token"} @{} 401

# CHAT 模組
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "[CHAT 模組] 聊天測試" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

ExecuteTest "CHAT-003" "/llm/chat" "POST" $userHeaders @{message="test"} 400
ExecuteTest "CHAT-004" "/llm/chat" "POST" $userHeaders @{llmId="33b5e4a4-798b-41fd-abad-21e2e68831b1"} 400
ExecuteTest "CHAT-005" "/llm/chat" "POST" $userHeaders @{llmId="invalid-id"; message="test"} 500

# USER 模組
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "[USER 模組] 用戶測試" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

ExecuteTest "USER-002" "/llm/user/register" "POST" $null @{username="user"; password="user"; email=$USER_EMAIL} 400
ExecuteTest "USER-003" "/llm/user/register" "POST" $null @{password="test"; email="test@example.com"} 400
ExecuteTest "USER-004" "/llm/user/register" "POST" $null @{username="test"; email="test@example.com"} 400
ExecuteTest "USER-005" "/llm/user/register" "POST" $null @{username="test"; password="test"} 400
ExecuteTest "USER-009" "/llm/user/update" "POST" $userHeaders @{id="invalid"; email="test@example.com"; status="ACTIVE"} 404

Write-Host "`n⚠️ [已知缺陷] USER-012 & USER-014: @RolesAllowed('all') 權限無映射" -ForegroundColor Yellow
$testResults.skipped += 2
$testResults.total_tests += 2

# PERMISSION 模組
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "[PERMISSION 模組] 權限測試" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

ExecuteTest "PERM-002" "/llm/permission/add" "POST" $null @{name="test_perm"; num=999} 401
ExecuteTest "PERM-003" "/llm/permission/add" "POST" $adminHeaders @{num=999} 400
ExecuteTest "PERM-004" "/llm/permission/add" "POST" $adminHeaders @{name="test_perm"} 400

# SYSTEM SETTING 模組
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "[SYSTEM SETTING 模組] 系統設定測試" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

ExecuteTest "SYS-002" "/systemSetting/get" "POST" $null @{key="default_llm_platform"} 200
ExecuteTest "SYS-003" "/systemSetting/get" "POST" $null @{} 400
ExecuteTest "SYS-004" "/systemSetting/get" "POST" $null @{key="nonexistent_key"} 404

# TOOL 模組
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "[TOOL 模組] 工具測試" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

ExecuteTest "TOOL-002" "/llm/tool/get" "POST" $userHeaders @{id=1} 200
ExecuteTest "TOOL-003" "/llm/tool/get" "POST" $userHeaders @{} 400

# MCP 模組
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "[MCP 模組] MCP 測試" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

ExecuteTest "MCP-003" "/llm/mcpServer/get" "POST" $userHeaders @{mcpId="invalid"} 404
ExecuteTest "MCP-004" "/llm/mcpServer/get" "POST" $userHeaders @{} 400

# VECTOR 模組
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "[VECTOR 模組] 向量測試" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

ExecuteTest "VEC-003" "/llm/vector/update" "POST" $userHeaders @{id="invalid"; collectionName="test"} 404

# 保存結果
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "測試結果摘要" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

$passRate = if ($testResults.total_tests -gt 0) { ($testResults.passed / $testResults.total_tests) * 100 } else { 0 }

Write-Host "📊 總測試數: $($testResults.total_tests)" -ForegroundColor Cyan
Write-Host "   ✅ 通過: $($testResults.passed)" -ForegroundColor Green
Write-Host "   ❌ 失敗: $($testResults.failed)" -ForegroundColor Red
Write-Host "   ⏭️ 跳過: $($testResults.skipped)" -ForegroundColor Yellow
Write-Host "   通過率: $([Math]::Round($passRate, 1))%" -ForegroundColor Cyan

$resultsFile = Join-Path $repoRoot 'test_results_manual.json'
$testResults | ConvertTo-Json -Depth 10 | Out-File -FilePath $resultsFile -Encoding UTF8
Write-Host "`n📊 詳細結果已保存至: $resultsFile" -ForegroundColor Green
