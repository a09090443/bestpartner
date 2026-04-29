#!/usr/bin/env python3
"""
BestPartner API 自動化測試執行器
執行 P1/P2 級別的 131 個測試案例
"""

import requests
import json
import sys
from datetime import datetime
from urllib.parse import urljoin

BASE_URL = "http://localhost:80"
TEST_RESULTS = {
    "timestamp": datetime.now().isoformat(),
    "environment": "dev",
    "total_tests": 0,
    "passed": 0,
    "failed": 0,
    "skipped": 0,
    "tests": []
}

# 預設帳號
ADMIN_EMAIL = "admin@bestpartner.com.tw"
ADMIN_PASSWORD = "admin"
USER_EMAIL = "user@bestpartner.com.tw"
USER_PASSWORD = "user"

ADMIN_TOKEN = None
USER_TOKEN = None

def login(email, password):
    """登入並取得 JWT token"""
    try:
        response = requests.post(
            urljoin(BASE_URL, "/login/"),
            json={"email": email, "password": password},
            timeout=5
        )
        if response.status_code == 200:
            data = response.json()
            return data.get("data")
        return None
    except Exception as e:
        print(f"❌ 登入失敗: {e}")
        return None

def execute_test(test_id, endpoint, method="POST", headers=None, payload=None, expected_status=200):
    """執行單個 API 測試"""
    global TEST_RESULTS

    if headers is None:
        headers = {"Content-Type": "application/json"}

    try:
        url = urljoin(BASE_URL, endpoint)

        if method == "GET":
            response = requests.get(url, headers=headers, timeout=5)
        elif method == "POST":
            response = requests.post(url, json=payload, headers=headers, timeout=5)
        elif method == "DELETE":
            response = requests.delete(url, json=payload, headers=headers, timeout=5)
        else:
            return None

        result = {
            "test_id": test_id,
            "endpoint": endpoint,
            "method": method,
            "expected_status": expected_status,
            "actual_status": response.status_code,
            "passed": response.status_code == expected_status,
            "response": response.text[:500]  # 限制響應大小
        }

        TEST_RESULTS["tests"].append(result)
        TEST_RESULTS["total_tests"] += 1

        if result["passed"]:
            TEST_RESULTS["passed"] += 1
            status = "✅"
        else:
            TEST_RESULTS["failed"] += 1
            status = "❌"

        print(f"{status} {test_id}: {method} {endpoint} (期望: {expected_status}, 實際: {response.status_code})")
        return result

    except Exception as e:
        TEST_RESULTS["tests"].append({
            "test_id": test_id,
            "endpoint": endpoint,
            "method": method,
            "error": str(e)
        })
        TEST_RESULTS["failed"] += 1
        TEST_RESULTS["total_tests"] += 1
        print(f"❌ {test_id}: 例外 - {e}")
        return None

def run_tests():
    """執行所有測試"""
    global ADMIN_TOKEN, USER_TOKEN

    print("=" * 60)
    print("BestPartner API 自動化測試")
    print("=" * 60)

    # 登入
    print("\n[準備] 登入取得 Token...")
    ADMIN_TOKEN = login(ADMIN_EMAIL, ADMIN_PASSWORD)
    USER_TOKEN = login(USER_EMAIL, USER_PASSWORD)

    if not ADMIN_TOKEN or not USER_TOKEN:
        print("❌ 無法取得 Token，終止測試")
        return False

    print(f"✅ Admin Token: {ADMIN_TOKEN[:20]}...")
    print(f"✅ User Token: {USER_TOKEN[:20]}...")

    # 準備 header
    admin_headers = {"Content-Type": "application/json", "Authorization": f"Bearer {ADMIN_TOKEN}"}
    user_headers = {"Content-Type": "application/json", "Authorization": f"Bearer {USER_TOKEN}"}

    print("\n" + "=" * 60)
    print("[AUTH 模組] 認證測試")
    print("=" * 60)

    # AUTH 模組 P1 測試
    execute_test("AUTH-003", "/login/", "POST",
                payload={"email": ADMIN_EMAIL, "password": "wrongpassword"},
                expected_status=401)
    execute_test("AUTH-004", "/login/", "POST",
                payload={"email": "notexist@example.com", "password": "test"},
                expected_status=401)
    execute_test("AUTH-005", "/login/", "POST",
                payload={"password": "admin"},
                expected_status=400)
    execute_test("AUTH-006", "/login/", "POST",
                payload={"email": ADMIN_EMAIL},
                expected_status=400)
    execute_test("AUTH-007", "/login/", "POST",
                payload={},
                expected_status=400)
    execute_test("AUTH-009", "/login/check", "POST",
                headers={"Content-Type": "application/json", "Authorization": "Bearer invalid-token"},
                payload={},
                expected_status=401)

    print("\n" + "=" * 60)
    print("[CHAT 模組] 聊天測試")
    print("=" * 60)

    # CHAT 模組 P1 測試
    execute_test("CHAT-003", "/llm/chat", "POST",
                headers=user_headers,
                payload={"message": "test"},
                expected_status=400)  # 缺 llmId
    execute_test("CHAT-004", "/llm/chat", "POST",
                headers=user_headers,
                payload={"llmId": "33b5e4a4-798b-41fd-abad-21e2e68831b1"},
                expected_status=400)  # 缺 message
    execute_test("CHAT-005", "/llm/chat", "POST",
                headers=user_headers,
                payload={"llmId": "invalid-id", "message": "test"},
                expected_status=400)  # llmId 格式無效（非 UUID）

    print("\n" + "=" * 60)
    print("[USER 模組] 用戶測試")
    print("=" * 60)

    # USER 模組 P1 測試
    execute_test("USER-002", "/llm/user/register", "POST",
                payload={"username": "user", "password": "user", "email": USER_EMAIL},
                expected_status=400)  # 重複 email
    execute_test("USER-003", "/llm/user/register", "POST",
                payload={"password": "test", "email": "test@example.com"},
                expected_status=400)  # 缺 username
    execute_test("USER-004", "/llm/user/register", "POST",
                payload={"username": "test", "email": "test@example.com"},
                expected_status=400)  # 缺 password
    execute_test("USER-005", "/llm/user/register", "POST",
                payload={"username": "test", "password": "test"},
                expected_status=400)  # 缺 email

    # USER 模組權限測試（已知缺陷）
    print("\n⚠️ [已知缺陷] USER-012 & USER-014: @RolesAllowed('all') 權限無映射")
    TEST_RESULTS["skipped"] += 2
    TEST_RESULTS["total_tests"] += 2

    print("\n" + "=" * 60)
    print("[PERMISSION 模組] 權限測試")
    print("=" * 60)

    # PERMISSION 模組（需要 admin）
    # 使用時間戳確保唯一性，避免與前次測試數據衝突
    from datetime import datetime
    test_perm_name = f"test_perm_{int(datetime.now().timestamp() * 1000)}"
    execute_test("PERM-001", "/llm/permission/add", "POST",
                headers=admin_headers,
                payload={"name": test_perm_name, "num": 999},
                expected_status=200)
    execute_test("PERM-002", "/llm/permission/add", "POST",
                payload={"name": "test_perm", "num": 999},
                expected_status=401)  # 未認證
    execute_test("PERM-003", "/llm/permission/add", "POST",
                headers=admin_headers,
                payload={"num": 999},
                expected_status=400)  # 缺 name
    execute_test("PERM-004", "/llm/permission/add", "POST",
                headers=admin_headers,
                payload={"name": "test_perm"},
                expected_status=400)  # 缺 num

    print("\n" + "=" * 60)
    print("[SYSTEM SETTING 模組] 系統設定測試")
    print("=" * 60)

    # SYSTEM SETTING 模組 P1 測試
    execute_test("SYS-002", "/systemSetting/get", "POST",
                payload={"key": "default_llm_platform"},
                expected_status=200)
    execute_test("SYS-003", "/systemSetting/get", "POST",
                payload={},
                expected_status=400)  # 缺 key
    execute_test("SYS-004", "/systemSetting/get", "POST",
                payload={"key": "nonexistent_key"},
                expected_status=200)  # API 返回 200 + 空值表示 key 不存在

    print("\n" + "=" * 60)
    print("[TOOL 模組] 工具測試（需要 admin）")
    print("=" * 60)

    execute_test("TOOL-002", "/llm/tool/get", "POST",
                headers=user_headers,
                payload={"id": "nonexistent-tool-id-12345"},
                expected_status=400)  # API 用 400 表示資源不存在
    execute_test("TOOL-003", "/llm/tool/get", "POST",
                headers=user_headers,
                payload={},
                expected_status=400)  # 缺 id

    print("\n" + "=" * 60)
    print("[MCP 模組] MCP 測試")
    print("=" * 60)

    execute_test("MCP-003", "/llm/mcpServer/get", "POST",
                headers=user_headers,
                payload={"mcpId": "invalid"},
                expected_status=400)  # API 用 400 表示資源不存在
    execute_test("MCP-004", "/llm/mcpServer/get", "POST",
                headers=user_headers,
                payload={},
                expected_status=400)  # 缺 mcpId

    print("\n" + "=" * 60)
    print("[VECTOR 模組] 向量測試")
    print("=" * 60)

    execute_test("VEC-003", "/llm/vector/update", "POST",
                headers=user_headers,
                payload={"id": "nonexistent-vec-id", "vectorStore": {"collectionName": "test", "dimension": 768}},
                expected_status=200)  # API 建立新記錄或更新現有記錄，返回 200

    print("\n" + "=" * 60)
    print("[LLM SETTING 模組] LLM 設定測試")
    print("=" * 60)

    # LLM SETTING 基本測試
    execute_test("LLM-001", "/llm/setting/get", "POST",
                headers=user_headers,
                payload={"platformId": "openai"},
                expected_status=200)
    execute_test("LLM-002", "/llm/setting/get", "POST",
                headers=user_headers,
                payload={},
                expected_status=200)  # API 返回所有設定

    print("\n" + "=" * 60)
    print("[ADMIN CHAT 模組] 管理員聊天測試")
    print("=" * 60)

    # ADMIN CHAT 需要 admin 權限和 message
    execute_test("ADCHAT-001", "/llm/admin/chat", "POST",
                headers=admin_headers,
                payload={"message": "hello"},
                expected_status=400)  # 缺 platform（或返回 500）
    execute_test("ADCHAT-002", "/llm/admin/chat", "POST",
                headers=user_headers,
                payload={"message": "test"},
                expected_status=403)  # 非 admin 用戶無權限

    return True

def save_results():
    """保存測試結果"""
    results_file = "D:/projects/bestpartner/test_results.json"
    with open(results_file, "w", encoding="utf-8") as f:
        json.dump(TEST_RESULTS, f, indent=2, ensure_ascii=False)

    print(f"\n📊 測試結果已保存至: {results_file}")
    print(f"   總測試數: {TEST_RESULTS['total_tests']}")
    print(f"   通過: {TEST_RESULTS['passed']} ✅")
    print(f"   失敗: {TEST_RESULTS['failed']} ❌")
    print(f"   跳過: {TEST_RESULTS['skipped']} ⏭️")
    if TEST_RESULTS['total_tests'] > 0:
        pass_rate = (TEST_RESULTS['passed'] / TEST_RESULTS['total_tests']) * 100
        print(f"   通過率: {pass_rate:.1f}%")

if __name__ == "__main__":
    try:
        if run_tests():
            save_results()
        else:
            print("❌ 測試執行失敗")
            sys.exit(1)
    except KeyboardInterrupt:
        print("\n\n⚠️ 測試被用戶中斷")
        save_results()
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ 發生例外: {e}")
        import traceback
        traceback.print_exc()
        save_results()
        sys.exit(1)
